package it.fantasyarena.combat.battle;

import java.util.List;

/**
 * Decide gli scontri di apertura di una battaglia, a partire dal roster completo. Pura: nessun
 * dado, nessuna mutazione dei combattenti o delle squadre.
 */
public interface EngagementPlanner {

  List<Engagement> openingEngagements(BattleRoster roster);
}
