package it.fantasyarena.combat.hero;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import it.fantasyarena.combat.hero.HeroProgress.ArmourUpgrade;
import it.fantasyarena.combat.hero.HeroProgress.CharacteristicGain;
import it.fantasyarena.combat.hero.HeroProgress.WeaponSwap;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.Characteristic;

/**
 * Tutte le scelte del protagonista, in un posto solo: quale arma impugnare fra la sua e quelle
 * rimaste sul terreno, quali pezzi d'armatura raccogliere, dove finiscono i punti caratteristica
 * guadagnati con la vittoria. È deliberatamente l'unico punto da toccare per ribilanciare la
 * progressione: l'{@code Arena} scandisce i round e non decide niente, il motore non sa nemmeno
 * che esista una progressione.
 *
 * <p>I criteri di scelta sono due comparatori, ed è lì che si interviene. Oggi dicono: vale di più
 * il pezzo che colpisce o para di più, e a parità di valore quello più raro. La rarità è lo
 * spareggio e non il criterio, perché è il valore numerico — non il colore del nome — a entrare
 * davvero nei Rating calcolati dal motore.
 *
 * <p>La casualità di questa classe (dove cadono i punti caratteristica) è una deroga consapevole
 * alla regola per cui nel gioco il caso vive solo in {@code FighterFactory}: è casualità di
 * progressione, non di generazione. Il {@link Random} è iniettabile proprio perché i test possano
 * pilotarla.
 */
public class HeroBrain {

  /**
   * I punti che una vittoria vale. Alzarlo accelera la crescita del protagonista round dopo round.
   */
  private static final int CHARACTERISTIC_POINTS_PER_VICTORY = 3;

  private static final Comparator<WeaponResult> BY_OFFENSIVE_VALUE = Comparator
      .comparingInt(WeaponResult::attack)
      .thenComparingInt(weapon -> weapon.rarity().ordinal());

  private static final Comparator<ArmourResult> BY_DEFENSIVE_VALUE = Comparator
      .comparingInt(ArmourResult::defense)
      .thenComparingInt(piece -> piece.rarity().ordinal());

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
   * La procedura di fine scontro, dal punto di vista di chi l'ha vinto: si guarda intorno, prende
   * quel che gli serve dal bottino e spende i punti guadagnati. Restituisce la scheda cresciuta
   * insieme al racconto di come ci si è arrivati.
   *
   * <p>La cura completa non compare qui perché non è una scelta: è una conseguenza del fatto che
   * il combattente del round successivo nasce nuovo dalla scheda.
   */
  public HeroProgress progressAfterVictory(Hero hero, Spoils spoils) {
    validate(hero, spoils);

    WeaponSwap weaponSwap = chooseBetterWeapon(hero, spoils);
    ArmourChoice armourChoice = chooseArmourPieces(hero, spoils);
    List<CharacteristicGain> characteristicGains = distributeCharacteristicPoints(hero.character());
    Hero grownHero = applyGrowth(hero, weaponSwap, armourChoice, characteristicGains);

    return new HeroProgress(grownHero, weaponSwap, armourChoice.newPieces(), armourChoice.upgrades(),
        characteristicGains);
  }

  /**
   * L'arma migliore fra quelle a terra, se batte quella che il protagonista già impugna. A parità
   * di valore tiene la sua: non si cambia arma per niente.
   */
  private WeaponSwap chooseBetterWeapon(Hero hero, Spoils spoils) {
    WeaponResult currentWeapon = hero.weapon();
    Optional<WeaponResult> bestOnTheGround = spoils.weapons().stream().max(BY_OFFENSIVE_VALUE);
    boolean worthSwapping = bestOnTheGround
        .filter(candidate -> BY_OFFENSIVE_VALUE.compare(candidate, currentWeapon) > 0)
        .isPresent();

    return worthSwapping ? new WeaponSwap(currentWeapon, bestOnTheGround.get()) : null;
  }

  /**
   * Slot per slot: se quella parte del corpo è scoperta il pezzo si prende comunque, se è già
   * protetta si prende solo se difende di più di quello indossato.
   */
  private ArmourChoice chooseArmourPieces(Hero hero, Spoils spoils) {
    List<ArmourResult> newPieces = new ArrayList<>();
    List<ArmourUpgrade> upgrades = new ArrayList<>();

    bestPieceBySlot(spoils.armourPieces()).forEach((slot, candidate) -> {
      Optional<ArmourResult> wornPiece = hero.pieceCovering(slot);
      if (wornPiece.isEmpty()) {
        newPieces.add(candidate);
      } else if (BY_DEFENSIVE_VALUE.compare(candidate, wornPiece.get()) > 0) {
        upgrades.add(new ArmourUpgrade(wornPiece.get(), candidate));
      }
    });
    return new ArmourChoice(newPieces, upgrades);
  }

  /**
   * Del bottino interessa un solo pezzo per slot, il migliore: due elmi raccolti insieme non si
   * indossano entrambi.
   */
  private Map<Armour, ArmourResult> bestPieceBySlot(List<ArmourResult> armourPieces) {
    return armourPieces.stream()
        .collect(Collectors.toMap(ArmourResult::armour, piece -> piece,
            BinaryOperator.maxBy(BY_DEFENSIVE_VALUE), () -> new EnumMap<>(Armour.class)));
  }

  /**
   * I punti della vittoria cadono uno alla volta su una caratteristica estratta a caso, come fa il
   * generatore del toolkit quando distribuisce il monte punti iniziale. Il risultato è aggregato:
   * tre punti finiti sulla stessa caratteristica sono una voce sola da 3.
   */
  private List<CharacteristicGain> distributeCharacteristicPoints(CharacterResult character) {
    List<CharacterCharacteristic> characteristics = character.characteristics();
    Map<Characteristic, Integer> pointsByCharacteristic = new EnumMap<>(Characteristic.class);

    for (int point = 0; point < CHARACTERISTIC_POINTS_PER_VICTORY; point++) {
      Characteristic drawn = characteristics.get(random.nextInt(characteristics.size())).characteristic();
      pointsByCharacteristic.merge(drawn, 1, Integer::sum);
    }
    return pointsByCharacteristic.entrySet().stream()
        .map(entry -> new CharacteristicGain(entry.getKey(), entry.getValue()))
        .toList();
  }

  private Hero applyGrowth(Hero hero, WeaponSwap weaponSwap, ArmourChoice armourChoice,
      List<CharacteristicGain> characteristicGains) {
    Hero grownHero = hero.withCharacter(grow(hero.character(), characteristicGains));
    if (weaponSwap != null) {
      grownHero = grownHero.withWeapon(weaponSwap.taken());
    }
    for (ArmourResult piece : armourChoice.takenPieces()) {
      grownHero = grownHero.wearing(piece);
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

  private void validate(Hero hero, Spoils spoils) {
    if (hero == null) {
      throw new IllegalArgumentException("hero must not be null");
    }
    if (spoils == null) {
      throw new IllegalArgumentException("spoils must not be null: use an empty Spoils instead");
    }
  }

  /**
   * L'esito della cernita del bottino, distinto fra parti del corpo prima scoperte e rimpiazzi di
   * pezzi già indossati: sono due eventi diversi da raccontare, e restano diversi fino al logger.
   */
  private record ArmourChoice(List<ArmourResult> newPieces, List<ArmourUpgrade> upgrades) {

    List<ArmourResult> takenPieces() {
      List<ArmourResult> taken = new ArrayList<>(newPieces);
      upgrades.forEach(upgrade -> taken.add(upgrade.taken()));
      return taken;
    }
  }
}
