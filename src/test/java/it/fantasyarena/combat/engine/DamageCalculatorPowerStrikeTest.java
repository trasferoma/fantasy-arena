package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.IntrinsicRatings;
import it.fantasyarena.combat.testsupport.CombatFixtures;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;

/**
 * DoD 5 della SPEC colpo-potente: il moltiplicatore del colpo potente e' uno step separato da
 * quello del critico e cumulativo con esso (un colpo potente e critico moltiplica il danno per
 * entrambi). Rating intrinseci impostati direttamente (bypassando la derivazione da
 * {@code RatingStrategy}) per isolare il calcolo dal resto della pipeline.
 */
class DamageCalculatorPowerStrikeTest {

  @Test
  void colpoPotenteASegno_raddoppiaDanno_cumulativoColCritico() {
    CombatSettings settings = CombatSettings.defaults();
    Fighter attacker = buildFighter("Attaccante", 10.0, 0.0);
    Fighter defender = buildFighter("Difensore", 0.0, 4.0);
    DamageCalculator damageCalculator =
        new DamageCalculator(settings, new MomentumRules(settings), new StaminaRules(settings));

    DiceThrow varianceThrow = new DiceThrow(50, 100); // varianza neutra: normalized 0.5
    DefenseOutcome hitTaken = new DefenseOutcome(DefenseOutcome.DefenseResult.HIT_TAKEN, 0.0);

    int plainDamage = damageCalculator.calculateDamage(attacker, defender, CombatContext.empty(),
        new HitOutcome(true, false), hitTaken, varianceThrow, false);
    int powerOnlyDamage = damageCalculator.calculateDamage(attacker, defender, CombatContext.empty(),
        new HitOutcome(true, false), hitTaken, varianceThrow, true);
    int criticalOnlyDamage = damageCalculator.calculateDamage(attacker, defender, CombatContext.empty(),
        new HitOutcome(true, true), hitTaken, varianceThrow, false);
    int criticalAndPowerDamage = damageCalculator.calculateDamage(attacker, defender, CombatContext.empty(),
        new HitOutcome(true, true), hitTaken, varianceThrow, true);

    assertEquals(8, plainDamage, "danno grezzo di riferimento: offensiveRating 10 - 0.5 * defensiveRating 4");
    assertEquals(10, powerOnlyDamage, "il colpo potente da solo aggiunge il 30% di danno (damageMultiplier 1.3)");
    assertEquals(14, criticalOnlyDamage, "il critico da solo applica il suo moltiplicatore (1.75)");
    assertEquals(18, criticalAndPowerDamage,
        "colpo potente e critico sono cumulativi: 8 * 1.75 (critico) * 1.3 (potente) = 18,2 -> 18");
  }

  /**
   * Costruisce un {@link Fighter} con Rating intrinseci impostati direttamente: a Stamina e
   * Salute piene (Fighter appena creato) i moltiplicatori di momentum/affaticamento restano 1.0,
   * cosi' l'offesa/difesa effettiva coincide esattamente con {@code offensiveRating}/
   * {@code defensiveRating} passati.
   */
  private static Fighter buildFighter(String name, double offensiveRating, double defensiveRating) {
    CharacterResult character = CombatFixtures.createWarrior(name, 10, 10, 10, 10, 10);
    WeaponResult weapon = CombatFixtures.createSword(0);
    ArmourResult armour = CombatFixtures.createChestplate(0);
    IntrinsicRatings ratings = new IntrinsicRatings(offensiveRating, defensiveRating, 100, 50);
    return new Fighter(character, weapon, armour, null, ratings);
  }
}
