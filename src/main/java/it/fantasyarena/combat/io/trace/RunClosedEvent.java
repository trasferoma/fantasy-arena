package it.fantasyarena.combat.io.trace;

import it.fantasyarena.combat.RoundOutcome;

/**
 * La chiusura della corsa: com'è finita l'ultima prova giocata e a quale numero, la stessa coppia
 * di dati di {@code RunConclusion}.
 */
public record RunClosedEvent(TraceEventKind event, RoundOutcome outcome, int lastTrial) {

  public RunClosedEvent(RoundOutcome outcome, int lastTrial) {
    this(TraceEventKind.RUN_CLOSED, outcome, lastTrial);
  }
}
