package it.fantasyarena.combat;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import it.fantasyarena.combat.chronicle.ArenaChronicle;
import it.fantasyarena.combat.chronicle.ChallengerBudgetChronicle;
import it.fantasyarena.combat.chronicle.CombatantSnapshot;
import it.fantasyarena.combat.chronicle.RunConclusion;
import it.fantasyarena.combat.chronicle.TrialChronicle;
import it.fantasyarena.combat.chronicle.TrialShape;
import it.fantasyarena.combat.factory.FighterFactory;
import it.fantasycombatsystem.battle.EngagementTurn;
import it.fantasycombatsystem.battle.RoundLogEntry;
import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.result.ActionOutcome;
import it.fantasycombatsystem.result.FighterVitals;

/**
 * Harness temporaneo di misura per il lavoro descritto in
 * {@code spec-bilanciamento-pressione-avversari.md} e
 * {@code implementation-bilanciamento-pressione-avversari.md}: gioca {@value #RUNS} corse con
 * {@link SilentArenaRun} e stampa su stdout un rapporto testuale leggibile a occhio, da incollare
 * nel registro del piano prima e dopo ogni leva di bilanciamento.
 *
 * <p>Comando verificato per l'esecuzione:
 *
 * <pre>{@code
 * mvn -q test-compile exec:java -Dexec.mainClass=it.fantasyarena.combat.BalanceProbe -Dexec.classpathScope=test
 * }</pre>
 *
 * <p>Non è un test JUnit, per due motivi. Primo: non ha un esito verde o rosso, è una misura
 * statistica su un campione casuale, da leggere a occhio e confrontare con altre misure dello
 * stesso genere raccolte prima e dopo una modifica. Secondo: {@value #RUNS} corse per ogni
 * esecuzione di {@code mvn test} costerebbero minuti a ogni build, per un controllo che non
 * asserisce nulla. Il nome non finisce per {@code Test} apposta: surefire non lo raccoglie, e
 * {@code mvn test} resta verde e veloce.
 *
 * <p>È una classe temporanea: il piano di bilanciamento ne prevede la rimozione a fine lavoro,
 * dopo che le misure di baseline, intermedia e finale sono state trascritte nel registro.
 *
 * <p>Legge soltanto i numeri già presenti nella cronaca: nessuna regola di combattimento è
 * ricalcolata qui, né i rating né il danno.
 */
public final class BalanceProbe {

  private static final int RUNS = 1000;

  private BalanceProbe() {
  }

  public static void main(String[] args) {
    RunAggregator aggregator = simulate(RUNS);
    new Report(aggregator).print();
  }

  private static RunAggregator simulate(int runCount) {
    CombatSettings settings = CombatSettings.defaults();
    RunAggregator aggregator = new RunAggregator();
    for (int run = 0; run < runCount; run++) {
      ArenaChronicle chronicle = new SilentArenaRun(settings).get();
      aggregator.accept(chronicle);
    }
    return aggregator;
  }

  /**
   * Accumula le grandezze del rapporto su tutte le corse giocate: la distribuzione di dove finisce
   * la corsa a livello globale, e le grandezze per numero di prova dentro un {@link TrialAccumulator}
   * per prova. Nessun dato viene ricalcolato: ogni metodo si limita a leggere la cronaca e a
   * sommarlo dentro un contatore.
   */
  private static final class RunAggregator {

    private long totalRuns;
    private long runsReachingTheEnd;
    private final Map<Integer, Map<RoundOutcome, Long>> conclusionsByLastTrial = new TreeMap<>();
    private final Map<Integer, TrialAccumulator> trialsByNumber = new TreeMap<>();

    void accept(ArenaChronicle chronicle) {
      totalRuns++;
      acceptConclusion(chronicle.conclusion(), chronicle.plannedTrials());
      chronicle.trials().forEach(this::acceptTrial);
    }

    private void acceptConclusion(RunConclusion conclusion, int plannedTrials) {
      conclusionsByLastTrial.computeIfAbsent(conclusion.lastTrial(), lastTrial -> new EnumMap<>(RoundOutcome.class))
          .merge(conclusion.outcome(), 1L, Long::sum);
      if (conclusion.lastTrial() == plannedTrials) {
        runsReachingTheEnd++;
      }
    }

    private void acceptTrial(TrialChronicle trial) {
      trialsByNumber.computeIfAbsent(trial.number(), number -> new TrialAccumulator()).accept(trial);
    }

    long totalRuns() {
      return totalRuns;
    }

    long runsReachingTheEnd() {
      return runsReachingTheEnd;
    }

