package it.fantasyarena.combat.result;

import java.util.List;
import java.util.Optional;

import it.fantasyarena.combat.model.Fighter;

/**
 * Risultato finale dello scontro: esito, eventuale vincitore (assente solo in caso di
 * {@link CombatOutcome#DRAW}), numero di turni effettivamente giocati, log completo e
 * stato finale (vita e stamina) dei due combattenti.
 */
public record CombatResult(CombatOutcome outcome, Optional<Fighter> winner, int rounds, List<TurnLogEntry> log,
    List<FighterVitals> finalVitals) {

  public CombatResult {
    finalVitals = List.copyOf(finalVitals);
  }
}
