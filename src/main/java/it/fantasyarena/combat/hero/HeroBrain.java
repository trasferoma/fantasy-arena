package it.fantasyarena.combat.hero;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import it.fantasyarena.combat.hero.HeroProgress.ArmourUpgrade;
import it.fantasyarena.combat.hero.HeroProgress.CharacteristicGain;
import it.fantasyarena.combat.hero.HeroProgress.JewelUpgrade;
import it.fantasyarena.combat.hero.HeroProgress.NewJewel;
import it.fantasyarena.combat.hero.HeroProgress.WeaponSwap;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.jewelgenerator.result.JewelResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Characteristic;
import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.RarityTable;

/**
 * Tutte le scelte del protagonista, in un posto solo: se l'unico oggetto trovato a fine livello
 * vale la pena impugnarlo o indossarlo, dove finiscono i tre punti guadagnati con la vittoria. È
 * deliberatamente l'unico punto da toccare per ribilanciare la progressione: l'{@code Arena}
 * scandisce i round e non decide niente, il {@code FighterFactory} genera l'oggetto ma non giudica
 * se vale la pena tenerlo, il motore non sa nemmeno che esista una progressione.
 *
 * <p>I criteri di scelta per arma, armatura e gioiello sono tre comparatori, ed è lì che si
 * interviene. Per arma e armatura vale di più il pezzo che colpisce o para di più, e per il
 * gioiello vale di più quello coi buff complessivamente maggiori: in tutti e tre i casi la rarità
 * è solo lo spareggio a parità di valore, mai il criterio. A parità piena si tiene il proprio, in
 * tutte e tre le categorie. Il gioiello non vale più punti caratteristica di suo: i suoi buff, come
 * quelli di arma e armatura, contano attraverso {@link Hero#effectiveCharacter()}, non attraverso
 * la progressione.
 *
 * <p>La casualità di questa classe (dove cadono i punti caratteristica della vittoria) è una
 * deroga consapevole alla regola per cui nel gioco il caso vive solo in {@code FighterFactory}: è
 * casualità di progressione, non di generazione. Il {@link Random} è iniettabile proprio perché i
 * test possano pilotarla.
 */
public class HeroBrain {

  /**
   * I punti che una vittoria vale. Alzarlo accelera la crescita del protagonista round dopo round.
   */
  private static final int CHARACTERISTIC_POINTS_PER_VICTORY = 3;

  /**
   * La distribuzione della rarità del loot alle prove 1-2: {@code LEGENDARY} resta accessibile ma
   * marginale, e il pavimento è {@code UNCOMMON}.
   */
  private static final RarityTable OPENING_TRIALS_LOOT_RARITY_TABLE = RarityTable.builder()
      .entry(Rarity.UNCOMMON, 70)
      .entry(Rarity.RARE, 20)
      .entry(Rarity.EPIC, 5)
      .entry(Rarity.LEGENDARY, 5)
      .build();

  /**
   * La distribuzione della rarità del loot alle prove 3-5: il pavimento resta {@code UNCOMMON}
   * come in {@link #OPENING_TRIALS_LOOT_RARITY_TABLE}, ma il peso si sposta verso {@code RARE}.
   */
  private static final RarityTable EARLY_TRIALS_LOOT_RARITY_TABLE = RarityTable.builder()
      .entry(Rarity.UNCOMMON, 65)
      .entry(Rarity.RARE, 20)
      .entry(Rarity.EPIC, 10)
      .entry(Rarity.LEGENDARY, 5)
      .build();

  /**
   * La distribuzione della rarità del loot alle prove 6-8: qui, e non prima, il pavimento sale a
   * {@code RARE}, e il peso principale resta su {@code RARE} stesso, con {@code EPIC} minoritario.
   */
  private static final RarityTable MID_TRIALS_LOOT_RARITY_TABLE = RarityTable.builder()
      .entry(Rarity.RARE, 85)
      .entry(Rarity.EPIC, 10)
      .entry(Rarity.LEGENDARY, 5)
      .build();

