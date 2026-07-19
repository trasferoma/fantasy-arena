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
import it.fantasyarena.combat.factory.FighterFactory;
import it.fantasyarena.combat.io.CombatLogger;
import it.fantasyarena.combat.io.ConsoleCombatLogger;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.rating.DefaultRatingStrategy;
import it.fantasyarena.combat.rating.RatingStrategy;
import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * Facade del sottosistema di combattimento: prepara i combattenti, esegue il duello
 * dimostrativo e ne riporta l'esito. Nessuna formula qui: solo orchestrazione parlante,
 * la composizione delle dipendenze avviene qui come radice dell'applicazione.
 */
public class Arena {

  private final FighterFactory fighterFactory;
  private final CombatEngine combatEngine;
  private final CombatLogger logger;

  public Arena() {
    CombatSettings settings = CombatSettings.defaults();
    RatingStrategy ratingStrategy = new DefaultRatingStrategy(settings);
    MomentumRules momentumRules = new MomentumRules(settings);
    StaminaRules staminaRules = new StaminaRules(settings);
    DiceRoller diceRoller = new DiceRoller();

    HitResolver hitResolver = new HitResolver(settings);
    DefenseResolver defenseResolver = new DefenseResolver(settings);
    DamageCalculator damageCalculator = new DamageCalculator(settings, momentumRules, staminaRules);
    InitiativeResolver initiativeResolver = new InitiativeResolver();
    TurnOrchestrator turnOrchestrator = new TurnOrchestrator(
        diceRoller, hitResolver, defenseResolver, damageCalculator, momentumRules, staminaRules);

    this.fighterFactory = new FighterFactory(ratingStrategy);
    this.combatEngine = new CombatEngine(diceRoller, initiativeResolver, turnOrchestrator, settings);
    this.logger = new ConsoleCombatLogger();
  }

  public void run() {
    Duelists duelists = prepareFighters();
    CombatResult outcome = runDuel(duelists.first(), duelists.second());
    reportOutcome(outcome);
  }

  private Duelists prepareFighters() {
    Fighter first = fighterFactory.createSwordWarrior();
    Fighter second = fighterFactory.createSwordWarrior();
    return new Duelists(first, second);
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

  private record Duelists(Fighter first, Fighter second) {
  }
}
