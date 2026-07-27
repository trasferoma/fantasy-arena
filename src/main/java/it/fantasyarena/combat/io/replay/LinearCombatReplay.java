package it.fantasyarena.combat.io.replay;

import it.fantasyarena.combat.io.log.CombatLogger;
import it.fantasyarena.combat.io.terminal.ScreenCleaner;
import it.fantasyarena.combat.io.terminal.ScreenRefresh;
import it.fantasyarena.combat.io.terminal.TurnPacer;
import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.result.CombatResult;
import it.fantasycombatsystem.result.TurnLogEntry;

/**
 * {@link CombatReplay} lineare: stampa i turni in sequenza su console, uno dopo l'altro,
 * scandendo l'attesa tra un turno e il successivo con il {@link TurnPacer}. Pulisce lo schermo
 * prima di ogni turno tramite {@link ScreenCleaner}: in {@link ScreenRefresh#SCROLL} (comportamento
 * storico del progetto) è un no-op e i turni restano accodati uno sotto l'altro; i due
 * combattenti non servono qui (il log lineare turno per turno è già autosufficiente).
 */
public class LinearCombatReplay implements CombatReplay {

  private final CombatLogger logger;
  private final TurnPacer turnPacer;
  private final ScreenCleaner screenCleaner;

  public LinearCombatReplay(CombatLogger logger, TurnPacer turnPacer, ScreenCleaner screenCleaner) {
    this.logger = logger;
    this.turnPacer = turnPacer;
    this.screenCleaner = screenCleaner;
  }

  @Override
  public void replay(CombatResult outcome, Fighter first, Fighter second) {
    for (TurnLogEntry entry : outcome.log()) {
      screenCleaner.clear();
      logger.logTurn(entry);
      turnPacer.awaitNextTurn();
    }
  }
}
