package it.fantasyarena.combat.result;

/**
 * Esito completo di un turno: la voce di log da mostrare e l'eventuale override d'iniziativa
 * prodotto da quel turno. Il {@code CombatEngine} usa {@code override} per decidere l'iniziativa
 * del turno successivo fuori dalla formula: una schivata riuscita ({@code DODGE_STEAL}) o un
 * riposo ({@code REST_YIELD}) forzano deterministicamente il difensore corrente come prossimo
 * attaccante; {@code NONE} lascia decidere la formula.
 */
public record TurnResult(TurnLogEntry logEntry, InitiativeOverride override) {
}
