package it.fantasyarena.combat.engine;

import java.util.ArrayList;
import java.util.List;

import it.fantasyarena.combat.config.CombatFormulas;
import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceRoller;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.FighterState;
import it.fantasyarena.combat.result.ActionOutcome;
import it.fantasyarena.combat.result.InitiativeOverride;
import it.fantasyarena.combat.result.TurnHighlight;
import it.fantasyarena.combat.result.TurnLogEntry;
import it.fantasyarena.combat.result.TurnResult;

/**
 * Orchestra il singolo turno: lancia i dadi via {@link DiceRoller} e passa i
 * {@link DiceThrow} risultanti ai resolver puri del core, poi applica danno e aggiorna
 * stamina/momentum. Nessuna formula qui: solo orchestrazione parlante. Un'azione (attacco,
 * schivata, parata) parte solo se pagabile per intero: se non lo è, si ripiega su un'azione
 * più economica o, in ultima istanza, sul riposo/colpo pieno.
 */
public class TurnOrchestrator {

  private final DiceRoller diceRoller;
  private final HitResolver hitResolver;
  private final DefenseResolver defenseResolver;
  private final DamageCalculator damageCalculator;
  private final MomentumRules momentumRules;
  private final StaminaRules staminaRules;
  private final CombatSettings settings;
  private final TurnChronicler turnChronicler;
  private final PowerStrikeResolver powerStrikeResolver;

  public TurnOrchestrator(DiceRoller diceRoller, HitResolver hitResolver, DefenseResolver defenseResolver,
      DamageCalculator damageCalculator, MomentumRules momentumRules, StaminaRules staminaRules,
      CombatSettings settings) {
    this.diceRoller = diceRoller;
    this.hitResolver = hitResolver;
    this.defenseResolver = defenseResolver;
    this.damageCalculator = damageCalculator;
    this.momentumRules = momentumRules;
    this.staminaRules = staminaRules;
    this.settings = settings;
    this.turnChronicler = new TurnChronicler();
    this.powerStrikeResolver = new PowerStrikeResolver(settings);
  }

  public TurnResult playTurn(int turnNumber, Fighter attacker, Fighter defender, CombatContext context) {
    TurnResult turnResult = resolveTurn(turnNumber, attacker, defender, context);

    // Chi non e' stato l'attore in questo turno recupera passivamente Stamina a fine turno:
    // attutisce l'usura senza annullare i costi di difesa gia' pagati sopra, nello stesso turno.
    defender.state().recoverStamina(staminaRules.passiveRecovery());

    return turnResult;
  }

  private TurnResult resolveTurn(int turnNumber, Fighter attacker, Fighter defender, CombatContext context) {
    // Verifica-poi-decrementa: il cooldown avanza anche nei turni di riposo, non solo quando si
    // attacca davvero.
    boolean powerReady = attacker.state().powerStrikeReady();
    attacker.state().tickPowerStrikeCooldown();

    int attackCost = staminaRules.effectiveAttackCost(attacker.state().consecutiveInitiativeWins());
    if (staminaRules.shouldRest(attacker.state().currentStamina(), attacker.ratings().maxStamina())
        || !attacker.state().canAfford(attackCost)) {
      return resolveRest(turnNumber, attacker);
    }

    int powerCost = powerCost(attackCost);
    boolean powerStrike = decidePowerStrike(attacker, powerReady, powerCost);
    attacker.state().consumeStamina(powerStrike ? powerCost : attackCost);
    if (powerStrike) {
      attacker.state().startPowerStrikeCooldown(settings.powerStrikeWeights().cooldownTurns());
    }

    DiceThrow attackThrow = diceRoller.d20();
    HitOutcome hitOutcome = hitResolver.resolveHit(attacker, defender, attackThrow);

    if (!hitOutcome.hit()) {
      return resolveMiss(turnNumber, attacker, defender, powerStrike);
    }

    return resolveHitLanded(turnNumber, attacker, defender, context, hitOutcome, attackThrow, powerStrike);
  }

  /**
   * Decide se l'attaccante tenta il colpo potente: il jitter di decisione è tirato SOLO se il
   * costo raddoppiato è pagabile E il cooldown è esaurito, cosi' un colpo potente non tentabile
   * (per costo o per cooldown attivo) non consuma alcun dado in più.
   */
  private boolean decidePowerStrike(Fighter attacker, boolean powerReady, int powerCost) {
    if (!powerReady || !attacker.state().canAfford(powerCost)) {
      return false;
    }
    DiceThrow powerJitter = diceRoller.roll(settings.powerStrikeWeights().jitterDiceFaces());
    return powerStrikeResolver.decide(attacker, powerJitter);
  }

  private int powerCost(int attackCost) {
    return CombatFormulas.powerStrikeCost(settings.powerStrikeWeights(), attackCost);
  }

