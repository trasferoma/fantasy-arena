package it.fantasyarena.combat;

import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.RarityTable;

/**
 * L'equipaggiamento con cui nasce una stazione generata del percorso: due tabelle di rarità
 * distinte — una per l'arma, una per i pezzi d'armatura — e quanti pezzi si indossano. Vive accanto
 * a {@link ChallengerBudget} e non come campo di {@link TrialStation} per lo stesso motivo per cui
 * {@link TrialStation#shape()} non è un campo: sarebbe un dato <em>custodito</em> che è funzione del
 * solo numero della stazione, e il progetto risolve questi dati alla lettura invece di affiancarli
 * alle fonti — qui la fonte è {@link #forTrial(int)}, le quattro fasce dichiarate una volta sola.
 *
 * <p>Le due tabelle non sono una scelta di modellazione a caso: sono la conseguenza di una misura.
 * Nelle formule del motore l'attacco dell'arma pesa {@code 2,0} su {@code OFF} e la somma delle
 * difese dei pezzi indossati pesa altrettanto {@code 2,0} su {@code DEF} — l'arma decide
 * <strong>quanto fanno male</strong> gli sfidanti, i pezzi d'armatura decidono <strong>quanto
 * resistono</strong>, e sono due grandezze che il bilanciamento deve muovere in direzioni
 * <em>opposte</em>. Con una tabella sola alzarla per renderli pericolosi li rende anche immortali,
 * abbassarla per renderli abbattibili li rende innocui, e il bersaglio non è raggiungibile. La
 * misura lo dimostra con un numero preciso: con la difesa del protagonista a 62 punti alla nona
 * prova, {@code danno = max(1, OFF − 0,5·DEF)} fa perdere 31 punti a ogni colpo, quindi sotto i 31
 * punti di offensivo un avversario infligge un solo danno e sopra i 40 tre avversari lo uccidono in
 * quattro turni: nove punti di offensivo separano l'inoffensivo dal letale, e nessuna tabella sola
 * può governarli insieme all'armatura senza sbagliare uno dei due lati.
 *
 * <p>La tabella dell'arma resta più generosa di quella dell'armatura di proposito, e non è
 * un'incoerenza fra le due: sono due leve con due scopi distinti. Quella dell'arma deve spingere gli
 * sfidanti oltre la soglia dei 31-40 punti di offensivo perché il danno smetta di essere quasi
 * sempre quello minimo del motore; quella dell'armatura resta più prudente perché la difesa somma
 * su più pezzi indossati insieme e cresce in fretta — un solo pezzo {@code EPIC} vale già 14-22
 * punti di {@code DEF}.
 *
 * <p>È il gemello di {@link TrialLoot#forTrial(int)} sul lato degli avversari, e ne condivide
 * la forma a quattro scaglioni sul percorso a dieci prove (1-2, 3-5, 6-8, 9-10): il protagonista
 * tiene il meglio di molte estrazioni successive lungo la corsa, lo sfidante nasce da una sola
 * estrazione e non ha una seconda occasione. Le due tabelle di questa classe non seguono però un
 * gradino fisso sotto quella del loot dell'eroe: rispondono al vincolo del danno minimo del motore,
 * non alla generosità della sua progressione, e la tabella dell'arma può quindi superarla dove la
 * misura lo richiede.
 *
 * <p>La prima fascia (prove 1-2) riproduce <strong>identico</strong> l'equipaggiamento di oggi —
 * entrambe le tabelle {@code UNCOMMON} certo, un pezzo solo — e non per prudenza cosmetica: la
 * misura sul percorso precedente a questo lavoro registra che il 36,8% delle corse muore già alla
 * prima prova, ed è un problema aperto che questa modifica non deve peggiorare. Le prove 1-2
 * restano quindi fuori dalla scala che inasprisce le altre.
 *
 * <p>I pesi delle altre tre fasce non sono scelti a intuito: sono il risultato di una taratura
 * empirica, misurata con la sonda {@code BalanceProbe} su 1000 corse per iterazione contro il
 * bersaglio dichiarato dall'utente — chi supera la prova 4 deve arrivare in fondo al percorso nel
 * 35% dei casi — e i pesi scritti in questa classe lo centrano al 34,9%. La taratura ha rivelato un
 * fatto controintuitivo: le fasce 3-5 e 6-8 hanno dovuto <strong>riabbassare</strong> il pavimento
 * di rarità invece di alzarlo, perché la prova 4 (primo scontro due-contro-uno) e la prova 7 (primo
 * scontro tre-contro-uno) erano già i due punti più letali del percorso per il solo scatto di
 * numerosità degli sfidanti: la rarità non doveva sommarsi a quello scatto, doveva compensarlo
 * restando bassa.
 *
 * @param weaponRarityTable tabella pesata da cui estrarre la rarità dell'arma, non nulla
 * @param armourRarityTable tabella pesata da cui estrarre la rarità dei pezzi d'armatura, non nulla
 * @param armourPieceCount numero di pezzi d'armatura da indossare, almeno 1
 */
public record ChallengerEquipment(RarityTable weaponRarityTable, RarityTable armourRarityTable, int armourPieceCount) {

  private static final RarityTable OPENING_TRIALS_WEAPON_RARITY_TABLE =
      RarityTable.builder().entry(Rarity.UNCOMMON, 100).build();

  private static final RarityTable OPENING_TRIALS_ARMOUR_RARITY_TABLE =
      RarityTable.builder().entry(Rarity.UNCOMMON, 100).build();

