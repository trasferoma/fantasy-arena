package it.fantasyarena.combat.chronicle;

/**
 * La forma di una prova: NvN a battaglia, oppure 1v1 a duello. Dice quale delle due liste di passi
 * di {@link TrialChronicle} è popolata; l'altra resta vuota.
 */
public enum TrialShape {
  BATTLE,
  DUEL
}
