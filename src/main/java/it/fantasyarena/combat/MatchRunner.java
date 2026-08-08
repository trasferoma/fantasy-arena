package it.fantasyarena.combat;

import it.fantasyarena.combat.io.replay.ConsoleMatchPresentation;
import it.fantasyarena.combat.io.replay.MatchPresentation;
import it.fantasyarena.combat.io.replay.ReplayMode;
import it.fantasyarena.combat.io.terminal.ScreenRefresh;
import it.fantasycombatsystem.CombatSystem;
import it.fantasycombatsystem.battle.BattleResult;
import it.fantasycombatsystem.battle.BattleSetup;
import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.result.CombatResult;

/**
 * Fa giocare <em>un singolo scontro</em>: riceve i combattenti già pronti, ne chiede l'esito al
 * motore e lo consegna alla {@link MatchPresentation} che riceve come collaboratore, restituendolo
 * anche a chi lo ha chiesto. Nient'altro — chi combatte, perché, e cosa succede dopo sono affari
 * dell'{@link Arena}, che di questa classe si serve una volta per prova.
 *
 * <p>Nessuna regola di combattimento qui: quelle vivono nella libreria
 * {@code fantasy-combat-system}, dietro l'unica porta d'ingresso {@link CombatSystem}. Questa
 * classe non decide più nemmeno <em>come</em> e <em>con che ritmo</em> lo scontro viene mostrato:
 * quella responsabilità è della {@link MatchPresentation}, sostituibile fra la console di oggi e
 * una presentazione muta o futura, senza toccare questa classe.
 *
 * <p>Il motore restituisce l'intero log in un colpo solo; {@link #playDuel} e {@link #playBattle}
 * lo chiedono, lo consegnano alla presentazione e lo restituiscono al chiamante, che può
 * ignorarlo: è quello che fa oggi l'{@link Arena}.
 *
 * <p>I {@link CombatSettings} passati qui devono essere gli stessi con cui i combattenti sono stati
 * assemblati (vedi {@link it.fantasyarena.combat.factory.FighterFactory}): i Rating intrinseci sono
 * calcolati alla creazione del combattente e non vengono ricalcolati durante lo scontro.
 */
public class MatchRunner {

  private final CombatSystem combatSystem;
  private final MatchPresentation presentation;

  public MatchRunner(CombatSettings settings) {
    this(settings, ReplayMode.SCREEN, ScreenRefresh.CLEAR);
  }

  public MatchRunner(CombatSettings settings, ReplayMode mode) {
    this(settings, mode, ScreenRefresh.CLEAR);
  }

  public MatchRunner(CombatSettings settings, ReplayMode mode, ScreenRefresh screenRefresh) {
    this(CombatSystem.withDefaults(settings), settings, mode, screenRefresh);
  }

  /**
   * Costruttore con motore esplicito: serve a chi deve rendere lo scontro riproducibile (un
   * {@link CombatSystem} costruito su dadi pilotati) invece di affidarsi ai dadi reali.
   */
  public MatchRunner(CombatSystem combatSystem, CombatSettings settings, ReplayMode mode, ScreenRefresh screenRefresh) {
    this(combatSystem, new ConsoleMatchPresentation(settings, mode, screenRefresh));
  }

  /**
   * Costruttore con presentazione esplicita: serve a chi deve sostituire la console con un'altra
   * {@link MatchPresentation} — muta, o una futura presentazione web — senza toccare come lo
   * scontro viene giocato.
   */
  public MatchRunner(CombatSettings settings, MatchPresentation presentation) {
    this(CombatSystem.withDefaults(settings), presentation);
  }

  private MatchRunner(CombatSystem combatSystem, MatchPresentation presentation) {
    this.combatSystem = combatSystem;
    this.presentation = presentation;
  }

  public CombatResult playDuel(Fighter first, Fighter second) {
    CombatResult result = combatSystem.duel(first, second);
    presentation.presentDuel(first, second, result);
    return result;
  }

  public BattleResult playBattle(BattleSetup setup) {
    BattleResult result = combatSystem.battle(setup);
    presentation.presentBattle(setup, result);
    return result;
  }

  /**
   * I {@link CombatSettings} con cui questo scontro viene giocato, letti dal {@link CombatSystem}:
   * servono a chi deve portarli nella cronaca, non alla presentazione.
   */
  public CombatSettings settings() {
    return combatSystem.settings();
  }
}
