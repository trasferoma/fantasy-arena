package it.fantasyarena.combat.battle;

import java.util.List;

import it.fantasyarena.combat.model.Fighter;

/**
 * Bersaglio "appiccicoso": mantiene il bersaglio corrente finché è ancora vivo e presente fra i
 * nemici disponibili (per identità, mai per nome: due nemici possono chiamarsi allo stesso modo).
 * Quando va cambiato, sceglie il più debole per rapporto Salute corrente/massima; a parità, la
 * Stamina corrente minore; a ulteriore parità, l'ordine in cui compare in {@code livingEnemies}.
 */
public final class StickyTargetSelector implements TargetSelector {

  @Override
  public Fighter selectTarget(Fighter attacker, Fighter currentTarget, List<Fighter> livingEnemies) {
    validateLivingEnemies(livingEnemies);

    if (currentTarget != null && FighterIdentity.containsSame(livingEnemies, currentTarget)) {
      return currentTarget;
    }

    return weakest(livingEnemies);
  }

  private Fighter weakest(List<Fighter> livingEnemies) {
    Fighter weakest = livingEnemies.get(0);
    double weakestHealthRatio = healthRatio(weakest);

    for (int i = 1; i < livingEnemies.size(); i++) {
      Fighter candidate = livingEnemies.get(i);
      double candidateHealthRatio = healthRatio(candidate);

      if (isWeaker(candidate, candidateHealthRatio, weakest, weakestHealthRatio)) {
        weakest = candidate;
        weakestHealthRatio = candidateHealthRatio;
      }
    }

    return weakest;
  }

  private boolean isWeaker(Fighter candidate, double candidateHealthRatio, Fighter currentWeakest,
      double currentWeakestHealthRatio) {
    if (candidateHealthRatio != currentWeakestHealthRatio) {
      return candidateHealthRatio < currentWeakestHealthRatio;
    }
    return candidate.state().currentStamina() < currentWeakest.state().currentStamina();
  }

  private double healthRatio(Fighter fighter) {
    return (double) fighter.state().currentHealth() / fighter.ratings().maxHealth();
  }

  private void validateLivingEnemies(List<Fighter> livingEnemies) {
    if (livingEnemies == null || livingEnemies.isEmpty()) {
      throw new IllegalArgumentException("livingEnemies must not be null or empty, was: " + livingEnemies);
    }
  }
}
