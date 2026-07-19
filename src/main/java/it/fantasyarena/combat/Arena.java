package it.fantasyarena.combat;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceRoller;
import it.fantasyarena.combat.engine.CombatEngine;
import it.fantasyarena.combat.engine.DamageCalculator;
import it.fantasyarena.combat.engine.DefenseResolver;
import it.fantasyarena.combat.engine.HitResolver;
import it.fantasyarena.combat.engine.InitiativeResolver;
import it.fantasyarena.combat.engine.MomentumRules;
import it.fantasyarena.combat.engine.StaminaRules;
import it.fantasyarena.combat.engine.TurnOrchestrator;
import it.fantasyarena.combat.io.CombatLogger;
import it.fantasyarena.combat.io.ConsoleCombatLogger;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * Facade del sottosistema di combattimento: riceve due combattenti già pronti, dispone
 * il duello dimostrativo e ne riporta l'esito. Nessuna formula qui: solo orchestrazione
 * parlante. La preparazione dei combattenti è responsabilità esterna (vedi
 * {@link it.fantasyarena.Main}).
 */
public class Arena {

  private final CombatEngine combatEngine;
  private final CombatLogger logger;

  public Arena(CombatSettings settings) {
    MomentumRules momentumRules = new MomentumRules(settings);
    StaminaRules staminaRules = new StaminaRules(settings);
    DiceRoller diceRoller = new DiceRoller();

    HitResolver hitResolver = new HitResolver(settings);
    DefenseResolver defenseResolver = new DefenseResolver(settings);
    DamageCalculator damageCalculator = new DamageCalculator(settings, momentumRules, staminaRules);
    InitiativeResolver initiativeResolver = new InitiativeResolver();
    TurnOrchestrator turnOrchestrator = new TurnOrchestrator(
        diceRoller, hitResolver, defenseResolver, damageCalculator, momentumRules, staminaRules);

    this.combatEngine = new CombatEngine(diceRoller, initiativeResolver, turnOrchestrator, settings);
    this.logger = new ConsoleCombatLogger();
  }

  public void run(Fighter first, Fighter second) {
    logger.reportMatchup(first, second);
    CombatResult outcome = runDuel(first, second);
    reportOutcome(outcome);
  }

  private CombatResult runDuel(Fighter first, Fighter second) {
    return combatEngine.fight(first, second, CombatContext.empty());
  }

  private void reportOutcome(CombatResult outcome) {
    for (TurnLogEntry entry : outcome.log()) {
      logger.logTurn(entry);
    }
    logger.reportOutcome(outcome);
  }
}
