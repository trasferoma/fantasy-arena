package it.fantasyarena.combat.io;

import java.util.List;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * {@link CombatReplay} a pagina: ad ogni turno rivelato pulisce lo schermo (tramite
 * {@link ScreenCleaner}, no-op in {@link ScreenRefresh#SCROLL}) e ridisegna l'intera pagina
 * prodotta da {@link CombatScreenRenderer}, a tre colonne: pannelli delle barre di vita/stamina,
 * eventi del turno corrente e schede dei due combattenti.
 */
public class ScreenCombatReplay implements CombatReplay {

  private final TurnPacer turnPacer;
  private final int maxTurns;
  private final ScreenCleaner screenCleaner;

  /**
   * @param maxTurns tetto di turni del duello, mostrato come denominatore del contatore di turno
   */
  public ScreenCombatReplay(TurnPacer turnPacer, int maxTurns, ScreenCleaner screenCleaner) {
    this.turnPacer = turnPacer;
    this.maxTurns = maxTurns;
    this.screenCleaner = screenCleaner;
  }

  @Override
  public void replay(CombatResult outcome, Fighter first, Fighter second) {
    List<TurnLogEntry> log = outcome.log();
    if (log.isEmpty()) {
      return;
    }

    CombatScreenRenderer renderer =
        new CombatScreenRenderer(first, second, log, outcome.finalVitals(), maxTurns);
    for (int turnPosition = 0; turnPosition < log.size(); turnPosition++) {
      screenCleaner.clear();
      System.out.print(renderer.render(turnPosition));
      turnPacer.awaitNextTurn();
    }
  }
}
