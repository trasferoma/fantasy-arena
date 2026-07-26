package it.fantasyarena.combat.io;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.result.InitiativeBreakdown;
import it.fantasyarena.combat.result.InitiativeOverride;
import it.fantasyarena.combat.result.InitiativeReport;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * Verifica la generalizzazione N-aria di {@code TurnLogFormatter.describeScoreWinner}: con 3
 * breakdown il confronto mostra il totale del vincitore (massimo assoluto) e il secondo massimo,
 * non il minimo assoluto; con 2 breakdown l'output resta identico carattere per carattere a
 * quello di {@link TurnLogFormatterTest} (che non viene toccato da questo task).
 */
class TurnLogFormatterNAryTest {

  private final TurnLogFormatter formatter = new TurnLogFormatter();

  @Test
  void conTrePartecipanti_ilValoreDiConfrontoEIlSecondoMassimo_nonIlMinimo() {
    InitiativeBreakdown weak = new InitiativeBreakdown("Weak", 2.0, 2.0, 2.0, 4.0, 10.0, 10, 40, 5, 5, 4);
    InitiativeBreakdown strong = new InitiativeBreakdown("Strong", 10.0, 10.0, 5.0, 5.0, 30.0, 40, 40, 20, 10, 5);
    InitiativeBreakdown medium = new InitiativeBreakdown("Medium", 6.0, 6.0, 4.0, 4.0, 20.0, 25, 40, 12, 8, 4);
    InitiativeReport initiative = new InitiativeReport(
        List.of(weak, strong, medium), "Strong", "Strong", InitiativeOverride.NONE);

    TurnLogEntry entry = new TurnLogEntry(1, "Strong attacca Weak")
        .withInitiative(initiative);

    List<String> lines = formatter.formatCompact(entry);

    assertEquals(List.of(
        "-> vince l'iniziativa (punteggio): Strong (30,0 vs 20,0)",
        "-> primo ad agire: Strong",
        "Strong attacca Weak"), lines);
  }

  @Test
  void conDueBreakdown_lOutputRestaIdenticoAlDuello1v1() {
    InitiativeBreakdown aliceBreakdown =
        new InitiativeBreakdown("Alice", 10.0, 5.0, 2.5, 1.0, 18.5, 30, 40, 10, 5, 3);
    InitiativeBreakdown bobBreakdown =
        new InitiativeBreakdown("Bob", 8.0, 4.0, 2.0, 3.0, 17.0, 25, 40, 8, 4, 6);
    InitiativeReport initiative = new InitiativeReport(
        List.of(aliceBreakdown, bobBreakdown), "Alice", "Alice", InitiativeOverride.NONE);

    TurnLogEntry entry = new TurnLogEntry(3, "Alice attacca Bob e lo colpisce")
        .withInitiative(initiative);

    List<String> lines = formatter.formatCompact(entry);

    assertEquals(List.of(
        "-> vince l'iniziativa (punteggio): Alice (18,5 vs 17,0)",
        "-> primo ad agire: Alice",
        "Alice attacca Bob e lo colpisce"), lines);
  }
}
