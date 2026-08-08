package it.fantasyarena.combat.io.web;

import it.fantasyarena.combat.chronicle.ArenaChronicle;
import it.fantasyarena.json.JsonSupport;

/**
 * Traduce la cronaca in JSON per il frontend, delegando a {@link JsonSupport}: l'{@link
 * com.fasterxml.jackson.databind.ObjectMapper} non vive più qui, ma nel package neutro
 * {@code it.fantasyarena.json}, condiviso con {@code combat.io.trace} senza che l'uno dei due
 * importi l'altro (Fase 8). Il comportamento resta quello di sempre: nessuna configurazione oltre
 * ai valori predefiniti di Jackson, i valori nulli restano nel JSON, gli enum si serializzano come
 * il loro nome, e i tipi di {@code combat.chronicle} non portano nessuna annotazione di Jackson.
 */
public class ChronicleJson {

  /**
   * Traduce la cronaca in una stringa JSON.
   *
   * @param chronicle cronaca da tradurre, non nulla
   * @return la cronaca come JSON
   * @throws IllegalStateException se la traduzione fallisce, il che indicherebbe un tipo della
   *     cronaca non serializzabile da Jackson
   */
  public String toJson(ArenaChronicle chronicle) {
    return JsonSupport.toJson(chronicle);
  }
}
