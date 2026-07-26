package it.fantasyarena.combat.battle;

import java.util.List;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * Esito interno di uno scambio: puo' referenziare {@link Fighter}, a differenza dei record di
 * log rivolti alla presentazione. {@code participantsAtRoundStart} e' lo snapshot dei
 * partecipanti vivi dello scontro catturato prima di giocare lo scambio (equivalente a "vivi a
 * inizio round" per questo scontro, essendo gli scontri disgiunti fra loro).
 */
public record PlayedExchange(Engagement engagement, Fighter actor, Fighter target, TurnLogEntry entry,
    List<Fighter> participantsAtRoundStart) {

  public PlayedExchange {
    participantsAtRoundStart = List.copyOf(participantsAtRoundStart);
  }
}
