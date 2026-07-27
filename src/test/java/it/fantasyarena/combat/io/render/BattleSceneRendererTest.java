package it.fantasyarena.combat.io.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasycombatsystem.battle.EngagementTurn;
import it.fantasycombatsystem.battle.RoundLogEntry;
import it.fantasycombatsystem.result.ActionOutcome;
import it.fantasycombatsystem.result.FighterVitals;
import it.fantasycombatsystem.result.TurnLogEntry;

/**
 * Verifica {@link BattleSceneRenderer}: puro (nessun I/O), quindi testato per asserzioni su
 * righe precise. Copre: un 1 contro 1 dentro una sola banda; un 2v1 con il terzo partecipante
 * sotto nella sua colonna, senza freccia; l'attaccante di fazione 1 (freccia rovesciata); i
 * delta mostrati solo se diversi da zero; il marcatore {@code [a terra]}; tutte le etichette
 * della formula breve; la banda "(in attesa)"; l'allineamento delle colonne su due bande con
 * nomi di lunghezza diversa; e la stabilita' della larghezza fra round con delta di lunghezza
 * diversa (le colonne sono calcolate una sola volta dal roster, non dal contenuto del round).
 */
class BattleSceneRendererTest {

  private static final String GAP = "  ";
  private static final int ARROW_WIDTH = 20;
  private static final int MIDDLE_WIDTH = GAP.length() * 2 + ARROW_WIDTH;
  private static final String RIGHT_ARROW = "=".repeat(ARROW_WIDTH - 1) + ">";
  private static final String LEFT_ARROW = "<" + "=".repeat(ARROW_WIDTH - 1);
  private static final String HEALTH_LABEL = "Vita:    ";
  private static final String STAMINA_LABEL = "Stamina: ";
  private static final String DOWN_SUFFIX = " [a terra]";

  @Test
  void unoControUno_staInUnaSolaBandaConFrecciaVersoIlBersaglio() {
    List<FighterProfile> roster = List.of(
        new FighterProfile("Alice", 0, 40, 20),
        new FighterProfile("Bobby", 1, 40, 20));
    BattleSceneRenderer renderer = new BattleSceneRenderer(roster);

    FighterVitals aliceVitals = new FighterVitals("Alice", 40, 40, 20, 20);
    FighterVitals bobbyVitals = new FighterVitals("Bobby", 40, 40, 20, 20);
    List<FighterVitals> vitals = List.of(aliceVitals, bobbyVitals);

    ActionOutcome action = new ActionOutcome(ActionOutcome.Kind.HIT, 17, 0, false, false);
    TurnLogEntry turnEntry = new TurnLogEntry(1, "Alice attacca Bobby").withVitals(vitals).withAction(action);
    EngagementTurn turn = new EngagementTurn(0, 0, 1, List.of(0, 1), turnEntry);
    RoundLogEntry round = new RoundLogEntry(1, List.of(turn), vitals, List.of());

    List<String> lines = renderer.renderRound(round);

    int columnWidth = expectedColumnWidth(roster);
    List<String> expected = List.of(
        header(1, columnWidth),
        "",
        padRight("Alice", columnWidth) + blank() + padRight("Bobby", columnWidth),
        padRight("Vita:    40/40", columnWidth) + GAP + center("colpisce (17)", ARROW_WIDTH) + GAP
            + padRight("Vita:    40/40", columnWidth),
        padRight("Stamina: 20/20", columnWidth) + GAP + RIGHT_ARROW + GAP + padRight("Stamina: 20/20", columnWidth));

    assertEquals(expected, lines);
  }

