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
import it.fantasyarena.combat.io.CombatReplay;
import it.fantasyarena.combat.io.ConsoleCombatLogger;
import it.fantasyarena.combat.io.EnterKeyTurnPacer;
import it.fantasyarena.combat.io.LinearCombatReplay;
import it.fantasyarena.combat.io.ReplayMode;
import it.fantasyarena.combat.io.ScreenCombatReplay;
import it.fantasyarena.combat.io.TurnPacer;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.CombatResult;

/**
 * Facade del sottosistema di combattimento: riceve due combattenti già pronti, dispone
 * il duello dimostrativo e ne riporta l'esito, scandendo il replay dei turni con
 * {@link TurnPacer}. Nessuna formula qui: solo orchestrazione parlante. La preparazione
 * dei combattenti è responsabilità esterna (vedi {@link it.fantasyarena.Main}). La modalità di
 * presentazione del replay ({@link ReplayMode}) è selezionabile senza toccare il motore.
 */
public class Arena {

  private final CombatEngine combatEngine;
  private final CombatLogger logger;
  private final CombatReplay replay;

  public Arena(CombatSettings settings) {
    this(settings, ReplayMode.SCREEN);
  }

  public Arena(CombatSettings settings, ReplayMode mode) {
    MomentumRules momentumRules = new MomentumRules(settings);
    StaminaRules staminaRules = new StaminaRules(settings);
    DiceRoller diceRoller = new DiceRoller();

    HitResolver hitResolver = new HitResolver(settings);
    DefenseResolver defenseResolver = new DefenseResolver(settings);
    DamageCalculator damageCalculator = new DamageCalculator(settings, momentumRules, staminaRules);
    InitiativeResolver initiativeResolver = new InitiativeResolver(settings);
    TurnOrchestrator turnOrchestrator = new TurnOrchestrator(
        diceRoller, hitResolver, defenseResolver, damageCalculator, momentumRules, staminaRules, settings);

    this.combatEngine = new CombatEngine(diceRoller, initiativeResolver, turnOrchestrator, settings);
    this.logger = new ConsoleCombatLogger();
    this.replay = buildReplay(mode, logger, new EnterKeyTurnPacer());
  }

  private CombatReplay buildReplay(ReplayMode mode, CombatLogger logger, TurnPacer turnPacer) {
    return switch (mode) {
      case LINEAR -> new LinearCombatReplay(logger, turnPacer);
      case SCREEN -> new ScreenCombatReplay(turnPacer);
    };
  }

  public void run(Fighter first, Fighter second) {
    logger.reportMatchup(first, second);
    CombatResult outcome = runDuel(first, second);
    replay.replay(outcome, first, second);
    logger.reportOutcome(outcome, first, second);
  }

  private CombatResult runDuel(Fighter first, Fighter second) {
    return combatEngine.fight(first, second, CombatContext.empty());
  }
}
