package it.fantasyarena.combat;

import it.fantasyarena.combat.factory.FighterFactory;
import it.fantasyarena.combat.hero.Hero;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkitcore.core.model.Characteristic;

/**
 * Il monte punti effettivo con cui una stazione generata si presenta a questo protagonista: il
 * monte che la stazione dichiara, lo sconto che la sua fortuna applica, e il monte che ne risulta
 * per l'intero schieramento. Vive accanto a {@link TrialPlan} e non in {@code HeroBrain} perché la
 * pressione del percorso è già dichiarata fuori dal cervello — {@code TrialPlan} è «il punto unico
 * da toccare per allungare o ribilanciare la pressione del percorso» — e uno sconto sul monte degli
 * avversari non è una scelta del protagonista, che è tutto ciò che {@code HeroBrain} governa.
 *
 * <p>La fortuna vale qualcosa perché è questa la leva che gliene dà uno: nel motore la
 * {@code LUCK} vale soltanto un punto percentuale di critico in più per punto, quasi una
 * caratteristica morta. Scontare il monte degli sfidanti in sua proporzione le dà peso senza
 * toccare il motore e senza abbassare singole caratteristiche generate, cosa per cui il toolkit non
 * espone alcuna API.
 *
 * <p>Lo sconto legge la fortuna <strong>effettiva</strong> del protagonista
 * ({@link Hero#effectiveCharacter()}), coi buff di arma, armatura e gioielli addosso, e non quella
 * base. Un gioiello che porti molta {@code LUCK} resta un caso ordinario, non un limite inventato:
 * senza un tetto lo sconto richiesto cresce senza freno, e la misura sul percorso precedente al
 * tetto registrava uno sconto salito fino al 47,1% del monte alla nona prova, col pavimento toccato
 * nell'11,0% delle prove giocate, mentre gli sfidanti restavano piatti a 21 di offensivo medio dalla
 * prima alla nona prova e il protagonista cresceva da 22 a 45. {@link #MAX_LUCK_DISCOUNT_PERCENT} è
 * la difesa contro questa deriva: lo sconto {@linkplain #luckDiscount() applicato} non supera mai
 * quella percentuale del monte dichiarato dalla stazione. È una leva di bilanciamento dichiarata e
 * ritarabile, non un dettaglio incidentale. Il pavimento —
 * {@code FighterFactory.MINIMUM_CHARACTERISTIC_POINTS_PER_CHALLENGER} per sfidante — resta una
 * guardia strutturale contro un monte già troppo basso in partenza: sui nove monti del percorso
 * attuale è sempre il tetto a mordere per primo, e il pavimento tornerebbe a mordere solo su una
 * stazione che dichiarasse meno di dieci punti per sfidante. I due limiti convivono, vince il più
 * stringente, e lo sconto non scende mai sotto zero.
 *
 * @param stationPoints monte punti dichiarato dalla stazione per l'intero schieramento
 * @param luckDiscount sconto effettivamente applicato, non quello teorico richiesto dalla fortuna
 * @param squadPoints monte punti effettivo dello schieramento, {@code stationPoints - luckDiscount}
 */
public record ChallengerBudget(int stationPoints, int luckDiscount, int squadPoints) {

  /**
   * Percentuale massima del monte dichiarato dalla stazione che lo sconto della fortuna può
   * erodere. È una leva di bilanciamento a sé, distinta dal pavimento: mentre il pavimento protegge
   * un monte già basso in partenza, questo tetto tiene sotto controllo lo sconto quando il monte è
   * ampio e la fortuna del protagonista è alta, il caso in cui il pavimento da solo non basta a
   * fermare la deriva descritta nel Javadoc di classe.
   */
  public static final int MAX_LUCK_DISCOUNT_PERCENT = 30;

  /**
   * Calcola il budget di una stazione generata: lo sconto richiesto è la fortuna effettiva del
   * protagonista moltiplicata per il numero di sfidanti, ma resta limitato dal più stringente fra
   * il pavimento ({@code stationPoints - pavimento}) e il tetto percentuale
   * ({@link #MAX_LUCK_DISCOUNT_PERCENT}{@code % di stationPoints}), e non scende mai sotto zero.
   *
   * @param stationPoints monte punti dichiarato dalla stazione per l'intero schieramento
   * @param hero protagonista la cui fortuna effettiva sconta il monte, non nullo
   * @param challengerCount numero di sfidanti dello schieramento, almeno 1
   * @return il budget con monte dichiarato, sconto applicato e monte effettivo
   * @throws IllegalArgumentException se {@code hero} è nullo o {@code challengerCount} è minore di 1
   */
  public static ChallengerBudget of(int stationPoints, Hero hero, int challengerCount) {
    validate(hero, challengerCount);

    int floor = FighterFactory.MINIMUM_CHARACTERISTIC_POINTS_PER_CHALLENGER * challengerCount;
    int requestedDiscount = effectiveLuckOf(hero) * challengerCount;
    int floorLimitedDiscount = Math.max(0, stationPoints - floor);
    int percentageLimitedDiscount = stationPoints * MAX_LUCK_DISCOUNT_PERCENT / 100;
    int affordableDiscount = Math.min(floorLimitedDiscount, percentageLimitedDiscount);
    int appliedDiscount = Math.min(requestedDiscount, affordableDiscount);

    return new ChallengerBudget(stationPoints, appliedDiscount, stationPoints - appliedDiscount);
  }

  /**
   * La fortuna effettiva del protagonista: {@code CharacterResult} non ha un accessor per singola
   * caratteristica, quindi si scorre {@code characteristics()} come già fa {@code EquipmentBonus}.
   */
  private static int effectiveLuckOf(Hero hero) {
    return hero.effectiveCharacter().characteristics().stream()
        .filter(characteristic -> characteristic.characteristic() == Characteristic.LUCK)
        .mapToInt(CharacterCharacteristic::value)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("hero character has no LUCK characteristic: " + hero.name()));
  }

  private static void validate(Hero hero, int challengerCount) {
    if (hero == null) {
      throw new IllegalArgumentException("hero must not be null");
    }
    if (challengerCount < 1) {
      throw new IllegalArgumentException("challengerCount must be >= 1, was: " + challengerCount);
    }
  }
}
