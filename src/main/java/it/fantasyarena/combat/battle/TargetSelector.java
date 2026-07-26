package it.fantasyarena.combat.battle;

import java.util.List;

import it.fantasyarena.combat.model.Fighter;

/**
 * Sceglie il bersaglio di {@code attacker} fra i nemici vivi. Puro: nessun dado, nessuna
 * mutazione.
 */
public interface TargetSelector {

  /**
   * @param currentTarget bersaglio corrente dell'attaccante, oppure {@code null} se non ne ha
   *     ancora uno
   */
  Fighter selectTarget(Fighter attacker, Fighter currentTarget, List<Fighter> livingEnemies);
}
