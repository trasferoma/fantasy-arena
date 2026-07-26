package it.fantasyarena.combat.io;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import it.fantasycombatsystem.battle.EngagementTurn;
import it.fantasycombatsystem.battle.RoundLogEntry;
import it.fantasycombatsystem.result.ActionOutcome;
import it.fantasycombatsystem.result.FighterVitals;
import it.fantasycombatsystem.result.TurnLogEntry;

/**
 * Costruisce, come righe di testo pure (nessun I/O), la scena ASCII di un round della battaglia
 * NvN: fazione 0 sempre a sinistra, fazione 1 sempre a destra, una freccia orizzontale
 * sull'attaccante di ogni scambio (verso il bersaglio) e una formula breve sull'azione, ricavata
 * da {@link ActionOutcome} e non dalla descrizione testuale gia' composta. Non dipende da
 * {@code Fighter}: riceve il {@link FighterProfile} di ciascun combattente (nome, squadra, vita e
 * stamina massime) una sola volta in costruzione, nell'ordine stabile di {@code BattleRoster#all()}.
 *
 * <p>Le larghezze di colonna sono calcolate una sola volta, in costruzione, dal caso peggiore
 * ricavabile dai soli valori massimi del roster (non dal contenuto di un round specifico):
 * restano quindi identiche per tutta la battaglia, cosi' ogni combattente resta nella stessa
 * colonna round dopo round e la scena non scivola lateralmente.
 */
public class BattleSceneRenderer {

  private static final int TEAM_LEFT = 0;
  private static final int TEAM_RIGHT = 1;
  private static final int ARROW_WIDTH = 20;
  private static final String GAP = "  ";
  private static final int MIDDLE_WIDTH = GAP.length() * 2 + ARROW_WIDTH;
  private static final String RIGHT_ARROW = "=".repeat(ARROW_WIDTH - 1) + ">";
  private static final String LEFT_ARROW = "<" + "=".repeat(ARROW_WIDTH - 1);
  private static final String HEALTH_LABEL = "Vita:    ";
  private static final String STAMINA_LABEL = "Stamina: ";
  private static final String DOWN_SUFFIX = " [a terra]";
  private static final String[] EMPTY_BLOCK = {"", "", ""};

  private final List<String> fighterNames;
  private final List<Integer> teamIndexes;
  private final int columnWidth;
  private final int totalWidth;

  public BattleSceneRenderer(List<FighterProfile> roster) {
    this.fighterNames = roster.stream().map(FighterProfile::name).toList();
    this.teamIndexes = roster.stream().map(FighterProfile::teamIndex).toList();
    this.columnWidth = computeColumnWidth(roster);
    this.totalWidth = columnWidth * 2 + MIDDLE_WIDTH;
  }

  public List<String> renderRound(RoundLogEntry round) {
    List<FighterVitals> startVitals = startOfRoundVitalsOf(round);
    List<FighterVitals> endVitals = round.vitals();

    List<String> lines = new ArrayList<>();
    lines.add(header(round.roundNumber()));
    lines.add("");

    List<EngagementTurn> turns = round.turns();
    for (int i = 0; i < turns.size(); i++) {
      if (i > 0) {
        lines.add("");
        lines.add(separator());
        lines.add("");
      }
      lines.addAll(renderExchangeBand(turns.get(i), startVitals, endVitals));
    }

    List<Integer> waitingIndexes = waitingParticipantIndexes(round);
    if (!waitingIndexes.isEmpty()) {
      lines.add("");
      lines.add("(in attesa)");
      lines.addAll(renderWaitingBand(waitingIndexes, startVitals, endVitals));
    }

    if (!round.events().isEmpty()) {
      lines.add("");
      lines.addAll(round.events());
    }

    return lines;
  }

  private List<String> renderExchangeBand(EngagementTurn turn, List<FighterVitals> startVitals,
      List<FighterVitals> endVitals) {

    List<Integer> leftColumn = columnParticipants(turn, TEAM_LEFT);
    List<Integer> rightColumn = columnParticipants(turn, TEAM_RIGHT);
    List<String[]> leftBlocks = blocksOf(leftColumn, startVitals, endVitals);
    List<String[]> rightBlocks = blocksOf(rightColumn, startVitals, endVitals);

    String arrow = arrowFor(turn);
    String formulaLabel = formulaLabel(turn.turn());

    List<String> lines = new ArrayList<>();
    int rowCount = Math.max(leftBlocks.size(), rightBlocks.size());
    for (int row = 0; row < rowCount; row++) {
      if (row > 0) {
        lines.add("");
      }
      String[] left = (row < leftBlocks.size() ? leftBlocks.get(row) : EMPTY_BLOCK);
      String[] right = (row < rightBlocks.size() ? rightBlocks.get(row) : EMPTY_BLOCK);
      boolean isLeadRow = (row == 0);
      lines.add(combineRow(left[0], right[0], blankMiddle()));
      lines.add(combineRow(left[1], right[1], isLeadRow ? formulaMiddle(formulaLabel) : blankMiddle()));
      lines.add(combineRow(left[2], right[2], isLeadRow ? arrowMiddle(arrow) : blankMiddle()));
    }
    return lines;
  }

