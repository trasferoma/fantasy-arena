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
import it.fantasyarena.combat.result.TurnHighlight;
import it.fantasyarena.combat.result.TurnLogEntry;
import it.fantasyarena.combat.testsupport.CombatFixtures;
import it.fantasyarena.combat.testsupport.StubDiceRoller;

/**
 * DoD 3, 4, 6 e 9 della SPEC colpo-potente, esercitati sul {@link TurnOrchestrator} reale con i
 * dadi pilotati da {@link StubDiceRoller}: il jitter di decisione si consuma SOLO quando il
 * colpo potente e' pagabile, il colpo potente scelto raddoppia il costo Stamina, un colpo
 * potente a segno emette l'highlight {@code POWER_STRIKE}, un colpo potente mancato non emette
 * highlight ma produce una descrizione dedicata, e quando non e' scelto (declinato o non
 * pagabile) gli effetti restano identici a oggi.
 */
class TurnOrchestratorPowerStrikeTest {

  @Test
  void nonPagabile_nessunColpoPotenteNessunJitter() {
    CombatSettings settings = CombatSettings.defaults();
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 10, 5, 5, 5, 5, 5, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 10, 5, 10, 5, 5, 5, 5);
    attacker.state().winInitiative();
    defender.state().consumeStamina(defender.ratings().maxStamina());

    // attackCost effettivo 6, powerCost 12: a Stamina 11 l'attacco base resta pagabile ma il
    // colpo potente no (e la soglia di riposo, 11, e' comunque rispettata).
    attacker.state().consumeStamina(attacker.ratings().maxStamina() - 11);
    assertEquals(11, attacker.state().currentStamina(), "precondizione: Stamina pagabile per l'attacco, non per il potente");

    // nessun jitter programmato: se il codice ne tirasse uno per errore, lo stub solleverebbe
    // un'eccezione per sequenza esaurita, oppure disallineerebbe l'attacco col tiro di varianza.
    List<DiceThrow> scriptedThrows = List.of(new DiceThrow(1, 20), new DiceThrow(50, 100));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnLogEntry entry = playSingleTurn(diceRoller, settings, attacker, defender);

