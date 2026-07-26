package it.fantasyarena.combat.battle;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * Esito interno di uno scambio: puo' referenziare {@link Fighter}, a differenza dei record di
 * log rivolti alla presentazione.
 */
public record PlayedExchange(Engagement engagement, Fighter actor, Fighter target, TurnLogEntry entry) {
}
