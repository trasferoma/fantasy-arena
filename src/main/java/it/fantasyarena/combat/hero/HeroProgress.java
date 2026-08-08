package it.fantasyarena.combat.hero;

import java.util.List;
import java.util.Optional;

import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.jewelgenerator.result.JewelResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Characteristic;

/**
 * Il resoconto di una procedura di fine scontro: la scheda cresciuta e, in forma di dati, cosa ne
 * è stato dell'unico {@link Loot} trovato. Serve a due scopi in un colpo solo — l'arena prende
 * {@link #grownHero()} e prosegue, il logger legge il resto e lo racconta all'utente — così la
 * narrazione non può divergere da quello che è davvero successo: è la stessa cosa, letta due
 * volte.
 *
 * <p>Il {@link #loot()} c'è sempre, e il suo destino si legge con {@link #lootFate()}: un
 * {@link LootFate} fra otto, mutuamente esclusivo col tipo di oggetto trovato. I campi che
 * seguono restano il dettaglio da cui quel destino si ricava — un'arma diventa
 * {@link #weaponSwap()} o niente (tenuta la propria), un pezzo d'armatura diventa
 * {@link #newPiece()} o {@link #armourUpgrade()} o niente (scartato) dentro un'unica
 * {@link ArmourDecision}, un gioiello diventa {@link #newJewel()} o {@link #jewelUpgrade()} o
 * niente (scartato) dentro un'unica {@link JewelDecision} — ma chi vuole sapere solo <em>cosa</em>
 * è successo, non ricostruirlo, legge {@link #lootFate()}. Nessuna stringa qui dentro: la
 * formattazione è del renderer. I campi del destino sono nulli quando quel destino non si è
 * verificato e affiorano come {@link Optional} solo in lettura, come già {@code weaponSwap}.
 */
public final class HeroProgress {

  private final Hero grownHero;
  private final Loot loot;
  private final WeaponSwap weaponSwap;
  private final ArmourDecision armourDecision;
  private final JewelDecision jewelDecision;
  private final List<CharacteristicGain> characteristicGains;

  /**
   * @param weaponSwap il cambio d'arma, {@code null} se il protagonista ha tenuto la sua
   * @param armourDecision l'esito della cernita del pezzo d'armatura trovato, non nullo
   * @param jewelDecision l'esito della cernita del gioiello trovato, non nullo
   */
  public HeroProgress(Hero grownHero, Loot loot, WeaponSwap weaponSwap, ArmourDecision armourDecision,
      JewelDecision jewelDecision, List<CharacteristicGain> characteristicGains) {
    validate(armourDecision, jewelDecision);
    this.grownHero = grownHero;
    this.loot = loot;
    this.weaponSwap = weaponSwap;
    this.armourDecision = armourDecision;
    this.jewelDecision = jewelDecision;
    this.characteristicGains = List.copyOf(characteristicGains);
  }

  public Hero grownHero() {
    return grownHero;
  }

  /**
   * L'unico oggetto trovato in questo livello, prima ancora di sapere cosa ne è stato.
   */
  public Loot loot() {
    return loot;
  }

  /**
   * Il destino dell'oggetto trovato: il tipo di {@link #loot()} sceglie fra arma, {@link
   * ArmourDecision#fate()} e {@link JewelDecision#fate()}, che risolvono l'invariante che
   * custodiscono senza esporre ai chiamanti quale dei loro campi è valorizzato. È la lettura da
   * preferire a chi vuole discriminare il caso senza ricostruirlo da una catena di
   * {@link Optional#isPresent()}.
   */
  public LootFate lootFate() {
    if (loot.weapon().isPresent()) {
      return weaponSwap != null ? LootFate.WEAPON_TAKEN : LootFate.WEAPON_DISCARDED;
    }
    if (loot.armourPiece().isPresent()) {
      return armourDecision.fate();
    }
    return jewelDecision.fate();
  }

  /**
   * Il cambio d'arma, assente se l'arma trovata non batteva quella che già impugna.
   */
  public Optional<WeaponSwap> weaponSwap() {
    return Optional.ofNullable(weaponSwap);
  }

  /**
   * Il pezzo d'armatura raccolto su uno slot prima scoperto.
   */
  public Optional<ArmourResult> newPiece() {
    return Optional.ofNullable(armourDecision.newPiece());
  }

  /**
   * Il pezzo che ha rimpiazzato un'armatura già indossata perché difende di più.
   */
  public Optional<ArmourUpgrade> armourUpgrade() {
    return Optional.ofNullable(armourDecision.upgrade());
  }

  /**
   * Il gioiello indossato su un tipo prima scoperto.
   */
  public Optional<NewJewel> newJewel() {
    return Optional.ofNullable(jewelDecision.newJewel());
  }

  /**
   * Il gioiello che ha rimpiazzato uno già indossato dello stesso tipo perché batteva i suoi buff.
   */
  public Optional<JewelUpgrade> jewelUpgrade() {
    return Optional.ofNullable(jewelDecision.upgrade());
  }

  public List<CharacteristicGain> characteristicGains() {
    return characteristicGains;
  }

