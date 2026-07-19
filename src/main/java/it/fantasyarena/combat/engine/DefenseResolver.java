package it.fantasyarena.combat.engine;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.config.CombatSettings.ChanceWeights;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.engine.DefenseOutcome.DefenseResult;
import it.fantasyarena.combat.model.Characteristics;
import it.fantasyarena.combat.model.Fighter;
import it.fantasytoolkitcore.core.model.Characteristic;

/**
 * Risoluzione pura della difesa: dato un {@link DiceThrow} in input, stabilisce se il
 * difensore schiva, para o subisce il colpo, con la relativa riduzione di danno. Nessun
 * lancio di dadi qui.
 */
public final class DefenseResolver {

  private final CombatSettings settings;

  public DefenseResolver(CombatSettings settings) {
    this.settings = settings;
  }

  public DefenseOutcome resolveDefense(Fighter defender, Fighter attacker, DiceThrow defenseThrow) {
    double dodgeChance = computeDodgeChance(defender, attacker);
    double parryChance = computeParryChance(defender);
    double roll = defenseThrow.normalized();

    if (roll <= dodgeChance) {
      return new DefenseOutcome(DefenseResult.DODGED, settings.chanceWeights().dodgeDamageReduction());
    }
    if (roll <= dodgeChance + parryChance) {
      return new DefenseOutcome(DefenseResult.PARRIED, settings.chanceWeights().parryDamageReduction());
    }
    return new DefenseOutcome(DefenseResult.HIT_TAKEN, 0.0);
  }

  private double computeDodgeChance(Fighter defender, Fighter attacker) {
    ChanceWeights chances = settings.chanceWeights();
    int defenderAgility = Characteristics.valueOf(defender.character(), Characteristic.AGILITY);
    int attackerAgility = Characteristics.valueOf(attacker.character(), Characteristic.AGILITY);
    double dodgeChance = chances.baseDodgeChance()
        + chances.dodgeChanceAgilityFactor() * (defenderAgility - attackerAgility);
    return clamp(dodgeChance, chances.minDodgeChance(), chances.maxDodgeChance());
  }

  /**
   * La SPEC lascia il denominatore della formula incompleto ("defDefensive/…"): risolto con
   * {@code chanceWeights().parryDefenseDivisor()} (default 200), tarabile in {@link CombatSettings}.
   * Usa il Defensive Rating completo, quindi un eventuale scudo (non esercitato in v1) alzerebbe
   * naturalmente la parata, coerente con "guerriero con scudo più alta" della SPEC.
   */
  private double computeParryChance(Fighter defender) {
    ChanceWeights chances = settings.chanceWeights();
    double parryChance = chances.baseParryChance() + defender.ratings().defensiveRating() / chances.parryDefenseDivisor();
    return clamp(parryChance, chances.minParryChance(), chances.maxParryChance());
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
