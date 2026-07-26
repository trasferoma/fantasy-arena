package it.fantasyarena.combat.hero;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;

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
 * <p>L'armatura è indicizzata per slot: è la forma che serve a decidere se un pezzo raccolto sul
 * campo copre una parte del corpo ancora scoperta oppure ne rimpiazza una già protetta. Due pezzi
 * dello stesso slot non possono convivere: l'ultimo passato vince.
 */
public final class Hero {

  private final CharacterResult character;
  private final WeaponResult weapon;
  private final Map<Armour, ArmourResult> armourBySlot;

  public Hero(CharacterResult character, WeaponResult weapon, Collection<ArmourResult> armourPieces) {
    validate(character, weapon, armourPieces);
    this.character = character;
    this.weapon = weapon;
    this.armourBySlot = indexBySlot(armourPieces);
  }

  private Hero(CharacterResult character, WeaponResult weapon, EnumMap<Armour, ArmourResult> armourBySlot) {
    this.character = character;
    this.weapon = weapon;
    this.armourBySlot = armourBySlot;
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
   * Somma delle caratteristiche del personaggio: è la misura con cui si genera lo sfidante
   * speculare dell'ultimo round, quello pari di statistiche ma armato meglio.
   */
  public int totalCharacteristicPoints() {
    return character.characteristics().stream()
        .mapToInt(CharacterCharacteristic::value)
        .sum();
  }

  public Hero withWeapon(WeaponResult newWeapon) {
    return new Hero(character, newWeapon, new EnumMap<>(armourBySlot));
  }

  /**
   * La scheda con quel pezzo addosso: copre uno slot scoperto o ne rimpiazza il contenuto. Che
   * valga la pena indossarlo lo decide {@link HeroBrain}, non questa classe.
   */
  public Hero wearing(ArmourResult piece) {
    EnumMap<Armour, ArmourResult> updated = new EnumMap<>(armourBySlot);
    updated.put(piece.armour(), piece);
    return new Hero(character, weapon, updated);
  }

  public Hero withCharacter(CharacterResult grownCharacter) {
    return new Hero(grownCharacter, weapon, new EnumMap<>(armourBySlot));
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
