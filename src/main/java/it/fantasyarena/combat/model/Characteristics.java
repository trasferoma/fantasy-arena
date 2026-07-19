package it.fantasyarena.combat.model;

import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkitcore.core.model.Characteristic;

/**
 * Helper di sola lettura per estrarre il valore di una singola caratteristica da un
 * {@link CharacterResult}. Nessuna formula: solo accesso al dato.
 */
public final class Characteristics {

  private Characteristics() {
  }

  public static int valueOf(CharacterResult character, Characteristic characteristic) {
    return character.characteristics().stream()
        .filter(entry -> entry.characteristic() == characteristic)
        .findFirst()
        .map(CharacterCharacteristic::value)
        .orElseThrow(() -> new IllegalStateException("Caratteristica mancante nel personaggio: " + characteristic));
  }
}
