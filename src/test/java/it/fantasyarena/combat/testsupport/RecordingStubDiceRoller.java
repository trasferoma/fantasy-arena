package it.fantasyarena.combat.testsupport;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import it.fantasyarena.combat.dice.DiceThrow;

/**
 * {@link StubDiceRoller} che registra la traccia delle richieste ("d20", "d100", "roll(6)") oltre
 * a restituire i valori scriptati: permette di asserire l'ORDINE di consumo dei dadi, non solo
 * l'esito. E' l'ordine di consumo il vincolo piu' fragile del motore.
 */
public class RecordingStubDiceRoller extends StubDiceRoller {

  private final List<String> trace;

  public RecordingStubDiceRoller(Deque<DiceThrow> scriptedThrows) {
    super(scriptedThrows);
    this.trace = new ArrayList<>();
  }

  public RecordingStubDiceRoller(List<DiceThrow> scriptedThrows) {
    super(scriptedThrows);
    this.trace = new ArrayList<>();
  }

  @Override
  public DiceThrow d20() {
    trace.add("d20");
    return super.d20();
  }

  @Override
  public DiceThrow d100() {
    trace.add("d100");
    return super.d100();
  }

  @Override
  public DiceThrow roll(int faces) {
    trace.add("roll(" + faces + ")");
    return super.roll(faces);
  }

  public List<String> trace() {
    return List.copyOf(trace);
  }
}
