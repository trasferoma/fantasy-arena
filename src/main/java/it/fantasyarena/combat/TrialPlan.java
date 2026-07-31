package it.fantasyarena.combat;

import java.util.List;

/**
 * Il percorso dell'arena: dieci stazioni in fila, dalla prima prova allo specchio finale. È
 * l'unico posto che sa com'è fatta la corsa — quante prove, contro quanti, con quale monte punti —
 * e {@link Arena} lo legge senza contenerlo: le scelte del protagonista restano tutte di
 * {@code HeroBrain}, la struttura della corsa non è una sua scelta.
 *
 * <p>Il percorso è cablato e deterministico: nessuna estrazione, nessuna configurazione da riga di
 * comando. Cresce di monte punti stazione dopo stazione (curva {@code 15 + 3 * (numero - 1)}) e di
 * numero di sfidanti (uno, poi due, poi tre), fino allo specchio finale che non dichiara un monte
 * punti proprio ma ricalca il protagonista com'è cresciuto nelle nove prove precedenti.
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
        TrialStation.generated(4, "due contro uno", 2, 24),
        TrialStation.generated(5, "di nuovo due insieme", 2, 27),
        TrialStation.generated(6, "la terza coppia", 2, 30),
        TrialStation.generated(7, "tre contro uno", 3, 33),
        TrialStation.generated(8, "ancora tre insieme", 3, 36),
        TrialStation.generated(9, "l'ultimo assalto in tre", 3, 39),
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
