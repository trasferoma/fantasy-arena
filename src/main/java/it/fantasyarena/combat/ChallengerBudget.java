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
 * base. È un rischio accettato, non un caso limite: un gioiello che porti molta {@code LUCK} può da
 * solo schiacciare lo schieramento fino al pavimento, ed è comportamento ordinario. Il pavimento —
 * {@code FighterFactory.MINIMUM_CHARACTERISTIC_POINTS_PER_CHALLENGER} per sfidante — è appunto la
 * difesa contro questo scenario: lo sconto {@linkplain #luckDiscount() applicato} non lo scavalca
 * mai, né scende mai sotto zero.
 *
 * @param stationPoints monte punti dichiarato dalla stazione per l'intero schieramento
 * @param luckDiscount sconto effettivamente applicato, non quello teorico richiesto dalla fortuna
 * @param squadPoints monte punti effettivo dello schieramento, {@code stationPoints - luckDiscount}
 */
public record ChallengerBudget(int stationPoints, int luckDiscount, int squadPoints) {

  /**
   * Calcola il budget di una stazione generata: lo sconto richiesto è la fortuna effettiva del
   * protagonista moltiplicata per il numero di sfidanti, ma resta limitato da entrambi i lati —
   * mai oltre {@code stationPoints - pavimento}, mai sotto zero.
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
    int affordableDiscount = Math.max(0, stationPoints - floor);
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
