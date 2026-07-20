package it.fantasyarena.combat.result;

/**
 * Stamina consumata e recuperata da un combattente in un singolo turno, come ammontari
 * effettivi (già passati per floor/cap) e non negativi. Pensato solo per il log: non
 * sostituisce lo stato corrente esposto da {@code FighterState}.
 */
public record StaminaChange(String name, int consumed, int recovered) {
}