  private static final int OPENING_TRIALS_ARMOUR_PIECE_COUNT = 1;

  private static final RarityTable EARLY_TRIALS_WEAPON_RARITY_TABLE = RarityTable.builder()
      .entry(Rarity.UNCOMMON, 90)
      .entry(Rarity.RARE, 10)
      .build();

  private static final RarityTable EARLY_TRIALS_ARMOUR_RARITY_TABLE =
      RarityTable.builder().entry(Rarity.UNCOMMON, 100).build();

  private static final int EARLY_TRIALS_ARMOUR_PIECE_COUNT = 1;

  private static final RarityTable MID_TRIALS_WEAPON_RARITY_TABLE = RarityTable.builder()
      .entry(Rarity.UNCOMMON, 25)
      .entry(Rarity.RARE, 70)
      .entry(Rarity.EPIC, 5)
      .build();

  private static final RarityTable MID_TRIALS_ARMOUR_RARITY_TABLE = RarityTable.builder()
      .entry(Rarity.UNCOMMON, 70)
      .entry(Rarity.RARE, 30)
      .build();

  private static final int MID_TRIALS_ARMOUR_PIECE_COUNT = 1;

  private static final RarityTable LATE_TRIALS_WEAPON_RARITY_TABLE = RarityTable.builder()
      .entry(Rarity.RARE, 55)
      .entry(Rarity.EPIC, 42)
      .entry(Rarity.LEGENDARY, 3)
      .build();

  private static final RarityTable LATE_TRIALS_ARMOUR_RARITY_TABLE = RarityTable.builder()
      .entry(Rarity.UNCOMMON, 40)
      .entry(Rarity.RARE, 60)
      .build();

  private static final int LATE_TRIALS_ARMOUR_PIECE_COUNT = 2;

  private static final ChallengerEquipment OPENING_TRIALS_EQUIPMENT = new ChallengerEquipment(
      OPENING_TRIALS_WEAPON_RARITY_TABLE, OPENING_TRIALS_ARMOUR_RARITY_TABLE, OPENING_TRIALS_ARMOUR_PIECE_COUNT);

  private static final ChallengerEquipment EARLY_TRIALS_EQUIPMENT = new ChallengerEquipment(
      EARLY_TRIALS_WEAPON_RARITY_TABLE, EARLY_TRIALS_ARMOUR_RARITY_TABLE, EARLY_TRIALS_ARMOUR_PIECE_COUNT);

  private static final ChallengerEquipment MID_TRIALS_EQUIPMENT = new ChallengerEquipment(
      MID_TRIALS_WEAPON_RARITY_TABLE, MID_TRIALS_ARMOUR_RARITY_TABLE, MID_TRIALS_ARMOUR_PIECE_COUNT);

  private static final ChallengerEquipment LATE_TRIALS_EQUIPMENT = new ChallengerEquipment(
      LATE_TRIALS_WEAPON_RARITY_TABLE, LATE_TRIALS_ARMOUR_RARITY_TABLE, LATE_TRIALS_ARMOUR_PIECE_COUNT);

  public ChallengerEquipment {
    if (weaponRarityTable == null) {
      throw new IllegalArgumentException("weaponRarityTable must not be null");
    }
    if (armourRarityTable == null) {
      throw new IllegalArgumentException("armourRarityTable must not be null");
    }
    if (armourPieceCount < 1) {
      throw new IllegalArgumentException("armourPieceCount must be >= 1, was: " + armourPieceCount);
    }
  }

  /**
   * L'equipaggiamento della fascia a cui appartiene questa stazione. Oltre la decima prova si
   * applica l'ultima fascia, come già fa {@link TrialLoot#forTrial(int)} col suo {@code default}:
   * un percorso più lungo di dieci stazioni non deve restare senza fascia.
   *
   * @param trialNumber il numero della stazione, quello dichiarato da {@link TrialStation#number()}
   * @return l'equipaggiamento della fascia corrispondente
   */
  public static ChallengerEquipment forTrial(int trialNumber) {
    return switch (trialNumber) {
      case 1, 2 -> OPENING_TRIALS_EQUIPMENT;
      case 3, 4, 5 -> EARLY_TRIALS_EQUIPMENT;
      case 6, 7, 8 -> MID_TRIALS_EQUIPMENT;
      default -> LATE_TRIALS_EQUIPMENT;
    };
  }

  /**
   * Il grado subito sopra quello ricevuto, mai oltre {@code LEGENDARY}: usato per l'arma dello
   * sfidante speculare dell'ultima prova, che vince un vantaggio sull'equipaggiamento in più
   * rispetto a un qualunque sfidante generato della sua stessa fascia — il grado di partenza si
   * estrae dalla {@link #weaponRarityTable()} della fascia, non dalla tabella dell'armatura.
   * Funzione pura, senza alcuna estrazione: chi chiama ha già pescato il grado di partenza.
   *
   * @param rarity il grado di partenza, non nullo
   * @return il grado immediatamente superiore, oppure {@code LEGENDARY} se {@code rarity} lo è già
   */
  public static Rarity oneGradeAbove(Rarity rarity) {
    if (rarity == null) {
      throw new IllegalArgumentException("rarity must not be null");
    }
    Rarity[] grades = Rarity.values();
    int elevatedOrdinal = Math.min(rarity.ordinal() + 1, grades.length - 1);
    return grades[elevatedOrdinal];
  }
}
