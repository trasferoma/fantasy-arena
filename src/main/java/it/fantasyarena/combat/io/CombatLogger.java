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

  /**
   * Stampa l'esito finale del duello, con le schede dei due combattenti e una narrazione
   * che ne spiega il "motivo" (favorito pre-scontro, ribaltone, eventi salienti).
   */
  void reportOutcome(CombatResult result, Fighter first, Fighter second);
}
