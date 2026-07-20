package it.fantasyarena.combat.engine;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.config.CombatSettings.MomentumWeights;
import it.fantasyarena.combat.config.CombatSettings.PowerStrikeWeights;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Characteristics;
import it.fantasyarena.combat.model.Fighter;
import it.fantasytoolkitcore.core.model.Characteristic;

/**
 * Decisione pura del colpo potente: stamina e vita residue alte spingono verso la scelta (parte
 * razionale); il momentum positivo genera overconfidence, attenuata da un'Intelligenza alta; un
 * micro-jitter (iniettato dallo shell, mai tirato qui) rompe i casi borderline. Nessun tiro
 * interno e nessuna verifica di affordabilità: entrambe restano responsabilità dello shell
 * ({@link TurnOrchestrator}).
 */
public final class PowerStrikeResolver {

  private final PowerStrikeWeights weights;
  private final MomentumWeights momentumWeights;

  public PowerStrikeResolver(CombatSettings settings) {
    this.weights = settings.powerStrikeWeights();
    this.momentumWeights = settings.momentumWeights();
  }

  /**
   * Vero sse lo score della decisione raggiunge o supera {@link PowerStrikeWeights#decisionThreshold()}.
   */
  public boolean decide(Fighter attacker, DiceThrow jitterThrow) {
    return score(attacker, jitterThrow) >= weights.decisionThreshold();
  }

  /**
   * Score della decisione: parte razionale (stamina/vita residue) più overconfidence da
   * momentum positivo (attenuata dall'Intelligenza) più un micro-jitter. Esposto package-visible
   * per gli assert deterministici dei casi borderline nei test.
   */
  double score(Fighter attacker, DiceThrow jitterThrow) {
    double staminaRatio = ratio(attacker.state().currentStamina(), attacker.ratings().maxStamina());
    double healthRatio = ratio(attacker.state().currentHealth(), attacker.ratings().maxHealth());
    double momentumNorm = clamp01((double) attacker.state().momentum() / momentumWeights.max());
    double intelligence = Characteristics.valueOf(attacker.character(), Characteristic.INTELLIGENCE);
    double intelFactor = clamp01(intelligence / weights.intelligenceReference());

    double rational = weights.staminaWeight() * staminaRatio + weights.healthWeight() * healthRatio;
    double overconfidence = weights.overconfidenceWeight() * momentumNorm;
    double jitterNormalized = jitterThrow.normalized();

    return rational + (1.0 - intelFactor) * overconfidence + weights.jitterWeight() * jitterNormalized;
  }

  private double ratio(int current, int max) {
    return (double) current / max;
  }

  private double clamp01(double value) {
    return Math.max(0.0, Math.min(1.0, value));
  }
}
