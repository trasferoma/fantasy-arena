package it.fantasyarena.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.factory.FighterFactory;
import it.fantasyarena.combat.hero.Hero;
import it.fantasyarena.combat.hero.HeroBrain;
import it.fantasyarena.combat.io.log.ConsoleArenaLogger;
import it.fantasyarena.combat.io.terminal.TurnPacer;
import it.fantasycombatsystem.battle.BattleSetup;
import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.factory.FighterAssembler;
import it.fantasycombatsystem.model.Fighter;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;

/**
 * Il percorso dell'arena, verificato senza affidarsi all'esito vero dei dadi: gli scontri sono
 * sostituiti da un doppio che decide chi cade, round per round. Quello che si controlla qui è la
 * <em>scansione</em> — quante prove si giocano, con quanti avversari, quando ci si ferma, cosa si
 * porta a casa — perché è l'unica cosa di cui questa classe è responsabile.
 *
 * <p>Il ritmo è azzerato (nessuna attesa dell'INVIO) e {@code System.out} viene catturato sullo
 * stampo di {@code ConsoleBattleLoggerTest}, così la suite resta silenziosa.
 */
class ArenaTest {

  private static final int LETHAL_DAMAGE = 10_000;
  private static final int HERO_TEAM = 0;
  private static final int CHALLENGERS_TEAM = 1;

  private final CombatSettings settings = CombatSettings.defaults();
  private final RecordingFighterFactory factory = new RecordingFighterFactory(settings);
  private final PrintStream originalOut = System.out;
  private ByteArrayOutputStream capturedOut;

  @BeforeEach
  void redirectConsole() {
    capturedOut = new ByteArrayOutputStream();
    System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
  }

  @AfterEach
  void restoreConsole() {
    System.setOut(originalOut);
  }

  @Test
  void treProveVinteInFilaPortanoAlTrionfo() {
    ScriptedFights fights = scripted(FightOutcome.HERO_WINS, FightOutcome.HERO_WINS, FightOutcome.HERO_WINS);

    arenaWith(fights).run();

    assertEquals(List.of(1, 2, 1), fights.challengerCounts(),
        "un avversario, poi due insieme, poi lo sfidante speculare");
    assertTrue(output().contains("ha superato tutte e tre le prove"), output());
  }

  @Test
  void soloLaProvaFinaleUsaIlDuelloASchermate() {
    ScriptedFights fights = scripted(FightOutcome.HERO_WINS, FightOutcome.HERO_WINS, FightOutcome.HERO_WINS);

    arenaWith(fights).run();

    assertEquals(List.of(Presentation.BATTLE, Presentation.BATTLE, Presentation.DUEL), fights.presentations(),
        "le prime due prove passano dalla battaglia, l'uno-contro-uno finale dal duello");
  }

  @Test
  void laCadutaAlPrimoRoundChiudeLArena() {
    ScriptedFights fights = scripted(FightOutcome.HERO_FALLS, FightOutcome.HERO_WINS, FightOutcome.HERO_WINS);

    arenaWith(fights).run();

    assertEquals(List.of(1), fights.challengerCounts(), "dopo la caduta non si gioca nessun altro round");
    assertTrue(output().contains("cade al round 1"), output());
    assertFalse(output().contains("PROCEDURA DI FINE SCONTRO"), "non si saccheggia uno scontro perso");
  }

  @Test
  void restareInPiediSenzaAbbattereTuttiNonApreIlRoundSuccessivo() {
    ScriptedFights fights = scripted(FightOutcome.HERO_WINS, FightOutcome.STALEMATE, FightOutcome.HERO_WINS);

    arenaWith(fights).run();

    assertEquals(List.of(1, 2), fights.challengerCounts());
    assertTrue(output().contains("non ha vinto lo scontro"), output());
  }

  @Test
  void fraUnaProvaELAltraIlProtagonistaCresceEVieneRaccontato() {
    ScriptedFights fights = scripted(FightOutcome.HERO_WINS, FightOutcome.HERO_WINS, FightOutcome.HERO_WINS);

    arenaWith(fights).run();

    assertEquals(3, countOccurrences(output(), "PROCEDURA DI FINE SCONTRO"),
        "una procedura di fine scontro per ogni prova vinta");
    assertEquals(3, countOccurrences(output(), "vita e stamina tornano piene"));
    assertEquals(3, countOccurrences(output(), "Crescita: "));
  }

  @Test
  void ogniProvaCominciaConUnCombattenteAVitaPiena() {
    ScriptedFights fights = scripted(FightOutcome.HERO_WINS, FightOutcome.HERO_WINS, FightOutcome.HERO_WINS);

    arenaWith(fights).run();

    assertEquals(3, factory.championsAtSummon().size());
    factory.championsAtSummon().forEach(champion -> assertEquals(champion.maxHealth(), champion.healthAtSummon(),
        "il combattente di ogni round nasce intero: la scheda non porta le ferite del round prima"));
    assertTrue(factory.lastSummonedChampion().state().currentHealth() < factory.lastSummonedChampion().ratings()
        .maxHealth(), "il copione deve davvero ferire il protagonista, altrimenti non c'è cura da verificare");
  }

  @Test
  void loSfidanteSpeculareRispecchiaIlProtagonistaCresciuto() {
    ScriptedFights fights = scripted(FightOutcome.HERO_WINS, FightOutcome.HERO_WINS, FightOutcome.HERO_WINS);

    arenaWith(fights).run();

    Fighter mirrorRival = fights.challengersOfRound(3).getFirst();
    Hero heroBeforeFinalRound = factory.heroOfRound(3);
    int rivalPoints = mirrorRival.character().characteristics().stream()
        .mapToInt(CharacterCharacteristic::value)
        .sum();

    assertEquals(heroBeforeFinalRound.totalCharacteristicPoints(), rivalPoints,
        "lo specchio deve rispecchiare il protagonista com'è cresciuto, non com'era all'inizio");
    assertEquals(heroBeforeFinalRound.armourPieceCount(), mirrorRival.armourPieces().size());
  }

