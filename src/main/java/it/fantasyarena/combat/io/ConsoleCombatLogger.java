package it.fantasyarena.combat.io;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.IntrinsicRatings;
import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.result.FighterVitals;
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
    System.out.println("Turno " + entry.turnNumber() + ": " + entry.description());

    if (!entry.vitals().isEmpty()) {
      System.out.println("         Vita -> " + describeVitals(entry.vitals()));
    }
  }

  private String describeVitals(List<FighterVitals> vitals) {
    return vitals.stream()
        .map(vital -> vital.name() + " " + vital.currentHealth() + "/" + vital.maxHealth())
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
  }

  private void printWinner(String label, CombatResult result) {
    String winnerName = result.winner()
        .map(Fighter::name)
        .orElseThrow(() -> new IllegalStateException("Esito con vincitore atteso ma assente"));
    System.out.println(label + ": " + winnerName + " (" + result.rounds() + " turni)");
  }
}
