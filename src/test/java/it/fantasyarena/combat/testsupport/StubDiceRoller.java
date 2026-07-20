package it.fantasyarena.combat.testsupport;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import it.fantasyarena.combat.dice.DiceRoller;
import it.fantasyarena.combat.dice.DiceThrow;

/**
 * Stub deterministico di {@link DiceRoller}: invece di lanciare dadi reali tramite il
 * toolkit, restituisce una sequenza di {@link DiceThrow} programmata in ordine di chiamata.
 * Usato nei test per rendere riproducibile un intero duello o per forzare esiti specifici
 * (colpito, mancato, schivato, parato, critico) nei resolver del core.
 */
public class StubDiceRoller extends DiceRoller {

  private final Deque<DiceThrow> scriptedThrows;

  public StubDiceRoller(Deque<DiceThrow> scriptedThrows) {
    this.scriptedThrows = scriptedThrows;
  }

  public StubDiceRoller(List<DiceThrow> scriptedThrows) {
    this(new ArrayDeque<>(scriptedThrows));
  }

  @Override
  public DiceThrow d20() {
    return nextThrow();
  }

  @Override
  public DiceThrow d100() {
    return nextThrow();
  }

  @Override
  public DiceThrow roll(int faces) {
    return nextThrow();
  }

  private DiceThrow nextThrow() {
    if (scriptedThrows.isEmpty()) {
      throw new IllegalStateException("Nessun DiceThrow programmato rimasto nello StubDiceRoller");
    }
    return scriptedThrows.poll();
  }
}
