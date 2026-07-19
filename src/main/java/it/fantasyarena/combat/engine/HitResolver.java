package it.fantasyarena.combat.engine;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.config.CombatSettings.ChanceWeights;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Characteristics;
import it.fantasyarena.combat.model.Fighter;
import it.fantasytoolkitcore.core.model.Characteristic;

/**
 * Risoluzione pura del test per colpire: dato un {@link DiceThrow} in input, stabilisce
 * se l'attacco va a segno e se è critico. Nessun lancio di dadi qui: la casualità arriva
 * già come input dallo shell. Il massimo naturale del tiro d'attacco è sempre un colpo
 * a segno e critico, indipendentemente da {@code hitChance}.
 */
public final class HitResolver {

  private final CombatSettings settings;

  public HitResolver(CombatSettings settings) {
    this.settings = settings;
  }

  public HitOutcome resolveHit(Fighter attacker, Fighter defender, DiceThrow attackThrow) {
    if (attackThrow.isNaturalMaximum()) {
      return new HitOutcome(true, true);
    }

    double hitChance = computeHitChance(attacker, defender);
    boolean hit = attackThrow.normalized() <= hitChance;
    boolean critical = hit && isCritical(attacker, attackThrow);
    return new HitOutcome(hit, critical);
  }

  private double computeHitChance(Fighter attacker, Fighter defender) {
    ChanceWeights chances = settings.chanceWeights();
    int attackerAgility = Characteristics.valueOf(attacker.character(), Characteristic.AGILITY);
    int defenderAgility = Characteristics.valueOf(defender.character(), Characteristic.AGILITY);
    double hitChance = chances.baseHitChance() + chances.hitChanceAgilityFactor() * (attackerAgility - defenderAgility);
    return clamp(hitChance, chances.minHitChance(), chances.maxHitChance());
  }

  /**
   * Critico su {@code critChance}, riusando lo stesso {@code attackThrow} già consumato per il
   * test di colpire: la SPEC illustra un solo tiro per il test di colpire, quindi il critico
   * non richiede un lancio dedicato ulteriore. Il caso "massimo naturale" è già gestito prima
   * di arrivare qui, in {@link #resolveHit}, con colpo e critico garantiti.
   */
  private boolean isCritical(Fighter attacker, DiceThrow attackThrow) {
    ChanceWeights chances = settings.chanceWeights();
    int luck = Characteristics.valueOf(attacker.character(), Characteristic.LUCK);
    double critChance = chances.baseCritChance() + chances.critChanceLuckFactor() * luck;
    critChance = clamp(critChance, chances.minCritChance(), chances.maxCritChance());
    return attackThrow.normalized() <= critChance;
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
