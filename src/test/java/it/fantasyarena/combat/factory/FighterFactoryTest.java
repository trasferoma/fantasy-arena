package it.fantasyarena.combat.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.hero.EquipmentBonus;
import it.fantasyarena.combat.hero.Hero;
import it.fantasyarena.combat.hero.Loot;
import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.model.Fighter;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.buffdebuffgenerator.result.BuffElement;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkit.jewelgenerator.result.JewelResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.ClassBonusTable;
import it.fantasytoolkitcore.core.model.RaceBonusTable;
import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.RarityTable;

/**
 * Verifica {@link FighterFactory#createChallengers(int, int, RarityTable, RarityTable, int)}
 * (numerosità, ripartizione del monte di squadra fra gli sfidanti a parti uguali col resto ai
 * primi, pavimento sotto il quale il monte è rifiutato, nomi tutti distinti, rarità dell'arma
 * sempre dentro la tabella dell'arma ricevuta e rarità dei pezzi sempre dentro la tabella
 * dell'armatura ricevuta — non più identica per tutti, perché l'estrazione è per singolo oggetto) e
 * {@link FighterFactory#rollLoot(RarityTable)} (rarità sempre contenuta in quelle dichiarate nella
 * tabella, tutti i tipi estraibili su più generazioni).
 */
class FighterFactoryTest {

  private static final int MANY_ROLLS = 200;

  private static final int FIVE_CHALLENGERS_SQUAD_POINTS = 40;

  private static final int FIVE_CHALLENGERS_ARMOUR_PIECE_COUNT = 2;

  private static final int PROTAGONIST_TOTAL_CHARACTERISTIC_POINTS = 15;

  private static final int SINGLE_ARMOUR_PIECE_COUNT = 1;

  private static final RarityTable RARE_TO_LEGENDARY_TABLE = RarityTable.builder()
      .entry(Rarity.RARE, 50)
      .entry(Rarity.EPIC, 30)
      .entry(Rarity.LEGENDARY, 20)
      .build();

  private static final RarityTable EPIC_ONLY_TABLE = RarityTable.builder()
      .entry(Rarity.EPIC, 100)
      .build();

  private static final RarityTable COMMON_ONLY_TABLE = RarityTable.builder()
      .entry(Rarity.COMMON, 100)
      .build();

  private final FighterFactory factory = FighterFactory.withDefaultRatings(CombatSettings.defaults());

  @Test
  void creaCinqueCombattentiConCinqueNomiDistintiERaritaDentroLeTabelleRicevute() {
    List<Fighter> fighters = factory.createChallengers(5, FIVE_CHALLENGERS_SQUAD_POINTS, RARE_TO_LEGENDARY_TABLE,
        EPIC_ONLY_TABLE, FIVE_CHALLENGERS_ARMOUR_PIECE_COUNT);

    assertEquals(5, fighters.size());

    Set<String> names = fighters.stream().map(Fighter::name).collect(Collectors.toCollection(HashSet::new));
    assertEquals(5, names.size(), "i 5 combattenti devono avere 5 nomi tutti distinti");

    Set<Rarity> allowedWeaponRarities = EnumSet.of(Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY);
    fighters.forEach(fighter -> {
      assertTrue(allowedWeaponRarities.contains(fighter.weapon().rarity()),
          "l'arma deve restare dentro la rarità dichiarata dalla tabella dell'arma");
      assertEquals(FIVE_CHALLENGERS_ARMOUR_PIECE_COUNT, fighter.armourPieces().size(),
          "ogni sfidante indossa il numero di pezzi che la fascia dichiara");
      fighter.armourPieces().forEach(piece -> assertEquals(Rarity.EPIC, piece.rarity(),
          "ogni pezzo d'armatura deve restare dentro la rarità dichiarata dalla tabella dell'armatura"));
    });
  }