  @Test
  void dueControUno_ilTerzoPartecipante_staSottoNellaSuaColonnaSenzaFreccia() {
    List<FighterProfile> roster = List.of(
        new FighterProfile("Amy", 0, 30, 10),
        new FighterProfile("Ally", 0, 30, 10),
        new FighterProfile("Bea", 1, 30, 10));
    BattleSceneRenderer renderer = new BattleSceneRenderer(roster);

    FighterVitals amyVitals = new FighterVitals("Amy", 30, 30, 10, 10);
    FighterVitals allyVitals = new FighterVitals("Ally", 30, 30, 10, 10);
    FighterVitals beaVitals = new FighterVitals("Bea", 30, 30, 10, 10);
    List<FighterVitals> vitals = List.of(amyVitals, allyVitals, beaVitals);

    ActionOutcome action = new ActionOutcome(ActionOutcome.Kind.PARRIED, 5, 0, false, false);
    TurnLogEntry turnEntry = new TurnLogEntry(1, "Amy attacca Bea").withVitals(vitals).withAction(action);
    EngagementTurn turn = new EngagementTurn(0, 0, 2, List.of(0, 1, 2), turnEntry);
    RoundLogEntry round = new RoundLogEntry(1, List.of(turn), vitals, List.of());

    List<String> lines = renderer.renderRound(round);

    int columnWidth = expectedColumnWidth(roster);
    List<String> expected = List.of(
        header(1, columnWidth),
        "",
        padRight("Amy", columnWidth) + blank() + padRight("Bea", columnWidth),
        padRight("Vita:    30/30", columnWidth) + GAP + center("parato (5)", ARROW_WIDTH) + GAP
            + padRight("Vita:    30/30", columnWidth),
        padRight("Stamina: 10/10", columnWidth) + GAP + RIGHT_ARROW + GAP + padRight("Stamina: 10/10", columnWidth),
        "",
        "Ally",
        "Vita:    30/30",
        "Stamina: 10/10");

    assertEquals(expected, lines);
  }

  @Test
  void attaccanteDiFazione1_mostraLaFrecciaRovesciata() {
    List<FighterProfile> roster = List.of(
        new FighterProfile("X", 0, 20, 5),
        new FighterProfile("Y", 1, 20, 5));
    BattleSceneRenderer renderer = new BattleSceneRenderer(roster);

    FighterVitals xVitals = new FighterVitals("X", 20, 20, 5, 5);
    FighterVitals yVitals = new FighterVitals("Y", 20, 20, 5, 5);
    List<FighterVitals> vitals = List.of(xVitals, yVitals);

    ActionOutcome action = new ActionOutcome(ActionOutcome.Kind.DODGED, 0, 0, false, false);
    TurnLogEntry turnEntry = new TurnLogEntry(1, "Y attacca X").withVitals(vitals).withAction(action);
    EngagementTurn turn = new EngagementTurn(0, 1, 0, List.of(0, 1), turnEntry);
    RoundLogEntry round = new RoundLogEntry(1, List.of(turn), vitals, List.of());

    List<String> lines = renderer.renderRound(round);

    int columnWidth = expectedColumnWidth(roster);
    List<String> expected = List.of(
        header(1, columnWidth),
        "",
        padRight("X", columnWidth) + blank() + padRight("Y", columnWidth),
        padRight("Vita:    20/20", columnWidth) + GAP + center("schivato", ARROW_WIDTH) + GAP
            + padRight("Vita:    20/20", columnWidth),
        padRight("Stamina: 5/5", columnWidth) + GAP + LEFT_ARROW + GAP + padRight("Stamina: 5/5", columnWidth));

    assertEquals(expected, lines);
  }

  @Test
  void deltaDiVitaEStamina_mostratiSoloSeDiversiDaZero() {
    List<FighterProfile> roster = List.of(
        new FighterProfile("Caracalla", 0, 38, 22),
        new FighterProfile("Kragg", 1, 39, 16));
    BattleSceneRenderer renderer = new BattleSceneRenderer(roster);

    FighterVitals caracallaStart = new FighterVitals("Caracalla", 38, 38, 22, 22);
    FighterVitals kraggStart = new FighterVitals("Kragg", 39, 39, 16, 16);
    List<FighterVitals> startVitals = List.of(caracallaStart, kraggStart);
    FighterVitals caracallaEnd = new FighterVitals("Caracalla", 38, 38, 22, 22);
    FighterVitals kraggEnd = new FighterVitals("Kragg", 22, 39, 11, 16);
    List<FighterVitals> endVitals = List.of(caracallaEnd, kraggEnd);

    ActionOutcome action = new ActionOutcome(ActionOutcome.Kind.HIT, 17, 0, false, false);
    TurnLogEntry turnEntry = new TurnLogEntry(1, "Caracalla attacca Kragg").withVitals(startVitals).withAction(action);
    EngagementTurn turn = new EngagementTurn(0, 0, 1, List.of(0, 1), turnEntry);
    RoundLogEntry round = new RoundLogEntry(1, List.of(turn), endVitals, List.of());

    List<String> lines = renderer.renderRound(round);

    int columnWidth = expectedColumnWidth(roster);
    List<String> expected = List.of(
        header(1, columnWidth),
        "",
        padRight("Caracalla", columnWidth) + blank() + padRight("Kragg", columnWidth),
        padRight("Vita:    38/38", columnWidth) + GAP + center("colpisce (17)", ARROW_WIDTH) + GAP
            + padRight("Vita:    22/39 (-17)", columnWidth),
        padRight("Stamina: 22/22", columnWidth) + GAP + RIGHT_ARROW + GAP
            + padRight("Stamina: 11/16 (-5)", columnWidth));

    assertEquals(expected, lines);
  }

