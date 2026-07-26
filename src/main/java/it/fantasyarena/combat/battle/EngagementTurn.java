package it.fantasyarena.combat.battle;

import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * Uno scambio giocato in uno scontro durante un round. Solo dati per il log.
 */
public record EngagementTurn(int engagementId, String attackerName, String targetName, TurnLogEntry turn) {
}
