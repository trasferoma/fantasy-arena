package it.fantasyarena.combat;

import it.fantasyarena.combat.chronicle.TrialShape;

/**
 * Una stazione del percorso dell'arena: il numero della prova, la sua descrizione, quanti sfidanti
 * la popolano, come nascono ({@link ChallengerOrigin}) e — solo per gli sfidanti generati — il monte
 * punti caratteristica dell'intero schieramento con cui nascono, non del singolo sfidante:
 * {@code FighterFactory.createChallengers} lo ripartisce fra i suoi sfidanti.
 *
 * <p>{@link #shape()} non è un componente del record: è duello quando l'avversario è uno solo,
 * battaglia quando sono più di uno, e custodirla accanto al numero di sfidanti creerebbe due campi
 * che devono restare d'accordo per disciplina di chi costruisce il record, invece che per
 * costruzione. Lo stesso precedente vive già in {@code RunConclusion.triumph()}.
 *
 * <p>{@link #characteristicPoints()} è {@code null} per la stazione dello specchio, non per
 * distrazione ma per costruzione: {@link ChallengerOrigin#MIRROR} ricalca il monte punti del
 * protagonista com'è cresciuto ({@code FighterFactory.createMirrorRival}), quindi quella stazione
 * non ne dichiara uno proprio. È un campo nullable e non un {@code Optional}, come già gli altri
 * dati facoltativi del progetto: {@link #generated} e {@link #mirror} sono i due soli modi di
 * costruire una stazione, e il costruttore compatto rifiuta le combinazioni che mischiano i due
 * mondi.
 */
public record TrialStation(int number, String description, int challengerCount, ChallengerOrigin challengerOrigin,
    Integer characteristicPoints) {

  public TrialStation {
    if (challengerOrigin == ChallengerOrigin.GENERATED && characteristicPoints == null) {
      throw new IllegalArgumentException("a generated station must declare its characteristicPoints");
    }
    if (challengerOrigin == ChallengerOrigin.MIRROR && characteristicPoints != null) {
      throw new IllegalArgumentException("a mirror station has no characteristicPoints of its own");
    }
  }

  static TrialStation generated(int number, String description, int challengerCount, int characteristicPoints) {
    return new TrialStation(number, description, challengerCount, ChallengerOrigin.GENERATED, characteristicPoints);
  }

  static TrialStation mirror(int number, String description) {
    return new TrialStation(number, description, 1, ChallengerOrigin.MIRROR, null);
  }

  /**
   * La forma della prova, derivata e non custodita: duello se l'avversario è uno solo, battaglia se
   * sono più di uno.
   */
  public TrialShape shape() {
    return challengerCount == 1 ? TrialShape.DUEL : TrialShape.BATTLE;
  }
}
