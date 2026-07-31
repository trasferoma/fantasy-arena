package it.fantasyarena.combat;

/**
 * Come nascono gli sfidanti di una stazione del percorso: generati da zero, oppure lo specchio del
 * protagonista. Enum e non booleano {@code mirror}: una terza origine — un campione ricorrente, uno
 * sfidante scriptato — deve diventare un errore di compilazione negli switch esaustivi che la
 * leggono, non un ramo dimenticato dietro un {@code if}.
 */
public enum ChallengerOrigin {
  GENERATED,
  MIRROR
}
