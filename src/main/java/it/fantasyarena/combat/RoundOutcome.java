package it.fantasyarena.combat;

/**
 * Com'è finita una prova, in un solo dato letto sia da {@link Arena} per decidere se proseguire sia
 * dal logger per raccontarlo. Sono tre casi e non due perché restare in piedi non equivale a vincere:
 * il protagonista può cadere, ma può anche resistere fino alla fine senza abbattere tutti gli
 * avversari, e sono due chiusure diverse — una è una disfatta, l'altra è un pareggio o una decisione
 * ai punti — che vanno raccontate con parole diverse.
 */
public enum RoundOutcome {

  /**
   * Vittoria piena: il protagonista è rimasto in piedi e ha abbattuto tutti gli sfidanti. È il solo
   * caso che apre la prova successiva.
   */
  WON,

  /**
   * Il protagonista è caduto in combattimento. L'arena si chiude qui.
   */
  FELL,

  /**
   * Il protagonista è rimasto in piedi ma non ha abbattuto tutti gli sfidanti: un pareggio o una
   * decisione ai punti. Non è una caduta, ma non basta ad aprire la prova successiva.
   */
  STOOD_WITHOUT_WINNING
}