  private TurnResult resolveRest(int turnNumber, Fighter attacker) {
    attacker.state().loseInitiative();

    int before = attacker.state().currentStamina();
    attacker.state().recoverStamina(staminaRules.restRecovery());
    int recovered = attacker.state().currentStamina() - before;
    String description = attacker.name() + " riposa e recupera " + recovered + " stamina.";
    ActionOutcome action = new ActionOutcome(ActionOutcome.Kind.REST, 0, recovered, false, false);
    TurnLogEntry logEntry = new TurnLogEntry(turnNumber, description).withAction(action);
    return new TurnResult(logEntry, InitiativeOverride.REST_YIELD);
  }

  private TurnResult resolveMiss(int turnNumber, Fighter attacker, Fighter defender, boolean powerStrike) {
    applyMomentumDelta(attacker, momentumRules.deltaForMiss());
    String weaponName = String.valueOf(attacker.weapon().weapon());
    String description = turnChronicler.describeMiss(attacker.name(), defender.name(), weaponName, powerStrike);
    ActionOutcome action = new ActionOutcome(ActionOutcome.Kind.MISS, 0, 0, false, powerStrike);
    TurnLogEntry logEntry = new TurnLogEntry(turnNumber, description).withAction(action);
    return new TurnResult(logEntry, InitiativeOverride.NONE);
  }

  private TurnResult resolveHitLanded(int turnNumber, Fighter attacker, Fighter defender, CombatContext context,
      HitOutcome hitOutcome, DiceThrow attackThrow, boolean powerStrike) {

    boolean defenderCanDefend = staminaRules.canDefend(defender.state().currentStamina());
    DefenseOutcome defenseOutcome = resolveDefense(defender, attacker, defenderCanDefend);
    recordActionTally(attacker, defender, defenseOutcome);

    DiceThrow varianceThrow = diceRoller.d100();
    int damage = damageCalculator.calculateDamage(attacker, defender, context, hitOutcome, defenseOutcome,
        varianceThrow, powerStrike);
    defender.state().applyDamage(damage);
    applyImpactStamina(defender, defenseOutcome, damage);

    updateMomentumAfterHit(attacker, defender, hitOutcome, defenseOutcome);

    List<TurnHighlight> highlights =
        collectHighlights(defenseOutcome, hitOutcome, attackThrow, damage, defender, powerStrike);
    String weaponName = String.valueOf(attacker.weapon().weapon());
    String prefix = turnChronicler.describeAttackPrefix(attacker.name(), defender.name(), weaponName, powerStrike);
    String description = prefix
        + turnChronicler.describeOutcome(defenseOutcome.result(), damage, defenderCanDefend, highlights,
            defender.name());

    boolean defenderDodged = defenseOutcome.result() == DefenseOutcome.DefenseResult.DODGED;
    InitiativeOverride override = defenderDodged ? InitiativeOverride.DODGE_STEAL : InitiativeOverride.NONE;
    ActionOutcome action = new ActionOutcome(actionKindOf(defenseOutcome.result()), damage, 0,
        hitOutcome.critical(), powerStrike);
    TurnLogEntry logEntry = new TurnLogEntry(turnNumber, description).withHighlights(highlights).withAction(action);
    return new TurnResult(logEntry, override);
  }

  private ActionOutcome.Kind actionKindOf(DefenseOutcome.DefenseResult result) {
    return switch (result) {
      case HIT_TAKEN -> ActionOutcome.Kind.HIT;
      case PARRIED -> ActionOutcome.Kind.PARRIED;
      case DODGED -> ActionOutcome.Kind.DODGED;
    };
  }

  /**
   * Raccoglie tutti gli highlight applicabili al colpo: fonte unica di verità, letta sia dalla
   * descrizione del turno sia (in futuro) dalla narrazione finale. Gli highlight offensivi
   * (perfetto/critico/pesante/colpo potente) hanno senso solo su un colpo pieno andato a segno;
   * il colpo di grazia si applica invece a qualunque esito di difesa, se il difensore risulta
   * sconfitto.
   */
  private List<TurnHighlight> collectHighlights(DefenseOutcome defenseOutcome, HitOutcome hitOutcome,
      DiceThrow attackThrow, int damage, Fighter defender, boolean powerStrike) {
    List<TurnHighlight> highlights = new ArrayList<>();
    if (defenseOutcome.result() == DefenseOutcome.DefenseResult.HIT_TAKEN) {
      collectOffensiveHighlights(highlights, hitOutcome, attackThrow, damage, defender);
      if (powerStrike) {
        highlights.add(TurnHighlight.POWER_STRIKE);
      }
    }
    if (defender.isDefeated()) {
      highlights.add(TurnHighlight.KNOCKOUT);
    }
    return highlights;
  }

