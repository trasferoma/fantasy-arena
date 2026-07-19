package it.fantasyarena.combat.result;

import java.util.List;

/**
 * Voce di log di un singolo turno: numero del turno, descrizione testuale di ciò che è
 * accaduto (in italiano, pronta per la stampa su console) e lo snapshot di vita dei due
 * combattenti catturato subito dopo il turno.
 */
public record TurnLogEntry(int turnNumber, String description, List<FighterVitals> vitals) {

  public TurnLogEntry(int turnNumber, String description) {
    this(turnNumber, description, List.of());
  }

  public TurnLogEntry withVitals(List<FighterVitals> vitals) {
    return new TurnLogEntry(turnNumber, description, vitals);
  }
}