  /**
   * La distribuzione della rarità del loot alle prove 9-10, l'ultimo tratto del percorso: pesi
   * identici a {@link #MID_TRIALS_LOOT_RARITY_TABLE}. Il pavimento resta {@code RARE} e
   * {@code LEGENDARY} non cresce oltre il suo peso dello scaglione precedente: la taratura
   * empirica non ha richiesto di inasprire ulteriormente il loot dell'eroe in questo tratto.
   */
  private static final RarityTable LATE_TRIALS_LOOT_RARITY_TABLE = RarityTable.builder()
      .entry(Rarity.RARE, 85)
      .entry(Rarity.EPIC, 10)
      .entry(Rarity.LEGENDARY, 5)
      .build();

  private static final Comparator<WeaponResult> BY_OFFENSIVE_VALUE = Comparator
      .comparingInt(WeaponResult::attack)
      .thenComparingInt(weapon -> weapon.rarity().ordinal());

  private static final Comparator<ArmourResult> BY_DEFENSIVE_VALUE = Comparator
      .comparingInt(ArmourResult::defense)
      .thenComparingInt(piece -> piece.rarity().ordinal());

  /**
   * Il gioiello non ha attacco né difesa da confrontare: il criterio è il valore totale dei suoi
   * buff, con la rarità come spareggio — la stessa forma di {@link #BY_OFFENSIVE_VALUE} e
   * {@link #BY_DEFENSIVE_VALUE}.
   */
  private static final Comparator<JewelResult> BY_JEWEL_VALUE = Comparator
      .<JewelResult>comparingInt(jewel -> EquipmentBonus.totalValueOf(jewel.buffs()))
      .thenComparingInt(jewel -> jewel.rarity().ordinal());

  private final Random random;

  public HeroBrain() {
    this(new Random());
  }

  public HeroBrain(Random random) {
    if (random == null) {
      throw new IllegalArgumentException("random must not be null");
    }
    this.random = random;
  }

  /**
   * La tabella con cui viene estratta la rarità del loot di quel livello. È il punto unico da
   * toccare per ritarare la progressione della rarità, a quattro scaglioni sul percorso a dieci
   * prove: 1-2, 3-5, 6-8, 9-10.
   *
   * <p>La distribuzione è pesata, non uniforme: un pavimento espresso come rarità minima
   * ({@code minRarity}) renderebbe equiprobabili tutti i gradi dalla soglia in su, e un
   * {@code LEGENDARY} finirebbe per uscire tanto spesso quanto il grado del pavimento stesso — col
   * risultato che il loot sopra il raro diventa la norma invece dell'eccezione. È il difetto che
   * ogni scaglione di questa tabella corregge, mantenendo {@code LEGENDARY} minoritario per tutto
   * il percorso.
   *
   * <p>Il pavimento non sale a ogni scaglione: {@code UNCOMMON} resta estraibile fino alla prova
   * 5, e sale a {@code RARE} solo dalla prova 6 in poi — l'ultimo scaglione (9-10) non alza
   * ulteriormente il pavimento e condivide con {@link #MID_TRIALS_LOOT_RARITY_TABLE} pesi identici.
   * La ragione della ritaratura: un'arma {@code LEGENDARY} ha attacco 15-25 contro 3-6 di una
   * {@code UNCOMMON} e porta buff per una decina di punti, mentre una vittoria di progressione ne
   * vale solo tre di caratteristica — un solo drop leggendario valeva quindi più di tre vittorie, e
   * con la distribuzione precedente a questa ritaratura arrivava troppo presto nella corsa.
   */
  public RarityTable lootRarityTable(int level) {
    return switch (level) {
      case 1, 2 -> OPENING_TRIALS_LOOT_RARITY_TABLE;
      case 3, 4, 5 -> EARLY_TRIALS_LOOT_RARITY_TABLE;
      case 6, 7, 8 -> MID_TRIALS_LOOT_RARITY_TABLE;
      default -> LATE_TRIALS_LOOT_RARITY_TABLE;
    };
  }

