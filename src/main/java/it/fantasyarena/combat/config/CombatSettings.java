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
    InitiativeWeights initiativeWeights,
    ChronicleWeights chronicleWeights,
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
        InitiativeWeights.defaults(),
        ChronicleWeights.defaults(),
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
   * Costi Stamina per azione, penalità progressive di affaticamento, recupero da riposo,
   * soglia sotto la quale conviene riposare invece di attaccare, fattore di impatto
   * proporzionale al danno subito da chi incassa un colpo pieno, malus di catena sul costo
   * d'attacco e recupero passivo di chi perde l'iniziativa. Valori del malus di catena e del
   * recupero passivo provvisori (taratura empirica).
   */
  public record StaminaWeights(
      int attackCost,
      int parryCost,
      int dodgeCost,
      int impactCost,
      double highRatioThreshold,
      double lowRatioThreshold,
      double mediumFatiguePenalty,
      double heavyFatiguePenalty,
      int restRecovery,
      int restThreshold,
      double impactStaminaDamageFactor,
      int chainMalusStep,
      int chainMalusCap,
      int passiveRecovery) {

    public static StaminaWeights defaults() {
      return new StaminaWeights(6, 4, 5, 2, 0.50, 0.25, 0.15, 0.30, 12, 11, 0.5, 2, 6, 4);
    }
  }

  /**
   * Pesi della formula d'iniziativa: rapporto Stamina corrente/massima, Agilità, Intelligenza
   * e un micro-jitter (piccolo dado) che rompe pareggi e simmetrie. Valori di default
   * provvisori (taratura empirica): {@code wStamina} domina senza schiacciare agilità e
   * intelligenza.
   */
  public record InitiativeWeights(
      double wStamina,
      double wAgility,
      double wIntelligence,
      double wJitter,
      int jitterDiceFaces) {

    public static InitiativeWeights defaults() {
      return new InitiativeWeights(25.0, 1.0, 0.5, 0.5, 6);
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

  /**
   * Soglia (percentuale della vita massima del bersaglio) oltre la quale un colpo è
   * considerato "pesante" dal cronista del turno.
   */
  public record ChronicleWeights(double heavyBlowHealthRatio) {

    public static ChronicleWeights defaults() {
      return new ChronicleWeights(0.25);
    }
  }
}
