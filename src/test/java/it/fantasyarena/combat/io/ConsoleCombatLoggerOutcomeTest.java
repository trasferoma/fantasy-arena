package it.fantasyarena.combat.io;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.result.CombatOutcome;
import it.fantasycombatsystem.result.CombatResult;
import it.fantasycombatsystem.result.FighterVitals;
import it.fantasycombatsystem.result.InitiativeOverride;
import it.fantasycombatsystem.result.InitiativeReport;
import it.fantasycombatsystem.result.Scorecard;
import it.fantasycombatsystem.result.TurnHighlight;
import it.fantasycombatsystem.result.TurnLogEntry;
import it.fantasycombatsystem.testsupport.CombatFixtures;

/**
 * Verifica la narrazione finale di {@link ConsoleCombatLogger#reportOutcome}: favorito
 * pre-scontro (via {@code FavoriteEstimator}), ribaltone rispetto al pronostico o pronostico
 * rispettato, citazione di un evento notevole con il numero di turno, e i casi {@code DRAW} e
 * {@code TIMEOUT_DECISION}.
 */
class ConsoleCombatLoggerOutcomeTest {

  private final ConsoleCombatLogger logger = new ConsoleCombatLogger();
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
  void mostraIlPronosticoConCorniceAInizioBattaglia() {
    Fighter alice = strongFighter("Alice");
    Fighter bob = weakFighter("Bob");

    logger.reportMatchup(alice, bob);

    String output = capturedOutput();
    assertTrue(output.contains("Pronostico: il favorito è Alice (attacco+difesa "));
    assertTrue(output.contains("--------------------"));
  }

  @Test
  void ilPronosticoDichiaraEquilibrioSenzaFavoritoNetto() {
    Fighter alice = strongFighter("Alice");
    Fighter twin = strongFighter("Twin");

    logger.reportMatchup(alice, twin);

    String output = capturedOutput();
    assertTrue(output.contains("Pronostico: scontro equilibrato, nessun favorito netto"));
  }

  @Test
  void citaIlFavoritoEIlPronosticoRispettatoQuandoVinceIlFavorito() {
    Fighter alice = strongFighter("Alice");
    Fighter bob = weakFighter("Bob");
    CombatResult result = victoryResult(alice, bob, alice);

    logger.reportOutcome(result, alice, bob);

    String output = capturedOutput();
    assertTrue(output.contains("Favorito alla vigilia: Alice."));
    assertTrue(output.contains("Vince Alice: pronostico rispettato."));
  }

  @Test
  void citaIlRibaltoneQuandoVinceIlNonFavorito() {
    Fighter alice = strongFighter("Alice");
    Fighter bob = weakFighter("Bob");
    CombatResult result = victoryResult(alice, bob, bob);

    logger.reportOutcome(result, alice, bob);

    String output = capturedOutput();
    assertTrue(output.contains("Favorito alla vigilia: Alice."));
    assertTrue(output.contains("Vince Bob: ribaltone rispetto al pronostico!"));
  }

  @Test
  void dichiaraEquilibrioSenzaFavoritoNetto() {
    Fighter alice = strongFighter("Alice");
    Fighter twin = strongFighter("Twin");
    CombatResult result = victoryResult(alice, twin, alice);

    logger.reportOutcome(result, alice, twin);

    String output = capturedOutput();
    assertTrue(output.contains("Alla vigilia equilibrato, nessun favorito netto."));
    assertTrue(output.contains("Vince Alice."));
  }

  @Test
  void citaUnEventoNotevoleConIlNumeroDiTurno() {
    Fighter alice = strongFighter("Alice");
    Fighter bob = weakFighter("Bob");
    TurnLogEntry highlightedTurn = new TurnLogEntry(5, "Alice attacca Bob e lo colpisce")
        .withInitiative(initiativeChosenBy("Alice"))
        .withHighlights(List.of(TurnHighlight.CRITICAL));
    CombatResult result = new CombatResult(
        CombatOutcome.VICTORY, alice, 5, List.of(highlightedTurn), finalVitals(alice, bob), List.of());

    logger.reportOutcome(result, alice, bob);

    String output = capturedOutput();
    assertTrue(output.contains("Da ricordare: il colpo critico di Alice al turno 5."));
  }

  @Test
  void citaIlColpoPotenteQuandoENelPrimoHighlightNotevole() {
    Fighter alice = strongFighter("Alice");
    Fighter bob = weakFighter("Bob");
    TurnLogEntry highlightedTurn = new TurnLogEntry(7, "Alice tenta un colpo potente su Bob e lo colpisce")
        .withInitiative(initiativeChosenBy("Alice"))
        .withHighlights(List.of(TurnHighlight.POWER_STRIKE, TurnHighlight.HEAVY_BLOW));
    CombatResult result = new CombatResult(
        CombatOutcome.VICTORY, alice, 7, List.of(highlightedTurn), finalVitals(alice, bob), List.of());

    logger.reportOutcome(result, alice, bob);

    String output = capturedOutput();
    assertTrue(output.contains("Da ricordare: il colpo potente di Alice al turno 7."),
        "il colpo potente prevale sul colpo pesante quando entrambi sono presenti");
  }

