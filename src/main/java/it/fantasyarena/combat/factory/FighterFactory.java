package it.fantasyarena.combat.factory;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.IntrinsicRatings;
import it.fantasyarena.combat.rating.RatingStrategy;
import it.fantasytoolkit.armourgenerator.ArmourGeneratorTool;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.CharacterGeneratorTool;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.WeaponGeneratorTool;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.CharacterClass;
import it.fantasytoolkitcore.core.model.Weapon;

/**
 * Costruisce i {@link Fighter} a partire dai generatori del toolkit, applicando la
 * {@link RatingStrategy} per calcolare i Rating intrinseci. Nessuno scudo in v1.
 */
public class FighterFactory {

  private static final int TOTAL_CHARACTERISTIC_POINTS = 50;

  private final RatingStrategy ratingStrategy;

  public FighterFactory(RatingStrategy ratingStrategy) {
    this.ratingStrategy = ratingStrategy;
  }

  public Fighter createSwordWarrior() {
    CharacterResult character = generateWarrior();
    WeaponResult weapon = generateSword();
    ArmourResult armour = generateChestplate();
    IntrinsicRatings ratings = ratingStrategy.computeRatings(character, weapon, armour, null);
    return new Fighter(character, weapon, armour, null, ratings);
  }

  private CharacterResult generateWarrior() {
    return CharacterGeneratorTool.building()
        .randomRace()
        .characterClass(CharacterClass.WARRIOR)
        .addNickname()
        .allCharacteristics()
        .totalPoints(TOTAL_CHARACTERISTIC_POINTS)
        .generate();
  }

  private WeaponResult generateSword() {
    return WeaponGeneratorTool.building()
        .weapon(Weapon.SWORD)
        .randomRarity()
        .noStatusEffect()
        .generate();
  }

  private ArmourResult generateChestplate() {
    return ArmourGeneratorTool.building()
        .armour(Armour.CHESTPLATE)
        .randomRarity()
        .noStatusEffect()
        .generate();
  }
}
