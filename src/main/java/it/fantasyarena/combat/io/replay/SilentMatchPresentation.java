package it.fantasyarena.combat.io.replay;

import it.fantasycombatsystem.battle.BattleResult;
import it.fantasycombatsystem.battle.BattleSetup;
import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.result.CombatResult;

/**
 * {@link MatchPresentation} muta: non mostra niente e non attende niente. Serve alla passata
 * silenziosa dell'arena, che gioca la partita senza scrivere su console né leggere da standard
 * input.
 */
public class SilentMatchPresentation implements MatchPresentation {

  @Override
  public void presentDuel(Fighter first, Fighter second, CombatResult result) {
  }

  @Override
  public void presentBattle(BattleSetup setup, BattleResult result) {
  }
}