  /**
   * Il monte richiesto al generatore è quindici punti, ma {@code totalCharacteristicPoints()} misura
   * il personaggio con i bonus di razza e classe già sommati: si ricava il monte richiesto per
   * sottrazione, come già fa {@link #requestedPointsOf}, per non far dipendere il test dalla razza
   * estratta a caso.
   */
  @Test
  void ilProtagonistaPartePerSempreConRaritaUncommonUnPezzoEQuindiciPunti() {
    Hero hero = factory.createProtagonist();

    int raceBonus = sumRaceBonus(RaceBonusTable.withDefaultBonuses().bonusesFor(hero.character().race()));
    int classBonus = sumClassBonus(
        ClassBonusTable.withDefaultBonuses().bonusesFor(hero.character().characterClass()));
    int requestedPoints = hero.totalCharacteristicPoints() - raceBonus - classBonus;

    assertEquals(Rarity.UNCOMMON, hero.weapon().rarity());
    assertEquals(1, hero.armourPieceCount());
    assertEquals(Rarity.UNCOMMON, hero.armourPieces().getFirst().rarity());
    assertEquals(PROTAGONIST_TOTAL_CHARACTERISTIC_POINTS, requestedPoints);
  }

  /**
   * Il monte punti richiesto dal singolo sfidante è la sua quota del monte di squadra, non la somma
   * finale: i bonus di razza e classe restano attivi per gli sfidanti (a differenza dello specchio,
   * che li disattiva per eguagliare esattamente il protagonista) e sono sempre positivi, quindi la
   * somma reale supera sempre la quota richiesta, mai la eguaglia né la scavalca al ribasso.
   */
  @Test
  void ogniSfidanteRiceveAlmenoIlMontePuntiRichiestoPiuIBonusDiRazzaEClasse() {
    int squadPoints = 50;
    int count = 3;

    List<Fighter> fighters = factory.createChallengers(count, squadPoints, RARE_TO_LEGENDARY_TABLE,
        RARE_TO_LEGENDARY_TABLE, SINGLE_ARMOUR_PIECE_COUNT);
    List<Integer> requestedPointsPerChallenger = partitionSquadPoints(squadPoints, count);

    IntStream.range(0, fighters.size()).forEach(index -> {
      int totalPoints = fighters.get(index).character().characteristics().stream()
          .mapToInt(CharacterCharacteristic::value)
          .sum();
      assertTrue(totalPoints > requestedPointsPerChallenger.get(index),
          "la quota richiesta è la base: i bonus di razza e classe la fanno sempre crescere");
    });
  }

  /**
   * Prova rigorosa (non solo "totalPoints > requestedPoints", già vera anche senza buff) che i
   * buff di arma e armatura sono davvero sommati alle caratteristiche con cui lo sfidante scende
   * in campo: la somma dopo la sola generazione del personaggio è nota per contratto del toolkit
   * ({@code quotaRichiesta + bonusRazza + bonusClasse}), quindi l'eccedenza rispetto a quella somma
   * può venire soltanto dai buff dell'equipaggiamento.
   */
  @Test
  void gliSfidantiScendonoInCampoConLeCaratteristicheEffettiveComprensiveDeiBuffDellEquipaggiamento() {
    int squadPoints = 50;
    int count = 3;

    List<Fighter> fighters = factory.createChallengers(count, squadPoints, RARE_TO_LEGENDARY_TABLE,
        RARE_TO_LEGENDARY_TABLE, SINGLE_ARMOUR_PIECE_COUNT);
    List<Integer> requestedPointsPerChallenger = partitionSquadPoints(squadPoints, count);

    IntStream.range(0, fighters.size()).forEach(index -> {
      Fighter fighter = fighters.get(index);
      int raceBonus = sumRaceBonus(RaceBonusTable.withDefaultBonuses().bonusesFor(fighter.character().race()));
      int classBonus = sumClassBonus(
          ClassBonusTable.withDefaultBonuses().bonusesFor(fighter.character().characterClass()));
      int equipmentBonus = EquipmentBonus.totalValueOf(equipmentBuffsOf(fighter));
      int expectedTotal = requestedPointsPerChallenger.get(index) + raceBonus + classBonus + equipmentBonus;

      int actualTotal = fighter.character().characteristics().stream()
          .mapToInt(CharacterCharacteristic::value)
          .sum();
      assertEquals(expectedTotal, actualTotal,
          "le caratteristiche dello sfidante devono comprendere anche i buff dell'equipaggiamento");
    });
  }

