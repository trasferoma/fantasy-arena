package it.fantasyarena.combat.io.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.hero.Hero;
import it.fantasyarena.combat.hero.HeroProgress;
import it.fantasyarena.combat.hero.HeroProgress.ArmourDecision;
import it.fantasyarena.combat.hero.HeroProgress.CharacteristicGain;
import it.fantasyarena.combat.hero.HeroProgress.JewelDecision;
import it.fantasyarena.combat.hero.HeroProgress.WeaponSwap;
import it.fantasyarena.combat.hero.Loot;
import it.fantasycombatsystem.testsupport.CombatFixtures;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.buffdebuffgenerator.result.BuffElement;
import it.fantasytoolkit.jewelgenerator.result.JewelResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.Characteristic;
import it.fantasytoolkitcore.core.model.Jewel;
import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.Weapon;

/**
 * Il racconto della procedura di fine scontro, verificato sul testo prodotto: deve nominare sempre
 * l'oggetto trovato e il suo destino — equipaggiato, sostituito o scartato — qualunque sia il tipo
 * di loot arrivato, oltre a cura e crescita.
 */
class HeroProgressFormatterTest {

  private final HeroProgressFormatter formatter = new HeroProgressFormatter();

  @Test
  void raccontaCuraArmaImpugnataECrescita() {
    HeroProgress progress = new HeroProgress(heroWith(sword(9, Rarity.RARE), chestplate(4)),
        Loot.ofWeapon(sword(9, Rarity.RARE)),
        new WeaponSwap(sword(4, Rarity.COMMON), sword(9, Rarity.RARE)),
        ArmourDecision.none(), JewelDecision.none(),
        List.of(new CharacteristicGain(Characteristic.STRENGTH, 2), new CharacteristicGain(Characteristic.LUCK, 1)));

    List<String> lines = formatter.lines(progress);

    assertEquals("--- PROCEDURA DI FINE SCONTRO ---", lines.getFirst());
    assertTrue(lines.get(1).contains("vita e stamina tornano piene"), lines.get(1));
    assertTrue(lines.contains("Arma: trovi SWORD (RARE, atk 9), lasci SWORD (COMMON, atk 4) e la impugni."),
        lines.toString());
    assertTrue(lines.stream().noneMatch(line -> line.startsWith("Bonus")),
        "un'arma senza buff non deve produrre nessuna riga di bonus: " + lines);
    assertEquals("Crescita: +2 STRENGTH, +1 LUCK.", lines.getLast());
  }

  @Test
  void raccontaLArmaImpugnataConIBonusDiEntrambeLeArmi() {
    WeaponResult found = swordWithBuff(9, Rarity.RARE, Characteristic.STRENGTH, 3);
    WeaponResult dropped = swordWithBuff(4, Rarity.COMMON, Characteristic.LUCK, 1);
    HeroProgress progress = new HeroProgress(heroWith(found, chestplate(4)),
        Loot.ofWeapon(found), new WeaponSwap(dropped, found),
        ArmourDecision.none(), JewelDecision.none(),
        List.of(new CharacteristicGain(Characteristic.STRENGTH, 3)));

    List<String> lines = formatter.lines(progress);

    assertTrue(lines.contains("Bonus dell'oggetto trovato: +3 STRENGTH."), lines.toString());
    assertTrue(lines.contains("Bonus dell'oggetto lasciato: +1 LUCK."), lines.toString());
  }

  @Test
  void raccontaLArmaScartataQuandoNonBatteLaSua() {
    HeroProgress progress = new HeroProgress(heroWith(sword(6, Rarity.COMMON), chestplate(4)),
        Loot.ofWeapon(sword(3, Rarity.COMMON)),
        null, ArmourDecision.none(), JewelDecision.none(),
        List.of(new CharacteristicGain(Characteristic.AGILITY, 3)));

    List<String> lines = formatter.lines(progress);

    assertTrue(lines.contains("Arma: trovi SWORD (COMMON, atk 3), non batte la tua: la scarti."), lines.toString());
  }

  @Test
  void raccontaIlPezzoDArmaturaIndossatoSuUnoSlotScoperto() {
    HeroProgress progress = new HeroProgress(heroWith(sword(5, Rarity.COMMON), chestplate(4)),
        Loot.ofArmourPiece(piece(Armour.HELMET, 3)),
        null, ArmourDecision.covering(piece(Armour.HELMET, 3)), JewelDecision.none(),
        List.of(new CharacteristicGain(Characteristic.AGILITY, 3)));

    List<String> lines = formatter.lines(progress);

    assertTrue(lines.contains("Armatura: trovi HELMET (COMMON, def 3), copre una parte del corpo prima scoperta: "
        + "la indossi."), lines.toString());
  }

