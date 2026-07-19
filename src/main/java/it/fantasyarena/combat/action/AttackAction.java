package it.fantasyarena.combat.action;

import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.model.Fighter;

/**
 * Attacco base con l'arma equipaggiata: l'unica {@link CombatAction} disponibile in v1.
 */
public final class AttackAction implements CombatAction {

  private final int staminaCost;

  public AttackAction(int staminaCost) {
    this.staminaCost = staminaCost;
  }

  @Override
  public int staminaCost() {
    return staminaCost;
  }

  @Override
  public String describe(Fighter actor, Fighter target, CombatContext context) {
    return actor.name() + " attacca " + target.name() + " con " + actor.weapon().weapon();
  }
}
