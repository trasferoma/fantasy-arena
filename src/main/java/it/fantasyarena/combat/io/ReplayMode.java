package it.fantasyarena.combat.io;

/**
 * Modalità di presentazione del replay del combattimento: {@code LINEAR} stampa i turni in
 * sequenza su console, {@code SCREEN} mostra una pagina a schermo intero con barre verticali
 * di vita/stamina dei due combattenti e il log cumulativo dei turni rivelati a fianco.
 */
public enum ReplayMode {
  LINEAR,
  SCREEN
}
