package it.fantasyarena.combat.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Verifica {@link ScreenCleaner}: in {@link ScreenRefresh#CLEAR} emette sia la sequenza ANSI di
 * pulizia sia le righe vuote successive, in {@link ScreenRefresh#SCROLL} non stampa nulla. Usa il
 * costruttore di test che inietta il {@link PrintStream} di destinazione, senza toccare
 * {@code System.out}.
 */
class ScreenCleanerTest {

  private static final String CLEAR_SEQUENCE = "\033[H\033[2J";
  private static final int EXPECTED_BLANK_LINES = 80;

  private final ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
  private final PrintStream destination = new PrintStream(capturedOut, true, StandardCharsets.UTF_8);

  @Test
  void emetteLaSequenzaAnsiSeguitaDalleRigheVuoteInModalitaClear() {
    ScreenCleaner screenCleaner = new ScreenCleaner(ScreenRefresh.CLEAR, destination);

    screenCleaner.clear();

    String expected = CLEAR_SEQUENCE + System.lineSeparator().repeat(EXPECTED_BLANK_LINES);
    assertEquals(expected, capturedOutput());
  }

  @Test
  void nonEmetteNullaInModalitaScroll() {
    ScreenCleaner screenCleaner = new ScreenCleaner(ScreenRefresh.SCROLL, destination);

    screenCleaner.clear();

    assertTrue(capturedOutput().isEmpty());
  }

  private String capturedOutput() {
    return capturedOut.toString(StandardCharsets.UTF_8);
  }
}