  @Test
  void combattenteAVitaZero_mostraIlMarcatoreATerra() {
    List<FighterProfile> roster = List.of(
        new FighterProfile("Hero", 0, 40, 20),
        new FighterProfile("Villain", 1, 40, 20));
    BattleSceneRenderer renderer = new BattleSceneRenderer(roster);

    FighterVitals heroStart = new FighterVitals("Hero", 40, 40, 20, 20);
    FighterVitals villainStart = new FighterVitals("Villain", 10, 40, 15, 20);
    List<FighterVitals> startVitals = List.of(heroStart, villainStart);
    FighterVitals heroEnd = new FighterVitals("Hero", 40, 40, 20, 20);
    FighterVitals villainEnd = new FighterVitals("Villain", 0, 40, 15, 20);
    List<FighterVitals> endVitals = List.of(heroEnd, villainEnd);

    ActionOutcome action = new ActionOutcome(ActionOutcome.Kind.HIT, 10, 0, false, false);
    TurnLogEntry turnEntry = new TurnLogEntry(1, "Hero attacca Villain").withVitals(startVitals).withAction(action);
    EngagementTurn turn = new EngagementTurn(0, 0, 1, List.of(0, 1), turnEntry);
    RoundLogEntry round = new RoundLogEntry(1, List.of(turn), endVitals, List.of());

    List<String> lines = renderer.renderRound(round);

    int columnWidth = expectedColumnWidth(roster);
    List<String> expected = List.of(
        header(1, columnWidth),
        "",
        padRight("Hero", columnWidth) + blank() + padRight("Villain [a terra]", columnWidth),
        padRight("Vita:    40/40", columnWidth) + GAP + center("colpisce (10)", ARROW_WIDTH) + GAP
            + padRight("Vita:    0/40 (-10)", columnWidth),
        padRight("Stamina: 20/20", columnWidth) + GAP + RIGHT_ARROW + GAP + padRight("Stamina: 15/20", columnWidth));

    assertEquals(expected, lines);
  }

  @Test
  void formulaBreve_copreTutteLeEtichetteDellaTabella() {
    assertTrue(vitaRigaDi(new ActionOutcome(ActionOutcome.Kind.HIT, 12, 0, true, false)).contains("critico (12)"));
    assertTrue(vitaRigaDi(new ActionOutcome(ActionOutcome.Kind.HIT, 12, 0, true, true)).contains("critico (12)"),
        "critico e colpo potente insieme: vince critico");
    assertTrue(
        vitaRigaDi(new ActionOutcome(ActionOutcome.Kind.HIT, 12, 0, false, true)).contains("colpo potente (12)"));
    assertTrue(vitaRigaDi(new ActionOutcome(ActionOutcome.Kind.HIT, 12, 0, false, false)).contains("colpisce (12)"));
    assertTrue(vitaRigaDi(new ActionOutcome(ActionOutcome.Kind.MISS, 0, 0, false, false)).contains("manca"));
    assertTrue(vitaRigaDi(new ActionOutcome(ActionOutcome.Kind.PARRIED, 4, 0, false, false)).contains("parato (4)"));
    assertTrue(vitaRigaDi(new ActionOutcome(ActionOutcome.Kind.DODGED, 0, 0, false, false)).contains("schivato"));
    assertTrue(vitaRigaDi(new ActionOutcome(ActionOutcome.Kind.REST, 0, 3, false, false)).contains("riposa (+3)"));
  }