  @Test
  void raccontaLaSostituzioneDelPezzoDArmatura() {
    HeroProgress progress = new HeroProgress(heroWith(sword(5, Rarity.COMMON), chestplate(6)),
        Loot.ofArmourPiece(chestplate(6)),
        null, ArmourDecision.replacing(chestplate(2), chestplate(6)), JewelDecision.none(),
        List.of(new CharacteristicGain(Characteristic.AGILITY, 3)));

    List<String> lines = formatter.lines(progress);

    assertTrue(lines.contains("Armatura: trovi CHESTPLATE (COMMON, def 6), sostituisce CHESTPLATE (COMMON, def 2)."),
        lines.toString());
  }

  @Test
  void raccontaIlPezzoDArmaturaScartatoQuandoDifendeMenoODellaSua() {
    HeroProgress progress = new HeroProgress(heroWith(sword(5, Rarity.COMMON), chestplate(6)),
        Loot.ofArmourPiece(chestplate(2)),
        null, ArmourDecision.none(), JewelDecision.none(),
        List.of(new CharacteristicGain(Characteristic.AGILITY, 3)));

    List<String> lines = formatter.lines(progress);

    assertTrue(lines.contains("Armatura: trovi CHESTPLATE (COMMON, def 2), difende meno o quanto la tua: la scarti."),
        lines.toString());
  }

  @Test
  void raccontaIlGioielloIndossatoSuUnTipoScopertoConIlSuoBonus() {
    JewelResult jewel = jewelWithBuff(Jewel.RING, Rarity.RARE, Characteristic.STRENGTH, 2);
    HeroProgress progress = new HeroProgress(heroWith(sword(5, Rarity.COMMON), chestplate(4)),
        Loot.ofJewel(jewel),
        null, ArmourDecision.none(), JewelDecision.wearing(jewel),
        List.of(new CharacteristicGain(Characteristic.AGILITY, 5)));

    List<String> lines = formatter.lines(progress);

    assertTrue(lines.contains("Gioiello: trovi RING (RARE), è un tipo che non portavi ancora: lo indossi."),
        lines.toString());
    assertTrue(lines.contains("Bonus dell'oggetto trovato: +2 STRENGTH."), lines.toString());
    assertEquals("Crescita: +5 AGILITY.", lines.getLast());
  }

  @Test
  void raccontaLaSostituzioneDelGioielloConIBonusDiEntrambi() {
    JewelResult found = jewelWithBuff(Jewel.RING, Rarity.EPIC, Characteristic.STRENGTH, 3);
    JewelResult dropped = jewelWithBuff(Jewel.RING, Rarity.UNCOMMON, Characteristic.RESISTANCE, 1);
    HeroProgress progress = new HeroProgress(heroWith(sword(5, Rarity.COMMON), chestplate(4)),
        Loot.ofJewel(found),
        null, ArmourDecision.none(), JewelDecision.replacing(dropped, found),
        List.of(new CharacteristicGain(Characteristic.AGILITY, 3)));

    List<String> lines = formatter.lines(progress);

    assertTrue(lines.contains("Gioiello: trovi RING (EPIC), sostituisce RING (UNCOMMON)."), lines.toString());
    assertTrue(lines.contains("Bonus dell'oggetto trovato: +3 STRENGTH."), lines.toString());
    assertTrue(lines.contains("Bonus dell'oggetto lasciato: +1 RESISTANCE."), lines.toString());
  }

  @Test
  void raccontaIlGioielloScartatoQuandoNonBatteQuelloIndossato() {
    JewelResult found = jewel(Jewel.RING, Rarity.COMMON);
    HeroProgress progress = new HeroProgress(heroWith(sword(5, Rarity.COMMON), chestplate(4)),
        Loot.ofJewel(found),
        null, ArmourDecision.none(), JewelDecision.none(),
        List.of(new CharacteristicGain(Characteristic.AGILITY, 3)));

    List<String> lines = formatter.lines(progress);

    assertTrue(lines.contains("Gioiello: trovi RING (COMMON), non batte quello che porti: lo scarti."),
        lines.toString());
  }

  private Hero heroWith(WeaponResult weapon, ArmourResult armour) {
    return new Hero(CombatFixtures.createWarrior("Protagonista", 10, 10, 10, 10, 10), weapon, List.of(armour));
  }

  private WeaponResult sword(int attack, Rarity rarity) {
    return new WeaponResult(Weapon.SWORD, rarity, List.of(), List.of(), attack);
  }

  private WeaponResult swordWithBuff(int attack, Rarity rarity, Characteristic characteristic, int buffValue) {
    return new WeaponResult(Weapon.SWORD, rarity, List.of(new BuffElement(characteristic, buffValue)), List.of(),
        attack);
  }

  private ArmourResult chestplate(int defense) {
    return piece(Armour.CHESTPLATE, defense);
  }

  private ArmourResult piece(Armour slot, int defense) {
    return new ArmourResult(slot, Rarity.COMMON, List.of(), List.of(), defense);
  }

  private JewelResult jewel(Jewel type, Rarity rarity) {
    return new JewelResult(type, rarity, List.of(), List.of());
  }

  private JewelResult jewelWithBuff(Jewel type, Rarity rarity, Characteristic characteristic, int buffValue) {
    return new JewelResult(type, rarity, List.of(new BuffElement(characteristic, buffValue)), List.of());
  }
}
