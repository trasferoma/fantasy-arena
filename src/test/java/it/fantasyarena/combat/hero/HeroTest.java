package it.fantasyarena.combat.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasycombatsystem.testsupport.CombatFixtures;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.buffdebuffgenerator.result.BuffElement;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.jewelgenerator.result.JewelResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.Characteristic;
import it.fantasytoolkitcore.core.model.Jewel;
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

  @Test
  void unProtagonistaNasceSenzaGioielli() {
    Hero hero = new Hero(character, sword, List.of(piece(Armour.CHESTPLATE, 4)));

    assertEquals(0, hero.jewelCount());
    assertTrue(hero.jewels().isEmpty());
  }

  @Test
  void elencaIGioielliNellOrdineDeiTipiNonInQuelloDiRaccolta() {
    Hero hero = new Hero(character, sword, List.of(piece(Armour.CHESTPLATE, 4)))
        .wearing(jewel(Jewel.EARRING, Rarity.COMMON))
        .wearing(jewel(Jewel.RING, Rarity.COMMON));

    List<Jewel> types = hero.jewels().stream().map(JewelResult::jewel).toList();

    assertEquals(List.of(Jewel.RING, Jewel.EARRING), types,
        "l'ordine deve seguire gli slot di Jewel, non l'ordine con cui i gioielli sono arrivati");
  }

  @Test
  void indossareUnGioielloNonModificaLaSchedaDiPartenza() {
    Hero hero = new Hero(character, sword, List.of(piece(Armour.CHESTPLATE, 4)));

    Hero grown = hero.wearing(jewel(Jewel.RING, Rarity.COMMON));

    assertEquals(0, hero.jewelCount(), "la scheda di partenza deve restare intatta");
    assertEquals(1, grown.jewelCount());
  }

  @Test
  void unGioielloRimpiazzaQuelloDelloStessoTipo() {
    Hero hero = new Hero(character, sword, List.of(piece(Armour.CHESTPLATE, 4)))
        .wearing(jewel(Jewel.RING, Rarity.COMMON));

    Hero reequipped = hero.wearing(jewel(Jewel.RING, Rarity.LEGENDARY));

    assertEquals(1, reequipped.jewelCount(), "non si indossano due anelli");
    assertEquals(Rarity.LEGENDARY, reequipped.jewelOfType(Jewel.RING).orElseThrow().rarity());
  }

  @Test
  void unTipoScopertoNonHaGioiello() {
    Hero hero = new Hero(character, sword, List.of(piece(Armour.CHESTPLATE, 4)))
        .wearing(jewel(Jewel.RING, Rarity.COMMON));

    assertTrue(hero.jewelOfType(Jewel.NECKLACE).isEmpty());
  }

  @Test
  void leCaratteristicheEffettiveSommanoIBuffDiArmaArmaturaEGioiello() {
    WeaponResult buffedSword = weapon(Characteristic.STRENGTH, 2);
    ArmourResult buffedChestplate = piece(Armour.CHESTPLATE, 4, Characteristic.RESISTANCE, 1);
    JewelResult buffedRing = jewel(Jewel.RING, Rarity.COMMON, Characteristic.STRENGTH, 3);
    Hero hero = new Hero(character, buffedSword, List.of(buffedChestplate)).wearing(buffedRing);

    CharacterResult effective = hero.effectiveCharacter();

    assertEquals(valueOf(character, Characteristic.STRENGTH) + 5, valueOf(effective, Characteristic.STRENGTH),
        "spada e anello contribuiscono entrambi alla forza");
    assertEquals(valueOf(character, Characteristic.RESISTANCE) + 1, valueOf(effective, Characteristic.RESISTANCE));
  }

  @Test
  void laSchedaBaseNonRisenteDeiBuffDellEquipaggiamento() {
    WeaponResult buffedSword = weapon(Characteristic.STRENGTH, 2);
    Hero hero = new Hero(character, buffedSword, List.of(piece(Armour.CHESTPLATE, 4)));

    hero.effectiveCharacter();

    assertEquals(character, hero.character(), "la lettura delle caratteristiche effettive non deve alterare la base");
    assertEquals(character.characteristics(), hero.character().characteristics());
  }

  @Test
  void sostituireUnPezzoSostituisceIBonusNelleCaratteristicheEffettive() {
    ArmourResult weakChestplate = piece(Armour.CHESTPLATE, 4, Characteristic.RESISTANCE, 1);
    ArmourResult strongerChestplate = piece(Armour.CHESTPLATE, 7, Characteristic.RESISTANCE, 5);
    Hero hero = new Hero(character, sword, List.of(weakChestplate));

    Hero reequipped = hero.wearing(strongerChestplate);

    assertEquals(valueOf(character, Characteristic.RESISTANCE) + 5,
        valueOf(reequipped.effectiveCharacter(), Characteristic.RESISTANCE),
        "il combattente materializzato dopo il cambio riflette i buff del pezzo preso");
    assertEquals(valueOf(character, Characteristic.RESISTANCE) + 1,
        valueOf(hero.effectiveCharacter(), Characteristic.RESISTANCE),
        "la scheda di partenza continua a riflettere il pezzo lasciato");
  }

  private int valueOf(CharacterResult character, Characteristic characteristic) {
    return character.characteristics().stream()
        .filter(entry -> entry.characteristic() == characteristic)
        .mapToInt(CharacterCharacteristic::value)
        .findFirst()
        .orElseThrow();
  }

  private WeaponResult weapon(Characteristic characteristic, int value) {
    return new WeaponResult(Weapon.SWORD, Rarity.COMMON, List.of(new BuffElement(characteristic, value)), List.of(), 5);
  }

  private ArmourResult piece(Armour slot, int defense) {
    return new ArmourResult(slot, Rarity.COMMON, List.of(), List.of(), defense);
  }

  private ArmourResult piece(Armour slot, int defense, Characteristic characteristic, int buffValue) {
    return new ArmourResult(slot, Rarity.COMMON, List.of(new BuffElement(characteristic, buffValue)), List.of(), defense);
  }

  private JewelResult jewel(Jewel type, Rarity rarity) {
    return new JewelResult(type, rarity, List.of(), List.of());
  }

  private JewelResult jewel(Jewel type, Rarity rarity, Characteristic characteristic, int buffValue) {
    return new JewelResult(type, rarity, List.of(new BuffElement(characteristic, buffValue)), List.of());
  }
}
