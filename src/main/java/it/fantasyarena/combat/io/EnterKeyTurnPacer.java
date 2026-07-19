package it.fantasyarena.combat.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * {@link TurnPacer} interattivo: attende la pressione del tasto INVIO tra un turno e il
 * successivo. Se lo standard input non e' interattivo (EOF), il replay prosegue senza
 * bloccarsi.
 */
public class EnterKeyTurnPacer implements TurnPacer {

  private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

  private boolean hintShown = false;

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

    System.out.println("(premi INVIO per avanzare al turno successivo)");
    hintShown = true;
  }
}
