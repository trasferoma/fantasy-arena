package it.fantasyarena.combat.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stato mutabile del combattente durante lo scontro: Health e Stamina correnti, Momentum,
 * catena di vittorie d'iniziativa consecutive, cooldown del colpo potente e status effect
 * attivi (lista vuota in v1). Separato dai Rating intrinseci, che restano sempre immutabili.
 * Nessuna formula qui: i floor a 0/max sono semplici invarianti del contenitore, non regole di
 * bilanciamento (che vivono in {@code MomentumRules}/{@code StaminaRules}). Contratto di
 * affordabilita': un'azione non deve partire se {@link #canAfford(int)} e' falso;
 * {@link #consumeStamina(int)} resta con un floor a 0 come guardia difensiva interna, non come
 * meccanismo di pagamento parziale a debito. I contatori
 * {@code staminaConsumedThisTurn}/{@code staminaRecoveredThisTurn} sono puro bookkeeping per il
 * log del turno corrente, non regole di bilanciamento: vanno azzerati da chi orchestra il turno
 * con {@link #resetTurnStaminaCounters()}. Il contatore {@code powerStrikeCooldown} (0 = pronto)
 * e' semplice bookkeeping del contenitore: la semantica verifica-poi-decrementa e l'avvio del
 * cooldown all'esecuzione del colpo potente sono responsabilita' di chi orchestra il turno.
 */
public final class FighterState {

  private final int maxStamina;
  private final List<String> statusEffects;

  private int currentHealth;
  private int currentStamina;
  private int momentum;
  private int consecutiveInitiativeWins;
  private int staminaConsumedThisTurn;
  private int staminaRecoveredThisTurn;
  private int powerStrikeCooldown;

  public FighterState(int maxHealth, int maxStamina) {
    this.maxStamina = maxStamina;
    this.currentHealth = maxHealth;
    this.currentStamina = maxStamina;
    this.momentum = 0;
    this.consecutiveInitiativeWins = 0;
    this.staminaConsumedThisTurn = 0;
    this.staminaRecoveredThisTurn = 0;
    this.powerStrikeCooldown = 0;
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

  public int staminaConsumedThisTurn() {
    return staminaConsumedThisTurn;
  }

  public int staminaRecoveredThisTurn() {
    return staminaRecoveredThisTurn;
  }

  public List<String> statusEffects() {
    return Collections.unmodifiableList(statusEffects);
  }

  public boolean isDefeated() {
    return currentHealth <= 0;
  }

  /**
   * Vero sse il cooldown del colpo potente e' esaurito: da verificare PRIMA di decrementarlo
   * con {@link #tickPowerStrikeCooldown()}, secondo la semantica verifica-poi-decrementa.
   */
  public boolean powerStrikeReady() {
    return powerStrikeCooldown == 0;
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
    int before = currentStamina;
    currentStamina = Math.max(0, Math.min(maxStamina, currentStamina - amount));
    staminaConsumedThisTurn += before - currentStamina;
  }

  public void recoverStamina(int amount) {
    // Il cap al massimo e' un invariante del contenitore, non una regola di bilanciamento.
    int before = currentStamina;
    currentStamina = Math.min(maxStamina, currentStamina + amount);
    staminaRecoveredThisTurn += currentStamina - before;
  }

  /**
   * Azzera i contatori di Stamina consumata/recuperata nel turno: da chiamare a inizio turno,
   * prima di qualsiasi {@link #consumeStamina(int)}/{@link #recoverStamina(int)} su questo stato.
   */
  public void resetTurnStaminaCounters() {
    staminaConsumedThisTurn = 0;
    staminaRecoveredThisTurn = 0;
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

  /**
   * Decrementa il cooldown del colpo potente di 1, se ancora attivo: avanza anche nei turni di
   * riposo, non solo in quelli in cui il combattente attacca.
   */
  public void tickPowerStrikeCooldown() {
    if (powerStrikeCooldown > 0) {
      powerStrikeCooldown--;
    }
  }

  /**
   * Avvia il cooldown del colpo potente dopo un'esecuzione: per {@code turns} turni d'azione
   * successivi {@link #powerStrikeReady()} restera' falso.
   */
  public void startPowerStrikeCooldown(int turns) {
    powerStrikeCooldown = turns;
  }
}
