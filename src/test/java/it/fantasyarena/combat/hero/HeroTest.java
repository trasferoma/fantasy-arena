package it.fantasyarena.combat.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasycombatsystem.testsupport.CombatFixtures;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.Weapon;

/**
 * La scheda del protagonista: immutabile, indicizzata per slot d'armatura e priva di decisioni
 * (quelle sono di {@link HeroBrain}).
 */
class HeroTest {

  private final CharacterResult character = CombatFixtures.createWarrior("Protagonista", 10, 11, 12, 13, 14);
  private final WeaponResult sword = new WeaponResult(Weapon.SWORD, Rarity.COMMON, List.of(), List.of(), 5);

  @Test
  void elencaIPezziNellOrdineDegliSlotNonInQuelloDiRaccolta() {
    Hero hero = new Hero(character, sword, List.of(piece(Armour.BOOTS, 2), piece(Armour.CHESTPLATE, 4)));

    List<Armour> slots = hero.armourPieces().stream().map(ArmourResult::armour).toList();

    assertEquals(List.of(Armour.CHESTPLATE, Armour.BOOTS), slots,
        "l'ordine deve seguire gli slot di Armour, non l'ordine con cui i pezzi sono arrivati");
  }

  @Test
  void indossareUnPezzoNonModificaLaSchedaDiPartenza() {
    Hero hero = new Hero(character, sword, List.of(piece(Armour.CHESTPLATE, 4)));

    Hero grown = hero.wearing(piece(Armour.HELMET, 3));

    assertEquals(1, hero.armourPieceCount(), "la scheda di partenza deve restare intatta");
    assertEquals(2, grown.armourPieceCount());
  }

  @Test
  void unPezzoRimpiazzaQuelloDelloStessoSlot() {
    Hero hero = new Hero(character, sword, List.of(piece(Armour.CHESTPLATE, 4)));

    Hero reequipped = hero.wearing(piece(Armour.CHESTPLATE, 9));

    assertEquals(1, reequipped.armourPieceCount(), "non si indossano due corazze");
    assertEquals(9, reequipped.pieceCovering(Armour.CHESTPLATE).orElseThrow().defense());
  }

  @Test
  void unoSlotScopertoNonHaPezzo() {
    Hero hero = new Hero(character, sword, List.of(piece(Armour.CHESTPLATE, 4)));

    assertTrue(hero.pieceCovering(Armour.HELMET).isEmpty());
  }

  @Test
  void sommaIPuntiCaratteristicaDelPersonaggio() {
    Hero hero = new Hero(character, sword, List.of(piece(Armour.CHESTPLATE, 4)));

    int expected = character.characteristics().stream().mapToInt(entry -> entry.value()).sum();

    assertEquals(expected, hero.totalCharacteristicPoints());
  }

  @Test
  void rifiutaUnaSchedaSenzaArmaOSenzaArmatura() {
    assertThrows(IllegalArgumentException.class, () -> new Hero(character, null, List.of(piece(Armour.BOOTS, 2))));
    assertThrows(IllegalArgumentException.class, () -> new Hero(character, sword, List.of()));
    assertThrows(IllegalArgumentException.class, () -> new Hero(null, sword, List.of(piece(Armour.BOOTS, 2))));
  }

  private ArmourResult piece(Armour slot, int defense) {
    return new ArmourResult(slot, Rarity.COMMON, List.of(), List.of(), defense);
  }
}
