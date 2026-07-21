package it.fantasyarena.combat.config;

import it.fantasyarena.combat.config.CombatSettings.ChanceWeights;
import it.fantasyarena.combat.config.CombatSettings.ChronicleWeights;
import it.fantasyarena.combat.config.CombatSettings.InitiativeWeights;
import it.fantasyarena.combat.config.CombatSettings.MomentumWeights;
import it.fantasyarena.combat.config.CombatSettings.PowerStrikeWeights;
import it.fantasyarena.combat.config.CombatSettings.RatingWeights;
import it.fantasyarena.combat.config.CombatSettings.StaminaWeights;

/**
 * Fonte unica di verità delle formule del motore di combattimento: metodi statici puri,
 * deterministici e senza effetti collaterali. Consuma solo i pesi di {@link CombatSettings} e
 * primitivi: nessuna dipendenza dal package {@code engine} (i resolver dipendono da qui, non il
 * contrario). I resolver restano shell sottili che estraggono le stat dai combattenti, chiamano
 * queste formule e costruiscono gli esiti di dominio.
 */
public final class CombatFormulas {

  private static final double DEFENSE_DAMAGE_DAMPENING = 0.5;

  private CombatFormulas() {
  }

  public static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  public static double clamp01(double value) {
    return clamp(value, 0.0, 1.0);
  }

  public static double ratio(int current, int max) {
    return (double) current / max;
  }

  public static double offensiveRating(RatingWeights weights, int strength, int agility, int weaponAttack,
      double classBonus, double raceBonus) {
    return weights.strengthOffenseWeight() * strength
        + weights.agilityOffenseWeight() * agility
        + weights.weaponAttackWeight() * weaponAttack
        + classBonus
        + raceBonus;
  }

  public static double defensiveRating(RatingWeights weights, int resistance, int agility, int armourDefense,
      int shieldDefense, double classBonus, double raceBonus) {
    return weights.resistanceDefenseWeight() * resistance
        + weights.agilityDefenseWeight() * agility
        + weights.armourDefenseWeight() * armourDefense
        + weights.shieldDefenseWeight() * shieldDefense
        + classBonus
        + raceBonus;
  }

  public static int maxHealth(RatingWeights weights, int resistance, int stamina) {
    return weights.maxHealthBase() + weights.maxHealthPerResistance() * resistance
        + weights.maxHealthPerStamina() * stamina;
  }

  public static int maxStamina(RatingWeights weights, int stamina) {
    return weights.maxStaminaBase() + weights.maxStaminaPerStamina() * stamina;
  }

  public static int clampMomentum(MomentumWeights weights, int momentum) {
    return Math.max(weights.min(), Math.min(weights.max(), momentum));
  }

  public static double momentumEffectMultiplier(MomentumWeights weights, int momentum) {
    double normalized = (double) momentum / weights.max();
    double capped = clamp(normalized, -1.0, 1.0);
    return 1.0 + capped * weights.effectCap();
  }

  /**
   * Nessuna penalità sopra la soglia alta, -15% (default) nella fascia intermedia, -30%
   * (default) sotto la soglia bassa.
   */
  public static double fatigueMultiplier(StaminaWeights weights, int currentStamina, int maxStamina) {
    double ratio = ratio(currentStamina, maxStamina);

    if (ratio > weights.highRatioThreshold()) {
      return 1.0;
    }
    if (ratio >= weights.lowRatioThreshold()) {
      return 1.0 - weights.mediumFatiguePenalty();
    }
    return 1.0 - weights.heavyFatiguePenalty();
  }

  public static int impactStaminaLoss(StaminaWeights weights, int damage) {
    return Math.max(weights.impactCost(), (int) Math.round(damage * weights.impactStaminaDamageFactor()));
  }

  public static int effectiveAttackCost(StaminaWeights weights, int consecutiveInitiativeWins) {
    int chainMalus = Math.min(weights.chainMalusCap(), weights.chainMalusStep() * (consecutiveInitiativeWins - 1));
    return weights.attackCost() + chainMalus;
  }

  public static boolean shouldRest(StaminaWeights weights, int currentStamina, int maxStamina) {
    return currentStamina <= 0 || currentStamina < weights.restThresholdRatio() * maxStamina;
  }

  public static double hitChance(ChanceWeights weights, int attackerAgility, int defenderAgility) {
    double hitChance = weights.baseHitChance() + weights.hitChanceAgilityFactor() * (attackerAgility - defenderAgility);
    return clamp(hitChance, weights.minHitChance(), weights.maxHitChance());
  }

