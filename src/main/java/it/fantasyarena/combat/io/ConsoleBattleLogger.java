package it.fantasyarena.combat.io;

import java.util.List;
import java.util.Locale;

import it.fantasyarena.combat.battle.BattleResult;
import it.fantasyarena.combat.battle.BattleSetup;
import it.fantasyarena.combat.battle.EngagementTurn;
import it.fantasyarena.combat.battle.RoundLogEntry;
import it.fantasyarena.combat.battle.Team;
import it.fantasyarena.combat.battle.TeamScore;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.Scorecard;

/**
 * Logger testuale, in stile log, per la battaglia NvN: stampa gli schieramenti, il log round per
 * round e l'esito finale, senza attesa dell'INVIO e senza la messa in pagina a schermo del
 * duello 1v1. Riusa {@link TurnLogFormatter} per la formattazione compatta dello scambio e
 * {@link FighterCardFormatter} per le schede dei combattenti, cosi' le due modalita' di
 * presentazione restano testualmente coerenti. Una sola classe concreta, senza interfaccia
 * dedicata: a differenza di {@link CombatLogger} (un solo implementatore per ragioni storiche),
 * qui non c'e' un secondo utilizzo che giustifichi l'astrazione.
 */
public class ConsoleBattleLogger {

  private final TurnLogFormatter turnFormatter = new TurnLogFormatter();
  private final FighterCardFormatter cardFormatter = new FighterCardFormatter();

  /**
   * Stampa gli schieramenti: intestazione, poi per ogni squadra il nome e la scheda di ogni
   * membro. I combattenti sono numerati progressivamente sull'intera battaglia (non ricominciando
   * da 1 a ogni squadra), cosi' l'indice resta un identificatore stabile nel log.
   */
  public void reportSetup(BattleSetup setup) {
    System.out.println("=== Schieramenti ===");

    int fighterIndex = 1;
    for (Team team : setup.teams()) {
      System.out.println();
      System.out.println(team.name() + ":");
      for (Fighter member : team.members()) {
        cardFormatter.card(fighterIndex, member).forEach(System.out::println);
        System.out.println();
        fighterIndex++;
      }
    }
  }

  /**
   * Stampa un round: intestazione, uno scambio per scontro attivo (identificato dallo scontro e
   * dai due contendenti, seguito dalla formattazione compatta dello scambio), le eventuali note
   * di round (riassegnazioni di vincitori liberi) e lo stato finale di round di ogni combattente.
   */
  public void logRound(RoundLogEntry round) {
    System.out.println("--- Round " + round.roundNumber() + " ---");

    round.turns().forEach(this::logEngagementTurn);
    round.events().forEach(System.out::println);

    System.out.println("Stato -> " + turnFormatter.describeVitals(round.vitals()));
  }

  private void logEngagementTurn(EngagementTurn turn) {
    System.out.println("[scontro " + turn.engagementId() + "] " + turn.attackerName() + " -> " + turn.targetName());
    turnFormatter.formatCompact(turn.turn()).forEach(line -> System.out.println("  " + line));
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
