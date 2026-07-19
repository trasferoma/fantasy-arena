package it.fantasyarena.combat.engine;

import it.fantasyarena.combat.action.AttackAction;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceRoller;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.FighterState;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * Orchestra il singolo turno: lancia i dadi via {@link DiceRoller} e passa i
 * {@link DiceThrow} risultanti ai resolver puri del core, poi applica danno e aggiorna
 * stamina/momentum. Nessuna formula qui: solo orchestrazione parlante.
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

  public TurnLogEntry playTurn(int turnNumber, Fighter attacker, Fighter defender, CombatContext context) {
    if (staminaRules.shouldRest(attacker.state().currentStamina())) {
      return resolveRest(turnNumber, attacker);
    }

    payAttackCost(attacker);

    DiceThrow attackThrow = diceRoller.d20();
    HitOutcome hitOutcome = hitResolver.resolveHit(attacker, defender, attackThrow);

    if (!hitOutcome.hit()) {
      return resolveMiss(turnNumber, attacker, defender);
    }

    return resolveHitLanded(turnNumber, attacker, defender, context, hitOutcome);
  }

  private void payAttackCost(Fighter attacker) {
    attacker.state().consumeStamina(attackAction.staminaCost());
  }

  private TurnLogEntry resolveRest(int turnNumber, Fighter attacker) {
    int before = attacker.state().currentStamina();
    attacker.state().recoverStamina(staminaRules.restRecovery());
    int recovered = attacker.state().currentStamina() - before;
    String description = attacker.name() + " riposa e recupera " + recovered + " stamina.";
    return new TurnLogEntry(turnNumber, description);
  }

  private TurnLogEntry resolveMiss(int turnNumber, Fighter attacker, Fighter defender) {
    applyMomentumDelta(attacker, momentumRules.deltaForMiss());
    String description = attacker.name() + " attacca " + defender.name() + " ma manca il colpo.";
    return new TurnLogEntry(turnNumber, description);
  }

  private TurnLogEntry resolveHitLanded(int turnNumber, Fighter attacker, Fighter defender, CombatContext context,
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
    return new TurnLogEntry(turnNumber, description);
  }

  private DefenseOutcome resolveDefense(Fighter defender, Fighter attacker, boolean defenderCanDefend) {
    if (!defenderCanDefend) {
      return new DefenseOutcome(DefenseOutcome.DefenseResult.HIT_TAKEN, 0.0);
    }

    DiceThrow defenseThrow = diceRoller.d20();
    DefenseOutcome outcome = defenseResolver.resolveDefense(defender, attacker, defenseThrow);
    payDefenseCost(defender, outcome);
    return outcome;
  }

  private void payDefenseCost(Fighter defender, DefenseOutcome defenseOutcome) {
    // Il costo Stamina di un colpo pieno non e' piu' fisso: e' proporzionale al danno e viene
    // applicato in applyImpactStamina, dopo il calcolo del danno stesso.
    int cost = switch (defenseOutcome.result()) {
      case DODGED -> staminaRules.dodgeCost();
      case PARRIED -> staminaRules.parryCost();
      case HIT_TAKEN -> 0;
    };
    defender.state().consumeStamina(cost);
  }

  /**
   * Su un colpo pieno, la Stamina di chi incassa cala in proporzione al danno subito (con
   * minimo garantito): niente di piu' pesante per parata/schivata riuscite, gia' pagate in
   * {@link #payDefenseCost}.
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
