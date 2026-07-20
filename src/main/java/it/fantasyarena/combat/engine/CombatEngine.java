package it.fantasyarena.combat.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceRoller;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.CombatOutcome;
import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.result.FighterVitals;
import it.fantasyarena.combat.result.InitiativeOverride;
import it.fantasyarena.combat.result.InitiativeReport;
import it.fantasyarena.combat.result.StaminaChange;
import it.fantasyarena.combat.result.TurnLogEntry;
import it.fantasyarena.combat.result.TurnResult;

/**
 * Orchestra l'intero duello: iniziativa, ciclo dei turni, condizione di fine ed esito.
 * Nessuna formula qui: delega ogni calcolo ai resolver del core tramite
 * {@link TurnOrchestrator} e {@link InitiativeResolver}. L'iniziativa è ricalcolata a fine di
 * ogni turno (nessuno swap cieco): il prossimo attaccante è deciso dai punteggi dei resolver,
 * con override deterministico se il difensore ha appena schivato o l'attaccante ha riposato.
 */
public class CombatEngine {

  private final DiceRoller diceRoller;
  private final InitiativeResolver initiativeResolver;
  private final TurnOrchestrator turnOrchestrator;
  private final CombatSettings settings;

  public CombatEngine(DiceRoller diceRoller, InitiativeResolver initiativeResolver, TurnOrchestrator turnOrchestrator,
      CombatSettings settings) {
    this.diceRoller = diceRoller;
    this.initiativeResolver = initiativeResolver;
    this.turnOrchestrator = turnOrchestrator;
    this.settings = settings;
  }

  public CombatResult fight(Fighter first, Fighter second, CombatContext context) {
    InitiativeDecision firstMoverDecision = resolveFirstMover(first, second);
    Fighter attacker = firstMoverDecision.chosen();
    Fighter defender = (attacker == first) ? second : first;
    applyInitiativeShift(attacker, defender);
    InitiativeReport currentInitiativeReport = firstMoverDecision.report();

    List<TurnLogEntry> log = new ArrayList<>();
    int turnNumber = 0;

    while (turnNumber < settings.maxTurns() && !first.isDefeated() && !second.isDefeated()) {
      turnNumber++;

      attacker.state().resetTurnStaminaCounters();
      defender.state().resetTurnStaminaCounters();

      List<FighterVitals> startOfTurnVitals = vitalsSnapshot(first, second);
      TurnResult turnResult = turnOrchestrator.playTurn(turnNumber, attacker, defender, context);
      TurnLogEntry logEntry = turnResult.logEntry()
          .withVitals(startOfTurnVitals)
          .withInitiative(currentInitiativeReport)
          .withStaminaChanges(staminaChanges(attacker, defender));
      log.add(logEntry);

      boolean combatContinues = turnNumber < settings.maxTurns() && !first.isDefeated() && !second.isDefeated();
      if (!combatContinues) {
        break;
      }

      InitiativeDecision nextDecision = resolveNextAttacker(attacker, defender, turnResult.override());
      Fighter nextAttacker = nextDecision.chosen();
      Fighter nextDefender = (nextAttacker == attacker) ? defender : attacker;
      applyInitiativeShift(nextAttacker, nextDefender);

      attacker = nextAttacker;
      defender = nextDefender;
      currentInitiativeReport = nextDecision.report();
    }

    return buildResult(first, second, turnNumber, log);
  }

  private InitiativeDecision resolveFirstMover(Fighter first, Fighter second) {
    DiceThrow firstJitter = rollJitter();
    DiceThrow secondJitter = rollJitter();
    return initiativeResolver.resolveFirstMover(first, second, firstJitter, secondJitter);
  }

  private InitiativeDecision resolveNextAttacker(Fighter attacker, Fighter defender, InitiativeOverride override) {
    DiceThrow attackerJitter = rollJitter();
    DiceThrow defenderJitter = rollJitter();
    return initiativeResolver.resolveNextAttacker(attacker, defender, override, attackerJitter, defenderJitter);
  }

  private List<StaminaChange> staminaChanges(Fighter attacker, Fighter defender) {
    return List.of(toStaminaChange(attacker), toStaminaChange(defender));
  }

  private StaminaChange toStaminaChange(Fighter fighter) {
    return new StaminaChange(fighter.name(), fighter.state().staminaConsumedThisTurn(),
        fighter.state().staminaRecoveredThisTurn());
  }

  private DiceThrow rollJitter() {
    int jitterDiceFaces = settings.initiativeWeights().jitterDiceFaces();
    return diceRoller.roll(jitterDiceFaces);
  }

  /**
   * Applica lo shift d'iniziativa: chi attacca il prossimo turno prosegue/avvia la sua catena
   * di attacchi consecutivi, chi la perde la azzera. Il recupero passivo di Stamina di chi non
   * è l'attore è responsabilità di {@link TurnOrchestrator#playTurn}, scoped al turno in cui
   * si verifica.
   */
  private void applyInitiativeShift(Fighter nextAttacker, Fighter nextDefender) {
    nextAttacker.state().winInitiative();
    nextDefender.state().loseInitiative();
  }

  private CombatResult buildResult(Fighter first, Fighter second, int rounds, List<TurnLogEntry> log) {
    List<FighterVitals> finalVitals = vitalsSnapshot(first, second);
    if (first.isDefeated() || second.isDefeated()) {
      Fighter winner = first.isDefeated() ? second : first;
      return new CombatResult(CombatOutcome.VICTORY, Optional.of(winner), rounds, log, finalVitals);
    }
    return buildTimeoutResult(first, second, rounds, log, finalVitals);
  }

  private CombatResult buildTimeoutResult(Fighter first, Fighter second, int rounds, List<TurnLogEntry> log,
      List<FighterVitals> finalVitals) {
    double firstHealthRatio = healthRatio(first);
    double secondHealthRatio = healthRatio(second);

    if (firstHealthRatio == secondHealthRatio) {
      return new CombatResult(CombatOutcome.DRAW, Optional.empty(), rounds, log, finalVitals);
    }

    Fighter winner = (firstHealthRatio > secondHealthRatio) ? first : second;
    return new CombatResult(CombatOutcome.TIMEOUT_DECISION, Optional.of(winner), rounds, log, finalVitals);
  }

  private double healthRatio(Fighter fighter) {
    return (double) fighter.state().currentHealth() / fighter.ratings().maxHealth();
  }

  private List<FighterVitals> vitalsSnapshot(Fighter first, Fighter second) {
    return List.of(toVitals(first), toVitals(second));
  }

  private FighterVitals toVitals(Fighter fighter) {
    return new FighterVitals(fighter.name(), fighter.state().currentHealth(), fighter.ratings().maxHealth(),
        fighter.state().currentStamina(), fighter.ratings().maxStamina());
  }
}
