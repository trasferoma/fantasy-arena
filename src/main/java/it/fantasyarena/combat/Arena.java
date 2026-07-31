package it.fantasyarena.combat;

import java.util.List;
import java.util.function.Function;

import it.fantasyarena.combat.factory.FighterFactory;
import it.fantasyarena.combat.hero.Hero;
import it.fantasyarena.combat.hero.HeroBrain;
import it.fantasyarena.combat.hero.HeroProgress;
import it.fantasyarena.combat.hero.Loot;
import it.fantasyarena.combat.io.log.ConsoleArenaLogger;
import it.fantasyarena.combat.io.terminal.EnterKeyTurnPacer;
import it.fantasyarena.combat.io.replay.ReplayMode;
import it.fantasyarena.combat.io.terminal.ScreenRefresh;
import it.fantasyarena.combat.io.terminal.TurnPacer;
import it.fantasycombatsystem.battle.BattleSetup;
import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.model.Fighter;
import it.fantasytoolkitcore.core.model.RarityTable;

/**
 * L'arena del protagonista: un solo eroe, tre prove in fila, e fra una prova e l'altra la
 * procedura di fine scontro che lo fa crescere. È la parte del gioco che il motore
 * deliberatamente non ha — la <em>progressione</em> — e vive tutta qui.
 *
 * <p>Questa classe scandisce i passaggi e non ne decide nessuno. Gli scontri li risolve per intero
 * il {@code fantasy-combat-system} attraverso {@link MatchRunner}. Le prime due prove usano il
 * percorso battaglia — anche quella contro un avversario solo, che passa comunque da
 * {@link BattleSetup} — mentre la prova finale, che è un uno-contro-uno, usa il duello a schermate:
 * più curato, e visivamente distingue lo scontro che chiude l'arena. Le scelte del protagonista —
 * quale arma tenere, quali pezzi raccogliere, dove spendere i punti — sono tutte di
 * {@link HeroBrain}, che è il punto unico da toccare per ribilanciare la progressione.
 *
 * <p>Fra il protagonista e i suoi combattenti c'è una distinzione che regge tutto il resto: la
 * {@link Hero} è la scheda che sopravvive ai round, il {@code Fighter} è chi scende in campo in un
 * round solo e ne esce ferito. A ogni prova il protagonista viene materializzato di nuovo dalla
 * scheda ({@code FighterFactory.summon}), ed è da qui che arriva la cura completa promessa dalla
 * procedura: non si guarisce nessuno, si torna in campo interi.
 *
 * <p>Si avanza solo con una vittoria piena. Sopravvivere non basta: vedi {@link #outcomeOf}.
 *
 * <p>Ogni prova restituisce un {@link RoundReport}: la scheda cresciuta se il protagonista l'ha
 * superata, o un rapporto di chiusura altrimenti. Non è un dettaglio cosmetico rispetto a un
 * {@code Optional}: il rapporto è il posto giusto per accorciare la lettura in {@link #run()} con
 * {@link RoundReport#andThen}, che gioca la prova successiva solo se questa è stata vinta.
 */
public class Arena {

  private static final String FIRST_ROUND_DESCRIPTION = "il primo avversario";
  private static final String SECOND_ROUND_DESCRIPTION = "due contro uno";
  private static final String FINAL_ROUND_DESCRIPTION = "lo sfidante speculare, armato meglio";

  private static final int FIRST_ROUND = 1;
  private static final int SECOND_ROUND = 2;
  private static final int FINAL_ROUND = 3;

  private static final int LONE_CHALLENGER = 1;
  private static final int CHALLENGER_PAIR = 2;

  private static final String NEXT_ROUND_HINT = "(premi INVIO per affrontare il round successivo)";

  private final FighterFactory fighterFactory;
  private final HeroBrain heroBrain;
  private final MatchRunner battleRunner;
  private final MatchRunner duelRunner;
  private final ConsoleArenaLogger logger;
  private final TurnPacer roundPacer;

  public Arena(CombatSettings settings) {
    this(settings, ScreenRefresh.CLEAR);
  }

