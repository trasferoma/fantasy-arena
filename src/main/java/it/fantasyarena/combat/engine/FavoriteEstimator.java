package it.fantasyarena.combat.engine;

import java.util.Comparator;
import java.util.Optional;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.IntrinsicRatings;

/**
 * Stima pura (nessun RNG) del favorito pre-scontro tra due combattenti, dai soli Rating
 * intrinseci: vince chi ha la somma Offensive+Defensive Rating più alta; a parità decide
 * {@code maxHealth}, poi {@code maxStamina}; se anche questi coincidono lo scontro è
 * equilibrato (nessun favorito netto).
 */
public final class FavoriteEstimator {

  private static final Comparator<Fighter> BY_FAVORITE_PRECEDENCE = Comparator
      .comparingDouble(FavoriteEstimator::totalRating)
      .thenComparingInt(fighter -> fighter.ratings().maxHealth())
      .thenComparingInt(fighter -> fighter.ratings().maxStamina());

  public Optional<Fighter> favorite(Fighter first, Fighter second) {
    int precedence = BY_FAVORITE_PRECEDENCE.compare(first, second);
    if (precedence == 0) {
      return Optional.empty();
    }
    return Optional.of(precedence > 0 ? first : second);
  }

  private static double totalRating(Fighter fighter) {
    IntrinsicRatings ratings = fighter.ratings();
    return ratings.offensiveRating() + ratings.defensiveRating();
  }
}
