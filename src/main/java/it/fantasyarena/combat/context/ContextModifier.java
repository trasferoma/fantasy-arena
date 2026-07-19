package it.fantasyarena.combat.context;

/**
 * Modificatore temporaneo applicato durante il turno (es. terreno, meteo, benedizioni).
 * Agisce solo sui valori effettivi del turno: non altera mai i Rating intrinseci.
 */
public record ContextModifier(double offensiveMultiplier, double defensiveMultiplier) {
}
