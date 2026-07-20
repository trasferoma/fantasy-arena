package it.fantasyarena.combat.io;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.IntrinsicRatings;
import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.result.TurnLogEntry;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;

/**
 * Logger che stampa il duello su console: riepilogo pre-combattimento, log turno per
 * turno con vita residua dei due contendenti, seguito dall'esito finale. La formattazione
 * testuale del turno è delegata a {@link TurnLogFormatter}, condivisa anche dal replay a pagina.
 */
public class ConsoleCombatLogger implements CombatLogger {

  private final TurnLogFormatter formatter = new TurnLogFormatter();

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
        + " | ATK " + formatter.formatRating(ratings.offensiveRating()) + " | DEF "
        + formatter.formatRating(ratings.defensiveRating()));
  }

  @Override
  public void logTurn(TurnLogEntry entry) {
    formatter.format(entry).forEach(System.out::println);
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

    System.out.println("Stato -> " + formatter.describeVitals(result.finalVitals()));
  }

  private void printWinner(String label, CombatResult result) {
    String winnerName = result.winner()
        .map(Fighter::name)
        .orElseThrow(() -> new IllegalStateException("Esito con vincitore atteso ma assente"));
    System.out.println(label + ": " + winnerName + " (" + result.rounds() + " turni)");
  }
}
