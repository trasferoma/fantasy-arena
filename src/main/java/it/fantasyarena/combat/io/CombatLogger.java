package it.fantasyarena.combat.io;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * Astrazione dell'output del combattimento: consente di sostituire la stampa su console
 * con modalità silenziose o batch (v1.5) senza toccare il motore.
 */
public interface CombatLogger {

  /**
   * Stampa il riepilogo pre-combattimento dei due contendenti.
   */
  void reportMatchup(Fighter first, Fighter second);

  void logTurn(TurnLogEntry entry);

  void reportOutcome(CombatResult result);
}
