package it.fantasyarena.combat.chronicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.hero.Hero;
import it.fantasyarena.combat.hero.HeroProgress;
import it.fantasyarena.combat.hero.HeroProgress.ArmourUpgrade;
import it.fantasyarena.combat.hero.HeroProgress.CharacteristicGain;
import it.fantasyarena.combat.hero.HeroProgress.JewelUpgrade;
import it.fantasyarena.combat.hero.HeroProgress.NewJewel;
import it.fantasyarena.combat.hero.Loot;
import it.fantasyarena.combat.hero.LootFate;
import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.testsupport.CombatFixtures;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.jewelgenerator.result.JewelResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.Characteristic;
import it.fantasytoolkitcore.core.model.Jewel;
import it.fantasytoolkitcore.core.model.Race;
import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.Weapon;

/**
 * Il solo punto che traduce {@link Fighter}, {@link Hero} e {@link HeroProgress} nei record della
 * cronaca: verifica che la fotografia del combattente porti i dati immutabili attesi e non la vita
 * corrente, che solo la fotografia del protagonista porti i gioielli, e che la procedura di fine
 * scontro fotografi l'oggetto trovato e quello lasciato sul destino già risolto da
 * {@link HeroProgress#lootFate()}.
 */
class ChronicleMapperTest {

  private final ChronicleMapper mapper = new ChronicleMapper();

  @Test
  void fotografaIlCombattenteConIDatiImmutabiliENonLaVitaCorrente() {
    Fighter fighter = CombatFixtures.createFighter("Protagonista", 10, 11, 12, 13, 14, 9, 4);
    fighter.state().applyDamage(5);

    CombatantSnapshot snapshot = mapper.snapshotCombatant(fighter, 2, 1);

    assertEquals(2, snapshot.rosterIndex());
    assertEquals(1, snapshot.teamIndex());
    assertEquals("Protagonista", snapshot.name());
    assertEquals(Race.HUMAN, snapshot.race());
    assertEquals(fighter.character().characterClass(), snapshot.characterClass());
    assertEquals(fighter.character().characteristics(), snapshot.characteristics());
    assertEquals(new ItemSnapshot(ItemKind.WEAPON, Weapon.SWORD.name(), Rarity.COMMON, 9), snapshot.weapon());
    assertEquals(List.of(new ItemSnapshot(ItemKind.ARMOUR, Armour.CHESTPLATE.name(), Rarity.COMMON, 4)),
        snapshot.armourPieces());
    assertEquals(fighter.ratings().maxHealth(), snapshot.maxHealth(),
        "la vita massima non cambia con la ferita appena inflitta");
    assertEquals(fighter.ratings().maxStamina(), snapshot.maxStamina());
    assertEquals(fighter.ratings().offensiveRating(), snapshot.offensiveRating());
    assertEquals(fighter.ratings().defensiveRating(), snapshot.defensiveRating());
  }

  @Test
  void fotografaLEroeConIGioielliCheIlCombattenteNonPorta() {
    JewelResult ring = new JewelResult(Jewel.RING, Rarity.RARE, List.of(), List.of());
    Hero hero = heroWithBasicGear().wearing(ring);

    HeroSnapshot snapshot = mapper.snapshotHero(hero);

    assertEquals(List.of(new ItemSnapshot(ItemKind.JEWEL, Jewel.RING.name(), Rarity.RARE, null)),
        snapshot.jewels(), "la fotografia del protagonista porta i gioielli, quella del combattente non li ha");
    assertTrue(hero.jewels().stream().anyMatch(jewel -> jewel.jewel() == Jewel.RING),
        "il gioiello indossato deve comparire nella scheda sorgente");
  }

  @Test
  void fotografaLaProceduraDiFineScontroConLOggettoSostituitoEQuelloLasciato() {
    Hero hero = heroWithBasicGear();
    ArmourResult dropped = new ArmourResult(Armour.CHESTPLATE, Rarity.COMMON, List.of(), List.of(), 4);
    ArmourResult found = new ArmourResult(Armour.CHESTPLATE, Rarity.RARE, List.of(), List.of(), 9);
    HeroProgress progress = new HeroProgress(hero, Loot.ofArmourPiece(found), null, null,
        new ArmourUpgrade(dropped, found), null, null,
        List.of(new CharacteristicGain(Characteristic.STRENGTH, 3)));

    ProgressChronicle chronicle = mapper.snapshotProgress(progress);

    assertEquals(LootFate.ARMOUR_REPLACED, progress.lootFate());
    assertEquals(progress.lootFate(), chronicle.fate(),
        "il destino nella cronaca deve essere quello già risolto da HeroProgress, non uno rideriviato");
    assertEquals(new ItemSnapshot(ItemKind.ARMOUR, Armour.CHESTPLATE.name(), Rarity.RARE, 9), chronicle.found());
    assertEquals(new ItemSnapshot(ItemKind.ARMOUR, Armour.CHESTPLATE.name(), Rarity.COMMON, 4), chronicle.dropped());
    assertNull(chronicle.jewelBonusPoints(), "un'armatura sostituita non porta un bonus di gioiello");
  }

