package it.fantasyarena.combat.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.hero.HeroProgress.ArmourUpgrade;
import it.fantasyarena.combat.hero.HeroProgress.CharacteristicGain;
import it.fantasycombatsystem.testsupport.CombatFixtures;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.buffdebuffgenerator.result.BuffElement;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.jewelgenerator.result.JewelResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.Characteristic;
import it.fantasytoolkitcore.core.model.Jewel;
import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.Weapon;

/**
 * Le decisioni del protagonista dopo una vittoria: se vale la pena tenere l'unico oggetto di loot
 * trovato e dove finiscono i punti guadagnati, vittoria più eventuale bonus del gioiello. Il
 * {@link Random} è a seme fisso, così la distribuzione dei punti resta verificabile senza ripetere
 * le esecuzioni sperando in una statistica.
 */
class HeroBrainTest {

  private static final int POINTS_PER_VICTORY = 3;

  private final HeroBrain brain = new HeroBrain(new Random(42));

  @Test
  void impugnaLArmaTrovataSeBattePiuDellaSuaPerAttacco() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4));
    Loot loot = Loot.ofWeapon(sword(9, Rarity.COMMON));

    HeroProgress progress = brain.progressAfterVictory(hero, loot);

    assertTrue(progress.weaponSwap().isPresent(), "un'arma che colpisce di più va impugnata");
    assertEquals(9, progress.weaponSwap().get().taken().attack());
    assertEquals(5, progress.weaponSwap().get().dropped().attack());
    assertEquals(9, progress.grownHero().weapon().attack());
  }

  @Test
  void scartaLArmaTrovataAParitaDiAttacco() {
    WeaponResult ownSword = sword(9, Rarity.COMMON);
    Hero hero = heroWith(ownSword, chestplate(4));
    Loot loot = Loot.ofWeapon(sword(9, Rarity.COMMON));

    HeroProgress progress = brain.progressAfterVictory(hero, loot);

    assertTrue(progress.weaponSwap().isEmpty(), "a parità di valore non si cambia arma per niente");
    assertSame(ownSword, progress.grownHero().weapon());
  }

  @Test
  void scartaLArmaTrovataQuandoBattePeggioDellaSua() {
    Hero hero = heroWith(sword(9, Rarity.COMMON), chestplate(4));
    Loot loot = Loot.ofWeapon(sword(3, Rarity.COMMON));

    HeroProgress progress = brain.progressAfterVictory(hero, loot);

    assertTrue(progress.weaponSwap().isEmpty());
    assertEquals(9, progress.grownHero().weapon().attack());
  }

  @Test
  void indossaIlPezzoTrovatoCheCopreUnoSlotScoperto() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4));
    Loot loot = Loot.ofArmourPiece(piece(Armour.HELMET, 1));

    HeroProgress progress = brain.progressAfterVictory(hero, loot);

    assertEquals(piece(Armour.HELMET, 1), progress.newPiece().orElseThrow(),
        "una parte del corpo scoperta si protegge anche con un pezzo scarso");
    assertTrue(progress.armourUpgrade().isEmpty());
    assertEquals(2, progress.grownHero().armourPieceCount());
  }

  @Test
  void sostituisceIlPezzoIndossatoSoloSeQuelloTrovatoDifendeDiPiu() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4));
    Loot loot = Loot.ofArmourPiece(piece(Armour.CHESTPLATE, 7));

    HeroProgress progress = brain.progressAfterVictory(hero, loot);

    assertEquals(new ArmourUpgrade(chestplate(4), piece(Armour.CHESTPLATE, 7)), progress.armourUpgrade().orElseThrow());
    assertTrue(progress.newPiece().isEmpty(), "lo slot era già coperto: è un rimpiazzo, non una novità");
    assertEquals(1, progress.grownHero().armourPieceCount());
    assertEquals(7, progress.grownHero().pieceCovering(Armour.CHESTPLATE).orElseThrow().defense());
  }

  @Test
  void scartaIlPezzoTrovatoCheDifendeMenoOQuantoQuelloIndossato() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(6));
    Loot loot = Loot.ofArmourPiece(piece(Armour.CHESTPLATE, 2));

    HeroProgress progress = brain.progressAfterVictory(hero, loot);

    assertTrue(progress.armourUpgrade().isEmpty());
    assertTrue(progress.newPiece().isEmpty());
    assertEquals(6, progress.grownHero().pieceCovering(Armour.CHESTPLATE).orElseThrow().defense());
  }

  @Test
  void indossaIlGioielloTrovatoSuUnTipoNonPossedutoENonFruttaPuntiOltreAiTreDellaVittoria() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4));
    int pointsBefore = hero.totalCharacteristicPoints();
    Loot loot = Loot.ofJewel(jewel(Jewel.RING, Rarity.RARE));

    HeroProgress progress = brain.progressAfterVictory(hero, loot);

    assertTrue(progress.newJewel().isPresent());
    assertTrue(progress.jewelUpgrade().isEmpty());
    assertEquals(1, progress.grownHero().jewelCount());
    assertEquals(Rarity.RARE, progress.grownHero().jewelOfType(Jewel.RING).orElseThrow().rarity());
    int distributed = progress.characteristicGains().stream().mapToInt(CharacteristicGain::points).sum();
    assertEquals(POINTS_PER_VICTORY, distributed, "il gioiello indossato non aggiunge punti oltre quelli della vittoria");
    assertEquals(pointsBefore + POINTS_PER_VICTORY, progress.grownHero().totalCharacteristicPoints());
  }

  @Test
  void sostituisceIlGioielloIndossatoSoloSeQuelloTrovatoEPiuRaroAParitaDiBuff() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4)).wearing(jewel(Jewel.RING, Rarity.UNCOMMON));
    Loot loot = Loot.ofJewel(jewel(Jewel.RING, Rarity.EPIC));

    HeroProgress progress = brain.progressAfterVictory(hero, loot);

    assertEquals(Rarity.UNCOMMON, progress.jewelUpgrade().orElseThrow().dropped().rarity());
    assertEquals(Rarity.EPIC, progress.jewelUpgrade().orElseThrow().taken().rarity());
    assertTrue(progress.newJewel().isEmpty(), "il tipo era già occupato: è un rimpiazzo, non una novità");
    assertEquals(1, progress.grownHero().jewelCount(), "due gioielli dello stesso tipo non convivono");
    assertEquals(Rarity.EPIC, progress.grownHero().jewelOfType(Jewel.RING).orElseThrow().rarity());
  }

  @Test
  void tieneIlGioielloDaiBuffPiuAltiEScartaAParitaDiValore() {
    JewelResult worn = jewelWithBuff(Jewel.RING, Rarity.RARE, Characteristic.STRENGTH, 4);
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4)).wearing(worn);
    JewelResult strongerFound = jewelWithBuff(Jewel.RING, Rarity.RARE, Characteristic.STRENGTH, 7);

    HeroProgress strongerProgress = brain.progressAfterVictory(hero, Loot.ofJewel(strongerFound));

    assertEquals(strongerFound, strongerProgress.jewelUpgrade().orElseThrow().taken(),
        "un valore di buff maggiore vale il rimpiazzo anche a parità di rarità");

    JewelResult sameValueFound = jewelWithBuff(Jewel.RING, Rarity.RARE, Characteristic.STRENGTH, 4);

    HeroProgress tieProgress = brain.progressAfterVictory(hero, Loot.ofJewel(sameValueFound));

    assertTrue(tieProgress.jewelUpgrade().isEmpty(), "a parità di valore dei buff e di rarità si tiene il proprio");
  }

  @Test
  void laRaritaDelGioielloDecideSoloAParitaDiValoreDeiBuff() {
    JewelResult worn = jewelWithBuff(Jewel.RING, Rarity.UNCOMMON, Characteristic.STRENGTH, 4);
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4)).wearing(worn);
    JewelResult rarerSameValueFound = jewelWithBuff(Jewel.RING, Rarity.EPIC, Characteristic.STRENGTH, 4);

    HeroProgress progress = brain.progressAfterVictory(hero, Loot.ofJewel(rarerSameValueFound));

    assertEquals(rarerSameValueFound, progress.jewelUpgrade().orElseThrow().taken(),
        "a parità di valore dei buff decide la rarità");
  }

  @Test
  void scartaIlGioielloTrovatoDiRaritaParioInferioreDelloStessoTipo() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4)).wearing(jewel(Jewel.RING, Rarity.RARE));
    Loot loot = Loot.ofJewel(jewel(Jewel.RING, Rarity.RARE));

    HeroProgress progress = brain.progressAfterVictory(hero, loot);

    assertTrue(progress.newJewel().isEmpty());
    assertTrue(progress.jewelUpgrade().isEmpty());
    assertEquals(1, progress.grownHero().jewelCount());
    assertEquals(Rarity.RARE, progress.grownHero().jewelOfType(Jewel.RING).orElseThrow().rarity());
  }

  @Test
  void ilGioielloScartatoNonFruttaPuntiExtraOltreAiTreDellaVittoria() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4)).wearing(jewel(Jewel.RING, Rarity.RARE));
    Loot loot = Loot.ofJewel(jewel(Jewel.RING, Rarity.COMMON));

    HeroProgress progress = brain.progressAfterVictory(hero, loot);

    int distributed = progress.characteristicGains().stream().mapToInt(CharacteristicGain::points).sum();
    assertEquals(POINTS_PER_VICTORY, distributed, "il gioiello scartato non aggiunge punti oltre quelli della vittoria");
  }

  @Test
  void distribuisceEsattamenteTrePuntiSulleCaratteristicheDelPersonaggioSenzaGioiello() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4));
    int pointsBefore = hero.totalCharacteristicPoints();
    Loot loot = Loot.ofArmourPiece(piece(Armour.CHESTPLATE, 1));

    HeroProgress progress = brain.progressAfterVictory(hero, loot);

    int distributed = progress.characteristicGains().stream().mapToInt(CharacteristicGain::points).sum();
    assertEquals(POINTS_PER_VICTORY, distributed);
    assertEquals(pointsBefore + POINTS_PER_VICTORY, progress.grownHero().totalCharacteristicPoints());
    assertFalse(progress.characteristicGains().isEmpty());
  }

  @Test
  void laSchedaCresciutaRestaLoStessoPersonaggio() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4));
    CharacterResult before = hero.character();

    CharacterResult after = brain.progressAfterVictory(hero, Loot.ofArmourPiece(piece(Armour.CHESTPLATE, 1)))
        .grownHero()
        .character();

    assertEquals(before.name(), after.name());
    assertEquals(before.race(), after.race());
    assertEquals(before.characterClass(), after.characterClass());
    assertEquals(before.characteristics().size(), after.characteristics().size());
  }

  private Hero heroWith(WeaponResult weapon, ArmourResult armour) {
    return new Hero(CombatFixtures.createWarrior("Protagonista", 10, 10, 10, 10, 10), weapon, List.of(armour));
  }

  private WeaponResult sword(int attack, Rarity rarity) {
    return new WeaponResult(Weapon.SWORD, rarity, List.of(), List.of(), attack);
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
