package it.fantasyarena.combat.engine;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.config.CombatSettings.ChanceWeights;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.IntrinsicRatings;

/**
 * Calcolo puro del danno: combina Rating, moltiplicatori di momentum/stamina/context,
 * variazione casuale (da un {@link DiceThrow} in input) ed esito di colpo/difesa. Danno
 * grezzo minimo garantito a 1 se il colpo va a segno; una difesa riuscita può comunque
 * azzerare il danno finale (schivata piena).
 */
public final class DamageCalculator {

  private final CombatSettings settings;
  private final MomentumRules momentumRules;
  private final StaminaRules staminaRules;

  public DamageCalculator(CombatSettings settings, MomentumRules momentumRules, StaminaRules staminaRules) {
    this.settings = settings;
    this.momentumRules = momentumRules;
    this.staminaRules = staminaRules;
  }

  public int calculateDamage(Fighter attacker, Fighter defender, CombatContext context, HitOutcome hitOutcome,
      DefenseOutcome defenseOutcome, DiceThrow varianceThrow, boolean powerStrike) {

    if (!hitOutcome.hit()) {
      return 0;
    }

    double effectiveOffense = effectiveOffense(attacker, context);
    double effectiveDefense = effectiveDefense(defender, context);
    double rawDamage = Math.max(1.0, effectiveOffense - 0.5 * effectiveDefense);

    double variedDamage = applyVariance(rawDamage, varianceThrow);
    double criticalDamage = applyCritical(variedDamage, hitOutcome);
    double poweredDamage = applyPowerStrike(criticalDamage, powerStrike);
    double finalDamage = poweredDamage * (1.0 - defenseOutcome.damageReduction());

    return Math.max(0, (int) Math.round(finalDamage));
  }

  private double effectiveOffense(Fighter attacker, CombatContext context) {
    IntrinsicRatings ratings = attacker.ratings();
    double momentumMult = momentumRules.effectMultiplier(attacker.state().momentum());
    double staminaMult = staminaRules.fatigueMultiplier(attacker.state().currentStamina(), ratings);
    return ratings.offensiveRating() * momentumMult * staminaMult * context.offensiveMultiplier();
  }

  private double effectiveDefense(Fighter defender, CombatContext context) {
    IntrinsicRatings ratings = defender.ratings();
    double momentumMult = momentumRules.effectMultiplier(defender.state().momentum());
    double staminaMult = staminaRules.fatigueMultiplier(defender.state().currentStamina(), ratings);
    return ratings.defensiveRating() * momentumMult * staminaMult * context.defensiveMultiplier();
  }

  private double applyVariance(double rawDamage, DiceThrow varianceThrow) {
    ChanceWeights chances = settings.chanceWeights();
    double variance = (varianceThrow.normalized() * 2.0 - 1.0) * chances.damageVarianceRange();
    return rawDamage * (1.0 + variance);
  }

  private double applyCritical(double damage, HitOutcome hitOutcome) {
    if (!hitOutcome.critical()) {
      return damage;
    }
    return damage * settings.chanceWeights().criticalDamageMultiplier();
  }

  /**
   * Moltiplicatore del colpo potente: step separato dal critico e cumulativo con esso (un colpo
   * potente e critico moltiplica il danno per entrambi).
   */
  private double applyPowerStrike(double damage, boolean powerStrike) {
    if (!powerStrike) {
      return damage;
    }
    return damage * settings.powerStrikeWeights().damageMultiplier();
  }
}
