package it.fantasyarena.combat.battle;

import java.util.List;
import java.util.Optional;

import it.fantasyarena.combat.result.CombatOutcome;
import it.fantasyarena.combat.result.FighterVitals;
import it.fantasyarena.combat.result.Scorecard;

/**
 * Risultato finale della battaglia: esito, eventuale squadra vincitrice (assente solo in caso di
 * {@link CombatOutcome#DRAW}), numero di round effettivamente giocati, log round per round, stato
 * finale (vita e stamina) di ogni combattente del roster e il dettaglio del calcolo a punti (vuoto
 * su {@link CombatOutcome#VICTORY}, valorizzato su {@code TIMEOUT_DECISION}/{@code DRAW}) sia per
 * combattente sia aggregato per squadra.
 */
public record BattleResult(CombatOutcome outcome, Optional<Team> winningTeam, int rounds,
    List<RoundLogEntry> roundLog, List<FighterVitals> finalVitals, List<Scorecard> scorecards,
    List<TeamScore> teamScores) {

  public BattleResult {
    roundLog = List.copyOf(roundLog);
    finalVitals = List.copyOf(finalVitals);
    scorecards = List.copyOf(scorecards);
    teamScores = List.copyOf(teamScores);
  }
}
