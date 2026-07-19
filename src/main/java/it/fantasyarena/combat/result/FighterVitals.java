package it.fantasyarena.combat.result;

/**
 * Snapshot immutabile di vita e stamina di un combattente catturato subito dopo un
 * turno, prima che lo stato mutabile del {@code Fighter} venga eventualmente
 * modificato dai turni successivi.
 */
public record FighterVitals(String name, int currentHealth, int maxHealth, int currentStamina, int maxStamina) {
}
