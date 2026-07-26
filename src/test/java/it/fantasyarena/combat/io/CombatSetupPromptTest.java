package it.fantasyarena.combat.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Verifica {@link CombatSetupPrompt#askScreenRefresh}: default {@link ScreenRefresh#CLEAR} su riga
 * vuota, EOF e valore non riconosciuto; {@link ScreenRefresh#SCROLL} solo su risposta negativa
 * esplicita.
 */
class CombatSetupPromptTest {

  @Test
  void restituisceClearSuRigaVuota() {
    CombatSetupPrompt prompt = promptReading("\n");

    assertEquals(ScreenRefresh.CLEAR, prompt.askScreenRefresh());
  }

  @Test
  void restituisceClearSuEof() {
    CombatSetupPrompt prompt = promptReading("");

    assertEquals(ScreenRefresh.CLEAR, prompt.askScreenRefresh());
  }

  @Test
  void restituisceClearSuValoreNonRiconosciuto() {
    CombatSetupPrompt prompt = promptReading("boh\n");

    assertEquals(ScreenRefresh.CLEAR, prompt.askScreenRefresh());
  }

  @Test
  void restituisceScrollSullaRispostaNegativa() {
    CombatSetupPrompt prompt = promptReading("n\n");

    assertEquals(ScreenRefresh.SCROLL, prompt.askScreenRefresh());
  }

  @Test
  void restituisceScrollSullaRispostaNegativaEstesaSenzaBadareAlleMaiuscole() {
    CombatSetupPrompt prompt = promptReading("NO\n");

    assertEquals(ScreenRefresh.SCROLL, prompt.askScreenRefresh());
  }

  private CombatSetupPrompt promptReading(String input) {
    InputStream stream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    return new CombatSetupPrompt(stream);
  }
}