  @Test
  void ilColpoCriticoPrevaleSulColpoPotenteQuandoCompresenti() {
    Fighter alice = strongFighter("Alice");
    Fighter bob = weakFighter("Bob");
    TurnLogEntry highlightedTurn = new TurnLogEntry(9, "Alice tenta un colpo potente su Bob e lo colpisce")
        .withInitiative(initiativeChosenBy("Alice"))
        .withHighlights(List.of(TurnHighlight.CRITICAL, TurnHighlight.POWER_STRIKE));
    CombatResult result = new CombatResult(
        CombatOutcome.VICTORY, alice, 9, List.of(highlightedTurn), finalVitals(alice, bob), List.of());

    logger.reportOutcome(result, alice, bob);

    String output = capturedOutput();
    assertTrue(output.contains("Da ricordare: il colpo critico di Alice al turno 9."),
        "il critico prevale sul colpo potente nella precedenza della citazione");
  }

  @Test
  void gestisceIlPareggioSenzaConfermareOSmentireIlPronostico() {
    Fighter alice = strongFighter("Alice");
    Fighter bob = weakFighter("Bob");
    List<Scorecard> scorecards = List.of(
        scorecard(alice.name(), 0.60, 0.60, 3, 0, false),
        scorecard(bob.name(), 0.60, 0.60, 3, 0, false));
    Fighter noWinner = null;
    CombatResult result = new CombatResult(
        CombatOutcome.DRAW, noWinner, 10, List.of(), finalVitals(alice, bob), scorecards);

    logger.reportOutcome(result, alice, bob);

    String output = capturedOutput();
    assertTrue(output.contains("Pareggio dopo 10 turni."));
    assertTrue(output.contains("Pareggio: pronostico né confermato né smentito."));
    assertTrue(output.contains("Decisione ai punti:"));
    assertTrue(output.contains("Alice: salute +0 (60% vs 60%), colpi a segno 3x2=6, parate 0x1=0, schivate 0x1=0  ->  6"));
  }

  @Test
  void gestisceLaVittoriaAiPuntiPerTimeout() {
    Fighter alice = strongFighter("Alice");
    Fighter bob = weakFighter("Bob");
    List<Scorecard> scorecards = List.of(
        scorecard(alice.name(), 0.60, 0.52, 5, 3, true),
        scorecard(bob.name(), 0.52, 0.60, 4, 1, false));
    CombatResult result = new CombatResult(
        CombatOutcome.TIMEOUT_DECISION, alice, 20, List.of(), finalVitals(alice, bob), scorecards);

    logger.reportOutcome(result, alice, bob);

    String output = capturedOutput();
    assertTrue(output.contains("Timeout ai punti, vince: Alice (20 turni)"));
    assertTrue(output.contains("Vince Alice: pronostico rispettato."));
    assertTrue(output.contains("Decisione ai punti:"));
    assertTrue(output.contains("Alice: salute +2 (60% vs 52%), colpi a segno 5x2=10, parate 3x1=3, schivate 0x1=0  ->  15"));
    assertTrue(output.contains("Bob: salute +0 (52% vs 60%), colpi a segno 4x2=8, parate 1x1=1, schivate 0x1=0  ->  9"));
  }

  @Test
  void nonMostraLaDecisioneAiPuntiSuVittoriaPerKo() {
    Fighter alice = strongFighter("Alice");
    Fighter bob = weakFighter("Bob");
    CombatResult result = victoryResult(alice, bob, alice);

    logger.reportOutcome(result, alice, bob);

    String output = capturedOutput();
    assertFalse(output.contains("Decisione ai punti:"));
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

  private CombatResult victoryResult(Fighter first, Fighter second, Fighter winner) {
    return new CombatResult(CombatOutcome.VICTORY, winner, 3, List.of(), finalVitals(first, second), List.of());
  }

  private List<FighterVitals> finalVitals(Fighter first, Fighter second) {
    return List.of(
        new FighterVitals(first.name(), first.ratings().maxHealth(), first.ratings().maxHealth(),
            first.ratings().maxStamina(), first.ratings().maxStamina()),
        new FighterVitals(second.name(), second.ratings().maxHealth(), second.ratings().maxHealth(),
            second.ratings().maxStamina(), second.ratings().maxStamina()));
  }

  private InitiativeReport initiativeChosenBy(String name) {
    return new InitiativeReport(List.of(), name, name, InitiativeOverride.NONE);
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
