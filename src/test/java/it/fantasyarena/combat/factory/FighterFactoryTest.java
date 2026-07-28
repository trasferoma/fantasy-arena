package it.fantasyarena.combat.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.hero.Loot;
import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.model.Fighter;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.jewelgenerator.result.JewelResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Rarity;

/**
 * Verifica {@link FighterFactory#createChallengers(int)} (numerosità richiesta, nomi tutti
 * distinti, rarità di arma/armatura condivise da tutti, cosi' che nessuno parta avvantaggiato) e
 * {@link FighterFactory#rollLoot(Rarity)} (rarità mai sotto la soglia, tutti i tipi estraibili su
 * più generazioni).
 */
class FighterFactoryTest {

  private static final int MANY_ROLLS = 200;

  private final FighterFactory factory = FighterFactory.withDefaultRatings(CombatSettings.defaults());

  @Test
  void creaCinqueCombattentiConCinqueNomiDistintiEStessaRarita() {
    List<Fighter> fighters = factory.createChallengers(5);

    assertEquals(5, fighters.size());

    Set<String> names = fighters.stream().map(Fighter::name).collect(Collectors.toCollection(HashSet::new));
    assertEquals(5, names.size(), "i 5 combattenti devono avere 5 nomi tutti distinti");

    Set<String> weaponRarities = fighters.stream().map(fighter -> fighter.weapon().rarity().name())
        .collect(Collectors.toCollection(HashSet::new));
    Set<String> armourRarities = fighters.stream()
        .flatMap(fighter -> fighter.armourPieces().stream())
        .map(piece -> piece.rarity().name())
        .collect(Collectors.toCollection(HashSet::new));
    assertEquals(1, weaponRarities.size(), "l'arma deve avere la stessa rarità per tutti");
    assertEquals(1, armourRarities.size(), "l'armatura deve avere la stessa rarità per tutti");
  }

  @Test
  void rifiutaUnaNumerositaMinoreDiUno() {
    assertThrows(IllegalArgumentException.class, () -> factory.createChallengers(0));
  }

  @Test
  void ilLootNonScendeMaiSottoLaSogliaDiRarita() {
    for (Rarity floor : Rarity.values()) {
      IntStream.range(0, MANY_ROLLS)
          .mapToObj(roll -> factory.rollLoot(floor))
          .forEach(loot -> assertTrue(rarityOf(loot).ordinal() >= floor.ordinal(),
              "il loot non deve mai scendere sotto " + floor));
    }
  }

  @Test
  void suMolteGenerazioniCompaionoTuttiITipiDiLoot() {
    Set<String> kindsFound = new HashSet<>();

    for (int roll = 0; roll < MANY_ROLLS; roll++) {
      Loot loot = factory.rollLoot(Rarity.COMMON);
      loot.weapon().ifPresent(found -> kindsFound.add("WEAPON"));
      loot.armourPiece().ifPresent(found -> kindsFound.add("ARMOUR"));
      loot.jewel().ifPresent(found -> kindsFound.add("JEWEL"));
    }

    assertEquals(Set.of("WEAPON", "ARMOUR", "JEWEL"), kindsFound,
        "su " + MANY_ROLLS + " estrazioni devono comparire tutti e tre i tipi");
  }

  private Rarity rarityOf(Loot loot) {
    return loot.weapon().map(WeaponResult::rarity)
        .or(() -> loot.armourPiece().map(ArmourResult::rarity))
        .or(() -> loot.jewel().map(JewelResult::rarity))
        .orElseThrow();
  }
}
