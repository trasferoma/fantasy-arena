package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.InitiativeOverride;
import it.fantasyarena.combat.result.TurnHighlight;
import it.fantasyarena.combat.result.TurnResult;
import it.fantasyarena.combat.testsupport.CombatFixtures;
import it.fantasyarena.combat.testsupport.StubDiceRoller;

/**
 * DoD 5 (recupero passivo che non annulla i costi di difesa), DoD 7 (ripiego
 * schivata -&gt; parata -&gt; colpo pieno quando i costi non sono pagabili) e DoD 9 (solo la
 * schivata effettiva ruba il tempo, la parata no), esercitati sul {@link TurnOrchestrator}
 * reale.
 */
class TurnOrchestratorDefenseTest {

  @Test
  void successfulDefense_reducesDamageAndUpdatesMomentum() {
    CombatSettings settings = CombatSettings.defaults();
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 30, 10, 5, 5, 5, 20, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 30, 10, 5, 5, 5, 20, 0);
    attacker.state().winInitiative();

    // attacco garantito a segno, difesa garantita in schivata (dodgeChance di base è 0.10),
    // varianza neutra: la sequenza forza deterministicamente l'esito DODGED.
    List<DiceThrow> scriptedThrows =
        List.of(new DiceThrow(1, 20), new DiceThrow(1, 20), new DiceThrow(50, 100));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnResult turn = playSingleTurn(diceRoller, settings, attacker, defender);

