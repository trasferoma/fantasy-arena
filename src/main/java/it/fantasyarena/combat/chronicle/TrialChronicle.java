package it.fantasyarena.combat.chronicle;

import java.util.List;

import it.fantasyarena.combat.RoundOutcome;
import it.fantasycombatsystem.battle.RoundLogEntry;
import it.fantasycombatsystem.result.FighterVitals;
import it.fantasycombatsystem.result.TurnLogEntry;

/**
 * La voce di cronaca di una prova giocata: numero, descrizione, forma dello scontro, il roster
 * fotografato prima dello scontro, il monte punti scontato dalla fortuna con cui gli sfidanti sono
 * nati ({@link #budget()}, {@code null} per la stazione dello specchio), i passi nell'ordine in cui
 * sono accaduti, lo stato finale dei combattenti, l'esito e — solo se la prova è stata vinta — i
 * dati della procedura di fine scontro ({@link #progress()}, {@code null} altrimenti).
 *
 * <p>I passi arrivano in due forme dichiarate, mai unificate: {@link #rounds()} per la battaglia
 * NvN, {@link #turns()} per il duello 1v1. {@link #shape()} dice quale delle due è popolata;
 * l'altra resta vuota. Il duello non porta indici di roster perché il motore non ne fornisce —
 * l'unico modo di correlare un'azione al suo autore sarebbe il nome, che il motore dichiara
 * inaffidabile come identificatore — e fabbricarli qui significherebbe inventare un dato che il
 * motore non ha deciso.
 *
 * <p>{@link #finalVitals()} esiste per un'asimmetria del motore fra le due forme: ogni
 * {@code RoundLogEntry} della battaglia porta già lo stato a fine round, ma ogni
 * {@code TurnLogEntry} del duello porta lo stato a <em>inizio</em> turno (il motore appiattisce la
 * battaglia degenere e ogni turno riceve i vitali con cui è cominciato). Lo stato dopo l'ultimo
 * passo del duello, quindi, non è mai nel log — non arriva mai a mostrare la vita a zero — e un
 * lettore che ha solo questo JSON non potrebbe ricostruirlo. {@link #finalVitals()} lo porta
 * esplicitamente, preso da dove il motore lo calcola per intero ({@code CombatResult.finalVitals()}
 * o {@code BattleResult.finalVitals()}).
 */
public record TrialChronicle(int number, String description, TrialShape shape, List<CombatantSnapshot> roster,
    ChallengerBudgetChronicle budget, List<RoundLogEntry> rounds, List<TurnLogEntry> turns,
    List<FighterVitals> finalVitals, RoundOutcome outcome, ProgressChronicle progress) {

  public TrialChronicle {
    roster = List.copyOf(roster);
    rounds = List.copyOf(rounds);
    turns = List.copyOf(turns);
    finalVitals = List.copyOf(finalVitals);
  }
}
