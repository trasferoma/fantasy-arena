package it.fantasyarena.combat.io;

import java.util.List;
import java.util.Optional;

import it.fantasyarena.combat.engine.FavoriteEstimator;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.CombatOutcome;
import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.result.TurnHighlight;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * Logger che stampa il duello su console: riepilogo pre-combattimento, log turno per
 * turno con vita residua dei due contendenti, seguito dall'esito finale. La formattazione
 * testuale del turno è delegata a {@link TurnLogFormatter}, condivisa anche dal replay a pagina;
 * la scheda dei combattenti è delegata a {@link FighterCardFormatter}. Il riepilogo finale
 * aggiunge una narrazione da cronista basata sul favorito pre-scontro, stimato da
 * {@link FavoriteEstimator} (componente puro di dominio, senza RNG).
 */
public class ConsoleCombatLogger implements CombatLogger {

  private final TurnLogFormatter formatter = new TurnLogFormatter();
  private final FighterCardFormatter cardFormatter = new FighterCardFormatter();
  private final FavoriteEstimator favoriteEstimator = new FavoriteEstimator();

  @Override
  public void reportMatchup(Fighter first, Fighter second) {
    System.out.println("=== Combattenti ===\n");
    printCards(first, second);
    printPrognosis(first, second);
  }

  /**
   * Pronostico pre-battaglia, in una cornice di trattini subito dopo le schede: il favorito
   * stimato da {@link FavoriteEstimator} dai soli rating, prima che i dadi dicano la loro.
   */
  private void printPrognosis(Fighter first, Fighter second) {
    FavoriteEstimator.Verdict verdict = favoriteEstimator.assess(first, second);
    String content = "Pronostico: " + describePrognosis(verdict);
    String frame = "-".repeat(content.length());

    System.out.println();
    System.out.println(frame);
    System.out.println(content);
    System.out.println(frame);
    System.out.println();
  }

  private String describePrognosis(FavoriteEstimator.Verdict verdict) {
    return verdict.favorite()
        .map(fighter -> "il favorito è " + fighter.name() + " (" + describePrognosisReason(verdict) + ")")
        .orElse("scontro equilibrato, nessun favorito netto");
  }

  /**
   * Motivo del pronostico: il criterio che ha deciso il favorito e i valori confrontati (del
   * favorito e dell'avversario). Rating con un decimale, vita e stamina come interi.
   */
  private String describePrognosisReason(FavoriteEstimator.Verdict verdict) {
    String favoriteValue = formatBasisValue(verdict.basis(), verdict.favoriteValue());
    String opponentValue = formatBasisValue(verdict.basis(), verdict.opponentValue());
    return switch (verdict.basis()) {
      case RATING -> "attacco+difesa " + favoriteValue + " vs " + opponentValue;
      case HEALTH -> "attacco+difesa pari, più vita " + favoriteValue + " vs " + opponentValue;
      case STAMINA -> "attacco+difesa e vita pari, più stamina " + favoriteValue + " vs " + opponentValue;
      case EVEN -> "nessun vantaggio netto";
    };
  }

  private String formatBasisValue(FavoriteEstimator.Basis basis, double value) {
    if (basis == FavoriteEstimator.Basis.RATING) {
      return formatter.formatRating(value);
    }
    return Long.toString(Math.round(value));
  }

  @Override
  public void logTurn(TurnLogEntry entry) {
    formatter.format(entry).forEach(System.out::println);
  }

  @Override
  public void reportOutcome(CombatResult result, Fighter first, Fighter second) {
    System.out.println();
    System.out.println("=== Esito del duello ===");

    switch (result.outcome()) {
      case VICTORY -> printWinner("Vince", result);
      case TIMEOUT_DECISION -> printWinner("Timeout ai punti, vince", result);
      case DRAW -> System.out.println("Pareggio dopo " + result.rounds() + " turni.");
    }

    System.out.println("Stato -> " + formatter.describeVitals(result.finalVitals()));

    System.out.println();
    System.out.println("nell'ultimo scontro:");
    printCards(first, second);

    printChronicle(result, first, second);
  }

  private void printCards(Fighter first, Fighter second) {
    cardFormatter.card(1, first).forEach(System.out::println);
    System.out.println();

    cardFormatter.card(2, second).forEach(System.out::println);
    System.out.println();
  }

  private void printWinner(String label, CombatResult result) {
    String winnerName = result.winner()
        .map(Fighter::name)
        .orElseThrow(() -> new IllegalStateException("Esito con vincitore atteso ma assente"));
    System.out.println(label + ": " + winnerName + " (" + result.rounds() + " turni)");
  }

  /**
   * Narrazione da cronista che spiega il "motivo" dell'esito: il favorito pre-scontro, chi ha
   * vinto (con l'eventuale ribaltone rispetto al pronostico) e almeno un evento notevole
   * tracciato durante il duello, se presente.
   */
  private void printChronicle(CombatResult result, Fighter first, Fighter second) {
    Optional<Fighter> favorite = favoriteEstimator.favorite(first, second);
    System.out.println(describeFavorite(favorite));
    System.out.println(describeVerdict(result, favorite));
    describeNotableEvent(result.log()).ifPresent(System.out::println);
  }

  private String describeFavorite(Optional<Fighter> favorite) {
    return favorite
        .map(fighter -> "Favorito alla vigilia: " + fighter.name() + ".")
        .orElse("Alla vigilia equilibrato, nessun favorito netto.");
  }

  private String describeVerdict(CombatResult result, Optional<Fighter> favorite) {
    if (result.outcome() == CombatOutcome.DRAW) {
      return "Pareggio: pronostico né confermato né smentito.";
    }

    String winnerName = result.winner()
        .map(Fighter::name)
        .orElseThrow(() -> new IllegalStateException("Esito con vincitore atteso ma assente"));
    if (favorite.isEmpty()) {
      return "Vince " + winnerName + ".";
    }

    boolean upset = !favorite.get().name().equals(winnerName);
    return (upset
        ? "Vince " + winnerName + ": ribaltone rispetto al pronostico!"
        : "Vince " + winnerName + ": pronostico rispettato.");
  }

  private Optional<String> describeNotableEvent(List<TurnLogEntry> log) {
    return log.stream()
        .filter(entry -> !entry.highlights().isEmpty())
        .findFirst()
        .map(this::describeNotableEntry);
  }

  private String describeNotableEntry(TurnLogEntry entry) {
    String attackerName = entry.initiative().chosenName();
    String label = describeHighlightLabel(entry.highlights());
    return "Da ricordare: " + label + " di " + attackerName + " al turno " + entry.turnNumber() + ".";
  }

  /**
   * Un turno può avere più highlight compresenti (es. un 20 naturale è anche un critico): qui
   * si sceglie un'unica etichetta dominante per la citazione, con precedenza {@code KNOCKOUT >
   * PERFECT_HIT > CRITICAL > POWER_STRIKE > HEAVY_BLOW}.
   */
  private String describeHighlightLabel(List<TurnHighlight> highlights) {
    if (highlights.contains(TurnHighlight.KNOCKOUT)) {
      return "il colpo di grazia";
    }
    if (highlights.contains(TurnHighlight.PERFECT_HIT)) {
      return "il colpo perfetto";
    }
    if (highlights.contains(TurnHighlight.CRITICAL)) {
      return "il colpo critico";
    }
    if (highlights.contains(TurnHighlight.POWER_STRIKE)) {
      return "il colpo potente";
    }
    return "il colpo pesante";
  }
}
