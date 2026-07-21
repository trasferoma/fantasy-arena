package it.fantasyarena.combat.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings.ChanceWeights;
import it.fantasyarena.combat.config.CombatSettings.ChronicleWeights;
import it.fantasyarena.combat.config.CombatSettings.MomentumWeights;
import it.fantasyarena.combat.config.CombatSettings.StaminaWeights;

/**
 * Assert deterministici mirati sulle formule pure di {@link CombatFormulas}: casi limite (clamp
 * ai bordi, floor/cap, fasce) non ridondanti coi test dei resolver, che già coprono il
 * comportamento end-to-end tramite le shell.
 */
class CombatFormulasTest {

  private static final double DELTA = 1e-9;

  private final CombatSettings settings = CombatSettings.defaults();

  @Test
  void clamp_limitaAiBordi() {
    assertEquals(0.0, CombatFormulas.clamp(-5.0, 0.0, 10.0), DELTA);
    assertEquals(10.0, CombatFormulas.clamp(15.0, 0.0, 10.0), DELTA);
    assertEquals(5.0, CombatFormulas.clamp(5.0, 0.0, 10.0), DELTA);
  }

  @Test
  void clamp01_limitaTraZeroEUno() {
    assertEquals(0.0, CombatFormulas.clamp01(-0.5), DELTA);
    assertEquals(1.0, CombatFormulas.clamp01(1.5), DELTA);
  }

  @Test
  void ratio_rapportoSemplice() {
    assertEquals(0.5, CombatFormulas.ratio(5, 10), DELTA);
  }

  @Test
  void hitChance_clampAiLimiti() {
    ChanceWeights weights = settings.chanceWeights();
    assertEquals(weights.maxHitChance(), CombatFormulas.hitChance(weights, 100, 0), DELTA);
    assertEquals(weights.minHitChance(), CombatFormulas.hitChance(weights, 0, 100), DELTA);
  }

  @Test
  void dodgeChance_clampAiLimiti() {
    ChanceWeights weights = settings.chanceWeights();
    assertEquals(weights.maxDodgeChance(), CombatFormulas.dodgeChance(weights, 100, 0), DELTA);
    assertEquals(weights.minDodgeChance(), CombatFormulas.dodgeChance(weights, 0, 100), DELTA);
  }

  @Test
  void momentumEffectMultiplier_agliEstremiEAZero() {
    MomentumWeights weights = settings.momentumWeights();
    assertEquals(0.85, CombatFormulas.momentumEffectMultiplier(weights, weights.min()), DELTA);
    assertEquals(1.15, CombatFormulas.momentumEffectMultiplier(weights, weights.max()), DELTA);
    assertEquals(1.0, CombatFormulas.momentumEffectMultiplier(weights, 0), DELTA);
  }

  @Test
  void fatigueMultiplier_nelleTreFasce() {
    StaminaWeights weights = settings.staminaWeights();
    assertEquals(1.0, CombatFormulas.fatigueMultiplier(weights, 80, 100), DELTA);
    assertEquals(1.0 - weights.mediumFatiguePenalty(), CombatFormulas.fatigueMultiplier(weights, 30, 100), DELTA);
    assertEquals(1.0 - weights.heavyFatiguePenalty(), CombatFormulas.fatigueMultiplier(weights, 10, 100), DELTA);
  }

  @Test
  void rawDamage_floorAUnoEDampeningMeta() {
    assertEquals(1.0, CombatFormulas.rawDamage(1.0, 100.0), DELTA);
    assertEquals(10.0, CombatFormulas.rawDamage(20.0, 20.0), DELTA);
  }

  @Test
  void impactStaminaLoss_minimoGarantito() {
    StaminaWeights weights = settings.staminaWeights();
    assertEquals(weights.impactCost(), CombatFormulas.impactStaminaLoss(weights, 0));
  }

  @Test
  void effectiveAttackCost_capDelMalusDiCatena() {
    StaminaWeights weights = settings.staminaWeights();
    int expectedCapped = weights.attackCost() + weights.chainMalusCap();
    assertEquals(expectedCapped, CombatFormulas.effectiveAttackCost(weights, 100));
  }

  @Test
  void shouldRest_sopraESottoSoglia() {
    StaminaWeights weights = settings.staminaWeights();
    int maxStamina = 100;
    int aboveThreshold = (int) (weights.restThresholdRatio() * maxStamina) + 1;
    int belowThreshold = (int) (weights.restThresholdRatio() * maxStamina) - 1;

    assertFalse(CombatFormulas.shouldRest(weights, aboveThreshold, maxStamina));
    assertTrue(CombatFormulas.shouldRest(weights, belowThreshold, maxStamina));
  }

  @Test
  void isHeavyBlow_sogliaPercentualeVitaMassima() {
    ChronicleWeights weights = settings.chronicleWeights();
    int maxHealth = 100;
    int heavyBlowThreshold = (int) Math.ceil(weights.heavyBlowHealthRatio() * maxHealth);

    assertTrue(CombatFormulas.isHeavyBlow(weights, heavyBlowThreshold, maxHealth));
    assertFalse(CombatFormulas.isHeavyBlow(weights, heavyBlowThreshold - 1, maxHealth));
  }

  @Test
  void dodgesEParries_sulleFasceDelloStessoTiro() {
    double dodgeChance = 0.20;
    double parryChance = 0.10;

    assertTrue(CombatFormulas.dodges(0.15, dodgeChance));
    assertFalse(CombatFormulas.parries(0.15, dodgeChance, parryChance));

    assertFalse(CombatFormulas.dodges(0.25, dodgeChance));
    assertTrue(CombatFormulas.parries(0.25, dodgeChance, parryChance));

    assertFalse(CombatFormulas.dodges(0.35, dodgeChance));
    assertFalse(CombatFormulas.parries(0.35, dodgeChance, parryChance));
  }
}
