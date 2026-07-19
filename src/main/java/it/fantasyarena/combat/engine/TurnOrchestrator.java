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

  private TurnLogEntry resolveMiss(int turnNumber, Fighter attacker, Fighter defender) {
    applyMomentumDelta(attacker, momentumRules.deltaForMiss());
    String description = attacker.name() + " attacca " + defender.name() + " ma manca il colpo.";
    return new TurnLogEntry(turnNumber, description);
  }

  private TurnLogEntry resolveHitLanded(int turnNumber, Fighter attacker, Fighter defender, CombatContext context,
      HitOutcome hitOutcome) {

    DiceThrow defenseThrow = diceRoller.d20();
    DefenseOutcome defenseOutcome = defenseResolver.resolveDefense(defender, attacker, defenseThrow);
    payDefenseCost(defender, defenseOutcome);

    DiceThrow varianceThrow = diceRoller.d100();
    int damage = damageCalculator.calculateDamage(attacker, defender, context, hitOutcome, defenseOutcome, varianceThrow);
    defender.state().applyDamage(damage);

    updateMomentumAfterHit(attacker, defender, hitOutcome, defenseOutcome);

    String description = attackAction.describe(attacker, defender, context) + describeDefense(defenseOutcome, damage);
    return new TurnLogEntry(turnNumber, description);
  }

  private void payDefenseCost(Fighter defender, DefenseOutcome defenseOutcome) {
    int cost = switch (defenseOutcome.result()) {
      case DODGED -> staminaRules.dodgeCost();
      case PARRIED -> staminaRules.parryCost();
      case HIT_TAKEN -> staminaRules.impactCost();
    };
    defender.state().consumeStamina(cost);
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

  private String describeDefense(DefenseOutcome defenseOutcome, int damage) {
    return switch (defenseOutcome.result()) {
      case DODGED -> ", schivato.";
      case PARRIED -> ", parato (" + damage + " danni).";
      case HIT_TAKEN -> ", colpo a segno (" + damage + " danni).";
    };
  }
}
