package it.fantasyarena.combat.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings.PowerStrikeWeights;

/**
 * DoD 1 della SPEC colpo-potente: i pesi/soglia del colpo potente sono agganciati a
 * {@link CombatSettings#defaults()} con i valori empirici (provvisori) previsti dalla SPEC.
 */
class CombatSettingsPowerStrikeTest {

  @Test
  void powerStrikeWeights_neiDefaults() {
    PowerStrikeWeights weights = CombatSettings.defaults().powerStrikeWeights();

    assertEquals(2, weights.costMultiplier());
    assertEquals(1.3, weights.damageMultiplier());
    assertEquals(0.5, weights.staminaWeight());
    assertEquals(0.5, weights.healthWeight());
    assertEquals(0.5, weights.overconfidenceWeight());
    assertEquals(18.0, weights.intelligenceReference());
    assertEquals(0.2, weights.jitterWeight());
    assertEquals(6, weights.jitterDiceFaces());
    assertEquals(0.6, weights.decisionThreshold());
    assertEquals(4, weights.cooldownTurns());
  }
}