  @Test
  void fotografaIlBonusDelGioielloIndossatoSuUnTipoPrimaScoperto() {
    Hero hero = heroWithBasicGear();
    JewelResult found = new JewelResult(Jewel.RING, Rarity.RARE, List.of(), List.of());
    HeroProgress progress = new HeroProgress(hero, Loot.ofJewel(found), null, null, null,
        new NewJewel(found, 5), null, List.of(new CharacteristicGain(Characteristic.STRENGTH, 3)));

    ProgressChronicle chronicle = mapper.snapshotProgress(progress);

    assertEquals(LootFate.JEWEL_WORN_ON_EMPTY_TYPE, progress.lootFate());
    assertEquals(5, chronicle.jewelBonusPoints(), "il bonus del gioiello indossato deve comparire nella cronaca");
  }

  @Test
  void fotografaIlBonusDelGioielloCheSostituisceUnoMenoRaro() {
    Hero hero = heroWithBasicGear();
    JewelResult dropped = new JewelResult(Jewel.RING, Rarity.RARE, List.of(), List.of());
    JewelResult found = new JewelResult(Jewel.RING, Rarity.EPIC, List.of(), List.of());
    HeroProgress progress = new HeroProgress(hero, Loot.ofJewel(found), null, null, null, null,
        new JewelUpgrade(dropped, found, 8), List.of(new CharacteristicGain(Characteristic.STRENGTH, 3)));

    ProgressChronicle chronicle = mapper.snapshotProgress(progress);

    assertEquals(LootFate.JEWEL_REPLACED, progress.lootFate());
    assertEquals(8, chronicle.jewelBonusPoints(), "il bonus del gioiello che sostituisce deve comparire nella cronaca");
  }

  @Test
  void fotografaLaProceduraDiFineScontroSenzaOggettoLasciatoQuandoLoTrovatoESiScartato() {
    Hero hero = new Hero(CombatFixtures.createWarrior("Protagonista", 10, 11, 12, 13, 14),
        new WeaponResult(Weapon.SWORD, Rarity.COMMON, List.of(), List.of(), 9),
        List.of(new ArmourResult(Armour.CHESTPLATE, Rarity.COMMON, List.of(), List.of(), 4)));
    WeaponResult found = new WeaponResult(Weapon.SWORD, Rarity.COMMON, List.of(), List.of(), 3);
    HeroProgress progress = new HeroProgress(hero, Loot.ofWeapon(found), null, null, null, null, null,
        List.of(new CharacteristicGain(Characteristic.AGILITY, 3)));

    ProgressChronicle chronicle = mapper.snapshotProgress(progress);

    assertEquals(LootFate.WEAPON_DISCARDED, progress.lootFate());
    assertEquals(progress.lootFate(), chronicle.fate(),
        "il destino nella cronaca deve essere quello già risolto da HeroProgress, non uno rideriviato");
    assertEquals(new ItemSnapshot(ItemKind.WEAPON, Weapon.SWORD.name(), Rarity.COMMON, 3), chronicle.found());
    assertNull(chronicle.dropped(), "un'arma scartata non lascia niente al suo posto");
  }

  @Test
  void fotografaIlGioielloScartatoSenzaBonusPercheNonEStatoIndossato() {
    JewelResult worn = new JewelResult(Jewel.RING, Rarity.RARE, List.of(), List.of());
    Hero hero = heroWithBasicGear().wearing(worn);
    JewelResult found = new JewelResult(Jewel.RING, Rarity.COMMON, List.of(), List.of());
    HeroProgress progress = new HeroProgress(hero, Loot.ofJewel(found), null, null, null, null, null,
        List.of(new CharacteristicGain(Characteristic.AGILITY, 3)));

    ProgressChronicle chronicle = mapper.snapshotProgress(progress);

    assertEquals(LootFate.JEWEL_DISCARDED, progress.lootFate());
    assertNull(chronicle.jewelBonusPoints(),
        "un gioiello trovato e scartato non è stato indossato, quindi non vale un bonus");
  }

  /**
   * La scheda di base condivisa da quasi tutti i test di questa classe: spada e corazza comuni,
   * nessun gioiello. Ogni test aggiunge sopra questa base solo ciò che lo differenzia (il loot
   * trovato, il gioiello già indossato, il destino da verificare).
   */
  private Hero heroWithBasicGear() {
    return new Hero(CombatFixtures.createWarrior("Protagonista", 10, 11, 12, 13, 14),
        new WeaponResult(Weapon.SWORD, Rarity.COMMON, List.of(), List.of(), 5),
        List.of(new ArmourResult(Armour.CHESTPLATE, Rarity.COMMON, List.of(), List.of(), 4)));
  }
}
