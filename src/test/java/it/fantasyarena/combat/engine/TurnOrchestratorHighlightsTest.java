package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.config.CombatSettings.ChronicleWeights;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.TurnHighlight;
import it.fantasyarena.combat.result.TurnLogEntry;
import it.fantasyarena.combat.testsupport.CombatFixtures;
import it.fantasyarena.combat.testsupport.StubDiceRoller;

/**
 * DoD 1-4 della SPEC cronaca-duello: gli highlight tipizzati tracciati da
 * {@link TurnOrchestrator#playTurn} sulla {@link TurnLogEntry}, con i dadi pilotati da
 * {@link StubDiceRoller} per forzare deterministicamente critico, 20 naturale, colpo di grazia
 * e colpo pesante.
 */
class TurnOrchestratorHighlightsTest {

  @Test
  void colpoCritico_emetteCriticalNonPerfect() {
    CombatSettings settings = CombatFixtures.withPowerStrikeUnaffordable(CombatSettings.defaults());
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 10, 5, 5, 5, 5, 5, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 10, 5, 10, 5, 5, 5, 5);
    attacker.state().winInitiative();

    // tiro d'attacco basso (non massimo naturale): sotto la soglia di critico del fortunale
    // dell'attaccante, quindi critico garantito senza essere un 20 naturale.
    List<DiceThrow> scriptedThrows =
        List.of(new DiceThrow(1, 20), new DiceThrow(20, 20), new DiceThrow(50, 100));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnLogEntry entry = playSingleTurn(diceRoller, settings, attacker, defender);

    assertTrue(entry.highlights().contains(TurnHighlight.CRITICAL), "il colpo deve essere tracciato come critico");
    assertFalse(entry.highlights().contains(TurnHighlight.PERFECT_HIT),
        "un critico non dovuto al 20 naturale non e' un colpo perfetto");
  }

  @Test
  void ventiNaturale_emettePerfectHit_conCriticalCompresente() {
    CombatSettings settings = CombatFixtures.withPowerStrikeUnaffordable(CombatSettings.defaults());
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 10, 5, 5, 5, 5, 5, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 10, 5, 10, 5, 5, 5, 5);
    attacker.state().winInitiative();

    // 20 naturale al tiro d'attacco: colpo e critico garantiti per la SPEC.
    List<DiceThrow> scriptedThrows =
        List.of(new DiceThrow(20, 20), new DiceThrow(20, 20), new DiceThrow(50, 100));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnLogEntry entry = playSingleTurn(diceRoller, settings, attacker, defender);

    assertTrue(entry.highlights().contains(TurnHighlight.PERFECT_HIT), "il 20 naturale deve emettere il colpo perfetto");
    assertTrue(entry.highlights().contains(TurnHighlight.CRITICAL),
        "il critico resta comunque tracciato come compresente al colpo perfetto");
  }

  @Test
  void colpoDiGrazia_emetteKnockout() {
    CombatSettings settings = CombatFixtures.withPowerStrikeUnaffordable(CombatSettings.defaults());
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 10, 5, 5, 5, 5, 5, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 10, 5, 10, 5, 5, 5, 5);
    attacker.state().winInitiative();
    // il difensore parte da 1 vita: qualunque colpo pieno lo abbatte.
    defender.state().applyDamage(defender.ratings().maxHealth() - 1);

    List<DiceThrow> scriptedThrows =
        List.of(new DiceThrow(1, 20), new DiceThrow(20, 20), new DiceThrow(50, 100));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnLogEntry entry = playSingleTurn(diceRoller, settings, attacker, defender);

    assertTrue(defender.isDefeated(), "precondizione: il difensore deve risultare sconfitto dopo il colpo");
    assertTrue(entry.highlights().contains(TurnHighlight.KNOCKOUT), "il colpo di grazia deve emettere KNOCKOUT");
  }

  @Test
  void dannoOltreSoglia_emetteHeavyBlow_soglieConfigurabili() {
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 30, 10, 5, 5, 5, 5, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 10, 10, 5, 5, 5, 10, 0);
    attacker.state().winInitiative();

    // tiro d'attacco a segno ma non critico ne' massimo naturale, difesa senza schivata ne'
    // parata, varianza neutra: isola il solo colpo pesante (danno atteso 43 su vita massima 55).
    List<DiceThrow> scriptedThrows =
        List.of(new DiceThrow(15, 20), new DiceThrow(20, 20), new DiceThrow(50, 100));

    CombatSettings defaultThreshold = CombatFixtures.withPowerStrikeUnaffordable(CombatSettings.defaults());
    TurnLogEntry withDefaultThreshold =
        playSingleTurn(new StubDiceRoller(scriptedThrows), defaultThreshold, attacker, defender);

    assertTrue(withDefaultThreshold.highlights().contains(TurnHighlight.HEAVY_BLOW),
        "con la soglia di default (25%) il danno deve superarla ed emettere HEAVY_BLOW");
    assertFalse(withDefaultThreshold.highlights().contains(TurnHighlight.CRITICAL));
    assertFalse(withDefaultThreshold.highlights().contains(TurnHighlight.PERFECT_HIT));
    assertFalse(withDefaultThreshold.highlights().contains(TurnHighlight.KNOCKOUT));

    // ricreo i combattenti freschi: la stessa sequenza di dadi deve riprodurre lo stesso danno.
    Fighter attackerAgain = CombatFixtures.createFighter("Attaccante", 30, 10, 5, 5, 5, 5, 0);
    Fighter defenderAgain = CombatFixtures.createFighter("Difensore", 10, 10, 5, 5, 5, 10, 0);
    attackerAgain.state().winInitiative();

    CombatSettings raisedThreshold = withChronicleWeights(defaultThreshold, new ChronicleWeights(0.9));
    TurnLogEntry withRaisedThreshold =
        playSingleTurn(new StubDiceRoller(scriptedThrows), raisedThreshold, attackerAgain, defenderAgain);

    assertFalse(withRaisedThreshold.highlights().contains(TurnHighlight.HEAVY_BLOW),
        "alzando la soglia (90%) lo stesso danno non deve piu' emettere HEAVY_BLOW");
  }

  private static CombatSettings withChronicleWeights(CombatSettings settings, ChronicleWeights chronicleWeights) {
    return new CombatSettings(settings.ratingWeights(), settings.momentumWeights(), settings.staminaWeights(),
        settings.chanceWeights(), settings.initiativeWeights(), chronicleWeights, settings.powerStrikeWeights(),
        settings.maxTurns());
  }

  private static TurnLogEntry playSingleTurn(StubDiceRoller diceRoller, CombatSettings settings, Fighter attacker,
      Fighter defender) {
    TurnOrchestrator turnOrchestrator = new TurnOrchestrator(diceRoller, new HitResolver(settings),
        new DefenseResolver(settings), new DamageCalculator(settings, new MomentumRules(settings),
            new StaminaRules(settings)), new MomentumRules(settings), new StaminaRules(settings), settings);
    return turnOrchestrator.playTurn(1, attacker, defender, CombatContext.empty()).logEntry();
  }
}
