package it.fantasyarena;

import java.util.List;

import it.fantasyarena.combat.Arena;
import it.fantasyarena.combat.battle.BattleSetup;
import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.factory.FighterFactory;
import it.fantasyarena.combat.io.FactionSizePrompt;
import it.fantasyarena.combat.io.ReplayMode;
import it.fantasyarena.combat.model.Fighter;

/**
 * Punto d'ingresso dell'applicazione: chiede all'utente quanti combattenti compongono ciascuna
 * fazione, genera in un'unica chiamata alla {@link FighterFactory} i combattenti di entrambe (i
 * nomi restano univoci sull'intera battaglia, non solo dentro una fazione) e li affida
 * all'{@link Arena}. Con esattamente un combattente per fazione (1 vs 1) usa il percorso a
 * schermo storico del duello; con qualunque altra numerosità (NvN) usa il log testuale round per
 * round.
 */
public class Main {

  private static final int DEFAULT_FACTION_SIZE = 1;

  public static void main(String[] args) {
    CombatSettings settings = CombatSettings.defaults();
    FactionSizePrompt sizePrompt = new FactionSizePrompt();

    int sizeA = sizePrompt.askFighterCount("A", DEFAULT_FACTION_SIZE);
    int sizeB = sizePrompt.askFighterCount("B", DEFAULT_FACTION_SIZE);

    FighterFactory fighterFactory = FighterFactory.withDefaultRatings(settings);
    List<Fighter> fighters = fighterFactory.createMatchedSwordWarriors(sizeA + sizeB);
    List<Fighter> teamA = fighters.subList(0, sizeA);
    List<Fighter> teamB = fighters.subList(sizeA, sizeA + sizeB);

    Arena arena = new Arena(settings, ReplayMode.SCREEN);
    if (sizeA == 1 && sizeB == 1) {
      arena.run(teamA.getFirst(), teamB.getFirst());
    } else {
      arena.runBattle(BattleSetup.of(List.of(teamA, teamB)));
    }
  }
}
