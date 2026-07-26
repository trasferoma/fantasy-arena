package it.fantasyarena.combat.battle;

import java.util.List;
import java.util.Optional;

import it.fantasyarena.combat.model.Fighter;

/**
 * Assegna il vincitore libero allo scontro attivo dove la sua squadra è più in inferiorità
 * numerica: quello con il maggior deficit {@code (nemici vivi - alleati vivi)} per la squadra di
 * {@code freeWinner}. A parità di deficit vince l'{@code id} più basso, indipendentemente
 * dall'ordine in cui gli scontri sono passati in {@code activeEngagements}.
 */
public final class OutnumberedAllyAssigner implements FreeWinnerAssigner {

  @Override
  public Optional<Engagement> assign(Fighter freeWinner, List<Engagement> activeEngagements, BattleRoster roster) {
    if (activeEngagements == null || activeEngagements.isEmpty()) {
      return Optional.empty();
    }

    Team ownTeam = roster.teamOf(freeWinner);
    Engagement best = activeEngagements.get(0);
    int bestDeficit = deficitFor(ownTeam, best, roster);

    for (int i = 1; i < activeEngagements.size(); i++) {
      Engagement candidate = activeEngagements.get(i);
      int candidateDeficit = deficitFor(ownTeam, candidate, roster);

      if (isBetter(candidate, candidateDeficit, best, bestDeficit)) {
        best = candidate;
        bestDeficit = candidateDeficit;
      }
    }

    return Optional.of(best);
  }

  private boolean isBetter(Engagement candidate, int candidateDeficit, Engagement currentBest,
      int currentBestDeficit) {
    if (candidateDeficit != currentBestDeficit) {
      return candidateDeficit > currentBestDeficit;
    }
    return candidate.id() < currentBest.id();
  }

  private int deficitFor(Team ownTeam, Engagement engagement, BattleRoster roster) {
    int allyCount = 0;
    int enemyCount = 0;

    for (Fighter participant : engagement.livingParticipants()) {
      if (roster.teamOf(participant) == ownTeam) {
        allyCount++;
      } else {
        enemyCount++;
      }
    }

    return enemyCount - allyCount;
  }
}
