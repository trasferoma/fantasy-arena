package it.fantasyarena.combat.engine;

import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Characteristics;
import it.fantasyarena.combat.model.Fighter;
import it.fantasytoolkitcore.core.model.Characteristic;

/**
 * Risoluzione pura dell'iniziativa: combina il tiro di ciascun combattente con la sua
 * Agilità per stabilire chi agisce per primo. La SPEC non specifica una formula esplicita
 * per l'iniziativa: risolto con {@code tiro + Agilità}, a parità vince il primo combattente
 * passato (tie-break deterministico).
 */
public final class InitiativeResolver {

  public Fighter resolveFirstMover(Fighter first, Fighter second, DiceThrow firstThrow, DiceThrow secondThrow) {
    int firstScore = initiativeScore(first, firstThrow);
    int secondScore = initiativeScore(second, secondThrow);
    return (firstScore >= secondScore) ? first : second;
  }

  private int initiativeScore(Fighter fighter, DiceThrow diceThrow) {
    int agility = Characteristics.valueOf(fighter.character(), Characteristic.AGILITY);
    return diceThrow.value() + agility;
  }
}
