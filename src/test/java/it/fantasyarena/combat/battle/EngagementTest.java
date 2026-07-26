package it.fantasyarena.combat.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.InitiativeOverride;
import it.fantasyarena.combat.testsupport.CombatFixtures;

/**
 * Verifica {@link Engagement}: ordine d'iniziativa con e senza ultimo attore, stato di scontro
 * concluso/attivo, rifiuto del partecipante duplicato e registrazione dello scambio.
 */
class EngagementTest {

  @Test
  void initiativeOrder_senzaUltimoAttore_eOrdineDiIngresso() {
    Fighter a = CombatFixtures.createFighter("A", 15, 15, 15, 15, 15, 10, 10);
    Fighter b = CombatFixtures.createFighter("B", 15, 15, 15, 15, 15, 10, 10);
    Engagement engagement = new Engagement(0, List.of(a, b));

    assertEquals(List.of(a, b), engagement.initiativeOrder());
  }

  @Test
  void initiativeOrder_conUltimoAttore_loMetteInTesta() {
    Fighter a = CombatFixtures.createFighter("A", 15, 15, 15, 15, 15, 10, 10);
    Fighter b = CombatFixtures.createFighter("B", 15, 15, 15, 15, 15, 10, 10);
    Engagement engagement = new Engagement(0, List.of(a, b));

    engagement.recordExchange(b, a, InitiativeOverride.NONE);

    assertEquals(List.of(b, a), engagement.initiativeOrder());
  }

  @Test
  void initiativeOrder_conUltimoAttoreMorto_ricadeSullOrdineDiIngresso() {
    Fighter a = CombatFixtures.createFighter("A", 15, 15, 15, 15, 15, 10, 10);
    Fighter b = CombatFixtures.createFighter("B", 15, 15, 15, 15, 15, 10, 10);
    Engagement engagement = new Engagement(0, List.of(a, b));
    engagement.recordExchange(b, a, InitiativeOverride.NONE);

    b.state().applyDamage(b.ratings().maxHealth());

    assertEquals(List.of(a), engagement.initiativeOrder());
  }

  @Test
  void isActive_conDueSquadreRappresentateFraIVivi_eVero() {
    Fighter a = CombatFixtures.createFighter("A", 15, 15, 15, 15, 15, 10, 10);
    Fighter b = CombatFixtures.createFighter("B", 15, 15, 15, 15, 15, 10, 10);
    BattleSetup setup = BattleSetup.duel(a, b);
    BattleRoster roster = BattleRoster.of(setup.teams());
    Engagement engagement = new Engagement(0, List.of(a, b));

    assertTrue(engagement.isActive(roster));
  }

  @Test
  void isActive_conSoliAlleatiVivi_eFalso() {
    Fighter a0 = CombatFixtures.createFighter("A0", 15, 15, 15, 15, 15, 10, 10);
    Fighter a1 = CombatFixtures.createFighter("A1", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);
    BattleSetup setup = BattleSetup.of(List.of(List.of(a0, a1), List.of(b0)));
    BattleRoster roster = BattleRoster.of(setup.teams());
    Engagement engagement = new Engagement(0, List.of(a0, a1, b0));
    b0.state().applyDamage(b0.ratings().maxHealth());

    assertFalse(engagement.isActive(roster));
  }

  @Test
  void isActive_2v1_eVero() {
    Fighter a0 = CombatFixtures.createFighter("A0", 15, 15, 15, 15, 15, 10, 10);
    Fighter a1 = CombatFixtures.createFighter("A1", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);
    BattleSetup setup = BattleSetup.of(List.of(List.of(a0, a1), List.of(b0)));
    BattleRoster roster = BattleRoster.of(setup.teams());
    Engagement engagement = new Engagement(0, List.of(a0, a1, b0));

    assertTrue(engagement.isActive(roster));
  }

  @Test
  void join_partecipanteDuplicato_lanciaEccezione() {
    Fighter a = CombatFixtures.createFighter("A", 15, 15, 15, 15, 15, 10, 10);
    Fighter b = CombatFixtures.createFighter("B", 15, 15, 15, 15, 15, 10, 10);
    Engagement engagement = new Engagement(0, List.of(a, b));

    assertThrows(IllegalArgumentException.class, () -> engagement.join(a));
  }

  @Test
  void recordExchange_aggiornaUltimoAttoreBersaglioEOverride() {
    Fighter a = CombatFixtures.createFighter("A", 15, 15, 15, 15, 15, 10, 10);
    Fighter b = CombatFixtures.createFighter("B", 15, 15, 15, 15, 15, 10, 10);
    Engagement engagement = new Engagement(0, List.of(a, b));

    assertNull(engagement.lastActor());
    assertNull(engagement.currentTargetOf(a));
    assertEquals(InitiativeOverride.NONE, engagement.pendingOverride());

    engagement.recordExchange(a, b, InitiativeOverride.DODGE_STEAL);

    assertSame(a, engagement.lastActor());
    assertSame(b, engagement.currentTargetOf(a));
    assertEquals(InitiativeOverride.DODGE_STEAL, engagement.pendingOverride());
  }
}