    Map<Integer, Map<RoundOutcome, Long>> conclusionsByLastTrial() {
      return conclusionsByLastTrial;
    }

    Map<Integer, TrialAccumulator> trialsByNumber() {
      return trialsByNumber;
    }
  }

  /**
   * Le grandezze di un solo numero di prova, sommate su tutte le corse che l'hanno giocata. Il
   * budget e il pavimento contano solo le prove a sfidanti generati ({@link TrialChronicle#budget()}
   * non nullo): lo specchio non dichiara un monte proprio e non passa da nessuno sconto. Il danno
   * si legge per due strade distinte, perché il duello non porta indici di roster: nella battaglia
   * si osservano i colpi degli sfidanti uno per uno, nel duello si usa la vita persa dal
   * protagonista nell'intera prova come proxy dichiarato — le due misure non sono la stessa cosa e
   * il rapporto le mostra separate.
   */
  private static final class TrialAccumulator {

    private static final int PROTAGONIST_TEAM_INDEX = 0;
    private static final int PROTAGONIST_ROSTER_INDEX = 0;

    private long playedCount;
    private long budgetTrialsCount;
    private long stationPointsSum;
    private long luckDiscountSum;
    private long squadPointsSum;
    private double discountPercentSum;
    private long floorTouchedCount;
    private double protagonistOffensiveSum;
    private double protagonistDefensiveSum;
    private double challengerOffensiveSum;
    private double challengerDefensiveSum;
    private long challengerSampleCount;
    private long battleHitCount;
    private long battleDamageSum;
    private long battleOneDamageHitCount;
    private long duelOccurrences;
    private long duelHealthLostSum;

    void accept(TrialChronicle trial) {
      playedCount++;
      acceptBudget(trial);
      acceptRatings(trial.roster());
      acceptDamage(trial);
    }

    private void acceptBudget(TrialChronicle trial) {
      ChallengerBudgetChronicle budget = trial.budget();
      if (budget == null) {
        return;
      }

      budgetTrialsCount++;
      stationPointsSum += budget.stationPoints();
      luckDiscountSum += budget.luckDiscount();
      squadPointsSum += budget.squadPoints();
      discountPercentSum += discountPercentOf(budget);
      if (isAtFloor(budget, challengerCountOf(trial.roster()))) {
        floorTouchedCount++;
      }
    }

    private void acceptRatings(List<CombatantSnapshot> roster) {
      for (CombatantSnapshot combatant : roster) {
        if (combatant.teamIndex() == PROTAGONIST_TEAM_INDEX) {
          protagonistOffensiveSum += combatant.offensiveRating();
          protagonistDefensiveSum += combatant.defensiveRating();
        } else {
          challengerOffensiveSum += combatant.offensiveRating();
          challengerDefensiveSum += combatant.defensiveRating();
          challengerSampleCount++;
        }
      }
    }

    private void acceptDamage(TrialChronicle trial) {
      if (trial.shape() == TrialShape.BATTLE) {
        acceptBattleHits(trial.rounds());
      } else {
        acceptDuelHealthLostProxy(trial.roster(), trial.finalVitals());
      }
    }

    /**
     * Scorre ogni scambio di ogni round e conserva solo quelli in cui l'attaccante non è il
     * protagonista: sono i colpi degli sfidanti, l'unico dato che interessa questa sezione del
     * rapporto.
     */
    private void acceptBattleHits(List<RoundLogEntry> rounds) {
      for (RoundLogEntry round : rounds) {
        for (EngagementTurn turn : round.turns()) {
          if (turn.attackerIndex() != PROTAGONIST_ROSTER_INDEX) {
            acceptLandedHit(turn.turn().action());
          }
        }
      }
    }

    /**
     * Conta solo i colpi che hanno davvero attraversato la formula del danno ({@code HIT} e
     * {@code PARRIED}): un fallito o uno schivato non ha inflitto niente, e mescolarlo abbasserebbe
     * sia il danno medio sia la quota dei colpi fermati dal pavimento di un punto, senza che sia
     * mai stato un colpo che quel pavimento potesse fermare.
     */
    private void acceptLandedHit(ActionOutcome action) {
      if (action == null || !isLandedHit(action.kind())) {
        return;
      }

      battleHitCount++;
      battleDamageSum += action.damage();
      if (action.damage() == 1) {
        battleOneDamageHitCount++;
      }
    }

