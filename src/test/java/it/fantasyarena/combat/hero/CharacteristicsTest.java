package it.fantasyarena.combat.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import it.fantasycombatsystem.testsupport.CombatFixtures;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkitcore.core.model.CharacterClass;
import it.fantasytoolkitcore.core.model.Characteristic;
import it.fantasytoolkitcore.core.model.Race;

/**
 * L'unica ricostruzione a mano di un {@code CharacterResult}, condivisa da {@code EquipmentBonus}
 * e {@code HeroBrain}: un delta si somma alla caratteristica corrispondente, un delta su una
 * caratteristica assente viene ignorato, razza, classe e nome restano quelli di partenza.
 */
class CharacteristicsTest {

  private final CharacterResult character = CombatFixtures.createWarrior("Protagonista", 10, 11, 12, 13, 14);

  @Test
  void sommaIlDeltaAllaCaratteristicaCorrispondente() {
    Map<Characteristic, Integer> deltas = new EnumMap<>(Characteristic.class);
    deltas.put(Characteristic.STRENGTH, 3);

    CharacterResult increased = Characteristics.increasedBy(character, deltas);

    assertEquals(13, valueOf(increased, Characteristic.STRENGTH));
    assertEquals(10, valueOf(character, Characteristic.STRENGTH), "il personaggio base non cambia");
  }

  @Test
  void ignoraUnDeltaSuUnaCaratteristicaNonPresenteNelPersonaggio() {
    CharacterResult withoutLuck = new CharacterResult(Race.HUMAN, CharacterClass.WARRIOR, "SenzaFortuna",
        List.of(new CharacterCharacteristic(Characteristic.STRENGTH, 10),
            new CharacterCharacteristic(Characteristic.AGILITY, 11)));
    Map<Characteristic, Integer> deltas = new EnumMap<>(Characteristic.class);
    deltas.put(Characteristic.LUCK, 99);

    CharacterResult increased = Characteristics.increasedBy(withoutLuck, deltas);

    boolean containsLuck = increased.characteristics().stream()
        .anyMatch(entry -> entry.characteristic() == Characteristic.LUCK);
    assertFalse(containsLuck, "un delta su una caratteristica assente non deve aggiungere una voce per LUCK");
    assertEquals(10, valueOf(increased, Characteristic.STRENGTH), "le caratteristiche presenti non cambiano");
    assertEquals(11, valueOf(increased, Characteristic.AGILITY), "le caratteristiche presenti non cambiano");
  }

  @Test
  void unaMappaDiDeltaVuotaLasciaLeCaratteristicheInvariate() {
    CharacterResult increased = Characteristics.increasedBy(character, Map.of());

    character.characteristics().forEach(entry ->
        assertEquals(entry.value(), valueOf(increased, entry.characteristic())));
  }

  @Test
  void preservaRazzaClasseENomeDelPersonaggio() {
    CharacterResult increased = Characteristics.increasedBy(character, Map.of());

    assertEquals(character.race(), increased.race());
    assertEquals(character.characterClass(), increased.characterClass());
    assertEquals(character.name(), increased.name());
  }

  @Test
  void rifiutaUnPersonaggioODeltaNulli() {
    assertThrows(IllegalArgumentException.class, () -> Characteristics.increasedBy(null, Map.of()));
    assertThrows(IllegalArgumentException.class, () -> Characteristics.increasedBy(character, null));
  }

  private int valueOf(CharacterResult character, Characteristic characteristic) {
    return character.characteristics().stream()
        .filter(entry -> entry.characteristic() == characteristic)
        .mapToInt(CharacterCharacteristic::value)
        .findFirst()
        .orElseThrow();
  }
}
