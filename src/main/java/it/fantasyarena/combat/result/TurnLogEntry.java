package it.fantasyarena.combat.result;

import java.util.List;

/**
 * Voce di log di un singolo turno: numero del turno, descrizione testuale di ciò che è
 * accaduto (in italiano, pronta per la stampa su console), lo snapshot di vita dei due
 * combattenti catturato a inizio turno (prima delle azioni del turno), il report
 * d'iniziativa che ha scelto l'attaccante di questo turno ({@code null} se non disponibile),
 * la Stamina consumata/recuperata da ciascun combattente in questo turno, gli eventuali
 * highlight notevoli tracciati per il colpo del turno (fonte unica di verità, letta anche
 * dalla narrazione finale) e l'esito dell'azione come dato ({@link ActionOutcome},
 * {@code null} se non disponibile), che consente alla presentazione a scena di comporre
 * un'etichetta breve senza interpretare la descrizione.
 */
public record TurnLogEntry(
    int turnNumber,
    String description,
    List<FighterVitals> vitals,
    InitiativeReport initiative,
    List<StaminaChange> staminaChanges,
    List<TurnHighlight> highlights,
    ActionOutcome action) {

  public TurnLogEntry {
    vitals = List.copyOf(vitals);
    staminaChanges = List.copyOf(staminaChanges);
    highlights = List.copyOf(highlights);
  }

  public TurnLogEntry(int turnNumber, String description) {
    this(turnNumber, description, List.of(), null, List.of(), List.of(), null);
  }

  public TurnLogEntry withVitals(List<FighterVitals> vitals) {
    return new TurnLogEntry(turnNumber, description, vitals, initiative, staminaChanges, highlights, action);
  }

  public TurnLogEntry withInitiative(InitiativeReport initiative) {
    return new TurnLogEntry(turnNumber, description, vitals, initiative, staminaChanges, highlights, action);
  }

  public TurnLogEntry withStaminaChanges(List<StaminaChange> staminaChanges) {
    return new TurnLogEntry(turnNumber, description, vitals, initiative, staminaChanges, highlights, action);
  }

  public TurnLogEntry withHighlights(List<TurnHighlight> highlights) {
    return new TurnLogEntry(turnNumber, description, vitals, initiative, staminaChanges, highlights, action);
  }

  public TurnLogEntry withAction(ActionOutcome action) {
    return new TurnLogEntry(turnNumber, description, vitals, initiative, staminaChanges, highlights, action);
  }
}
