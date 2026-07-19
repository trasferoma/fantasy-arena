package it.fantasyarena.combat.dice;

/**
 * Esito tipizzato di un lancio di dadi: valore ottenuto e numero di facce del dado.
 * Porta anche le facce per riconoscere il "critico al massimo naturale" ({@code value == faces}).
 * I resolver del core ricevono sempre {@code DiceThrow}, mai interi nudi.
 */
public record DiceThrow(int value, int faces) {

  /**
   * Restituisce se il lancio ha ottenuto il massimo naturale (es. 20 su un d20).
   */
  public boolean isNaturalMaximum() {
    return value == faces;
  }

  /**
   * Normalizza il risultato in {@code (0,1]}, utile per confrontarlo con le soglie
   * di probabilità (hitChance, dodgeChance, parryChance, critChance) del core.
   */
  public double normalized() {
    return (double) value / faces;
  }
}
