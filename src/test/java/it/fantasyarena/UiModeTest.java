package it.fantasyarena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Verifica {@link UiMode#fromArgs(String[])}, l'unico punto in cui gli argomenti della riga di
 * comando vengono letti: nessun argomento, l'argomento web con e senza porta, e i due modi in cui
 * quegli argomenti possono essere rifiutati (modalità sconosciuta, porta non valida).
 */
class UiModeTest {

  @Test
  void nessunArgomento_restituisceLaModalitaConsole() {
    UiMode mode = UiMode.fromArgs(new String[0]);

    assertInstanceOf(UiMode.ConsoleMode.class, mode);
  }

  @Test
  void argomentoWeb_restituisceLaModalitaWebConLaPortaPredefinita() {
    UiMode mode = UiMode.fromArgs(new String[] {"web"});

    UiMode.WebMode webMode = assertInstanceOf(UiMode.WebMode.class, mode);
    assertEquals(8080, webMode.port());
  }

  @Test
  void argomentoWebConPorta_restituisceLaModalitaWebConQuellaPorta() {
    UiMode mode = UiMode.fromArgs(new String[] {"web", "9090"});

    UiMode.WebMode webMode = assertInstanceOf(UiMode.WebMode.class, mode);
    assertEquals(9090, webMode.port());
  }

  @Test
  void argomentoNonRiconosciuto_lanciaEccezioneCheElencaLeModalitaAmmesse() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> UiMode.fromArgs(new String[] {"grafica"}));

    assertTrue(exception.getMessage().contains("console"));
    assertTrue(exception.getMessage().contains("web"));
  }

  @Test
  void portaNonNumerica_lanciaEccezioneEsplicita() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> UiMode.fromArgs(new String[] {"web", "ottomilaeottanta"}));

    assertTrue(exception.getMessage().contains("ottomilaeottanta"));
  }

  @Test
  void portaFuoriIntervallo_lanciaEccezioneEsplicita() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> UiMode.fromArgs(new String[] {"web", "70000"}));

    assertTrue(exception.getMessage().contains("70000"));
  }
}
