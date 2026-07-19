package it.fantasyarena;

import it.fantasyarena.combat.Arena;

/**
 * Punto d'ingresso dell'applicazione: nessuna logica, invoca solo {@link Arena}.
 */
public class Main {

  public static void main(String[] args) {
    new Arena().run();
  }
}
