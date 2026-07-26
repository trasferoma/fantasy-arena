package it.fantasyarena.combat.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.testsupport.CombatFixtures;
import it.fantasyarena.combat.testsupport.RecordingStubDiceRoller;

/**
 * 2v2: due scontri attivi (E0 = [A0, B0], E1 = [A1, B1] per {@link PairwiseEngagementPlanner}),
 * uno scambio per scontro nello stesso round, nell'ordine di id. Verifica sulla {@code trace()}
 * che i dadi si consumino esattamente scontro dopo scontro: prima tutti quelli di E0, poi tutti
 * quelli di E1.
 */
class BattleEngine2v2Test {

  @Test
  void unRound_giocaEntrambiGliScontriNellOrdineDiId_conDadiSequenziali() {
    CombatSettings settings = CombatFixtures.withPowerStrikeUnaffordable(withMaxTurns(1));

    Fighter a0 = buildFighter("A0");
    Fighter a1 = buildFighter("A1");
    Fighter b0 = buildFighter("B0");
    Fighter b1 = buildFighter("B1");
    BattleSetup setup = BattleSetup.of(List.of(List.of(a0, a1), List.of(b0, b1)));

    // Ogni scontro consuma: 2 jitter d'iniziativa (nessun ultimo attore ancora) + 1 tiro
    // d'attacco mancato garantito (19/20, agilita' pari: normalized 0.95 > hitChance 0.75).
    List<DiceThrow> sequence = List.of(
        new DiceThrow(3, 6), new DiceThrow(3, 6), new DiceThrow(19, 20),  // scontro E0
        new DiceThrow(3, 6), new DiceThrow(3, 6), new DiceThrow(19, 20)); // scontro E1
    RecordingStubDiceRoller diceRoller = new RecordingStubDiceRoller(sequence);
    BattleEngine battleEngine = CombatFixtures.buildBattleEngine(diceRoller, settings);

    BattleResult result = battleEngine.fight(setup, CombatContext.empty());

    assertEquals(1, result.roundLog().size());
    RoundLogEntry round = result.roundLog().get(0);
    assertEquals(2, round.turns().size(), "un round con due scontri attivi gioca due scambi");
    assertEquals(0, round.turns().get(0).engagementId());
    assertEquals(1, round.turns().get(1).engagementId());
    assertEquals(List.of("roll(6)", "roll(6)", "d20", "roll(6)", "roll(6)", "d20"), diceRoller.trace());
  }

  private static Fighter buildFighter(String name) {
    return CombatFixtures.createFighter(name, 30, 10, 5, 200, 5, 20, 0);
  }

  private static CombatSettings withMaxTurns(int maxTurns) {
    CombatSettings defaults = CombatSettings.defaults();
    return new CombatSettings(defaults.ratingWeights(), defaults.momentumWeights(), defaults.staminaWeights(),
        defaults.chanceWeights(), defaults.initiativeWeights(), defaults.chronicleWeights(),
        defaults.powerStrikeWeights(), defaults.scoreWeights(), maxTurns);
  }
}
