package it.fantasyarena;

import it.fantasyarena.combat.Arena;
import it.fantasyarena.combat.io.terminal.ScreenRefresh;
import it.fantasycombatsystem.config.CombatSettings;

/**
 * Punto d'ingresso dell'applicazione: apre l'arena del protagonista e le lascia condurre la
 * partita. Tutto quello che c'era da chiedere all'utente sulla forma dello scontro non serve più —
 * le tre prove e la loro numerosità le decide l'{@link Arena} — e resta soltanto la scelta di come
 * rinfrescare lo schermo.
 *
 * <p>I singoli scontri li mette in scena {@code it.fantasyarena.combat.MatchRunner}, di cui l'arena
 * si serve una volta per prova.
 */
public class Main {

  public static void main(String[] args) {
    CombatSettings settings = CombatSettings.defaults();
    // ScreenRefresh screenRefresh = new CombatSetupPrompt().askScreenRefresh();
    ScreenRefresh screenRefresh = ScreenRefresh.CLEAR;

    new Arena(settings, screenRefresh).run();
  }
}
