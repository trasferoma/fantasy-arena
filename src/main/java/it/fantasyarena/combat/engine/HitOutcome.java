package it.fantasyarena.combat.engine;

/**
 * Esito puro del test per colpire: se l'attacco va a segno e se è critico.
 */
public record HitOutcome(boolean hit, boolean critical) {
}
