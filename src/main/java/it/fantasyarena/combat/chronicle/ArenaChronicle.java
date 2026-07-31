package it.fantasyarena.combat.chronicle;

import java.util.List;

/**
 * Il registro completo di una corsa nell'arena: l'ingresso del protagonista, la lunghezza prevista
 * del percorso, una voce per ogni prova giocata (mai per quelle non giocate) e la conclusione. Soli
 * dati, nessuna stringa di presentazione: le frasi restano dei renderer di {@code combat.io}, che
 * questo package non conosce e da cui non dipende.
 *
 * <p>{@link #plannedTrials()} esiste perché il lettore di questa cronaca ha solo il JSON, non la
 * tabella Java del percorso: senza questo campo dovrebbe indovinare quante prove erano previste dal
 * numero di voci giocate, che è esattamente lo spoiler da evitare — una corsa persa alla prima prova
 * mostrerebbe «Prova 1/1» e rivelerebbe da sola che finisce lì. Il valore resta quello della
 * lunghezza intera del percorso, anche quando la corsa si chiude molto prima della fine.
 *
 * <p>Esiste perché una partita di questo gioco non ha scelte del giocatore: {@code Arena.run()}
 * genera, combatte e fa crescere il protagonista senza chiedere niente, quindi è completamente
 * determinata nell'istante in cui viene giocata. Una UI non deve pilotare un motore turno per
 * turno: le basta leggere un registro già scritto, avanti e indietro a piacere. Il mapper che
 * costruisce questi record ({@link ChronicleMapper}) spiega perché fotografa invece di
 * referenziare.
 */
public record ArenaChronicle(HeroSnapshot protagonist, int plannedTrials, List<TrialChronicle> trials,
    RunConclusion conclusion) {

  public ArenaChronicle {
    trials = List.copyOf(trials);
    if (plannedTrials < trials.size()) {
      throw new IllegalArgumentException(
          "plannedTrials (" + plannedTrials + ") must be >= trials played (" + trials.size() + ")");
    }
  }
}