  private ScriptedFights scripted(FightOutcome... outcomes) {
    return new ScriptedFights(settings, List.of(outcomes));
  }

  /**
   * Lo stesso doppio prende il posto di entrambe le presentazioni — battaglia e duello — perché
   * quello che il test verifica è la scansione dei round, non quale delle due sia stata usata.
   */
  private Arena arenaWith(ScriptedFights fights) {
    return new Arena(factory, new HeroBrain(new Random(7)), fights, fights, new ConsoleArenaLogger(), noPause());
  }

  private TurnPacer noPause() {
    return () -> {
    };
  }

  private String output() {
    return capturedOut.toString(StandardCharsets.UTF_8);
  }

  private int countOccurrences(String text, String fragment) {
    int occurrences = 0;
    for (int index = text.indexOf(fragment); index >= 0; index = text.indexOf(fragment, index + 1)) {
      occurrences++;
    }
    return occurrences;
  }

  /**
   * Come finisce uno scontro, deciso a tavolino invece che dai dadi. {@code STALEMATE} lascia tutti
   * in piedi: nessuno cade, quindi non c'è la vittoria piena che apre il round successivo.
   */
  private enum FightOutcome {
    HERO_WINS,
    HERO_FALLS,
    STALEMATE
  }

  /**
   * Con quale delle due strade lo scontro è stato mostrato.
   */
  private enum Presentation {
    BATTLE,
    DUEL
  }

  /**
   * Prende il posto della presentazione dello scontro: non mostra niente, applica l'esito previsto
   * e registra chi è sceso in campo a ogni round.
   */
  private static final class ScriptedFights extends MatchRunner {

    private final List<FightOutcome> outcomes;
    private final List<List<Fighter>> challengersByRound = new ArrayList<>();
    private final List<Presentation> presentationByRound = new ArrayList<>();

    private ScriptedFights(CombatSettings settings, List<FightOutcome> outcomes) {
      super(settings);
      this.outcomes = outcomes;
    }

    @Override
    public void playBattle(BattleSetup setup) {
      presentationByRound.add(Presentation.BATTLE);
      play(setup.teams().get(HERO_TEAM).members(), setup.teams().get(CHALLENGERS_TEAM).members());
    }

    /**
     * L'ultima prova passa dal duello a schermate, non dalla battaglia: il doppio deve intercettare
     * anche questa strada, altrimenti il terzo round giocherebbe lo scontro vero.
     */
    @Override
    public void playDuel(Fighter champion, Fighter rival) {
      presentationByRound.add(Presentation.DUEL);
      play(List.of(champion), List.of(rival));
    }

    private void play(List<Fighter> heroTeam, List<Fighter> challengers) {
      challengersByRound.add(challengers);

      switch (outcomes.get(challengersByRound.size() - 1)) {
        case HERO_WINS -> {
          strikeDown(challengers);
          wound(heroTeam);
        }
        case HERO_FALLS -> strikeDown(heroTeam);
        case STALEMATE -> { }
      }
    }

    private void strikeDown(List<Fighter> fighters) {
      fighters.forEach(fighter -> fighter.state().applyDamage(LETHAL_DAMAGE));
    }

    /**
     * Anche una prova vinta lascia il segno: senza ferite la cura di fine scontro non sarebbe
     * osservabile, e il test la darebbe per buona senza verificarla.
     */
    private void wound(List<Fighter> fighters) {
      fighters.forEach(fighter -> fighter.state().applyDamage(fighter.ratings().maxHealth() / 2));
    }

    private List<Integer> challengerCounts() {
      return challengersByRound.stream().map(List::size).toList();
    }

    private List<Presentation> presentations() {
      return presentationByRound;
    }

    private List<Fighter> challengersOfRound(int round) {
      return challengersByRound.get(round - 1);
    }
  }

  /**
   * La factory vera, che in più annota la scheda con cui il protagonista scende in campo a ogni
   * round: è l'unico modo per verificare che lo sfidante speculare rispecchi il protagonista
   * cresciuto e non quello di partenza.
   */
  private static final class RecordingFighterFactory extends FighterFactory {

    private final List<Hero> heroesByRound = new ArrayList<>();
    private final List<Fighter> summonedChampions = new ArrayList<>();
    private final List<SummonedChampion> championsAtSummon = new ArrayList<>();

    private RecordingFighterFactory(CombatSettings settings) {
      super(FighterAssembler.withDefaultRatings(settings));
    }

    @Override
    public Fighter summon(Hero hero) {
      Fighter champion = super.summon(hero);
      heroesByRound.add(hero);
      summonedChampions.add(champion);
      championsAtSummon.add(new SummonedChampion(champion.ratings().maxHealth(), champion.state().currentHealth()));
      return champion;
    }

    private Hero heroOfRound(int round) {
      return heroesByRound.get(round - 1);
    }

    private List<SummonedChampion> championsAtSummon() {
      return championsAtSummon;
    }

    private Fighter lastSummonedChampion() {
      return summonedChampions.getLast();
    }
  }

  /**
   * Vita massima e vita corrente fotografate <em>nell'istante</em> in cui il combattente scende in
   * campo: lo stato del {@code Fighter} è mutabile, e a fine partita direbbe soltanto com'è finita.
   */
  private record SummonedChampion(int maxHealth, int healthAtSummon) {
  }
}
