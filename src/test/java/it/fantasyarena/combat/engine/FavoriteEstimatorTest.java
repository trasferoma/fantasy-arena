package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.testsupport.CombatFixtures;

/**
 * DoD 6 della SPEC cronaca-duello: {@link FavoriteEstimator} è puro (nessun RNG) e sceglie il
 * favorito per somma Offensive+Defensive Rating, con spareggio su {@code maxHealth} poi
 * {@code maxStamina}; a parità totale nessun favorito netto (scontro equilibrato).
 */
class FavoriteEstimatorTest {

  private final FavoriteEstimator estimator = new FavoriteEstimator();

  @Test
  void favoritoNetto_perSommaOffensiveEDefensiveRating() {
    Fighter strong = CombatFixtures.createFighter("Forte", 30, 15, 15, 10, 5, 25, 15);
    Fighter weak = CombatFixtures.createFighter("Debole", 10, 5, 5, 5, 5, 5, 0);

    Optional<Fighter> favorite = estimator.favorite(strong, weak);

    assertTrue(favorite.isPresent());
    assertSame(strong, favorite.orElseThrow());
  }

  @Test
  void spareggio_suMaxHealthAParitaDiRatingTotale() {
    // Stesso strength+weaponAttack (offensiveRating uguale) e stessa combinazione
    // resistance/armourDefense che pareggia il defensiveRating, ma resistance piu' alta (quindi
    // maxHealth maggiore) per il primo: l'agilita' e' bilanciata per compensare il
    // defensiveRating (agilityDefenseWeight 0.5) cosi' che il totale off+def resti identico.
    Fighter higherHealth = CombatFixtures.createFighter("AltaVita", 20, 10, 10, 5, 5, 10, 0);
    Fighter lowerHealth = CombatFixtures.createFighter("BassaVita", 20, 20, 0, 5, 5, 10, 0);

    assertEqualTotalRating(higherHealth, lowerHealth);
    assertTrue(higherHealth.ratings().maxHealth() > lowerHealth.ratings().maxHealth());

    Optional<Fighter> favorite = estimator.favorite(higherHealth, lowerHealth);

    assertTrue(favorite.isPresent());
    assertSame(higherHealth, favorite.orElseThrow());
  }

  @Test
  void spareggio_suMaxStaminaAParitaDiRatingEMaxHealth() {
    // maxHealth dipende sia da resistance (peso 5) sia da stamina (peso 2): per tenerlo
    // costante spostando stamina, resistance viene compensata in proporzione (-2 ogni +5 di
    // stamina); agilita' e strength assorbono a loro volta l'effetto collaterale sul
    // defensiveRating/offensiveRating, cosi' che il totale off+def resti identico.
    Fighter higherStamina = CombatFixtures.createFighter("AltaStamina", 20, 10, 12, 10, 5, 10, 0);
    Fighter lowerStamina = CombatFixtures.createFighter("BassaStamina", 22, 6, 14, 5, 5, 10, 0);

    assertEqualTotalRating(higherStamina, lowerStamina);
    assertEqualMaxHealth(higherStamina, lowerStamina);
    assertTrue(higherStamina.ratings().maxStamina() > lowerStamina.ratings().maxStamina());

    Optional<Fighter> favorite = estimator.favorite(higherStamina, lowerStamina);

    assertTrue(favorite.isPresent());
    assertSame(higherStamina, favorite.orElseThrow());
  }

  @Test
  void scontroEquilibrato_nessunFavoritoATotaleParitaEDiPool() {
    Fighter first = CombatFixtures.createFighter("Uno", 20, 10, 10, 10, 5, 10, 0);
    Fighter second = CombatFixtures.createFighter("Due", 20, 10, 10, 10, 5, 10, 0);

    Optional<Fighter> favorite = estimator.favorite(first, second);

    assertTrue(favorite.isEmpty(), "a parita' totale di rating e pool nessuno dei due e' favorito");
  }

  private static void assertEqualTotalRating(Fighter first, Fighter second) {
    double firstTotal = first.ratings().offensiveRating() + first.ratings().defensiveRating();
    double secondTotal = second.ratings().offensiveRating() + second.ratings().defensiveRating();
    assertEquals(firstTotal, secondTotal, 0.0001, "precondizione del test: il rating totale deve coincidere");
  }

  private static void assertEqualMaxHealth(Fighter first, Fighter second) {
    assertEquals(first.ratings().maxHealth(), second.ratings().maxHealth(),
        "precondizione del test: la vita massima deve coincidere");
  }
}
