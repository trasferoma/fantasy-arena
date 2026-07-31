package it.fantasyarena;

import it.fantasycombatsystem.config.CombatSettings;

/**
 * Punto d'ingresso dell'applicazione: legge la {@link UiMode} dagli argomenti della riga di comando
 * e la lascia avviarsi. Nessun argomento apre l'arena in console, esattamente come prima che la
 * modalità web esistesse; l'argomento {@code "web"} avvia il server al suo posto, senza giocare né
 * stampare nessuno scontro in console.
 *
 * <p>I singoli scontri li mette in scena {@code it.fantasyarena.combat.MatchRunner}, di cui l'arena
 * si serve una volta per prova, sia in console sia nella partita muta che alimenta il server.
 *
 * <p></p>Accedi a http://127.0.0.1:8080/ per la versione web.</p>
 */
public class Main {

  public static void main(String[] args) {
    CombatSettings settings = CombatSettings.defaults();

    UiMode.fromArgs(args).launch(settings);
  }
}
