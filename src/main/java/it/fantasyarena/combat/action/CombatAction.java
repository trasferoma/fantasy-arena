package it.fantasyarena.combat.action;

import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.model.Fighter;

/**
 * Azione di combattimento eseguibile in un turno. Punto di estensione per abilità di
 * classe e magie future; in v1 esiste solo {@link AttackAction}.
 */
public interface CombatAction {

  int staminaCost();

  String describe(Fighter actor, Fighter target, CombatContext context);
}
