package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.ActionOutcome;
import it.fantasyarena.combat.result.TurnResult;
import it.fantasyarena.combat.testsupport.CombatFixtures;
import it.fantasyarena.combat.testsupport.StubDiceRoller;

/**
 * Verifica che {@link TurnOrchestrator} valorizzi correttamente {@link ActionOutcome} in
 * {@link it.fantasyarena.combat.result.TurnLogEntry#action()} per ciascun esito di scambio: colpo
 * a segno, mancato, parato, schivato e riposo. Scenari ripresi dagli stessi dadi scriptati di
 * {@link TurnOrchestratorDefenseTest} e {@link TurnOrchestratorRestTest}, qui esercitati per
 * verificare il dato e non la descrizione testuale.
 */
class TurnOrchestratorActionOutcomeTest {

  @Test
  void colpoASegno_valorizzaActionOutcomeHit() {
    CombatSettings settings = CombatFixtures.withPowerStrikeUnaffordable(CombatSettings.defaults());
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 10, 5, 5, 5, 5, 5, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 10, 5, 10, 5, 5, 5, 5);
    attacker.state().winInitiative();

    List<DiceThrow> scriptedThrows =
        List.of(new DiceThrow(1, 20), new DiceThrow(20, 20), new DiceThrow(50, 100));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnResult turn = playSingleTurn(diceRoller, settings, attacker, defender);
    ActionOutcome action = turn.logEntry().action();

    int expectedDamage = defender.ratings().maxHealth() - defender.state().currentHealth();
    assertEquals(ActionOutcome.Kind.HIT, action.kind());
    assertEquals(expectedDamage, action.damage());
    assertTrue(action.critical(), "il tiro d'attacco (1,20) e' anche critico per questo attaccante");
    assertFalse(action.powerStrike());
  }

  @Test
  void colpoMancato_valorizzaActionOutcomeMiss() {
    CombatSettings settings = CombatFixtures.withPowerStrikeUnaffordable(CombatSettings.defaults());
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 30, 10, 5, 5, 5, 20, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 30, 10, 5, 5, 5, 20, 0);
    attacker.state().winInitiative();

    StubDiceRoller diceRoller = new StubDiceRoller(List.of(new DiceThrow(19, 20)));

    TurnResult turn = playSingleTurn(diceRoller, settings, attacker, defender);
    ActionOutcome action = turn.logEntry().action();

    assertEquals(ActionOutcome.Kind.MISS, action.kind());
    assertEquals(0, action.damage());
    assertFalse(action.critical());
    assertFalse(action.powerStrike());
  }

  @Test
  void colpoParato_valorizzaActionOutcomeParried() {
    CombatSettings settings = CombatFixtures.withPowerStrikeUnaffordable(CombatSettings.defaults());
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 30, 10, 5, 5, 5, 20, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 30, 10, 5, 5, 5, 20, 0);
    attacker.state().winInitiative();
    defender.state().consumeStamina(defender.ratings().maxStamina() - 4);

    List<DiceThrow> scriptedThrows =
        List.of(new DiceThrow(1, 20), new DiceThrow(1, 20), new DiceThrow(50, 100));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnResult turn = playSingleTurn(diceRoller, settings, attacker, defender);
    ActionOutcome action = turn.logEntry().action();

    int expectedDamage = defender.ratings().maxHealth() - defender.state().currentHealth();
    assertEquals(ActionOutcome.Kind.PARRIED, action.kind());
    assertEquals(expectedDamage, action.damage());
    assertTrue(expectedDamage > 0, "la parata riduce il danno ma non lo azzera come farebbe la schivata");
  }

  @Test
  void colpoSchivato_valorizzaActionOutcomeDodged() {
    CombatSettings settings = CombatFixtures.withPowerStrikeUnaffordable(CombatSettings.defaults());
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 30, 10, 5, 5, 5, 20, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 30, 10, 5, 5, 5, 20, 0);
    attacker.state().winInitiative();

    List<DiceThrow> scriptedThrows =
        List.of(new DiceThrow(1, 20), new DiceThrow(1, 20), new DiceThrow(50, 100));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnResult turn = playSingleTurn(diceRoller, settings, attacker, defender);
    ActionOutcome action = turn.logEntry().action();

    assertEquals(ActionOutcome.Kind.DODGED, action.kind());
    assertEquals(0, action.damage(), "una schivata piena deve azzerare il danno registrato nell'azione");
  }

  @Test
  void riposo_valorizzaActionOutcomeRest() {
    CombatSettings settings = CombatFixtures.withPowerStrikeUnaffordable(CombatSettings.defaults());
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 30, 10, 5, 5, 5, 20, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 30, 10, 5, 5, 5, 20, 0);
    attacker.state().consumeStamina(attacker.ratings().maxStamina() - 5);
    int staminaBeforeRest = attacker.state().currentStamina();

    StubDiceRoller diceRoller = new StubDiceRoller(List.of());

    TurnResult turn = playSingleTurn(diceRoller, settings, attacker, defender);
    ActionOutcome action = turn.logEntry().action();

    int actualRecovered = attacker.state().currentStamina() - staminaBeforeRest;
    assertEquals(ActionOutcome.Kind.REST, action.kind());
    assertEquals(actualRecovered, action.staminaRecovered());
    assertEquals(0, action.damage());
    assertFalse(action.critical());
    assertFalse(action.powerStrike());
  }

  private static TurnResult playSingleTurn(StubDiceRoller diceRoller, CombatSettings settings, Fighter attacker,
      Fighter defender) {
    TurnOrchestrator turnOrchestrator = new TurnOrchestrator(diceRoller, new HitResolver(settings),
        new DefenseResolver(settings), new DamageCalculator(settings, new MomentumRules(settings),
            new StaminaRules(settings)), new MomentumRules(settings), new StaminaRules(settings), settings);
    return turnOrchestrator.playTurn(1, attacker, defender, CombatContext.empty());
  }
}
