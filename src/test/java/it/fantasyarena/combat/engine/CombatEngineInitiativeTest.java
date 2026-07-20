package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceRoller;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.factory.FighterFactory;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.IntrinsicRatings;
import it.fantasyarena.combat.result.CombatOutcome;
import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.result.InitiativeOverride;
import it.fantasyarena.combat.result.InitiativeReport;
import it.fantasyarena.combat.result.TurnLogEntry;
import it.fantasyarena.combat.testsupport.CombatFixtures;
import it.fantasyarena.combat.testsupport.StubDiceRoller;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.CharacterClass;
import it.fantasytoolkitcore.core.model.Characteristic;
import it.fantasytoolkitcore.core.model.Race;

/**
 * DoD 1 (nessuno swap cieco, prossimo attore dal ricalcolo dell'iniziativa), DoD 9 (la schivata
 * ruba il tempo e resetta la catena dell'avversario, la parata no) e terminazione garantita con
 * entrambi i combattenti esausti, esercitati end-to-end su {@link CombatEngine} con
 * {@link StubDiceRoller} al posto del toolkit.
 */
class CombatEngineInitiativeTest {

  /**
   * Verifica end-to-end, su un duello reale (dadi veri, combattenti generati dal toolkit), che
   * chi il report d'iniziativa indica come {@code chosenName} sia sempre chi agisce davvero nel
   * turno, e che senza override il vincitore per punteggio coincida con chi agisce.
   */
  @RepeatedTest(20)
  void ilFighterSceltoDallIniziativaESempreQuelloCheAgisceNelTurno() {
    CombatSettings settings = CombatSettings.defaults();
    FighterFactory factory = FighterFactory.withDefaultRatings(settings);
    FighterFactory.Duelists duelists = factory.createMatchedSwordWarriors();

    CombatEngine engine = CombatFixtures.buildEngine(new DiceRoller(), settings);
    CombatResult result = engine.fight(duelists.first(), duelists.second(), CombatContext.empty());

    assertFalse(result.log().isEmpty());
    for (TurnLogEntry entry : result.log()) {
      InitiativeReport initiative = entry.initiative();
      assertNotNull(initiative);
      assertTrue(entry.description().startsWith(initiative.chosenName()),
          "Turno " + entry.turnNumber() + ": '" + entry.description() + "' non inizia con '"
              + initiative.chosenName() + "'");
      if (initiative.override() == InitiativeOverride.NONE) {
        assertEquals(initiative.scoreWinnerName(), initiative.chosenName());
      }
    }
  }

  /**
   * DoD 1 — con un vantaggio di Agilita' che domina il jitter, lo stesso combattente resta
   * l'attaccante turno dopo turno: nessuno swap cieco residuo (che alternerebbe rigidamente).
   */
  @Test
  void noBlindSwap_dominantInitiativeKeepsAttacking() {
    Fighter dominant = CombatFixtures.createFighter("Dominante", 20, 15, 10, 300, 5, 15, 0);
    Fighter weak = CombatFixtures.createFighter("Debole", 20, 10, 10, 300, 5, 15, 0);
    CombatSettings settings = withMaxTurns(4);

    // 2 jitter per il primo attore, poi per ogni turno: 1 tiro d'attacco (mancato garantito,
    // hitChance 0.85 < 18/20 normalizzato) + 2 jitter di fine turno. Il jitter e' sempre nel
    // caso peggiore per il Dominante (1 contro 6 del Debole): il vantaggio di Agilita' (+5)
    // deve comunque dominare.
    List<DiceThrow> sequence = new ArrayList<>();
    sequence.add(new DiceThrow(1, 6));
    sequence.add(new DiceThrow(6, 6));
    for (int turn = 0; turn < 4; turn++) {
      sequence.add(new DiceThrow(18, 20));
      sequence.add(new DiceThrow(1, 6));
      sequence.add(new DiceThrow(6, 6));
    }
    StubDiceRoller diceRoller = new StubDiceRoller(sequence);
    CombatEngine engine = CombatFixtures.buildEngine(diceRoller, settings);

    CombatResult result = engine.fight(dominant, weak, CombatContext.empty());

    assertEquals(4, result.log().size());
    for (TurnLogEntry entry : result.log()) {
      assertTrue(entry.description().startsWith("Dominante attacca"),
          "il Dominante deve restare l'attaccante in ogni turno, senza alternanza rigida");
    }
    assertEquals(4, dominant.state().consecutiveInitiativeWins(), "la catena prosegue turno dopo turno");
    assertEquals(0, weak.state().consecutiveInitiativeWins(), "il Debole non attacca mai");
  }

