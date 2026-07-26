package it.fantasyarena.combat.result;

/**
 * Cosa ha fatto l'attore nello scambio, in forma di dati e non di prosa: consente alla
 * presentazione di comporre etichette brevi senza interpretare la descrizione gia' composta.
 */
public record ActionOutcome(Kind kind, int damage, int staminaRecovered, boolean critical, boolean powerStrike) {

  public enum Kind {
    HIT,
    MISS,
    PARRIED,
    DODGED,
    REST
  }
}
