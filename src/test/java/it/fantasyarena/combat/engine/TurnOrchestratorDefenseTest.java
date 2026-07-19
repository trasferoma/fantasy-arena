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
 * DoD 8 — la difesa riuscita (qui una schivata forzata dallo stub) azzera il danno subito e
 * aggiorna il Momentum secondo le regole, esercitata sul {@link TurnOrchestrator} reale.
 */
class TurnOrchestratorDefenseTest {

  @Test
  void successfulDefense_reducesDamageAndUpdatesMomentum() {
    CombatSettings settings = CombatSettings.defaults();
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 30, 10, 5, 5, 5, 20, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 30, 10, 5, 5, 5, 20, 0);

    // attacco garantito a segno, difesa garantita in schivata (dodgeChance di base è 0.10),
    // varianza neutra: la sequenza forza deterministicamente l'esito DODGED.
    List<DiceThrow> scriptedThrows =
        List.of(new DiceThrow(1, 20), new DiceThrow(1, 20), new DiceThrow(50, 100));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnLogEntry turn = playSingleTurn(diceRoller, settings, attacker, defender);

    assertEquals(defender.ratings().maxHealth(), defender.state().currentHealth(),
        "una schivata piena deve azzerare il danno subito");
    assertEquals(settings.momentumWeights().gainOnDodgeSuccess(), defender.state().momentum(),
        "la schivata riuscita deve aumentare il Momentum del difensore secondo le regole");
    assertEquals(defender.ratings().maxStamina() - settings.staminaWeights().dodgeCost(), defender.state().currentStamina(),
        "la schivata consuma il costo Stamina della schivata, non quello dell'impatto");
    assertEquals(attacker.ratings().maxStamina() - settings.staminaWeights().attackCost(), attacker.state().currentStamina(),
        "l'attacco consuma il costo Stamina dell'azione di attacco");
    assertEquals(1, turn.turnNumber());
  }

  @Test
  void fullHitTaken_consumesStaminaProportionallyToDamage() {
    CombatSettings settings = CombatSettings.defaults();
    // Rating offensivi/difensivi contenuti apposta: il tiro d'attacco (1,20) genera sempre un
    // critico (critChance minima 0.05 = normalized di 1/20), qui il danno risultante resta
    // comunque ben sotto Salute e Stamina massime del difensore, cosi' l'assert puo' derivare
    // il danno reale dal delta di vita senza incappare nel floor a 0 di nessuno dei due pool.
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 10, 5, 5, 5, 5, 5, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 10, 5, 10, 5, 5, 5, 5);

    // attacco garantito a segno, difesa che non schiva ne' para (dodge/parry chance basse a
    // fronte di un tiro alto), varianza neutra: la sequenza forza l'esito HIT_TAKEN.
    List<DiceThrow> scriptedThrows =
        List.of(new DiceThrow(1, 20), new DiceThrow(20, 20), new DiceThrow(50, 100));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    playSingleTurn(diceRoller, settings, attacker, defender);

    assertTrue(defender.state().currentHealth() < defender.ratings().maxHealth(),
        "precondizione: il difensore deve aver subito danno");

    StaminaRules staminaRules = new StaminaRules(settings);
    int damage = defender.ratings().maxHealth() - defender.state().currentHealth();
    int expectedStaminaLoss = staminaRules.impactStaminaLoss(damage);
    assertEquals(defender.ratings().maxStamina() - expectedStaminaLoss, defender.state().currentStamina(),
        "chi incassa un colpo pieno perde stamina proporzionale al danno");
  }

  private static TurnLogEntry playSingleTurn(StubDiceRoller diceRoller, CombatSettings settings, Fighter attacker,
      Fighter defender) {
    TurnOrchestrator turnOrchestrator = new TurnOrchestrator(diceRoller, new HitResolver(settings),
        new DefenseResolver(settings), new DamageCalculator(settings, new MomentumRules(settings),
            new StaminaRules(settings)), new MomentumRules(settings), new StaminaRules(settings));
    return turnOrchestrator.playTurn(1, attacker, defender, CombatContext.empty());
  }
}
