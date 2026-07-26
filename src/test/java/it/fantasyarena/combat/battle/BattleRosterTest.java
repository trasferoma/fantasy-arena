package it.fantasyarena.combat.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.testsupport.CombatFixtures;

/**
 * Verifica {@link BattleRoster}: ordine di {@link BattleRoster#all()}, correlazione
 * combattente -> squadra per identità (anche con nomi uguali), esclusione di alleati e morti da
 * {@link BattleRoster#livingEnemiesOf}, e {@link BattleRoster#teamsStillStanding()} dopo
 * l'azzeramento di una squadra.
 */
class BattleRosterTest {

  @Test
  void all_rispettaOrdineDiSquadraEDiRoster() {
    Fighter a0 = CombatFixtures.createFighter("A0", 15, 15, 15, 15, 15, 10, 10);
    Fighter a1 = CombatFixtures.createFighter("A1", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);
    BattleSetup setup = BattleSetup.of(List.of(List.of(a0, a1), List.of(b0)));

    BattleRoster roster = BattleRoster.of(setup.teams());

    assertEquals(List.of(a0, a1, b0), roster.all());
  }

  @Test
  void teamOf_correlaPerIdentitaAncheConNomiUguali() {
    Fighter a0 = CombatFixtures.createFighter("Twin", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("Twin", 15, 15, 15, 15, 15, 10, 10);
    BattleSetup setup = BattleSetup.of(List.of(List.of(a0), List.of(b0)));

    BattleRoster roster = BattleRoster.of(setup.teams());

    assertSame(setup.teams().get(0), roster.teamOf(a0));
    assertSame(setup.teams().get(1), roster.teamOf(b0));
  }

  @Test
  void teamOf_combattenteSconosciuto_lanciaEccezione() {
    Fighter a0 = CombatFixtures.createFighter("A0", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);
    Fighter unknown = CombatFixtures.createFighter("Unknown", 15, 15, 15, 15, 15, 10, 10);
    BattleSetup setup = BattleSetup.of(List.of(List.of(a0), List.of(b0)));

    BattleRoster roster = BattleRoster.of(setup.teams());

    assertThrows(IllegalArgumentException.class, () -> roster.teamOf(unknown));
  }

  @Test
  void livingEnemiesOf_escludeAlleatiEMorti() {
    Fighter a0 = CombatFixtures.createFighter("A0", 15, 15, 15, 15, 15, 10, 10);
    Fighter a1 = CombatFixtures.createFighter("A1", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);
    Fighter b1 = CombatFixtures.createFighter("B1", 15, 15, 15, 15, 15, 10, 10);
    BattleSetup setup = BattleSetup.of(List.of(List.of(a0, a1), List.of(b0, b1)));
    BattleRoster roster = BattleRoster.of(setup.teams());
    b1.state().applyDamage(b1.ratings().maxHealth());

    List<Fighter> enemies = roster.livingEnemiesOf(a0);

    assertEquals(List.of(b0), enemies);
  }

  @Test
  void teamsStillStanding_escludeLaSquadraAzzerata() {
    Fighter a0 = CombatFixtures.createFighter("A0", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);
    BattleSetup setup = BattleSetup.of(List.of(List.of(a0), List.of(b0)));
    BattleRoster roster = BattleRoster.of(setup.teams());
    b0.state().applyDamage(b0.ratings().maxHealth());

    List<Team> standing = roster.teamsStillStanding();

    assertEquals(1, standing.size());
    assertSame(setup.teams().get(0), standing.get(0));
  }
}
