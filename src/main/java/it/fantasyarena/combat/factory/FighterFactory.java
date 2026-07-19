package it.fantasyarena.combat.factory;

import java.util.Random;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.IntrinsicRatings;
import it.fantasyarena.combat.rating.DefaultRatingStrategy;
import it.fantasyarena.combat.rating.RatingStrategy;
import it.fantasytoolkit.armourgenerator.ArmourGeneratorTool;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.CharacterGeneratorTool;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.WeaponGeneratorTool;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.CharacterClass;
import it.fantasytoolkitcore.core.model.Race;
import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.Weapon;

/**
 * Costruisce i {@link Fighter} a partire dai generatori del toolkit, applicando la
 * {@link RatingStrategy} per calcolare i Rating intrinseci. Nessuno scudo in v1.
 */
public class FighterFactory {

  private static final int TOTAL_CHARACTERISTIC_POINTS = 10;

  private final RatingStrategy ratingStrategy;
  private final Random random = new Random();

  public FighterFactory(RatingStrategy ratingStrategy) {
    this.ratingStrategy = ratingStrategy;
  }

  /**
   * Crea una factory che calcola i Rating intrinseci con la strategia di default,
   * tarata sugli stessi {@link CombatSettings} usati poi dall'Arena per il combattimento.
   */
  public static FighterFactory withDefaultRatings(CombatSettings settings) {
    return new FighterFactory(new DefaultRatingStrategy(settings));
  }

  /**
   * Crea due combattenti equi-equipaggiati: la rarita' dell'arma e quella dell'armatura
   * vengono estratte una sola volta e condivise da entrambi, cosi' che nessuno dei due
   * parta con un vantaggio di equipaggiamento sull'altro.
   */
  public Duelists createMatchedSwordWarriors() {
    Rarity weaponRarity = Rarity.UNCOMMON; // randomRarity();
    Rarity armourRarity = Rarity.UNCOMMON; // randomRarity();
    Fighter first = createSwordWarrior(weaponRarity, armourRarity);
    Fighter second = createSwordWarrior(weaponRarity, armourRarity);
    return new Duelists(first, second);
  }

  /**
   * Crea un guerriero con spada e corazza della rarita' indicata.
   */
  public Fighter createSwordWarrior(Rarity weaponRarity, Rarity armourRarity) {
    CharacterResult character = generateWarrior();
    WeaponResult weapon = generateSword(weaponRarity);
    ArmourResult armour = generateChestplate(armourRarity);
    IntrinsicRatings ratings = ratingStrategy.computeRatings(character, weapon, armour, null);
    return new Fighter(character, weapon, armour, null, ratings);
  }

  private Rarity randomRarity() {
    Rarity[] rarities = Rarity.values();
    return rarities[random.nextInt(rarities.length)];
  }

  private CharacterResult generateWarrior() {
    return CharacterGeneratorTool.building()
        .race(Race.HUMAN)
        .characterClass(CharacterClass.WARRIOR)
        // .addNickname()
        .allCharacteristics()
        .totalPoints(TOTAL_CHARACTERISTIC_POINTS)
        .generate();
  }

  private WeaponResult generateSword(Rarity rarity) {
    return WeaponGeneratorTool.building()
        .weapon(Weapon.SWORD)
        .rarity(rarity)
        .noStatusEffect()
        .generate();
  }

  private ArmourResult generateChestplate(Rarity rarity) {
    return ArmourGeneratorTool.building()
        .armour(Armour.CHESTPLATE)
        .rarity(rarity)
        .noStatusEffect()
        .generate();
  }

  /**
   * Coppia di combattenti equi-equipaggiati, pronti per disputare il duello.
   */
  public record Duelists(Fighter first, Fighter second) {
  }
}
