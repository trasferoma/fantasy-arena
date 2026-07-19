package it.fantasyarena.combat.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.testsupport.CombatFixtures;

/**
 * DoD 1 — un {@link Fighter} costruito con Rating deterministici li mantiene invariati
 * anche dopo che il suo {@link FighterState} è stato mutato durante lo scontro.
 */
class FighterRatingsImmutabilityTest {

  @Test
  void ratingsImmutableDuringCombat() {
    Fighter fighter = CombatFixtures.createFighter("Combattente", 15, 10, 12, 8, 5, 6, 4);
    IntrinsicRatings ratingsBefore = fighter.ratings();

    fighter.state().applyDamage(9999);
    fighter.state().consumeStamina(9999);
    fighter.state().setMomentum(75);

    IntrinsicRatings ratingsAfter = fighter.ratings();
    assertSame(ratingsBefore, ratingsAfter, "i Rating devono restare la stessa istanza immutabile");
    assertEquals(ratingsBefore.offensiveRating(), ratingsAfter.offensiveRating());
    assertEquals(ratingsBefore.defensiveRating(), ratingsAfter.defensiveRating());
    assertEquals(ratingsBefore.maxHealth(), ratingsAfter.maxHealth());
    assertEquals(ratingsBefore.maxStamina(), ratingsAfter.maxStamina());
  }
}
