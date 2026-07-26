package it.fantasyarena.combat.engine;

import java.util.ArrayList;
import java.util.List;

import it.fantasyarena.combat.config.CombatFormulas;
import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.config.CombatSettings.InitiativeWeights;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Characteristics;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.InitiativeBreakdown;
import it.fantasyarena.combat.result.InitiativeOverride;
import it.fantasyarena.combat.result.InitiativeReport;
import it.fantasytoolkitcore.core.model.Characteristic;

/**
 * Risoluzione pura dell'iniziativa: chi attacca è deciso dal rapporto Stamina corrente/massima
 * (dominante), Agilità, Intelligenza e un micro-jitter (dado piccolo) che rompe pareggi e
 * simmetrie. Una schivata riuscita o un riposo cedono/rubano deterministicamente il tempo,
 * ignorando la formula, tramite un {@link InitiativeOverride}. Il breakdown di ogni combattente
 * partecipante al test a punteggio è sempre calcolato, anche sotto override quando restano
 * candidati da testare, per poterlo mostrare nel log. Nessun lancio di dadi qui: i
 * {@link DiceThrow} del jitter arrivano già dallo shell. La classe è nativamente N-aria: le
 * firme binarie storiche restano come adapter sottili sopra i metodi N-ari, a comportamento
 * osservabile identico.
 */
public final class InitiativeResolver {

  private final InitiativeWeights weights;

  public InitiativeResolver(CombatSettings settings) {
    this.weights = settings.initiativeWeights();
  }

  /**
   * Punteggio d'iniziativa di un singolo combattente: il mattone N-ario, già presente ma privato,
   * ora esposto perché i chiamanti possano comporlo su un numero qualunque di partecipanti.
   */
  public InitiativeBreakdown breakdown(Fighter fighter, DiceThrow jitter) {
    int agility = Characteristics.valueOf(fighter.character(), Characteristic.AGILITY);
    int intelligence = Characteristics.valueOf(fighter.character(), Characteristic.INTELLIGENCE);
    int currentStamina = fighter.state().currentStamina();
    int maxStamina = fighter.ratings().maxStamina();
    int jitterValue = jitter.value();

    double staminaComponent = CombatFormulas.initiativeStaminaComponent(weights, currentStamina, maxStamina);
    double agilityComponent = CombatFormulas.initiativeAgilityComponent(weights, agility);
    double intelligenceComponent = CombatFormulas.initiativeIntelligenceComponent(weights, intelligence);
    double jitterComponent = CombatFormulas.initiativeJitterComponent(weights, jitterValue);
    double total = staminaComponent + agilityComponent + intelligenceComponent + jitterComponent;

    return new InitiativeBreakdown(fighter.name(), staminaComponent, agilityComponent, intelligenceComponent,
        jitterComponent, total, currentStamina, maxStamina, agility, intelligence, jitterValue);
  }

  /**
   * Decide l'attore fra N partecipanti quando il test a punteggio va eseguito: vince il
   * punteggio più alto; a parità vince chi compare prima in {@code ordered}. Un jitter per
   * partecipante, nello stesso ordine della lista. Nessun override: il report riporta sempre
   * {@link InitiativeOverride#NONE}.
   */
  public InitiativeDecision resolveInitiative(List<Fighter> ordered, List<DiceThrow> jitters) {
    validateOrdered(ordered);
    validateJitters(ordered, jitters);

    List<InitiativeBreakdown> breakdowns = breakdownsOf(ordered, jitters);
    Fighter winner = highestScoring(ordered, breakdowns);

    InitiativeReport report = new InitiativeReport(breakdowns, winner.name(), winner.name(), InitiativeOverride.NONE);
    return new InitiativeDecision(winner, report);
  }

  /**
   * Il tempo è rubato: chi ha schivato agisce, senza test a punteggio. Nessun breakdown: il
   * report porta solo il motivo dell'override e chi agisce davvero.
   */
  public InitiativeDecision stolenTime(Fighter thief, InitiativeOverride override) {
    validateNotNull(thief, "thief");
    validateNotNull(override, "override");

    InitiativeReport report = new InitiativeReport(List.of(), thief.name(), thief.name(), override);
    return new InitiativeDecision(thief, report);
  }

  /**
   * Il tempo è ceduto: chi ha riposato è escluso e il test si svolge fra i restanti. Con un solo
   * candidato residuo non c'è nulla da testare: nessun breakdown calcolato, {@code jitters} deve
   * essere vuoto. Con più candidati residui, i {@code breakdowns} riportati sono solo quelli dei
   * candidati (lo yielder non vi compare mai), nell'ordine in cui compaiono in {@code ordered}.
   */
  public InitiativeDecision yieldedTime(Fighter yielder, List<Fighter> ordered, List<DiceThrow> jitters,
      InitiativeOverride override) {
    validateOrdered(ordered);
    validateNotNull(yielder, "yielder");
    validateYielderPresent(yielder, ordered);
    validateNotNull(override, "override");

    List<Fighter> candidates = candidatesExcluding(yielder, ordered);
    if (candidates.size() == 1) {
      return yieldedTimeWithSingleCandidate(candidates.get(0), jitters, override);
    }

    validateJitters(candidates, jitters);
    List<InitiativeBreakdown> breakdowns = breakdownsOf(candidates, jitters);
    Fighter winner = highestScoring(candidates, breakdowns);

    InitiativeReport report = new InitiativeReport(breakdowns, winner.name(), winner.name(), override);
    return new InitiativeDecision(winner, report);
  }

