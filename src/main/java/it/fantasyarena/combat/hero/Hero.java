package it.fantasyarena.combat.hero;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.jewelgenerator.result.JewelResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.Jewel;

/**
 * La scheda del protagonista fra uno scontro e l'altro: chi è, cosa impugna, cosa indossa. È
 * quello che sopravvive ai round, mentre il {@code Fighter} vive un solo scontro e ne esce ferito.
 *
 * <p>Immutabile: ogni conquista produce una scheda nuova invece di modificare questa. È la
 * proprietà su cui si regge la cura di fine scontro — il protagonista non viene "guarito", viene
 * rimandato in campo come combattente nuovo dalla stessa scheda, e vita e stamina ripartono piene
 * perché è così che nasce ogni {@code Fighter}. Chi materializza il combattente è
 * {@code FighterFactory}, l'unico posto che possiede l'assemblatore del motore.
 *
 * <p>L'armatura è indicizzata per slot e i gioielli per tipo: è la forma che serve a decidere se
 * un pezzo raccolto sul campo copre una parte del corpo (o un tipo di gioiello) ancora scoperta
 * oppure ne rimpiazza una già occupata. Due pezzi dello stesso slot, o due gioielli dello stesso
 * tipo, non possono convivere: l'ultimo passato vince. A differenza dell'armatura, nessun
 * protagonista nasce con un gioiello: la mappa parte vuota e si popola solo per conquista.
 *
 * <p>I gioielli sono custoditi ma non entrano nello scontro: {@code FighterFactory.summon} non li
 * passa al {@code FighterAssembler} del motore, che non sa montarli.
 */
public final class Hero {

  private final CharacterResult character;
  private final WeaponResult weapon;
  private final Map<Armour, ArmourResult> armourBySlot;
  private final Map<Jewel, JewelResult> jewelsByType;

  public Hero(CharacterResult character, WeaponResult weapon, Collection<ArmourResult> armourPieces) {
    validate(character, weapon, armourPieces);
    this.character = character;
    this.weapon = weapon;
    this.armourBySlot = indexBySlot(armourPieces);
    this.jewelsByType = new EnumMap<>(Jewel.class);
  }

  private Hero(CharacterResult character, WeaponResult weapon, EnumMap<Armour, ArmourResult> armourBySlot,
      EnumMap<Jewel, JewelResult> jewelsByType) {
    this.character = character;
    this.weapon = weapon;
    this.armourBySlot = armourBySlot;
    this.jewelsByType = jewelsByType;
  }

  public String name() {
    return character.name();
  }

  public CharacterResult character() {
    return character;
  }

  public WeaponResult weapon() {
    return weapon;
  }

  /**
   * I pezzi indossati, sempre nell'ordine degli slot di {@link Armour}: la scheda a schermo non
   * deve cambiare disposizione da un round all'altro solo perché è cambiato l'ordine di raccolta.
   */
  public List<ArmourResult> armourPieces() {
    return List.copyOf(armourBySlot.values());
  }

  /**
   * Il pezzo che copre quello slot, se il protagonista ne indossa uno.
   */
  public Optional<ArmourResult> pieceCovering(Armour slot) {
    return Optional.ofNullable(armourBySlot.get(slot));
  }

  public int armourPieceCount() {
    return armourBySlot.size();
  }

  /**
   * I gioielli indossati, sempre nell'ordine di {@link Jewel}, per la stessa ragione per cui
   * {@link #armourPieces()} segue l'ordine di {@link Armour}.
   */
  public List<JewelResult> jewels() {
    return List.copyOf(jewelsByType.values());
  }

  /**
   * Il gioiello indossato di quel tipo, se il protagonista ne porta uno.
   */
  public Optional<JewelResult> jewelOfType(Jewel type) {
    return Optional.ofNullable(jewelsByType.get(type));
  }

  public int jewelCount() {
    return jewelsByType.size();
  }

  /**
   * Somma delle caratteristiche del personaggio: è la misura con cui si genera lo sfidante
   * speculare dell'ultimo round, quello pari di statistiche ma armato meglio.
   */
  public int totalCharacteristicPoints() {
    return character.characteristics().stream()
        .mapToInt(CharacterCharacteristic::value)
        .sum();
  }

  public Hero withWeapon(WeaponResult newWeapon) {
    return new Hero(character, newWeapon, new EnumMap<>(armourBySlot), new EnumMap<>(jewelsByType));
  }

  /**
   * La scheda con quel pezzo addosso: copre uno slot scoperto o ne rimpiazza il contenuto. Che
   * valga la pena indossarlo lo decide {@link HeroBrain}, non questa classe.
   */
  public Hero wearing(ArmourResult piece) {
    EnumMap<Armour, ArmourResult> updated = new EnumMap<>(armourBySlot);
    updated.put(piece.armour(), piece);
    return new Hero(character, weapon, updated, new EnumMap<>(jewelsByType));
  }

  /**
   * La scheda con quel gioiello addosso: occupa un tipo scoperto o ne rimpiazza il contenuto. Che
   * valga la pena indossarlo lo decide {@link HeroBrain}, non questa classe.
   */
  public Hero wearing(JewelResult jewel) {
    EnumMap<Jewel, JewelResult> updated = new EnumMap<>(jewelsByType);
    updated.put(jewel.jewel(), jewel);
    return new Hero(character, weapon, new EnumMap<>(armourBySlot), updated);
  }

  public Hero withCharacter(CharacterResult grownCharacter) {
    return new Hero(grownCharacter, weapon, new EnumMap<>(armourBySlot), new EnumMap<>(jewelsByType));
  }

  private static EnumMap<Armour, ArmourResult> indexBySlot(Collection<ArmourResult> armourPieces) {
    EnumMap<Armour, ArmourResult> bySlot = new EnumMap<>(Armour.class);
    armourPieces.forEach(piece -> bySlot.put(piece.armour(), piece));
    return bySlot;
  }

  private static void validate(CharacterResult character, WeaponResult weapon,
      Collection<ArmourResult> armourPieces) {
    if (character == null) {
      throw new IllegalArgumentException("character must not be null");
    }
    if (weapon == null) {
      throw new IllegalArgumentException("weapon must not be null: " + character.name() + " has nothing to fight with");
    }
    if (armourPieces == null || armourPieces.isEmpty()) {
      throw new IllegalArgumentException("armourPieces must not be null or empty: " + character.name()
          + " cannot enter the arena unarmoured");
    }
  }
}
