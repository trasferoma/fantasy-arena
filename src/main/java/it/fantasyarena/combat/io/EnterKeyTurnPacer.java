package it.fantasyarena.combat.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * {@link TurnPacer} interattivo: attende la pressione del tasto INVIO tra un turno e il
 * successivo. Se lo standard input non e' interattivo (EOF), il replay prosegue senza
 * bloccarsi. Il messaggio di suggerimento e' parametrizzabile via costruttore: il costruttore
 * senza argomenti conserva il testo storico del duello 1v1 ("turno successivo").
 */
public class EnterKeyTurnPacer implements TurnPacer {

  private static final String DEFAULT_HINT = "(premi INVIO per avanzare al turno successivo)";

  private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
  private final String hint;

  private boolean hintShown = false;

  public EnterKeyTurnPacer() {
    this(DEFAULT_HINT);
  }

  public EnterKeyTurnPacer(String hint) {
    this.hint = hint;
  }

  @Override
  public void awaitNextTurn() {
    showHintOnce();

    try {
      reader.readLine();
    } catch (IOException e) {
      // Stdin non disponibile: il replay prosegue senza bloccarsi.
    }
  }

  private void showHintOnce() {
    if (hintShown) {
      return;
    }

    System.out.println(hint);
    hintShown = true;
  }
}
