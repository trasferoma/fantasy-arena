package it.fantasyarena.combat.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.FighterVitals;
import it.fantasyarena.combat.result.InitiativeBreakdown;
import it.fantasyarena.combat.result.InitiativeOverride;
import it.fantasyarena.combat.result.InitiativeReport;
import it.fantasyarena.combat.result.TurnLogEntry;
import it.fantasyarena.combat.testsupport.CombatFixtures;

/**
 * Verifica la matematica delle barre ({@link CombatScreenRenderer#filledCells}), il segno di
 * variazione ({@link CombatScreenRenderer#changeSign}) e il render di pagina a tre colonne:
 * composizione dei pannelli (colonna 1), semantica "stato dopo il turno rivelato", marcatore di
 * iniziativa, segno +/- sui valori numerici, colonna 2 limitata al solo turno corrente (non il
 * log cumulativo) e colonna 3 con le schede dei due combattenti.
 */
class CombatScreenRendererTest {

  @Test
  void filledCellsCalcolaLeCelleRiempiteArrotondandoAllUnitaPiuVicina() {
    assertEquals(8, CombatScreenRenderer.filledCells(40, 50, 10));
    assertEquals(0, CombatScreenRenderer.filledCells(0, 50, 10));
    assertEquals(10, CombatScreenRenderer.filledCells(50, 50, 10));
    assertEquals(0, CombatScreenRenderer.filledCells(1, 50, 10));
  }

  @Test
  void filledCellsRestituisceZeroConMaxNonPositivo() {
    assertEquals(0, CombatScreenRenderer.filledCells(5, 0, 10));
    assertEquals(0, CombatScreenRenderer.filledCells(5, -1, 10));
  }

  @Test
  void changeSignRestituisceIlSegnoDellaVariazione() {
    assertEquals("+", CombatScreenRenderer.changeSign(5, 3));
    assertEquals("-", CombatScreenRenderer.changeSign(3, 5));
    assertEquals(" ", CombatScreenRenderer.changeSign(4, 4));
  }

  @Test
  void renderDelPrimoTurnoMostraSoloIlTurnoCorrenteEIVitalsSuccessivi() {
    CombatScreenRenderer renderer =
        new CombatScreenRenderer(aliceFighter(), bobFighter(), buildLog(), buildFinalVitals());

    String page = renderer.render(0);

    assertTrue(page.contains("Alice"));
    assertTrue(page.contains("Bob"));
    assertTrue(page.contains("=== Duello — turno 1 / 3 ==="));
    assertTrue(page.contains("(INVIO per avanzare — turno 1/3)"));
    assertTrue(page.contains("Alice attacca Bob"));
    assertFalse(page.contains("Bob para il colpo"));
    assertFalse(page.contains("Alice manca il colpo"));
    // Stato dopo il turno 1 = vitals del turno 2 in log.
    assertTrue(page.contains("40/50"));
    assertTrue(page.contains("44/50"));
  }

  @Test
  void renderDellUltimoTurnoMostraSoloIlTurnoCorrenteRivelatoEIVitalsFinali() {
    CombatScreenRenderer renderer =
        new CombatScreenRenderer(aliceFighter(), bobFighter(), buildLog(), buildFinalVitals());

    String page = renderer.render(2);

    assertTrue(page.contains("=== Duello — turno 3 / 3 ==="));
    assertTrue(page.contains("(INVIO per avanzare — turno 3/3)"));
    // Colonna 2: solo il turno corrente (3), non il log cumulativo dei turni precedenti.
    assertTrue(page.contains("Alice manca il colpo"));
    assertFalse(page.contains("Alice attacca Bob"));
    assertFalse(page.contains("Bob para il colpo"));
    // Nessun turno successivo: si mostrano i vitals finali.
    assertTrue(page.contains("35/50"));
    assertTrue(page.contains("40/50"));
  }

  @Test
  void renderColonnaCentraleEConcisaSenzaDettagliEliminati() {
    TurnLogEntry entry = new TurnLogEntry(1, "Alice attacca Bob e lo colpisce")
        .withVitals(List.of(
            new FighterVitals("Alice", 40, 50, 20, 40),
            new FighterVitals("Bob", 44, 50, 25, 40)))
        .withInitiative(buildInitiative("Alice"));
    CombatScreenRenderer renderer =
        new CombatScreenRenderer(aliceFighter(), bobFighter(), List.of(entry), buildFinalVitals());

    String page = renderer.render(0);

    assertTrue(page.contains("-> vince l'iniziativa (punteggio): Alice"));
    assertTrue(page.contains("-> primo ad agire: Alice"));
    assertTrue(page.contains("Alice attacca Bob e lo colpisce"));
    assertFalse(page.contains("Turno 1:"));
    assertFalse(page.contains("Iniziativa a inizio turno"));
    assertFalse(page.contains("Stamina ->"));
    assertFalse(page.contains("agilità"));
  }

