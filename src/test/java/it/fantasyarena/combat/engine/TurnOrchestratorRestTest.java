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
import it.fantasyarena.combat.result.TurnLogEntry;
import it.fantasyarena.combat.result.TurnResult;
import it.fantasyarena.combat.testsupport.CombatFixtures;
import it.fantasyarena.combat.testsupport.StubDiceRoller;

/**
 * Meccanica di riposo e regole dure a Stamina 0: sotto soglia l'attaccante riposa invece di
 * attaccare (nessun tiro di dado, nessun danno all'avversario), un difensore esausto subisce
 * il colpo pieno senza poter difendersi, il gate di affordabilita' dell'attacco (DoD 6) forza
 * il riposo anche quando il costo effettivo (con malus di catena) non e' pagabile, e il riposo
 * azzera sempre la catena di iniziativa (DoD 4).
 */
class TurnOrchestratorRestTest {

  @Test
  void attackerBelowThreshold_restsInsteadOfAttacking() {
    CombatSettings settings = CombatFixtures.withPowerStrikeUnaffordable(CombatSettings.defaults());
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 30, 10, 5, 5, 5, 20, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 30, 10, 5, 5, 5, 20, 0);
    attacker.state().consumeStamina(attacker.ratings().maxStamina() - 5);

    StubDiceRoller diceRoller = new StubDiceRoller(List.of());

    TurnLogEntry turn = playSingleTurn(diceRoller, settings, attacker, defender).logEntry();

    assertTrue(attacker.state().currentStamina() > 5, "il riposo deve recuperare Stamina rispetto a prima del turno");
    assertEquals(defender.ratings().maxHealth(), defender.state().currentHealth(),
        "il riposo e' neutro: l'avversario non subisce nulla");
    assertTrue(turn.description().contains("riposa"), "la description deve segnalare il riposo");
  }

  @Test
  void exhaustedDefender_takesFullHitWithoutDefending() {
    CombatSettings settings = CombatFixtures.withPowerStrikeUnaffordable(CombatSettings.defaults());
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 30, 10, 5, 5, 5, 20, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 30, 10, 5, 5, 5, 20, 0);
    attacker.state().winInitiative();
    defender.state().consumeStamina(defender.ratings().maxStamina());

    List<DiceThrow> scriptedThrows = List.of(new DiceThrow(1, 20), new DiceThrow(50, 100));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnLogEntry turn = playSingleTurn(diceRoller, settings, attacker, defender).logEntry();

    assertTrue(defender.state().currentHealth() < defender.ratings().maxHealth(),
        "un difensore esausto non puo' difendersi e subisce il colpo pieno");
    assertTrue(turn.description().contains("difensore esausto"),
        "la description deve segnalare che il difensore era esausto");
  }

  /**
   * DoD 6 — il costo effettivo dell'attacco cresce con la catena fino al cap (12 con i
   * default): a Stamina 9 su un pool massimo di 22 (9 e' sopra la soglia generica di riposo,
   * 40% di 22 = 8.8, quindi {@code shouldRest} da solo direbbe di attaccare) l'attacco non e'
   * comunque pagabile e l'attaccante riposa.
   */
  @Test
  void unaffordableAttack_restsInsteadOfAttacking() {
    CombatSettings settings = CombatFixtures.withPowerStrikeUnaffordable(CombatSettings.defaults());
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 30, 10, 5, 4, 5, 20, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 30, 10, 5, 5, 5, 20, 0);

    // Quarto attacco consecutivo: il malus di catena e' al cap (effectiveAttackCost = 12).
    attacker.state().winInitiative();
    attacker.state().winInitiative();
    attacker.state().winInitiative();
    attacker.state().winInitiative();
    assertEquals(4, attacker.state().consecutiveInitiativeWins());
    int maxStamina = attacker.ratings().maxStamina();
    int currentStamina = 9;
    attacker.state().consumeStamina(maxStamina - currentStamina);
    assertEquals(currentStamina, attacker.state().currentStamina());
    assertFalse(new StaminaRules(settings).shouldRest(attacker.state().currentStamina(), maxStamina),
        "precondizione: la soglia generica di riposo da sola non basterebbe a far riposare");

    StubDiceRoller diceRoller = new StubDiceRoller(List.of());

    TurnLogEntry turn = playSingleTurn(diceRoller, settings, attacker, defender).logEntry();

    assertTrue(turn.description().contains("riposa"),
        "il costo effettivo (con malus di catena) non e' pagabile: l'attaccante deve riposare");
    assertEquals(0, attacker.state().consecutiveInitiativeWins(),
        "il riposo azzera la catena di iniziativa anche se l'attaccante la conservava");
    assertEquals(defender.ratings().maxHealth(), defender.state().currentHealth(),
        "il riposo e' neutro: l'avversario non subisce nulla");
  }

  private static TurnResult playSingleTurn(StubDiceRoller diceRoller, CombatSettings settings, Fighter attacker,
      Fighter defender) {
    TurnOrchestrator turnOrchestrator = new TurnOrchestrator(diceRoller, new HitResolver(settings),
        new DefenseResolver(settings), new DamageCalculator(settings, new MomentumRules(settings),
            new StaminaRules(settings)), new MomentumRules(settings), new StaminaRules(settings), settings);
    return turnOrchestrator.playTurn(1, attacker, defender, CombatContext.empty());
  }
}
