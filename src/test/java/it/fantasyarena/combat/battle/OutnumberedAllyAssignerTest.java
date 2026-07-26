package it.fantasyarena.combat.battle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.testsupport.CombatFixtures;

/**
 * Verifica {@link OutnumberedAllyAssigner}: scelta dello scontro attivo con il maggior deficit
 * numerico per la squadra del vincitore libero, tie-break sull'id più basso (indipendente
 * dall'ordine della lista in ingresso), e nessuna assegnazione senza scontri attivi.
 */
class OutnumberedAllyAssignerTest {

  private final OutnumberedAllyAssigner assigner = new OutnumberedAllyAssigner();

  @Test
  void assign_sceglieLoScontroConMaggiorDeficit() {
    Fighter a0 = CombatFixtures.createFighter("A0", 15, 15, 15, 15, 15, 10, 10);
    Fighter a1 = CombatFixtures.createFighter("A1", 15, 15, 15, 15, 15, 10, 10);
    Fighter freeWinner = CombatFixtures.createFighter("FreeWinner", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);
    Fighter b1 = CombatFixtures.createFighter("B1", 15, 15, 15, 15, 15, 10, 10);
    Fighter b2 = CombatFixtures.createFighter("B2", 15, 15, 15, 15, 15, 10, 10);
    BattleSetup setup = BattleSetup.of(List.of(List.of(a0, a1, freeWinner), List.of(b0, b1, b2)));
    BattleRoster roster = BattleRoster.of(setup.teams());
    Engagement lowDeficit = new Engagement(0, List.of(a0, b0));
    Engagement highDeficit = new Engagement(1, List.of(a1, b1, b2));

    Optional<Engagement> chosen = assigner.assign(freeWinner, List.of(lowDeficit, highDeficit), roster);

    assertTrue(chosen.isPresent());
    assertSame(highDeficit, chosen.get());
  }

  @Test
  void assign_pareggioDiDeficit_sceglieLIdPiuBasso() {
    Fighter a0 = CombatFixtures.createFighter("A0", 15, 15, 15, 15, 15, 10, 10);
    Fighter a1 = CombatFixtures.createFighter("A1", 15, 15, 15, 15, 15, 10, 10);
    Fighter freeWinner = CombatFixtures.createFighter("FreeWinner", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);
    Fighter b1 = CombatFixtures.createFighter("B1", 15, 15, 15, 15, 15, 10, 10);
    BattleSetup setup = BattleSetup.of(List.of(List.of(a0, a1, freeWinner), List.of(b0, b1)));
    BattleRoster roster = BattleRoster.of(setup.teams());
    Engagement first = new Engagement(0, List.of(a0, b0));
    Engagement second = new Engagement(1, List.of(a1, b1));

    // Passo l'id piu' basso per secondo nella lista, per dimostrare che il tie-break e' sull'id
    // dell'Engagement e non sull'ordine della lista in ingresso.
    Optional<Engagement> chosen = assigner.assign(freeWinner, List.of(second, first), roster);

    assertTrue(chosen.isPresent());
    assertSame(first, chosen.get());
  }

  @Test
  void assign_nessunoScontroAttivo_restituisceOptionalVuoto() {
    Fighter freeWinner = CombatFixtures.createFighter("FreeWinner", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);
    BattleSetup setup = BattleSetup.duel(freeWinner, b0);
    BattleRoster roster = BattleRoster.of(setup.teams());

    Optional<Engagement> chosen = assigner.assign(freeWinner, List.of(), roster);

    assertFalse(chosen.isPresent());
  }
}