    assertEquals(5, attacker.state().currentStamina(), "solo il costo base (6) deve essere consumato, non il doppio");
    assertFalse(entry.highlights().contains(TurnHighlight.POWER_STRIKE), "il colpo potente non pagabile non va tentato");
    assertFalse(entry.description().contains("potente"), "senza colpo potente la descrizione resta quella di sempre");
  }

  @Test
  void colpoPotenteScelto_consumaDoppiaStamina_emettePowerStrikeASegno() {
    CombatSettings settings = CombatSettings.defaults();
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 10, 5, 5, 5, 5, 5, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 10, 5, 10, 5, 5, 5, 5);
    attacker.state().winInitiative();
    defender.state().consumeStamina(defender.ratings().maxStamina());

    // Stamina e vita piene: il PowerStrikeResolver sceglie il colpo potente qualunque sia il
    // jitter (parte razionale gia' sopra soglia). Tiro d'attacco a segno ma non critico ne'
    // massimo naturale (isola l'highlight del colpo potente).
    List<DiceThrow> scriptedThrows =
        List.of(new DiceThrow(3, 6), new DiceThrow(15, 20), new DiceThrow(50, 100));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnLogEntry entry = playSingleTurn(diceRoller, settings, attacker, defender);

    assertEquals(13, attacker.state().currentStamina(),
        "il colpo potente consuma il doppio del costo base (25 - 2*6 = 13)");
    assertTrue(entry.highlights().contains(TurnHighlight.POWER_STRIKE), "il colpo potente a segno deve emettere l'highlight");
    assertTrue(entry.description().contains("potente"), "la descrizione deve citare il colpo potente");
  }

  @Test
  void colpoPotenteMancato_nessunHighlight_descrizioneDedicata() {
    CombatSettings settings = CombatSettings.defaults();
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 10, 5, 5, 5, 5, 5, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 10, 5, 10, 5, 5, 5, 5);
    attacker.state().winInitiative();

    // Stamina e vita piene: colpo potente scelto. Tiro d'attacco 19/20 con agilita' pari:
    // normalized 0.95 > hitChance 0.75, mancato garantito.
    List<DiceThrow> scriptedThrows = List.of(new DiceThrow(3, 6), new DiceThrow(19, 20));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnLogEntry entry = playSingleTurn(diceRoller, settings, attacker, defender);

    assertEquals(13, attacker.state().currentStamina(),
        "il doppio costo si consuma anche se il colpo potente manca: e' il rischio esplicito");
    assertTrue(entry.highlights().isEmpty(), "un colpo potente mancato non emette alcun highlight");
    assertEquals("Attaccante tenta un colpo potente su Difensore ma manca il colpo.", entry.description());
  }

  @Test
  void colpoPotenteDeclinato_effettiIdenticiAOggi() {
    CombatSettings settings = CombatSettings.defaults();
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 10, 5, 5, 5, 5, 5, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 10, 5, 10, 5, 5, 5, 5);
    attacker.state().winInitiative();
    defender.state().consumeStamina(defender.ratings().maxStamina());

    // Vita molto bassa (sotto il 20% del massimo) con Stamina piena: pagabile ma il resolver
    // declina, anche col jitter minimo.
    attacker.state().applyDamage(attacker.ratings().maxHealth() - 6);

    List<DiceThrow> scriptedThrows =
        List.of(new DiceThrow(1, 6), new DiceThrow(1, 20), new DiceThrow(50, 100));
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnLogEntry entry = playSingleTurn(diceRoller, settings, attacker, defender);

    assertEquals(19, attacker.state().currentStamina(), "declinato: si consuma solo il costo base (25 - 6 = 19)");
    assertFalse(entry.highlights().contains(TurnHighlight.POWER_STRIKE), "declinato: nessun highlight di colpo potente");
    assertFalse(entry.description().contains("potente"), "declinato: descrizione identica a un colpo pieno di sempre");
  }

  /**
   * DoD 11 — dopo un colpo potente eseguito, il cooldown (4 turni d'azione con i default)
   * impedisce di rieseguirlo anche se pagabile, e il jitter di decisione NON viene tirato
   * mentre il cooldown e' attivo: la sequenza {@link StubDiceRoller} contiene esattamente un
   * dado di jitter per il turno di esecuzione e uno per il turno di ripristino, nessuno per i
   * quattro turni intermedi. Se il codice tentasse un jitter di troppo, i tiri d'attacco si
   * disallineerebbero (o lo stub esaurirebbe la sequenza), facendo fallire le asserzioni sotto.
   */
  @Test
  void cooldown_impedisceLaRipetizioneFinoAllaScadenza_poiTornaDisponibile() {
    CombatSettings settings = CombatSettings.defaults();
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 10, 5, 5, 30, 5, 5, 0);
    Fighter defender = CombatFixtures.createFighter("Difensore", 10, 5, 5, 30, 5, 5, 0);
    attacker.state().winInitiative();

    // Stamina/vita piene: la parte razionale della decisione supera da sola la soglia (0.6),
    // qualunque sia il jitter. Il tiro d'attacco 19/20 con agilita' pari e' un mancato garantito
    // (nessun tiro di difesa/varianza necessario): isola il cooldown dal resto della risoluzione.
    List<DiceThrow> scriptedThrows = List.of(
        new DiceThrow(1, 6), new DiceThrow(19, 20),  // turno 1: jitter + attacco, colpo potente tentato
        new DiceThrow(19, 20),                       // turno 2: cooldown attivo, nessun jitter
        new DiceThrow(19, 20),                       // turno 3: cooldown attivo, nessun jitter
        new DiceThrow(19, 20),                       // turno 4: cooldown attivo, nessun jitter
        new DiceThrow(19, 20),                       // turno 5: cooldown attivo, nessun jitter
        new DiceThrow(1, 6), new DiceThrow(19, 20)); // turno 6: cooldown scaduto, jitter + attacco
    StubDiceRoller diceRoller = new StubDiceRoller(scriptedThrows);

    TurnLogEntry turn1 = playSingleTurn(diceRoller, settings, attacker, defender);
    assertEquals(88, attacker.state().currentStamina(), "turno 1: colpo potente eseguito, doppio costo (100 - 12)");
    assertTrue(turn1.description().contains("potente"), "turno 1: colpo potente tentato");
    assertFalse(attacker.state().powerStrikeReady(), "turno 1: il cooldown si avvia subito dopo l'esecuzione");

    TurnLogEntry turn2 = playSingleTurn(diceRoller, settings, attacker, defender);
    assertEquals(82, attacker.state().currentStamina(), "turno 2: cooldown attivo, solo costo base (88 - 6)");
    assertFalse(turn2.description().contains("potente"), "turno 2: colpo potente non ritentabile durante il cooldown");

    TurnLogEntry turn3 = playSingleTurn(diceRoller, settings, attacker, defender);
    assertEquals(76, attacker.state().currentStamina(), "turno 3: cooldown ancora attivo (82 - 6)");
    assertFalse(turn3.description().contains("potente"));

    TurnLogEntry turn4 = playSingleTurn(diceRoller, settings, attacker, defender);
    assertEquals(70, attacker.state().currentStamina(), "turno 4: cooldown ancora attivo (76 - 6)");
    assertFalse(turn4.description().contains("potente"));

    TurnLogEntry turn5 = playSingleTurn(diceRoller, settings, attacker, defender);
    assertEquals(64, attacker.state().currentStamina(), "turno 5: quarto e ultimo turno di cooldown (70 - 6)");
    assertFalse(turn5.description().contains("potente"));
    assertTrue(attacker.state().powerStrikeReady(), "dopo il quarto turno di cooldown il colpo potente torna pronto");

    TurnLogEntry turn6 = playSingleTurn(diceRoller, settings, attacker, defender);
    assertEquals(52, attacker.state().currentStamina(), "turno 6: cooldown scaduto, colpo potente di nuovo eseguito (64 - 12)");
    assertTrue(turn6.description().contains("potente"), "turno 6: il colpo potente e' di nuovo disponibile al quinto turno successivo");
  }

  private static TurnLogEntry playSingleTurn(StubDiceRoller diceRoller, CombatSettings settings, Fighter attacker,
      Fighter defender) {
    TurnOrchestrator turnOrchestrator = new TurnOrchestrator(diceRoller, new HitResolver(settings),
        new DefenseResolver(settings), new DamageCalculator(settings, new MomentumRules(settings),
            new StaminaRules(settings)), new MomentumRules(settings), new StaminaRules(settings), settings);
    return turnOrchestrator.playTurn(1, attacker, defender, CombatContext.empty()).logEntry();
  }
}
