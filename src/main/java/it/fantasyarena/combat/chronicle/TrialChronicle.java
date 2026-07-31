package it.fantasyarena.combat.chronicle;

import java.util.List;

import it.fantasyarena.combat.RoundOutcome;
import it.fantasycombatsystem.battle.RoundLogEntry;
import it.fantasycombatsystem.result.TurnLogEntry;

/**
 * La voce di cronaca di una prova giocata: numero, descrizione, forma dello scontro, il roster
 * fotografato prima dello scontro, i passi nell'ordine in cui sono accaduti, l'esito e — solo se la
 * prova è stata vinta — i dati della procedura di fine scontro ({@link #progress()}, {@code null}
 * altrimenti).
 *
 * <p>I passi arrivano in due forme dichiarate, mai unificate: {@link #rounds()} per la battaglia
 * NvN, {@link #turns()} per il duello 1v1. {@link #shape()} dice quale delle due è popolata;
 * l'altra resta vuota. Il duello non porta indici di roster perché il motore non ne fornisce —
 * l'unico modo di correlare un'azione al suo autore sarebbe il nome, che il motore dichiara
 * inaffidabile come identificatore — e fabbricarli qui significherebbe inventare un dato che il
 * motore non ha deciso.
 */
public record TrialChronicle(int number, String description, TrialShape shape, List<CombatantSnapshot> roster,
    List<RoundLogEntry> rounds, List<TurnLogEntry> turns, RoundOutcome outcome, ProgressChronicle progress) {

  public TrialChronicle {
    roster = List.copyOf(roster);
    rounds = List.copyOf(rounds);
    turns = List.copyOf(turns);
  }
}
