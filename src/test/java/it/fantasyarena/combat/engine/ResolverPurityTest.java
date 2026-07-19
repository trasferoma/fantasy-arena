package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.testsupport.CombatFixtures;

/**
 * DoD 3 (purezza) — dati gli stessi {@link DiceThrow} in input, i resolver del core
 * ({@link HitResolver}, {@link DefenseResolver}, {@link DamageCalculator}) restituiscono
 * sempre lo stesso esito: nessun lancio interno, nessun effetto collaterale.
 */
class ResolverPurityTest {

  @Test
  void pureResolver_sameDiceThrow_sameOutcome() {
    CombatSettings settings = CombatSettings.defaults();
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 16, 12, 10, 8, 6, 7, 3);
    Fighter defender = CombatFixtures.createFighter("Difensore", 12, 14, 11, 9, 4, 5, 5);

    HitResolver hitResolver = new HitResolver(settings);
    DiceThrow attackThrow = new DiceThrow(15, 20);
    HitOutcome firstHit = hitResolver.resolveHit(attacker, defender, attackThrow);
    HitOutcome secondHit = hitResolver.resolveHit(attacker, defender, attackThrow);
    assertEquals(firstHit, secondHit, "stesso DiceThrow in input deve produrre lo stesso HitOutcome");

    DefenseResolver defenseResolver = new DefenseResolver(settings);
    DiceThrow defenseThrow = new DiceThrow(9, 20);
    DefenseOutcome firstDefense = defenseResolver.resolveDefense(defender, attacker, defenseThrow);
    DefenseOutcome secondDefense = defenseResolver.resolveDefense(defender, attacker, defenseThrow);
    assertEquals(firstDefense, secondDefense, "stesso DiceThrow in input deve produrre lo stesso DefenseOutcome");

    DamageCalculator damageCalculator =
        new DamageCalculator(settings, new MomentumRules(settings), new StaminaRules(settings));
    DiceThrow varianceThrow = new DiceThrow(50, 100);
    int firstDamage = damageCalculator.calculateDamage(attacker, defender, CombatContext.empty(), firstHit,
        firstDefense, varianceThrow);
    int secondDamage = damageCalculator.calculateDamage(attacker, defender, CombatContext.empty(), firstHit,
        firstDefense, varianceThrow);
    assertEquals(firstDamage, secondDamage, "stesso DiceThrow in input deve produrre lo stesso danno calcolato");
  }
}