  @Test
  void azioneNulla_nonSolleva_eLasciaLaFormulaVuota() {
    List<FighterProfile> roster = List.of(
        new FighterProfile("Attaccante", 0, 30, 10),
        new FighterProfile("Difensore", 1, 30, 10));
    BattleSceneRenderer renderer = new BattleSceneRenderer(roster);

    FighterVitals attackerVitals = new FighterVitals("Attaccante", 30, 30, 10, 10);
    FighterVitals defenderVitals = new FighterVitals("Difensore", 30, 30, 10, 10);
    List<FighterVitals> vitals = List.of(attackerVitals, defenderVitals);

    TurnLogEntry turnEntry = new TurnLogEntry(1, "descrizione senza action").withVitals(vitals);
    EngagementTurn turn = new EngagementTurn(0, 0, 1, List.of(0, 1), turnEntry);
    RoundLogEntry round = new RoundLogEntry(1, List.of(turn), vitals, List.of());

    List<String> lines = renderer.renderRound(round);

    int columnWidth = expectedColumnWidth(roster);
    String expectedVitaLine =
        padRight("Vita:    30/30", columnWidth) + blank() + padRight("Vita:    30/30", columnWidth);
    assertEquals(expectedVitaLine, lines.get(3), "senza ActionOutcome la formula resta vuota, non solleva");
  }

  @Test
  void bandaInAttesa_perICombattentiViviNonCoinvoltiInAlcunoScambio() {
    List<FighterProfile> roster = List.of(
        new FighterProfile("Attacker", 0, 40, 20),
        new FighterProfile("Target", 1, 40, 15),
        new FighterProfile("Waiting", 0, 25, 10));
    BattleSceneRenderer renderer = new BattleSceneRenderer(roster);

    FighterVitals attackerVitals = new FighterVitals("Attacker", 40, 40, 20, 20);
    FighterVitals targetVitals = new FighterVitals("Target", 40, 40, 15, 15);
    FighterVitals waitingStart = new FighterVitals("Waiting", 25, 25, 8, 10);
    List<FighterVitals> startVitals = List.of(attackerVitals, targetVitals, waitingStart);
    FighterVitals waitingEnd = new FighterVitals("Waiting", 25, 25, 9, 10);
    List<FighterVitals> endVitals = List.of(attackerVitals, targetVitals, waitingEnd);

    ActionOutcome action = new ActionOutcome(ActionOutcome.Kind.MISS, 0, 0, false, false);
    TurnLogEntry turnEntry = new TurnLogEntry(1, "Attacker attacca Target").withVitals(startVitals).withAction(action);
    EngagementTurn turn = new EngagementTurn(0, 0, 1, List.of(0, 1), turnEntry);
    RoundLogEntry round = new RoundLogEntry(1, List.of(turn), endVitals, List.of());

    List<String> lines = renderer.renderRound(round);

    int columnWidth = expectedColumnWidth(roster);
    List<String> expected = List.of(
        header(1, columnWidth),
        "",
        padRight("Attacker", columnWidth) + blank() + padRight("Target", columnWidth),
        padRight("Vita:    40/40", columnWidth) + GAP + center("manca", ARROW_WIDTH) + GAP
            + padRight("Vita:    40/40", columnWidth),
        padRight("Stamina: 20/20", columnWidth) + GAP + RIGHT_ARROW + GAP + padRight("Stamina: 15/15", columnWidth),
        "",
        "(in attesa)",
        "Waiting",
        "Vita:    25/25",
        "Stamina: 9/10 (+1)");

    assertEquals(expected, lines);
  }

