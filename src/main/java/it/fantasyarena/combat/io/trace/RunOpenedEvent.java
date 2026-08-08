package it.fantasyarena.combat.io.trace;

import it.fantasyarena.combat.chronicle.HeroSnapshot;
import it.fantasycombatsystem.config.CombatSettings;

/**
 * L'apertura della corsa: l'istante in cui è cominciata, i {@link CombatSettings} completi con cui
 * è stata giocata, il protagonista con cui entra nell'arena e quante prove prevedeva il percorso.
 * Senza {@link #settings()} un numero qualunque del resto del log non è interpretabile: non si può
 * distinguere una taratura da un'altra.
 *
 * @param startedAt istante di apertura della corsa, in formato ISO-8601
 * @param plannedTrials lunghezza prevista del percorso, indipendentemente da quante prove la corsa
 *     arriverà davvero a giocare
 */
public record RunOpenedEvent(TraceEventKind event, String startedAt, CombatSettings settings,
    HeroSnapshot protagonist, int plannedTrials) {

  public RunOpenedEvent(String startedAt, CombatSettings settings, HeroSnapshot protagonist, int plannedTrials) {
    this(TraceEventKind.RUN_OPENED, startedAt, settings, protagonist, plannedTrials);
  }
}
