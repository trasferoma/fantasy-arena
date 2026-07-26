package it.fantasyarena.combat.io;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.battle.BattleResult;
import it.fantasyarena.combat.battle.BattleSetup;
import it.fantasyarena.combat.battle.EngagementTurn;
import it.fantasyarena.combat.battle.RoundLogEntry;
import it.fantasyarena.combat.battle.Team;
import it.fantasyarena.combat.battle.TeamScore;
import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.CombatOutcome;
import it.fantasyarena.combat.result.FighterVitals;
import it.fantasyarena.combat.result.Scorecard;
import it.fantasyarena.combat.result.TurnLogEntry;
import it.fantasyarena.combat.testsupport.CombatFixtures;

/**
 * Verifica {@link ConsoleBattleLogger}: schieramenti, log di round (intestazione, riga di
 * scontro, righe compatte indentate, eventi e stato) ed esito finale, sia {@code VICTORY} sia
 * {@code TIMEOUT_DECISION} (con punteggi di squadra e dettaglio a punti). Cattura di
 * {@code System.out} sullo stampo di {@link ConsoleCombatLoggerOutcomeTest}.
 */
class ConsoleBattleLoggerTest {

  private final ConsoleBattleLogger logger = new ConsoleBattleLogger();
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
  void reportSetupMostraLIntestazioneELeSchedeNumerateProgressivamente() {
    Fighter alice = strongFighter("Alice");
    Fighter bob = weakFighter("Bob");
    BattleSetup setup = BattleSetup.of(List.of(List.of(alice), List.of(bob)));

    logger.reportSetup(setup);

    String output = capturedOutput();
    assertTrue(output.contains("=== Schieramenti ==="));
    assertTrue(output.contains("Squadra 1:"));
    assertTrue(output.contains("[1] Alice"));
    assertTrue(output.contains("Squadra 2:"));
    assertTrue(output.contains("[2] Bob"));
  }

  @Test
  void logRoundMostraLaScenaConFrecciaEventiEStato() {
    Fighter alice = strongFighter("Alice");
    Fighter bob = weakFighter("Bob");
    BattleSetup setup = BattleSetup.of(List.of(List.of(alice), List.of(bob)));
    logger.reportSetup(setup);

    TurnLogEntry turnEntry = new TurnLogEntry(1, "Alice attacca Bob e lo colpisce");
    EngagementTurn turn = new EngagementTurn(0, 0, 1, List.of(0, 1), turnEntry);
    FighterVitals aliceVitals = new FighterVitals(alice.name(), 40, 50, 30, 40);
    FighterVitals bobVitals = new FighterVitals(bob.name(), 20, 50, 25, 40);
    RoundLogEntry round = new RoundLogEntry(1, List.of(turn), List.of(aliceVitals, bobVitals),
        List.of("Bob, libero, si unisce allo scontro 0."));

    logger.logRound(round);

    String output = capturedOutput();
    assertTrue(output.contains("Round 1"));
    assertTrue(output.contains("Alice"));
    assertTrue(output.contains("Bob"));
    assertTrue(output.contains("Vita:    40/50"));
    assertTrue(output.contains("Vita:    20/50"));
    assertTrue(output.contains("Stamina: 30/40"));
    assertTrue(output.contains("Stamina: 25/40"));
    assertTrue(output.contains("=".repeat(19) + ">"), "la freccia deve puntare verso il bersaglio (fazione 1)");
    assertTrue(output.contains("Bob, libero, si unisce allo scontro 0."));
  }

  @Test
  void reportOutcomeVictoryMostraLaSquadraVincitriceEIRound() {
    Fighter alice = strongFighter("Alice");
    Fighter bob = weakFighter("Bob");
    Team teamA = new Team(0, "Squadra 1", List.of(alice));
    BattleResult result = new BattleResult(CombatOutcome.VICTORY, Optional.of(teamA), 7, List.of(),
        finalVitals(alice, bob), List.of(), List.of());

    logger.reportOutcome(result);

    String output = capturedOutput();
    assertTrue(output.contains("=== Esito della battaglia ==="));
    assertTrue(output.contains("Vince: Squadra 1 (7 round)"));
    assertTrue(output.contains("Stato -> Alice"));
  }

  @Test
  void reportOutcomeTimeoutMostraPunteggiDiSquadraEDecisioneAiPunti() {
    Fighter alice = strongFighter("Alice");
    Fighter bob = weakFighter("Bob");
    Team teamA = new Team(0, "Squadra 1", List.of(alice));
    List<Scorecard> scorecards = List.of(
        scorecard(alice.name(), 0.60, 0.52, 5, 3, true),
        scorecard(bob.name(), 0.52, 0.60, 4, 1, false));
    List<TeamScore> teamScores = List.of(new TeamScore("Squadra 1", 15), new TeamScore("Squadra 2", 9));
    BattleResult result = new BattleResult(CombatOutcome.TIMEOUT_DECISION, Optional.of(teamA), 20, List.of(),
        finalVitals(alice, bob), scorecards, teamScores);

    logger.reportOutcome(result);

    String output = capturedOutput();
    assertTrue(output.contains("Timeout ai punti, vince: Squadra 1 (20 round)"));
    assertTrue(output.contains("Punteggi di squadra:"));
    assertTrue(output.contains("  Squadra 1: 15"));
    assertTrue(output.contains("  Squadra 2: 9"));
    assertTrue(output.contains("Decisione ai punti:"));
    assertTrue(output.contains("Alice: salute +2 (60% vs 52%), colpi a segno 5x2=10, parate 3x1=3, schivate 0x1=0  ->  15"));
  }

  private Scorecard scorecard(String fighterName, double healthRatio, double opponentHealthRatio, int hitsLanded,
      int parries, boolean healthAdvantage) {
    CombatSettings.ScoreWeights weights = CombatSettings.defaults().scoreWeights();
    int healthPoints = healthAdvantage ? weights.healthAdvantage() : 0;
    int hitPoints = hitsLanded * weights.hitLanded();
    int parryPoints = parries * weights.parry();
    int total = healthPoints + hitPoints + parryPoints;
    return new Scorecard(fighterName, healthRatio, opponentHealthRatio, healthPoints, hitsLanded, hitPoints, parries,
        parryPoints, 0, 0, weights, total);
  }

  private List<FighterVitals> finalVitals(Fighter first, Fighter second) {
    return List.of(
        new FighterVitals(first.name(), first.ratings().maxHealth(), first.ratings().maxHealth(),
            first.ratings().maxStamina(), first.ratings().maxStamina()),
        new FighterVitals(second.name(), second.ratings().maxHealth(), second.ratings().maxHealth(),
            second.ratings().maxStamina(), second.ratings().maxStamina()));
  }

  private Fighter strongFighter(String name) {
    return CombatFixtures.createFighter(name, 16, 14, 14, 45, 12, 8, 5);
  }

  private Fighter weakFighter(String name) {
    return CombatFixtures.createFighter(name, 10, 10, 10, 35, 8, 4, 2);
  }

  private String capturedOutput() {
    return capturedOut.toString(StandardCharsets.UTF_8);
  }
}
