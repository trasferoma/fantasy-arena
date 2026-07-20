package it.fantasyarena.combat.testsupport;

import java.util.List;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.dice.DiceRoller;
import it.fantasyarena.combat.engine.CombatEngine;
import it.fantasyarena.combat.engine.DamageCalculator;
import it.fantasyarena.combat.engine.DefenseResolver;
import it.fantasyarena.combat.engine.HitResolver;
import it.fantasyarena.combat.engine.InitiativeResolver;
import it.fantasyarena.combat.engine.MomentumRules;
import it.fantasyarena.combat.engine.StaminaRules;
import it.fantasyarena.combat.engine.TurnOrchestrator;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.IntrinsicRatings;
import it.fantasyarena.combat.rating.DefaultRatingStrategy;
import it.fantasyarena.combat.rating.RatingStrategy;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.CharacterClass;
import it.fantasytoolkitcore.core.model.Characteristic;
import it.fantasytoolkitcore.core.model.Race;
import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.Weapon;

/**
 * Fixture deterministiche per i test del Combat Engine: costruisce {@link CharacterResult},
 * {@link WeaponResult}, {@link ArmourResult} e {@link Fighter} con caratteristiche fisse
 * (nessun generatore casuale del toolkit), e assembla un {@link CombatEngine} completo dati
 * un {@link DiceRoller} (reale o stub) e un {@link CombatSettings}.
 */
public final class CombatFixtures {

  /**
   * Moltiplicatore di costo del colpo potente usato da {@link #withPowerStrikeUnaffordable}:
   * abbastanza grande da restare irraggiungibile per qualunque pool di Stamina di questi
   * fixture.
   */
  private static final int UNAFFORDABLE_POWER_STRIKE_COST_MULTIPLIER = 1_000_000;

  private CombatFixtures() {
  }

  /**
   * Costruisce un {@link Fighter} guerriero umano con spada e corazza, con Rating calcolati
   * dalla {@link DefaultRatingStrategy} reale sulle caratteristiche fisse fornite. Intelligenza
   * fissa a 10 (per varianti con Intelligenza configurabile, vedi l'overload dedicato).
   */
  public static Fighter createFighter(String name, int strength, int agility, int resistance, int stamina, int luck,
      int weaponAttack, int armourDefense) {
    return createFighter(name, strength, agility, resistance, stamina, luck, 10, weaponAttack, armourDefense);
  }

  /**
   * Come {@link #createFighter(String, int, int, int, int, int, int, int)}, con Intelligenza
   * configurabile: utile per i test del {@code PowerStrikeResolver}, la cui decisione dipende
   * anche da questa caratteristica.
   */
  public static Fighter createFighter(String name, int strength, int agility, int resistance, int stamina, int luck,
      int intelligence, int weaponAttack, int armourDefense) {
    CharacterResult character = createWarrior(name, strength, agility, resistance, stamina, luck, intelligence);
    WeaponResult weapon = createSword(weaponAttack);
    ArmourResult armour = createChestplate(armourDefense);
    RatingStrategy ratingStrategy = new DefaultRatingStrategy(CombatSettings.defaults());
    IntrinsicRatings ratings = ratingStrategy.computeRatings(character, weapon, armour, null);
    return new Fighter(character, weapon, armour, null, ratings);
  }

  public static CharacterResult createWarrior(String name, int strength, int agility, int resistance, int stamina,
      int luck) {
    return createWarrior(name, strength, agility, resistance, stamina, luck, 10);
  }

  public static CharacterResult createWarrior(String name, int strength, int agility, int resistance, int stamina,
      int luck, int intelligence) {
    List<CharacterCharacteristic> characteristics = List.of(
        new CharacterCharacteristic(Characteristic.STRENGTH, strength),
        new CharacterCharacteristic(Characteristic.AGILITY, agility),
        new CharacterCharacteristic(Characteristic.RESISTANCE, resistance),
        new CharacterCharacteristic(Characteristic.STAMINA, stamina),
        new CharacterCharacteristic(Characteristic.LUCK, luck),
        new CharacterCharacteristic(Characteristic.INTELLIGENCE, intelligence),
        new CharacterCharacteristic(Characteristic.CHARISMA, 10));
    return new CharacterResult(Race.HUMAN, CharacterClass.WARRIOR, name, characteristics);
  }

  public static WeaponResult createSword(int attack) {
    return new WeaponResult(Weapon.SWORD, Rarity.COMMON, List.of(), List.of(), attack);
  }

  public static ArmourResult createChestplate(int defense) {
    return new ArmourResult(Armour.CHESTPLATE, Rarity.COMMON, List.of(), List.of(), defense);
  }

  /**
   * Assembla un {@link CombatEngine} con tutti i resolver del core cablati sullo stesso
   * {@link CombatSettings} e sul {@link DiceRoller} fornito (reale o {@link StubDiceRoller}).
   */
  public static CombatEngine buildEngine(DiceRoller diceRoller, CombatSettings settings) {
    HitResolver hitResolver = new HitResolver(settings);
    DefenseResolver defenseResolver = new DefenseResolver(settings);
    MomentumRules momentumRules = new MomentumRules(settings);
    StaminaRules staminaRules = new StaminaRules(settings);
    DamageCalculator damageCalculator = new DamageCalculator(settings, momentumRules, staminaRules);
    TurnOrchestrator turnOrchestrator = new TurnOrchestrator(diceRoller, hitResolver, defenseResolver,
        damageCalculator, momentumRules, staminaRules, settings);
    InitiativeResolver initiativeResolver = new InitiativeResolver(settings);
    return new CombatEngine(diceRoller, initiativeResolver, turnOrchestrator, settings);
  }

  /**
   * Rende il costo del colpo potente strutturalmente non pagabile, qualunque sia la Stamina del
   * combattente: usato dai test che non riguardano il colpo potente per preservare, dado per
   * dado, la sequenza e gli esiti di oggi (nessun jitter di decisione consumato, nessun effetto
   * di costo/danno/highlight/descrizione del colpo potente).
   */
  public static CombatSettings withPowerStrikeUnaffordable(CombatSettings settings) {
    CombatSettings.PowerStrikeWeights base = settings.powerStrikeWeights();
    CombatSettings.PowerStrikeWeights unaffordable = new CombatSettings.PowerStrikeWeights(
        UNAFFORDABLE_POWER_STRIKE_COST_MULTIPLIER, base.damageMultiplier(), base.staminaWeight(),
        base.healthWeight(), base.overconfidenceWeight(), base.intelligenceReference(), base.jitterWeight(),
        base.jitterDiceFaces(), base.decisionThreshold(), base.cooldownTurns());
    return new CombatSettings(settings.ratingWeights(), settings.momentumWeights(), settings.staminaWeights(),
        settings.chanceWeights(), settings.initiativeWeights(), settings.chronicleWeights(), unaffordable,
        settings.maxTurns());
  }
}
