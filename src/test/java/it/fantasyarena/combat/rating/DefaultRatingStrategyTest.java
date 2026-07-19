package it.fantasyarena.combat.rating;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.config.CombatSettings.RatingWeights;
import it.fantasyarena.combat.model.IntrinsicRatings;
import it.fantasyarena.combat.testsupport.CombatFixtures;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;

/**
 * DoD 2 — MaxHealth e MaxStamina sono derivati dalle caratteristiche del personaggio
 * secondo le formule configurate in {@link RatingWeights} (base + peso*caratteristica).
 */
class DefaultRatingStrategyTest {

  @Test
  void maxHealthAndStaminaDerivedFromCharacteristics() {
    int resistance = 12;
    int stamina = 9;
    CombatSettings settings = CombatSettings.defaults();
    RatingWeights weights = settings.ratingWeights();

    CharacterResult character = CombatFixtures.createWarrior("Combattente", 10, 10, resistance, stamina, 5);
    WeaponResult weapon = CombatFixtures.createSword(6);
    ArmourResult armour = CombatFixtures.createChestplate(4);

    IntrinsicRatings ratings = new DefaultRatingStrategy(settings).computeRatings(character, weapon, armour, null);

    int expectedMaxHealth = weights.maxHealthBase()
        + weights.maxHealthPerResistance() * resistance
        + weights.maxHealthPerStamina() * stamina;
    int expectedMaxStamina = weights.maxStaminaBase() + weights.maxStaminaPerStamina() * stamina;

    assertEquals(expectedMaxHealth, ratings.maxHealth());
    assertEquals(expectedMaxStamina, ratings.maxStamina());
  }
}
