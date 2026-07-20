package it.fantasyarena.combat.result;

import java.util.List;

/**
 * Esito loggabile della decisione d'iniziativa di un turno: il breakdown di entrambi i
 * combattenti, il nome di chi vince per punteggio ({@code scoreWinnerName}, sempre calcolato
 * dalla formula, indipendente dall'override), il nome di chi agisce davvero come prossimo
 * attaccante ({@code chosenName}, che può divergere dal vincitore per punteggio sotto override)
 * e l'eventuale override che ha bypassato la formula. Nessun riferimento al {@code Fighter}
 * mutabile del model: solo dati per il log.
 */
public record InitiativeReport(
    List<InitiativeBreakdown> breakdowns, String scoreWinnerName, String chosenName, InitiativeOverride override) {

  public InitiativeReport {
    breakdowns = List.copyOf(breakdowns);
  }
}
