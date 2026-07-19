package it.fantasyarena.combat.io;

/**
 * Strategia di ritmo tra i turni durante il replay del combattimento: consente una
 * pausa interattiva (attesa dell'utente) oppure un avanzamento immediato in batch.
 */
public interface TurnPacer {

  void awaitNextTurn();
}
