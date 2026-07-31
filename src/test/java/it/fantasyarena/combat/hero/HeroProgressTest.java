package it.fantasyarena.combat.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.hero.HeroProgress.ArmourUpgrade;
import it.fantasyarena.combat.hero.HeroProgress.CharacteristicGain;
import it.fantasyarena.combat.hero.HeroProgress.JewelUpgrade;
import it.fantasyarena.combat.hero.HeroProgress.NewJewel;
import it.fantasyarena.combat.hero.HeroProgress.WeaponSwap;
import it.fantasycombatsystem.testsupport.CombatFixtures;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.jewelgenerator.result.JewelResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.Characteristic;
import it.fantasytoolkitcore.core.model.Jewel;
import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.Weapon;

/**
 * La derivazione di {@link HeroProgress#lootFate()}: i suoi otto casi, ciascuno costruito a mano
 * incrociando il tipo dell'unico {@link Loot} trovato con quale dei cinque campi del destino è
 * valorizzato — lo stesso incrocio che il metodo risolve.
 */
class HeroProgressTest {

  private static final List<CharacteristicGain> ANY_GAINS =
      List.of(new CharacteristicGain(Characteristic.STRENGTH, 3));

  @Test
  void armaTenutaQuandoLaTrovataBattePiuDellaImpugnata() {
    HeroProgress progress = new HeroProgress(heroWith(sword(5), chestplate(4)), Loot.ofWeapon(sword(9)),
        new WeaponSwap(sword(5), sword(9)), null, null, null, null, ANY_GAINS);

    assertEquals(LootFate.WEAPON_TAKEN, progress.lootFate());
  }

  @Test
  void armaScartataQuandoLaTrovataNonBattePiuDellaImpugnata() {
    HeroProgress progress = new HeroProgress(heroWith(sword(9), chestplate(4)), Loot.ofWeapon(sword(3)),
        null, null, null, null, null, ANY_GAINS);

    assertEquals(LootFate.WEAPON_DISCARDED, progress.lootFate());
  }

  @Test
  void armaturaIndossataSuUnoSlotPrimaScoperto() {
    HeroProgress progress = new HeroProgress(heroWith(sword(5), chestplate(4)),
        Loot.ofArmourPiece(piece(Armour.HELMET, 3)), null, piece(Armour.HELMET, 3), null, null, null, ANY_GAINS);

    assertEquals(LootFate.ARMOUR_WORN_ON_EMPTY_SLOT, progress.lootFate());
  }

  @Test
  void armaturaSostituitaQuandoLaTrovataDifendePiuDiQuellaIndossata() {
    HeroProgress progress = new HeroProgress(heroWith(sword(5), chestplate(6)),
        Loot.ofArmourPiece(chestplate(6)), null, null, new ArmourUpgrade(chestplate(2), chestplate(6)), null, null,
        ANY_GAINS);

    assertEquals(LootFate.ARMOUR_REPLACED, progress.lootFate());
  }

  @Test
  void armaturaScartataQuandoLaTrovataNonDifendePiuDiQuellaIndossata() {
    HeroProgress progress = new HeroProgress(heroWith(sword(5), chestplate(6)),
        Loot.ofArmourPiece(chestplate(2)), null, null, null, null, null, ANY_GAINS);

    assertEquals(LootFate.ARMOUR_DISCARDED, progress.lootFate());
  }

  @Test
  void gioielloIndossatoSuUnTipoPrimaScoperto() {
    JewelResult found = jewel(Jewel.RING, Rarity.RARE);
    HeroProgress progress = new HeroProgress(heroWith(sword(5), chestplate(4)), Loot.ofJewel(found),
        null, null, null, new NewJewel(found, 2), null, ANY_GAINS);

    assertEquals(LootFate.JEWEL_WORN_ON_EMPTY_TYPE, progress.lootFate());
  }

  @Test
  void gioielloSostituitoQuandoIlTrovatoEPiuRaroDiQuelloIndossato() {
    JewelResult found = jewel(Jewel.RING, Rarity.EPIC);
    JewelResult dropped = jewel(Jewel.RING, Rarity.UNCOMMON);
    HeroProgress progress = new HeroProgress(heroWith(sword(5), chestplate(4)), Loot.ofJewel(found),
        null, null, null, null, new JewelUpgrade(dropped, found, 3), ANY_GAINS);

    assertEquals(LootFate.JEWEL_REPLACED, progress.lootFate());
  }

  @Test
  void gioielloScartatoQuandoIlTrovatoNonEPiuRaroDiQuelloIndossato() {
    HeroProgress progress = new HeroProgress(heroWith(sword(5), chestplate(4)),
        Loot.ofJewel(jewel(Jewel.RING, Rarity.COMMON)), null, null, null, null, null, ANY_GAINS);

    assertEquals(LootFate.JEWEL_DISCARDED, progress.lootFate());
  }

  private Hero heroWith(WeaponResult weapon, ArmourResult armour) {
    return new Hero(CombatFixtures.createWarrior("Protagonista", 10, 10, 10, 10, 10), weapon, List.of(armour));
  }

  private WeaponResult sword(int attack) {
    return new WeaponResult(Weapon.SWORD, Rarity.COMMON, List.of(), List.of(), attack);
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
}
