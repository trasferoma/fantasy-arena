package it.fantasyarena.combat.model;

/**
 * Rating intrinseci e pool massimi del combattente. Calcolati una sola volta alla
 * costruzione del {@link Fighter} da una {@code RatingStrategy} e mai più modificati
 * durante lo scontro: lo stato mutabile vive separato in {@link FighterState}.
 */
public record IntrinsicRatings(double offensiveRating, double defensiveRating, int maxHealth, int maxStamina) {
}
