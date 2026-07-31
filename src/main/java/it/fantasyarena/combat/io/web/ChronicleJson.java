package it.fantasyarena.combat.io.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.fantasyarena.combat.chronicle.ArenaChronicle;

/**
 * Traduce la cronaca in JSON per il frontend. L'{@link ObjectMapper} si costruisce una sola volta,
 * alla creazione di questa classe, e si riusa per ogni cronaca da tradurre: dopo la costruzione è
 * privo di stato mutabile condiviso fra le chiamate, quindi sicuro da richiamare da più richieste
 * HTTP concorrenti (Fase 8).
 *
 * <p>Non serve nessuna configurazione oltre ai valori predefiniti di Jackson, perché coincidono già
 * con quanto la cronaca richiede: i valori nulli restano nel JSON (l'inclusione predefinita è
 * {@code ALWAYS}, quindi non si imposta {@code NON_NULL}) e gli enum si serializzano come il loro
 * nome (comportamento predefinito, nessun {@code @JsonFormat} necessario). I tipi di
 * {@code combat.chronicle} non portano nessuna annotazione di Jackson: questo package è l'unico a
 * conoscere la libreria.
 */
public class ChronicleJson {

  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Traduce la cronaca in una stringa JSON.
   *
   * @param chronicle cronaca da tradurre, non nulla
   * @return la cronaca come JSON
   * @throws IllegalStateException se la traduzione fallisce, il che indicherebbe un tipo della
   *     cronaca non serializzabile da Jackson
   */
  public String toJson(ArenaChronicle chronicle) {
    try {
      return objectMapper.writeValueAsString(chronicle);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Impossibile tradurre la cronaca in JSON", e);
    }
  }
}
