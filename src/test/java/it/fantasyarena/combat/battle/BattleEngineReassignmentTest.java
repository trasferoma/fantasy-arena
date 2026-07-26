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
 * 2v2 in cui uno scontro si chiude con un KO al round 1: il vincitore libero (A0) si unisce
 * all'altro scontro attivo (E1) a fine round, e al round 2 gioca in un unico scontro a 3
 * partecipanti, con un test a punteggio vero (3 jitter, nell'ordine ultimo attore poi il resto).
 */
class BattleEngineReassignmentTest {

  @Test
  void vincitoreLibero_siUnisceAllAltroScontro_eGiocaAlRoundSuccessivo() {
    CombatSettings settings = CombatFixtures.withPowerStrikeUnaffordable(withMaxTurns(2));

    // A0/B0: stesso profilo della schermaglia con KO garantito al primo colpo (vedi
    // BattleEngineDuelEquivalenceTest): stamina bassa, salute contenuta.
    Fighter a0 = CombatFixtures.createFighter("A0", 30, 10, 5, 5, 5, 20, 0);
    Fighter b0 = CombatFixtures.createFighter("B0", 30, 10, 5, 5, 5, 20, 0);
    // A1/B1: stamina ampia, restano semplicemente a mancarsi per tutta la durata del test.
    Fighter a1 = CombatFixtures.createFighter("A1", 30, 10, 5, 200, 5, 20, 0);
    Fighter b1 = CombatFixtures.createFighter("B1", 30, 10, 5, 200, 5, 20, 0);
    BattleSetup setup = BattleSetup.of(List.of(List.of(a0, a1), List.of(b0, b1)));

    List<DiceThrow> sequence = List.of(
        // Round 1, E0 = [A0, B0]: A0 vince l'iniziativa (jitter 6 contro 1), colpo garantito a
        // segno, nessuna schivata/parata, varianza neutra: B0 muore.
        new DiceThrow(6, 6), new DiceThrow(1, 6),
        new DiceThrow(1, 20), new DiceThrow(20, 20), new DiceThrow(50, 100),
        // Round 1, E1 = [A1, B1]: pareggio di jitter (vince A1 per tie-break), colpo mancato.
        new DiceThrow(3, 6), new DiceThrow(3, 6),
        new DiceThrow(19, 20),
        // Round 2, E1 ora a 3 (A1, B1, A0 appena riassegnato): test a punteggio vero, 3 jitter
        // in pareggio (vince A1, primo in initiativeOrder in quanto ultimo attore), colpo mancato.
        new DiceThrow(3, 6), new DiceThrow(3, 6), new DiceThrow(3, 6),
        new DiceThrow(19, 20));
    RecordingStubDiceRoller diceRoller = new RecordingStubDiceRoller(sequence);
    BattleEngine battleEngine = CombatFixtures.buildBattleEngine(diceRoller, settings);

    BattleResult result = battleEngine.fight(setup, CombatContext.empty());

    assertEquals(2, result.rounds());
    assertTrue(b0.isDefeated(), "precondizione: B0 deve morire al round 1");

    RoundLogEntry round1 = result.roundLog().get(0);
    assertEquals(2, round1.turns().size(), "round 1: entrambi gli scontri erano ancora attivi");
    assertEquals(List.of("A0, libero, si unisce allo scontro 1."), round1.events());

    RoundLogEntry round2 = result.roundLog().get(1);
    assertEquals(1, round2.turns().size(), "round 2: solo E1 resta attivo, E0 e' concluso");
    assertEquals(1, round2.turns().get(0).engagementId());
    assertTrue(round2.turns().get(0).attackerName().equals("A1") || round2.turns().get(0).attackerName().equals("B1")
            || round2.turns().get(0).attackerName().equals("A0"),
        "l'attore del round 2 deve essere uno dei tre partecipanti attuali dello scontro E1");

    List<String> expectedTrace = List.of(
        "roll(6)", "roll(6)", "d20", "d20", "d100",
        "roll(6)", "roll(6)", "d20",
        "roll(6)", "roll(6)", "roll(6)", "d20");
    assertEquals(expectedTrace, diceRoller.trace());
  }

  private static CombatSettings withMaxTurns(int maxTurns) {
    CombatSettings defaults = CombatSettings.defaults();
    return new CombatSettings(defaults.ratingWeights(), defaults.momentumWeights(), defaults.staminaWeights(),
        defaults.chanceWeights(), defaults.initiativeWeights(), defaults.chronicleWeights(),
        defaults.powerStrikeWeights(), defaults.scoreWeights(), maxTurns);
  }
}
