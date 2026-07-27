package it.fantasyarena.combat.io.terminal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * {@link TurnPacer} interattivo: attende la pressione del tasto INVIO tra un turno e il
 * successivo. Se lo standard input non e' interattivo (EOF), il replay prosegue senza
 * bloccarsi. Il messaggio di suggerimento e' parametrizzabile via costruttore: il costruttore
 * senza argomenti conserva il testo storico del duello 1v1 ("turno successivo"). Il factory
 * method {@link #withoutHint()} costruisce un pacer muto, per i percorsi in cui il suggerimento
 * e' gia' scritto altrove (es. la pagina del duello a schermo).
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

  /** Pacer senza alcun suggerimento: usato dove il testo è già stampato da chi lo circonda. */
  public static EnterKeyTurnPacer withoutHint() {
    return new EnterKeyTurnPacer(null);
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
    if (hintShown || hint == null) {
      return;
    }

    System.out.println(hint);
    hintShown = true;
  }
}
