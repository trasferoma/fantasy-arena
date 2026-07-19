package it.fantasyarena.combat.result;

import java.util.List;
import java.util.Optional;

import it.fantasyarena.combat.model.Fighter;

/**
 * Risultato finale dello scontro: esito, eventuale vincitore (assente solo in caso di
 * {@link CombatOutcome#DRAW}), numero di turni effettivamente giocati e log completo.
 */
public record CombatResult(CombatOutcome outcome, Optional<Fighter> winner, int rounds, List<TurnLogEntry> log) {
}
