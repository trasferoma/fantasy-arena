package it.fantasyarena.combat.io.replay;

import it.fantasycombatsystem.battle.BattleResult;
import it.fantasycombatsystem.battle.BattleSetup;
import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.result.CombatResult;

/**
 * Rappresenta il <em>come</em> e il <em>quando</em> mostrare uno scontro già giocato dal motore,
 * come collaboratore sostituibile di {@code MatchRunner}: chi lo riceve sa solo che lo scontro
 * viene presentato, non se ciò accade su console, in una pagina web o non accade affatto.
 *
 * <p>Nasce ora perché sta arrivando una seconda presentazione — quella web — che deve poter
 * ricevere lo stesso esito del motore senza stampare o attendere niente: non è una previsione, è
 * la superficie che {@code MatchRunner} già usava internamente, resa sostituibile.
 */
public interface MatchPresentation {

  void presentDuel(Fighter first, Fighter second, CombatResult result);

  void presentBattle(BattleSetup setup, BattleResult result);
}