  /**
   * La procedura di fine scontro, dal punto di vista di chi l'ha vinto: valuta l'unico oggetto
   * trovato e spende i tre punti che la vittoria vale. Restituisce la scheda cresciuta insieme al
   * racconto di come ci si è arrivati.
   *
   * <p>La cura completa non compare qui perché non è una scelta: è una conseguenza del fatto che
   * il combattente del round successivo nasce nuovo dalla scheda.
   */
  public HeroProgress progressAfterVictory(Hero hero, Loot loot) {
    validate(hero, loot);

    WeaponSwap weaponSwap = loot.weapon().map(candidate -> chooseBetterWeapon(hero, candidate)).orElse(null);
    ArmourDecision armourDecision = loot.armourPiece()
        .map(candidate -> chooseArmourPiece(hero, candidate))
        .orElse(ArmourDecision.none());
    JewelDecision jewelDecision = loot.jewel()
        .map(candidate -> chooseJewel(hero, candidate))
        .orElse(JewelDecision.none());

    List<CharacteristicGain> characteristicGains =
        distributeCharacteristicPoints(hero.character(), CHARACTERISTIC_POINTS_PER_VICTORY);
    Hero grownHero = applyGrowth(hero, weaponSwap, armourDecision, jewelDecision, characteristicGains);

    return new HeroProgress(grownHero, loot, weaponSwap, armourDecision.newPiece(), armourDecision.upgrade(),
        jewelDecision.newJewel(), jewelDecision.upgrade(), characteristicGains);
  }

  /**
   * L'arma trovata la si impugna solo se batte quella che il protagonista già ha. A parità di
   * valore tiene la sua: non si cambia arma per niente.
   */
  private WeaponSwap chooseBetterWeapon(Hero hero, WeaponResult found) {
    WeaponResult currentWeapon = hero.weapon();
    boolean worthSwapping = BY_OFFENSIVE_VALUE.compare(found, currentWeapon) > 0;
    return worthSwapping ? new WeaponSwap(currentWeapon, found) : null;
  }

  /**
   * Il pezzo trovato: se quella parte del corpo è scoperta si prende comunque, se è già protetta si
   * prende solo se difende di più di quello indossato.
   */
  private ArmourDecision chooseArmourPiece(Hero hero, ArmourResult found) {
    Optional<ArmourResult> wornPiece = hero.pieceCovering(found.armour());
    if (wornPiece.isEmpty()) {
      return ArmourDecision.covering(found);
    }
    if (BY_DEFENSIVE_VALUE.compare(found, wornPiece.get()) > 0) {
      return ArmourDecision.replacing(wornPiece.get(), found);
    }
    return ArmourDecision.none();
  }

  /**
   * Il gioiello trovato: se quel tipo è scoperto lo si indossa comunque, se è già occupato lo si
   * indossa solo se i suoi buff valgono di più di quello che lo occupa. A parità di valore tiene il
   * suo, come per arma e armatura.
   */
  private JewelDecision chooseJewel(Hero hero, JewelResult found) {
    Optional<JewelResult> wornJewel = hero.jewelOfType(found.jewel());
    if (wornJewel.isEmpty()) {
      return JewelDecision.wearing(found);
    }
    if (BY_JEWEL_VALUE.compare(found, wornJewel.get()) > 0) {
      return JewelDecision.replacing(wornJewel.get(), found);
    }
    return JewelDecision.none();
  }

  /**
   * I punti cadono uno alla volta su una caratteristica estratta a caso, come fa il generatore del
   * toolkit quando distribuisce il monte punti iniziale. Il risultato è aggregato: più punti finiti
   * sulla stessa caratteristica sono una voce sola.
   */
  private List<CharacteristicGain> distributeCharacteristicPoints(CharacterResult character, int pointsToDistribute) {
    List<CharacterCharacteristic> characteristics = character.characteristics();
    Map<Characteristic, Integer> pointsByCharacteristic = new EnumMap<>(Characteristic.class);

    for (int point = 0; point < pointsToDistribute; point++) {
      Characteristic drawn = characteristics.get(random.nextInt(characteristics.size())).characteristic();
      pointsByCharacteristic.merge(drawn, 1, Integer::sum);
    }
    return pointsByCharacteristic.entrySet().stream()
        .map(entry -> new CharacteristicGain(entry.getKey(), entry.getValue()))
        .toList();
  }

