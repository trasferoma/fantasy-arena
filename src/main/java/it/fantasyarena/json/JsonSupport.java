package it.fantasyarena.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * L'unico {@link ObjectMapper} del progetto, condiviso da chi deve tradurre dati in JSON senza
 * dipendere l'uno dall'altro: {@code combat.io.web.ChronicleJson}, per la cronaca richiesta dalla
 * pagina, e {@code combat.io.trace}, per le righe JSON Lines del log analitico. Vive in un package
 * neutro, fuori da {@code combat}, apposta perché nessuno dei due lati debba importare l'altro solo
 * per arrivare a un {@link ObjectMapper}.
 *
 * <p>Nessuna configurazione oltre ai valori predefiniti di Jackson: i valori nulli restano nel
 * JSON (l'inclusione predefinita è {@code ALWAYS}) e gli enum si serializzano come il loro nome.
 * Nessun modulo Jackson aggiuntivo — in particolare nessun supporto a {@code java.time} o a
 * {@code Optional} — per cui i dati passati qui devono già essere nella forma che Jackson sa
 * tradurre da sé: stringhe già formattate per gli istanti, mai {@code Optional}.
 */
public final class JsonSupport {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private JsonSupport() {
  }

  /**
   * Traduce un valore in una stringa JSON.
   *
   * @param value valore da tradurre, non nullo
   * @return il valore come JSON
   * @throws IllegalStateException se la traduzione fallisce, il che indicherebbe un tipo non
   *     serializzabile da Jackson
   */
  public static String toJson(Object value) {
    try {
      return OBJECT_MAPPER.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Impossibile tradurre in JSON: " + value.getClass().getSimpleName(), e);
    }
  }
}
