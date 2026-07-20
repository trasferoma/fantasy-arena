package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.CombatOutcome;
import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.result.TurnLogEntry;
import it.fantasyarena.combat.testsupport.CombatFixtures;
import it.fantasyarena.combat.testsupport.StubDiceRoller;

/**
 * DoD 3 (duello deterministico via stub), DoD 4 (terminazione) e DoD 9 (coerenza del
 * {@link CombatResult}) esercitati end-to-end sul {@link CombatEngine} reale con un
 * {@link StubDiceRoller} al posto del toolkit.
 */
class CombatEngineTest {

  /**
   * Sequenza truccata che garantisce sempre un colpo a segno (tiro basso, sotto qualunque
   * hitChance minima), sempre nessuna schivata/parata (tiro massimo, sopra dodge+parry) e
   * varianza neutra sul danno. I primi due tiri sono il jitter consumato da {@code CombatEngine}
   * per il primo attore: il pattern dei turni viene dopo, per non disallineare le chiamate. Il
   * colpo abbatte l'avversario al primo turno, quindi non serve alcun jitter successivo (il
   * duello finisce prima del ricalcolo di fine turno).
   */
  private static List<DiceThrow> guaranteedHitSequence(int turns) {
    List<DiceThrow> sequence = new ArrayList<>();
    sequence.add(new DiceThrow(1, 6));  // jitter iniziativa primo combattente
    sequence.add(new DiceThrow(6, 6)); // jitter iniziativa secondo combattente
    for (int i = 0; i < turns; i++) {
      sequence.add(new DiceThrow(1, 20));   // attacco: colpo garantito
      sequence.add(new DiceThrow(20, 20));  // difesa: né schivata né parata
      sequence.add(new DiceThrow(50, 100)); // varianza danno neutra
    }
    return sequence;
  }

  private static List<DiceThrow> alwaysMissSequence(int turns) {
    List<DiceThrow> sequence = new ArrayList<>();
    sequence.add(new DiceThrow(3, 6)); // jitter iniziativa primo combattente
    sequence.add(new DiceThrow(3, 6)); // jitter iniziativa secondo combattente
    for (int i = 0; i < turns; i++) {
      // 19 (non massimo naturale) con normalized 0.95 > hitChance 0.75 (agilità pari): mancato
      // garantito. Il 20 naturale ora è sempre colpo critico per SPEC, quindi non va usato qui.
      sequence.add(new DiceThrow(19, 20));
      // nessuno dei due combattenti e' mai stato colpito ne' ha subito danno: il duello non
      // termina in questo turno, quindi il Combat Engine ricalcola l'iniziativa a fine turno.
      sequence.add(new DiceThrow(3, 6)); // jitter iniziativa attaccante
      sequence.add(new DiceThrow(3, 6)); // jitter iniziativa difensore
    }
    return sequence;
  }

  private static CombatResult runDuel(List<DiceThrow> sequence) {
    return runDuel(sequence, 5);
  }

  /**
   * @param staminaCharacteristic caratteristica Stamina di entrambi i combattenti: un pool
   *     ampio evita che scattino riposi imprevisti nei turni lunghi, mantenendo allineata la
   *     sequenza di dadi truccata (un riposo salterebbe il tiro d'attacco, disallineando i
   *     tiri programmati successivi).
   */
  private static CombatResult runDuel(List<DiceThrow> sequence, int staminaCharacteristic) {
    Fighter first = CombatFixtures.createFighter("Guerriero A", 30, 10, 5, staminaCharacteristic, 5, 20, 0);
    Fighter second = CombatFixtures.createFighter("Guerriero B", 30, 10, 5, staminaCharacteristic, 5, 20, 0);
    CombatSettings settings = CombatSettings.defaults();
    StubDiceRoller diceRoller = new StubDiceRoller(sequence);
    CombatEngine engine = CombatFixtures.buildEngine(diceRoller, settings);
    return engine.fight(first, second, CombatContext.empty());
  }

  @Test
  void deterministicDuel_withStubbedDiceRoller() {
    List<DiceThrow> sequence = guaranteedHitSequence(CombatSettings.defaults().maxTurns());

    CombatResult firstRun = runDuel(sequence);
    CombatResult secondRun = runDuel(sequence);

    assertEquals(firstRun.outcome(), secondRun.outcome());
    assertEquals(firstRun.rounds(), secondRun.rounds());
    assertEquals(descriptions(firstRun), descriptions(secondRun));
  }

  private static List<String> descriptions(CombatResult result) {
    return result.log().stream().map(TurnLogEntry::description).toList();
  }

  @Test
  void everyCombatEnds_withinTurnCap() {
    CombatSettings settings = CombatSettings.defaults();
    List<DiceThrow> sequence = alwaysMissSequence(settings.maxTurns() + 5);

    // Pool di Stamina molto ampio: il consumo dell'attacco (anche mancato) non deve mai
    // costringere nessuno dei due a riposare durante l'intero test.
    CombatResult result = runDuel(sequence, 200);

    assertEquals(settings.maxTurns(), result.rounds(), "senza colpi a segno lo scontro deve fermarsi al tetto di turni");
    assertEquals(CombatOutcome.DRAW, result.outcome(), "a parità di Health invariata l'esito di timeout è un pareggio");
    assertTrue(result.winner().isEmpty());
    assertEquals(settings.maxTurns(), result.log().size());
  }

  @Test
  void combatResult_consistentWithProgress() {
    CombatResult result = runDuel(guaranteedHitSequence(CombatSettings.defaults().maxTurns()));

    assertEquals(CombatOutcome.VICTORY, result.outcome());
    assertEquals(1, result.rounds(), "il danno del primo colpo abbatte l'avversario al primo turno");
    assertEquals(result.rounds(), result.log().size(), "il numero di turni deve coincidere con le voci di log");
    assertTrue(result.winner().isPresent());
    assertFalse(result.winner().orElseThrow().isDefeated(), "il vincitore non deve essere sconfitto");
  }
}