  /**
   * I due {@link MatchRunner} sono distinti, non lo stesso usato in due modi: ognuno costruisce alla
   * prima chiamata il proprio {@link TurnPacer} col suggerimento adatto (round per la battaglia,
   * nessuno per la pagina del duello, che il suggerimento ce l'ha già disegnato dentro).
   * Condividerne uno solo significherebbe trascinare nel duello finale il suggerimento della
   * battaglia.
   */
  public Arena(CombatSettings settings, ScreenRefresh screenRefresh) {
    this(FighterFactory.withDefaultRatings(settings), new HeroBrain(),
        new MatchRunner(settings, ReplayMode.SCREEN, screenRefresh),
        new MatchRunner(settings, ReplayMode.SCREEN, screenRefresh), new ConsoleArenaLogger(),
        new EnterKeyTurnPacer(NEXT_ROUND_HINT));
  }

  /**
   * Costruttore con collaboratori espliciti: serve a chi deve rendere l'arena riproducibile
   * (generazione e cervello pilotati) invece di affidarsi al caso.
   */
  public Arena(FighterFactory fighterFactory, HeroBrain heroBrain, MatchRunner battleRunner,
      MatchRunner duelRunner, ConsoleArenaLogger logger, TurnPacer roundPacer) {
    this.fighterFactory = fighterFactory;
    this.heroBrain = heroBrain;
    this.battleRunner = battleRunner;
    this.duelRunner = duelRunner;
    this.logger = logger;
    this.roundPacer = roundPacer;
  }

  /**
   * Le tre prove in fila. Ogni round restituisce un rapporto: se il protagonista lo ha vinto porta
   * la scheda cresciuta e apre la prova successiva, altrimenti è un rapporto di chiusura e i round
   * successivi semplicemente non accadono.
   */
  public void run() {
    Hero protagonist = enterTheArena();

    RoundReport lastRound = fightLoneChallenger(protagonist)
        .andThen(this::fightChallengerPair)
        .andThen(this::fightMirrorRival);

    if (lastRound.isPassed()) {
      logger.reportTriumph(lastRound.grownHero());
    }
  }

  private Hero enterTheArena() {
    Hero protagonist = fighterFactory.createProtagonist();
    logger.reportEntrance(protagonist);
    return protagonist;
  }

  /**
   * Prima prova: un avversario alla pari, equipaggiato come lui.
   */
  private RoundReport fightLoneChallenger(Hero hero) {
    return fightRound(FIRST_ROUND, FIRST_ROUND_DESCRIPTION, hero,
        fighterFactory.createChallengers(LONE_CHALLENGER), this::playAsBattle);
  }

  /**
   * Seconda prova: due avversari insieme contro di lui.
   */
  private RoundReport fightChallengerPair(Hero hero) {
    return fightRound(SECOND_ROUND, SECOND_ROUND_DESCRIPTION, hero,
        fighterFactory.createChallengers(CHALLENGER_PAIR), this::playAsBattle);
  }

  /**
   * Ultima prova: uno sfidante con i suoi stessi punti caratteristica e altrettanti pezzi
   * d'armatura, ma con un'arma rara. Si genera adesso, non prima, perché deve rispecchiare il
   * protagonista <em>com'è cresciuto</em> nei due round precedenti. Essendo un uno-contro-uno, è
   * l'unica prova mostrata col duello a schermate.
   */
  private RoundReport fightMirrorRival(Hero hero) {
    return fightRound(FINAL_ROUND, FINAL_ROUND_DESCRIPTION, hero,
        List.of(fighterFactory.createMirrorRival(hero)), this::playAsDuel);
  }

  private RoundReport fightRound(int number, String description, Hero hero, List<Fighter> challengers,
      FightPlay play) {
    logger.announceRound(number, description, hero, challengers);

    Fighter champion = fighterFactory.summon(hero);
    play.play(champion, challengers);

    RoundOutcome outcome = outcomeOf(champion, challengers);
    if (outcome != RoundOutcome.WON) {
      logger.reportEndOfRun(hero, outcome, number);
      return RoundReport.endOfRun(outcome);
    }
    return RoundReport.passed(applyEndOfFightProcedure(hero, number));
  }

