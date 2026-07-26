package it.fantasyarena.combat.battle;

import java.util.List;

import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * Uno scambio giocato in uno scontro durante un round. Solo dati per il log: {@code attackerIndex}
 * e {@code targetIndex} sono posizioni in {@link BattleRoster#all()} (non nomi, per restare
 * immuni agli omonimi), {@code participantIndexes} sono i partecipanti vivi a inizio round di
 * quello scontro, in ordine di ingresso.
 */
public record EngagementTurn(int engagementId, int attackerIndex, int targetIndex,
    List<Integer> participantIndexes, TurnLogEntry turn) {

  public EngagementTurn {
    participantIndexes = List.copyOf(participantIndexes);
  }
}