  @Test
  void renderMostraLeSchedeDeiDueCombattentiInColonna3() {
    TurnLogEntry entry = new TurnLogEntry(1, "Alice attacca Bob")
        .withVitals(List.of(
            new FighterVitals("Alice", 40, 50, 20, 40),
            new FighterVitals("Bob", 44, 50, 25, 40)));
    CombatScreenRenderer renderer =
        new CombatScreenRenderer(aliceFighter(), bobFighter(), List.of(entry), buildFinalVitals());

    String page = renderer.render(0);

    assertTrue(page.contains("[1] Alice"));
    assertTrue(page.contains("[2] Bob"));
    assertTrue(page.contains("HUMAN WARRIOR"));
    assertTrue(page.contains("Arma"));
    assertTrue(page.contains("Arm."));
    assertTrue(page.contains("VIT"));
    assertTrue(page.contains("STA"));
  }

  @Test
  void renderMarcaConAsteriscoIlCombattenteConLIniziativa() {
    TurnLogEntry entry = new TurnLogEntry(1, "Alice attacca Bob")
        .withVitals(List.of(
            new FighterVitals("Alice", 40, 50, 20, 40),
            new FighterVitals("Bob", 44, 50, 25, 40)))
        .withInitiative(buildInitiative("Alice"));
    CombatScreenRenderer renderer =
        new CombatScreenRenderer(aliceFighter(), bobFighter(), List.of(entry), buildFinalVitals());

    String page = renderer.render(0);

    assertTrue(page.contains("*Alice*"));
    assertFalse(page.contains("*Bob*"));
  }

  @Test
  void renderNonMarcaNessunoSeLIniziativaNonEDisponibile() {
    TurnLogEntry entry = new TurnLogEntry(1, "Alice attacca Bob")
        .withVitals(List.of(
            new FighterVitals("Alice", 40, 50, 20, 40),
            new FighterVitals("Bob", 44, 50, 25, 40)));
    CombatScreenRenderer renderer =
        new CombatScreenRenderer(aliceFighter(), bobFighter(), List.of(entry), buildFinalVitals());

    String page = renderer.render(0);

    assertFalse(page.contains("*Alice*"));
    assertFalse(page.contains("*Bob*"));
  }

  @Test
  void renderMostraIlSegnoDiVariazioneSuVitaEStamina() {
    TurnLogEntry entry = new TurnLogEntry(1, "Alice attacca Bob")
        .withVitals(List.of(
            new FighterVitals("Alice", 40, 50, 20, 40),
            new FighterVitals("Bob", 44, 50, 25, 40)));
    List<FighterVitals> finalVitals = List.of(
        new FighterVitals("Alice", 30, 50, 20, 40),
        new FighterVitals("Bob", 44, 50, 28, 40));
    CombatScreenRenderer renderer =
        new CombatScreenRenderer(aliceFighter(), bobFighter(), List.of(entry), finalVitals);

    String page = renderer.render(0);

    // Vita di Alice scesa da 40 a 30 -> "-".
    assertTrue(page.contains("30/50-"));
    // Stamina di Bob salita da 25 a 28 -> "+".
    assertTrue(page.contains("28/40+"));
    // Vita di Bob invariata (44 -> 44): nessun segno.
    assertFalse(page.contains("44/50+"));
    assertFalse(page.contains("44/50-"));
  }

  private Fighter aliceFighter() {
    return CombatFixtures.createFighter("Alice", 14, 12, 12, 40, 10, 6, 3);
  }

  private Fighter bobFighter() {
    return CombatFixtures.createFighter("Bob", 13, 11, 12, 40, 10, 5, 3);
  }

  private InitiativeReport buildInitiative(String chosenName) {
    InitiativeBreakdown aliceBreakdown =
        new InitiativeBreakdown("Alice", 10.0, 5.0, 2.5, 1.0, 18.5, 20, 40, 10, 5, 3);
    InitiativeBreakdown bobBreakdown =
        new InitiativeBreakdown("Bob", 8.0, 4.0, 2.0, 3.0, 17.0, 25, 40, 8, 4, 6);
    return new InitiativeReport(
        List.of(aliceBreakdown, bobBreakdown), chosenName, chosenName, InitiativeOverride.NONE);
  }

  private List<TurnLogEntry> buildLog() {
    TurnLogEntry turn1 = new TurnLogEntry(1, "Alice attacca Bob")
        .withVitals(List.of(
            new FighterVitals("Alice", 45, 50, 34, 40),
            new FighterVitals("Bob", 48, 50, 36, 40)));
    TurnLogEntry turn2 = new TurnLogEntry(2, "Bob para il colpo")
        .withVitals(List.of(
            new FighterVitals("Alice", 40, 50, 30, 40),
            new FighterVitals("Bob", 44, 50, 32, 40)));
    TurnLogEntry turn3 = new TurnLogEntry(3, "Alice manca il colpo")
        .withVitals(List.of(
            new FighterVitals("Alice", 35, 50, 26, 40),
            new FighterVitals("Bob", 40, 50, 28, 40)));
    return List.of(turn1, turn2, turn3);
  }

  private List<FighterVitals> buildFinalVitals() {
    return List.of(
        new FighterVitals("Alice", 35, 50, 26, 40),
        new FighterVitals("Bob", 40, 50, 28, 40));
  }
}
