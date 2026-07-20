package it.fantasyarena.combat.io;

import java.util.List;

import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * {@link CombatReplay} a pagina: ad ogni turno rivelato pulisce lo schermo (sequenza ANSI) e
 * ridisegna l'intera pagina prodotta da {@link CombatScreenRenderer}, con i pannelli delle
 * barre di vita/stamina a sinistra e la coda del log cumulativo a destra.
 */
public class ScreenCombatReplay implements CombatReplay {

  private final TurnPacer turnPacer;

  public ScreenCombatReplay(TurnPacer turnPacer) {
    this.turnPacer = turnPacer;
  }

  @Override
  public void replay(CombatResult outcome) {
    List<TurnLogEntry> log = outcome.log();
    if (log.isEmpty()) {
      return;
    }

    CombatScreenRenderer renderer = new CombatScreenRenderer(log, outcome.finalVitals());
    for (int turnPosition = 0; turnPosition < log.size(); turnPosition++) {
      clearScreen();
      System.out.print(renderer.render(turnPosition));
      turnPacer.awaitNextTurn();
    }
  }

  private void clearScreen() {
    System.out.print("\033[H\033[2J");
    System.out.flush();
  }
}
