package it.fantasyarena.combat.battle;

import java.util.List;

import it.fantasyarena.combat.result.FighterVitals;

/**
 * Un round: ogni scontro attivo ha giocato uno scambio. {@code events} sono le note di round che
 * non appartengono a nessuno scambio (riassegnazioni del vincitore libero, chiusure di scontro).
 */
public record RoundLogEntry(int roundNumber, List<EngagementTurn> turns, List<FighterVitals> vitals,
    List<String> events) {

  public RoundLogEntry {
    turns = List.copyOf(turns);
    vitals = List.copyOf(vitals);
    events = List.copyOf(events);
  }
}
