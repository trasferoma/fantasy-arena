package it.fantasyarena.combat.chronicle;

import it.fantasyarena.combat.RoundOutcome;

/**
 * Come si è chiusa la corsa: com'è finita l'ultima prova giocata ({@link RoundOutcome}) e a quale
 * numero ({@link #lastTrial()}).
 */
public record RunConclusion(RoundOutcome outcome, int lastTrial) {

  /**
   * Vero sse la corsa si è chiusa con un trionfo. Non è un componente del record perché è
   * interamente derivabile da {@link #outcome()}: una prova vinta apre sempre la successiva, quindi
   * la corsa può chiudersi con {@code RoundOutcome.WON} solo dopo l'ultima prova. Custodirlo accanto
   * alla sua fonte creerebbe due campi che devono restare d'accordo per disciplina di chi costruisce
   * il record, invece che per costruzione.
   */
  public boolean triumph() {
    return outcome == RoundOutcome.WON;
  }
}
