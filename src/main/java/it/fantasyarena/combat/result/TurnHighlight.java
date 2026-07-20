package it.fantasyarena.combat.result;

/**
 * Evento notevole di un turno, tracciato come dato di dominio: arricchisce la descrizione
 * del turno e resta disponibile per la narrazione finale. Più eventi possono coesistere sullo
 * stesso turno (es. un 20 naturale è anche un colpo critico).
 */
public enum TurnHighlight {

  /**
   * Colpo critico non dovuto al 20 naturale.
   */
  CRITICAL,

  /**
   * Colpo perfetto: 20 naturale al tiro d'attacco.
   */
  PERFECT_HIT,

  /**
   * Colpo di grazia: porta il difensore a 0 vita.
   */
  KNOCKOUT,

  /**
   * Colpo pesante: danno oltre la soglia percentuale configurata della vita massima del
   * bersaglio.
   */
  HEAVY_BLOW,

  /**
   * Colpo potente andato a segno: l'attaccante ha rischiato il doppio della Stamina per il
   * doppio del danno.
   */
  POWER_STRIKE
}
