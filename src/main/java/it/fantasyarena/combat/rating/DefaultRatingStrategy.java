package it.fantasyarena.combat.rating;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.config.CombatSettings.RatingWeights;
import it.fantasyarena.combat.model.Characteristics;
import it.fantasyarena.combat.model.IntrinsicRatings;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Characteristic;

/**
 * Formule di base dei Rating intrinseci (funzione pura, deterministica). Pesi e bonus
 * tarabili vivono in {@link CombatSettings}: nessun numero magico qui.
 */
public final class DefaultRatingStrategy implements RatingStrategy {

  private final CombatSettings settings;

  public DefaultRatingStrategy(CombatSettings settings) {
    this.settings = settings;
  }

  @Override
  public IntrinsicRatings computeRatings(CharacterResult character, WeaponResult weapon, ArmourResult armour,
      ArmourResult shield) {
    double offensiveRating = computeOffensiveRating(character, weapon);
    double defensiveRating = computeDefensiveRating(character, armour, shield);
    int maxHealth = computeMaxHealth(character);
    int maxStamina = computeMaxStamina(character);
    return new IntrinsicRatings(offensiveRating, defensiveRating, maxHealth, maxStamina);
  }

  private double computeOffensiveRating(CharacterResult character, WeaponResult weapon) {
    RatingWeights weights = settings.ratingWeights();
    int strength = Characteristics.valueOf(character, Characteristic.STRENGTH);
    int agility = Characteristics.valueOf(character, Characteristic.AGILITY);
    double classBonus = weights.offensiveClassBonus().getOrDefault(character.characterClass(), 0.0);
    double raceBonus = weights.offensiveRaceBonus().getOrDefault(character.race(), 0.0);

    return weights.strengthOffenseWeight() * strength
        + weights.agilityOffenseWeight() * agility
        + weights.weaponAttackWeight() * weapon.attack()
        + classBonus
        + raceBonus;
  }

  private double computeDefensiveRating(CharacterResult character, ArmourResult armour, ArmourResult shield) {
    RatingWeights weights = settings.ratingWeights();
    int resistance = Characteristics.valueOf(character, Characteristic.RESISTANCE);
    int agility = Characteristics.valueOf(character, Characteristic.AGILITY);
    double shieldDefense = (shield != null) ? weights.shieldDefenseWeight() * shield.defense() : 0.0;
    double classBonus = weights.defensiveClassBonus().getOrDefault(character.characterClass(), 0.0);
    double raceBonus = weights.defensiveRaceBonus().getOrDefault(character.race(), 0.0);

    return weights.resistanceDefenseWeight() * resistance
        + weights.agilityDefenseWeight() * agility
        + weights.armourDefenseWeight() * armour.defense()
        + shieldDefense
        + classBonus
        + raceBonus;
  }

  private int computeMaxHealth(CharacterResult character) {
    RatingWeights weights = settings.ratingWeights();
    int resistance = Characteristics.valueOf(character, Characteristic.RESISTANCE);
    int stamina = Characteristics.valueOf(character, Characteristic.STAMINA);
    return weights.maxHealthBase()
        + weights.maxHealthPerResistance() * resistance
        + weights.maxHealthPerStamina() * stamina;
  }

  private int computeMaxStamina(CharacterResult character) {
    RatingWeights weights = settings.ratingWeights();
    int stamina = Characteristics.valueOf(character, Characteristic.STAMINA);
    return weights.maxStaminaBase() + weights.maxStaminaPerStamina() * stamina;
  }
}