  @Test
  void colonneAllineate_suDueBandeConNomiDiLunghezzaDiversa() {
    List<FighterProfile> roster = List.of(
        new FighterProfile("Al", 0, 30, 10),
        new FighterProfile("Bo", 1, 30, 10),
        new FighterProfile("Constantine", 0, 50, 20),
        new FighterProfile("Xerxes", 1, 40, 15));
    BattleSceneRenderer renderer = new BattleSceneRenderer(roster);

    FighterVitals al = new FighterVitals("Al", 30, 30, 10, 10);
    FighterVitals bo = new FighterVitals("Bo", 30, 30, 10, 10);
    FighterVitals constantine = new FighterVitals("Constantine", 50, 50, 20, 20);
    FighterVitals xerxesStart = new FighterVitals("Xerxes", 40, 40, 15, 15);
    FighterVitals xerxesEnd = new FighterVitals("Xerxes", 22, 40, 15, 15);
    List<FighterVitals> startVitals = List.of(al, bo, constantine, xerxesStart);
    List<FighterVitals> endVitals = List.of(al, bo, constantine, xerxesEnd);

    ActionOutcome missAction = new ActionOutcome(ActionOutcome.Kind.MISS, 0, 0, false, false);
    TurnLogEntry turn0Entry = new TurnLogEntry(1, "Al attacca Bo").withVitals(startVitals).withAction(missAction);
    EngagementTurn turn0 = new EngagementTurn(0, 0, 1, List.of(0, 1), turn0Entry);

    ActionOutcome hitAction = new ActionOutcome(ActionOutcome.Kind.HIT, 18, 0, false, false);
    TurnLogEntry turn1Entry =
        new TurnLogEntry(1, "Constantine attacca Xerxes").withVitals(startVitals).withAction(hitAction);
    EngagementTurn turn1 = new EngagementTurn(1, 2, 3, List.of(2, 3), turn1Entry);

    RoundLogEntry round = new RoundLogEntry(1, List.of(turn0, turn1), endVitals, List.of());

    List<String> lines = renderer.renderRound(round);

    int columnWidth = expectedColumnWidth(roster);
    int totalWidth = columnWidth * 2 + MIDDLE_WIDTH;
    List<String> expected = List.of(
        header(1, columnWidth),
        "",
        padRight("Al", columnWidth) + blank() + padRight("Bo", columnWidth),
        padRight("Vita:    30/30", columnWidth) + GAP + center("manca", ARROW_WIDTH) + GAP
            + padRight("Vita:    30/30", columnWidth),
        padRight("Stamina: 10/10", columnWidth) + GAP + RIGHT_ARROW + GAP + padRight("Stamina: 10/10", columnWidth),
        "",
        separator(totalWidth),
        "",
        padRight("Constantine", columnWidth) + blank() + padRight("Xerxes", columnWidth),
        padRight("Vita:    50/50", columnWidth) + GAP + center("colpisce (18)", ARROW_WIDTH) + GAP
            + padRight("Vita:    22/40 (-18)", columnWidth),
        padRight("Stamina: 20/20", columnWidth) + GAP + RIGHT_ARROW + GAP + padRight("Stamina: 15/15", columnWidth));

    assertEquals(expected, lines);
  }

  /**
   * Blinda il difetto segnalato: le larghezze devono derivare dal roster (valori massimi), non
   * dal contenuto del round. Due round dello stesso {@link BattleSceneRenderer}, uno con un delta
   * a una cifra ({@code -7}) e uno con un delta a tre cifre ({@code -123}), devono produrre righe
   * della stessa identica lunghezza: la posizione di ogni colonna resta ferma da un round all'altro.
   */
  @Test
  void larghezzaDiColonna_restaCostanteAncheConDeltaDiLunghezzaDiversaFraRound() {
    List<FighterProfile> roster = List.of(
        new FighterProfile("Attacker", 0, 200, 50),
        new FighterProfile("Defender", 1, 200, 50));
    BattleSceneRenderer renderer = new BattleSceneRenderer(roster);
    FighterVitals attackerVitals = new FighterVitals("Attacker", 200, 200, 50, 50);

    List<String> smallDelta = renderer.renderRound(roundWithDefenderHealth(attackerVitals, 193));
    List<String> largeDelta = renderer.renderRound(roundWithDefenderHealth(attackerVitals, 77));

    assertEquals(smallDelta.size(), largeDelta.size(), "stessa struttura di banda in entrambi i round");
    for (int i = 0; i < smallDelta.size(); i++) {
      assertEquals(smallDelta.get(i).length(), largeDelta.get(i).length(),
          "riga " + i + ": la larghezza di colonna non deve dipendere dal numero di cifre del delta");
    }
  }