  /**
   * DoD 9 — una schivata riuscita ruba il tempo: il difensore diventa deterministicamente il
   * prossimo attaccante (anche se non domina la formula), avvia la propria catena e resetta
   * quella dell'avversario.
   */
  @Test
  void dodgeStealsTime_resetsOpponentChain() {
    Fighter initial = CombatFixtures.createFighter("Iniziale", 15, 10, 10, 5, 5, 15, 0);
    Fighter dodger = CombatFixtures.createFighter("Schivatore", 15, 10, 10, 5, 5, 15, 0);
    CombatSettings settings = withMaxTurns(2);

    List<DiceThrow> sequence = List.of(
        new DiceThrow(6, 6), new DiceThrow(1, 6),   // jitter primo attore: Iniziale vince
        new DiceThrow(1, 20), new DiceThrow(1, 20), new DiceThrow(50, 100), // turno1: colpo, schivata
        // la schivata ruba il tempo (override): il test a punteggio non viene eseguito, quindi
        // nessun jitter e' consumato a fine turno1 (Parte 4 della SPEC cronaca-duello).
        new DiceThrow(16, 20),                      // turno2: Schivatore attacca, manca il colpo
        new DiceThrow(1, 6), new DiceThrow(1, 6));  // jitter di fine turno2
    StubDiceRoller diceRoller = new StubDiceRoller(sequence);
    CombatEngine engine = CombatFixtures.buildEngine(diceRoller, settings);

    CombatResult result = engine.fight(initial, dodger, CombatContext.empty());

    assertEquals(2, result.log().size());
    assertTrue(result.log().get(0).description().contains("schivato"), "il primo turno deve risolversi in una schivata");
    assertTrue(result.log().get(1).description().startsWith("Schivatore attacca"),
        "la schivata ruba il tempo: nel turno successivo attacca lo schivatore, non l'attaccante originario");
    assertEquals(0, initial.state().consecutiveInitiativeWins(), "chi perde l'iniziativa per furto del tempo azzera la catena");
    assertEquals(1, dodger.state().consecutiveInitiativeWins(), "lo schivatore avvia la propria catena rubando il tempo");
  }

  /**
   * DoD 9 — a differenza della schivata, una parata riuscita non ruba il tempo: il prossimo
   * attaccante resta deciso dalla formula normale, non da un override deterministico.
   */
  @Test
  void parry_doesNotStealTime_attackerContinuesByFormula() {
    Fighter dominant = CombatFixtures.createFighter("Dominante", 20, 15, 10, 300, 5, 15, 0);
    Fighter defender = CombatFixtures.createFighter("Debole", 20, 10, 30, 300, 5, 15, 30);
    CombatSettings settings = withMaxTurns(2);

    List<DiceThrow> sequence = List.of(
        new DiceThrow(1, 6), new DiceThrow(6, 6),    // jitter primo attore (caso peggiore per il Dominante)
        new DiceThrow(1, 20), new DiceThrow(3, 20), new DiceThrow(50, 100), // turno1: colpo, parata
        new DiceThrow(1, 6), new DiceThrow(6, 6),    // jitter di fine turno1 (nessun override: la formula decide)
        new DiceThrow(18, 20),                       // turno2: il Dominante attacca ancora, manca il colpo
        new DiceThrow(1, 6), new DiceThrow(6, 6));   // jitter di fine turno2
    StubDiceRoller diceRoller = new StubDiceRoller(sequence);
    CombatEngine engine = CombatFixtures.buildEngine(diceRoller, settings);

    CombatResult result = engine.fight(dominant, defender, CombatContext.empty());

    assertEquals(2, result.log().size());
    assertTrue(result.log().get(0).description().contains("parato"), "il primo turno deve risolversi in una parata");
    assertTrue(result.log().get(1).description().startsWith("Dominante attacca"),
        "la parata non ruba il tempo: l'attaccante resta lo stesso, deciso dalla formula");
    assertEquals(2, dominant.state().consecutiveInitiativeWins(), "la catena del Dominante prosegue: nessun furto del tempo");
    assertEquals(0, defender.state().consecutiveInitiativeWins(), "il difensore non ha rubato l'iniziativa parando");
  }

