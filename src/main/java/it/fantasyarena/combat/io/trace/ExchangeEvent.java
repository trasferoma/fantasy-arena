package it.fantasyarena.combat.io.trace;

import it.fantasycombatsystem.result.ActionOutcome;
import it.fantasycombatsystem.telemetry.TurnTelemetry;

/**
 * Uno scambio giocato: una riga per scambio, mai per round, perché è il livello a cui il
 * bilanciamento si analizza davvero (danno a colpo, tassi di parata e schivata). Porta l'azione e
 * la telemetria analitica completa già calcolate dal motore, senza ricalcolare niente.
 *
 * <p>{@link #round()}, {@link #attackerIndex()} e {@link #targetIndex()} sono {@code null} nel
 * duello: il motore non produce round per il duello 1v1, e non fornisce indici di roster attendibili
 * per correlare un'azione al suo autore (vedi {@code TrialChronicle}, Javadoc di classe).
 *
 * @param trial numero della prova a cui appartiene lo scambio
 * @param turnNumber numero del turno, così come il motore lo ha prodotto
 */
public record ExchangeEvent(TraceEventKind event, int trial, Integer round, int turnNumber, Integer attackerIndex,
    Integer targetIndex, ActionOutcome action, TurnTelemetry telemetry) {

  public ExchangeEvent(int trial, Integer round, int turnNumber, Integer attackerIndex, Integer targetIndex,
      ActionOutcome action, TurnTelemetry telemetry) {
    this(TraceEventKind.EXCHANGE, trial, round, turnNumber, attackerIndex, targetIndex, action, telemetry);
  }
}
