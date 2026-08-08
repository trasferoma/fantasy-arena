package it.fantasyarena.combat.io.trace;

import java.util.List;

import it.fantasyarena.combat.RoundOutcome;
import it.fantasyarena.combat.chronicle.ProgressChronicle;
import it.fantasycombatsystem.result.FighterVitals;

/**
 * La fine di una prova: come è andata, lo stato finale dei combattenti e — solo per una vittoria —
 * la procedura di fine scontro con il loot trovato e la crescita del protagonista.
 *
 * @param trial numero della prova
 * @param progress procedura di fine scontro, {@code null} quando la prova non è stata vinta
 *     (caduta o pareggio non danno né loot né punti caratteristica)
 */
public record TrialEndedEvent(TraceEventKind event, int trial, RoundOutcome outcome, List<FighterVitals> finalVitals,
    ProgressChronicle progress) {

  public TrialEndedEvent(int trial, RoundOutcome outcome, List<FighterVitals> finalVitals,
      ProgressChronicle progress) {
    this(TraceEventKind.TRIAL_ENDED, trial, outcome, finalVitals, progress);
  }

  public TrialEndedEvent {
    finalVitals = List.copyOf(finalVitals);
  }
}