  private List<String> renderWaitingBand(List<Integer> waitingIndexes, List<FighterVitals> startVitals,
      List<FighterVitals> endVitals) {

    List<Integer> leftColumn = filterByTeam(waitingIndexes, TEAM_LEFT);
    List<Integer> rightColumn = filterByTeam(waitingIndexes, TEAM_RIGHT);
    List<String[]> leftBlocks = blocksOf(leftColumn, startVitals, endVitals);
    List<String[]> rightBlocks = blocksOf(rightColumn, startVitals, endVitals);

    List<String> lines = new ArrayList<>();
    int rowCount = Math.max(leftBlocks.size(), rightBlocks.size());
    for (int row = 0; row < rowCount; row++) {
      if (row > 0) {
        lines.add("");
      }
      String[] left = (row < leftBlocks.size() ? leftBlocks.get(row) : EMPTY_BLOCK);
      String[] right = (row < rightBlocks.size() ? rightBlocks.get(row) : EMPTY_BLOCK);
      lines.add(combineRow(left[0], right[0], blankMiddle()));
      lines.add(combineRow(left[1], right[1], blankMiddle()));
      lines.add(combineRow(left[2], right[2], blankMiddle()));
    }
    return lines;
  }

  /**
   * Partecipanti di {@code turn} appartenenti a {@code team}, con il capofila (l'attaccante o il
   * bersaglio, quello dei due che appartiene a questo lato) sempre in prima posizione: cosi' la
   * freccia resta sulla prima coppia di blocchi, come richiesto.
   */
  private List<Integer> columnParticipants(EngagementTurn turn, int team) {
    int leadIndex = (teamIndexes.get(turn.attackerIndex()) == team ? turn.attackerIndex() : turn.targetIndex());

    List<Integer> column = new ArrayList<>();
    column.add(leadIndex);
    for (int index : turn.participantIndexes()) {
      if (index != leadIndex && teamIndexes.get(index) == team) {
        column.add(index);
      }
    }
    return column;
  }

  private List<Integer> filterByTeam(List<Integer> indexes, int team) {
    return indexes.stream().filter(index -> teamIndexes.get(index) == team).toList();
  }

  private String arrowFor(EngagementTurn turn) {
    return (teamIndexes.get(turn.attackerIndex()) == TEAM_LEFT ? RIGHT_ARROW : LEFT_ARROW);
  }

  private String formulaLabel(TurnLogEntry turn) {
    ActionOutcome action = turn.action();
    if (action == null) {
      return "";
    }
    return switch (action.kind()) {
      case HIT -> hitLabel(action);
      case MISS -> "manca";
      case PARRIED -> "parato (" + action.damage() + ")";
      case DODGED -> "schivato";
      case REST -> "riposa (+" + action.staminaRecovered() + ")";
    };
  }

  private String hitLabel(ActionOutcome action) {
    if (action.critical()) {
      return "critico (" + action.damage() + ")";
    }
    if (action.powerStrike()) {
      return "colpo potente (" + action.damage() + ")";
    }
    return "colpisce (" + action.damage() + ")";
  }

  /**
   * Combattenti vivi (per {@code round.vitals()}) che, in questo round, non compaiono fra i
   * partecipanti di alcuno scambio: il terzo uomo di uno scontro non ancora riassegnato, un
   * vincitore libero in attesa.
   */
  private List<Integer> waitingParticipantIndexes(RoundLogEntry round) {
    Set<Integer> engaged = new LinkedHashSet<>();
    for (EngagementTurn turn : round.turns()) {
      engaged.addAll(turn.participantIndexes());
    }

    List<Integer> waiting = new ArrayList<>();
    for (int index = 0; index < fighterNames.size(); index++) {
      if (!engaged.contains(index) && round.vitals().get(index).currentHealth() > 0) {
        waiting.add(index);
      }
    }
    return waiting;
  }

  /**
   * Snapshot d'inizio round, identico su ogni turno dello stesso round: {@code null} se il round
   * non ha giocato alcuno scambio, o se lo scambio non porta uno snapshot (nessun riferimento
   * disponibile per un delta in nessuno dei due casi).
   */
  private List<FighterVitals> startOfRoundVitalsOf(RoundLogEntry round) {
    if (round.turns().isEmpty()) {
      return null;
    }
    List<FighterVitals> vitals = round.turns().get(0).turn().vitals();
    return (vitals.isEmpty() ? null : vitals);
  }

