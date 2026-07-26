package it.fantasyarena.combat.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.engine.CombatEngine;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.testsupport.CombatFixtures;
import it.fantasyarena.combat.testsupport.RecordingStubDiceRoller;

/**
 * Il test piu' importante della Fase 1b: sullo stesso {@link CombatSettings} e la stessa sequenza
 * scriptata di dadi, {@link CombatEngine#fight} (duello 1v1 storico) e
 * {@link BattleEngine#fight} su {@link BattleSetup#duel} devono produrre esattamente lo stesso
 * esito, lo stesso numero di round, la stessa sequenza di descrizioni di turno e, soprattutto, la
 * stessa traccia di consumo dei dadi elemento per elemento: la prova che il motore N-ario e' un
 * puro sovrainsieme del duello binario, non una riscrittura con comportamento diverso.
 */
class BattleEngineDuelEquivalenceTest {

  @Test
  void colpoSempreASegno_conKoRapido_produceLaStessaSequenza() {
    CombatSettings settings = CombatSettings.defaults();
    List<DiceThrow> sequence = guaranteedHitSequence(settings.maxTurns());

    // Stamina bassa (5): salute massima contenuta, il primo colpo garantito abbatte l'avversario.
    assertDuelEquivalence(sequence, settings, 5);
  }

  @Test
  void colpoSempreMancato_finoAlCapDeiTurni_produceLaStessaSequenza() {
    CombatSettings settings = withMaxTurns(6);
    List<DiceThrow> sequence = alwaysMissSequence(settings.maxTurns());

    // Stamina ampia (200): nessun riposo imprevisto nei molti round esercitati fino al cap.
    assertDuelEquivalence(sequence, settings, 200);
  }

  @Test
  void unaSchivata_eserticaDodgeStealAZeroDadi_produceLaStessaSequenza() {
    CombatSettings settings = withMaxTurns(2);
    List<DiceThrow> sequence = List.of(
        new DiceThrow(6, 6), new DiceThrow(1, 6),   // jitter primo attore: il primo combattente vince
        new DiceThrow(1, 20), new DiceThrow(1, 20), new DiceThrow(50, 100), // turno1: colpo, schivata
        // la schivata ruba il tempo (override): nessun jitter consumato a fine turno1.
        new DiceThrow(16, 20),                      // turno2: lo schivatore attacca, manca il colpo
        new DiceThrow(1, 6), new DiceThrow(1, 6));  // jitter di fine turno2
    assertDuelEquivalence(sequence, settings, 5);
  }

  /**
   * Sequenza truccata che garantisce sempre un colpo a segno, mai una schivata/parata e una
   * varianza neutra sul danno: il colpo abbatte l'avversario al primo turno.
   */
  private static List<DiceThrow> guaranteedHitSequence(int turns) {
    List<DiceThrow> sequence = new ArrayList<>();
    sequence.add(new DiceThrow(1, 6));
    sequence.add(new DiceThrow(6, 6));
    for (int i = 0; i < turns; i++) {
      sequence.add(new DiceThrow(1, 20));
      sequence.add(new DiceThrow(20, 20));
      sequence.add(new DiceThrow(50, 100));
    }
    return sequence;
  }

  /**
   * Sequenza truccata che garantisce sempre un colpo mancato: nessuno dei due combattenti viene
   * mai colpito, quindi lo scontro prosegue fino al tetto di turni/round.
   */
  private static List<DiceThrow> alwaysMissSequence(int turns) {
    List<DiceThrow> sequence = new ArrayList<>();
    sequence.add(new DiceThrow(3, 6));
    sequence.add(new DiceThrow(3, 6));
    for (int i = 0; i < turns; i++) {
      sequence.add(new DiceThrow(19, 20));
      sequence.add(new DiceThrow(3, 6));
      sequence.add(new DiceThrow(3, 6));
    }
    return sequence;
  }

  private static void assertDuelEquivalence(List<DiceThrow> sequence, CombatSettings settings,
      int staminaCharacteristic) {
    CombatSettings settingsWithoutPowerStrike = CombatFixtures.withPowerStrikeUnaffordable(settings);

    RecordingStubDiceRoller combatEngineDiceRoller = new RecordingStubDiceRoller(sequence);
    Fighter combatEngineFirst = buildFighter("Guerriero A", staminaCharacteristic);
    Fighter combatEngineSecond = buildFighter("Guerriero B", staminaCharacteristic);
    CombatEngine combatEngine = CombatFixtures.buildEngine(combatEngineDiceRoller, settingsWithoutPowerStrike);
    CombatResult combatResult = combatEngine.fight(combatEngineFirst, combatEngineSecond, CombatContext.empty());

    RecordingStubDiceRoller battleEngineDiceRoller = new RecordingStubDiceRoller(sequence);
    Fighter battleEngineFirst = buildFighter("Guerriero A", staminaCharacteristic);
    Fighter battleEngineSecond = buildFighter("Guerriero B", staminaCharacteristic);
    BattleEngine battleEngine = CombatFixtures.buildBattleEngine(battleEngineDiceRoller, settingsWithoutPowerStrike);
    BattleResult battleResult =
        battleEngine.fight(BattleSetup.duel(battleEngineFirst, battleEngineSecond), CombatContext.empty());

    // Guardia di non-vacuita': senza di essa il confronto delle tracce passerebbe anche se
    // entrambi i motori non avessero tirato un solo dado, rendendo il test verde per niente.
    assertFalse(combatEngineDiceRoller.trace().isEmpty(), "lo scenario deve consumare almeno un dado");
    assertFalse(descriptionsOf(combatResult).isEmpty(), "lo scenario deve giocare almeno un turno");

    assertEquals(combatResult.outcome(), battleResult.outcome());
    assertEquals(combatResult.rounds(), battleResult.rounds());
    assertEquals(descriptionsOf(combatResult), descriptionsOf(battleResult));
    assertEquals(combatEngineDiceRoller.trace(), battleEngineDiceRoller.trace());
  }

  private static Fighter buildFighter(String name, int staminaCharacteristic) {
    return CombatFixtures.createFighter(name, 30, 10, 5, staminaCharacteristic, 5, 20, 0);
  }

  private static List<String> descriptionsOf(CombatResult result) {
    return result.log().stream().map(entry -> entry.description()).toList();
  }

  private static List<String> descriptionsOf(BattleResult result) {
    List<String> descriptions = new ArrayList<>();
    for (RoundLogEntry round : result.roundLog()) {
      for (EngagementTurn turn : round.turns()) {
        descriptions.add(turn.turn().description());
      }
    }
    return descriptions;
  }

  private static CombatSettings withMaxTurns(int maxTurns) {
    CombatSettings defaults = CombatSettings.defaults();
    return new CombatSettings(defaults.ratingWeights(), defaults.momentumWeights(), defaults.staminaWeights(),
        defaults.chanceWeights(), defaults.initiativeWeights(), defaults.chronicleWeights(),
        defaults.powerStrikeWeights(), defaults.scoreWeights(), maxTurns);
  }
}
