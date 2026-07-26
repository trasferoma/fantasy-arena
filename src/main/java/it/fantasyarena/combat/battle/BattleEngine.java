package it.fantasyarena.combat.battle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import it.fantasyarena.combat.config.CombatFormulas;
import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceRoller;
import it.fantasyarena.combat.engine.CombatEngine;
import it.fantasyarena.combat.engine.InitiativeResolver;
import it.fantasyarena.combat.engine.StaminaRules;
import it.fantasyarena.combat.engine.TurnOrchestrator;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.CombatOutcome;
import it.fantasyarena.combat.result.FighterVitals;
import it.fantasyarena.combat.result.Scorecard;
import it.fantasyarena.combat.result.StaminaChange;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * Orchestra la battaglia NvN, round dopo round: apre gli scontri iniziali, gioca uno scambio per
 * scontro attivo tramite {@link EngagementTurnPlayer}, applica il recupero passivo di chi non ha
 * partecipato ad alcuno scambio nel round e riassegna i vincitori liberi allo scontro attivo piu'
 * in inferiorita' numerica. Nessuna formula qui: solo orchestrazione parlante, sullo stampo di
 * {@link CombatEngine} generalizzato a N combattenti.
 *
 * <p>Il recupero passivo di Stamina resta diviso in due responsabilita' locali, deliberatamente
 * non unificate in un solo punto: {@link TurnOrchestrator#playTurn} applica il recupero al
 * bersaglio di OGNI scambio (la scatola resta ignara di squadre, round e altri scontri: e'
 * un'affermazione puramente binaria, "chi ha subito lo scambio invece di agirlo rifiata"), mentre
 * questo motore, a fine round, applica lo stesso recupero a chi in quel round non e' stato ne'
 * attore ne' bersaglio di alcuno scambio (gli "inattivi": il terzo uomo di un 2v1, un vincitore
 * libero in attesa, chi resta senza scontro attivo). Le due responsabilita' non si sovrappongono
 * mai: ogni scontro attivo gioca un solo scambio per round e gli scontri hanno partecipanti
 * disgiunti, quindi un combattente vivo e' attore al massimo una volta e bersaglio al massimo una
 * volta per round, mai entrambi. Gli insiemi {attori}, {bersagli} e {inattivi} partizionano quindi
 * i vivi del round, e ogni bersaglio riceve il recupero esattamente una volta (dalla scatola):
 * nessun doppio recupero. Nel duello 1v1 l'insieme degli inattivi e' sempre vuoto (nessuno resta
 * mai fuori dall'unico scontro): il punto 4 del round e' un no-op strutturale, e il duello binario
 * resta identico a quello prodotto da {@link CombatEngine}, senza bisogno di alcun adapter.
 *
 * <p>{@link CombatSettings#maxTurns()} e' reinterpretato qui come tetto di ROUND, non di singoli
 * scambi: il nome resta quello storico del duello 1v1 (dove round e scambio coincidevano), ma in
 * NvN i turni complessivamente giocati sono all'incirca {@code round * numero di scontri attivi}.
 *
 * <p>Un combattente appartiene a un solo scontro alla volta: l'appartenenza corrente e' tracciata
 * da questo motore in una mappa per identita' (non dallo scontro stesso, che non supporta la
 * rimozione di un partecipante), aggiornata a ogni riassegnazione di vincitore libero. E' questo
 * che impedisce a un vincitore libero di essere considerato "libero" due volte nello stesso round.
 */
public class BattleEngine {

  private final EngagementTurnPlayer turnPlayer;
  private final StaminaRules staminaRules;
  private final CombatSettings settings;
  private final EngagementPlanner planner;
  private final FreeWinnerAssigner assigner;

  public BattleEngine(DiceRoller diceRoller, InitiativeResolver initiativeResolver, TurnOrchestrator turnOrchestrator,
      StaminaRules staminaRules, CombatSettings settings, EngagementPlanner planner, TargetSelector targetSelector,
      FreeWinnerAssigner assigner) {
    this.turnPlayer = new EngagementTurnPlayer(diceRoller, initiativeResolver, turnOrchestrator, targetSelector,
        settings);
    this.staminaRules = staminaRules;
    this.settings = settings;
    this.planner = planner;
    this.assigner = assigner;
  }

  public BattleResult fight(BattleSetup setup, CombatContext context) {
    BattleRoster roster = BattleRoster.of(setup.teams());
    List<Engagement> engagements = planner.openingEngagements(roster);
    armInitialPowerStrikeCooldown(roster.all());
    Map<Fighter, Engagement> engagementByFighter = initialEngagementByFighter(engagements);

    List<RoundLogEntry> roundLog = new ArrayList<>();
    int round = 0;

    while (round < settings.maxTurns() && roster.teamsStillStanding().size() > 1) {
      round++;
      roundLog.add(playRound(round, roster, engagements, engagementByFighter, context));
    }

    return buildResult(roster, round, roundLog);
  }

  /**
   * Il colpo potente non e' disponibile fin dall'inizio della battaglia: ogni combattente comincia
   * gia' in cooldown, come se lo avesse appena eseguito. Pubblico perche' fa parte della
   * preparazione della battaglia richiamabile anche da chi assembla il {@link BattleEngine} da
   * fuori questo package (ad esempio l'adapter del duello 1v1).
   */
  public void armInitialPowerStrikeCooldown(List<Fighter> fighters) {
    int cooldownTurns = settings.powerStrikeWeights().cooldownTurns();
    for (Fighter fighter : fighters) {
      fighter.state().startPowerStrikeCooldown(cooldownTurns);
    }
  }

  private Map<Fighter, Engagement> initialEngagementByFighter(List<Engagement> engagements) {
    Map<Fighter, Engagement> engagementByFighter = new IdentityHashMap<>();
    for (Engagement engagement : engagements) {
      for (Fighter participant : engagement.participants()) {
        engagementByFighter.put(participant, engagement);
      }
    }
    return engagementByFighter;
  }

  private RoundLogEntry playRound(int roundNumber, BattleRoster roster, List<Engagement> engagements,
      Map<Fighter, Engagement> engagementByFighter, CombatContext context) {
    resetTurnStaminaCounters(roster.living());
    List<FighterVitals> startOfRoundVitals = vitalsSnapshot(roster.all());

    List<PlayedExchange> played = playActiveEngagements(roundNumber, roster, engagements, context,
        startOfRoundVitals);

    Set<Fighter> exchangeParticipants = exchangeParticipantsOf(played);
    recoverInactiveFighters(roster.living(), exchangeParticipants);

    List<String> events = reassignFreeWinners(roster, engagements, engagementByFighter);

    return buildRoundLogEntry(roundNumber, played, vitalsSnapshot(roster.all()), events);
  }

  private void resetTurnStaminaCounters(List<Fighter> living) {
    for (Fighter fighter : living) {
      fighter.state().resetTurnStaminaCounters();
    }
  }

  private List<PlayedExchange> playActiveEngagements(int roundNumber, BattleRoster roster,
      List<Engagement> engagements, CombatContext context, List<FighterVitals> startOfRoundVitals) {
    List<PlayedExchange> played = new ArrayList<>();
    for (Engagement engagement : engagements) {
      if (!engagement.isActive(roster)) {
        continue;
      }

      played.add(turnPlayer.play(roundNumber, engagement, roster, startOfRoundVitals, context));
      if (roster.teamsStillStanding().size() <= 1) {
        break;
      }
    }
    return played;
  }

  private Set<Fighter> exchangeParticipantsOf(List<PlayedExchange> played) {
    Set<Fighter> participants = Collections.newSetFromMap(new IdentityHashMap<>());
    for (PlayedExchange exchange : played) {
      participants.add(exchange.actor());
      participants.add(exchange.target());
    }
    return participants;
  }

  /**
   * Recupero passivo degli "inattivi": chi, fra i vivi, non e' stato ne' attore ne' bersaglio di
   * alcuno scambio in questo round. Il bersaglio di ogni scambio l'ha gia' ricevuto dentro
   * {@link TurnOrchestrator#playTurn}: vedi il Javadoc di classe per la partizione che esclude il
   * doppio recupero.
   */
  private void recoverInactiveFighters(List<Fighter> living, Set<Fighter> exchangeParticipants) {
    for (Fighter fighter : living) {
      if (!exchangeParticipants.contains(fighter)) {
        fighter.state().recoverStamina(staminaRules.passiveRecovery());
      }
    }
  }

  /**
   * Un combattente vivo e' "libero" quando lo scontro a cui appartiene (secondo
   * {@code engagementByFighter}, non secondo lo scontro stesso) non e' piu' attivo. Per ciascuno,
   * in ordine di roster, chiede all'assegnatore dove entrare fra gli scontri ancora attivi: se
   * trova posto, si unisce e la mappa di appartenenza viene aggiornata subito, cosi' che lo stesso
   * combattente non possa risultare "libero" una seconda volta piu' avanti in questo stesso round.
   */
  private List<String> reassignFreeWinners(BattleRoster roster, List<Engagement> engagements,
      Map<Fighter, Engagement> engagementByFighter) {
    List<String> events = new ArrayList<>();
    List<Engagement> activeEngagements = activeEngagementsOf(roster, engagements);

    for (Fighter fighter : roster.living()) {
      Engagement ownEngagement = engagementByFighter.get(fighter);
      if (ownEngagement.isActive(roster)) {
        continue;
      }

      Optional<Engagement> destination = assigner.assign(fighter, activeEngagements, roster);
      if (destination.isEmpty()) {
        continue;
      }

      Engagement joinedEngagement = destination.get();
      joinedEngagement.join(fighter);
      engagementByFighter.put(fighter, joinedEngagement);
      events.add(fighter.name() + ", libero, si unisce allo scontro " + joinedEngagement.id() + ".");
    }

    return events;
  }

  private List<Engagement> activeEngagementsOf(BattleRoster roster, List<Engagement> engagements) {
    return engagements.stream()
        .filter(engagement -> engagement.isActive(roster))
        .toList();
  }

  private RoundLogEntry buildRoundLogEntry(int roundNumber, List<PlayedExchange> played,
      List<FighterVitals> endOfRoundVitals, List<String> events) {
    List<EngagementTurn> turns = played.stream()
        .map(this::toEngagementTurn)
        .toList();
    return new RoundLogEntry(roundNumber, turns, endOfRoundVitals, events);
  }

  private EngagementTurn toEngagementTurn(PlayedExchange exchange) {
    TurnLogEntry entryWithStamina = exchange.entry().withStaminaChanges(staminaChangesOf(exchange));
    return new EngagementTurn(exchange.engagement().id(), exchange.actor().name(), exchange.target().name(),
        entryWithStamina);
  }

  private List<StaminaChange> staminaChangesOf(PlayedExchange exchange) {
    return List.of(toStaminaChange(exchange.actor()), toStaminaChange(exchange.target()));
  }

  private StaminaChange toStaminaChange(Fighter fighter) {
    return new StaminaChange(fighter.name(), fighter.state().staminaConsumedThisTurn(),
        fighter.state().staminaRecoveredThisTurn());
  }

  private List<FighterVitals> vitalsSnapshot(List<Fighter> fighters) {
    return fighters.stream()
        .map(this::toVitals)
        .toList();
  }

  private FighterVitals toVitals(Fighter fighter) {
    return new FighterVitals(fighter.name(), fighter.state().currentHealth(), fighter.ratings().maxHealth(),
        fighter.state().currentStamina(), fighter.ratings().maxStamina());
  }

  private BattleResult buildResult(BattleRoster roster, int rounds, List<RoundLogEntry> roundLog) {
    List<FighterVitals> finalVitals = vitalsSnapshot(roster.all());
    List<Team> standing = roster.teamsStillStanding();

    if (standing.size() == 1) {
      return new BattleResult(CombatOutcome.VICTORY, Optional.of(standing.get(0)), rounds, roundLog, finalVitals,
          List.of(), List.of());
    }
    if (standing.isEmpty()) {
      return new BattleResult(CombatOutcome.DRAW, Optional.empty(), rounds, roundLog, finalVitals, List.of(),
          List.of());
    }
    return buildTimeoutResult(roster, rounds, roundLog, finalVitals);
  }

  private BattleResult buildTimeoutResult(BattleRoster roster, int rounds, List<RoundLogEntry> roundLog,
      List<FighterVitals> finalVitals) {
    Map<Integer, Double> healthRatioByTeamIndex = aggregatedHealthRatioByTeamIndex(roster);
    List<Fighter> all = roster.all();

    List<Scorecard> scorecards = all.stream()
        .map(fighter -> buildScorecard(fighter, roster, healthRatioByTeamIndex))
        .toList();

    List<TeamScore> teamScores = teamScoresOf(roster, all, scorecards);

    return decideTimeoutOutcome(roster.teams(), rounds, roundLog, finalVitals, scorecards, teamScores);
  }

  /**
   * Le squadre sono indicizzate per {@code index} e non usate direttamente come chiave: {@link Team}
   * e' un record che contiene i {@link Fighter}, quindi il suo {@code hashCode} dipende dagli
   * identity hash dei membri. Funzionerebbe, ma significherebbe usare come chiave un contenitore di
   * oggetti mutabili, e confrontare due squadre con {@code !=} presupporrebbe che il roster
   * restituisca sempre la stessa istanza. L'indice e' l'identita' stabile e dichiarata.
   */
  private Map<Integer, Double> aggregatedHealthRatioByTeamIndex(BattleRoster roster) {
    Map<Integer, Double> ratios = new HashMap<>();
    for (Team team : roster.teams()) {
      ratios.put(team.index(), aggregatedHealthRatio(team));
    }
    return ratios;
  }

  /**
   * Rapporto Salute corrente/massima aggregato di UNA squadra: {@code somma(currentHealth) /
   * somma(maxHealth)}, non la media dei rapporti individuali. La media maschererebbe la perdita
   * di un membro intero (Salute 0 su un pool comunque massimo), l'aggregato no.
   */
  private double aggregatedHealthRatio(Team team) {
    int currentHealthSum = 0;
    int maxHealthSum = 0;
    for (Fighter member : team.members()) {
      currentHealthSum += member.state().currentHealth();
      maxHealthSum += member.ratings().maxHealth();
    }
    return CombatFormulas.ratio(currentHealthSum, maxHealthSum);
  }

  private Scorecard buildScorecard(Fighter fighter, BattleRoster roster, Map<Integer, Double> healthRatioByTeamIndex) {
    CombatSettings.ScoreWeights weights = settings.scoreWeights();
    Team ownTeam = roster.teamOf(fighter);
    double ownHealthRatio = healthRatioByTeamIndex.get(ownTeam.index());
    double opponentHealthRatio = opponentHealthRatio(roster, ownTeam, healthRatioByTeamIndex);
    boolean hasHealthAdvantage = ownHealthRatio > opponentHealthRatio;

    int hitsLanded = fighter.state().hitsLanded();
    int parries = fighter.state().parries();
    int dodges = fighter.state().dodges();

    int healthPoints = hasHealthAdvantage ? weights.healthAdvantage() : 0;
    int hitPoints = hitsLanded * weights.hitLanded();
    int parryPoints = parries * weights.parry();
    int dodgePoints = dodges * weights.dodge();
    int total = CombatFormulas.combatScore(weights, hasHealthAdvantage, hitsLanded, parries, dodges);

    return new Scorecard(fighter.name(), ownHealthRatio, opponentHealthRatio, healthPoints, hitsLanded, hitPoints,
        parries, parryPoints, dodges, dodgePoints, weights, total);
  }

  /**
   * V1 supporta esattamente 2 squadre ({@link BattleSetup}): la squadra "nemica" e' semplicemente
   * l'altra.
   */
  private double opponentHealthRatio(BattleRoster roster, Team ownTeam, Map<Integer, Double> healthRatioByTeamIndex) {
    for (Team team : roster.teams()) {
      if (team.index() != ownTeam.index()) {
        return healthRatioByTeamIndex.get(team.index());
      }
    }
    throw new IllegalStateException("nessuna squadra avversaria trovata per la squadra: " + ownTeam.name());
  }

  private List<TeamScore> teamScoresOf(BattleRoster roster, List<Fighter> all, List<Scorecard> scorecards) {
    Map<Integer, Integer> totalsByTeamIndex = new HashMap<>();
    for (int i = 0; i < all.size(); i++) {
      int teamIndex = roster.teamOf(all.get(i)).index();
      totalsByTeamIndex.merge(teamIndex, scorecards.get(i).total(), Integer::sum);
    }

    return roster.teams().stream()
        .map(team -> new TeamScore(team.name(), totalsByTeamIndex.getOrDefault(team.index(), 0)))
        .toList();
  }

  private BattleResult decideTimeoutOutcome(List<Team> teams, int rounds, List<RoundLogEntry> roundLog,
      List<FighterVitals> finalVitals, List<Scorecard> scorecards, List<TeamScore> teamScores) {
    int bestIndex = 0;
    boolean tied = false;

    for (int i = 1; i < teams.size(); i++) {
      if (teamScores.get(i).total() > teamScores.get(bestIndex).total()) {
        bestIndex = i;
        tied = false;
      } else if (teamScores.get(i).total() == teamScores.get(bestIndex).total()) {
        tied = true;
      }
    }

    if (tied) {
      return new BattleResult(CombatOutcome.DRAW, Optional.empty(), rounds, roundLog, finalVitals, scorecards,
          teamScores);
    }
    return new BattleResult(CombatOutcome.TIMEOUT_DECISION, Optional.of(teams.get(bestIndex)), rounds, roundLog,
        finalVitals, scorecards, teamScores);
  }
}
