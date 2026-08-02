package it.fantasyarena.combat;

import java.util.List;

/**
 * Il percorso dell'arena: dieci stazioni in fila, dalla prima prova allo specchio finale. È
 * l'unico posto che sa com'è fatta la corsa — quante prove, contro quanti, con quale monte punti —
 * e {@link Arena} lo legge senza contenerlo: le scelte del protagonista restano tutte di
 * {@code HeroBrain}, la struttura della corsa non è una sua scelta.
 *
 * <p>Il percorso è cablato e deterministico: nessuna estrazione, nessuna configurazione da riga di
 * comando. Cresce di numero di sfidanti stazione dopo stazione (uno, poi due, poi tre), fino allo
 * specchio finale che non dichiara un monte punti proprio ma ricalca il protagonista com'è
 * cresciuto nelle nove prove precedenti.
 *
 * <p>{@link TrialStation#characteristicPoints()} non è più il monte del singolo sfidante: è il
 * monte dell'<strong>intero schieramento</strong>, che {@code FighterFactory.createChallengers}
 * ripartisce fra gli sfidanti della stazione. La regola di derivazione è
 * {@code monteEroe(N) × moltiplicatore(numeroSfidanti)}, con {@code monteEroe(N) = 15 + 3 * (N - 1)}
 * — la stessa curva di crescita del protagonista — e moltiplicatore {@code 1.0} con un sfidante,
 * {@code 1.3} con due, {@code 1.5} con tre. Il moltiplicatore è <strong>documentato, non
 * implementato</strong>: i nove monti restano letterali cablati qui sotto, perché il percorso resta
 * un dato esplicito e leggibile, non una formula valutata a runtime. È inferiore al numero puro di
 * sfidanti perché sconta l'economia di azioni: {@code N} avversari attaccano {@code N} volte per
 * turno mentre il protagonista attacca una volta sola, quindi a parità di monte punti complessivo
 * lo schieramento numeroso vincerebbe comunque.
 */
public record TrialPlan(List<TrialStation> stations) {

  public TrialPlan {
    stations = List.copyOf(stations);
  }

  public static TrialPlan standard() {
    return new TrialPlan(List.of(
        TrialStation.generated(1, "il primo avversario", 1, 15),
        TrialStation.generated(2, "un altro sfidante, ancora uno solo", 1, 18),
        TrialStation.generated(3, "il terzo scontro alla pari", 1, 21),
        TrialStation.generated(4, "due contro uno", 2, 31),
        TrialStation.generated(5, "di nuovo due insieme", 2, 35),
        TrialStation.generated(6, "la terza coppia", 2, 39),
        TrialStation.generated(7, "tre contro uno", 3, 50),
        TrialStation.generated(8, "ancora tre insieme", 3, 54),
        TrialStation.generated(9, "l'ultimo assalto in tre", 3, 59),
        TrialStation.mirror(10, "lo sfidante speculare, armato meglio")));
  }

  /**
   * La lunghezza del percorso: quante stazioni lo compongono, previste a prescindere da quante ne
   * vengano davvero giocate.
   */
  public int length() {
    return stations.size();
  }
}
