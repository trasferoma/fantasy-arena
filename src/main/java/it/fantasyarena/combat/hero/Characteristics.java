package it.fantasyarena.combat.hero;

import java.util.List;
import java.util.Map;

import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkitcore.core.model.Characteristic;

/**
 * L'unico punto in cui un {@code CharacterResult} si ricostruisce a mano sommando dei delta alle
 * sue caratteristiche. Il toolkit non espone alcuna API per far crescere un personaggio esistente:
 * rigenerarlo col {@code CharacterGeneratorTool} ripescherebbe razza, nome e caratteristiche, cioè
 * creerebbe qualcun altro.
 *
 * <p>La classe serve due usi semanticamente diversi, che condividono però la stessa meccanica di
 * somma: la crescita permanente della scheda a fine livello ({@code HeroBrain.grow}) e il bonus
 * derivato dall'equipaggiamento, risolto alla lettura e mai custodito accanto alle caratteristiche
 * base ({@code EquipmentBonus.applyTo}).
 *
 * <p>Un delta su una caratteristica che il personaggio non possiede viene ignorato: la scheda
 * risultante non guadagna voci nuove, solo valori più alti su quelle già presenti.
 */
final class Characteristics {

  private Characteristics() {
  }

  /**
   * Il personaggio con i delta sommati alle caratteristiche corrispondenti, razza, classe e nome
   * invariati.
   *
   * @param base caratteristiche di partenza, non nullo
   * @param deltas quanto sommare a ciascuna caratteristica, non nullo
   * @return il personaggio con le caratteristiche aumentate
   * @throws IllegalArgumentException se {@code base} o {@code deltas} sono nulli
   */
  static CharacterResult increasedBy(CharacterResult base, Map<Characteristic, Integer> deltas) {
    validate(base, deltas);

    List<CharacterCharacteristic> increasedCharacteristics = base.characteristics().stream()
        .map(characteristic -> increasedBy(characteristic, deltas))
        .toList();

    return new CharacterResult(base.race(), base.characterClass(), base.name(), increasedCharacteristics);
  }

  private static CharacterCharacteristic increasedBy(CharacterCharacteristic characteristic,
      Map<Characteristic, Integer> deltas) {
    int delta = deltas.getOrDefault(characteristic.characteristic(), 0);
    return new CharacterCharacteristic(characteristic.characteristic(), characteristic.value() + delta);
  }

  private static void validate(CharacterResult base, Map<Characteristic, Integer> deltas) {
    if (base == null) {
      throw new IllegalArgumentException("base must not be null");
    }
    if (deltas == null) {
      throw new IllegalArgumentException("deltas must not be null");
    }
  }
}
