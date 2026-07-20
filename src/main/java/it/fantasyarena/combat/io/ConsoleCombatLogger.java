package it.fantasyarena.combat.io;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.IntrinsicRatings;
import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.result.FighterVitals;
import it.fantasyarena.combat.result.InitiativeBreakdown;
import it.fantasyarena.combat.result.InitiativeOverride;
import it.fantasyarena.combat.result.InitiativeReport;
import it.fantasyarena.combat.result.StaminaChange;
import it.fantasyarena.combat.result.TurnLogEntry;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;

/**
 * Logger che stampa il duello su console: riepilogo pre-combattimento, log turno per
 * turno con vita residua dei due contendenti, seguito dall'esito finale.
 */
public class ConsoleCombatLogger implements CombatLogger {

  @Override
  public void reportMatchup(Fighter first, Fighter second) {
    System.out.println("=== Combattenti ===");
    printFighterSummary(1, first);
    printFighterSummary(2, second);
  }

  private void printFighterSummary(int index, Fighter fighter) {
    CharacterResult character = fighter.character();
    WeaponResult weapon = fighter.weapon();
    ArmourResult armour = fighter.armour();
    IntrinsicRatings ratings = fighter.ratings();

    System.out.println("[" + index + "] " + fighter.name() + " - " + character.race() + " "
        + character.characterClass());
    System.out.println("    Arma: " + weapon.weapon() + " (" + weapon.rarity() + ") attacco " + weapon.attack()
        + " | Armatura: " + armour.armour() + " (" + armour.rarity() + ") difesa " + armour.defense());
    System.out.println("    Vita " + ratings.maxHealth() + " | Stamina " + ratings.maxStamina()
        + " | ATK " + formatRating(ratings.offensiveRating()) + " | DEF " + formatRating(ratings.defensiveRating()));
  }

  private String formatRating(double rating) {
    return String.format(Locale.ITALY, "%.1f", rating);
  }

  @Override
  public void logTurn(TurnLogEntry entry) {
    System.out.println("Turno " + entry.turnNumber() + ":" + describeVitalsSuffix(entry.vitals()));

    if (entry.initiative() != null) {
      printInitiative(entry.initiative());
    }

    System.out.println("         " + entry.description());

    if (!entry.staminaChanges().isEmpty()) {
      System.out.println("         Stamina -> " + describeStaminaChanges(entry.staminaChanges()));
    }
  }

  private String describeVitalsSuffix(List<FighterVitals> vitals) {
    if (vitals.isEmpty()) {
      return "";
    }
    return " Stato -> " + describeVitals(vitals);
  }

  private String describeVitals(List<FighterVitals> vitals) {
    return vitals.stream()
        .map(vital -> vital.name() + ": vita " + vital.currentHealth() + "/" + vital.maxHealth()
            + ", stamina " + vital.currentStamina() + "/" + vital.maxStamina())
        .collect(Collectors.joining(" | "));
  }

  private void printInitiative(InitiativeReport initiative) {
    System.out.println("         Iniziativa a inizio turno " + describeOverride(initiative.override()) + ":");
    for (InitiativeBreakdown breakdown : initiative.breakdowns()) {
      System.out.println("           " + describeBreakdown(breakdown));
    }
    System.out.println("         -> vince l'iniziativa (punteggio): " + describeScoreWinner(initiative));
    System.out.println("         -> primo ad agire: " + initiative.chosenName()
        + describeActorOverride(initiative.override()));
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

  @Override
  public void reportOutcome(CombatResult result) {
    System.out.println();
    System.out.println("=== Esito del duello ===");

    switch (result.outcome()) {
      case VICTORY -> printWinner("Vince", result);
      case TIMEOUT_DECISION -> printWinner("Timeout ai punti, vince", result);
      case DRAW -> System.out.println("Pareggio dopo " + result.rounds() + " turni.");
    }

    System.out.println("Stato -> " + describeVitals(result.finalVitals()));
  }

  private void printWinner(String label, CombatResult result) {
    String winnerName = result.winner()
        .map(Fighter::name)
        .orElseThrow(() -> new IllegalStateException("Esito con vincitore atteso ma assente"));
    System.out.println(label + ": " + winnerName + " (" + result.rounds() + " turni)");
  }
}
