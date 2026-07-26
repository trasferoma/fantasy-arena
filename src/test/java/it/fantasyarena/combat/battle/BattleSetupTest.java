package it.fantasyarena.combat.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.testsupport.CombatFixtures;

/**
 * Verifica {@link BattleSetup}: caso degenere del duello, numerazione di {@link BattleSetup#of},
 * vincoli di validazione (numero di squadre, squadra vuota, combattente ripetuto) e la scelta
 * deliberata di NON validare l'unicità dei nomi dei combattenti.
 */
class BattleSetupTest {

  @Test
  void duel_dueCombattenti_produceDueSquadreDaUnMembro() {
    Fighter first = CombatFixtures.createFighter("Alaric", 15, 15, 15, 15, 15, 10, 10);
    Fighter second = CombatFixtures.createFighter("Brom", 15, 15, 15, 15, 15, 10, 10);

    BattleSetup setup = BattleSetup.duel(first, second);

    assertEquals(2, setup.teams().size());
    assertEquals(1, setup.teams().get(0).members().size());
    assertEquals(1, setup.teams().get(1).members().size());
    assertSame(first, setup.teams().get(0).members().get(0));
    assertSame(second, setup.teams().get(1).members().get(0));
  }

  @Test
  void of_numeraIndiciENomiProgressivamente() {
    Fighter a0 = CombatFixtures.createFighter("A0", 15, 15, 15, 15, 15, 10, 10);
    Fighter a1 = CombatFixtures.createFighter("A1", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);

    BattleSetup setup = BattleSetup.of(List.of(List.of(a0, a1), List.of(b0)));

    assertEquals(0, setup.teams().get(0).index());
    assertEquals("Squadra 1", setup.teams().get(0).name());
    assertEquals(1, setup.teams().get(1).index());
    assertEquals("Squadra 2", setup.teams().get(1).name());
  }

  @Test
  void of_unaSolaSquadra_lanciaEccezione() {
    Fighter a0 = CombatFixtures.createFighter("A0", 15, 15, 15, 15, 15, 10, 10);

    assertThrows(IllegalArgumentException.class, () -> BattleSetup.of(List.of(List.of(a0))));
  }

  @Test
  void of_treSquadre_lanciaEccezione() {
    Fighter a0 = CombatFixtures.createFighter("A0", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);
    Fighter c0 = CombatFixtures.createFighter("C0", 15, 15, 15, 15, 15, 10, 10);

    assertThrows(IllegalArgumentException.class,
        () -> BattleSetup.of(List.of(List.of(a0), List.of(b0), List.of(c0))));
  }

  @Test
  void of_squadraVuota_lanciaEccezione() {
    Fighter a0 = CombatFixtures.createFighter("A0", 15, 15, 15, 15, 15, 10, 10);

    assertThrows(IllegalArgumentException.class, () -> BattleSetup.of(List.of(List.of(a0), List.of())));
  }

  @Test
  void of_combattenteRipetutoFraSquadre_lanciaEccezione() {
    Fighter shared = CombatFixtures.createFighter("Shared", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);

    assertThrows(IllegalArgumentException.class,
        () -> BattleSetup.of(List.of(List.of(shared, b0), List.of(shared))));
  }

  @Test
  void of_combattenteRipetutoNellaStessaSquadra_lanciaEccezione() {
    Fighter shared = CombatFixtures.createFighter("Shared", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("B0", 15, 15, 15, 15, 15, 10, 10);

    assertThrows(IllegalArgumentException.class,
        () -> BattleSetup.of(List.of(List.of(shared, shared), List.of(b0))));
  }

  @Test
  void of_dueCombattentiConLoStessoNome_eAccettato() {
    Fighter a0 = CombatFixtures.createFighter("Twin", 15, 15, 15, 15, 15, 10, 10);
    Fighter b0 = CombatFixtures.createFighter("Twin", 15, 15, 15, 15, 15, 10, 10);

    BattleSetup setup = BattleSetup.of(List.of(List.of(a0), List.of(b0)));

    assertSame(a0, setup.teams().get(0).members().get(0));
    assertSame(b0, setup.teams().get(1).members().get(0));
  }
}
