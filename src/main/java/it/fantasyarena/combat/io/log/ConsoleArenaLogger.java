package it.fantasyarena.combat.io.log;

import java.util.List;

import it.fantasyarena.combat.RoundOutcome;
import it.fantasyarena.combat.hero.Hero;
import it.fantasyarena.combat.hero.HeroProgress;
import it.fantasyarena.combat.io.render.HeroProgressFormatter;
import it.fantasycombatsystem.model.Fighter;

/**
 * {@link ArenaLogger} di console: presenta chi scende in campo, annuncia i round, racconta la
 * procedura di fine scontro e chiude la storia (trionfo o caduta). È l'unico punto che stampa: le
 * righe della procedura le compone il {@link HeroProgressFormatter}, che resta puro.
 *
 * <p>Non racconta lo <em>scontro</em>: quello è mestiere dei logger di combattimento, che l'arena
 * usa attraverso {@code MatchRunner}. Qui vive solo ciò che sta <em>fra</em> uno scontro e l'altro,
 * che è poi la parte che il motore non conosce.
 */
public class ConsoleArenaLogger implements ArenaLogger {

  private static final String SEPARATOR = "=".repeat(60);

  private final HeroProgressFormatter progressFormatter = new HeroProgressFormatter();

  @Override
  public void reportEntrance(Hero hero, int totalTrials) {
    System.out.println(SEPARATOR);
    System.out.println("L'ARENA");
    System.out.println(SEPARATOR);
    System.out.println("Il protagonista è " + hero.name() + ", e dovrà superare " + totalTrials + " prove.");
    System.out.println();
  }

  @Override
  public void announceRound(int number, String description, Hero hero, List<Fighter> challengers) {
    System.out.println(SEPARATOR);
    System.out.println("ROUND " + number + " — " + description);
    System.out.println(hero.name() + " contro " + describeChallengers(challengers));
    System.out.println(SEPARATOR);
  }

  @Override
  public void reportProgress(HeroProgress progress) {
    System.out.println();
    progressFormatter.lines(progress).forEach(System.out::println);
    System.out.println();
  }

  @Override
  public void reportEndOfRun(Hero hero, RoundOutcome outcome, int round) {
    String message = switch (outcome) {
      case FELL -> hero.name() + " cade al round " + round + ". L'arena si chiude qui.";
      case STOOD_WITHOUT_WINNING -> hero.name() + " resta in piedi al round " + round
          + ", ma non ha vinto lo scontro: senza una vittoria piena non si passa al round successivo.";
      case WON -> throw new IllegalArgumentException(
          "reportEndOfRun non accetta un esito di vittoria: il round " + round + " non è finito qui.");
    };

    System.out.println();
    System.out.println(SEPARATOR);
    System.out.println(message);
    System.out.println(SEPARATOR);
  }

  @Override
  public void reportTriumph(Hero hero, int totalTrials) {
    System.out.println();
    System.out.println(SEPARATOR);
    System.out.println(hero.name() + " ha superato tutte le " + totalTrials + " prove dell'arena.");
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
