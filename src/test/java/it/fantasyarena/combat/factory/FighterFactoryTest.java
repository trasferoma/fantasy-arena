package it.fantasyarena.combat.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.hero.Loot;
import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.model.Fighter;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkit.jewelgenerator.result.JewelResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.RarityTable;

/**
 * Verifica {@link FighterFactory#createChallengers(int, int)} (numerosità e monte punti richiesti,
 * nomi tutti distinti, rarità di arma/armatura condivise da tutti, cosi' che nessuno parta
 * avvantaggiato) e {@link FighterFactory#rollLoot(RarityTable)} (rarità sempre contenuta in quelle
 * dichiarate nella tabella, tutti i tipi estraibili su più generazioni).
 */
class FighterFactoryTest {

  private static final int MANY_ROLLS = 200;

  private static final int CHALLENGER_CHARACTERISTIC_POINTS = 15;

  private static final RarityTable RARE_TO_LEGENDARY_TABLE = RarityTable.builder()
      .entry(Rarity.RARE, 50)
      .entry(Rarity.EPIC, 30)
      .entry(Rarity.LEGENDARY, 20)
      .build();

  private static final RarityTable COMMON_ONLY_TABLE = RarityTable.builder()
      .entry(Rarity.COMMON, 100)
      .build();

  private final FighterFactory factory = FighterFactory.withDefaultRatings(CombatSettings.defaults());

  @Test
  void creaCinqueCombattentiConCinqueNomiDistintiEStessaRarita() {
    List<Fighter> fighters = factory.createChallengers(5, CHALLENGER_CHARACTERISTIC_POINTS);

    assertEquals(5, fighters.size());

    Set<String> names = fighters.stream().map(Fighter::name).collect(Collectors.toCollection(HashSet::new));
    assertEquals(5, names.size(), "i 5 combattenti devono avere 5 nomi tutti distinti");

    Set<String> weaponRarities = fighters.stream().map(fighter -> fighter.weapon().rarity().name())
        .collect(Collectors.toCollection(HashSet::new));
    Set<String> armourRarities = fighters.stream()
        .flatMap(fighter -> fighter.armourPieces().stream())
        .map(piece -> piece.rarity().name())
        .collect(Collectors.toCollection(HashSet::new));
    assertEquals(1, weaponRarities.size(), "l'arma deve avere la stessa rarità per tutti");
    assertEquals(1, armourRarities.size(), "l'armatura deve avere la stessa rarità per tutti");
  }

  /**
   * Il monte punti richiesto è la base distribuita dal generatore, non la somma finale: i bonus di
   * razza e classe restano attivi per gli sfidanti (a differenza dello specchio, che li disattiva
   * per eguagliare esattamente il protagonista) e sono sempre positivi, quindi la somma reale supera
   * sempre quella richiesta, mai la eguaglia né la scavalca al ribasso.
   */
  @Test
  void ogniSfidanteRiceveAlmenoIlMontePuntiRichiestoPiuIBonusDiRazzaEClasse() {
    int requestedPoints = 24;

    List<Fighter> fighters = factory.createChallengers(3, requestedPoints);

    fighters.forEach(fighter -> {
      int totalPoints = fighter.character().characteristics().stream()
          .mapToInt(CharacterCharacteristic::value)
          .sum();
      assertTrue(totalPoints > requestedPoints,
          "il monte punti richiesto è la base: i bonus di razza e classe la fanno sempre crescere");
    });
  }

  @Test
  void rifiutaUnaNumerositaMinoreDiUno() {
    assertThrows(IllegalArgumentException.class,
        () -> factory.createChallengers(0, CHALLENGER_CHARACTERISTIC_POINTS));
  }

  @Test
  void rifiutaUnMontePuntiMinoreDiUno() {
    assertThrows(IllegalArgumentException.class, () -> factory.createChallengers(1, 0));
  }

  @Test
  void ilLootRestaSempreDentroLeRaritaDichiarateNellaTabella() {
    Set<Rarity> allowedRarities = EnumSet.of(Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY);

    IntStream.range(0, MANY_ROLLS)
        .mapToObj(roll -> factory.rollLoot(RARE_TO_LEGENDARY_TABLE))
        .forEach(loot -> assertTrue(allowedRarities.contains(rarityOf(loot)),
            "il loot deve restare fra le rarità dichiarate nella tabella"));
  }

  @Test
  void suMolteGenerazioniCompaionoTuttiITipiDiLoot() {
    Set<String> kindsFound = new HashSet<>();

    for (int roll = 0; roll < MANY_ROLLS; roll++) {
      Loot loot = factory.rollLoot(COMMON_ONLY_TABLE);
      loot.weapon().ifPresent(found -> kindsFound.add("WEAPON"));
      loot.armourPiece().ifPresent(found -> kindsFound.add("ARMOUR"));
      loot.jewel().ifPresent(found -> kindsFound.add("JEWEL"));
    }

    assertEquals(Set.of("WEAPON", "ARMOUR", "JEWEL"), kindsFound,
        "su " + MANY_ROLLS + " estrazioni devono comparire tutti e tre i tipi");
  }

  private Rarity rarityOf(Loot loot) {
    return loot.weapon().map(WeaponResult::rarity)
        .or(() -> loot.armourPiece().map(ArmourResult::rarity))
        .or(() -> loot.jewel().map(JewelResult::rarity))
        .orElseThrow();
  }
}
