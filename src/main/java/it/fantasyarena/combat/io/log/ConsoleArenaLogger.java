package it.fantasyarena.combat.io.log;

import java.util.List;

import it.fantasyarena.combat.hero.Hero;
import it.fantasyarena.combat.hero.HeroProgress;
import it.fantasyarena.combat.io.render.HeroProgressFormatter;
import it.fantasycombatsystem.model.Fighter;

/**
 * La voce dell'arena del protagonista: presenta chi scende in campo, annuncia i round, racconta la
 * procedura di fine scontro e chiude la storia (trionfo o caduta). È l'unico punto che stampa: le
 * righe della procedura le compone il {@link HeroProgressFormatter}, che resta puro.
 *
 * <p>Non racconta lo <em>scontro</em>: quello è mestiere dei logger di combattimento, che l'arena
 * usa attraverso {@code MatchRunner}. Qui vive solo ciò che sta <em>fra</em> uno scontro e l'altro,
 * che è poi la parte che il motore non conosce.
 */
public class ConsoleArenaLogger {

  private static final String SEPARATOR = "=".repeat(60);

  private final HeroProgressFormatter progressFormatter = new HeroProgressFormatter();

  public void reportEntrance(Hero hero) {
    System.out.println(SEPARATOR);
    System.out.println("L'ARENA");
    System.out.println(SEPARATOR);
    System.out.println("Il protagonista è " + hero.name() + ", e dovrà superare tre prove.");
    System.out.println();
  }

  /**
   * Annuncia il round: chi lo affronta e contro quanti. Le schede dei combattenti non si stampano
   * qui — le stampa il logger della battaglia all'apertura dello scontro, e ripeterle sarebbe
   * rumore.
   */
  public void announceRound(int number, String description, Hero hero, List<Fighter> challengers) {
    System.out.println(SEPARATOR);
    System.out.println("ROUND " + number + " — " + description);
    System.out.println(hero.name() + " contro " + describeChallengers(challengers));
    System.out.println(SEPARATOR);
  }

  public void reportProgress(HeroProgress progress) {
    System.out.println();
    progressFormatter.lines(progress).forEach(System.out::println);
    System.out.println();
  }

  /**
   * La fine della corsa: il protagonista è caduto, oppure è rimasto in piedi senza però aver
   * abbattuto tutti. Sono due cose diverse e vanno dette in modo diverso.
   */
  public void reportEndOfRun(Hero hero, Fighter champion, int round) {
    System.out.println();
    System.out.println(SEPARATOR);
    if (champion.isDefeated()) {
      System.out.println(hero.name() + " cade al round " + round + ". L'arena si chiude qui.");
    } else {
      System.out.println(hero.name() + " resta in piedi al round " + round + ", ma non ha vinto lo scontro: "
          + "senza una vittoria piena non si passa al round successivo.");
    }
    System.out.println(SEPARATOR);
  }

  public void reportTriumph(Hero hero) {
    System.out.println();
    System.out.println(SEPARATOR);
    System.out.println(hero.name() + " ha superato tutte e tre le prove dell'arena.");
    System.out.println("Esce con " + hero.weapon().weapon() + " in pugno, " + hero.armourPieceCount()
        + " pezzi d'armatura addosso, " + hero.jewelCount() + " gioielli indossati e "
        + hero.totalCharacteristicPoints() + " punti caratteristica.");
    System.out.println(SEPARATOR);
  }

  private String describeChallengers(List<Fighter> challengers) {
    if (challengers.size() == 1) {
      return challengers.getFirst().name();
    }
    return challengers.size() + " avversari: " + challengers.stream()
        .map(Fighter::name)
        .reduce((first, second) -> first + ", " + second)
        .orElse("");
  }
}
