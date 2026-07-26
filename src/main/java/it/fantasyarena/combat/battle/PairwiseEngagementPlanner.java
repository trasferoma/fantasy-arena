package it.fantasyarena.combat.battle;

import java.util.ArrayList;
import java.util.List;

import it.fantasyarena.combat.model.Fighter;

/**
 * Piano di apertura "a coppie": accoppia un combattente per squadra, in ordine di roster, fino a
 * esaurire la squadra più piccola. I combattenti in surplus della squadra più numerosa vengono
 * distribuiti round-robin sugli scontri già aperti, a partire dall'id 0. Richiede esattamente
 * 2 squadre: un piano "a coppie" fra più di 2 squadre non è definito da questa fase.
 */
public final class PairwiseEngagementPlanner implements EngagementPlanner {

  private static final int TEAM_COUNT = 2;

  @Override
  public List<Engagement> openingEngagements(BattleRoster roster) {
    validateTeamCount(roster);

    List<Fighter> firstTeamMembers = roster.teams().get(0).members();
    List<Fighter> secondTeamMembers = roster.teams().get(1).members();
    int pairedCount = Math.min(firstTeamMembers.size(), secondTeamMembers.size());

    List<Engagement> engagements = openPairedEngagements(firstTeamMembers, secondTeamMembers, pairedCount);
    List<Fighter> surplus = surplusBeyond(firstTeamMembers, secondTeamMembers, pairedCount);
    distributeRoundRobin(surplus, engagements);

    return engagements;
  }

  private List<Engagement> openPairedEngagements(List<Fighter> firstTeamMembers, List<Fighter> secondTeamMembers,
      int pairedCount) {
    List<Engagement> engagements = new ArrayList<>(pairedCount);
    for (int i = 0; i < pairedCount; i++) {
      engagements.add(new Engagement(i, List.of(firstTeamMembers.get(i), secondTeamMembers.get(i))));
    }
    return engagements;
  }

  private List<Fighter> surplusBeyond(List<Fighter> firstTeamMembers, List<Fighter> secondTeamMembers,
      int pairedCount) {
    List<Fighter> largerTeam =
        (firstTeamMembers.size() > secondTeamMembers.size()) ? firstTeamMembers : secondTeamMembers;
    return largerTeam.subList(pairedCount, largerTeam.size());
  }

  private void distributeRoundRobin(List<Fighter> surplus, List<Engagement> engagements) {
    for (int i = 0; i < surplus.size(); i++) {
      Engagement target = engagements.get(i % engagements.size());
      target.join(surplus.get(i));
    }
  }

  private void validateTeamCount(BattleRoster roster) {
    if (roster.teams().size() != TEAM_COUNT) {
      throw new IllegalArgumentException(
          "PairwiseEngagementPlanner requires exactly " + TEAM_COUNT + " teams, was: " + roster.teams().size());
    }
  }
}
