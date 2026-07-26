package it.fantasyarena.combat.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.IntrinsicRatings;
import it.fantasyarena.combat.result.CombatOutcome;
import it.fantasyarena.combat.result.Scorecard;
import it.fantasyarena.combat.testsupport.CombatFixtures;
import it.fantasyarena.combat.testsupport.StubDiceRoller;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;

/**
 * Esiti di {@link BattleEngine#fight}: {@code maxTurns} a 0 non gioca alcun round (permette di
 * precostituire deterministicamente lo stato dei combattenti e verificare solo
 * {@code buildResult}/{@code buildTimeoutResult}, sullo stampo di
 * {@code CombatEngineTest.runNoTurnDuel}).
 */
class BattleOutcomeTest {

  @Test
  void vittoriaDiSquadra_winningTeamCorretto() {
    Fighter a0 = CombatFixtures.createFighter("A0", 10, 10, 5, 20, 5, 5, 0);
    Fighter b0 = CombatFixtures.createFighter("B0", 10, 10, 5, 20, 5, 5, 0);
    b0.state().applyDamage(b0.ratings().maxHealth());
    BattleSetup setup = BattleSetup.duel(a0, b0);

    BattleResult result = fightWithoutRounds(setup);

    assertEquals(CombatOutcome.VICTORY, result.outcome());
    assertEquals(setup.teams().get(0), result.winningTeam().orElseThrow());
    assertTrue(result.scorecards().isEmpty(), "su VICTORY gli scorecard restano vuoti, come nel duello 1v1");
    assertTrue(result.teamScores().isEmpty(), "su VICTORY i punteggi di squadra restano vuoti, come nel duello 1v1");
  }

  @Test
  void timeout_vinceLaSquadraConTotalePiuAlto() {
    Fighter a0 = CombatFixtures.createFighter("A0", 10, 10, 5, 20, 5, 5, 0);
    Fighter b0 = CombatFixtures.createFighter("B0", 10, 10, 5, 20, 5, 5, 0);
    // A0 ha meno Salute (50%) di B0 (80%), ma ha inflitto molti piu' colpi pieni a segno: deve
    // comunque vincere ai punti (5x2=10 contro 2 (vantaggio Salute) + 1x2=2).
    a0.state().applyDamage(a0.ratings().maxHealth() / 2);
    b0.state().applyDamage((int) (b0.ratings().maxHealth() * 0.2));
    for (int i = 0; i < 5; i++) {
      a0.state().recordHitLanded();
    }
    b0.state().recordHitLanded();
    BattleSetup setup = BattleSetup.duel(a0, b0);

    BattleResult result = fightWithoutRounds(setup);

    assertEquals(CombatOutcome.TIMEOUT_DECISION, result.outcome());
    assertEquals(setup.teams().get(0), result.winningTeam().orElseThrow());
    assertEquals(List.of(new TeamScore("Squadra 1", 10), new TeamScore("Squadra 2", 4)), result.teamScores());
  }

  @Test
  void pareggio_soloATotaliDiSquadraIdentici() {
    Fighter a0 = CombatFixtures.createFighter("A0", 10, 10, 5, 20, 5, 5, 0);
    Fighter b0 = CombatFixtures.createFighter("B0", 10, 10, 5, 20, 5, 5, 0);
    // stessa Salute (100%, nessun vantaggio) e lo stesso numero di colpi a segno: totali
    // identici (1x2=2 ciascuno).
    a0.state().recordHitLanded();
    b0.state().recordHitLanded();
    BattleSetup setup = BattleSetup.duel(a0, b0);

    BattleResult result = fightWithoutRounds(setup);

    assertEquals(CombatOutcome.DRAW, result.outcome());
    assertTrue(result.winningTeam().isEmpty());
    assertEquals(List.of(new TeamScore("Squadra 1", 2), new TeamScore("Squadra 2", 2)), result.teamScores());
  }

