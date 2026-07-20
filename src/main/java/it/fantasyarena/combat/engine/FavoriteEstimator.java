package it.fantasyarena.combat.engine;

import java.util.Optional;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.IntrinsicRatings;

/**
 * Stima pura (nessun RNG) del favorito pre-scontro tra due combattenti, dai soli Rating
 * intrinseci: vince chi ha la somma Offensive+Defensive Rating più alta; a parità decide
 * {@code maxHealth}, poi {@code maxStamina}; se anche questi coincidono lo scontro è
 * equilibrato (nessun favorito netto). Oltre al favorito, {@link #assess(Fighter, Fighter)}
 * espone anche la base della decisione e i valori confrontati, per motivare il pronostico.
 */
public final class FavoriteEstimator {

  /**
   * Criterio che ha deciso il favorito, in ordine di precedenza; {@link #EVEN} quando i due
   * combattenti sono equivalenti su tutti i criteri.
   */
  public enum Basis {
    RATING,
    HEALTH,
    STAMINA,
    EVEN
  }

  /**
   * Verdetto del pronostico: il favorito (assente se lo scontro è equilibrato), il criterio
   * che lo ha deciso e i valori confrontati (del favorito e dell'avversario) su quel criterio.
   */
  public record Verdict(Optional<Fighter> favorite, Basis basis, double favoriteValue, double opponentValue) {
  }

  public Verdict assess(Fighter first, Fighter second) {
    int ratingComparison = Double.compare(totalRating(first), totalRating(second));
    if (ratingComparison != 0) {
      return decided(first, second, ratingComparison, Basis.RATING, totalRating(first), totalRating(second));
    }

    int healthComparison = Integer.compare(first.ratings().maxHealth(), second.ratings().maxHealth());
    if (healthComparison != 0) {
      return decided(first, second, healthComparison, Basis.HEALTH,
          first.ratings().maxHealth(), second.ratings().maxHealth());
    }

    int staminaComparison = Integer.compare(first.ratings().maxStamina(), second.ratings().maxStamina());
    if (staminaComparison != 0) {
      return decided(first, second, staminaComparison, Basis.STAMINA,
          first.ratings().maxStamina(), second.ratings().maxStamina());
    }

    return new Verdict(Optional.empty(), Basis.EVEN, totalRating(first), totalRating(second));
  }

  public Optional<Fighter> favorite(Fighter first, Fighter second) {
    return assess(first, second).favorite();
  }

  private Verdict decided(Fighter first, Fighter second, int comparison, Basis basis, double firstValue,
      double secondValue) {

    boolean firstWins = comparison > 0;
    Fighter favorite = firstWins ? first : second;
    double favoriteValue = firstWins ? firstValue : secondValue;
    double opponentValue = firstWins ? secondValue : firstValue;
    return new Verdict(Optional.of(favorite), basis, favoriteValue, opponentValue);
  }

  private static double totalRating(Fighter fighter) {
    IntrinsicRatings ratings = fighter.ratings();
    return ratings.offensiveRating() + ratings.defensiveRating();
  }
}
