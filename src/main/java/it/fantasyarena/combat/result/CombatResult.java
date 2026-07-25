package it.fantasyarena.combat.result;

import java.util.List;
import java.util.Optional;

import it.fantasyarena.combat.model.Fighter;

/**
 * Risultato finale dello scontro: esito, eventuale vincitore (assente solo in caso di
 * {@link CombatOutcome#DRAW}), numero di turni effettivamente giocati, log completo, stato
 * finale (vita e stamina) dei due combattenti e il dettaglio del calcolo a punti (vuoto su
 * {@link CombatOutcome#VICTORY}, valorizzato su {@code TIMEOUT_DECISION}/{@code DRAW}).
 */
public record CombatResult(CombatOutcome outcome, Optional<Fighter> winner, int rounds, List<TurnLogEntry> log,
    List<FighterVitals> finalVitals, List<Scorecard> scorecards) {

  public CombatResult {
    finalVitals = List.copyOf(finalVitals);
    scorecards = List.copyOf(scorecards);
  }
}