  /**
   * Dimostra perche' il rapporto aggregato ({@code somma(currentHealth)/somma(maxHealth)}) e'
   * necessario e non sostituibile da una media dei rapporti individuali. Squadra A ha perso un
   * membro intero (A0, pool da 190 Salute, morto) ma conserva A1 vivo e a Salute piena (pool da
   * soli 10): l'aggregato e' 10/200 = 0.05. Una MEDIA dei rapporti individuali darebbe invece
   * (0/1 + 1/1) / 2 = 0.5: la Squadra A apparirebbe in vantaggio sulla Squadra B (Salute 30/100 =
   * 0.30), mascherando il fatto che alla Squadra A restano solo 10 punti Salute complessivi su
   * 200, contro i 30 su 100 della Squadra B. L'aggregato penalizza correttamente la Squadra A.
   */
  @Test
  void rapportoAggregato_penalizzaSquadraCheHaPersoUnMembro_mediaLoMascererebbe() {
    Fighter a0 = buildFighterWithMaxHealth("A0", 190);
    a0.state().applyDamage(190);
    Fighter a1 = buildFighterWithMaxHealth("A1", 10);
    Fighter b0 = buildFighterWithMaxHealth("B0", 100);
    b0.state().applyDamage(70);
    BattleSetup setup = BattleSetup.of(List.of(List.of(a0, a1), List.of(b0)));

    BattleResult result = fightWithoutRounds(setup);

    assertEquals(CombatOutcome.TIMEOUT_DECISION, result.outcome());
    assertEquals(setup.teams().get(1), result.winningTeam().orElseThrow(),
        "la Squadra B deve vincere: il suo rapporto aggregato (0.30) supera quello della Squadra A (0.05)");
    assertEquals(List.of(new TeamScore("Squadra 1", 0), new TeamScore("Squadra 2", 2)), result.teamScores());

    Scorecard a0Scorecard = scorecardOf(result, "A0");
    Scorecard a1Scorecard = scorecardOf(result, "A1");
    Scorecard b0Scorecard = scorecardOf(result, "B0");
    assertEquals(10.0 / 200.0, a0Scorecard.healthRatio(), 1e-9, "il rapporto di A0 e' quello aggregato di squadra, non il suo 0/190");
    assertEquals(10.0 / 200.0, a1Scorecard.healthRatio(), 1e-9, "il rapporto di A1 e' lo stesso aggregato di squadra, non il suo 10/10");
    assertEquals(30.0 / 100.0, b0Scorecard.healthRatio(), 1e-9);
    assertEquals(0, a0Scorecard.healthPoints());
    assertEquals(0, a1Scorecard.healthPoints());
    assertEquals(2, b0Scorecard.healthPoints());
  }

  private static Scorecard scorecardOf(BattleResult result, String fighterName) {
    for (Scorecard scorecard : result.scorecards()) {
      if (scorecard.fighterName().equals(fighterName)) {
        return scorecard;
      }
    }
    throw new IllegalStateException("nessuno scorecard trovato per: " + fighterName);
  }

  private static Fighter buildFighterWithMaxHealth(String name, int maxHealth) {
    CharacterResult character = CombatFixtures.createWarrior(name, 10, 10, 10, 10, 10);
    WeaponResult weapon = CombatFixtures.createSword(0);
    ArmourResult armour = CombatFixtures.createChestplate(0);
    IntrinsicRatings ratings = new IntrinsicRatings(10.0, 10.0, maxHealth, 100);
    return new Fighter(character, weapon, armour, null, ratings);
  }

  private static BattleResult fightWithoutRounds(BattleSetup setup) {
    CombatSettings settings = withMaxTurns(0);
    StubDiceRoller diceRoller = new StubDiceRoller(List.of());
    BattleEngine battleEngine = CombatFixtures.buildBattleEngine(diceRoller, settings);
    return battleEngine.fight(setup, CombatContext.empty());
  }

  private static CombatSettings withMaxTurns(int maxTurns) {
    CombatSettings defaults = CombatSettings.defaults();
    return new CombatSettings(defaults.ratingWeights(), defaults.momentumWeights(), defaults.staminaWeights(),
        defaults.chanceWeights(), defaults.initiativeWeights(), defaults.chronicleWeights(),
        defaults.powerStrikeWeights(), defaults.scoreWeights(), maxTurns);
  }
}
