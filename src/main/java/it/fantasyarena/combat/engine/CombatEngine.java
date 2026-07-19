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

/**
 * Orchestra l'intero duello: iniziativa, ciclo dei turni, condizione di fine ed esito.
 * Nessuna formula qui: delega ogni calcolo ai resolver del core tramite
 * {@link TurnOrchestrator}.
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

    List<TurnLogEntry> log = new ArrayList<>();
    int turnNumber = 0;

    while (turnNumber < settings.maxTurns() && !first.isDefeated() && !second.isDefeated()) {
      turnNumber++;
      TurnLogEntry entry = turnOrchestrator.playTurn(turnNumber, attacker, defender, context);
      log.add(entry.withVitals(vitalsSnapshot(first, second)));

      Fighter nextAttacker = defender;
      defender = attacker;
      attacker = nextAttacker;
    }

    return buildResult(first, second, turnNumber, log);
  }

  private Fighter resolveFirstMover(Fighter first, Fighter second) {
    DiceThrow firstThrow = diceRoller.d20();
    DiceThrow secondThrow = diceRoller.d20();
    return initiativeResolver.resolveFirstMover(first, second, firstThrow, secondThrow);
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
