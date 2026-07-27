package it.fantasyarena.combat.io.terminal;

import java.io.PrintStream;

/**
 * Pulizia dello schermo, unico punto in cui vive la sequenza ANSI. In {@link ScreenRefresh#SCROLL}
 * {@link #clear()} non fa nulla: le pagine restano tutte visibili una sotto l'altra.
 *
 * <p>In {@link ScreenRefresh#CLEAR} la pulizia avviene su due livelli, entrambi necessari:
 *
 * <ol>
 *   <li>la sequenza ANSI viene scritta su {@code System.out}, così su un terminale vero Jansi la
 *       traduce nella chiamata di pulizia nativa della console;
 *   <li>seguono un certo numero di righe vuote, che spingono il contenuto precedente fuori dalla
 *       finestra visibile. È l'unico effetto ottenibile nelle console append-only (tipicamente
 *       quelle integrate negli IDE), che non implementano il controllo del cursore e ignorano la
 *       sequenza ANSI anche quando la ricevono correttamente.
 * </ol>
 *
 * <p>Nessuna delle due parti è ridondante: la prima serve ai terminali reali, la seconda alle
 * console che non li emulano.
 */
public class ScreenCleaner {

  private static final String CLEAR_SEQUENCE = "\033[H\033[2J";

  /**
   * Numero di righe vuote stampate dopo la sequenza ANSI. Non è una vera pulizia dello schermo, ma
   * l'unico modo per ottenere l'effetto in una console append-only (es. quella di un IDE), che non
   * supporta il controllo del cursore: il valore copre l'altezza tipica di una console.
   */
  private static final int BLANK_LINE_COUNT = 80;

  private final ScreenRefresh refresh;
  private final PrintStream destination;

  /** Scrive su {@code System.out}, così Jansi può tradurre la sequenza ANSI su un terminale vero. */
  public ScreenCleaner(ScreenRefresh refresh) {
    this(refresh, System.out);
  }

  /** Costruttore di test: consente di verificare l'output senza toccare {@code System.out}. */
  ScreenCleaner(ScreenRefresh refresh, PrintStream destination) {
    this.refresh = refresh;
    this.destination = destination;
  }

  public void clear() {
    if (refresh == ScreenRefresh.SCROLL) {
      return;
    }

    destination.print(CLEAR_SEQUENCE);
    for (int i = 0; i < BLANK_LINE_COUNT; i++) {
      destination.println();
    }
    destination.flush();
  }
}
