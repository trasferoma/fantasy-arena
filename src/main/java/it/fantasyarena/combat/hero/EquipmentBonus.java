package it.fantasyarena.combat.hero;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import it.fantasytoolkit.buffdebuffgenerator.result.BuffElement;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkitcore.core.model.Characteristic;

/**
 * Il punto unico che somma i buff dell'equipaggiamento alle caratteristiche base di un personaggio,
 * producendo le caratteristiche effettive. Vive in {@code combat.hero} perché lo usano sia
 * {@code combat.factory} (per assemblare il {@code Fighter} con le caratteristiche già maggiorate)
 * sia {@code combat.chronicle} (per fotografare il protagonista): nessuno dei due deve dipendere
 * dall'altro, e la somma non va duplicata.
 *
 * <p>Un buff su una caratteristica che il personaggio non possiede viene ignorato: la scheda
 * risultante non guadagna voci nuove, solo valori più alti su quelle già presenti. Più buff sulla
 * stessa caratteristica, arrivati da oggetti diversi, si sommano.
 */
public final class EquipmentBonus {

  private EquipmentBonus() {
  }

  /**
   * Il personaggio con addosso la somma dei buff: la ricostruzione del {@code CharacterResult} è
   * delegata a {@link Characteristics}, unico punto in cui il toolkit viene aggirato per far
   * crescere un personaggio esistente.
   *
   * @param base caratteristiche base del personaggio, non nullo
   * @param buffs i buff di tutti gli oggetti equipaggiati, non nullo
   * @return il personaggio con le caratteristiche effettive
   * @throws IllegalArgumentException se {@code base} o {@code buffs} sono nulli
   */
  public static CharacterResult applyTo(CharacterResult base, Collection<BuffElement> buffs) {
    validate(base, buffs);

    Map<Characteristic, Integer> bonusByCharacteristic = totalByCharacteristic(buffs);
    return Characteristics.increasedBy(base, bonusByCharacteristic);
  }

  /**
   * Il valore totale dei buff, senza distinguere su quale caratteristica cadano: è il criterio con
   * cui {@code HeroBrain} confronta due gioielli, che non hanno né attacco né difesa da misurare.
   */
  public static int totalValueOf(Collection<BuffElement> buffs) {
    if (buffs == null) {
      throw new IllegalArgumentException("buffs must not be null");
    }
    return buffs.stream().mapToInt(BuffElement::value).sum();
  }

  private static Map<Characteristic, Integer> totalByCharacteristic(Collection<BuffElement> buffs) {
    Map<Characteristic, Integer> bonusByCharacteristic = new EnumMap<>(Characteristic.class);
    buffs.forEach(buff -> bonusByCharacteristic.merge(buff.characteristic(), buff.value(), Integer::sum));
    return bonusByCharacteristic;
  }

  private static void validate(CharacterResult base, Collection<BuffElement> buffs) {
    if (base == null) {
      throw new IllegalArgumentException("base must not be null");
    }
    if (buffs == null) {
      throw new IllegalArgumentException("buffs must not be null");
    }
  }
}
