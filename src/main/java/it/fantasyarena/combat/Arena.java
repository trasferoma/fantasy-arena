package it.fantasyarena.combat;

import it.fantasyarena.combat.io.CombatLogger;
import it.fantasyarena.combat.io.CombatReplay;
import it.fantasyarena.combat.io.ConsoleBattleLogger;
import it.fantasyarena.combat.io.ConsoleCombatLogger;
import it.fantasyarena.combat.io.EnterKeyTurnPacer;
import it.fantasyarena.combat.io.LinearCombatReplay;
import it.fantasyarena.combat.io.ReplayMode;
import it.fantasyarena.combat.io.ScreenCleaner;
import it.fantasyarena.combat.io.ScreenCombatReplay;
import it.fantasyarena.combat.io.ScreenRefresh;
import it.fantasyarena.combat.io.TurnPacer;
import it.fantasycombatsystem.CombatSystem;
import it.fantasycombatsystem.battle.BattleResult;
import it.fantasycombatsystem.battle.BattleSetup;
import it.fantasycombatsystem.battle.RoundLogEntry;
import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.result.CombatResult;

/**
 * Match runner del gioco: riceve i combattenti già pronti, chiede lo scontro al motore e lo rivela
 * all'utente. Nessuna regola di combattimento qui: quelle vivono nella libreria
 * {@code fantasy-combat-system}, dietro l'unica porta d'ingresso {@link CombatSystem}. Questa classe
 * possiede l'altra metà del problema, quella che il motore deliberatamente non affronta:
 * <em>come</em> e <em>con che ritmo</em> lo scontro viene mostrato.
 *
 * <p>Il motore restituisce l'intero log in un colpo solo; è qui che quel log viene rivelato un pezzo
 * alla volta. Due percorsi di presentazione, scelti dal chiamante in base alla forma dello scontro,
 * entrambi scanditi dallo stesso {@link TurnPacer} (costruito alla prima richiesta, con il testo di
 * suggerimento adatto al percorso che lo usa per primo): {@link #run} per il duello 1v1, a schermo
 * con modalità selezionabile ({@link ReplayMode}); {@link #runBattle} per la battaglia NvN, con la
 * scena ASCII round per round e attesa dell'INVIO fra un round e il successivo. La pulizia dello
 * schermo fra un turno/round e il successivo è una scelta ortogonale al {@link ReplayMode}
 * ({@link ScreenRefresh}), col default {@link ScreenRefresh#CLEAR}.
 *
 * <p>I {@link CombatSettings} passati qui devono essere gli stessi con cui i combattenti sono stati
 * assemblati (vedi {@link it.fantasyarena.combat.factory.FighterFactory}): i Rating intrinseci sono
 * calcolati alla creazione del combattente e non vengono ricalcolati durante lo scontro.
 */
public class Arena {

  private static final String BATTLE_ROUND_HINT = "(premi INVIO per avanzare al round successivo)";

  private final CombatSystem combatSystem;
  private final CombatSettings settings;
  private final CombatLogger logger;
  private final ReplayMode mode;
  private final ScreenRefresh screenRefresh;
  private final ScreenCleaner screenCleaner;

  private TurnPacer turnPacer;
  private CombatReplay replay;

  public Arena(CombatSettings settings) {
    this(settings, ReplayMode.SCREEN, ScreenRefresh.CLEAR);
  }

  public Arena(CombatSettings settings, ReplayMode mode) {
    this(settings, mode, ScreenRefresh.CLEAR);
  }

  public Arena(CombatSettings settings, ReplayMode mode, ScreenRefresh screenRefresh) {
    this(CombatSystem.withDefaults(settings), settings, mode, screenRefresh);
  }

  /**
   * Costruttore con motore esplicito: serve a chi deve rendere lo scontro riproducibile (un
   * {@link CombatSystem} costruito su dadi pilotati) invece di affidarsi ai dadi reali.
   */
  public Arena(CombatSystem combatSystem, CombatSettings settings, ReplayMode mode, ScreenRefresh screenRefresh) {
    this.combatSystem = combatSystem;
    this.settings = settings;
    this.logger = new ConsoleCombatLogger();
    this.mode = mode;
    this.screenRefresh = screenRefresh;
    this.screenCleaner = new ScreenCleaner(screenRefresh);
  }

  public void run(Fighter first, Fighter second) {
    logger.reportMatchup(first, second);
    CombatResult outcome = combatSystem.duel(first, second);
    duelReplay().replay(outcome, first, second);
    logger.reportOutcome(outcome, first, second);
  }

  /**
   * Costruito alla prima chiamata di {@link #run}, con l'unico {@link TurnPacer} di questo Arena.
   * In {@link ReplayMode#SCREEN} il pacer non stampa alcun suggerimento: la pagina prodotta da
   * {@link it.fantasyarena.combat.io.CombatScreenRenderer} lo mostra già. In
   * {@link ReplayMode#LINEAR} il testo resta quello storico ("turno successivo").
   */
  private CombatReplay duelReplay() {
    if (replay == null) {
      TurnPacer duelPacer = (mode == ReplayMode.SCREEN
          ? EnterKeyTurnPacer.withoutHint()
          : new EnterKeyTurnPacer());
      replay = buildReplay(mode, logger, sharedTurnPacer(duelPacer), settings.maxTurns());
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
   * Dispone una battaglia NvN: la fa giocare per intero al motore e ne stampa lo svolgimento con
   * {@link ConsoleBattleLogger}, la scena ASCII di ogni round (schermo pulito prima di ridisegnarla,
   * salvo {@link ScreenRefresh#SCROLL}), con attesa dell'INVIO (l'unico {@link TurnPacer} di questo
   * Arena, con il testo di suggerimento adatto al round) prima del round successivo.
   */
  public void runBattle(BattleSetup setup) {
    ConsoleBattleLogger battleLogger = new ConsoleBattleLogger();
    TurnPacer roundPacer = sharedTurnPacer(new EnterKeyTurnPacer(BATTLE_ROUND_HINT));

    battleLogger.reportSetup(setup);
    awaitReadingTimeForSetup(roundPacer);

    BattleResult result = combatSystem.battle(setup);
    for (RoundLogEntry round : result.roundLog()) {
      screenCleaner.clear();
      battleLogger.logRound(round);
      roundPacer.awaitNextTurn();
    }
    battleLogger.reportOutcome(result);
  }

  /**
   * Solo in {@link ScreenRefresh#CLEAR}: la prima pulizia di {@link #runBattle} cancellerebbe le
   * schede appena stampate da {@code reportSetup}, l'unico punto in cui si impara chi è chi.
   * Attende quindi una volta l'INVIO prima del primo round, così restano leggibili. In
   * {@link ScreenRefresh#SCROLL} non serve: le schede restano comunque visibili sopra i round.
   */
  private void awaitReadingTimeForSetup(TurnPacer roundPacer) {
    if (screenRefresh == ScreenRefresh.CLEAR) {
      roundPacer.awaitNextTurn();
    }
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
