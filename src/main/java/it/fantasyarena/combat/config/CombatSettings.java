package it.fantasyarena.combat.config;

import java.util.EnumMap;
import java.util.Map;

import it.fantasytoolkitcore.core.model.CharacterClass;
import it.fantasytoolkitcore.core.model.Race;

/**
 * Pesi, costi e soglie tarabili delle formule del motore di combattimento. Nessuna formula
 * vive qui: solo dati di configurazione consumati dal core. I valori di default sono
 * provvisori (taratura empirica), come previsto dalla SPEC.
 */
public record CombatSettings(
    RatingWeights ratingWeights,
    MomentumWeights momentumWeights,
    StaminaWeights staminaWeights,
    ChanceWeights chanceWeights,
    int maxTurns) {

  /**
   * Tetto di turni oltre il quale lo scontro termina per timeout (decisione ai punti).
   */
  public static CombatSettings defaults() {
    return new CombatSettings(
        RatingWeights.defaults(),
        MomentumWeights.defaults(),
        StaminaWeights.defaults(),
        ChanceWeights.defaults(),
        30);
  }

  /**
   * Pesi della formula di Offensive/Defensive Rating e dei pool massimi di Health/Stamina.
   */
  public record RatingWeights(
      double strengthOffenseWeight,
      double agilityOffenseWeight,
      double weaponAttackWeight,
      double resistanceDefenseWeight,
      double agilityDefenseWeight,
      double armourDefenseWeight,
      double shieldDefenseWeight,
      int maxHealthBase,
      int maxHealthPerResistance,
      int maxHealthPerStamina,
      int maxStaminaBase,
      int maxStaminaPerStamina,
      Map<CharacterClass, Double> offensiveClassBonus,
      Map<CharacterClass, Double> defensiveClassBonus,
      Map<Race, Double> offensiveRaceBonus,
      Map<Race, Double> defensiveRaceBonus) {

    public static RatingWeights defaults() {
      return new RatingWeights(
          1.0, 0.5, 2.0,
          1.0, 0.5, 2.0, 2.0,
          20, 5, 2,
          10, 3,
          Map.copyOf(offensiveClassBonusDefaults()),
          Map.copyOf(defensiveClassBonusDefaults()),
          Map.copyOf(offensiveRaceBonusDefaults()),
          Map.copyOf(defensiveRaceBonusDefaults()));
    }

    private static Map<CharacterClass, Double> offensiveClassBonusDefaults() {
      Map<CharacterClass, Double> bonus = new EnumMap<>(CharacterClass.class);
      bonus.put(CharacterClass.WARRIOR, 4.0);
      bonus.put(CharacterClass.RANGER, 3.0);
      bonus.put(CharacterClass.THIEF, 2.0);
      bonus.put(CharacterClass.MAGE, 1.0);
      return bonus;
    }

    private static Map<CharacterClass, Double> defensiveClassBonusDefaults() {
      Map<CharacterClass, Double> bonus = new EnumMap<>(CharacterClass.class);
      bonus.put(CharacterClass.WARRIOR, 4.0);
      bonus.put(CharacterClass.THIEF, 1.0);
      bonus.put(CharacterClass.RANGER, 1.0);
      bonus.put(CharacterClass.MAGE, 0.0);
      return bonus;
    }

    private static Map<Race, Double> offensiveRaceBonusDefaults() {
      Map<Race, Double> bonus = new EnumMap<>(Race.class);
      bonus.put(Race.ORC, 3.0);
      bonus.put(Race.UNDEAD, 2.0);
      bonus.put(Race.HUMAN, 1.0);
      bonus.put(Race.ELF, 1.0);
      return bonus;
    }

    private static Map<Race, Double> defensiveRaceBonusDefaults() {
      Map<Race, Double> bonus = new EnumMap<>(Race.class);
      bonus.put(Race.UNDEAD, 3.0);
      bonus.put(Race.ORC, 2.0);
      bonus.put(Race.HUMAN, 1.0);
      bonus.put(Race.ELF, 1.0);
      return bonus;
    }
  }

  /**
   * Range del Momentum, effetto massimo su danno/difesa e variazioni per evento di turno.
   */
  public record MomentumWeights(
      int min,
      int max,
      double effectCap,
      int gainOnHitLanded,
      int gainOnCriticalDealt,
      int gainOnParrySuccess,
      int gainOnDodgeSuccess,
      int lossOnHitTaken,
      int lossOnCriticalTaken,
      int lossOnMiss) {

    public static MomentumWeights defaults() {
      return new MomentumWeights(-100, 100, 0.15, 8, 15, 10, 10, -8, -15, -5);
    }
  }

  /**
   * Costi Stamina per azione e penalità progressive di affaticamento.
   */
  public record StaminaWeights(
      int attackCost,
      int parryCost,
      int dodgeCost,
      int impactCost,
      double highRatioThreshold,
      double lowRatioThreshold,
      double mediumFatiguePenalty,
      double heavyFatiguePenalty) {

    public static StaminaWeights defaults() {
      return new StaminaWeights(6, 4, 5, 2, 0.50, 0.25, 0.15, 0.30);
    }
  }

  /**
   * Soglie di probabilità per colpire/schivare/parare/critico e fattori del calcolo danno.
   */
  public record ChanceWeights(
      double baseHitChance,
      double hitChanceAgilityFactor,
      double minHitChance,
      double maxHitChance,
      double baseDodgeChance,
      double dodgeChanceAgilityFactor,
      double minDodgeChance,
      double maxDodgeChance,
      double baseParryChance,
      double parryDefenseDivisor,
      double minParryChance,
      double maxParryChance,
      double baseCritChance,
      double critChanceLuckFactor,
      double minCritChance,
      double maxCritChance,
      double criticalDamageMultiplier,
      double parryDamageReduction,
      double dodgeDamageReduction,
      double damageVarianceRange) {

    public static ChanceWeights defaults() {
      return new ChanceWeights(
          0.75, 0.02, 0.40, 0.95,
          0.10, 0.02, 0.02, 0.50,
          0.10, 200.0, 0.02, 0.50,
          0.05, 0.01, 0.05, 0.40,
          1.75, 0.70, 1.0, 0.10);
    }
  }
}
