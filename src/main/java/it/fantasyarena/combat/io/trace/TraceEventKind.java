package it.fantasyarena.combat.io.trace;

/**
 * Il campo discriminante di una riga del log analitico: dice quale dei cinque tipi di evento è
 * questa riga, così un lettore che ha solo il JSON sa quale forma aspettarsi senza doverla
 * indovinare dai campi presenti.
 */
public enum TraceEventKind {
  RUN_OPENED,
  TRIAL_STARTED,
  EXCHANGE,
  TRIAL_ENDED,
  RUN_CLOSED
}
