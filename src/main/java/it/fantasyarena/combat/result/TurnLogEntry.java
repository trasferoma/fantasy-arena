package it.fantasyarena.combat.result;

import java.util.List;

/**
 * Voce di log di un singolo turno: numero del turno, descrizione testuale di ciò che è
 * accaduto (in italiano, pronta per la stampa su console), lo snapshot di vita dei due
 * combattenti catturato a inizio turno (prima delle azioni del turno), il report
 * d'iniziativa che ha scelto l'attaccante di questo turno ({@code null} se non disponibile)
 * e la Stamina consumata/recuperata da ciascun combattente in questo turno.
 */
public record TurnLogEntry(
    int turnNumber,
    String description,
    List<FighterVitals> vitals,
    InitiativeReport initiative,
    List<StaminaChange> staminaChanges) {

  public TurnLogEntry {
    vitals = List.copyOf(vitals);
    staminaChanges = List.copyOf(staminaChanges);
  }

  public TurnLogEntry(int turnNumber, String description) {
    this(turnNumber, description, List.of(), null, List.of());
  }

  public TurnLogEntry withVitals(List<FighterVitals> vitals) {
    return new TurnLogEntry(turnNumber, description, vitals, initiative, staminaChanges);
  }

  public TurnLogEntry withInitiative(InitiativeReport initiative) {
    return new TurnLogEntry(turnNumber, description, vitals, initiative, staminaChanges);
  }

  public TurnLogEntry withStaminaChanges(List<StaminaChange> staminaChanges) {
    return new TurnLogEntry(turnNumber, description, vitals, initiative, staminaChanges);
  }
}
