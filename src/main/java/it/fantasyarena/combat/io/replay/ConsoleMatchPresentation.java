package it.fantasyarena.combat.io.replay;

import it.fantasyarena.combat.io.log.BattleLogger;
import it.fantasyarena.combat.io.log.CombatLogger;
import it.fantasyarena.combat.io.log.ConsoleBattleLogger;
import it.fantasyarena.combat.io.log.ConsoleCombatLogger;
import it.fantasyarena.combat.io.render.CombatScreenRenderer;
import it.fantasyarena.combat.io.terminal.EnterKeyTurnPacer;
import it.fantasyarena.combat.io.terminal.ScreenCleaner;
import it.fantasyarena.combat.io.terminal.ScreenRefresh;
import it.fantasyarena.combat.io.terminal.TurnPacer;
import it.fantasycombatsystem.battle.BattleResult;
import it.fantasycombatsystem.battle.BattleSetup;
import it.fantasycombatsystem.battle.RoundLogEntry;
import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.result.CombatResult;

/**
 * {@link MatchPresentation} di console: il codice con cui, prima che questa classe esistesse,
 * {@code MatchRunner} mostrava da sé ogni scontro — spostato qui di casa perché il <em>come</em> e
 * il <em>quando</em> mostrare uno scontro sono ora un collaboratore sostituibile, non una
 * responsabilità del runner.
 *
 * <p>Due percorsi, scelti dal chiamante in base alla forma dello scontro, entrambi scanditi dallo
 * stesso {@link TurnPacer} (costruito alla prima richiesta, con il testo di suggerimento adatto al
 * percorso che lo usa per primo): {@link #presentDuel} per il duello 1v1, a schermo con modalità
 * selezionabile ({@link ReplayMode}); {@link #presentBattle} per la battaglia NvN, con la scena
 * ASCII round per round e attesa dell'INVIO fra un round e il successivo. La pulizia dello schermo
 * fra un turno/round e il successivo è una scelta ortogonale al {@link ReplayMode}
 * ({@link ScreenRefresh}), col default {@link ScreenRefresh#CLEAR}.
 */
public class ConsoleMatchPresentation implements MatchPresentation {

  private static final String BATTLE_ROUND_HINT = "(premi INVIO per avanzare al round successivo)";

  private final CombatLogger combatLogger;
  private final BattleLogger battleLogger;
  private final ReplayMode mode;
  private final ScreenRefresh screenRefresh;
  private final ScreenCleaner screenCleaner;
  private final int maxTurns;

  private TurnPacer turnPacer;
  private CombatReplay replay;

  /**
   * @param settings usato solo per {@link CombatSettings#maxTurns()}, il tetto di turni mostrato
   *     come denominatore del contatore in {@link ReplayMode#SCREEN}
   */
  public ConsoleMatchPresentation(CombatSettings settings, ReplayMode mode, ScreenRefresh screenRefresh) {
    this.combatLogger = new ConsoleCombatLogger();
    this.battleLogger = new ConsoleBattleLogger();
    this.mode = mode;
    this.screenRefresh = screenRefresh;
    this.screenCleaner = new ScreenCleaner(screenRefresh);
    this.maxTurns = settings.maxTurns();
  }

  @Override
  public void presentDuel(Fighter first, Fighter second, CombatResult result) {
    combatLogger.reportMatchup(first, second);
    duelReplay().replay(result, first, second);
    combatLogger.reportOutcome(result, first, second);
  }

  /**
   * Costruito alla prima chiamata di {@link #presentDuel}, con l'unico {@link TurnPacer} di questa
   * presentazione. In {@link ReplayMode#SCREEN} il pacer non stampa alcun suggerimento: la pagina
   * prodotta da {@link CombatScreenRenderer} lo mostra già. In {@link ReplayMode#LINEAR} il testo
   * resta quello storico ("turno successivo").
   */
  private CombatReplay duelReplay() {
    if (replay == null) {
      TurnPacer duelPacer = (mode == ReplayMode.SCREEN
          ? EnterKeyTurnPacer.withoutHint()
          : new EnterKeyTurnPacer());
      replay = buildReplay(mode, combatLogger, sharedTurnPacer(duelPacer), maxTurns);
    }
    return replay;
  }

  private CombatReplay buildReplay(ReplayMode mode, CombatLogger logger, TurnPacer turnPacer,
      int maxTurns) {
    return switch (mode) {
      case LINEAR -> new LinearCombatReplay(logger, turnPacer, screenCleaner);
      case SCREEN -> new ScreenCombatReplay(turnPacer, maxTurns, screenCleaner);
    };
  }

  /**
   * Mostra una battaglia NvN già giocata per intero dal motore: gli schieramenti, la scena ASCII di
   * ogni round (schermo pulito prima di ridisegnarla, salvo {@link ScreenRefresh#SCROLL}), con
   * attesa dell'INVIO (l'unico {@link TurnPacer} di questa presentazione, con il testo di
   * suggerimento adatto al round) prima del round successivo.
   */
  @Override
  public void presentBattle(BattleSetup setup, BattleResult result) {
    battleLogger.reportSetup(setup);
    TurnPacer roundPacer = sharedTurnPacer(new EnterKeyTurnPacer(BATTLE_ROUND_HINT));
    awaitReadingTimeForSetup(roundPacer);

    for (RoundLogEntry round : result.roundLog()) {
      screenCleaner.clear();
      battleLogger.logRound(round);
      roundPacer.awaitNextTurn();
    }
    battleLogger.reportOutcome(result);
  }

  /**
   * Solo in {@link ScreenRefresh#CLEAR}: la prima pulizia di {@link #presentBattle} cancellerebbe
   * le schede appena stampate da {@code reportSetup}, l'unico punto in cui si impara chi è chi.
   * Attende quindi una volta l'INVIO prima del primo round, così restano leggibili. In
   * {@link ScreenRefresh#SCROLL} non serve: le schede restano comunque visibili sopra i round.
   */
  private void awaitReadingTimeForSetup(TurnPacer roundPacer) {
    if (screenRefresh == ScreenRefresh.CLEAR) {
      roundPacer.awaitNextTurn();
    }
  }

  /**
   * Un solo {@link TurnPacer} per tutta la presentazione, condiviso dai due percorsi: la prima
   * chiamata (da {@link #presentDuel} o {@link #presentBattle}, a seconda di quale viene invocato
   * per primo) fissa quale dei due candidati viene effettivamente costruito e usato.
   */
  private TurnPacer sharedTurnPacer(TurnPacer candidate) {
    if (turnPacer == null) {
      turnPacer = candidate;
    }
    return turnPacer;
  }
}
