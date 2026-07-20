package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.IntrinsicRatings;
import it.fantasyarena.combat.testsupport.CombatFixtures;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.CharacterClass;
import it.fantasytoolkitcore.core.model.Characteristic;
import it.fantasytoolkitcore.core.model.Race;

/**
 * DoD 2, 3, 9 (lato resolver) — purezza dell'{@link InitiativeResolver}, uso del rapporto
 * Stamina/maxStamina (non il valore assoluto), dominanza di Agilità/Intelligenza a Stamina
 * piena con jitter che incide solo sui pareggi, e override deterministico da schivata.
 */
class InitiativeResolverTest {

  private static final CombatSettings SETTINGS = CombatSettings.defaults();

  @Test
  void resolveNextAttacker_sameInput_sameOutcome() {
    InitiativeResolver resolver = new InitiativeResolver(SETTINGS);
    Fighter attacker = buildFighter("Attaccante", 15, 10, 40, 30);
    Fighter defender = buildFighter("Difensore", 8, 10, 40, 30);
    DiceThrow attackerJitter = new DiceThrow(3, 6);
    DiceThrow defenderJitter = new DiceThrow(4, 6);

    Fighter firstResult =
        resolver.resolveNextAttacker(attacker, defender, false, attackerJitter, defenderJitter);
    Fighter secondResult =
        resolver.resolveNextAttacker(attacker, defender, false, attackerJitter, defenderJitter);

    assertSame(firstResult, secondResult, "stesso input deve produrre sempre lo stesso esito");
  }

  @Test
  void initiativeUsesStaminaRatio_notAbsoluteValue() {
    InitiativeResolver resolver = new InitiativeResolver(SETTINGS);
    Fighter opponent = buildFighter("Avversario", 10, 10, 100, 100);

    // Stesso rapporto (0.5) con pool massimi molto diversi: deve produrre lo stesso esito
    // relativo all'avversario, a parità di Agilità/Intelligenza/jitter.
    Fighter lowMaxStamina = buildFighter("PoolPiccolo", 10, 10, 10, 5);
    Fighter highMaxStamina = buildFighter("PoolGrande", 10, 10, 200, 100);

    DiceThrow jitterA = new DiceThrow(3, 6);
    DiceThrow jitterB = new DiceThrow(3, 6);

    Fighter winnerLow = resolver.resolveFirstMover(lowMaxStamina, opponent, jitterA, jitterB);
    Fighter winnerHigh = resolver.resolveFirstMover(highMaxStamina, opponent, jitterA, jitterB);

    assertEquals(winnerLow == lowMaxStamina, winnerHigh == highMaxStamina,
        "a parita' di rapporto Stamina/maxStamina l'esito relativo all'avversario deve essere lo stesso");
  }

  @Test
  void fullStamina_higherAgilityAndIntelligence_dominatesOverJitter() {
    InitiativeResolver resolver = new InitiativeResolver(SETTINGS);
    Fighter strong = buildFighter("Forte", 30, 30, 40, 40);
    Fighter weak = buildFighter("Debole", 5, 5, 40, 40);

    // Il jitter massimo per il piu' debole contro il minimo per il piu' forte non deve
    // ribaltare un vantaggio di Agilita'/Intelligenza cosi' ampio.
    DiceThrow strongJitter = new DiceThrow(1, 6);
    DiceThrow weakJitter = new DiceThrow(6, 6);

    Fighter winner = resolver.resolveFirstMover(strong, weak, strongJitter, weakJitter);

    assertSame(strong, winner, "il vantaggio di Agilita'/Intelligenza deve dominare sul jitter");
  }

  @Test
  void fullStamina_equalStats_jitterBreaksTie() {
    InitiativeResolver resolver = new InitiativeResolver(SETTINGS);
    Fighter first = buildFighter("Prima", 10, 10, 40, 40);
    Fighter second = buildFighter("Seconda", 10, 10, 40, 40);

    Fighter winnerWithHigherJitter =
        resolver.resolveFirstMover(first, second, new DiceThrow(2, 6), new DiceThrow(6, 6));
    assertSame(second, winnerWithHigherJitter, "a parita' di stat il jitter piu' alto deve vincere");

    Fighter winnerWithEqualJitter =
        resolver.resolveFirstMover(first, second, new DiceThrow(3, 6), new DiceThrow(3, 6));
    assertSame(first, winnerWithEqualJitter, "a parita' assoluta il tie-break e' deterministico e stabile");
  }

  @Test
  void dodge_overridesFormula_regardlessOfScore() {
    InitiativeResolver resolver = new InitiativeResolver(SETTINGS);
    Fighter dominantAttacker = buildFighter("Dominante", 30, 30, 40, 40);
    Fighter dodgingDefender = buildFighter("Schivatore", 1, 1, 40, 1);

    Fighter nextAttacker = resolver.resolveNextAttacker(
        dominantAttacker, dodgingDefender, true, new DiceThrow(6, 6), new DiceThrow(1, 6));

    assertSame(dodgingDefender, nextAttacker,
        "la schivata ruba il tempo: il difensore diventa il prossimo attaccante anche con score peggiore");
  }

  /**
   * Costruisce un {@link Fighter} minimale con Agilita', Intelligenza e Stamina controllate
   * direttamente (bypassando la derivazione di {@code maxStamina} da {@code RatingWeights}),
   * utile per isolare la formula d'iniziativa dal resto della pipeline di Rating.
   */
  private static Fighter buildFighter(String name, int agility, int intelligence, int maxStamina,
      int currentStamina) {
    List<CharacterCharacteristic> characteristics = List.of(
        new CharacterCharacteristic(Characteristic.STRENGTH, 10),
        new CharacterCharacteristic(Characteristic.AGILITY, agility),
        new CharacterCharacteristic(Characteristic.RESISTANCE, 10),
        new CharacterCharacteristic(Characteristic.STAMINA, 10),
        new CharacterCharacteristic(Characteristic.LUCK, 10),
        new CharacterCharacteristic(Characteristic.INTELLIGENCE, intelligence),
        new CharacterCharacteristic(Characteristic.CHARISMA, 10));
    CharacterResult character = new CharacterResult(Race.HUMAN, CharacterClass.WARRIOR, name, characteristics);
    WeaponResult weapon = CombatFixtures.createSword(0);
    ArmourResult armour = CombatFixtures.createChestplate(0);
    IntrinsicRatings ratings = new IntrinsicRatings(0.0, 0.0, 100, maxStamina);

    Fighter fighter = new Fighter(character, weapon, armour, null, ratings);
    fighter.state().consumeStamina(maxStamina - currentStamina);
    return fighter;
  }
}
