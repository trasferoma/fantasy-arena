package it.fantasyarena.combat.engine;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.config.CombatSettings.MomentumWeights;

/**
 * Regole pure del Momentum: clamp del range, moltiplicatore limitato su danno/difesa e
 * variazioni per evento di turno. Nessun effetto collaterale: lo shell applica i valori
 * calcolati allo {@code FighterState}.
 */
public final class MomentumRules {

  private final CombatSettings settings;

  public MomentumRules(CombatSettings settings) {
    this.settings = settings;
  }

  public int clamp(int momentum) {
    MomentumWeights weights = settings.momentumWeights();
    return Math.max(weights.min(), Math.min(weights.max(), momentum));
  }

  public double effectMultiplier(int momentum) {
    MomentumWeights weights = settings.momentumWeights();
    double normalized = (double) momentum / weights.max();
    double capped = Math.max(-1.0, Math.min(1.0, normalized));
    return 1.0 + capped * weights.effectCap();
  }

  public int deltaForHitLanded() {
    return settings.momentumWeights().gainOnHitLanded();
  }

  public int deltaForCriticalDealt() {
    return settings.momentumWeights().gainOnCriticalDealt();
  }

  public int deltaForParrySuccess() {
    return settings.momentumWeights().gainOnParrySuccess();
  }

  public int deltaForDodgeSuccess() {
    return settings.momentumWeights().gainOnDodgeSuccess();
  }

  public int deltaForHitTaken() {
    return settings.momentumWeights().lossOnHitTaken();
  }

  public int deltaForCriticalTaken() {
    return settings.momentumWeights().lossOnCriticalTaken();
  }

  public int deltaForMiss() {
    return settings.momentumWeights().lossOnMiss();
  }
}
