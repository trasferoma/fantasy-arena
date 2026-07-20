package it.fantasyarena.combat.result;

import java.util.List;

/**
 * Esito loggabile della decisione d'iniziativa di un turno: il breakdown di entrambi i
 * combattenti, il nome di chi vince per punteggio ({@code scoreWinnerName}), il nome di chi
 * agisce davvero come prossimo attaccante ({@code chosenName}) e l'eventuale override che ha
 * bypassato la formula. Sotto override (il turno precedente ha prodotto una schivata riuscita o
 * un riposo) il test a punteggio non viene eseguito: {@code breakdowns} è vuoto e
 * {@code scoreWinnerName} vale come {@code chosenName}, un segnaposto non calcolato dalla
 * formula e non mostrato dalla presentazione (che decide in base a {@code override}). Nessun
 * riferimento al {@code Fighter} mutabile del model: solo dati per il log.
 */
public record InitiativeReport(
    List<InitiativeBreakdown> breakdowns, String scoreWinnerName, String chosenName, InitiativeOverride override) {

  public InitiativeReport {
    breakdowns = List.copyOf(breakdowns);
  }
}
