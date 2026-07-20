package it.fantasyarena.combat.io;

import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * {@link CombatReplay} lineare: stampa i turni in sequenza su console, uno dopo l'altro,
 * scandendo l'attesa tra un turno e il successivo con il {@link TurnPacer}. È il comportamento
 * storico del progetto, mantenuto invariato come alternativa al replay a pagina.
 */
public class LinearCombatReplay implements CombatReplay {

  private final CombatLogger logger;
  private final TurnPacer turnPacer;

  public LinearCombatReplay(CombatLogger logger, TurnPacer turnPacer) {
    this.logger = logger;
    this.turnPacer = turnPacer;
  }

  @Override
  public void replay(CombatResult outcome) {
    for (TurnLogEntry entry : outcome.log()) {
      logger.logTurn(entry);
      turnPacer.awaitNextTurn();
    }
  }
}
