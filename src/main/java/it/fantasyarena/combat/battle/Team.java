package it.fantasyarena.combat.battle;

import java.util.List;

import it.fantasyarena.combat.model.Fighter;

/**
 * Una squadra della battaglia: indice progressivo, nome e roster di combattenti. La lista dei
 * membri è copiata difensivamente in costruzione. L'appartenenza dei singoli {@link Fighter} va
 * sempre verificata per identità, mai per {@code equals} né per nome: due combattenti generati
 * possono chiamarsi allo stesso modo.
 */
public record Team(int index, String name, List<Fighter> members) {

  private static final int MIN_INDEX = 0;

  public Team {
    validateIndex(index);
    validateName(name);
    validateMembers(members);
    members = List.copyOf(members);
  }

  /**
   * Membri ancora vivi, nell'ordine del roster della squadra.
   */
  public List<Fighter> livingMembers() {
    return members.stream()
        .filter(member -> !member.isDefeated())
        .toList();
  }

  /**
   * Vero sse nessun membro della squadra è ancora vivo.
   */
  public boolean isEliminated() {
    return livingMembers().isEmpty();
  }

  /**
   * Vero sse {@code fighter} appartiene a questa squadra, per identità.
   */
  public boolean contains(Fighter fighter) {
    for (Fighter member : members) {
      if (member == fighter) {
        return true;
      }
    }
    return false;
  }

  private static void validateIndex(int index) {
    if (index < MIN_INDEX) {
      throw new IllegalArgumentException("index must be >= " + MIN_INDEX + ", was: " + index);
    }
  }

  private static void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be null or blank, was: " + name);
    }
  }

  private static void validateMembers(List<Fighter> members) {
    if (members == null || members.isEmpty()) {
      throw new IllegalArgumentException("members must not be null or empty, was: " + members);
    }
  }
}