  private InitiativeDecision yieldedTimeWithSingleCandidate(Fighter onlyCandidate, List<DiceThrow> jitters,
      InitiativeOverride override) {
    if (jitters == null || !jitters.isEmpty()) {
      throw new IllegalArgumentException(
          "jitters must be empty when only one candidate remains after excluding the yielder, was: " + jitters);
    }

    InitiativeReport report =
        new InitiativeReport(List.of(), onlyCandidate.name(), onlyCandidate.name(), override);
    return new InitiativeDecision(onlyCandidate, report);
  }

  /**
   * Decide il primo attore del duello con la stessa formula usata a fine turno: a Stamina
   * piena per entrambi (rapporto 1.0), l'ordine è deciso da Agilità, Intelligenza e jitter.
   * Nessun override possibile per il primo turno. Adapter binario sopra {@link #resolveInitiative}.
   */
  public InitiativeDecision resolveFirstMover(Fighter first, Fighter second, DiceThrow firstJitter,
      DiceThrow secondJitter) {
    return resolveInitiative(List.of(first, second), List.of(firstJitter, secondJitter));
  }

  /**
   * Decide chi attacca il turno successivo quando il test a punteggio va davvero eseguito
   * (nessun override): vince chi ha lo score d'iniziativa maggiore (tie-break stabile: a
   * parità vince l'attuale attaccante). Il breakdown di entrambi è calcolato per il log. Quando
   * il turno precedente ha prodotto un override, il chiamante deve usare
   * {@link #overriddenNextAttacker} invece di questo metodo: la formula non va testata. Adapter
   * binario sopra {@link #resolveInitiative}.
   */
  public InitiativeDecision resolveNextAttacker(Fighter currentAttacker, Fighter currentDefender,
      DiceThrow attackerJitter, DiceThrow defenderJitter) {
    return resolveInitiative(List.of(currentAttacker, currentDefender), List.of(attackerJitter, defenderJitter));
  }

  /**
   * Decide chi attacca il turno successivo quando il turno corrente ha prodotto un override
   * (il difensore ha schivato o l'attaccante ha riposato): il difensore corrente diventa
   * deterministicamente il prossimo attaccante, senza eseguire il test a punteggio. Nessun
   * breakdown calcolato e nessun vincitore per punteggio: il report porta solo il motivo
   * dell'override e chi agisce davvero. Il parametro {@code currentAttacker} non entra nel
   * calcolo: resta in firma per simmetria con {@link #resolveNextAttacker}, come da SPEC. Adapter
   * binario sopra {@link #stolenTime}.
   */
  public InitiativeDecision overriddenNextAttacker(Fighter currentAttacker, Fighter currentDefender,
      InitiativeOverride override) {
    return stolenTime(currentDefender, override);
  }

  /**
   * Calcola il breakdown di ogni combattente, jitter allineato per posizione nella stessa lista.
   */
  private List<InitiativeBreakdown> breakdownsOf(List<Fighter> fighters, List<DiceThrow> jitters) {
    List<InitiativeBreakdown> breakdowns = new ArrayList<>(fighters.size());
    for (int i = 0; i < fighters.size(); i++) {
      breakdowns.add(breakdown(fighters.get(i), jitters.get(i)));
    }
    return breakdowns;
  }

  /**
   * Scansione sinistra->destra che aggiorna il migliore solo su '>' stretto: a parità resta il
   * primo trovato, cioè l'indice più basso vince. È l'esatto equivalente N-ario del confronto
   * binario storico {@code firstBreakdown.total() >= secondBreakdown.total() ? first : second},
   * dove a parità vinceva sempre il primo argomento: qui l'elemento 0 gioca lo stesso ruolo.
   */
  private Fighter highestScoring(List<Fighter> fighters, List<InitiativeBreakdown> breakdowns) {
    Fighter best = fighters.get(0);
    double bestTotal = breakdowns.get(0).total();

    for (int i = 1; i < fighters.size(); i++) {
      double candidateTotal = breakdowns.get(i).total();
      if (candidateTotal > bestTotal) {
        best = fighters.get(i);
        bestTotal = candidateTotal;
      }
    }

    return best;
  }

  private List<Fighter> candidatesExcluding(Fighter yielder, List<Fighter> ordered) {
    List<Fighter> candidates = new ArrayList<>();
    for (Fighter fighter : ordered) {
      if (fighter != yielder) {
        candidates.add(fighter);
      }
    }
    return candidates;
  }

  private void validateOrdered(List<Fighter> ordered) {
    if (ordered == null || ordered.isEmpty()) {
      throw new IllegalArgumentException("ordered must not be null or empty, was: " + ordered);
    }
  }

  private void validateJitters(List<Fighter> fighters, List<DiceThrow> jitters) {
    if (jitters == null || jitters.size() != fighters.size()) {
      int actualSize = (jitters == null) ? -1 : jitters.size();
      throw new IllegalArgumentException(
          "jitters must have exactly " + fighters.size() + " elements (one per fighter), was: " + actualSize);
    }
  }

  private void validateYielderPresent(Fighter yielder, List<Fighter> ordered) {
    for (Fighter fighter : ordered) {
      if (fighter == yielder) {
        return;
      }
    }
    throw new IllegalArgumentException(
        "yielder must be present in ordered, was not found: " + yielder.name());
  }

  private void validateNotNull(Object value, String parameterName) {
    if (value == null) {
      throw new IllegalArgumentException(parameterName + " must not be null");
    }
  }
}
