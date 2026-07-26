package it.fantasyarena.combat.battle;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.InitiativeOverride;

/**
 * Uno scontro attivo all'interno della battaglia: un sottoinsieme di combattenti che si
 * affrontano in un turno. Mutabile, come {@link Fighter}: porta la memoria (ultimo attore,
 * bersagli correnti, override pendente) che serve a decidere il turno successivo. Un
 * {@link Fighter} deve appartenere a un solo {@code Engagement} alla volta, ma questo invariante
 * globale non è verificabile da qui: è responsabilità del chiamante (l'orchestratore di battaglia
 * di una fase successiva) evitare di far entrare lo stesso combattente in più scontri
 * contemporaneamente.
 */
public final class Engagement {

  private final int id;
  private final List<Fighter> participants;
  private final Map<Fighter, Fighter> currentTargetByFighter;

  private Fighter lastActor;
  private InitiativeOverride pendingOverride;

  public Engagement(int id, List<Fighter> initialParticipants) {
    validateParticipants(initialParticipants);
    this.id = id;
    this.participants = new ArrayList<>(initialParticipants);
    this.currentTargetByFighter = new IdentityHashMap<>();
    this.lastActor = null;
    this.pendingOverride = InitiativeOverride.NONE;
  }

  public int id() {
    return id;
  }

  /**
   * Tutti i partecipanti (vivi o morti), nell'ordine di ingresso nello scontro.
   */
  public List<Fighter> participants() {
    return List.copyOf(participants);
  }

  /**
   * Partecipanti ancora vivi, nell'ordine di ingresso nello scontro.
   */
  public List<Fighter> livingParticipants() {
    return participants.stream()
        .filter(participant -> !participant.isDefeated())
        .toList();
  }

  /**
   * Ordine d'iniziativa per il prossimo test a punteggio: i partecipanti vivi con l'ultimo
   * attore in prima posizione, seguito dagli altri nell'ordine di ingresso. Se non c'è ancora un
   * ultimo attore (nessuno scambio è stato giocato) o se l'ultimo attore è morto, l'ordine è
   * semplicemente quello di ingresso. Mettere l'ultimo attore in testa riproduce esattamente il
   * tie-break del duello 1v1 storico: a parità di punteggio vince il primo della lista, e nel
   * duello binario il primo della lista era sempre l'attaccante corrente. Qui generalizziamo la
   * stessa regola a N partecipanti.
   */
  public List<Fighter> initiativeOrder() {
    List<Fighter> living = livingParticipants();
    if (lastActor == null || !FighterIdentity.containsSame(living, lastActor)) {
      return living;
    }

    List<Fighter> ordered = new ArrayList<>(living.size());
    ordered.add(lastActor);
    for (Fighter fighter : living) {
      if (fighter != lastActor) {
        ordered.add(fighter);
      }
    }
    return ordered;
  }

  /**
   * Vero sse fra i partecipanti vivi sono rappresentate almeno due squadre diverse: uno scontro
   * con soli alleati vivi (o con un solo vivo, o nessuno) è concluso.
   */
  public boolean isActive(BattleRoster roster) {
    List<Fighter> living = livingParticipants();
    if (living.size() < 2) {
      return false;
    }

    // Confronto sull'indice e non sull'istanza: Team è un record, quindi confrontarlo con != si
    // appoggerebbe al fatto che il roster restituisca sempre la stessa istanza, invariante che
    // nessuno garantisce.
    int firstTeamIndex = roster.teamOf(living.get(0)).index();
    for (int i = 1; i < living.size(); i++) {
      if (roster.teamOf(living.get(i)).index() != firstTeamIndex) {
        return true;
      }
    }
    return false;
  }

  /**
   * Aggiunge un partecipante allo scontro: rifiuta se è già presente (per identità).
   */
  public void join(Fighter fighter) {
    if (FighterIdentity.containsSame(participants, fighter)) {
      throw new IllegalArgumentException(
          "fighter is already a participant of this engagement: " + fighter.name());
    }
    participants.add(fighter);
  }

  public InitiativeOverride pendingOverride() {
    return pendingOverride;
  }

  /**
   * Ultimo attore che ha giocato uno scambio in questo scontro, oppure {@code null} se nessuno
   * scambio è ancora stato giocato.
   */
  public Fighter lastActor() {
    return lastActor;
  }

  /**
   * Bersaglio corrente scelto da {@code fighter} nell'ultimo scambio in cui ha agito, oppure
   * {@code null} se non ne ha ancora uno.
   */
  public Fighter currentTargetOf(Fighter fighter) {
    return currentTargetByFighter.get(fighter);
  }

  /**
   * Registra lo scambio appena giocato: {@code actor} diventa l'ultimo attore, {@code target} il
   * suo bersaglio corrente, {@code override} l'override pendente per il turno successivo.
   */
  public void recordExchange(Fighter actor, Fighter target, InitiativeOverride override) {
    validateNotNull(actor, "actor");
    validateNotNull(target, "target");
    validateNotNull(override, "override");

    lastActor = actor;
    currentTargetByFighter.put(actor, target);
    pendingOverride = override;
  }

  private void validateParticipants(List<Fighter> initialParticipants) {
    if (initialParticipants == null || initialParticipants.isEmpty()) {
      throw new IllegalArgumentException(
          "initialParticipants must not be null or empty, was: " + initialParticipants);
    }
  }

  private void validateNotNull(Object value, String parameterName) {
    if (value == null) {
      throw new IllegalArgumentException(parameterName + " must not be null");
    }
  }
}
