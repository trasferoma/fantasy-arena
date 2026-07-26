package it.fantasyarena.combat.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.testsupport.CombatFixtures;

/**
 * Verifica {@link PairwiseEngagementPlanner}: accoppiamento 1 contro 1 in ordine di roster e
 * distribuzione round-robin del surplus, per i casi 1v1, 2v2, 2v1, 3v2 e 3v3. Le asserzioni
 * confrontano sempre l'identità dei partecipanti, mai il nome.
 */
class PairwiseEngagementPlannerTest {

  private final PairwiseEngagementPlanner planner = new PairwiseEngagementPlanner();

  @Test
  void openingEngagements_1v1_unoScontro() {
    Fighter a0 = CombatFixtures.createFighter("A0", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);
    BattleRoster roster = rosterOf(List.of(a0), List.of(b0));

    List<Engagement> engagements = planner.openingEngagements(roster);

    assertEquals(1, engagements.size());
    assertParticipants(engagements.get(0), 0, a0, b0);
  }

  @Test
  void openingEngagements_2v2_dueScontriUnoAUno() {
    Fighter a0 = CombatFixtures.createFighter("A0", 15, 15, 15, 15, 15, 10, 10);
    Fighter a1 = CombatFixtures.createFighter("A1", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);
    Fighter b1 = CombatFixtures.createFighter("B1", 15, 15, 15, 15, 15, 10, 10);
    BattleRoster roster = rosterOf(List.of(a0, a1), List.of(b0, b1));

    List<Engagement> engagements = planner.openingEngagements(roster);

    assertEquals(2, engagements.size());
    assertParticipants(engagements.get(0), 0, a0, b0);
    assertParticipants(engagements.get(1), 1, a1, b1);
  }

  @Test
  void openingEngagements_2v1_unoScontroConSurplus() {
    Fighter a0 = CombatFixtures.createFighter("A0", 15, 15, 15, 15, 15, 10, 10);
    Fighter a1 = CombatFixtures.createFighter("A1", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);
    BattleRoster roster = rosterOf(List.of(a0, a1), List.of(b0));

    List<Engagement> engagements = planner.openingEngagements(roster);

    assertEquals(1, engagements.size());
    assertParticipants(engagements.get(0), 0, a0, b0, a1);
  }

  @Test
  void openingEngagements_3v2_ilSurplusVaAlPrimoScontro() {
    Fighter a0 = CombatFixtures.createFighter("A0", 15, 15, 15, 15, 15, 10, 10);
    Fighter a1 = CombatFixtures.createFighter("A1", 15, 15, 15, 15, 15, 10, 10);
    Fighter a2 = CombatFixtures.createFighter("A2", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);
    Fighter b1 = CombatFixtures.createFighter("B1", 15, 15, 15, 15, 15, 10, 10);
    BattleRoster roster = rosterOf(List.of(a0, a1, a2), List.of(b0, b1));

    List<Engagement> engagements = planner.openingEngagements(roster);

    assertEquals(2, engagements.size());
    assertParticipants(engagements.get(0), 0, a0, b0, a2);
    assertParticipants(engagements.get(1), 1, a1, b1);
  }

  @Test
  void openingEngagements_3v3_treScontriUnoAUno() {
    Fighter a0 = CombatFixtures.createFighter("A0", 15, 15, 15, 15, 15, 10, 10);
    Fighter a1 = CombatFixtures.createFighter("A1", 15, 15, 15, 15, 15, 10, 10);
    Fighter a2 = CombatFixtures.createFighter("A2", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);
    Fighter b1 = CombatFixtures.createFighter("B1", 15, 15, 15, 15, 15, 10, 10);
    Fighter b2 = CombatFixtures.createFighter("B2", 15, 15, 15, 15, 15, 10, 10);
    BattleRoster roster = rosterOf(List.of(a0, a1, a2), List.of(b0, b1, b2));

    List<Engagement> engagements = planner.openingEngagements(roster);

    assertEquals(3, engagements.size());
    assertParticipants(engagements.get(0), 0, a0, b0);
    assertParticipants(engagements.get(1), 1, a1, b1);
    assertParticipants(engagements.get(2), 2, a2, b2);
  }

  private BattleRoster rosterOf(List<Fighter> teamA, List<Fighter> teamB) {
    BattleSetup setup = BattleSetup.of(List.of(teamA, teamB));
    return BattleRoster.of(setup.teams());
  }

  private void assertParticipants(Engagement engagement, int expectedId, Fighter... expectedParticipants) {
    assertEquals(expectedId, engagement.id());
    List<Fighter> participants = engagement.participants();
    assertEquals(expectedParticipants.length, participants.size());
    for (int i = 0; i < expectedParticipants.length; i++) {
      assertSame(expectedParticipants[i], participants.get(i));
    }
  }
}
