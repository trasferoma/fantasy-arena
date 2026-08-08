package it.fantasyarena.combat.io.trace;

import java.util.List;

import it.fantasyarena.combat.chronicle.ChallengerBudgetChronicle;
import it.fantasyarena.combat.chronicle.CombatantSnapshot;
import it.fantasyarena.combat.chronicle.TrialShape;

/**
 * L'inizio di una prova: numero, descrizione, forma dello scontro, lo schieramento fotografato
 * prima di combattere e il monte punti già scontato dalla fortuna con cui gli sfidanti sono nati.
 *
 * @param trial numero della prova
 * @param budget monte punti della stazione, {@code null} per la stazione dello specchio, che non
 *     dichiara un monte proprio
 */
public record TrialStartedEvent(TraceEventKind event, int trial, String description, TrialShape shape,
    List<CombatantSnapshot> roster, ChallengerBudgetChronicle budget) {

  public TrialStartedEvent(int trial, String description, TrialShape shape, List<CombatantSnapshot> roster,
      ChallengerBudgetChronicle budget) {
    this(TraceEventKind.TRIAL_STARTED, trial, description, shape, roster, budget);
  }

  public TrialStartedEvent {
    roster = List.copyOf(roster);
  }
}
