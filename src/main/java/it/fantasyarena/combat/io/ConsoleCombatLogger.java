package it.fantasyarena.combat.io;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * Logger che stampa il duello su console: log turno per turno seguito dall'esito finale.
 */
public class ConsoleCombatLogger implements CombatLogger {

  @Override
  public void logTurn(TurnLogEntry entry) {
    System.out.println("Turno " + entry.turnNumber() + ": " + entry.description());
  }

  @Override
  public void reportOutcome(CombatResult result) {
    System.out.println();
    System.out.println("=== Esito del duello ===");

    switch (result.outcome()) {
      case VICTORY -> printWinner("Vince", result);
      case TIMEOUT_DECISION -> printWinner("Timeout ai punti, vince", result);
      case DRAW -> System.out.println("Pareggio dopo " + result.rounds() + " turni.");
    }
  }

  private void printWinner(String label, CombatResult result) {
    String winnerName = result.winner()
        .map(Fighter::name)
        .orElseThrow(() -> new IllegalStateException("Esito con vincitore atteso ma assente"));
    System.out.println(label + ": " + winnerName + " (" + result.rounds() + " turni)");
  }
}
