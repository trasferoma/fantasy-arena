package it.fantasyarena.combat.io;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * {@link CombatReplay} lineare: stampa i turni in sequenza su console, uno dopo l'altro,
 * scandendo l'attesa tra un turno e il successivo con il {@link TurnPacer}. È il comportamento
 * storico del progetto, mantenuto invariato come alternativa al replay a pagina: i due
 * combattenti non servono qui (il log lineare turno per turno è già autosufficiente).
 */
public class LinearCombatReplay implements CombatReplay {

  private final CombatLogger logger;
  private final TurnPacer turnPacer;

  public LinearCombatReplay(CombatLogger logger, TurnPacer turnPacer) {
    this.logger = logger;
    this.turnPacer = turnPacer;
  }

  @Override
  public void replay(CombatResult outcome, Fighter first, Fighter second) {
    for (TurnLogEntry entry : outcome.log()) {
      logger.logTurn(entry);
      turnPacer.awaitNextTurn();
    }
  }
}