    private void acceptDuelHealthLostProxy(List<CombatantSnapshot> roster, List<FighterVitals> finalVitals) {
      int maxHealth = roster.get(PROTAGONIST_ROSTER_INDEX).maxHealth();
      int currentHealth = finalVitals.get(PROTAGONIST_ROSTER_INDEX).currentHealth();

      duelOccurrences++;
      duelHealthLostSum += maxHealth - currentHealth;
    }

    private static boolean isLandedHit(ActionOutcome.Kind kind) {
      return kind == ActionOutcome.Kind.HIT || kind == ActionOutcome.Kind.PARRIED;
    }

    private static double discountPercentOf(ChallengerBudgetChronicle budget) {
      return budget.stationPoints() == 0 ? 0.0 : budget.luckDiscount() * 100.0 / budget.stationPoints();
    }

    private static boolean isAtFloor(ChallengerBudgetChronicle budget, int challengerCount) {
      return budget.squadPoints() == FighterFactory.MINIMUM_CHARACTERISTIC_POINTS_PER_CHALLENGER * challengerCount;
    }

    private static int challengerCountOf(List<CombatantSnapshot> roster) {
      return (int) roster.stream().filter(combatant -> combatant.teamIndex() != PROTAGONIST_TEAM_INDEX).count();
    }

    long budgetTrialsCount() {
      return budgetTrialsCount;
    }

    double averageStationPoints() {
      return average(stationPointsSum, budgetTrialsCount);
    }

    double averageLuckDiscount() {
      return average(luckDiscountSum, budgetTrialsCount);
    }

    double averageSquadPoints() {
      return average(squadPointsSum, budgetTrialsCount);
    }

    double averageDiscountPercent() {
      return budgetTrialsCount == 0 ? 0.0 : discountPercentSum / budgetTrialsCount;
    }

    long floorTouchedCount() {
      return floorTouchedCount;
    }

    double averageProtagonistOffensive() {
      return average(protagonistOffensiveSum, playedCount);
    }

    double averageProtagonistDefensive() {
      return average(protagonistDefensiveSum, playedCount);
    }

    double averageChallengerOffensive() {
      return average(challengerOffensiveSum, challengerSampleCount);
    }

    double averageChallengerDefensive() {
      return average(challengerDefensiveSum, challengerSampleCount);
    }

    long battleHitCount() {
      return battleHitCount;
    }

    double averageBattleDamagePerHit() {
      return average(battleDamageSum, battleHitCount);
    }

    double oneDamageHitPercent() {
      return battleHitCount == 0 ? 0.0 : battleOneDamageHitCount * 100.0 / battleHitCount;
    }

    long duelOccurrences() {
      return duelOccurrences;
    }

    double averageDuelHealthLost() {
      return average(duelHealthLostSum, duelOccurrences);
    }

    private static double average(double sum, long count) {
      return count == 0 ? 0.0 : sum / count;
    }
  }

  /**
   * Stampa il rapporto aggregato in cinque sezioni, una per grandezza richiesta dal compito. Non
   * calcola nulla di nuovo: legge solo gli accumulatori già pronti di {@link RunAggregator} e li
   * formatta.
   */
  private static final class Report {

    private final RunAggregator aggregator;

    Report(RunAggregator aggregator) {
      this.aggregator = aggregator;
    }

    void print() {
      System.out.println("Rapporto di bilanciamento su " + aggregator.totalRuns() + " corse");
      printConclusionSection();
      printBudgetSection();
      printFloorSection();
      printRatingSection();
      printDamageSection();
    }

    private void printConclusionSection() {
      System.out.println();
      System.out.println("1) Dove finisce la corsa");
      System.out.println("Corse che arrivano in fondo al percorso: " + aggregator.runsReachingTheEnd() + " ("
          + formatPercent(aggregator.runsReachingTheEnd(), aggregator.totalRuns()) + "%)");
      System.out.println("Prova finale | vinta | pareggio | caduta | totale | % sulle corse totali");
      aggregator.conclusionsByLastTrial().forEach((lastTrial, outcomes) -> printConclusionRow(lastTrial, outcomes));
    }

    private void printConclusionRow(int lastTrial, Map<RoundOutcome, Long> outcomes) {
      long won = outcomes.getOrDefault(RoundOutcome.WON, 0L);
      long stood = outcomes.getOrDefault(RoundOutcome.STOOD_WITHOUT_WINNING, 0L);
      long fell = outcomes.getOrDefault(RoundOutcome.FELL, 0L);
      long total = won + stood + fell;
      System.out.printf(Locale.ITALY, "%12d | %5d | %8d | %6d | %6d | %6.1f%%%n", lastTrial, won, stood, fell, total,
          percentOf(total, aggregator.totalRuns()));
    }