    assertEquals(defender.ratings().maxHealth(), defender.state().currentHealth(),
        "una schivata piena deve azzerare il danno subito");
    assertEquals(settings.momentumWeights().gainOnDodgeSuccess(), defender.state().momentum(),
        "la schivata riuscita deve aumentare il Momentum del difensore secondo le regole");
    assertEquals(defender.ratings().maxStamina() - settings.staminaWeights().dodgeCost() + settings.staminaWeights().passiveRecovery(),
        defender.state().currentStamina(),
        "la schivata consuma il costo Stamina della schivata; il recupero passivo di fine turno "
            + "non lo annulla, si somma sopra (DoD 5)");
    assertEquals(attacker.ratings().maxStamina() - settings.staminaWeights().attackCost(), attacker.state().currentStamina(),
        "l'attacco consuma il costo Stamina dell'azione di attacco");
    assertEquals(1, turn.logEntry().turnNumber());
    assertEquals(InitiativeOverride.DODGE_STEAL, turn.override(),
        "una schivata effettiva ruba il tempo: l'override va propagato");
  }

  @Test
  void fullHitTaken_consumesStaminaProportionallyToDamage() {
    CombatSettings settings = CombatSettings.defaults();
    // Rating offensivi/difensivi contenuti apposta: il tiro d'attacco (1,20) genera sempre un
    // critico (critChance minima 0.05 = normalized di 1/20), qui il danno risultante resta
    // comunque ben sotto Salute e Stamina massime del difensore, cosi' l'assert puo' derivare
    // il danno reale dal delta di vita senza incappare nel floor a 0 di nessuno dei due pool.
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 10, 5, 5, 5, 5, 5, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 10, 5, 10, 5, 5, 5, 5);
    attacker.state().winInitiative();

    // attacco garantito a segno, difesa che non schiva ne' para (dodge/parry chance basse a
    // fronte di un tiro alto), varianza neutra: la sequenza forza l'esito HIT_TAKEN.
    List<DiceThrow> scriptedThrows =
        List.of(new DiceThrow(1, 20), new DiceThrow(20, 20), new DiceThrow(50, 100));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnResult turn = playSingleTurn(diceRoller, settings, attacker, defender);

    assertTrue(defender.state().currentHealth() < defender.ratings().maxHealth(),
        "precondizione: il difensore deve aver subito danno");
    assertEquals(InitiativeOverride.NONE, turn.override(), "un colpo pieno non ruba il tempo");

    StaminaRules staminaRules = new StaminaRules(settings);
    int damage = defender.ratings().maxHealth() - defender.state().currentHealth();
    int expectedStaminaLoss = staminaRules.impactStaminaLoss(damage);
    int expectedStaminaAfterRecovery = Math.min(defender.ratings().maxStamina(),
        defender.ratings().maxStamina() - expectedStaminaLoss + settings.staminaWeights().passiveRecovery());
    assertEquals(expectedStaminaAfterRecovery, defender.state().currentStamina(),
        "chi incassa un colpo pieno perde stamina proporzionale al danno, poi recupera passivamente (DoD 5)");
  }

  /**
   * DoD 7 — la schivata risolta dal tiro non e' pagabile (Stamina 4 &lt; dodgeCost 5), ma la
   * parata lo e' (parryCost 4): si ripiega sulla parata, che riduce il danno ma NON ruba il
   * tempo (DoD 9).
   */
  @Test
  void dodgeUnaffordable_fallsBackToParry() {
    CombatSettings settings = CombatSettings.defaults();
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 30, 10, 5, 5, 5, 20, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 30, 10, 5, 5, 5, 20, 0);
    attacker.state().winInitiative();
    defender.state().consumeStamina(defender.ratings().maxStamina() - 4);

    List<DiceThrow> scriptedThrows =
        List.of(new DiceThrow(1, 20), new DiceThrow(1, 20), new DiceThrow(50, 100));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnResult turn = playSingleTurn(diceRoller, settings, attacker, defender);

    assertEquals(InitiativeOverride.NONE, turn.override(),
        "il ripiego in parata non ruba il tempo, a differenza della schivata");
    assertTrue(turn.logEntry().description().contains("parato"), "l'esito finale deve essere una parata");
    assertTrue(defender.state().currentHealth() < defender.ratings().maxHealth(),
        "la parata riduce il danno ma non lo azzera come farebbe la schivata");
    assertEquals(settings.momentumWeights().gainOnParrySuccess(), defender.state().momentum(),
        "il Momentum riflette la parata realmente avvenuta, non la schivata originariamente tirata");
    assertEquals(settings.staminaWeights().passiveRecovery(), defender.state().currentStamina(),
        "parryCost (4) pagato dalla Stamina residua (4) porta a 0, poi il recupero passivo la riporta a 4");
  }

  /**
   * DoD 7 — ne' la schivata ne' la parata sono pagabili: nonostante il tiro risolva una
   * schivata, il difensore subisce il colpo pieno senza rubare il tempo.
   */
  @Test
  void neitherDodgeNorParryAffordable_takesFullHit() {
    CombatSettings settings = CombatSettings.defaults();
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 30, 10, 5, 5, 5, 20, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 30, 10, 5, 5, 5, 20, 0);
    attacker.state().winInitiative();
    defender.state().consumeStamina(defender.ratings().maxStamina() - 3);

    List<DiceThrow> scriptedThrows =
        List.of(new DiceThrow(1, 20), new DiceThrow(1, 20), new DiceThrow(50, 100));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnResult turn = playSingleTurn(diceRoller, settings, attacker, defender);

    assertEquals(InitiativeOverride.NONE, turn.override(), "senza Stamina per nessuna difesa il tempo non viene rubato");
    // il tiro d'attacco (1,20) e' anche critico: la cronaca arricchisce la coda in "colpo
    // critico a segno" (DoD 5 della SPEC cronaca-duello), quindi l'assert cerca "a segno" e non
    // piu' la stringa piatta "colpo a segno".
    assertTrue(turn.logEntry().description().contains("a segno"), "l'esito finale deve essere un colpo pieno");
    assertTrue(turn.logEntry().highlights().contains(TurnHighlight.CRITICAL),
        "il tiro d'attacco (1,20) e' anche critico per questo attaccante");
    assertFalse(turn.logEntry().description().contains("esausto"),
        "il difensore poteva ancora tentare la difesa (Stamina > 0): non e' il caso dell'esausto");

    int expectedMomentum = settings.momentumWeights().lossOnHitTaken() + settings.momentumWeights().lossOnCriticalTaken();
    assertEquals(expectedMomentum, defender.state().momentum(),
        "un colpo pieno (critico, tiro d'attacco 1/20) applica le due perdite di Momentum combinate");
  }

  private static TurnResult playSingleTurn(StubDiceRoller diceRoller, CombatSettings settings, Fighter attacker,
      Fighter defender) {
    TurnOrchestrator turnOrchestrator = new TurnOrchestrator(diceRoller, new HitResolver(settings),
        new DefenseResolver(settings), new DamageCalculator(settings, new MomentumRules(settings),
            new StaminaRules(settings)), new MomentumRules(settings), new StaminaRules(settings), settings);
    return turnOrchestrator.playTurn(1, attacker, defender, CombatContext.empty());
  }
}
