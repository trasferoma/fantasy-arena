package it.fantasyarena.combat.result;

/**
 * Motivo per cui l'iniziativa del turno successivo è stata decisa fuori dalla formula.
 * {@code NONE} indica una decisione presa dalla formula (stamina/agilità/intelligenza/jitter).
 * {@code DODGE_STEAL} indica che la schivata riuscita ruba deterministicamente il tempo allo
 * schivatore. {@code REST_YIELD} indica che il riposo cede deterministicamente il tempo
 * all'avversario. Sia {@code DODGE_STEAL} che {@code REST_YIELD} forzano il difensore corrente
 * come prossimo attaccante, ignorando lo score.
 */
public enum InitiativeOverride {
  NONE,
  DODGE_STEAL,
  REST_YIELD
}
