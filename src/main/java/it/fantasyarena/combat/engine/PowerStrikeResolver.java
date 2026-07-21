package it.fantasyarena.combat.engine;

import it.fantasyarena.combat.config.CombatFormulas;
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
    double staminaRatio = CombatFormulas.ratio(attacker.state().currentStamina(), attacker.ratings().maxStamina());
    double healthRatio = CombatFormulas.ratio(attacker.state().currentHealth(), attacker.ratings().maxHealth());
    double intelligence = Characteristics.valueOf(attacker.character(), Characteristic.INTELLIGENCE);

    return CombatFormulas.powerStrikeScore(weights, momentumWeights, staminaRatio, healthRatio,
        attacker.state().momentum(), intelligence, jitterThrow.normalized());
  }
}