  private void playAsBattle(Fighter champion, List<Fighter> challengers) {
    battleRunner.playBattle(BattleSetup.of(List.of(List.of(champion), challengers)));
  }

  /**
   * Il duello a schermate vuole due combattenti, non due schieramenti: si usa solo dove lo
   * sfidante è uno solo.
   */
  private void playAsDuel(Fighter champion, List<Fighter> challengers) {
    duelRunner.playDuel(champion, challengers.getFirst());
  }

  /**
   * Si prosegue solo con una vittoria piena: il protagonista in piedi e tutti gli avversari
   * abbattuti. Restare vivo dopo un pareggio o una decisione ai punti non apre il round
   * successivo: il loot non dipende più da chi è caduto, ma resta il premio della prova superata, e
   * da uno scontro non vinto non ne arriva nessuno. La caduta e il pareggio sono comunque due esiti
   * distinti, perché a valle vanno raccontati in modo diverso.
   */
  private RoundOutcome outcomeOf(Fighter champion, List<Fighter> challengers) {
    if (champion.isDefeated()) {
      return RoundOutcome.FELL;
    }
    if (challengers.stream().allMatch(Fighter::isDefeated)) {
      return RoundOutcome.WON;
    }
    return RoundOutcome.STOOD_WITHOUT_WINNING;
  }

  /**
   * La procedura di fine scontro: cura, l'unico oggetto di loot della prova, punti caratteristica.
   * Viene raccontata per intero e poi attende l'INVIO, così chi guarda ha il tempo di leggere cosa
   * è cambiato prima che lo schermo si pulisca per il round successivo.
   *
   * <p>Il livello serve solo a stabilire quanto pregiato può essere il loot, e la tabella con cui
   * estrarne la rarità la decide il {@link HeroBrain}: qui si passa il numero della prova, non un
   * criterio.
   */
  private Hero applyEndOfFightProcedure(Hero hero, int level) {
    RarityTable rarityTable = heroBrain.lootRarityTable(level);
    Loot loot = fighterFactory.rollLoot(rarityTable);
    HeroProgress progress = heroBrain.progressAfterVictory(hero, loot);

    logger.reportProgress(progress);
    roundPacer.awaitNextTurn();
    return progress.grownHero();
  }

  /**
   * Come si gioca lo scontro di un round. Esiste perché la scansione del round è identica per tutte
   * e tre le prove — annuncio, materializzazione, scontro, verifica, procedura — e l'unica cosa che
   * cambia è quale {@link MatchRunner} lo mette in scena: sarebbe un peccato duplicare tutto il
   * resto per quella sola differenza.
   */
  @FunctionalInterface
  private interface FightPlay {

    void play(Fighter champion, List<Fighter> challengers);
  }

  /**
   * L'esito di una prova: l'{@link RoundOutcome} con cui è finita, e la scheda cresciuta se è stata
   * superata. Esiste per non far uscire un {@code Optional} generico dai metodi di prova — il
   * concetto non è "un eroe, forse", è "questa prova è andata così" — e per portare con sé
   * {@link #andThen}, che incatena la prova successiva con la stessa cortocircuitazione che prima
   * dava {@code flatMap}: se il rapporto non è di vittoria, la prova successiva non si gioca nemmeno.
   */
  private record RoundReport(RoundOutcome outcome, Hero grownHero) {

    static RoundReport passed(Hero grownHero) {
      return new RoundReport(RoundOutcome.WON, grownHero);
    }

    static RoundReport endOfRun(RoundOutcome outcome) {
      return new RoundReport(outcome, null);
    }

    boolean isPassed() {
      return outcome == RoundOutcome.WON;
    }

    RoundReport andThen(Function<Hero, RoundReport> nextRound) {
      return isPassed() ? nextRound.apply(grownHero) : this;
    }
  }
}
