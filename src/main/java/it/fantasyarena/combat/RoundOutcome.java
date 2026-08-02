package it.fantasyarena.combat;

/**
 * Com'è finita una prova, in un solo dato letto sia da {@link Arena} per decidere se proseguire sia
 * dal logger per raccontarlo. Sono tre casi e non due perché restare in piedi non equivale a vincere:
 * il protagonista può cadere, ma può anche resistere fino alla fine senza abbattere tutti gli
 * avversari, e sono due chiusure diverse — una è una disfatta, l'altra è un pareggio o una decisione
 * ai punti — che vanno raccontate con parole diverse. Solo la caduta chiude la corsa: sia la
 * vittoria sia il pareggio aprono la prova successiva, ma soltanto la vittoria porta con sé loot e
 * punti caratteristica.
 */
public enum RoundOutcome {

  /**
   * Vittoria piena: il protagonista è rimasto in piedi e ha abbattuto tutti gli sfidanti. Apre la
   * prova successiva con la scheda cresciuta dalla procedura di fine scontro.
   */
  WON,

  /**
   * Il protagonista è caduto in combattimento. L'arena si chiude qui.
   */
  FELL,

  /**
   * Il protagonista è rimasto in piedi ma non ha abbattuto tutti gli sfidanti: un pareggio o una
   * decisione ai punti. Non è una caduta: apre comunque la prova successiva, ma senza loot né punti
   * caratteristica, con la scheda invariata rispetto a prima.
   */
  STOOD_WITHOUT_WINNING
}
