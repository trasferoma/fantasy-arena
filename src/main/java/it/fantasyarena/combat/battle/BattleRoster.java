package it.fantasyarena.combat.battle;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import it.fantasyarena.combat.model.Fighter;

/**
 * Vista di sola lettura sull'insieme dei combattenti di una battaglia, derivata da un elenco di
 * {@link Team}. {@link #all()} fissa l'ordine di roster: prima i membri della squadra 0, poi
 * quelli della squadra 1, e così via, nell'ordine in cui compaiono nel roster di ciascuna
 * squadra. È il riferimento di ogni tie-break successivo del motore (piani di scontro,
 * assegnazione dei vincitori liberi, ...). La correlazione combattente -> squadra usa una
 * {@link IdentityHashMap}: due combattenti possono avere lo stesso nome, quindi ogni ricerca è
 * per identità, mai per {@code equals} o per nome.
 */
public final class BattleRoster {

  private final List<Team> teams;
  private final List<Fighter> all;
  private final Map<Fighter, Team> teamByFighter;

  private BattleRoster(List<Team> teams) {
    this.teams = teams;
    this.all = buildAll(teams);
    this.teamByFighter = buildTeamByFighter(teams);
  }

  public static BattleRoster of(List<Team> teams) {
    if (teams == null || teams.isEmpty()) {
      throw new IllegalArgumentException("teams must not be null or empty, was: " + teams);
    }
    return new BattleRoster(List.copyOf(teams));
  }

  public List<Team> teams() {
    return teams;
  }

  /**
   * Tutti i combattenti della battaglia, nell'ordine di roster: membri della squadra 0, poi
   * della squadra 1, e così via.
   */
  public List<Fighter> all() {
    return all;
  }

  /**
   * Combattenti ancora vivi, nell'ordine di roster.
   */
  public List<Fighter> living() {
    return all.stream()
        .filter(fighter -> !fighter.isDefeated())
        .toList();
  }

  /**
   * Squadra di appartenenza di {@code fighter}, per identità.
   */
  public Team teamOf(Fighter fighter) {
    Team team = teamByFighter.get(fighter);
    if (team == null) {
      throw new IllegalArgumentException("fighter is not part of this roster: " + fighter.name());
    }
    return team;
  }

  /**
   * Nemici ancora vivi di {@code fighter} (combattenti di squadre diverse dalla sua), nell'ordine
   * di roster.
   */
  public List<Fighter> livingEnemiesOf(Fighter fighter) {
    Team ownTeam = teamOf(fighter);
    return all.stream()
        .filter(candidate -> !candidate.isDefeated() && teamByFighter.get(candidate) != ownTeam)
        .toList();
  }

  /**
   * Vero sse {@code a} e {@code b} appartengono alla stessa squadra.
   */
  public boolean areAllies(Fighter a, Fighter b) {
    return teamOf(a) == teamOf(b);
  }

  /**
   * Squadre con almeno un membro vivo, nell'ordine dei rispettivi indici.
   */
  public List<Team> teamsStillStanding() {
    return teams.stream()
        .filter(team -> !team.isEliminated())
        .toList();
  }

  private static List<Fighter> buildAll(List<Team> teams) {
    return teams.stream()
        .flatMap(team -> team.members().stream())
        .toList();
  }

  private static Map<Fighter, Team> buildTeamByFighter(List<Team> teams) {
    Map<Fighter, Team> map = new IdentityHashMap<>();
    for (Team team : teams) {
      for (Fighter member : team.members()) {
        map.put(member, team);
      }
    }
    return map;
  }
}