  /**
   * Larghezza di colonna calcolata una sola volta dal caso peggiore ricavabile dai valori massimi
   * del roster: il nome piu' lungo (col suffisso {@code [a terra]}, sempre possibile) e, per
   * ciascun combattente, la riga di vita/stamina nel caso peggiore (valore corrente con le stesse
   * cifre del massimo, delta della larghezza massima possibile). Non dipende da alcun round: resta
   * costante per tutta la battaglia.
   */
  private int computeColumnWidth(List<FighterProfile> roster) {
    int longest = 0;
    for (FighterProfile profile : roster) {
      longest = Math.max(longest, profile.name().length() + DOWN_SUFFIX.length());
      longest = Math.max(longest, worstCaseLineLength(HEALTH_LABEL, profile.maxHealth()));
      longest = Math.max(longest, worstCaseLineLength(STAMINA_LABEL, profile.maxStamina()));
    }
    return longest;
  }

  /**
   * Lunghezza massima possibile di una riga "Etichetta: corrente/massimo (segno delta)": il
   * numeratore puo' avere al massimo le stesse cifre del massimo (il valore corrente non supera
   * mai il massimo), e il delta non puo' eccedere in cifre il massimo stesso.
   */
  private int worstCaseLineLength(String label, int maxValue) {
    int digits = Integer.toString(maxValue).length();
    int currentOverMax = 2 * digits + 1;
    int worstCaseDelta = digits + 4;
    return label.length() + currentOverMax + worstCaseDelta;
  }

  private List<String[]> blocksOf(List<Integer> indexes, List<FighterVitals> startVitals,
      List<FighterVitals> endVitals) {
    return indexes.stream().map(index -> blockOf(index, startVitals, endVitals)).toList();
  }

  private String[] blockOf(int index, List<FighterVitals> startVitals, List<FighterVitals> endVitals) {
    FighterVitals end = endVitals.get(index);
    FighterVitals start = (startVitals != null ? startVitals.get(index) : end);

    String nameLine = fighterNames.get(index) + (end.currentHealth() <= 0 ? DOWN_SUFFIX : "");
    String healthLine = HEALTH_LABEL + end.currentHealth() + "/" + end.maxHealth()
        + deltaSuffix(end.currentHealth() - start.currentHealth());
    String staminaLine = STAMINA_LABEL + end.currentStamina() + "/" + end.maxStamina()
        + deltaSuffix(end.currentStamina() - start.currentStamina());
    return new String[] {nameLine, healthLine, staminaLine};
  }

  private String deltaSuffix(int delta) {
    if (delta == 0) {
      return "";
    }
    return (delta > 0 ? " (+" : " (") + delta + ")";
  }

  /**
   * Combina il testo di sinistra e quello di destra su una riga: se il lato destro e' vuoto (la
   * colonna opposta non ha proprio un blocco a questa riga, es. il terzo uomo senza contropartita)
   * restituisce solo il testo di sinistra, senza riempimento, perche' non c'e' altra colonna da
   * allineare dopo. Se invece il lato destro esiste, entrambi i lati sono riempiti alla stessa
   * {@link #columnWidth}, cosi' la riga ha sempre la stessa lunghezza totale indipendentemente
   * dal numero di cifre di valori e delta: e' cio' che tiene ogni combattente fermo nella stessa
   * colonna round dopo round.
   */
  private String combineRow(String leftText, String rightText, String middle) {
    if (rightText.isEmpty()) {
      return leftText;
    }
    return padRight(leftText, columnWidth) + middle + padRight(rightText, columnWidth);
  }

  private String blankMiddle() {
    return " ".repeat(MIDDLE_WIDTH);
  }

  private String formulaMiddle(String label) {
    return GAP + center(label, ARROW_WIDTH) + GAP;
  }

  private String arrowMiddle(String arrow) {
    return GAP + arrow + GAP;
  }

  private String header(int roundNumber) {
    String label = " Round " + roundNumber + " ";
    int totalPadding = Math.max(0, totalWidth - label.length());
    int leftPadding = totalPadding / 2;
    int rightPadding = totalPadding - leftPadding;
    return "=".repeat(leftPadding) + label + "=".repeat(rightPadding);
  }

  private String separator() {
    StringBuilder builder = new StringBuilder();
    while (builder.length() < totalWidth) {
      builder.append("- ");
    }
    return builder.substring(0, totalWidth);
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

  private String padRight(String text, int width) {
    if (text.length() >= width) {
      return text;
    }
    return text + " ".repeat(width - text.length());
  }
}