  private static void validate(ArmourDecision armourDecision, JewelDecision jewelDecision) {
    if (armourDecision == null) {
      throw new IllegalArgumentException("armourDecision must not be null");
    }
    if (jewelDecision == null) {
      throw new IllegalArgumentException("jewelDecision must not be null");
    }
  }

  /**
   * L'arma lasciata a terra e quella raccolta al suo posto.
   */
  public record WeaponSwap(WeaponResult dropped, WeaponResult taken) {
  }

  /**
   * Il pezzo scartato e quello che ora copre lo stesso slot.
   */
  public record ArmourUpgrade(ArmourResult dropped, ArmourResult taken) {
  }

  /**
   * L'esito della cernita del pezzo d'armatura trovato: custodisce l'invariante per cui al
   * massimo uno dei due campi è valorizzato, distinguendo la parte del corpo prima scoperta dal
   * rimpiazzo di un pezzo già indossato — sono due eventi diversi da raccontare, e restano diversi
   * fino al logger. {@link #takenPiece()} esiste per chi deve applicare la conquista alla scheda,
   * {@link #fate()} per chi deve solo dire com'è andata, nessuno dei due per chi vuole dedurlo
   * controllando i campi da fuori.
   */
  public record ArmourDecision(ArmourResult newPiece, ArmourUpgrade upgrade) {

    public static ArmourDecision none() {
      return new ArmourDecision(null, null);
    }

    public static ArmourDecision covering(ArmourResult piece) {
      return new ArmourDecision(piece, null);
    }

    public static ArmourDecision replacing(ArmourResult dropped, ArmourResult taken) {
      return new ArmourDecision(null, new ArmourUpgrade(dropped, taken));
    }

    /**
     * Il pezzo effettivamente conquistato, copra uno slot scoperto o ne rimpiazzi uno già
     * occupato; {@code null} se il pezzo trovato è stato scartato.
     */
    public ArmourResult takenPiece() {
      if (newPiece != null) {
        return newPiece;
      }
      return upgrade != null ? upgrade.taken() : null;
    }

    /**
     * Il destino del pezzo trovato, risolto sull'invariante che questo record custodisce: nessun
     * chiamante deve dedurlo controllando quale dei due campi non è nullo.
     */
    LootFate fate() {
      if (newPiece != null) {
        return LootFate.ARMOUR_WORN_ON_EMPTY_SLOT;
      }
      return upgrade != null ? LootFate.ARMOUR_REPLACED : LootFate.ARMOUR_DISCARDED;
    }
  }

  /**
   * Il gioiello preso su un tipo prima scoperto. Non vale più punti caratteristica di suo: i suoi
   * eventuali buff contano già attraverso {@link Hero#effectiveCharacter()}.
   */
  public record NewJewel(JewelResult jewel) {
  }

  /**
   * Il gioiello scartato e quello che ora occupa lo stesso tipo. Non vale più punti caratteristica
   * di suo: i suoi eventuali buff contano già attraverso {@link Hero#effectiveCharacter()}.
   */
  public record JewelUpgrade(JewelResult dropped, JewelResult taken) {
  }

  /**
   * L'esito della cernita del gioiello trovato: custodisce l'invariante per cui al massimo uno
   * dei due campi è valorizzato, distinguendo il tipo prima scoperto dal rimpiazzo di un gioiello
   * già indossato — sono due eventi diversi da raccontare, e restano diversi fino al logger.
   * {@link #takenJewel()} esiste per chi deve applicare la conquista alla scheda, {@link #fate()}
   * per chi deve solo dire com'è andata, nessuno dei due per chi vuole dedurlo controllando i campi
   * da fuori.
   */
  public record JewelDecision(NewJewel newJewel, JewelUpgrade upgrade) {

    public static JewelDecision none() {
      return new JewelDecision(null, null);
    }

    public static JewelDecision wearing(JewelResult jewel) {
      return new JewelDecision(new NewJewel(jewel), null);
    }

    public static JewelDecision replacing(JewelResult dropped, JewelResult taken) {
      return new JewelDecision(null, new JewelUpgrade(dropped, taken));
    }

    /**
     * Il gioiello effettivamente conquistato, occupi un tipo scoperto o ne rimpiazzi uno già
     * indossato; {@code null} se il gioiello trovato è stato scartato.
     */
    public JewelResult takenJewel() {
      if (newJewel != null) {
        return newJewel.jewel();
      }
      return upgrade != null ? upgrade.taken() : null;
    }

    /**
     * Il destino del gioiello trovato, risolto sull'invariante che questo record custodisce:
     * nessun chiamante deve dedurlo controllando quale dei due campi non è nullo.
     */
    LootFate fate() {
      if (newJewel != null) {
        return LootFate.JEWEL_WORN_ON_EMPTY_TYPE;
      }
      return upgrade != null ? LootFate.JEWEL_REPLACED : LootFate.JEWEL_DISCARDED;
    }
  }

  /**
   * I punti finiti su una singola caratteristica, già aggregati: tre punti sulla stessa
   * caratteristica sono una voce da 3, non tre voci da 1.
   */
  public record CharacteristicGain(Characteristic characteristic, int points) {
  }
}
