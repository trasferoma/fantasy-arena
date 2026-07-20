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

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.CombatOutcome;
import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.result.FighterVitals;
import it.fantasyarena.combat.result.InitiativeOverride;
import it.fantasyarena.combat.result.InitiativeReport;
import it.fantasyarena.combat.result.TurnHighlight;
import it.fantasyarena.combat.result.TurnLogEntry;
import it.fantasyarena.combat.testsupport.CombatFixtures;

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
  void citaIlFavoritoEIlPronosticoRispettatoQuandoVinceIlFavorito() {
    Fighter alice = strongFighter("Alice");
    Fighter bob = weakFighter("Bob");
    CombatResult result = victoryResult(alice, bob, Optional.of(alice));

    logger.reportOutcome(result, alice, bob);

    String output = capturedOutput();
    assertTrue(output.contains("Favorito alla vigilia: Alice."));
    assertTrue(output.contains("Vince Alice: pronostico rispettato."));
  }

  @Test
  void citaIlRibaltoneQuandoVinceIlNonFavorito() {
    Fighter alice = strongFighter("Alice");
    Fighter bob = weakFighter("Bob");
    CombatResult result = victoryResult(alice, bob, Optional.of(bob));

    logger.reportOutcome(result, alice, bob);

    String output = capturedOutput();
    assertTrue(output.contains("Favorito alla vigilia: Alice."));
    assertTrue(output.contains("Vince Bob: ribaltone rispetto al pronostico!"));
  }

  @Test
  void dichiaraEquilibrioSenzaFavoritoNetto() {
    Fighter alice = strongFighter("Alice");
    Fighter twin = strongFighter("Twin");
    CombatResult result = victoryResult(alice, twin, Optional.of(alice));

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
        CombatOutcome.VICTORY, Optional.of(alice), 5, List.of(highlightedTurn), finalVitals(alice, bob));

    logger.reportOutcome(result, alice, bob);

    String output = capturedOutput();
    assertTrue(output.contains("Da ricordare: il colpo critico di Alice al turno 5."));
  }

  @Test
  void gestisceIlPareggioSenzaConfermareOSmentireIlPronostico() {
    Fighter alice = strongFighter("Alice");
    Fighter bob = weakFighter("Bob");
    CombatResult result = new CombatResult(
        CombatOutcome.DRAW, Optional.empty(), 10, List.of(), finalVitals(alice, bob));

    logger.reportOutcome(result, alice, bob);

    String output = capturedOutput();
    assertTrue(output.contains("Pareggio dopo 10 turni."));
    assertTrue(output.contains("Pareggio: pronostico né confermato né smentito."));
  }

  @Test
  void gestisceLaVittoriaAiPuntiPerTimeout() {
    Fighter alice = strongFighter("Alice");
    Fighter bob = weakFighter("Bob");
    CombatResult result = new CombatResult(
        CombatOutcome.TIMEOUT_DECISION, Optional.of(alice), 20, List.of(), finalVitals(alice, bob));

    logger.reportOutcome(result, alice, bob);

    String output = capturedOutput();
    assertTrue(output.contains("Timeout ai punti, vince: Alice (20 turni)"));
    assertTrue(output.contains("Vince Alice: pronostico rispettato."));
  }

  private CombatResult victoryResult(Fighter first, Fighter second, Optional<Fighter> winner) {
    return new CombatResult(CombatOutcome.VICTORY, winner, 3, List.of(), finalVitals(first, second));
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
