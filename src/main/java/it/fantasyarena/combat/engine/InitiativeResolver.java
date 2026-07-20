package it.fantasyarena.combat.engine;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.config.CombatSettings.InitiativeWeights;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Characteristics;
import it.fantasyarena.combat.model.Fighter;
import it.fantasytoolkitcore.core.model.Characteristic;

/**
 * Risoluzione pura dell'iniziativa: chi attacca è deciso dal rapporto Stamina corrente/massima
 * (dominante), Agilità, Intelligenza e un micro-jitter (dado piccolo) che rompe pareggi e
 * simmetrie. Una schivata riuscita ruba il tempo: assegna l'iniziativa allo schivatore con un
 * override deterministico che ignora la formula. Nessun lancio di dadi qui: i {@link DiceThrow}
 * del jitter arrivano già dallo shell.
 */
public final class InitiativeResolver {

  private final InitiativeWeights weights;

  public InitiativeResolver(CombatSettings settings) {
    this.weights = settings.initiativeWeights();
  }

  /**
   * Decide il primo attore del duello con la stessa formula usata a fine turno: a Stamina
   * piena per entrambi (rapporto 1.0), l'ordine è deciso da Agilità, Intelligenza e jitter.
   */
  public Fighter resolveFirstMover(Fighter first, Fighter second, DiceThrow firstJitter, DiceThrow secondJitter) {
    return resolveByHigherScore(first, second, firstJitter, secondJitter);
  }

  /**
   * Decide chi attacca il turno successivo. Se il difensore del turno corrente ha schivato,
   * ruba deterministicamente il tempo e diventa il prossimo attaccante, ignorando la formula;
   * altrimenti vince chi ha lo score d'iniziativa maggiore (tie-break stabile: a parità vince
   * l'attuale attaccante).
   */
  public Fighter resolveNextAttacker(Fighter currentAttacker, Fighter currentDefender, boolean defenderDodged,
      DiceThrow attackerJitter, DiceThrow defenderJitter) {

    if (defenderDodged) {
      return currentDefender;
    }
    return resolveByHigherScore(currentAttacker, currentDefender, attackerJitter, defenderJitter);
  }

  private Fighter resolveByHigherScore(Fighter first, Fighter second, DiceThrow firstJitter, DiceThrow secondJitter) {
    double firstScore = initiativeScore(first, firstJitter);
    double secondScore = initiativeScore(second, secondJitter);
    return (firstScore >= secondScore) ? first : second;
  }

  private double initiativeScore(Fighter fighter, DiceThrow jitterThrow) {
    int agility = Characteristics.valueOf(fighter.character(), Characteristic.AGILITY);
    int intelligence = Characteristics.valueOf(fighter.character(), Characteristic.INTELLIGENCE);
    double staminaRatio = (double) fighter.state().currentStamina() / fighter.ratings().maxStamina();

    return weights.wStamina() * staminaRatio
        + weights.wAgility() * agility
        + weights.wIntelligence() * intelligence
        + weights.wJitter() * jitterThrow.value();
  }
}
