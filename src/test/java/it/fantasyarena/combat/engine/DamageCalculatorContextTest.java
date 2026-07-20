package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.context.ContextModifier;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.IntrinsicRatings;
import it.fantasyarena.combat.testsupport.CombatFixtures;

/**
 * DoD 7 — con {@link CombatContext#empty()} (v1) i Rating non cambiano e l'esito è
 * invariante rispetto all'assenza di modificatori: un context vuoto è equivalente a un
 * context con un unico modificatore neutro (moltiplicatori 1.0).
 */
class DamageCalculatorContextTest {

  @Test
  void emptyContext_doesNotAlterRatingsNorOutcome() {
    CombatContext emptyContext = CombatContext.empty();
    assertEquals(1.0, emptyContext.offensiveMultiplier(), "il context vuoto non deve alterare l'offesa effettiva");
    assertEquals(1.0, emptyContext.defensiveMultiplier(), "il context vuoto non deve alterare la difesa effettiva");

    CombatSettings settings = CombatSettings.defaults();
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 16, 12, 10, 8, 6, 7, 3);
    Fighter defender = CombatFixtures.createFighter("Difensore", 12, 14, 11, 9, 4, 5, 5);
    IntrinsicRatings ratingsBefore = attacker.ratings();

    DamageCalculator damageCalculator =
        new DamageCalculator(settings, new MomentumRules(settings), new StaminaRules(settings));
    HitOutcome hitOutcome = new HitOutcome(true, false);
    DefenseOutcome defenseOutcome = new DefenseOutcome(DefenseOutcome.DefenseResult.HIT_TAKEN, 0.0);
    DiceThrow varianceThrow = new DiceThrow(50, 100);

    CombatContext neutralContext = CombatContext.of(List.of(new ContextModifier(1.0, 1.0)));

    int damageWithEmptyContext = damageCalculator.calculateDamage(attacker, defender, emptyContext, hitOutcome,
        defenseOutcome, varianceThrow, false);
    int damageWithNeutralContext = damageCalculator.calculateDamage(attacker, defender, neutralContext, hitOutcome,
        defenseOutcome, varianceThrow, false);

    assertEquals(damageWithNeutralContext, damageWithEmptyContext,
        "un context vuoto deve produrre lo stesso danno di un context con modificatore neutro");
    assertSame(ratingsBefore, attacker.ratings(), "i Rating devono restare la stessa istanza dopo l'uso del context");
  }
}
