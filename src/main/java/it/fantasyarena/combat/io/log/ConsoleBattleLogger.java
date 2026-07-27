package it.fantasyarena.combat.io.log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import it.fantasyarena.combat.io.render.BattleSceneRenderer;
import it.fantasyarena.combat.io.render.FighterCardFormatter;
import it.fantasyarena.combat.io.render.FighterProfile;
import it.fantasyarena.combat.io.render.TurnLogFormatter;
import it.fantasycombatsystem.battle.BattleResult;
import it.fantasycombatsystem.battle.BattleSetup;
import it.fantasycombatsystem.battle.RoundLogEntry;
import it.fantasycombatsystem.battle.Team;
import it.fantasycombatsystem.battle.TeamScore;
import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.result.Scorecard;

/**
 * Logger testuale per la battaglia NvN: stampa gli schieramenti, la scena ASCII round per round
 * (delegata a {@link BattleSceneRenderer}) e l'esito finale, senza attesa dell'INVIO (a carico del
 * chiamante, fra un round e il successivo) e senza la messa in pagina a schermo del duello 1v1.
 * Riusa {@link TurnLogFormatter#describeVitals} per lo stato finale e {@link FighterCardFormatter}
 * per le schede dei combattenti, cosi' le due modalita' di presentazione restano testualmente
 * coerenti sull'esito. Una sola classe concreta, senza interfaccia dedicata: a differenza di
 * {@link CombatLogger} (un solo implementatore per ragioni storiche), qui non c'e' un secondo
 * utilizzo che giustifichi l'astrazione.
 */
public class ConsoleBattleLogger {

  private final TurnLogFormatter turnFormatter = new TurnLogFormatter();
  private final FighterCardFormatter cardFormatter = new FighterCardFormatter();

  private BattleSceneRenderer sceneRenderer;

  /**
   * Stampa gli schieramenti: intestazione, poi per ogni squadra il nome e la scheda di ogni
   * membro. I combattenti sono numerati progressivamente sull'intera battaglia (non ricominciando
   * da 1 a ogni squadra), cosi' l'indice resta un identificatore stabile nel log. Costruisce anche
   * il {@link BattleSceneRenderer} usato da {@link #logRound}, una sola volta per l'intera
   * battaglia: le larghezze di colonna si calcolano cosi' dai valori massimi del roster e restano
   * costanti round dopo round.
   */
  public void reportSetup(BattleSetup setup) {
    System.out.println("=== Schieramenti ===");

    List<FighterProfile> roster = new ArrayList<>();
    int fighterIndex = 1;
    for (Team team : setup.teams()) {
      System.out.println();
      System.out.println(team.name() + ":");
      for (Fighter member : team.members()) {
        cardFormatter.card(fighterIndex, member).forEach(System.out::println);
        System.out.println();
        roster.add(new FighterProfile(member.name(), team.index(), member.ratings().maxHealth(),
            member.ratings().maxStamina()));
        fighterIndex++;
      }
    }
    this.sceneRenderer = new BattleSceneRenderer(roster);
  }

  /**
   * Stampa la scena ASCII di un round: fazione 0 a sinistra, fazione 1 a destra, una freccia
   * dall'attaccante al bersaglio per scontro. Delega interamente a {@link BattleSceneRenderer},
   * costruito da {@link #reportSetup}.
   */
  public void logRound(RoundLogEntry round) {
    sceneRenderer.renderRound(round).forEach(System.out::println);
    System.out.println();
  }

  /**
   * Stampa l'esito finale della battaglia: intestazione, poi l'esito (con squadra vincitrice
   * quando prevista), l'eventuale dettaglio a punti e lo stato finale di ogni combattente.
   */
  public void reportOutcome(BattleResult result) {
    System.out.println();
    System.out.println("=== Esito della battaglia ===");

    switch (result.outcome()) {
      case VICTORY -> printVictory(result);
      case TIMEOUT_DECISION -> printTimeoutDecision(result);
      case DRAW -> System.out.println("Pareggio dopo " + result.rounds() + " round.");
    }

    printTeamScores(result.teamScores());
    printScoreDetails(result.scorecards());

    System.out.println("Stato -> " + turnFormatter.describeVitals(result.finalVitals()));
  }

  private void printVictory(BattleResult result) {
    String winningTeamName = winningTeamNameOrThrow(result, "VICTORY");
    System.out.println("Vince: " + winningTeamName + " (" + result.rounds() + " round)");
  }

  private void printTimeoutDecision(BattleResult result) {
    String winningTeamName = winningTeamNameOrThrow(result, "TIMEOUT_DECISION");
    System.out.println("Timeout ai punti, vince: " + winningTeamName + " (" + result.rounds() + " round)");
  }

  private String winningTeamNameOrThrow(BattleResult result, String outcomeLabel) {
    return result.winningTeam()
        .map(Team::name)
        .orElseThrow(
            () -> new IllegalStateException("Esito " + outcomeLabel + " con squadra vincitrice attesa ma assente"));
  }

  private void printTeamScores(List<TeamScore> teamScores) {
    if (teamScores.isEmpty()) {
      return;
    }

    System.out.println();
    System.out.println("Punteggi di squadra:");
    teamScores.forEach(score -> System.out.println("  " + score.teamName() + ": " + score.total()));
  }

  /**
   * Dettaglio del calcolo a punti che ha deciso l'esito per timeout (o pareggio): una riga per
   * combattente, con il punteggio di ogni voce e il totale. Assente su {@code VICTORY} (nessuna
   * scorecard). Sullo stampo di {@code ConsoleCombatLogger.printScoreDetails}.
   */
  private void printScoreDetails(List<Scorecard> scorecards) {
    if (scorecards.isEmpty()) {
      return;
    }

    System.out.println();
    System.out.println("Decisione ai punti:");
    scorecards.forEach(scorecard -> System.out.println("  " + describeScorecard(scorecard)));
  }

  private String describeScorecard(Scorecard scorecard) {
    return scorecard.fighterName() + ": salute +" + scorecard.healthPoints()
        + " (" + formatPercent(scorecard.healthRatio()) + " vs " + formatPercent(scorecard.opponentHealthRatio()) + ")"
        + ", colpi a segno " + scorecard.hitsLanded() + "x" + scorecard.weights().hitLanded()
        + "=" + scorecard.hitPoints()
        + ", parate " + scorecard.parries() + "x" + scorecard.weights().parry() + "=" + scorecard.parryPoints()
        + ", schivate " + scorecard.dodges() + "x" + scorecard.weights().dodge() + "=" + scorecard.dodgePoints()
        + "  ->  " + scorecard.total();
  }

  private String formatPercent(double ratio) {
    return String.format(Locale.ITALY, "%.0f%%", ratio * 100.0);
  }
}
