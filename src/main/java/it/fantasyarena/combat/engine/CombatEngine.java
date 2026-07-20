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
import it.fantasyarena.combat.result.TurnLogEntry;
import it.fantasyarena.combat.result.TurnResult;

/**
 * Orchestra l'intero duello: iniziativa, ciclo dei turni, condizione di fine ed esito.
 * Nessuna formula qui: delega ogni calcolo ai resolver del core tramite
 * {@link TurnOrchestrator} e {@link InitiativeResolver}. L'iniziativa è ricalcolata a fine di
 * ogni turno (nessuno swap cieco): il prossimo attaccante è deciso dai punteggi dei resolver,
 * con override deterministico se il difensore ha appena schivato.
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
    Fighter attacker = resolveFirstMover(first, second);
    Fighter defender = (attacker == first) ? second : first;
    applyInitiativeShift(attacker, defender);

    List<TurnLogEntry> log = new ArrayList<>();
    int turnNumber = 0;

    while (turnNumber < settings.maxTurns() && !first.isDefeated() && !second.isDefeated()) {
      turnNumber++;
      TurnResult turnResult = turnOrchestrator.playTurn(turnNumber, attacker, defender, context);
      log.add(turnResult.logEntry().withVitals(vitalsSnapshot(first, second)));

      boolean combatContinues = turnNumber < settings.maxTurns() && !first.isDefeated() && !second.isDefeated();
      if (!combatContinues) {
        break;
      }

      Fighter nextAttacker = resolveNextAttacker(attacker, defender, turnResult.defenderDodged());
      Fighter nextDefender = (nextAttacker == attacker) ? defender : attacker;
      applyInitiativeShift(nextAttacker, nextDefender);

      attacker = nextAttacker;
      defender = nextDefender;
    }

    return buildResult(first, second, turnNumber, log);
  }

  private Fighter resolveFirstMover(Fighter first, Fighter second) {
    DiceThrow firstJitter = rollJitter();
    DiceThrow secondJitter = rollJitter();
    return initiativeResolver.resolveFirstMover(first, second, firstJitter, secondJitter);
  }

  private Fighter resolveNextAttacker(Fighter attacker, Fighter defender, boolean defenderDodged) {
    DiceThrow attackerJitter = rollJitter();
    DiceThrow defenderJitter = rollJitter();
    return initiativeResolver.resolveNextAttacker(attacker, defender, defenderDodged, attackerJitter, defenderJitter);
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
    if (first.isDefeated() || second.isDefeated()) {
      Fighter winner = first.isDefeated() ? second : first;
      return new CombatResult(CombatOutcome.VICTORY, Optional.of(winner), rounds, log);
    }
    return buildTimeoutResult(first, second, rounds, log);
  }

  private CombatResult buildTimeoutResult(Fighter first, Fighter second, int rounds, List<TurnLogEntry> log) {
    double firstHealthRatio = healthRatio(first);
    double secondHealthRatio = healthRatio(second);

    if (firstHealthRatio == secondHealthRatio) {
      return new CombatResult(CombatOutcome.DRAW, Optional.empty(), rounds, log);
    }

    Fighter winner = (firstHealthRatio > secondHealthRatio) ? first : second;
    return new CombatResult(CombatOutcome.TIMEOUT_DECISION, Optional.of(winner), rounds, log);
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
