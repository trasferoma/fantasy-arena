package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.TurnLogEntry;
import it.fantasyarena.combat.testsupport.CombatFixtures;
import it.fantasyarena.combat.testsupport.StubDiceRoller;

/**
 * Meccanica di riposo e regole dure a Stamina 0: sotto soglia l'attaccante riposa invece di
 * attaccare (nessun tiro di dado, nessun danno all'avversario), e un difensore esausto subisce
 * il colpo pieno senza poter schivare o parare.
 */
class TurnOrchestratorRestTest {

  @Test
  void attackerBelowThreshold_restsInsteadOfAttacking() {
    CombatSettings settings = CombatSettings.defaults();
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 30, 10, 5, 5, 5, 20, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 30, 10, 5, 5, 5, 20, 0);
    attacker.state().consumeStamina(attacker.ratings().maxStamina() - 5);

    StubDiceRoller diceRoller = new StubDiceRoller(List.of());

    TurnLogEntry turn = playSingleTurn(diceRoller, settings, attacker, defender);

    assertTrue(attacker.state().currentStamina() > 5, "il riposo deve recuperare Stamina rispetto a prima del turno");
    assertEquals(defender.ratings().maxHealth(), defender.state().currentHealth(),
        "il riposo e' neutro: l'avversario non subisce nulla");
    assertTrue(turn.description().contains("riposa"), "la description deve segnalare il riposo");
  }

  @Test
  void exhaustedDefender_takesFullHitWithoutDefending() {
    CombatSettings settings = CombatSettings.defaults();
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 30, 10, 5, 5, 5, 20, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 30, 10, 5, 5, 5, 20, 0);
    defender.state().consumeStamina(defender.ratings().maxStamina());

    List<DiceThrow> scriptedThrows = List.of(new DiceThrow(1, 20), new DiceThrow(50, 100));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnLogEntry turn = playSingleTurn(diceRoller, settings, attacker, defender);

    assertTrue(defender.state().currentHealth() < defender.ratings().maxHealth(),
        "un difensore esausto non puo' difendersi e subisce il colpo pieno");
    assertTrue(turn.description().contains("difensore esausto"),
        "la description deve segnalare che il difensore era esausto");
  }

  private static TurnLogEntry playSingleTurn(StubDiceRoller diceRoller, CombatSettings settings, Fighter attacker,
      Fighter defender) {
    TurnOrchestrator turnOrchestrator = new TurnOrchestrator(diceRoller, new HitResolver(settings),
        new DefenseResolver(settings), new DamageCalculator(settings, new MomentumRules(settings),
            new StaminaRules(settings)), new MomentumRules(settings), new StaminaRules(settings));
    return turnOrchestrator.playTurn(1, attacker, defender, CombatContext.empty());
  }
}
