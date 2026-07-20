package it.fantasyarena.combat.engine;

import java.util.List;

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
 * ignorando la formula, tramite un {@link InitiativeOverride}. Il breakdown di entrambi i
 * combattenti è sempre calcolato, anche sotto override, per poterlo mostrare nel log. Nessun
 * lancio di dadi qui: i {@link DiceThrow} del jitter arrivano già dallo shell.
 */
public final class InitiativeResolver {

  private final InitiativeWeights weights;

  public InitiativeResolver(CombatSettings settings) {
    this.weights = settings.initiativeWeights();
  }

  /**
   * Decide il primo attore del duello con la stessa formula usata a fine turno: a Stamina
   * piena per entrambi (rapporto 1.0), l'ordine è deciso da Agilità, Intelligenza e jitter.
   * Nessun override possibile per il primo turno.
   */
  public InitiativeDecision resolveFirstMover(Fighter first, Fighter second, DiceThrow firstJitter,
      DiceThrow secondJitter) {

    InitiativeBreakdown firstBreakdown = breakdownOf(first, firstJitter);
    InitiativeBreakdown secondBreakdown = breakdownOf(second, secondJitter);

    Fighter chosen = resolveByHigherScore(first, second, firstBreakdown, secondBreakdown);
    InitiativeReport report = new InitiativeReport(
        List.of(firstBreakdown, secondBreakdown), chosen.name(), chosen.name(), InitiativeOverride.NONE);
    return new InitiativeDecision(chosen, report);
  }

  /**
   * Decide chi attacca il turno successivo. Se {@code override} non è {@code NONE} (il
   * difensore del turno corrente ha schivato o l'attaccante ha riposato), il difensore corrente
   * diventa deterministicamente il prossimo attaccante, ignorando la formula; altrimenti vince
   * chi ha lo score d'iniziativa maggiore (tie-break stabile: a parità vince l'attuale
   * attaccante). Il breakdown di entrambi è calcolato comunque, per il log.
   */
  public InitiativeDecision resolveNextAttacker(Fighter currentAttacker, Fighter currentDefender,
      InitiativeOverride override, DiceThrow attackerJitter, DiceThrow defenderJitter) {

    InitiativeBreakdown attackerBreakdown = breakdownOf(currentAttacker, attackerJitter);
    InitiativeBreakdown defenderBreakdown = breakdownOf(currentDefender, defenderJitter);

    Fighter scoreWinner = resolveByHigherScore(currentAttacker, currentDefender, attackerBreakdown, defenderBreakdown);
    Fighter chosen = (override != InitiativeOverride.NONE) ? currentDefender : scoreWinner;

    InitiativeReport report = new InitiativeReport(
        List.of(attackerBreakdown, defenderBreakdown), scoreWinner.name(), chosen.name(), override);
    return new InitiativeDecision(chosen, report);
  }

  private Fighter resolveByHigherScore(Fighter first, Fighter second, InitiativeBreakdown firstBreakdown,
      InitiativeBreakdown secondBreakdown) {
    return (firstBreakdown.total() >= secondBreakdown.total()) ? first : second;
  }

  private InitiativeBreakdown breakdownOf(Fighter fighter, DiceThrow jitterThrow) {
    int agility = Characteristics.valueOf(fighter.character(), Characteristic.AGILITY);
    int intelligence = Characteristics.valueOf(fighter.character(), Characteristic.INTELLIGENCE);
    int currentStamina = fighter.state().currentStamina();
    int maxStamina = fighter.ratings().maxStamina();
    int jitterValue = jitterThrow.value();
    double staminaRatio = (double) currentStamina / maxStamina;

    double staminaComponent = weights.wStamina() * staminaRatio;
    double agilityComponent = weights.wAgility() * agility;
    double intelligenceComponent = weights.wIntelligence() * intelligence;
    double jitterComponent = weights.wJitter() * jitterValue;
    double total = staminaComponent + agilityComponent + intelligenceComponent + jitterComponent;

    return new InitiativeBreakdown(fighter.name(), staminaComponent, agilityComponent, intelligenceComponent,
        jitterComponent, total, currentStamina, maxStamina, agility, intelligence, jitterValue);
  }
}
