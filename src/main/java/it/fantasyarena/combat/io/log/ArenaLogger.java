package it.fantasyarena.combat.io.log;

import java.util.List;

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
   */
  void announceRound(int number, String description, Hero hero, List<Fighter> challengers);

  void reportProgress(HeroProgress progress);

  /**
   * La fine della corsa: il protagonista è caduto, oppure è rimasto in piedi senza però aver
   * abbattuto tutti. Sono due cose diverse e vanno raccontate in modo diverso.
   *
   * @throws IllegalArgumentException se l'esito è {@link RoundOutcome#WON}: una vittoria non è una
   *     fine della corsa, ed è un uso sbagliato di questo metodo passargliela.
   */
  void reportEndOfRun(Hero hero, RoundOutcome outcome, int round);

  /**
   * Annuncia il trionfo: il protagonista ha superato tutte le prove del percorso. Il numero
   * arriva come dato dal percorso, la frase resta di chi implementa.
   */
  void reportTriumph(Hero hero, int totalTrials);
}
