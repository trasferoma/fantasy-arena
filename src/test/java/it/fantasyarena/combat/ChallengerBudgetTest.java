package it.fantasyarena.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.factory.FighterFactory;
import it.fantasyarena.combat.hero.Hero;
import it.fantasycombatsystem.testsupport.CombatFixtures;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.buffdebuffgenerator.result.BuffElement;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.jewelgenerator.result.JewelResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.Characteristic;
import it.fantasytoolkitcore.core.model.Jewel;
import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.Weapon;

/**
 * Il calcolo del monte punti scontato dalla fortuna: la fortuna letta è quella effettiva del
 * protagonista, coi buff dell'equipaggiamento addosso, e lo sconto resta sempre entro il pavimento
 * e non scende mai sotto zero.
 */
class ChallengerBudgetTest {

  private static final int CHALLENGER_COUNT = 3;
  private static final int STATION_POINTS = 39;

  @Test
  void laFortunaEffettivaScontaIlMonteInProporzioneAlNumeroDiSfidanti() {
    Hero hero = heroWithLuck(5);

    ChallengerBudget budget = ChallengerBudget.of(STATION_POINTS, hero, CHALLENGER_COUNT);

    assertEquals(STATION_POINTS, budget.stationPoints());
    assertEquals(5 * CHALLENGER_COUNT, budget.luckDiscount());
    assertEquals(STATION_POINTS - 5 * CHALLENGER_COUNT, budget.squadPoints());
  }

  /**
   * Con la fortuna effettiva, un gioiello che porti molta {@code LUCK} può da solo schiacciare lo
   * schieramento fino al pavimento: qui è comportamento ordinario, non un caso limite inventato,
   * perché la fortuna arriva da un vero pezzo d'equipaggiamento indossato.
   */
  @Test
  void unGioielloGenerosoDiFortunaSchiacciaLoScontoFinoAlPavimentoEQuelloRegistratoEQuelloApplicato() {
    int count = 2;
    int stationPoints = 31;
    int floor = FighterFactory.MINIMUM_CHARACTERISTIC_POINTS_PER_CHALLENGER * count;
    Hero hero = heroWithLuckyJewel(10, 50);

    ChallengerBudget budget = ChallengerBudget.of(stationPoints, hero, count);

    assertEquals(floor, budget.squadPoints(), "il pavimento tiene anche con una fortuna molto alta");
    assertEquals(stationPoints - floor, budget.luckDiscount(),
        "lo sconto registrato è quello effettivamente applicato, non quello teorico richiesto dalla fortuna");
  }

  @Test
  void loScontoNonScendeMaiSottoZero() {
    int count = 1;
    int floor = FighterFactory.MINIMUM_CHARACTERISTIC_POINTS_PER_CHALLENGER * count;
    Hero hero = heroWithLuck(9);

    ChallengerBudget budget = ChallengerBudget.of(floor, hero, count);

    assertEquals(0, budget.luckDiscount(), "il monte è già al pavimento: non c'è margine per scontare");
    assertEquals(floor, budget.squadPoints());
  }

  @Test
  void rifiutaUnEroeNulloOUnaNumerositaMinoreDiUno() {
    Hero hero = heroWithLuck(5);

    assertThrows(IllegalArgumentException.class, () -> ChallengerBudget.of(STATION_POINTS, null, CHALLENGER_COUNT));
    assertThrows(IllegalArgumentException.class, () -> ChallengerBudget.of(STATION_POINTS, hero, 0));
  }

  private Hero heroWithLuck(int luck) {
    CharacterResult character = CombatFixtures.createWarrior("Protagonista", 10, 11, 12, 13, luck);
    WeaponResult weapon = new WeaponResult(Weapon.SWORD, Rarity.COMMON, List.of(), List.of(), 5);
    ArmourResult armour = new ArmourResult(Armour.CHESTPLATE, Rarity.COMMON, List.of(), List.of(), 4);
    return new Hero(character, weapon, List.of(armour));
  }

  private Hero heroWithLuckyJewel(int baseLuck, int jewelLuckBonus) {
    Hero hero = heroWithLuck(baseLuck);
    JewelResult luckyJewel = new JewelResult(Jewel.RING, Rarity.LEGENDARY,
        List.of(new BuffElement(Characteristic.LUCK, jewelLuckBonus)), List.of());
    return hero.wearing(luckyJewel);
  }
}
