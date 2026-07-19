package it.fantasyarena.combat.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stato mutabile del combattente durante lo scontro: Health e Stamina correnti, Momentum
 * e status effect attivi (lista vuota in v1). Separato dai Rating intrinseci, che restano
 * sempre immutabili. Nessuna formula qui: i floor a 0/max sono semplici invarianti del
 * contenitore, non regole di bilanciamento (che vivono in {@code MomentumRules}/{@code StaminaRules}).
 */
public final class FighterState {

  private final int maxStamina;
  private final List<String> statusEffects;

  private int currentHealth;
  private int currentStamina;
  private int momentum;

  public FighterState(int maxHealth, int maxStamina) {
    this.maxStamina = maxStamina;
    this.currentHealth = maxHealth;
    this.currentStamina = maxStamina;
    this.momentum = 0;
    this.statusEffects = new ArrayList<>();
  }

  public int currentHealth() {
    return currentHealth;
  }

  public int currentStamina() {
    return currentStamina;
  }

  public int momentum() {
    return momentum;
  }

  public List<String> statusEffects() {
    return Collections.unmodifiableList(statusEffects);
  }

  public boolean isDefeated() {
    return currentHealth <= 0;
  }

  public void applyDamage(int amount) {
    currentHealth = Math.max(0, currentHealth - amount);
  }

  public void consumeStamina(int amount) {
    currentStamina = Math.max(0, Math.min(maxStamina, currentStamina - amount));
  }

  public void recoverStamina(int amount) {
    // Il cap al massimo e' un invariante del contenitore, non una regola di bilanciamento.
    currentStamina = Math.min(maxStamina, currentStamina + amount);
  }

  public void setMomentum(int momentum) {
    this.momentum = momentum;
  }
}
