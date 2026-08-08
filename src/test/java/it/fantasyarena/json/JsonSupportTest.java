package it.fantasyarena.json;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Verifica il solo contratto che conta per chi dipende da {@link JsonSupport}: i valori nulli
 * restano nel JSON e gli enum si serializzano come il loro nome, senza nessuna configurazione
 * aggiuntiva.
 */
class JsonSupportTest {

  private record Sample(String name, Integer missing, SampleKind kind) {
  }

  private enum SampleKind {
    FIRST
  }

  private static final class FailingBean {

    public String getValue() {
      throw new IllegalStateException("valore non disponibile");
    }
  }

  @Test
  void iValoriNulliRestanoNelJsonEGliEnumSiSerializzanoComeIlLoroNome() {
    String json = JsonSupport.toJson(new Sample("Rivale", null, SampleKind.FIRST));

    assertTrue(json.contains("\"name\":\"Rivale\""));
    assertTrue(json.contains("\"missing\":null"));
    assertTrue(json.contains("\"kind\":\"FIRST\""));
  }

  @Test
  void unValoreNonSerializzabileSolleveUnaEccezioneSignificativa() {
    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> JsonSupport.toJson(new FailingBean()));

    assertTrue(exception.getMessage().contains("Impossibile tradurre in JSON"));
  }
}
