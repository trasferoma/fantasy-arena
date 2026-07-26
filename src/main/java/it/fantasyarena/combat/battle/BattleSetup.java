package it.fantasyarena.combat.battle;

import java.util.ArrayList;
import java.util.List;

import it.fantasyarena.combat.model.Fighter;

/**
 * Configurazione iniziale di una battaglia: l'elenco delle squadre partecipanti. V1 supporta
 * esattamente 2 squadre: con 3 o più squadre le regole di riassegnazione dei vincitori liberi e
 * di fine battaglia richiederebbero un concetto di alleanza fra squadre che questa fase non
 * introduce.
 */
public record BattleSetup(List<Team> teams) {

  private static final int TEAM_COUNT_V1 = 2;

  public BattleSetup {
    validateTeamCount(teams);
    teams = List.copyOf(teams);
    validateNoDuplicateFighters(teams);
  }

  /**
   * Caso degenere: il duello storico 1v1, come due squadre da un solo membro ciascuna.
   */
  public static BattleSetup duel(Fighter first, Fighter second) {
    Team firstTeam = new Team(0, "Squadra 1", List.of(first));
    Team secondTeam = new Team(1, "Squadra 2", List.of(second));
    return new BattleSetup(List.of(firstTeam, secondTeam));
  }

  /**
   * Costruisce le squadre a partire dai roster forniti, con nome {@code "Squadra " + (indice + 1)}
   * e indice progressivo da 0.
   */
  public static BattleSetup of(List<List<Fighter>> teamRosters) {
    if (teamRosters == null) {
      throw new IllegalArgumentException("teamRosters must not be null");
    }

    List<Team> teams = new ArrayList<>();
    for (int i = 0; i < teamRosters.size(); i++) {
      teams.add(new Team(i, "Squadra " + (i + 1), teamRosters.get(i)));
    }
    return new BattleSetup(teams);
  }

  private static void validateTeamCount(List<Team> teams) {
    if (teams == null || teams.size() != TEAM_COUNT_V1) {
      int actualSize = (teams == null) ? -1 : teams.size();
      throw new IllegalArgumentException(
          "teams must contain exactly " + TEAM_COUNT_V1 + " teams in v1 (3+ teams require alliance rules "
              + "not yet supported), was: " + actualSize);
    }
  }

  /**
   * Verifica che nessun combattente compaia più di una volta, nella stessa squadra o fra
   * squadre diverse: confronto sempre per identità. NON valida l'unicità dei nomi: due
   * combattenti generati a runtime possono chiamarsi allo stesso modo (collisione plausibile del
   * generatore di nomi), e il motore correla i combattenti per identità e non per nome, quindi
   * non ne soffre. Rifiutare per nome duplicato farebbe fallire la demo senza un motivo tecnico
   * reale: è una scelta deliberata, non una svista.
   */
  private static void validateNoDuplicateFighters(List<Team> teams) {
    List<Fighter> seen = new ArrayList<>();
    for (Team team : teams) {
      for (Fighter member : team.members()) {
        if (FighterIdentity.containsSame(seen, member)) {
          throw new IllegalArgumentException(
              "a fighter must not appear more than once across teams, duplicate: " + member.name());
        }
        seen.add(member);
      }
    }
  }
}