  private static RoundLogEntry roundWithDefenderHealth(FighterVitals attackerVitals, int defenderCurrentHealth) {
    FighterVitals defenderStart = new FighterVitals("Defender", 200, 200, 50, 50);
    List<FighterVitals> startVitals = List.of(attackerVitals, defenderStart);
    FighterVitals defenderEnd = new FighterVitals("Defender", defenderCurrentHealth, 200, 50, 50);
    List<FighterVitals> endVitals = List.of(attackerVitals, defenderEnd);

    ActionOutcome action = new ActionOutcome(ActionOutcome.Kind.HIT, 200 - defenderCurrentHealth, 0, false, false);
    TurnLogEntry turnEntry =
        new TurnLogEntry(1, "Attacker attacca Defender").withVitals(startVitals).withAction(action);
    EngagementTurn turn = new EngagementTurn(0, 0, 1, List.of(0, 1), turnEntry);
    return new RoundLogEntry(1, List.of(turn), endVitals, List.of());
  }

  private String vitaRigaDi(ActionOutcome action) {
    List<FighterProfile> roster = List.of(
        new FighterProfile("Attacker", 0, 30, 10),
        new FighterProfile("Defender", 1, 30, 10));
    BattleSceneRenderer renderer = new BattleSceneRenderer(roster);

    FighterVitals attackerVitals = new FighterVitals("Attacker", 30, 30, 10, 10);
    FighterVitals defenderVitals = new FighterVitals("Defender", 30, 30, 10, 10);
    List<FighterVitals> vitals = List.of(attackerVitals, defenderVitals);

    TurnLogEntry turnEntry = new TurnLogEntry(1, "descrizione").withVitals(vitals).withAction(action);
    EngagementTurn turn = new EngagementTurn(0, 0, 1, List.of(0, 1), turnEntry);
    RoundLogEntry round = new RoundLogEntry(1, List.of(turn), vitals, List.of());

    return renderer.renderRound(round).get(3);
  }

  /**
   * Ricalcola indipendentemente la larghezza di colonna attesa dal roster, con lo stesso
   * algoritmo del renderer (caso peggiore dai soli valori massimi): non delega alla classe sotto
   * test, cosi' l'asserzione resta un confronto reale col comportamento atteso.
   */
  private static int expectedColumnWidth(List<FighterProfile> roster) {
    int longest = 0;
    for (FighterProfile profile : roster) {
      longest = Math.max(longest, profile.name().length() + DOWN_SUFFIX.length());
      longest = Math.max(longest, worstCaseLineLength(HEALTH_LABEL, profile.maxHealth()));
      longest = Math.max(longest, worstCaseLineLength(STAMINA_LABEL, profile.maxStamina()));
    }
    return longest;
  }

  private static int worstCaseLineLength(String label, int maxValue) {
    int digits = Integer.toString(maxValue).length();
    int currentOverMax = 2 * digits + 1;
    int worstCaseDelta = digits + 4;
    return label.length() + currentOverMax + worstCaseDelta;
  }

  private static String header(int roundNumber, int columnWidth) {
    String label = " Round " + roundNumber + " ";
    int totalWidth = columnWidth * 2 + MIDDLE_WIDTH;
    int totalPadding = Math.max(0, totalWidth - label.length());
    int leftPadding = totalPadding / 2;
    int rightPadding = totalPadding - leftPadding;
    return "=".repeat(leftPadding) + label + "=".repeat(rightPadding);
  }

  private static String separator(int totalWidth) {
    StringBuilder builder = new StringBuilder();
    while (builder.length() < totalWidth) {
      builder.append("- ");
    }
    return builder.substring(0, totalWidth);
  }

  private static String blank() {
    return " ".repeat(MIDDLE_WIDTH);
  }

  private static String center(String text, int width) {
    int totalPadding = width - text.length();
    int leftPadding = totalPadding / 2;
    int rightPadding = totalPadding - leftPadding;
    return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
  }

  private static String padRight(String text, int width) {
    return text + " ".repeat(Math.max(0, width - text.length()));
  }
}
