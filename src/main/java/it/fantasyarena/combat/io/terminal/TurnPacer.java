package it.fantasyarena.combat.io.terminal;

/**
 * Strategia di ritmo tra i turni durante il replay del combattimento: consente una
 * pausa interattiva (attesa dell'utente) oppure un avanzamento immediato in batch.
 */
public interface TurnPacer {

  void awaitNextTurn();

  /**
   * Pacer che non attende mai: nessuna pausa e nessuna lettura da {@code System.in}. A differenza
   * di {@link EnterKeyTurnPacer#withoutHint()}, che tace il suggerimento ma continua comunque a
   * leggere l'INVIO, questo pacer non tocca lo standard input in nessun caso — è la scansione
   * corretta per la passata muta dell'arena, dove nessuno deve restare in attesa di una lettura che
   * non arriverà mai.
   */
  static TurnPacer none() {
    return () -> { };
  }
}