    private void printBudgetSection() {
      System.out.println();
      System.out.println("2) Sconto della fortuna per prova (solo sfidanti generati)");
      System.out.println("Prova | occorrenze | monte dichiarato medio | sconto medio | monte effettivo medio | sconto % medio");
      aggregator.trialsByNumber().forEach((number, stats) -> printBudgetRow(number, stats));
    }

    private void printBudgetRow(int trialNumber, TrialAccumulator stats) {
      if (stats.budgetTrialsCount() == 0) {
        return;
      }
      System.out.printf(Locale.ITALY, "%5d | %10d | %22.1f | %12.1f | %21.1f | %13.1f%%%n", trialNumber,
          stats.budgetTrialsCount(), stats.averageStationPoints(), stats.averageLuckDiscount(),
          stats.averageSquadPoints(), stats.averageDiscountPercent());
    }

    private void printFloorSection() {
      long totalBudgetTrials = 0;
      long totalFloorTouched = 0;
      for (TrialAccumulator stats : aggregator.trialsByNumber().values()) {
        totalBudgetTrials += stats.budgetTrialsCount();
        totalFloorTouched += stats.floorTouchedCount();
      }

      System.out.println();
      System.out.println("3) Quante volte si tocca il pavimento (squadPoints == 7 x numero sfidanti)");
      System.out.println("Prove giocate con sfidanti generati: " + totalBudgetTrials);
      System.out.println(
          "Volte al pavimento: " + totalFloorTouched + " (" + formatPercent(totalFloorTouched, totalBudgetTrials)
              + "% delle prove giocate)");
    }

    private void printRatingSection() {
      System.out.println();
      System.out.println("4) Rating medi per prova");
      System.out.println("Prova | OFF protagonista | DEF protagonista | OFF sfidanti | DEF sfidanti");
      aggregator.trialsByNumber().forEach((number, stats) -> printRatingRow(number, stats));
    }

    private void printRatingRow(int trialNumber, TrialAccumulator stats) {
      System.out.printf(Locale.ITALY, "%5d | %17.1f | %17.1f | %12.1f | %12.1f%n", trialNumber,
          stats.averageProtagonistOffensive(), stats.averageProtagonistDefensive(), stats.averageChallengerOffensive(),
          stats.averageChallengerDefensive());
    }

    /**
     * Le due forme non condividono la stessa unità di misura — colpo per colpo nella battaglia,
     * intera prova nel duello — quindi restano due tabelle separate invece di una sola riga con
     * colonne vuote: unirle suggerirebbe un confronto che i dati non permettono di fare.
     */
    private void printDamageSection() {
      System.out.println();
      System.out.println("5) Danno inflitto dagli sfidanti al protagonista");
      System.out.println("Battaglia: danno medio per colpo (HIT/PARRIED) e quota di colpi da 1 danno, contati colpo "
          + "per colpo.");
      System.out.println("Prova | colpi osservati | danno medio a colpo | colpi da 1 danno %");
      aggregator.trialsByNumber().forEach((number, stats) -> printBattleDamageRow(number, stats));

      System.out.println();
      System.out.println("Duello: nessun indice per attribuire i colpi, quindi si usa la vita persa dal "
          + "protagonista nell'intera prova come proxy dichiarato, non un dato per-colpo: non va letto come "
          + "omogeneo alla misura della battaglia.");
      System.out.println("Prova | prove giocate | vita persa media (proxy)");
      aggregator.trialsByNumber().forEach((number, stats) -> printDuelDamageRow(number, stats));
    }

    private void printBattleDamageRow(int trialNumber, TrialAccumulator stats) {
      if (stats.battleHitCount() == 0) {
        return;
      }
      System.out.printf(Locale.ITALY, "%5d | %15d | %20.1f | %19.1f%%%n", trialNumber, stats.battleHitCount(),
          stats.averageBattleDamagePerHit(), stats.oneDamageHitPercent());
    }

    private void printDuelDamageRow(int trialNumber, TrialAccumulator stats) {
      if (stats.duelOccurrences() == 0) {
        return;
      }
      System.out.printf(Locale.ITALY, "%5d | %13d | %25.1f%n", trialNumber, stats.duelOccurrences(),
          stats.averageDuelHealthLost());
    }

    private static double percentOf(long numerator, long denominator) {
      return denominator == 0 ? 0.0 : numerator * 100.0 / denominator;
    }

    private static String formatPercent(long numerator, long denominator) {
      return String.format(Locale.ITALY, "%.1f", percentOf(numerator, denominator));
    }
  }
}