  private void collectOffensiveHighlights(List<TurnHighlight> highlights, HitOutcome hitOutcome,
      DiceThrow attackThrow, int damage, Fighter defender) {
    if (attackThrow.isNaturalMaximum()) {
      highlights.add(TurnHighlight.PERFECT_HIT);
    }
    if (hitOutcome.critical()) {
      highlights.add(TurnHighlight.CRITICAL);
    }
    if (CombatFormulas.isHeavyBlow(settings.chronicleWeights(), damage, defender.ratings().maxHealth())) {
      highlights.add(TurnHighlight.HEAVY_BLOW);
    }
  }

  /**
   * Aggiorna i contatori cumulativi usati dalla decisione ai punti in caso di timeout: un colpo
   * pieno andato a segno conta per l'attaccante, una parata o una schivata riuscite contano per
   * il difensore.
   */
  private void recordActionTally(Fighter attacker, Fighter defender, DefenseOutcome defenseOutcome) {
    switch (defenseOutcome.result()) {
      case HIT_TAKEN -> attacker.state().recordHitLanded();
      case PARRIED -> defender.state().recordParry();
      case DODGED -> defender.state().recordDodge();
    }
  }

  private DefenseOutcome resolveDefense(Fighter defender, Fighter attacker, boolean defenderCanDefend) {
    if (!defenderCanDefend) {
      return new DefenseOutcome(DefenseOutcome.DefenseResult.HIT_TAKEN, 0.0);
    }

    DiceThrow defenseThrow = diceRoller.d20();
    DefenseOutcome outcome = defenseResolver.resolveDefense(defender, attacker, defenseThrow);
    return payDefenseCostWithFallback(defender, outcome);
  }

  /**
   * Applica il costo Stamina della difesa risolta dal tiro, con ripiego se non pagabile:
   * schivata non pagabile -&gt; parata se pagabile -&gt; altrimenti colpo pieno. La parata
   * risolta direttamente dal tiro segue la stessa regola: se non pagabile, colpo pieno. Nessuna
   * azione parte se non e' interamente pagabile con la Stamina corrente.
   */
  private DefenseOutcome payDefenseCostWithFallback(Fighter defender, DefenseOutcome outcome) {
    return switch (outcome.result()) {
      case DODGED -> resolveDodgeWithFallback(defender, outcome);
      case PARRIED -> resolveParryWithFallback(defender, outcome);
      case HIT_TAKEN -> outcome;
    };
  }

  private DefenseOutcome resolveDodgeWithFallback(Fighter defender, DefenseOutcome dodgeOutcome) {
    FighterState state = defender.state();
    if (state.canAfford(staminaRules.dodgeCost())) {
      state.consumeStamina(staminaRules.dodgeCost());
      return dodgeOutcome;
    }
    if (state.canAfford(staminaRules.parryCost())) {
      state.consumeStamina(staminaRules.parryCost());
      return defenseResolver.parryFallbackOutcome();
    }
    return new DefenseOutcome(DefenseOutcome.DefenseResult.HIT_TAKEN, 0.0);
  }

  private DefenseOutcome resolveParryWithFallback(Fighter defender, DefenseOutcome parryOutcome) {
    FighterState state = defender.state();
    if (state.canAfford(staminaRules.parryCost())) {
      state.consumeStamina(staminaRules.parryCost());
      return parryOutcome;
    }
    return new DefenseOutcome(DefenseOutcome.DefenseResult.HIT_TAKEN, 0.0);
  }

  /**
   * Su un colpo pieno, la Stamina di chi incassa cala in proporzione al danno subito (con
   * minimo garantito): niente di piu' pesante per parata/schivata riuscite, gia' pagate in
   * {@link #payDefenseCostWithFallback}.
   */
  private void applyImpactStamina(Fighter defender, DefenseOutcome defenseOutcome, int damage) {
    if (defenseOutcome.result() == DefenseOutcome.DefenseResult.HIT_TAKEN) {
      defender.state().consumeStamina(staminaRules.impactStaminaLoss(damage));
    }
  }

  private void updateMomentumAfterHit(Fighter attacker, Fighter defender, HitOutcome hitOutcome,
      DefenseOutcome defenseOutcome) {
    switch (defenseOutcome.result()) {
      case DODGED -> applyMomentumDelta(defender, momentumRules.deltaForDodgeSuccess());
      case PARRIED -> applyMomentumDelta(defender, momentumRules.deltaForParrySuccess());
      case HIT_TAKEN -> applyMomentumForLandedHit(attacker, defender, hitOutcome);
    }
  }

  private void applyMomentumForLandedHit(Fighter attacker, Fighter defender, HitOutcome hitOutcome) {
    applyMomentumDelta(attacker, momentumRules.deltaForHitLanded());
    applyMomentumDelta(defender, momentumRules.deltaForHitTaken());

    if (hitOutcome.critical()) {
      applyMomentumDelta(attacker, momentumRules.deltaForCriticalDealt());
      applyMomentumDelta(defender, momentumRules.deltaForCriticalTaken());
    }
  }

  private void applyMomentumDelta(Fighter fighter, int delta) {
    FighterState state = fighter.state();
    state.setMomentum(momentumRules.clamp(state.momentum() + delta));
  }
}
