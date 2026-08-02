package it.fantasyarena.combat.io.log;

import java.util.List;

import it.fantasyarena.combat.ChallengerBudget;
import it.fantasyarena.combat.RoundOutcome;
import it.fantasyarena.combat.hero.Hero;
import it.fantasyarena.combat.hero.HeroProgress;
import it.fantasycombatsystem.model.Fighter;

/**
 * {@link ArenaLogger} muto: non stampa niente. Serve a chi deve giocare l'arena senza mostrarla su
 * console.
 */
public class SilentArenaLogger implements ArenaLogger {

  @Override
  public void reportEntrance(Hero hero, int totalTrials) {
  }

  @Override
  public void announceRound(int number, String description, Hero hero, List<Fighter> challengers,
      ChallengerBudget budget) {
  }

  @Override
  public void reportProgress(HeroProgress progress) {
  }

  @Override
  public void reportEndOfRun(Hero hero, RoundOutcome outcome, int round) {
  }

  @Override
  public void reportTrialCrossed(Hero hero, int round) {
  }

  @Override
  public void reportTriumph(Hero hero, int totalTrials) {
  }
}
