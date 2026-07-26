package it.fantasyarena.combat.battle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.testsupport.CombatFixtures;
import it.fantasyarena.combat.testsupport.RecordingStubDiceRoller;

/**
 * Il recupero passivo di Stamina e' diviso in due responsabilita' (vedi il Javadoc di
 * {@link BattleEngine}): la scatola recupera il bersaglio di OGNI scambio, il motore recupera a
 * fine round chi non e' stato ne' attore ne' bersaglio (gli "inattivi"). In un 2v1 il bersaglio
 * (dalla scatola) e l'alleato inattivo (dal motore) devono recuperare, l'attore no. In un 1v1
 * l'insieme degli inattivi e' sempre vuoto: recupera solo il difensore, esattamente come oggi, e
 * il motore non aggiunge nulla (nessun doppio recupero).
 */
class BattlePassiveRecoveryTest {

  private static final int STAMINA_CHARACTERISTIC = 15;
  private static final int PRE_CONSUMED_STAMINA = 10;

  @Test
  void unoControUno_recuperaSoloIlDifensore_nessunContributoDalMotore() {
    CombatSettings settings = CombatFixtures.withPowerStrikeUnaffordable(withMaxTurns(1));

    Fighter a0 = buildFighterWithConsumedStamina("A0");
    Fighter b0 = buildFighterWithConsumedStamina("B0");
    BattleSetup setup = BattleSetup.duel(a0, b0);

    // Test a punteggio 1v1 (A0 vince per jitter), colpo mancato: nessun costo di difesa pagato
    // da B0, cosi' il delta finale di B0 riflette SOLO il recupero passivo, non altre variazioni.
    List<DiceThrow> sequence = List.of(
        new DiceThrow(6, 6), new DiceThrow(1, 6),
        new DiceThrow(19, 20));
    RecordingStubDiceRoller diceRoller = new RecordingStubDiceRoller(sequence);
    BattleEngine battleEngine = CombatFixtures.buildBattleEngine(diceRoller, settings);

    battleEngine.fight(setup, CombatContext.empty());

    int expectedActorStamina = fullStaminaMinusConsumed() - settings.staminaWeights().attackCost();
    int expectedDefenderStamina = fullStaminaMinusConsumed() + settings.staminaWeights().passiveRecovery();
    assertEquals(expectedActorStamina, a0.state().currentStamina(), "l'attore consuma il costo d'attacco, non recupera nulla");
    assertEquals(expectedDefenderStamina, b0.state().currentStamina(),
        "il difensore recupera esattamente una volta (dalla scatola): nessun doppio recupero dal motore");
  }

  @Test
  void dueControUno_recuperaBersaglioEAlleatoInattivo_nonLAttore() {
    CombatSettings settings = CombatFixtures.withPowerStrikeUnaffordable(withMaxTurns(1));

    Fighter a0 = buildFighterWithConsumedStamina("A0");
    Fighter a1 = buildFighterWithConsumedStamina("A1");
    Fighter b0 = buildFighterWithConsumedStamina("B0");
    BattleSetup setup = BattleSetup.of(List.of(List.of(a0, a1), List.of(b0)));

    // Test a punteggio fra i tre (A0 vince nettamente per jitter), colpo mancato su B0.
    List<DiceThrow> sequence = List.of(
        new DiceThrow(6, 6), new DiceThrow(1, 6), new DiceThrow(1, 6),
        new DiceThrow(19, 20));
    RecordingStubDiceRoller diceRoller = new RecordingStubDiceRoller(sequence);
    BattleEngine battleEngine = CombatFixtures.buildBattleEngine(diceRoller, settings);

    battleEngine.fight(setup, CombatContext.empty());

    int expectedActorStamina = fullStaminaMinusConsumed() - settings.staminaWeights().attackCost();
    int expectedRecoveredStamina = fullStaminaMinusConsumed() + settings.staminaWeights().passiveRecovery();
    assertEquals(expectedActorStamina, a0.state().currentStamina(), "l'attore (A0) consuma il costo d'attacco, non recupera nulla");
    assertEquals(expectedRecoveredStamina, b0.state().currentStamina(),
        "il bersaglio (B0) recupera dalla scatola, come nel duello 1v1");
    assertEquals(expectedRecoveredStamina, a1.state().currentStamina(),
        "l'alleato inattivo (A1), ne' attore ne' bersaglio, recupera dal motore a fine round");
  }

  private static Fighter buildFighterWithConsumedStamina(String name) {
    Fighter fighter = CombatFixtures.createFighter(name, 30, 10, 5, STAMINA_CHARACTERISTIC, 5, 20, 0);
    fighter.state().consumeStamina(PRE_CONSUMED_STAMINA);
    return fighter;
  }

  private static int fullStaminaMinusConsumed() {
    // maxStaminaBase(10) + maxStaminaPerStamina(3) * STAMINA_CHARACTERISTIC, con i pesi di default.
    int maxStamina = CombatSettings.defaults().ratingWeights().maxStaminaBase()
        + CombatSettings.defaults().ratingWeights().maxStaminaPerStamina() * STAMINA_CHARACTERISTIC;
    return maxStamina - PRE_CONSUMED_STAMINA;
  }

  private static CombatSettings withMaxTurns(int maxTurns) {
    CombatSettings defaults = CombatSettings.defaults();
    return new CombatSettings(defaults.ratingWeights(), defaults.momentumWeights(), defaults.staminaWeights(),
        defaults.chanceWeights(), defaults.initiativeWeights(), defaults.chronicleWeights(),
        defaults.powerStrikeWeights(), defaults.scoreWeights(), maxTurns);
  }
}
