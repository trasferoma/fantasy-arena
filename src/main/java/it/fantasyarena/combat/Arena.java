package it.fantasyarena.combat;

import it.fantasyarena.combat.battle.BattleEngine;
import it.fantasyarena.combat.battle.BattleResult;
import it.fantasyarena.combat.battle.BattleSetup;
import it.fantasyarena.combat.battle.OutnumberedAllyAssigner;
import it.fantasyarena.combat.battle.PairwiseEngagementPlanner;
import it.fantasyarena.combat.battle.RoundLogEntry;
import it.fantasyarena.combat.battle.StickyTargetSelector;
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
import it.fantasyarena.combat.io.ConsoleBattleLogger;
import it.fantasyarena.combat.io.ConsoleCombatLogger;
import it.fantasyarena.combat.io.EnterKeyTurnPacer;
import it.fantasyarena.combat.io.LinearCombatReplay;
import it.fantasyarena.combat.io.ReplayMode;
import it.fantasyarena.combat.io.ScreenCombatReplay;
import it.fantasyarena.combat.io.TurnPacer;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.CombatResult;

/**
 * Facade del sottosistema di combattimento: riceve i combattenti già pronti e ne dispone lo
 * scontro. Nessuna formula qui: solo orchestrazione parlante. La preparazione dei combattenti è
 * responsabilità esterna (vedi {@link it.fantasyarena.Main}). Due percorsi di presentazione,
 * scelti dal chiamante in base alla forma dello scontro, entrambi scanditi dallo stesso
 * {@link TurnPacer} (costruito alla prima richiesta, con il testo di suggerimento adatto al
 * percorso che lo usa per primo): {@link #run} per il duello 1v1 storico, a schermo con modalità
 * selezionabile ({@link ReplayMode}); {@link #runBattle} per la battaglia NvN, con la scena ASCII
 * round per round e attesa dell'INVIO fra un round e il successivo.
 */
public class Arena {

  private static final String BATTLE_ROUND_HINT = "(premi INVIO per avanzare al round successivo)";

  private final DiceRoller diceRoller;
  private final InitiativeResolver initiativeResolver;
  private final TurnOrchestrator turnOrchestrator;
  private final StaminaRules staminaRules;
  private final CombatSettings settings;
  private final CombatEngine combatEngine;
  private final CombatLogger logger;
  private final ReplayMode mode;

  private TurnPacer turnPacer;
  private CombatReplay replay;

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

    this.diceRoller = diceRoller;
    this.initiativeResolver = initiativeResolver;
    this.turnOrchestrator = turnOrchestrator;
    this.staminaRules = staminaRules;
    this.settings = settings;
    this.combatEngine = new CombatEngine(diceRoller, initiativeResolver, turnOrchestrator, settings);
    this.logger = new ConsoleCombatLogger();
    this.mode = mode;
  }

  private CombatReplay buildReplay(ReplayMode mode, CombatLogger logger, TurnPacer turnPacer,
      int maxTurns) {
    return switch (mode) {
      case LINEAR -> new LinearCombatReplay(logger, turnPacer);
      case SCREEN -> new ScreenCombatReplay(turnPacer, maxTurns);
    };
  }

  public void run(Fighter first, Fighter second) {
    logger.reportMatchup(first, second);
    CombatResult outcome = runDuel(first, second);
    duelReplay().replay(outcome, first, second);
    logger.reportOutcome(outcome, first, second);
  }

  private CombatResult runDuel(Fighter first, Fighter second) {
    return combatEngine.fight(first, second, CombatContext.empty());
  }

  /**
   * Costruito alla prima chiamata di {@link #run}, con l'unico {@link TurnPacer} di questo Arena
   * (testo di suggerimento di default, invariato rispetto a oggi).
   */
  private CombatReplay duelReplay() {
    if (replay == null) {
      replay = buildReplay(mode, logger, sharedTurnPacer(new EnterKeyTurnPacer()), settings.maxTurns());
    }
    return replay;
  }

  /**
   * Dispone una battaglia NvN: assembla un {@link BattleEngine} sugli stessi collaboratori già
   * costruiti da questo Arena (più {@link StaminaRules} e le tre policy di default), la gioca per
   * intero e ne stampa lo svolgimento con {@link ConsoleBattleLogger}: la scena ASCII di ogni
   * round, con attesa dell'INVIO (l'unico {@link TurnPacer} di questo Arena, con il testo di
   * suggerimento adatto al round) prima del round successivo.
   */
  public void runBattle(BattleSetup setup) {
    BattleEngine battleEngine = new BattleEngine(diceRoller, initiativeResolver, turnOrchestrator, staminaRules,
        settings, new PairwiseEngagementPlanner(), new StickyTargetSelector(), new OutnumberedAllyAssigner());
    ConsoleBattleLogger battleLogger = new ConsoleBattleLogger();
    TurnPacer roundPacer = sharedTurnPacer(new EnterKeyTurnPacer(BATTLE_ROUND_HINT));

    battleLogger.reportSetup(setup);
    BattleResult result = battleEngine.fight(setup, CombatContext.empty());
    for (RoundLogEntry round : result.roundLog()) {
      battleLogger.logRound(round);
      roundPacer.awaitNextTurn();
    }
    battleLogger.reportOutcome(result);
  }

  /**
   * Un solo {@link TurnPacer} per tutto l'Arena, condiviso dai due percorsi: la prima chiamata
   * (da {@link #run} o {@link #runBattle}, a seconda di quale viene invocato) fissa quale dei due
   * candidati viene effettivamente costruito e usato.
   */
  private TurnPacer sharedTurnPacer(TurnPacer candidate) {
    if (turnPacer == null) {
      turnPacer = candidate;
    }
    return turnPacer;
  }
}
