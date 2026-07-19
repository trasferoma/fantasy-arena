package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.config.CombatSettings.MomentumWeights;

/**
 * DoD 5 — il Momentum resta sempre in {@code [-100, +100]} e il suo effetto su danno/difesa
 * è limitato a ±15%.
 */
class MomentumRulesTest {

  private static final double DELTA = 1e-9;

  @Test
  void momentumWithinRange_effectCappedAt15pct() {
    CombatSettings settings = CombatSettings.defaults();
    MomentumWeights weights = settings.momentumWeights();
    MomentumRules momentumRules = new MomentumRules(settings);

    assertEquals(weights.max(), momentumRules.clamp(weights.max() + 500), "il momentum non deve mai superare il massimo");
    assertEquals(weights.min(), momentumRules.clamp(weights.min() - 500), "il momentum non deve mai scendere sotto il minimo");
    assertEquals(0, momentumRules.clamp(0));

    assertEquals(1.0 + weights.effectCap(), momentumRules.effectMultiplier(weights.max()), DELTA,
        "al momentum massimo l'effetto deve essere limitato a +15%");
    assertEquals(1.0 - weights.effectCap(), momentumRules.effectMultiplier(weights.min()), DELTA,
        "al momentum minimo l'effetto deve essere limitato a -15%");
    assertEquals(1.0, momentumRules.effectMultiplier(0), DELTA, "a momentum zero l'effetto deve essere neutro");

    // Valori oltre il range teorico non devono ampliare l'effetto oltre il cap del ±15%.
    assertEquals(1.0 + weights.effectCap(), momentumRules.effectMultiplier(weights.max() * 10), DELTA);
    assertEquals(1.0 - weights.effectCap(), momentumRules.effectMultiplier(weights.min() * 10), DELTA);
  }
}
