package it.fantasyarena.combat.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasycombatsystem.testsupport.CombatFixtures;
import it.fantasytoolkit.buffdebuffgenerator.result.BuffElement;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkitcore.core.model.Characteristic;

/**
 * Il punto unico che somma i buff dell'equipaggiamento alle caratteristiche base: più buff sulla
 * stessa caratteristica si sommano, un buff su una caratteristica assente viene ignorato, un
 * personaggio senza buff non cambia.
 */
class EquipmentBonusTest {

  private final CharacterResult character = CombatFixtures.createWarrior("Protagonista", 10, 11, 12, 13, 14);

  @Test
  void sommaIlBuffAllaCaratteristicaColpita() {
    List<BuffElement> buffs = List.of(new BuffElement(Characteristic.STRENGTH, 3));

    CharacterResult effective = EquipmentBonus.applyTo(character, buffs);

    assertEquals(13, valueOf(effective, Characteristic.STRENGTH));
    assertEquals(10, valueOf(character, Characteristic.STRENGTH), "il personaggio base non cambia");
  }

  @Test
  void sommaPiuBuffSullaStessaCaratteristicaDaOggettiDiversi() {
    List<BuffElement> buffs = List.of(
        new BuffElement(Characteristic.STRENGTH, 3),
        new BuffElement(Characteristic.STRENGTH, 2));

    CharacterResult effective = EquipmentBonus.applyTo(character, buffs);

    assertEquals(15, valueOf(effective, Characteristic.STRENGTH));
  }

  @Test
  void ignoraUnBuffSuUnaCaratteristicaNonPresenteNelPersonaggio() {
    CharacterResult withoutLuck = CombatFixtures.createWarrior("SenzaFortuna", 10, 11, 12, 13, 14);
    List<BuffElement> buffs = List.of(new BuffElement(Characteristic.LUCK, 99));

    CharacterResult effective = EquipmentBonus.applyTo(withoutLuck, buffs);

    assertEquals(withoutLuck.characteristics().size(), effective.characteristics().size(),
        "un buff su una caratteristica assente non deve aggiungere voci alla scheda");
  }

  @Test
  void unOggettoSenzaBuffNonCambiaLeCaratteristiche() {
    CharacterResult effective = EquipmentBonus.applyTo(character, List.of());

    character.characteristics().forEach(entry ->
        assertEquals(entry.value(), valueOf(effective, entry.characteristic())));
  }

  @Test
  void totalValueOfSommaTuttiIValoriDeiBuff() {
    List<BuffElement> buffs = List.of(
        new BuffElement(Characteristic.STRENGTH, 3),
        new BuffElement(Characteristic.AGILITY, 2));

    assertEquals(5, EquipmentBonus.totalValueOf(buffs));
  }

  @Test
  void rifiutaUnPersonaggioONeiBuffNulli() {
    assertThrows(IllegalArgumentException.class, () -> EquipmentBonus.applyTo(null, List.of()));
    assertThrows(IllegalArgumentException.class, () -> EquipmentBonus.applyTo(character, null));
  }

  private int valueOf(CharacterResult character, Characteristic characteristic) {
    return character.characteristics().stream()
        .filter(entry -> entry.characteristic() == characteristic)
        .mapToInt(CharacterCharacteristic::value)
        .findFirst()
        .orElseThrow();
  }
}
