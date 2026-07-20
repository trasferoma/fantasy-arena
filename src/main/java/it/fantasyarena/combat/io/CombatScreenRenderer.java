package it.fantasyarena.combat.io;

import java.util.ArrayList;
import java.util.List;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.FighterVitals;
import it.fantasyarena.combat.result.InitiativeReport;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * Costruisce, come stringa multilinea pura (nessun I/O), la pagina del replay "a schermo" di
 * un turno, a tre colonne affiancate: i due pannelli ASCII con barre verticali di vita/stamina
 * dei combattenti (stato successivo al turno rivelato, marcatore {@code *Nome*} su chi ha
 * l'iniziativa nel turno corrente, segno +/- di variazione di vita e stamina durante il turno),
 * solo gli eventi del turno corrente (in forma compatta, non il log cumulativo) e infine le
 * schede compatte dei due combattenti prodotte da {@link FighterCardFormatter}. Usato da
 * {@link ScreenCombatReplay}, che si occupa solo di pulire lo schermo e stampare quanto
 * prodotto qui.
 */
public class CombatScreenRenderer {

  private static final int BAR_HEIGHT = 10;
  private static final int BASE_COLUMN_WIDTH = 8;
  private static final int MAX_COLUMN_WIDTH = 20;
  private static final int MIN_CURRENT_TURN_COLUMN_WIDTH = 40;
  private static final int MAX_CURRENT_TURN_COLUMN_WIDTH = 130;
  private static final String PANEL_GAP = "  ";

  private final Fighter first;
  private final Fighter second;
  private final List<TurnLogEntry> log;
  private final List<FighterVitals> finalVitals;
  private final TurnLogFormatter formatter = new TurnLogFormatter();
  private final FighterCardFormatter cardFormatter = new FighterCardFormatter();
  private final int currentTurnColumnWidth;

  public CombatScreenRenderer(Fighter first, Fighter second, List<TurnLogEntry> log,
      List<FighterVitals> finalVitals) {
    this.first = first;
    this.second = second;
    this.log = List.copyOf(log);
    this.finalVitals = List.copyOf(finalVitals);
    this.currentTurnColumnWidth = computeCurrentTurnColumnWidth();
  }

  /**
   * Larghezza della colonna 2 (eventi del turno): pari alla riga di evento più lunga dell'intero
   * duello, così nessun turno viene troncato e la colonna 3 (schede) inizia sempre alla stessa
   * posizione. Vincolata tra {@link #MIN_CURRENT_TURN_COLUMN_WIDTH} e
   * {@link #MAX_CURRENT_TURN_COLUMN_WIDTH} per evitare colonne troppo strette o eccessive.
   */
  private int computeCurrentTurnColumnWidth() {
    int longest = log.stream()
        .flatMap(entry -> formatter.formatCompact(entry).stream())
        .mapToInt(String::length)
        .max()
        .orElse(MIN_CURRENT_TURN_COLUMN_WIDTH);
    return Math.max(MIN_CURRENT_TURN_COLUMN_WIDTH, Math.min(MAX_CURRENT_TURN_COLUMN_WIDTH, longest));
  }

  public String render(int turnPosition) {
    List<FighterVitals> vitals = vitalsAfter(turnPosition);
    List<FighterVitals> vitalsBeforeTurn = vitalsBeforeTurn(turnPosition);
    String chosenName = chosenName(turnPosition);

    FighterVitals firstVitals = vitals.get(0);
    FighterVitals secondVitals = vitals.get(1);
    boolean firstChosen = isChosen(firstVitals.name(), chosenName);
    boolean secondChosen = isChosen(secondVitals.name(), chosenName);
    // Se lo snapshot di inizio turno non è utilizzabile, usiamo lo stato corrente come
    // riferimento: changeSign restituisce così "nessuna variazione" invece di un segno fasullo.
    FighterVitals firstBefore = (vitalsBeforeTurn != null ? vitalsBeforeTurn.get(0) : firstVitals);
    FighterVitals secondBefore = (vitalsBeforeTurn != null ? vitalsBeforeTurn.get(1) : secondVitals);

    int columnWidth = computeColumnWidth(
        panelHeader(firstVitals.name(), firstChosen), panelHeader(secondVitals.name(), secondChosen));

    List<String> firstPanel = buildPanel(firstVitals, firstBefore, firstChosen, columnWidth);
    List<String> secondPanel = buildPanel(secondVitals, secondBefore, secondChosen, columnWidth);
    List<String> currentTurnColumn = buildCurrentTurnColumn(turnPosition);
    List<String> fighterCardsColumn = buildFighterCardsColumn();

    StringBuilder page = new StringBuilder();
    page.append("=== Duello — turno ").append(turnPosition + 1).append(" / ").append(log.size()).append(" ===\n");

    for (int row = 0; row < firstPanel.size(); row++) {
      String middleLine = middleLineAt(currentTurnColumn, row);
      String rightLine = lineAt(fighterCardsColumn, row);
      page.append(firstPanel.get(row)).append(PANEL_GAP).append(secondPanel.get(row)).append(PANEL_GAP)
          .append(middleLine).append(PANEL_GAP).append(rightLine).append('\n');
    }

    page.append("(INVIO per avanzare — turno ").append(turnPosition + 1).append("/").append(log.size()).append(")\n");
    return page.toString();
  }

  /**
   * Stato dei due combattenti da mostrare nel pannello: quello successivo al turno rivelato
   * (il turno {@code turnPosition + 1}), oppure lo stato finale se non c'è un turno successivo
   * o il suo snapshot non è utilizzabile.
   */
  private List<FighterVitals> vitalsAfter(int turnPosition) {
    List<FighterVitals> candidate = finalVitals;
    if (turnPosition + 1 < log.size() && log.get(turnPosition + 1).vitals().size() == 2) {
      candidate = log.get(turnPosition + 1).vitals();
    }
    if (candidate.size() != 2) {
      candidate = finalVitals;
    }
    if (candidate.isEmpty()) {
      candidate = log.get(turnPosition).vitals();
    }
    return candidate;
  }

  /**
   * Stato dei due combattenti a inizio turno corrente, usato come riferimento per il segno
   * +/- di variazione. {@code null} se lo snapshot non è utilizzabile (caso limite).
   */
  private List<FighterVitals> vitalsBeforeTurn(int turnPosition) {
    List<FighterVitals> candidate = log.get(turnPosition).vitals();
    return (candidate.size() == 2 ? candidate : null);
  }

  /**
   * Nome del combattente che ha l'iniziativa nel turno corrente, {@code null} se il report
   * d'iniziativa non è disponibile per questo turno.
   */
  private String chosenName(int turnPosition) {
    InitiativeReport initiative = log.get(turnPosition).initiative();
    return (initiative != null ? initiative.chosenName() : null);
  }

  private boolean isChosen(String name, String chosenName) {
    return name.equals(chosenName);
  }

  private String panelHeader(String name, boolean chosen) {
    return (chosen ? "*" + name + "*" : name);
  }

  /**
   * Larghezza (per colonna VITA/STAMINA) sufficiente a ospitare l'header più lungo tra i due
   * pannelli, marcatore {@code *Nome*} incluso, entro il limite {@link #MAX_COLUMN_WIDTH}: oltre
   * quel limite l'header viene troncato da {@link #center}, ma i due pannelli restano sempre
   * della stessa larghezza.
   */
  private int computeColumnWidth(String firstHeader, String secondHeader) {
    int neededWidth = Math.max(firstHeader.length(), secondHeader.length());
    int columnWidth = BASE_COLUMN_WIDTH;
    while (columnWidth < MAX_COLUMN_WIDTH && (columnWidth * 2 + 1) < neededWidth) {
      columnWidth++;
    }
    return columnWidth;
  }

  private List<String> buildPanel(
      FighterVitals vitals, FighterVitals previousVitals, boolean chosen, int columnWidth) {

    int panelInnerWidth = columnWidth * 2 + 1;
    String border = "+" + "-".repeat(panelInnerWidth) + "+";
    int healthFilled = filledCells(vitals.currentHealth(), vitals.maxHealth(), BAR_HEIGHT);
    int staminaFilled = filledCells(vitals.currentStamina(), vitals.maxStamina(), BAR_HEIGHT);

    List<String> lines = new ArrayList<>();
    lines.add(border);
    lines.add("|" + center(panelHeader(vitals.name(), chosen), panelInnerWidth) + "|");
    lines.add("|" + center("VITA", columnWidth) + " " + center("STAMINA", columnWidth) + "|");

    for (int row = 0; row < BAR_HEIGHT; row++) {
      int rowFromBottom = BAR_HEIGHT - row;
      String healthCell = barCell(rowFromBottom <= healthFilled, columnWidth);
      String staminaCell = barCell(rowFromBottom <= staminaFilled, columnWidth);
      lines.add("|" + healthCell + " " + staminaCell + "|");
    }

    String healthLabel = vitals.currentHealth() + "/" + vitals.maxHealth()
        + changeSign(vitals.currentHealth(), previousVitals.currentHealth());
    String staminaLabel = vitals.currentStamina() + "/" + vitals.maxStamina()
        + changeSign(vitals.currentStamina(), previousVitals.currentStamina());
    lines.add("|" + center(healthLabel, columnWidth) + " " + center(staminaLabel, columnWidth) + "|");
    lines.add(border);

    return lines;
  }

  private String barCell(boolean filled, int columnWidth) {
    return (filled ? "#" : ".").repeat(columnWidth);
  }

  private String center(String text, int width) {
    if (text.length() >= width) {
      return text.substring(0, width);
    }
    int totalPadding = width - text.length();
    int leftPadding = totalPadding / 2;
    int rightPadding = totalPadding - leftPadding;
    return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
  }

  /**
   * Colonna 2: solo gli eventi del turno corrente (in forma compatta), non il log cumulativo
   * dei turni precedenti. Ogni riga è allineata a {@link #currentTurnColumnWidth} (dimensionata
   * sulla riga più lunga del duello) così la colonna 3 (schede) inizia sempre alla stessa
   * posizione; il troncamento scatta solo oltre {@link #MAX_CURRENT_TURN_COLUMN_WIDTH}.
   */
  private List<String> buildCurrentTurnColumn(int turnPosition) {
    return formatter.formatCompact(log.get(turnPosition)).stream()
        .map(this::formatCurrentTurnLine)
        .toList();
  }

  private String formatCurrentTurnLine(String line) {
    String truncated = (line.length() <= currentTurnColumnWidth
        ? line
        : line.substring(0, currentTurnColumnWidth - 3) + "...");
    return padRight(truncated, currentTurnColumnWidth);
  }

  private String padRight(String text, int width) {
    if (text.length() >= width) {
      return text;
    }
    return text + " ".repeat(width - text.length());
  }

  /**
   * Colonna 3: le schede compatte dei due combattenti, impilate (prima il combattente 1, poi
   * il combattente 2), uguali a ogni turno.
   */
  private List<String> buildFighterCardsColumn() {
    List<String> lines = new ArrayList<>();
    lines.addAll(cardFormatter.card(1, first));
    lines.add("");
    lines.addAll(cardFormatter.card(2, second));
    return lines;
  }

  /**
   * Riga della colonna 2 (larghezza fissa) alla posizione {@code row}, o una riga vuota della
   * stessa larghezza se la colonna finisce prima: mantiene allineato l'inizio della colonna 3.
   */
  private String middleLineAt(List<String> lines, int row) {
    return (row < lines.size() ? lines.get(row) : " ".repeat(currentTurnColumnWidth));
  }

  /**
   * Riga generica (ultima colonna, nessun bisogno di allineamento successivo) alla posizione
   * {@code row}, o stringa vuota se la colonna finisce prima.
   */
  private String lineAt(List<String> lines, int row) {
    return (row < lines.size() ? lines.get(row) : "");
  }

  static int filledCells(int current, int max, int barHeight) {
    if (max <= 0) {
      return 0;
    }

    double ratio = (double) current / max;
    long filled = Math.round(ratio * barHeight);
    if (filled < 0) {
      return 0;
    }
    if (filled > barHeight) {
      return barHeight;
    }
    return (int) filled;
  }

  /**
   * Segno della variazione di una metrica (vita o stamina) durante il turno rivelato:
   * {@code "+"} se il valore corrente supera quello precedente, {@code "-"} se è inferiore,
   * uno spazio se è invariato (mantiene l'allineamento delle colonne).
   */
  static String changeSign(int current, int previous) {
    if (current > previous) {
      return "+";
    }
    if (current < previous) {
      return "-";
    }
    return " ";
  }
}
