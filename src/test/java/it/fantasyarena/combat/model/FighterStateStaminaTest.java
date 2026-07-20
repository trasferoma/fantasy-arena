package it.fantasyarena.combat.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * DoD 8 — contratto di affordabilita' di {@link FighterState}: {@code canAfford(amount)} e'
 * vero sse la Stamina corrente copre {@code amount}, la Stamina non scende mai sotto 0, e le
 * operazioni parlanti sulla catena di iniziativa ({@code winInitiative}/{@code loseInitiative})
 * si comportano come da contratto.
 */
class FighterStateStaminaTest {

  @Test
  void canAfford_isTrueOnlyWhenStaminaCoversTheAmount() {
    FighterState state = new FighterState(100, 20);

    assertTrue(state.canAfford(20), "con Stamina piena si puo' pagare fino al massimo");
    assertTrue(state.canAfford(0), "il costo zero e' sempre pagabile");
    assertFalse(state.canAfford(21), "non si puo' pagare piu' della Stamina corrente");

    state.consumeStamina(15);
    assertEquals(5, state.currentStamina());
    assertTrue(state.canAfford(5), "al limite esatto l'azione e' pagabile");
    assertFalse(state.canAfford(6), "un costo superiore alla Stamina residua non e' pagabile");
  }

  @Test
  void consumeStamina_neverGoesBelowZero() {
    FighterState state = new FighterState(100, 10);

    state.consumeStamina(999);

    assertEquals(0, state.currentStamina(), "il floor a 0 resta una guardia difensiva del contenitore");
  }

  @Test
  void winInitiative_incrementsTheChainOnEachRealAttack() {
    FighterState state = new FighterState(100, 20);

    assertEquals(0, state.consecutiveInitiativeWins(), "nessun attacco ancora effettuato");

    state.winInitiative();
    assertEquals(1, state.consecutiveInitiativeWins(), "primo attacco della catena");

    state.winInitiative();
    state.winInitiative();
    assertEquals(3, state.consecutiveInitiativeWins(), "la catena prosegue con attacchi consecutivi");
  }

  @Test
  void loseInitiative_resetsTheChainToZero() {
    FighterState state = new FighterState(100, 20);
    state.winInitiative();
    state.winInitiative();
    assertEquals(2, state.consecutiveInitiativeWins());

    state.loseInitiative();

    assertEquals(0, state.consecutiveInitiativeWins(), "la perdita dell'iniziativa azzera la catena");
  }
}
