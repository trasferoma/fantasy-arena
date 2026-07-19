package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.testsupport.CombatFixtures;

/**
 * Verifica mirata (non parte della mappa DoD 1-9) sul "critico su massimo naturale" descritto
 * dalla SPEC ({@code diceThrow.value() == diceThrow.faces()} implica sempre critico).
 * <p>
 * Test lasciato volutamente ROSSO: conferma un difetto di produzione già sospettato in fase di
 * pianificazione. {@link HitResolver#resolveHit} calcola {@code hit = attackThrow.normalized() <=
 * hitChance}; su un tiro naturale massimo {@code normalized()} vale {@code 1.0}, mentre
 * {@code hitChance} è sempre clampato a un massimo di {@code 0.95}
 * ({@link CombatSettings.ChanceWeights#maxHitChance()}). Di conseguenza {@code hit} risulta
 * sempre {@code false} su un tiro naturale massimo, e il ramo "critico su massimo naturale" in
 * {@code HitResolver.isCritical} non scatta mai, perché viene raggiunto solo quando
 * {@code hit} è già {@code true}. Non correggere il codice di produzione qui: la decisione va
 * presa a valle (vedi report).
 */
class HitResolverNaturalMaximumTest {

  @Test
  void naturalMaximumRoll_shouldAlwaysHitWithCritical_perSpec() {
    CombatSettings settings = CombatSettings.defaults();
    HitResolver hitResolver = new HitResolver(settings);
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 16, 12, 10, 8, 6, 7, 3);
    Fighter defender = CombatFixtures.createFighter("Difensore", 12, 14, 11, 9, 4, 5, 5);

    DiceThrow naturalMaximum = new DiceThrow(20, 20);
    HitOutcome outcome = hitResolver.resolveHit(attacker, defender, naturalMaximum);

    assertTrue(outcome.hit(), "un tiro naturale massimo dovrebbe sempre andare a segno per la SPEC");
    assertTrue(outcome.critical(), "un tiro naturale massimo dovrebbe sempre essere critico per la SPEC");
  }
}
