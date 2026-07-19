package it.fantasyarena;

import it.fantasyarena.combat.Arena;
import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.factory.FighterFactory;
import it.fantasyarena.combat.model.Fighter;

/**
 * Punto d'ingresso dell'applicazione: genera i due combattenti tramite la
 * {@link FighterFactory} e li passa all'{@link Arena} per disputare il duello.
 */
public class Main {

  public static void main(String[] args) {
    CombatSettings settings = CombatSettings.defaults();
    FighterFactory fighterFactory = FighterFactory.withDefaultRatings(settings);
    Fighter first = fighterFactory.createSwordWarrior();
    Fighter second = fighterFactory.createSwordWarrior();
    new Arena(settings).run(first, second);
  }
}
