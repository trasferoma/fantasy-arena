package it.fantasyarena;

import it.fantasyarena.combat.Arena;
import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.factory.FighterFactory;
import it.fantasyarena.combat.io.ReplayMode;

/**
 * Punto d'ingresso dell'applicazione: genera una coppia di combattenti equi-equipaggiati
 * tramite la {@link FighterFactory} e li passa all'{@link Arena} per disputare il duello.
 */
public class Main {

  public static void main(String[] args) {
    CombatSettings settings = CombatSettings.defaults();
    FighterFactory fighterFactory = FighterFactory.withDefaultRatings(settings);
    FighterFactory.Duelists duelists = fighterFactory.createMatchedSwordWarriors();
    new Arena(settings, ReplayMode.SCREEN).run(duelists.first(), duelists.second());
  }
}
