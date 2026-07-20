package it.fantasyarena.combat.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.result.FighterVitals;
import it.fantasyarena.combat.result.InitiativeBreakdown;
import it.fantasyarena.combat.result.InitiativeOverride;
import it.fantasyarena.combat.result.InitiativeReport;
import it.fantasyarena.combat.result.StaminaChange;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * Verifica che {@link TurnLogFormatter} produca esattamente le righe (ordine e prefissi di
 * spazi inclusi) che {@code ConsoleCombatLogger.logTurn} stampava prima dell'estrazione.
 */
class TurnLogFormatterTest {

  private final TurnLogFormatter formatter = new TurnLogFormatter();

  @Test
  void formatDiUnaVoceMinimaProduceSoloIntestazioneEDescrizione() {
    TurnLogEntry entry = new TurnLogEntry(1, "Nessuno si muove");

    List<String> lines = formatter.format(entry);

    assertEquals(List.of("Turno 1:", "         Nessuno si muove"), lines);
  }

  @Test
  void formatDiUnaVoceCompletaProduceStatoIniziativaDescrizioneEStamina() {
    FighterVitals aliceVitals = new FighterVitals("Alice", 40, 50, 30, 40);
    FighterVitals bobVitals = new FighterVitals("Bob", 45, 50, 25, 40);

    InitiativeBreakdown aliceBreakdown =
        new InitiativeBreakdown("Alice", 10.0, 5.0, 2.5, 1.0, 18.5, 30, 40, 10, 5, 3);
    InitiativeBreakdown bobBreakdown =
        new InitiativeBreakdown("Bob", 8.0, 4.0, 2.0, 3.0, 17.0, 25, 40, 8, 4, 6);
    InitiativeReport initiative = new InitiativeReport(
        List.of(aliceBreakdown, bobBreakdown), "Alice", "Alice", InitiativeOverride.NONE);

    StaminaChange aliceChange = new StaminaChange("Alice", 6, 0);
    StaminaChange bobChange = new StaminaChange("Bob", 0, 4);

    TurnLogEntry entry = new TurnLogEntry(3, "Alice attacca Bob e lo colpisce")
        .withVitals(List.of(aliceVitals, bobVitals))
        .withInitiative(initiative)
        .withStaminaChanges(List.of(aliceChange, bobChange));

    List<String> lines = formatter.format(entry);

    List<String> expected = List.of(
        "Turno 3: Stato -> Alice: vita 40/50, stamina 30/40 | Bob: vita 45/50, stamina 25/40",
        "         Iniziativa a inizio turno (formula punteggio):",
        "           Alice: stamina 10,0 [30/40] + agilità 5,0 [10] + int 2,5 [5] + jitter 1,0 [dado 3] = 18,5",
        "           Bob: stamina 8,0 [25/40] + agilità 4,0 [8] + int 2,0 [4] + jitter 3,0 [dado 6] = 17,0",
        "         -> vince l'iniziativa (punteggio): Alice (18,5 vs 17,0)",
        "         -> primo ad agire: Alice",
        "         Alice attacca Bob e lo colpisce",
        "         Stamina -> Alice: -6 consumata, +0 recuperata | Bob: -0 consumata, +4 recuperata");

    assertEquals(expected, lines);
  }

  @Test
  void formatCompactDiUnaVoceMinimaProduceSoloLaDescrizione() {
    TurnLogEntry entry = new TurnLogEntry(1, "Nessuno si muove");

    List<String> lines = formatter.formatCompact(entry);

    assertEquals(List.of("Nessuno si muove"), lines);
  }

  @Test
  void formatCompactDiUnaVoceConIniziativaProduceEsitoIniziativaEDescrizione() {
    InitiativeBreakdown aliceBreakdown =
        new InitiativeBreakdown("Alice", 10.0, 5.0, 2.5, 1.0, 18.5, 30, 40, 10, 5, 3);
    InitiativeBreakdown bobBreakdown =
        new InitiativeBreakdown("Bob", 8.0, 4.0, 2.0, 3.0, 17.0, 25, 40, 8, 4, 6);
    InitiativeReport initiative = new InitiativeReport(
        List.of(aliceBreakdown, bobBreakdown), "Alice", "Alice", InitiativeOverride.NONE);

    TurnLogEntry entry = new TurnLogEntry(3, "Alice attacca Bob e lo colpisce")
        .withInitiative(initiative);

    List<String> lines = formatter.formatCompact(entry);

    List<String> expected = List.of(
        "-> vince l'iniziativa (punteggio): Alice (18,5 vs 17,0)",
        "-> primo ad agire: Alice",
        "Alice attacca Bob e lo colpisce");

    assertEquals(expected, lines);
    lines.forEach(line -> {
      assertFalse(line.startsWith("Turno"));
      assertFalse(line.contains("agilità"));
      assertFalse(line.contains("Stamina ->"));
    });
  }
}