  /**
   * Il monte di squadra si ripartisce fra gli sfidanti a parti uguali, col resto ai primi: la somma
   * delle quote individuali richieste — ricavate per sottrazione dei bonus di razza, classe ed
   * equipaggiamento dalle caratteristiche effettive di ciascuno sfidante realmente generato — deve
   * tornare esattamente al monte di squadra ricevuto da {@code createChallengers}, sia quando la
   * divisione è esatta sia quando non lo è.
   */
  @Test
  void laSommaDeiMontiIndividualiGeneratiEEsattamenteIlMonteDiSquadraRicevuto() {
    assertSquadPointsFullyDistributed(31, 2);
    assertSquadPointsFullyDistributed(50, 3);
  }

  private void assertSquadPointsFullyDistributed(int squadPoints, int count) {
    List<Fighter> fighters = factory.createChallengers(count, squadPoints, RARE_TO_LEGENDARY_TABLE,
        RARE_TO_LEGENDARY_TABLE, SINGLE_ARMOUR_PIECE_COUNT);

    int totalRequestedPoints = fighters.stream().mapToInt(this::requestedPointsOf).sum();
    assertEquals(squadPoints, totalRequestedPoints,
        "la somma delle quote individuali deve coincidere col monte di squadra ricevuto");
  }

  private int requestedPointsOf(Fighter fighter) {
    int raceBonus = sumRaceBonus(RaceBonusTable.withDefaultBonuses().bonusesFor(fighter.character().race()));
    int classBonus = sumClassBonus(
        ClassBonusTable.withDefaultBonuses().bonusesFor(fighter.character().characterClass()));
    int equipmentBonus = EquipmentBonus.totalValueOf(equipmentBuffsOf(fighter));
    int actualTotal = fighter.character().characteristics().stream()
        .mapToInt(CharacterCharacteristic::value)
        .sum();
    return actualTotal - raceBonus - classBonus - equipmentBonus;
  }

  private List<Integer> partitionSquadPoints(int squadCharacteristicPoints, int count) {
    int basePoints = squadCharacteristicPoints / count;
    int remainder = squadCharacteristicPoints % count;
    return IntStream.range(0, count)
        .mapToObj(index -> basePoints + (index < remainder ? 1 : 0))
        .toList();
  }

  private List<BuffElement> equipmentBuffsOf(Fighter fighter) {
    List<BuffElement> buffs = new ArrayList<>(fighter.weapon().buffs());
    fighter.armourPieces().forEach(piece -> buffs.addAll(piece.buffs()));
    return buffs;
  }

  private int sumRaceBonus(List<RaceBonusTable.CharacteristicBonus> bonuses) {
    return bonuses.stream().mapToInt(RaceBonusTable.CharacteristicBonus::value).sum();
  }

  private int sumClassBonus(List<ClassBonusTable.CharacteristicBonus> bonuses) {
    return bonuses.stream().mapToInt(ClassBonusTable.CharacteristicBonus::value).sum();
  }

  @Test
  void rifiutaUnaNumerositaMinoreDiUno() {
    assertThrows(IllegalArgumentException.class,
        () -> factory.createChallengers(0, FIVE_CHALLENGERS_SQUAD_POINTS, RARE_TO_LEGENDARY_TABLE,
            RARE_TO_LEGENDARY_TABLE, SINGLE_ARMOUR_PIECE_COUNT));
  }

  @Test
  void rifiutaUnMonteDiSquadraSottoIlPavimentoDiSettePuntiPerSfidante() {
    int count = 3;
    int belowFloor = FighterFactory.MINIMUM_CHARACTERISTIC_POINTS_PER_CHALLENGER * count - 1;

    assertThrows(IllegalArgumentException.class,
        () -> factory.createChallengers(count, belowFloor, RARE_TO_LEGENDARY_TABLE, RARE_TO_LEGENDARY_TABLE,
            SINGLE_ARMOUR_PIECE_COUNT));
  }

  @Test
  void accettaEsattamenteIlPavimentoDiSettePuntiPerSfidante() {
    int count = 3;
    int exactFloor = FighterFactory.MINIMUM_CHARACTERISTIC_POINTS_PER_CHALLENGER * count;

    List<Fighter> fighters = factory.createChallengers(count, exactFloor, RARE_TO_LEGENDARY_TABLE,
        RARE_TO_LEGENDARY_TABLE, SINGLE_ARMOUR_PIECE_COUNT);

    assertEquals(count, fighters.size());
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
