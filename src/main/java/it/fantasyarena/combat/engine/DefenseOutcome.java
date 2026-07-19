package it.fantasyarena.combat.engine;

/**
 * Esito puro del test di difesa: come ha reagito il difensore e quanta riduzione
 * di danno ne deriva ({@code 1.0} = danno azzerato).
 */
public record DefenseOutcome(DefenseResult result, double damageReduction) {

  public enum DefenseResult {
    DODGED,
    PARRIED,
    HIT_TAKEN
  }
}
