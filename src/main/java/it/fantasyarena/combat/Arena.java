package it.fantasyarena.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import it.fantasyarena.combat.chronicle.ArenaChronicle;
import it.fantasyarena.combat.chronicle.ChronicleMapper;
import it.fantasyarena.combat.chronicle.CombatantSnapshot;
import it.fantasyarena.combat.chronicle.HeroSnapshot;
import it.fantasyarena.combat.chronicle.ProgressChronicle;
import it.fantasyarena.combat.chronicle.RunConclusion;
import it.fantasyarena.combat.chronicle.TrialChronicle;
import it.fantasyarena.combat.chronicle.TrialShape;
import it.fantasyarena.combat.factory.FighterFactory;
import it.fantasyarena.combat.hero.Hero;
import it.fantasyarena.combat.hero.HeroBrain;
import it.fantasyarena.combat.hero.HeroProgress;
import it.fantasyarena.combat.hero.Loot;
import it.fantasyarena.combat.io.log.ArenaLogger;
import it.fantasyarena.combat.io.log.ConsoleArenaLogger;
import it.fantasyarena.combat.io.terminal.EnterKeyTurnPacer;
import it.fantasyarena.combat.io.replay.ReplayMode;
import it.fantasyarena.combat.io.terminal.ScreenRefresh;
import it.fantasyarena.combat.io.terminal.TurnPacer;
import it.fantasycombatsystem.battle.BattleResult;
import it.fantasycombatsystem.battle.BattleSetup;
import it.fantasycombatsystem.battle.RoundLogEntry;
import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.result.CombatResult;
import it.fantasycombatsystem.result.TurnLogEntry;
import it.fantasytoolkitcore.core.model.RarityTable;

/**
 * L'arena del protagonista: un solo eroe, il percorso di {@link TrialPlan} in fila, e fra una prova
 * e l'altra la procedura di fine scontro che lo fa crescere. È la parte del gioco che il motore
 * deliberatamente non ha — la <em>progressione</em> — e vive tutta qui.
 *
 * <p>Questa classe scandisce le stazioni del percorso e non ne decide nessuna: la tabella dice
 * quanti sfidanti affrontare, come nascono e con quale monte punti, e {@link TrialStation#shape()}
 * deriva dal numero di sfidanti quale delle due strade usare — duello quando è uno solo, battaglia
 * quando sono più di uno, quest'ultima sempre attraverso {@link BattleSetup}. Gli scontri li
 * risolve per intero il {@code fantasy-combat-system} attraverso {@link MatchRunner}. Le scelte del
 * protagonista — quale arma tenere, quali pezzi raccogliere, dove spendere i punti — sono tutte di
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
 *
 * <p>{@link #run()} non gioca soltanto: registra anche la cronaca della corsa e la restituisce come
 * {@link ArenaChronicle}. La scansione resta esattamente quella di sempre — questa classe non
 * decide niente di nuovo, si limita a fotografare quello che stava già decidendo. Il
 * {@link RoundReport} è il posto dove quella cronaca si accumula: ogni volta che una prova si gioca,
 * il rapporto restituito porta con sé anche la lista delle voci scritte finora, oltre all'esito e
 * alla scheda cresciuta. È una scelta deliberata rispetto all'alternativa di un accumulatore locale
 * a {@link #run()}: un campo mutabile sarebbe bastato altrettanto bene, ma avrebbe convissuto male
 * con la cortocircuitazione di {@link RoundReport#andThen} — che qui resta l'unico posto a decidere
 * se la prova successiva si gioca — mentre il rapporto che accumula le proprie voci rimane un
 * valore, non uno stato: ogni {@link RoundReport} è una fotografia completa e immutabile di
 * «l'arena fin qui», e nessun metodo muta un campo di istanza per costruirla.
 */
public class Arena {

  /**
   * Indici di squadra e di roster del protagonista: sempre il primo, sempre solo. Sono la stessa
   * convenzione con cui {@link #playAsBattle} costruisce il {@link BattleSetup} — squadra 0 il
   * protagonista, squadra 1 gli sfidanti — applicata anche al duello, dove il motore non produce
   * indici ma il roster fotografato li porta comunque, per posizione.
   */
  private static final int PROTAGONIST_ROSTER_INDEX = 0;
  private static final int PROTAGONIST_TEAM_INDEX = 0;
  private static final int CHALLENGERS_TEAM_INDEX = 1;

