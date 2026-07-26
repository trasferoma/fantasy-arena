package it.fantasyarena.combat.battle;

import java.util.List;
import java.util.Optional;

import it.fantasyarena.combat.model.Fighter;

/**
 * Decide in quale scontro attivo inserire un combattente rimasto libero (vincitore di uno
 * scontro concluso). Puro: si limita a scegliere, non muta nulla.
 */
public interface FreeWinnerAssigner {

  Optional<Engagement> assign(Fighter freeWinner, List<Engagement> activeEngagements, BattleRoster roster);
}
