package it.fantasyarena.combat.result;

/**
 * Esito completo di un turno: la voce di log da mostrare e se il difensore ha realmente
 * schivato (dopo un eventuale ripiego per Stamina insufficiente). Il {@code Combat Engine} usa
 * {@code defenderDodged} per l'override deterministico dell'iniziativa: solo la schivata ruba
 * il tempo, non la parata.
 */
public record TurnResult(TurnLogEntry logEntry, boolean defenderDodged) {
}