  public static double critChance(ChanceWeights weights, int luck) {
    double critChance = weights.baseCritChance() + weights.critChanceLuckFactor() * luck;
    return clamp(critChance, weights.minCritChance(), weights.maxCritChance());
  }

  public static boolean isHit(double roll, double hitChance) {
    return roll <= hitChance;
  }

  public static boolean isCritical(double roll, double critChance) {
    return roll <= critChance;
  }

  public static double dodgeChance(ChanceWeights weights, int defenderAgility, int attackerAgility) {
    double dodgeChance =
        weights.baseDodgeChance() + weights.dodgeChanceAgilityFactor() * (defenderAgility - attackerAgility);
    return clamp(dodgeChance, weights.minDodgeChance(), weights.maxDodgeChance());
  }

  public static double parryChance(ChanceWeights weights, double defensiveRating) {
    double parryChance = weights.baseParryChance() + defensiveRating / weights.parryDefenseDivisor();
    return clamp(parryChance, weights.minParryChance(), weights.maxParryChance());
  }

  public static boolean dodges(double roll, double dodgeChance) {
    return roll <= dodgeChance;
  }

  public static boolean parries(double roll, double dodgeChance, double parryChance) {
    return roll > dodgeChance && roll <= dodgeChance + parryChance;
  }

  public static double effectiveRating(double baseRating, double momentumMultiplier, double fatigueMultiplier,
      double contextMultiplier) {
    return baseRating * momentumMultiplier * fatigueMultiplier * contextMultiplier;
  }

  public static double rawDamage(double effectiveOffense, double effectiveDefense) {
    return Math.max(1.0, effectiveOffense - DEFENSE_DAMAGE_DAMPENING * effectiveDefense);
  }

  public static double applyDamageVariance(ChanceWeights weights, double rawDamage, double normalizedRoll) {
    double variance = (normalizedRoll * 2.0 - 1.0) * weights.damageVarianceRange();
    return rawDamage * (1.0 + variance);
  }

  public static double applyCriticalDamage(ChanceWeights weights, double damage, boolean critical) {
    if (!critical) {
      return damage;
    }
    return damage * weights.criticalDamageMultiplier();
  }

  /**
   * Moltiplicatore del colpo potente: step separato dal critico e cumulativo con esso (un colpo
   * potente e critico moltiplica il danno per entrambi).
   */
  public static double applyPowerStrikeDamage(PowerStrikeWeights weights, double damage, boolean powerStrike) {
    if (!powerStrike) {
      return damage;
    }
    return damage * weights.damageMultiplier();
  }

  public static double applyDamageReduction(double damage, double reduction) {
    return damage * (1.0 - reduction);
  }

  public static int powerStrikeCost(PowerStrikeWeights weights, int attackCost) {
    return weights.costMultiplier() * attackCost;
  }

  /**
   * Score della decisione del colpo potente: parte razionale (stamina/vita residue) più
   * overconfidence da momentum positivo (attenuata dall'Intelligenza) più un micro-jitter.
   */
  public static double powerStrikeScore(PowerStrikeWeights weights, MomentumWeights momentumWeights,
      double staminaRatio, double healthRatio, int momentum, double intelligence, double jitterNormalized) {
    double momentumNorm = clamp01((double) momentum / momentumWeights.max());
    double intelFactor = clamp01(intelligence / weights.intelligenceReference());

    double rational = weights.staminaWeight() * staminaRatio + weights.healthWeight() * healthRatio;
    double overconfidence = weights.overconfidenceWeight() * momentumNorm;

    return rational + (1.0 - intelFactor) * overconfidence + weights.jitterWeight() * jitterNormalized;
  }

  public static double initiativeStaminaComponent(InitiativeWeights weights, int currentStamina, int maxStamina) {
    return weights.wStamina() * ratio(currentStamina, maxStamina);
  }

  public static double initiativeAgilityComponent(InitiativeWeights weights, int agility) {
    return weights.wAgility() * agility;
  }

  public static double initiativeIntelligenceComponent(InitiativeWeights weights, int intelligence) {
    return weights.wIntelligence() * intelligence;
  }

  public static double initiativeJitterComponent(InitiativeWeights weights, int jitterValue) {
    return weights.wJitter() * jitterValue;
  }

  public static boolean isHeavyBlow(ChronicleWeights weights, int damage, int maxHealth) {
    return damage >= weights.heavyBlowHealthRatio() * maxHealth;
  }
}
