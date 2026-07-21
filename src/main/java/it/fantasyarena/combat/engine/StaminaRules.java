package it.fantasyarena.combat.engine;

import it.fantasyarena.combat.config.CombatFormulas;
import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.model.IntrinsicRatings;

/**
 * Regole pure della Stamina: costo delle azioni e penalità progressive di affaticamento,
 * calcolate sul rapporto tra Stamina corrente e massima.
 */
public final class StaminaRules {

  private final CombatSettings settings;

  public StaminaRules(CombatSettings settings) {
    this.settings = settings;
  }

  public int attackCost() {
    return settings.staminaWeights().attackCost();
  }

  public int parryCost() {
    return settings.staminaWeights().parryCost();
  }

  public int dodgeCost() {
    return settings.staminaWeights().dodgeCost();
  }

  public int impactCost() {
    return settings.staminaWeights().impactCost();
  }

  public double impactStaminaDamageFactor() {
    return settings.staminaWeights().impactStaminaDamageFactor();
  }

  /**
   * Stamina persa da chi incassa un colpo pieno, proporzionale al danno subito con un minimo
   * garantito pari a {@link #impactCost()}.
   */
  public int impactStaminaLoss(int damage) {
    return CombatFormulas.impactStaminaLoss(settings.staminaWeights(), damage);
  }

  public int restRecovery() {
    return settings.staminaWeights().restRecovery();
  }

  public double restThresholdRatio() {
    return settings.staminaWeights().restThresholdRatio();
  }

  public int chainMalusStep() {
    return settings.staminaWeights().chainMalusStep();
  }

  public int chainMalusCap() {
    return settings.staminaWeights().chainMalusCap();
  }

  public int passiveRecovery() {
    return settings.staminaWeights().passiveRecovery();
  }

  /**
   * Costo effettivo dell'attacco, cresciuto dal malus di catena: il primo turno d'attacco dopo
   * aver conquistato l'iniziativa costa il base {@code attackCost}, ogni turno consecutivo
   * successivo aggiunge uno step, fino al cap.
   */
  public int effectiveAttackCost(int consecutiveInitiativeWins) {
    return CombatFormulas.effectiveAttackCost(settings.staminaWeights(), consecutiveInitiativeWins);
  }

  public boolean canAttack(int currentStamina) {
    return currentStamina > 0;
  }

  public boolean canDefend(int currentStamina) {
    return currentStamina > 0;
  }

  /**
   * Policy a soglia con riserva difensiva: conviene riposare non solo a Stamina esaurita, ma
   * gia' quando scende sotto {@code restThresholdRatio} del pool massimo di Stamina, per
   * evitare di restare senza risorse per difendersi al turno successivo. Espressa come
   * percentuale (anziche' soglia assoluta) la regola scala correttamente sia su pool piccoli
   * sia su pool grandi.
   */
  public boolean shouldRest(int currentStamina, int maxStamina) {
    return CombatFormulas.shouldRest(settings.staminaWeights(), currentStamina, maxStamina);
  }

  /**
   * Moltiplicatore di affaticamento applicato ad attacco e difesa: nessuna penalità sopra
   * la soglia alta, -15% nella fascia intermedia, -30% sotto la soglia bassa.
   */
  public double fatigueMultiplier(int currentStamina, IntrinsicRatings ratings) {
    return CombatFormulas.fatigueMultiplier(settings.staminaWeights(), currentStamina, ratings.maxStamina());
  }
}
