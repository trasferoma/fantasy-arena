package it.fantasyarena.combat.battle;

import java.util.List;
import java.util.Optional;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.CombatResult;
import it.fantasyarena.combat.result.TurnLogEntry;

/**
 * Appiattisce il {@link BattleResult} di una battaglia degenere (due squadre da un solo membro)
 * nella vista storica {@link CombatResult} del duello 1v1. Non e' una conversione generica: su
 * una battaglia non degenere solleva {@link IllegalStateException}.
 */
public final class DuelResultAdapter {

  private static final int EXPECTED_FIGHTER_COUNT = 2;
  private static final int EXPECTED_TEAM_MEMBER_COUNT = 1;
  private static final int MAX_EXCHANGES_PER_ROUND = 1;

  private DuelResultAdapter() {
  }

  public static CombatResult toCombatResult(BattleResult battle) {
    validateDegenerateBattle(battle);

    Optional<Fighter> winner = battle.winningTeam().map(team -> team.members().get(0));
    List<TurnLogEntry> log = flattenLog(battle.roundLog());

    return new CombatResult(battle.outcome(), winner, battle.rounds(), log, battle.finalVitals(),
        battle.scorecards());
  }

  private static void validateDegenerateBattle(BattleResult battle) {
    validateFighterCount(battle);
    battle.winningTeam().ifPresent(DuelResultAdapter::validateSingleMemberTeam);
    validateAtMostOneExchangePerRound(battle.roundLog());
  }

  /**
   * Con esattamente 2 squadre non vuote (invariante di {@link BattleSetup}), 2 combattenti
   * totali implicano necessariamente 1 membro per squadra: e' la firma della battaglia degenere.
   */
  private static void validateFighterCount(BattleResult battle) {
    int fighterCount = battle.finalVitals().size();
    if (fighterCount != EXPECTED_FIGHTER_COUNT) {
      throw new IllegalStateException(
          "DuelResultAdapter richiede una battaglia degenere di " + EXPECTED_FIGHTER_COUNT
              + " combattenti totali (due squadre da un membro), trovati: " + fighterCount);
    }
  }

  private static void validateSingleMemberTeam(Team team) {
    int memberCount = team.members().size();
    if (memberCount != EXPECTED_TEAM_MEMBER_COUNT) {
      throw new IllegalStateException(
          "DuelResultAdapter richiede squadre da " + EXPECTED_TEAM_MEMBER_COUNT
              + " membro ciascuna, la squadra vincitrice '" + team.name() + "' ne ha " + memberCount);
    }
  }

  private static void validateAtMostOneExchangePerRound(List<RoundLogEntry> roundLog) {
    for (RoundLogEntry round : roundLog) {
      int exchangeCount = round.turns().size();
      if (exchangeCount > MAX_EXCHANGES_PER_ROUND) {
        throw new IllegalStateException(
            "DuelResultAdapter richiede al massimo " + MAX_EXCHANGES_PER_ROUND
                + " scambio per round, trovati " + exchangeCount + " scambi nel round " + round.roundNumber());
      }
    }
  }

  private static List<TurnLogEntry> flattenLog(List<RoundLogEntry> roundLog) {
    return roundLog.stream()
        .flatMap(round -> round.turns().stream())
        .map(EngagementTurn::turn)
        .toList();
  }
}
