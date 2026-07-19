package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.config.CombatSettings.StaminaWeights;
import it.fantasyarena.combat.model.FighterState;
import it.fantasyarena.combat.model.IntrinsicRatings;

/**
 * DoD 6 — la Stamina non cala per il solo passare del turno, cala solo per azioni/impatto,
 * sotto le soglie scattano le penalità progressive e non scende mai sotto 0.
 */
class StaminaRulesTest {

  private static final double DELTA = 1e-9;

  @Test
  void staminaDropsOnlyOnActionsAndImpact_withPenalties() {
    CombatSettings settings = CombatSettings.defaults();
    StaminaWeights weights = settings.staminaWeights();
    StaminaRules staminaRules = new StaminaRules(settings);

    int maxHealth = 100;
    int maxStamina = 40;
    IntrinsicRatings ratings = new IntrinsicRatings(50.0, 30.0, maxHealth, maxStamina);
    FighterState state = new FighterState(maxHealth, maxStamina);

    assertEquals(maxStamina, state.currentStamina(), "il solo passare del turno non deve consumare Stamina");

    state.consumeStamina(staminaRules.attackCost());
    assertEquals(maxStamina - weights.attackCost(), state.currentStamina());
    assertEquals(1.0, staminaRules.fatigueMultiplier(state.currentStamina(), ratings), DELTA,
        "sopra la soglia alta non c'è penalità di affaticamento");

    state.consumeStamina(14);
    assertEquals(20, state.currentStamina());
    assertEquals(1.0 - weights.mediumFatiguePenalty(), staminaRules.fatigueMultiplier(state.currentStamina(), ratings),
        DELTA, "nella fascia intermedia scatta la penalità del 15%");

    state.consumeStamina(11);
    assertEquals(9, state.currentStamina());
    assertEquals(1.0 - weights.heavyFatiguePenalty(), staminaRules.fatigueMultiplier(state.currentStamina(), ratings),
        DELTA, "sotto la soglia bassa scatta la penalità del 30%");

    state.consumeStamina(999);
    assertEquals(0, state.currentStamina(), "la Stamina ha un floor a 0, mai negativa");
  }
}
