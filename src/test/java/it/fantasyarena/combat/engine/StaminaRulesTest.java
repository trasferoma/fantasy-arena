package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

  @Test
  void restAndHardRulesAtZeroStamina() {
    CombatSettings settings = CombatSettings.defaults();
    StaminaWeights weights = settings.staminaWeights();
    StaminaRules staminaRules = new StaminaRules(settings);

    assertEquals(12, weights.restRecovery());
    assertEquals(11, weights.restThreshold());
    assertEquals(12, staminaRules.restRecovery());
    assertEquals(11, staminaRules.restThreshold());

    assertTrue(staminaRules.shouldRest(0), "a Stamina 0 il riposo e' obbligatorio");
    assertTrue(staminaRules.shouldRest(10), "sotto la soglia di riposo conviene riposare");
    assertFalse(staminaRules.shouldRest(11), "alla soglia di riposo si puo' ancora attaccare");
    assertFalse(staminaRules.shouldRest(20), "sopra la soglia di riposo non serve riposare");

    assertFalse(staminaRules.canAttack(0), "a Stamina 0 non si puo' attaccare");
    assertTrue(staminaRules.canAttack(1), "con almeno 1 Stamina si puo' ancora attaccare");

    assertFalse(staminaRules.canDefend(0), "a Stamina 0 non ci si puo' difendere");
    assertTrue(staminaRules.canDefend(1), "con almeno 1 Stamina ci si puo' ancora difendere");
  }

  @Test
  void impactStaminaLoss_scalesWithDamageWithFloor() {
    CombatSettings settings = CombatSettings.defaults();
    StaminaRules staminaRules = new StaminaRules(settings);

    assertEquals(0.5, staminaRules.impactStaminaDamageFactor(), DELTA);
    assertEquals(2, staminaRules.impactStaminaLoss(2), "sotto il minimo il floor resta impactCost");
    assertEquals(2, staminaRules.impactStaminaLoss(4), "al limite del minimo il floor resta impactCost");
    assertEquals(4, staminaRules.impactStaminaLoss(8), "oltre il minimo la perdita e' proporzionale al danno");
    assertEquals(6, staminaRules.impactStaminaLoss(12), "la perdita cresce linearmente col danno");
  }

  @Test
  void effectiveAttackCost_growsWithChain_upToCap() {
    CombatSettings settings = CombatSettings.defaults();
    StaminaWeights weights = settings.staminaWeights();
    StaminaRules staminaRules = new StaminaRules(settings);

    assertEquals(2, weights.chainMalusStep());
    assertEquals(6, weights.chainMalusCap());
    assertEquals(2, staminaRules.chainMalusStep());
    assertEquals(6, staminaRules.chainMalusCap());

    assertEquals(6, staminaRules.effectiveAttackCost(1), "primo turno d'attacco: nessun malus");
    assertEquals(8, staminaRules.effectiveAttackCost(2), "secondo attacco consecutivo: +1 step");
    assertEquals(10, staminaRules.effectiveAttackCost(3), "terzo attacco consecutivo: +2 step");
    assertEquals(12, staminaRules.effectiveAttackCost(4), "quarto attacco consecutivo: malus al cap");
    assertEquals(12, staminaRules.effectiveAttackCost(5), "oltre il cap il malus non cresce ulteriormente");
  }

  @Test
  void passiveRecovery_readFromSettings() {
    CombatSettings settings = CombatSettings.defaults();
    StaminaRules staminaRules = new StaminaRules(settings);

    assertEquals(4, settings.staminaWeights().passiveRecovery());
    assertEquals(4, staminaRules.passiveRecovery());
  }
}
