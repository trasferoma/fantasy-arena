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
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.Weapon;

/**
 * Le decisioni del protagonista dopo una vittoria: cosa raccoglie dal terreno e dove finiscono i
 * punti guadagnati. Il {@link Random} è a seme fisso, così la distribuzione dei punti resta
 * verificabile senza ripetere le esecuzioni sperando in una statistica.
 */
class HeroBrainTest {

  private static final int POINTS_PER_VICTORY = 3;

  private final HeroBrain brain = new HeroBrain(new Random(42));

  @Test
  void raccoglieLArmaMiglioreLasciataSulTerreno() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4));
    Spoils spoils = new Spoils(List.of(sword(9, Rarity.COMMON)), List.of());

    HeroProgress progress = brain.progressAfterVictory(hero, spoils);

    assertTrue(progress.weaponSwap().isPresent(), "un'arma che colpisce di più va raccolta");
    assertEquals(9, progress.weaponSwap().get().taken().attack());
    assertEquals(5, progress.weaponSwap().get().dropped().attack());
    assertEquals(9, progress.grownHero().weapon().attack());
  }

  @Test
  void tieneLaSuaArmaQuandoIlBottinoNonOffreDiMeglio() {
    WeaponResult ownSword = sword(9, Rarity.COMMON);
    Hero hero = heroWith(ownSword, chestplate(4));
    Spoils spoils = new Spoils(List.of(sword(9, Rarity.COMMON), sword(3, Rarity.COMMON)), List.of());

    HeroProgress progress = brain.progressAfterVictory(hero, spoils);

    assertTrue(progress.weaponSwap().isEmpty(), "a parità di valore non si cambia arma per niente");
    assertSame(ownSword, progress.grownHero().weapon());
  }

  @Test
  void aParitaDiAttaccoPreferisceLArmaPiuRara() {
    Hero hero = heroWith(sword(6, Rarity.COMMON), chestplate(4));
    Spoils spoils = new Spoils(List.of(new WeaponResult(Weapon.AXE, Rarity.RARE, List.of(), List.of(), 6)), List.of());

    HeroProgress progress = brain.progressAfterVictory(hero, spoils);

    assertTrue(progress.weaponSwap().isPresent(), "la rarità è lo spareggio a parità di attacco");
    assertEquals(Rarity.RARE, progress.grownHero().weapon().rarity());
  }

  @Test
  void indossaUnPezzoCheCopreUnoSlotScoperto() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4));
    Spoils spoils = new Spoils(List.of(), List.of(piece(Armour.HELMET, 1)));

    HeroProgress progress = brain.progressAfterVictory(hero, spoils);

    assertEquals(List.of(piece(Armour.HELMET, 1)), progress.newPieces(),
        "una parte del corpo scoperta si protegge anche con un pezzo scarso");
    assertTrue(progress.armourUpgrades().isEmpty());
    assertEquals(2, progress.grownHero().armourPieceCount());
  }

  @Test
  void sostituisceIlPezzoIndossatoSoloSeQuelloATerraDifendeDiPiu() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4));
    Spoils spoils = new Spoils(List.of(), List.of(piece(Armour.CHESTPLATE, 7)));

    HeroProgress progress = brain.progressAfterVictory(hero, spoils);

    assertEquals(List.of(new ArmourUpgrade(chestplate(4), piece(Armour.CHESTPLATE, 7))), progress.armourUpgrades());
    assertTrue(progress.newPieces().isEmpty(), "lo slot era già coperto: è un rimpiazzo, non una novità");
    assertEquals(1, progress.grownHero().armourPieceCount());
    assertEquals(7, progress.grownHero().pieceCovering(Armour.CHESTPLATE).orElseThrow().defense());
  }

  @Test
  void ignoraIlPezzoCheDifendeMenoDiQuelloIndossato() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(6));
    Spoils spoils = new Spoils(List.of(), List.of(piece(Armour.CHESTPLATE, 2)));

    HeroProgress progress = brain.progressAfterVictory(hero, spoils);

    assertTrue(progress.armourUpgrades().isEmpty());
    assertTrue(progress.newPieces().isEmpty());
    assertEquals(6, progress.grownHero().pieceCovering(Armour.CHESTPLATE).orElseThrow().defense());
  }

  @Test
  void fraDuePezziDelloStessoSlotPrendeSoloIlMigliore() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4));
    Spoils spoils = new Spoils(List.of(), List.of(piece(Armour.BOOTS, 2), piece(Armour.BOOTS, 5)));

    HeroProgress progress = brain.progressAfterVictory(hero, spoils);

    assertEquals(1, progress.newPieces().size(), "due paia di stivali non si indossano entrambi");
    assertEquals(5, progress.newPieces().getFirst().defense());
  }

  @Test
  void distribuisceEsattamenteTrePuntiSulleCaratteristicheDelPersonaggio() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4));
    int pointsBefore = hero.totalCharacteristicPoints();

    HeroProgress progress = brain.progressAfterVictory(hero, new Spoils(List.of(), List.of()));

    int distributed = progress.characteristicGains().stream().mapToInt(CharacteristicGain::points).sum();
    assertEquals(POINTS_PER_VICTORY, distributed);
    assertEquals(pointsBefore + POINTS_PER_VICTORY, progress.grownHero().totalCharacteristicPoints());
    assertFalse(progress.characteristicGains().isEmpty());
  }

  @Test
  void laSchedaCresciutaRestaLoStessoPersonaggio() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4));
    CharacterResult before = hero.character();

    CharacterResult after = brain.progressAfterVictory(hero, new Spoils(List.of(), List.of()))
        .grownHero()
        .character();

    assertEquals(before.name(), after.name());
    assertEquals(before.race(), after.race());
    assertEquals(before.characterClass(), after.characterClass());
    assertEquals(before.characteristics().size(), after.characteristics().size());
  }

  @Test
  void senzaBottinoCresceComunqueInCaratteristiche() {
    Hero hero = heroWith(sword(5, Rarity.COMMON), chestplate(4));

    HeroProgress progress = brain.progressAfterVictory(hero, new Spoils(List.of(), List.of()));

    assertTrue(progress.weaponSwap().isEmpty());
    assertTrue(progress.newPieces().isEmpty());
    assertTrue(progress.armourUpgrades().isEmpty());
    assertEquals(POINTS_PER_VICTORY,
        progress.characteristicGains().stream().mapToInt(CharacteristicGain::points).sum());
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
}
