package it.fantasyarena.combat.io.trace;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import it.fantasyarena.combat.chronicle.ArenaChronicle;
import it.fantasyarena.combat.chronicle.RunConclusion;
import it.fantasyarena.combat.chronicle.TrialChronicle;
import it.fantasyarena.json.JsonSupport;
import it.fantasycombatsystem.battle.EngagementTurn;
import it.fantasycombatsystem.battle.RoundLogEntry;
import it.fantasycombatsystem.result.TurnLogEntry;

/**
 * Traduce una {@link ArenaChronicle} nelle righe JSON Lines del log analitico: una funzione pura,
 * senza filesystem e senza {@code System.out}, testabile passando una cronaca costruita a mano.
 * Non sceglie il file su cui finiranno le righe e non le scrive: sono le altre due responsabilità
 * dichiarate della SPEC, in unità distinte che vivranno accanto a questa.
 *
 * <p>Produce cinque tipi di evento, ciascuno riconoscibile dal campo discriminante di
 * {@link TraceEventKind}: l'apertura della corsa, l'inizio di ogni prova, uno scambio per ogni
 * turno giocato — <strong>mai</strong> uno per round — la fine di ogni prova e la chiusura della
 * corsa. Ogni evento porta solo dati che la cronaca o il motore hanno già calcolato: nessuna
 * formula, nessun ricalcolo.
 *
 * <p>{@link Clock} è iniettato invece di leggere {@link Clock#systemUTC()} internamente, sullo
 * stesso principio del {@code DiceRoller} del motore: isola l'unica sorgente di non-determinismo di
 * questa classe (l'istante di apertura della corsa) dietro un collaboratore sostituibile, così i
 * test possono fissarlo invece di subirlo.
 */
public class ChronicleTraceComposer {

  private final Clock clock;

  public ChronicleTraceComposer() {
    this(Clock.systemUTC());
  }

  public ChronicleTraceComposer(Clock clock) {
    this.clock = clock;
  }

  public List<String> compose(ArenaChronicle chronicle) {
    List<String> lines = new ArrayList<>();
    lines.add(toLine(runOpenedEvent(chronicle)));
    for (TrialChronicle trial : chronicle.trials()) {
      appendTrialLines(lines, trial);
    }
    lines.add(toLine(runClosedEvent(chronicle.conclusion())));
    return List.copyOf(lines);
  }

  private void appendTrialLines(List<String> lines, TrialChronicle trial) {
    lines.add(toLine(trialStartedEvent(trial)));
    for (ExchangeEvent exchange : exchangesOf(trial)) {
      lines.add(toLine(exchange));
    }
    lines.add(toLine(trialEndedEvent(trial)));
  }

  private RunOpenedEvent runOpenedEvent(ArenaChronicle chronicle) {
    return new RunOpenedEvent(clock.instant().toString(), chronicle.settings(), chronicle.protagonist(),
        chronicle.plannedTrials());
  }

  private TrialStartedEvent trialStartedEvent(TrialChronicle trial) {
    return new TrialStartedEvent(trial.number(), trial.description(), trial.shape(), trial.roster(), trial.budget());
  }

  /**
   * Gli scambi di una prova, nella forma che la sua {@link TrialChronicle#shape()} dichiara.
   * Esaustivo e senza {@code default}, come gli altri switch su {@code TrialShape} del progetto:
   * una terza forma deve fermare la compilazione, non sparire in un ramo dimenticato.
   */
  private List<ExchangeEvent> exchangesOf(TrialChronicle trial) {
    return switch (trial.shape()) {
      case BATTLE -> battleExchanges(trial);
      case DUEL -> duelExchanges(trial);
    };
  }

  /**
   * Uno scambio per ogni {@link EngagementTurn} di ogni round, non uno per round: un round di
   * battaglia con più scontri attivi gioca più scambi, e ciascuno merita la propria riga.
   */
  private List<ExchangeEvent> battleExchanges(TrialChronicle trial) {
    List<ExchangeEvent> exchanges = new ArrayList<>();
    for (RoundLogEntry round : trial.rounds()) {
      for (EngagementTurn engagementTurn : round.turns()) {
        exchanges.add(battleExchangeEvent(trial.number(), round.roundNumber(), engagementTurn));
      }
    }
    return exchanges;
  }

  private ExchangeEvent battleExchangeEvent(int trialNumber, int roundNumber, EngagementTurn engagementTurn) {
    TurnLogEntry turn = engagementTurn.turn();
    return new ExchangeEvent(trialNumber, roundNumber, turn.turnNumber(), engagementTurn.attackerIndex(),
        engagementTurn.targetIndex(), turn.action(), turn.telemetry());
  }

  private List<ExchangeEvent> duelExchanges(TrialChronicle trial) {
    return trial.turns().stream()
        .map(turn -> duelExchangeEvent(trial.number(), turn))
        .toList();
  }

  private ExchangeEvent duelExchangeEvent(int trialNumber, TurnLogEntry turn) {
    return new ExchangeEvent(trialNumber, null, turn.turnNumber(), null, null, turn.action(), turn.telemetry());
  }

  private TrialEndedEvent trialEndedEvent(TrialChronicle trial) {
    return new TrialEndedEvent(trial.number(), trial.outcome(), trial.finalVitals(), trial.progress());
  }

  private RunClosedEvent runClosedEvent(RunConclusion conclusion) {
    return new RunClosedEvent(conclusion.outcome(), conclusion.lastTrial());
  }

  private String toLine(Object event) {
    return JsonSupport.toJson(event);
  }
}