  /**
   * Caso limite: entrambi i combattenti esausti (Stamina massima sotto il costo base
   * dell'attacco) riposano per l'intera durata, senza mai attaccare. Il tetto {@code maxTurns}
   * garantisce comunque la terminazione, senza loop infiniti.
   */
  @Test
  void bothExhausted_terminatesWithinMaxTurns_withoutInfiniteLoop() {
    Fighter first = buildExhaustedFighter("EsaustoUno");
    Fighter second = buildExhaustedFighter("EsaustoDue");
    CombatSettings settings = withMaxTurns(5);

    List<DiceThrow> sequence = List.of(
        new DiceThrow(3, 6), new DiceThrow(3, 6),   // jitter primo attore
        new DiceThrow(3, 6), new DiceThrow(3, 6),   // jitter fine turno 1
        new DiceThrow(3, 6), new DiceThrow(3, 6),   // jitter fine turno 2
        new DiceThrow(3, 6), new DiceThrow(3, 6),   // jitter fine turno 3
        new DiceThrow(3, 6), new DiceThrow(3, 6),   // jitter fine turno 4
        new DiceThrow(3, 6), new DiceThrow(3, 6));  // jitter fine turno 5
    StubDiceRoller diceRoller = new StubDiceRoller(sequence);
    CombatEngine engine = CombatFixtures.buildEngine(diceRoller, settings);

    CombatResult result = engine.fight(first, second, CombatContext.empty());

    assertEquals(5, result.rounds(), "il tetto di turni resta la garanzia di terminazione");
    assertEquals(5, result.log().size());
    assertEquals(CombatOutcome.DRAW, result.outcome(), "nessun attacco e' mai avvenuto: pareggio a Salute invariata");
    for (TurnLogEntry entry : result.log()) {
      assertTrue(entry.description().contains("riposa"), "senza Stamina sufficiente si riposa sempre, mai a debito");
    }
    assertEquals(first.ratings().maxHealth(), first.state().currentHealth());
    assertEquals(second.ratings().maxHealth(), second.state().currentHealth());
    assertTrue(first.state().currentStamina() <= 5 && second.state().currentStamina() <= 5,
        "la Stamina resta sempre entro il pool massimo, mai presa in prestito");
  }

  /**
   * Il colpo potente e' reso strutturalmente non pagabile: questi test verificano dinamiche di
   * iniziativa/schivata/parata con sequenze di dadi scriptate, non il colpo potente, e vanno
   * preservate identiche a oggi (nessun jitter di decisione consumato).
   */
  private static CombatSettings withMaxTurns(int maxTurns) {
    CombatSettings defaults = CombatFixtures.withPowerStrikeUnaffordable(CombatSettings.defaults());
    return new CombatSettings(defaults.ratingWeights(), defaults.momentumWeights(), defaults.staminaWeights(),
        defaults.chanceWeights(), defaults.initiativeWeights(), defaults.chronicleWeights(),
        defaults.powerStrikeWeights(), maxTurns);
  }

  /**
   * Combattente con {@code maxStamina} (5) volutamente sotto il costo base dell'attacco (6):
   * non potra' mai attaccare, qualunque sia il recupero (limitato dal cap del pool). Costruito
   * direttamente (bypassando la derivazione di {@code maxStamina} da {@code RatingWeights}) per
   * isolare il caso limite dal resto della pipeline di Rating.
   */
  private static Fighter buildExhaustedFighter(String name) {
    List<CharacterCharacteristic> characteristics = List.of(
        new CharacterCharacteristic(Characteristic.STRENGTH, 10),
        new CharacterCharacteristic(Characteristic.AGILITY, 10),
        new CharacterCharacteristic(Characteristic.RESISTANCE, 10),
        new CharacterCharacteristic(Characteristic.STAMINA, 10),
        new CharacterCharacteristic(Characteristic.LUCK, 10),
        new CharacterCharacteristic(Characteristic.INTELLIGENCE, 10),
        new CharacterCharacteristic(Characteristic.CHARISMA, 10));
    CharacterResult character = new CharacterResult(Race.HUMAN, CharacterClass.WARRIOR, name, characteristics);
    WeaponResult weapon = CombatFixtures.createSword(0);
    ArmourResult armour = CombatFixtures.createChestplate(0);
    IntrinsicRatings ratings = new IntrinsicRatings(10.0, 10.0, 100, 5);
    return new Fighter(character, weapon, armour, null, ratings);
  }
}
