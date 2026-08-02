package it.fantasyarena.combat.io.log;

import java.util.List;

import it.fantasyarena.combat.ChallengerBudget;
import it.fantasyarena.combat.RoundOutcome;
import it.fantasyarena.combat.hero.Hero;
import it.fantasyarena.combat.hero.HeroProgress;
import it.fantasycombatsystem.model.Fighter;

/**
 * Astrazione della voce dell'arena del protagonista: presenta chi scende in campo, annuncia i
 * round, racconta la procedura di fine scontro e chiude la storia (trionfo o caduta).
 *
 * <p>Nasce ora perché sta arrivando una seconda presentazione — quella web — che deve poter
 * ricevere questi stessi annunci senza stampare niente: non è una previsione, è la superficie già
 * esposta da {@link ConsoleArenaLogger}, resa sostituibile.
 */
public interface ArenaLogger {

  /**
   * Annuncia l'ingresso del protagonista nell'arena, con quante prove lo aspettano in tutto: il
   * numero arriva come dato dal percorso, la frase resta di chi implementa.
   */
  void reportEntrance(Hero hero, int totalTrials);

  /**
   * Annuncia il round: chi lo affronta e contro quanti. Le schede dei combattenti non arrivano
   * qui: le mostra il logger della battaglia all'apertura dello scontro.
   *
   * <p>{@code budget} arriva come tipo di dominio, non come fotografia di cronaca — la stessa
   * scelta già fatta per {@link RoundOutcome} — ed è {@code null} per la stazione dello specchio,
   * che non passa da nessuno sconto: l'annuncio in quel caso non dice niente sulla fortuna.
   */
  void announceRound(int number, String description, Hero hero, List<Fighter> challengers, ChallengerBudget budget);

  void reportProgress(HeroProgress progress);

  /**
   * La fine della corsa: il protagonista è caduto in questo round e l'arena si chiude qui. Un
   * pareggio non è più una fine della corsa — vedi {@link #reportTrialCrossed} — e non va passato
   * a questo metodo più di quanto ci vada una vittoria.
   *
   * @throws IllegalArgumentException se l'esito non è {@link RoundOutcome#FELL}: solo la caduta
   *     chiude la corsa, e passare qui una vittoria o un pareggio è un uso sbagliato del metodo.
   */
  void reportEndOfRun(Hero hero, RoundOutcome outcome, int round);

  /**
   * Il protagonista resta in piedi ma non ha abbattuto tutti gli sfidanti: la prova non chiude la
   * corsa, prosegue senza loot né punti caratteristica. Non è una fine da raccontare, è un
   * passaggio.
   */
  void reportTrialCrossed(Hero hero, int round);

  /**
   * Annuncia il trionfo: il protagonista ha superato tutte le prove del percorso. Il numero
   * arriva come dato dal percorso, la frase resta di chi implementa.
   */
  void reportTriumph(Hero hero, int totalTrials);
}
