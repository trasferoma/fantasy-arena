package it.fantasyarena.combat.engine;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.config.CombatSettings.StaminaWeights;
import it.fantasyarena.combat.model.IntrinsicRatings;

/**
 * Regole pure della Stamina: costo delle azioni e penalità progressive di affaticamento,
 * calcolate sul rapporto tra Stamina corrente e massima.
 */
public final class StaminaRules {

  private final CombatSettings settings;

  public StaminaRules(CombatSettings settings) {
    this.settings = settings;
  }

  public int attackCost() {
    return settings.staminaWeights().attackCost();
  }

  public int parryCost() {
    return settings.staminaWeights().parryCost();
  }

  public int dodgeCost() {
    return settings.staminaWeights().dodgeCost();
  }

  public int impactCost() {
    return settings.staminaWeights().impactCost();
  }

  /**
   * Moltiplicatore di affaticamento applicato ad attacco e difesa: nessuna penalità sopra
   * la soglia alta, -15% nella fascia intermedia, -30% sotto la soglia bassa.
   */
  public double fatigueMultiplier(int currentStamina, IntrinsicRatings ratings) {
    StaminaWeights weights = settings.staminaWeights();
    double ratio = (double) currentStamina / ratings.maxStamina();

    if (ratio > weights.highRatioThreshold()) {
      return 1.0;
    }
    if (ratio >= weights.lowRatioThreshold()) {
      return 1.0 - weights.mediumFatiguePenalty();
    }
    return 1.0 - weights.heavyFatiguePenalty();
  }
}
