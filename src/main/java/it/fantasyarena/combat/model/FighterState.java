package it.fantasyarena.combat.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stato mutabile del combattente durante lo scontro: Health e Stamina correnti, Momentum,
 * catena di vittorie d'iniziativa consecutive e status effect attivi (lista vuota in v1).
 * Separato dai Rating intrinseci, che restano sempre immutabili. Nessuna formula qui: i floor
 * a 0/max sono semplici invarianti del contenitore, non regole di bilanciamento (che vivono in
 * {@code MomentumRules}/{@code StaminaRules}). Contratto di affordabilita': un'azione non deve
 * partire se {@link #canAfford(int)} e' falso; {@link #consumeStamina(int)} resta con un floor
 * a 0 come guardia difensiva interna, non come meccanismo di pagamento parziale a debito.
 */
public final class FighterState {

  private final int maxStamina;
  private final List<String> statusEffects;

  private int currentHealth;
  private int currentStamina;
  private int momentum;
  private int consecutiveInitiativeWins;

  public FighterState(int maxHealth, int maxStamina) {
    this.maxStamina = maxStamina;
    this.currentHealth = maxHealth;
    this.currentStamina = maxStamina;
    this.momentum = 0;
    this.consecutiveInitiativeWins = 0;
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

  public int consecutiveInitiativeWins() {
    return consecutiveInitiativeWins;
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

  /**
   * Vero sse la Stamina corrente basta a coprire {@code amount}: un'azione va verificata con
   * questo metodo PRIMA di essere avviata, non dopo.
   */
  public boolean canAfford(int amount) {
    return currentStamina >= amount;
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

  /**
   * Registra una vittoria d'iniziativa: il combattente attacca davvero, conservando o
   * conquistando l'iniziativa. Incrementa la catena di attacchi consecutivi.
   */
  public void winInitiative() {
    consecutiveInitiativeWins++;
  }

  /**
   * Azzera la catena di attacchi consecutivi: il combattente perde l'iniziativa oppure
   * riposa invece di attaccare (il riposo scarica la fatica della catena).
   */
  public void loseInitiative() {
    consecutiveInitiativeWins = 0;
  }
}
