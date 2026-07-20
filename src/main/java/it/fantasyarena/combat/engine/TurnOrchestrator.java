package it.fantasyarena.combat.engine;

import it.fantasyarena.combat.action.AttackAction;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceRoller;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.FighterState;
import it.fantasyarena.combat.result.InitiativeOverride;
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
  private final AttackAction attackAction;

  public TurnOrchestrator(DiceRoller diceRoller, HitResolver hitResolver, DefenseResolver defenseResolver,
      DamageCalculator damageCalculator, MomentumRules momentumRules, StaminaRules staminaRules) {
    this.diceRoller = diceRoller;
    this.hitResolver = hitResolver;
    this.defenseResolver = defenseResolver;
    this.damageCalculator = damageCalculator;
    this.momentumRules = momentumRules;
    this.staminaRules = staminaRules;
    this.attackAction = new AttackAction(staminaRules.attackCost());
  }

  public TurnResult playTurn(int turnNumber, Fighter attacker, Fighter defender, CombatContext context) {
    TurnResult turnResult = resolveTurn(turnNumber, attacker, defender, context);

    // Chi non e' stato l'attore in questo turno recupera passivamente Stamina a fine turno:
    // attutisce l'usura senza annullare i costi di difesa gia' pagati sopra, nello stesso turno.
    defender.state().recoverStamina(staminaRules.passiveRecovery());

    return turnResult;
  }

  private TurnResult resolveTurn(int turnNumber, Fighter attacker, Fighter defender, CombatContext context) {
    int attackCost = staminaRules.effectiveAttackCost(attacker.state().consecutiveInitiativeWins());
    if (staminaRules.shouldRest(attacker.state().currentStamina()) || !attacker.state().canAfford(attackCost)) {
      return resolveRest(turnNumber, attacker);
    }

    attacker.state().consumeStamina(attackCost);

    DiceThrow attackThrow = diceRoller.d20();
    HitOutcome hitOutcome = hitResolver.resolveHit(attacker, defender, attackThrow);

    if (!hitOutcome.hit()) {
      return resolveMiss(turnNumber, attacker, defender);
    }

    return resolveHitLanded(turnNumber, attacker, defender, context, hitOutcome);
  }

  private TurnResult resolveRest(int turnNumber, Fighter attacker) {
    attacker.state().loseInitiative();

    int before = attacker.state().currentStamina();
    attacker.state().recoverStamina(staminaRules.restRecovery());
    int recovered = attacker.state().currentStamina() - before;
    String description = attacker.name() + " riposa e recupera " + recovered + " stamina.";
    return new TurnResult(new TurnLogEntry(turnNumber, description), InitiativeOverride.REST_YIELD);
  }

  private TurnResult resolveMiss(int turnNumber, Fighter attacker, Fighter defender) {
    applyMomentumDelta(attacker, momentumRules.deltaForMiss());
    String description = attacker.name() + " attacca " + defender.name() + " ma manca il colpo.";
    return new TurnResult(new TurnLogEntry(turnNumber, description), InitiativeOverride.NONE);
  }

  private TurnResult resolveHitLanded(int turnNumber, Fighter attacker, Fighter defender, CombatContext context,
      HitOutcome hitOutcome) {

    boolean defenderCanDefend = staminaRules.canDefend(defender.state().currentStamina());
    DefenseOutcome defenseOutcome = resolveDefense(defender, attacker, defenderCanDefend);

    DiceThrow varianceThrow = diceRoller.d100();
    int damage = damageCalculator.calculateDamage(attacker, defender, context, hitOutcome, defenseOutcome, varianceThrow);
    defender.state().applyDamage(damage);
    applyImpactStamina(defender, defenseOutcome, damage);

    updateMomentumAfterHit(attacker, defender, hitOutcome, defenseOutcome);

    String description =
        attackAction.describe(attacker, defender, context) + describeDefense(defenseOutcome, damage, defenderCanDefend);
    boolean defenderDodged = defenseOutcome.result() == DefenseOutcome.DefenseResult.DODGED;
    InitiativeOverride override = defenderDodged ? InitiativeOverride.DODGE_STEAL : InitiativeOverride.NONE;
    return new TurnResult(new TurnLogEntry(turnNumber, description), override);
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

  private String describeDefense(DefenseOutcome defenseOutcome, int damage, boolean defenderCanDefend) {
    return switch (defenseOutcome.result()) {
      case DODGED -> ", schivato.";
      case PARRIED -> ", parato (" + damage + " danni).";
      case HIT_TAKEN -> defenderCanDefend
          ? ", colpo a segno (" + damage + " danni)."
          : ", colpo a segno (difensore esausto, " + damage + " danni).";
    };
  }
}
