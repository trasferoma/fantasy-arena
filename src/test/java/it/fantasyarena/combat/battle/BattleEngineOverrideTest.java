package it.fantasyarena.combat.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.testsupport.CombatFixtures;
import it.fantasyarena.combat.testsupport.RecordingStubDiceRoller;

/**
 * Uno scontro a 3 (2v1: A0, A1 contro B0, riuniti in un solo {@link Engagement} dal
 * {@link PairwiseEngagementPlanner}): {@link it.fantasyarena.combat.result.InitiativeOverride#DODGE_STEAL}
 * fa agire lo schivatore consumando zero dadi d'iniziativa, poi
 * {@link it.fantasyarena.combat.result.InitiativeOverride#REST_YIELD} esclude chi ha riposato e fa
 * un test a punteggio vero fra gli altri due (2 jitter), verificato sulla {@code trace()}.
 */
class BattleEngineOverrideTest {

  @Test
  void dodgeStealPoiRestYield_consumaIDadiAttesi() {
    CombatSettings settings = CombatFixtures.withPowerStrikeUnaffordable(withMaxTurns(3));

    Fighter a0 = CombatFixtures.createFighter("A0", 30, 10, 5, 15, 5, 20, 0);
    Fighter a1 = CombatFixtures.createFighter("A1", 30, 10, 5, 15, 5, 20, 0);
    // B0 parte con Stamina volutamente bassa (13 il massimo): dopo aver pagato il costo della
    // schivata al round 1 e ricevuto il recupero passivo dalla scatola, scende sotto la soglia
    // di riposo per il round 2.
    Fighter b0 = CombatFixtures.createFighter("B0", 30, 10, 5, 1, 5, 20, 0);
    b0.state().consumeStamina(b0.ratings().maxStamina() - 6);
    BattleSetup setup = BattleSetup.of(List.of(List.of(a0, a1), List.of(b0)));

    List<DiceThrow> sequence = List.of(
        // Round 1: test a punteggio fra i tre (nessun ultimo attore ancora), A0 vince
        // nettamente per Stamina piena e jitter piu' alto. Colpo garantito a segno, schivata
        // garantita di B0 (dodgeChance base 0.10): DODGE_STEAL.
        new DiceThrow(6, 6), new DiceThrow(1, 6), new DiceThrow(1, 6),
        new DiceThrow(1, 20), new DiceThrow(1, 20), new DiceThrow(50, 100)
        // Round 2 (DODGE_STEAL): B0 agisce rubando il tempo, zero dadi d'iniziativa. B0 e' sotto
        // soglia di riposo: riposa, zero dadi anche per l'azione (nessuna voce da aggiungere).
        ,
        // Round 3 (REST_YIELD): B0 e' escluso, test vero fra A0 e A1 (2 jitter). A0 vince
        // (jitter piu' alto compensa il lieve svantaggio di Stamina lasciato dal round 1).
        // Colpo mancato (hitChance 0.75 con agilita' pari, 19/20 normalizzato 0.95 sopra soglia).
        new DiceThrow(6, 6), new DiceThrow(1, 6),
        new DiceThrow(19, 20));
    RecordingStubDiceRoller diceRoller = new RecordingStubDiceRoller(sequence);
    BattleEngine battleEngine = CombatFixtures.buildBattleEngine(diceRoller, settings);

    BattleResult result = battleEngine.fight(setup, CombatContext.empty());

    assertEquals(3, result.rounds());

    // Ordine di roster (BattleSetup.of(List.of(List.of(a0, a1), List.of(b0)))): A0=0, A1=1, B0=2.
    int a0Index = 0;
    int b0Index = 2;

    RoundLogEntry round1 = result.roundLog().get(0);
    assertEquals(1, round1.turns().size());
    assertEquals(a0Index, round1.turns().get(0).attackerIndex());
    assertTrue(round1.turns().get(0).turn().description().contains("schivato"),
        "round 1 deve risolversi in una schivata di B0");

    RoundLogEntry round2 = result.roundLog().get(1);
    assertEquals(1, round2.turns().size());
    assertEquals(b0Index, round2.turns().get(0).attackerIndex(),
        "DODGE_STEAL: chi ha schivato (B0) deve essere l'attore del round successivo");
    assertTrue(round2.turns().get(0).turn().description().contains("riposa"),
        "B0 e' sotto soglia di riposo: deve riposare invece di attaccare");

    RoundLogEntry round3 = result.roundLog().get(2);
    assertEquals(1, round3.turns().size());
    assertEquals(a0Index, round3.turns().get(0).attackerIndex(),
        "REST_YIELD: il test a punteggio deve escludere B0 e svolgersi solo fra A0 e A1");

    List<String> expectedTrace = List.of(
        "roll(6)", "roll(6)", "roll(6)", "d20", "d20", "d100",
        "roll(6)", "roll(6)", "d20");
    assertEquals(expectedTrace, diceRoller.trace(),
        "round 1: 3 jitter (test a 3); round 2: zero dadi (DODGE_STEAL, poi riposo); "
            + "round 3: 2 jitter (REST_YIELD fra i restanti due) + 1 tiro d'attacco");
  }

  private static CombatSettings withMaxTurns(int maxTurns) {
    CombatSettings defaults = CombatSettings.defaults();
    return new CombatSettings(defaults.ratingWeights(), defaults.momentumWeights(), defaults.staminaWeights(),
        defaults.chanceWeights(), defaults.initiativeWeights(), defaults.chronicleWeights(),
        defaults.powerStrikeWeights(), defaults.scoreWeights(), maxTurns);
  }
}
