package it.fantasyarena.combat.io.log;

import it.fantasycombatsystem.battle.BattleResult;
import it.fantasycombatsystem.battle.BattleSetup;
import it.fantasycombatsystem.battle.RoundLogEntry;

/**
 * Astrazione della voce della battaglia NvN: annuncia gli schieramenti, rivela un round alla
 * volta e ne racconta l'esito finale.
 *
 * <p>Nasce ora perché sta arrivando una seconda presentazione — quella web — che deve poter
 * ricevere questi stessi annunci senza stamparli: non è una previsione, è la superficie già
 * esposta da {@link ConsoleBattleLogger}, resa sostituibile.
 */
public interface BattleLogger {

  /**
   * Annuncia gli schieramenti: intestazione, poi per ogni squadra il nome e la scheda di ogni
   * membro.
   */
  void reportSetup(BattleSetup setup);

  /**
   * Rivela un round della battaglia.
   */
  void logRound(RoundLogEntry round);

  /**
   * Racconta l'esito finale della battaglia: intestazione, poi l'esito (con squadra vincitrice
   * quando prevista), l'eventuale dettaglio a punti e lo stato finale di ogni combattente.
   */
  void reportOutcome(BattleResult result);
}