  private Hero applyGrowth(Hero hero, WeaponSwap weaponSwap, ArmourDecision armourDecision,
      JewelDecision jewelDecision, List<CharacteristicGain> characteristicGains) {
    Hero grownHero = hero.withCharacter(grow(hero.character(), characteristicGains));
    if (weaponSwap != null) {
      grownHero = grownHero.withWeapon(weaponSwap.taken());
    }
    ArmourResult takenPiece = armourDecision.takenPiece();
    if (takenPiece != null) {
      grownHero = grownHero.wearing(takenPiece);
    }
    JewelResult takenJewel = jewelDecision.takenJewel();
    if (takenJewel != null) {
      grownHero = grownHero.wearing(takenJewel);
    }
    return grownHero;
  }

  /**
   * Il personaggio con i punti spesi. Il {@code CharacterResult} si ricostruisce a mano perché il
   * toolkit non espone alcuna API per far crescere un personaggio esistente: rigenerarlo col
   * {@code CharacterGeneratorTool} ripescherebbe razza, nome e caratteristiche, cioè creerebbe
   * qualcun altro.
   */
  private CharacterResult grow(CharacterResult character, List<CharacteristicGain> characteristicGains) {
    Map<Characteristic, Integer> pointsByCharacteristic = characteristicGains.stream()
        .collect(Collectors.toMap(CharacteristicGain::characteristic, CharacteristicGain::points));
    List<CharacterCharacteristic> grownCharacteristics = character.characteristics().stream()
        .map(entry -> new CharacterCharacteristic(entry.characteristic(),
            entry.value() + pointsByCharacteristic.getOrDefault(entry.characteristic(), 0)))
        .toList();

    return new CharacterResult(character.race(), character.characterClass(), character.name(),
        grownCharacteristics);
  }

  private void validate(Hero hero, Loot loot) {
    if (hero == null) {
      throw new IllegalArgumentException("hero must not be null");
    }
    if (loot == null) {
      throw new IllegalArgumentException("loot must not be null");
    }
  }

  /**
   * L'esito della cernita del pezzo d'armatura trovato, distinto fra parte del corpo prima scoperta
   * e rimpiazzo di un pezzo già indossato: sono due eventi diversi da raccontare, e restano diversi
   * fino al logger. Nessuno dei due campi è presente quando il pezzo trovato è stato scartato.
   */
  private record ArmourDecision(ArmourResult newPiece, ArmourUpgrade upgrade) {

    private static ArmourDecision none() {
      return new ArmourDecision(null, null);
    }

    private static ArmourDecision covering(ArmourResult piece) {
      return new ArmourDecision(piece, null);
    }

    private static ArmourDecision replacing(ArmourResult dropped, ArmourResult taken) {
      return new ArmourDecision(null, new ArmourUpgrade(dropped, taken));
    }

    private ArmourResult takenPiece() {
      if (newPiece != null) {
        return newPiece;
      }
      return upgrade != null ? upgrade.taken() : null;
    }
  }

  /**
   * L'esito della cernita del gioiello trovato, distinto fra tipo prima scoperto e rimpiazzo di un
   * gioiello già indossato: sono due eventi diversi da raccontare, e restano diversi fino al
   * logger. Nessuno dei due campi è presente quando il gioiello trovato è stato scartato.
   */
  private record JewelDecision(NewJewel newJewel, JewelUpgrade upgrade) {

    private static JewelDecision none() {
      return new JewelDecision(null, null);
    }

    private static JewelDecision wearing(JewelResult jewel) {
      return new JewelDecision(new NewJewel(jewel), null);
    }

    private static JewelDecision replacing(JewelResult dropped, JewelResult taken) {
      return new JewelDecision(null, new JewelUpgrade(dropped, taken));
    }

    private JewelResult takenJewel() {
      if (newJewel != null) {
        return newJewel.jewel();
      }
      return upgrade != null ? upgrade.taken() : null;
    }
  }
}