  private static final String NEXT_ROUND_HINT = "(premi INVIO per affrontare il round successivo)";

  private final FighterFactory fighterFactory;
  private final HeroBrain heroBrain;
  private final MatchRunner battleRunner;
  private final MatchRunner duelRunner;
  private final ArenaLogger logger;
  private final TurnPacer roundPacer;
  private final ChronicleMapper chronicleMapper = new ChronicleMapper();
  private final TrialPlan plan = TrialPlan.standard();

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
      MatchRunner duelRunner, ArenaLogger logger, TurnPacer roundPacer) {
    this.fighterFactory = fighterFactory;
    this.heroBrain = heroBrain;
    this.battleRunner = battleRunner;
    this.duelRunner = duelRunner;
    this.logger = logger;
    this.roundPacer = roundPacer;
  }

  /**
   * Le stazioni del percorso, una dopo l'altra. Ogni prova restituisce un rapporto: se il
   * protagonista l'ha vinta porta la scheda cresciuta e apre la stazione successiva, altrimenti è
   * un rapporto di chiusura e le stazioni successive semplicemente non si giocano. Il rapporto
   * porta con sé anche la cronaca scritta fin qui, così alla fine basta leggerla dall'ultimo
   * rapporto della catena.
   *
   * <p>Il ciclo concatena {@link RoundReport#andThen} stazione per stazione invece di piegare la
   * lista con una {@code reduce}: una {@code reduce} sequenziale richiederebbe un combinatore mai
   * invocato, cioè una riga che esiste solo per il compilatore. La valutazione resta comunque pigra
   * quanto basta: {@link #fightStation} — e con esso {@code createMirrorRival} sulla stazione dello
   * specchio — si invoca solo quando la stazione precedente è stata vinta, non prima.
   */
  public ArenaChronicle run() {
    Hero protagonist = enterTheArena();
    HeroSnapshot protagonistSnapshot = chronicleMapper.snapshotHero(protagonist);

    RoundReport lastRound = RoundReport.entering(protagonist);
    for (TrialStation station : plan.stations()) {
      lastRound = lastRound.andThen(previous -> fightStation(station, previous));
    }

    if (lastRound.passed()) {
      logger.reportTriumph(lastRound.grownHero(), plan.length());
    }

    TrialChronicle lastTrial = lastRound.trials().getLast();
    return new ArenaChronicle(protagonistSnapshot, plan.length(), lastRound.trials(),
        new RunConclusion(lastTrial.outcome(), lastTrial.number()));
  }

  private Hero enterTheArena() {
    Hero protagonist = fighterFactory.createProtagonist();
    logger.reportEntrance(protagonist, plan.length());
    return protagonist;
  }

  /**
   * Una stazione del percorso: gli sfidanti si generano secondo la sua origine (
   * {@link #challengersFor}) e si affrontano con la strada di presentazione che la sua forma
   * dichiara ({@link #playFor}).
   */
  private RoundReport fightStation(TrialStation station, RoundReport previous) {
    List<Fighter> challengers = challengersFor(station, previous.grownHero());
    return fightRound(station.number(), station.description(), previous, challengers, playFor(station));
  }

  /**
   * Chi affronta il protagonista in questa stazione: sfidanti generati col monte punti che la
   * stazione dichiara, oppure lo specchio del protagonista com'è cresciuto fin qui. Lo switch è
   * esaustivo e senza {@code default}: una terza origine deve fermare la compilazione, non sparire
   * in un ramo dimenticato.
   */
  private List<Fighter> challengersFor(TrialStation station, Hero hero) {
    return switch (station.challengerOrigin()) {
      case GENERATED -> fighterFactory.createChallengers(station.challengerCount(), station.characteristicPoints());
      case MIRROR -> List.of(fighterFactory.createMirrorRival(hero));
    };
  }

  /**
   * Con quale delle due strade di presentazione si gioca questa stazione, derivata dalla sua
   * {@link TrialStation#shape()}. Esaustivo e senza {@code default}, per lo stesso motivo di
   * {@link #challengersFor}.
   */
  private FightPlay playFor(TrialStation station) {
    return switch (station.shape()) {
      case BATTLE -> this::playAsBattle;
      case DUEL -> this::playAsDuel;
    };
  }

  /**
   * La scansione di una prova: annuncio, materializzazione, fotografia del roster, scontro,
   * verifica dell'esito, procedura di fine scontro se vinta. La fotografia del roster avviene
   * <em>prima</em> di {@code play}, quando il campione e gli sfidanti sono ancora integri: è così
   * che la cronaca non porta mai le ferite dello scontro appena giocato.
   */
  private RoundReport fightRound(int number, String description, RoundReport previous, List<Fighter> challengers,
      FightPlay play) {
    Hero hero = previous.grownHero();
    logger.announceRound(number, description, hero, challengers);

    Fighter champion = fighterFactory.summon(hero);
    List<CombatantSnapshot> roster = snapshotRoster(champion, challengers);
    TrialSteps steps = play.play(champion, challengers);

    RoundOutcome outcome = outcomeOf(champion, challengers);
    if (outcome != RoundOutcome.WON) {
      logger.reportEndOfRun(hero, outcome, number);
      return previous.lostTrial(chronicleOf(number, description, steps, roster, outcome, null));
    }

    HeroProgress progress = applyEndOfFightProcedure(hero, number);
    ProgressChronicle progressChronicle = chronicleMapper.snapshotProgress(progress);
    return previous.wonTrial(progress.grownHero(),
        chronicleOf(number, description, steps, roster, outcome, progressChronicle));
  }

  /**
   * La voce di cronaca di una prova, assemblata una volta sola: la usano sia il ramo vinto sia
   * quello non vinto di {@link #fightRound}, che si distinguono solo per {@code progress} ({@code
   * null} quando la prova non è stata vinta). Tenerla in un solo posto evita che le due costruzioni
   * — identiche per sei degli otto argomenti — divergano silenziosamente se {@link TrialChronicle}
   * cambia forma.
   */
  private TrialChronicle chronicleOf(int number, String description, TrialSteps steps,
      List<CombatantSnapshot> roster, RoundOutcome outcome, ProgressChronicle progress) {
    return new TrialChronicle(number, description, steps.shape(), roster, steps.rounds(), steps.turns(), outcome,
        progress);
  }

  /**
   * Il roster di uno scontro, fotografato con gli indici di {@code BattleRoster#all()}: il
   * protagonista sempre per primo in squadra 0, gli sfidanti a seguire in squadra 1. Nel duello il
   * motore non produce indici equivalenti — solo il nome correla un'azione al suo autore, e il
   * motore lo dichiara inaffidabile — ma il roster li porta comunque, per posizione: nessun passo
   * del duello vi si riferisce, a differenza degli {@code EngagementTurn} della battaglia.
   */
  private List<CombatantSnapshot> snapshotRoster(Fighter champion, List<Fighter> challengers) {
    List<CombatantSnapshot> roster = new ArrayList<>();
    roster.add(chronicleMapper.snapshotCombatant(champion, PROTAGONIST_ROSTER_INDEX, PROTAGONIST_TEAM_INDEX));
    for (int challengerIndex = 0; challengerIndex < challengers.size(); challengerIndex++) {
      int rosterIndex = PROTAGONIST_ROSTER_INDEX + 1 + challengerIndex;
      roster.add(chronicleMapper.snapshotCombatant(challengers.get(challengerIndex), rosterIndex,
          CHALLENGERS_TEAM_INDEX));
    }
    return List.copyOf(roster);
  }

  private TrialSteps playAsBattle(Fighter champion, List<Fighter> challengers) {
    BattleResult result = battleRunner.playBattle(BattleSetup.of(List.of(List.of(champion), challengers)));
    return TrialSteps.ofBattle(result.roundLog());
  }

  /**
   * Il duello a schermate vuole due combattenti, non due schieramenti: si usa solo dove lo
   * sfidante è uno solo.
   */
  private TrialSteps playAsDuel(Fighter champion, List<Fighter> challengers) {
    CombatResult result = duelRunner.playDuel(champion, challengers.getFirst());
    return TrialSteps.ofDuel(result.log());
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
  private HeroProgress applyEndOfFightProcedure(Hero hero, int level) {
    RarityTable rarityTable = heroBrain.lootRarityTable(level);
    Loot loot = fighterFactory.rollLoot(rarityTable);
    HeroProgress progress = heroBrain.progressAfterVictory(hero, loot);

    logger.reportProgress(progress);
    roundPacer.awaitNextTurn();
    return progress;
  }

  /**
   * Come si gioca lo scontro di un round: chiede al {@link MatchRunner} adatto (battaglia o duello)
   * l'esito e lo consegna già ridotto ai soli passi che la cronaca deve portare ({@link TrialSteps}).
   * Esiste perché la scansione del round è identica per tutte le stazioni del percorso — annuncio,
   * materializzazione, scontro, verifica, procedura — e l'unica cosa che cambia è quale
   * {@link MatchRunner} lo mette in scena: sarebbe un peccato duplicare tutto il resto per quella
   * sola differenza.
   */
  @FunctionalInterface
  private interface FightPlay {

    TrialSteps play(Fighter champion, List<Fighter> challengers);
  }

  /**
   * I passi prodotti dal motore per una prova, nella forma che il motore ha deciso: una lista di
   * {@link RoundLogEntry} per la battaglia, una di {@link TurnLogEntry} per il duello. L'altra lista
   * resta vuota — {@link #shape()} dice quale delle due leggere — perché fabbricare indici o turni
   * per l'una a partire dall'altra significherebbe inventare un dato che il motore non ha deciso.
   */
  private record TrialSteps(TrialShape shape, List<RoundLogEntry> rounds, List<TurnLogEntry> turns) {

    static TrialSteps ofBattle(List<RoundLogEntry> rounds) {
      return new TrialSteps(TrialShape.BATTLE, rounds, List.of());
    }

    static TrialSteps ofDuel(List<TurnLogEntry> turns) {
      return new TrialSteps(TrialShape.DUEL, List.of(), turns);
    }
  }

  /**
   * L'esito di una prova in costruzione: se la catena può proseguire ({@link #passed}), la scheda
   * cresciuta se sì, e la cronaca scritta fino a qui compresa. Esiste per non far uscire un
   * {@code Optional} generico dai metodi di prova — il concetto non è "un eroe, forse", è "questa
   * prova è andata così" — e per portare con sé {@link #andThen}, che incatena la prova successiva
   * con la stessa cortocircuitazione che prima dava {@code flatMap}: se il rapporto non è di
   * vittoria, la prova successiva non si gioca nemmeno.
   *
   * <p>Non porta un {@link RoundOutcome} proprio: quello con cui è finita l'ultima prova giocata è
   * già dentro l'ultima voce di {@link #trials()} ({@code TrialChronicle#outcome()}), e {@link
   * #run()} lo legge da lì. Duplicarlo qui vorrebbe dire custodire due copie dello stesso dato
   * invece di risolverlo alla lettura da un'unica fonte — e la seconda copia sarebbe scomoda da
   * riempire onestamente prima che la prima prova sia stata giocata. {@link #passed} resta perciò
   * un semplice segnale di via libera per {@link #andThen}: vero anche all'ingresso nell'arena,
   * quando nessuna prova è stata ancora giocata, perché lì significa solo "si può cominciare", non
   * "si è vinto".
   *
   * <p>{@link #trials()} è l'accumulatore della cronaca: {@link #wonTrial} e {@link #lostTrial}
   * costruiscono il rapporto successivo aggiungendovi una voce, senza mai mutare la lista che
   * portavano. Ogni {@code RoundReport} resta così un valore immutabile — non un contenitore che
   * cresce nel tempo — ed è per questo che la cronaca vive qui invece che in un campo di
   * {@link Arena}: un campo si presterebbe a restare sporco fra una chiamata a {@link #run()} e la
   * successiva sulla stessa istanza, un valore che passa di rapporto in rapporto no.
   */
  private record RoundReport(boolean passed, Hero grownHero, List<TrialChronicle> trials) {

    private RoundReport {
      trials = List.copyOf(trials);
    }

    /**
     * Il rapporto con cui il protagonista entra nell'arena: nessuna prova ancora giocata, {@code
     * passed} vero solo per dare il via libera alla prima chiamata di {@link #andThen} — non per
     * dichiarare una vittoria che non c'è ancora stata.
     */
    static RoundReport entering(Hero protagonist) {
      return new RoundReport(true, protagonist, List.of());
    }

    RoundReport wonTrial(Hero grownHero, TrialChronicle trial) {
      return new RoundReport(true, grownHero, appending(trial));
    }

    RoundReport lostTrial(TrialChronicle trial) {
      return new RoundReport(false, null, appending(trial));
    }

    RoundReport andThen(Function<RoundReport, RoundReport> nextRound) {
      return passed ? nextRound.apply(this) : this;
    }

    private List<TrialChronicle> appending(TrialChronicle trial) {
      List<TrialChronicle> updated = new ArrayList<>(trials);
      updated.add(trial);
      return List.copyOf(updated);
    }
  }
}
