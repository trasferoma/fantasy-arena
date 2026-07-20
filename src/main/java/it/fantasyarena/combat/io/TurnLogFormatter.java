package it.fantasyarena.combat.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import it.fantasyarena.combat.result.FighterVitals;
import it.fantasyarena.combat.result.InitiativeBreakdown;
import it.fantasyarena.combat.result.InitiativeOverride;
import it.fantasyarena.combat.result.InitiativeReport;
import it.fantasyarena.combat.result.StaminaChange;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * Formatta in righe di testo (italiano, pronte per la stampa) il contenuto di un
 * {@link TurnLogEntry}: stato dei combattenti, eventuale report d'iniziativa, descrizione
 * dell'azione e variazioni di Stamina. Puro (nessun I/O): condiviso sia dal replay lineare
 * su console sia da quello a pagina, cosi' le due modalita' restano testualmente identiche.
 */
public class TurnLogFormatter {

  public List<String> format(TurnLogEntry entry) {
    List<String> lines = new ArrayList<>();
    lines.add("Turno " + entry.turnNumber() + ":" + describeVitalsSuffix(entry.vitals()));

    if (entry.initiative() != null) {
      lines.addAll(formatInitiative(entry.initiative()));
    }

    lines.add("         " + entry.description());

    if (!entry.staminaChanges().isEmpty()) {
      lines.add("         Stamina -> " + describeStaminaChanges(entry.staminaChanges()));
    }

    return lines;
  }

  /**
   * Versione concisa di {@link #format}, usata dal replay a pagina: solo l'esito
   * dell'iniziativa (senza breakdown per-combattente) e la descrizione dell'azione, senza il
   * prefisso "Turno N:" né lo stato dei combattenti né le variazioni di Stamina. Sotto override
   * (nessun test a punteggio eseguito) la riga del vincitore per punteggio è omessa.
   */
  public List<String> formatCompact(TurnLogEntry entry) {
    List<String> lines = new ArrayList<>();

    if (entry.initiative() != null) {
      InitiativeReport initiative = entry.initiative();
      if (!initiative.breakdowns().isEmpty()) {
        lines.add("-> vince l'iniziativa (punteggio): " + describeScoreWinner(initiative));
      }
      lines.add("-> primo ad agire: " + initiative.chosenName() + describeActorOverride(initiative.override()));
    }

    lines.add(entry.description());
    return lines;
  }

  public String describeVitals(List<FighterVitals> vitals) {
    return vitals.stream()
        .map(vital -> vital.name() + ": vita " + vital.currentHealth() + "/" + vital.maxHealth()
            + ", stamina " + vital.currentStamina() + "/" + vital.maxStamina())
        .collect(Collectors.joining(" | "));
  }

  public String formatRating(double rating) {
    return String.format(Locale.ITALY, "%.1f", rating);
  }

  private String describeVitalsSuffix(List<FighterVitals> vitals) {
    if (vitals.isEmpty()) {
      return "";
    }
    return " Stato -> " + describeVitals(vitals);
  }

  /**
   * Sotto override (il turno precedente ha prodotto una schivata riuscita o un riposo) il test
   * a punteggio non viene eseguito: né i breakdown per-combattente né la riga del vincitore per
   * punteggio hanno senso, quindi vengono omessi e resta solo chi agisce.
   */
  private List<String> formatInitiative(InitiativeReport initiative) {
    List<String> lines = new ArrayList<>();
    lines.add("         Iniziativa a inizio turno " + describeOverride(initiative.override()) + ":");

    if (initiative.breakdowns().isEmpty()) {
      lines.add("         -> primo ad agire: " + initiative.chosenName()
          + describeActorOverride(initiative.override()));
      return lines;
    }

    for (InitiativeBreakdown breakdown : initiative.breakdowns()) {
      lines.add("           " + describeBreakdown(breakdown));
    }
    lines.add("         -> vince l'iniziativa (punteggio): " + describeScoreWinner(initiative));
    lines.add("         -> primo ad agire: " + initiative.chosenName()
        + describeActorOverride(initiative.override()));
    return lines;
  }

  private String describeScoreWinner(InitiativeReport initiative) {
    double firstTotal = initiative.breakdowns().get(0).total();
    double secondTotal = initiative.breakdowns().get(1).total();
    double winnerTotal = Math.max(firstTotal, secondTotal);
    double loserTotal = Math.min(firstTotal, secondTotal);
    return initiative.scoreWinnerName() + " (" + formatRating(winnerTotal) + " vs " + formatRating(loserTotal) + ")";
  }

  private String describeOverride(InitiativeOverride override) {
    return switch (override) {
      case NONE -> "(formula punteggio)";
      case DODGE_STEAL -> "(schivata: ruba il tempo)";
      case REST_YIELD -> "(riposo: cede il tempo)";
    };
  }

  private String describeActorOverride(InitiativeOverride override) {
    return switch (override) {
      case NONE -> "";
      case DODGE_STEAL -> " (la schivata ruba il tempo)";
      case REST_YIELD -> " (il riposo cede il tempo)";
    };
  }

  private String describeBreakdown(InitiativeBreakdown breakdown) {
    return breakdown.name() + ": stamina " + formatRating(breakdown.staminaComponent())
        + " [" + breakdown.currentStamina() + "/" + breakdown.maxStamina() + "]"
        + " + agilità " + formatRating(breakdown.agilityComponent()) + " [" + breakdown.agility() + "]"
        + " + int " + formatRating(breakdown.intelligenceComponent()) + " [" + breakdown.intelligence() + "]"
        + " + jitter " + formatRating(breakdown.jitterComponent()) + " [dado " + breakdown.jitterValue() + "]"
        + " = " + formatRating(breakdown.total());
  }

  private String describeStaminaChanges(List<StaminaChange> staminaChanges) {
    return staminaChanges.stream()
        .map(change -> change.name() + ": -" + change.consumed() + " consumata, +" + change.recovered()
            + " recuperata")
        .collect(Collectors.joining(" | "));
  }
}
