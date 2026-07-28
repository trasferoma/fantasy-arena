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
 * <p>Il {@link #loot()} c'è sempre, ma il suo destino è uno solo fra sei, ed è mutuamente
 * esclusivo col tipo di oggetto trovato: un'arma diventa {@link #weaponSwap()} o niente (tenuta la
 * propria), un pezzo d'armatura diventa {@link #newPiece()} o {@link #armourUpgrade()} o niente
 * (scartato), un gioiello diventa {@link #newJewel()} o {@link #jewelUpgrade()} o niente
 * (scartato). Nessuna stringa qui dentro: la formattazione è del renderer. I campi del destino sono
 * nulli quando quel destino non si è verificato e affiorano come {@link Optional} solo in lettura,
 * come già {@code weaponSwap} prima di questo cambio.
 */
public final class HeroProgress {

  private final Hero grownHero;
  private final Loot loot;
  private final WeaponSwap weaponSwap;
  private final ArmourResult newPiece;
  private final ArmourUpgrade armourUpgrade;
  private final NewJewel newJewel;
  private final JewelUpgrade jewelUpgrade;
  private final List<CharacteristicGain> characteristicGains;

  /**
   * @param weaponSwap il cambio d'arma, {@code null} se il protagonista ha tenuto la sua
   * @param newPiece il pezzo raccolto su uno slot prima scoperto, {@code null} altrimenti
   * @param armourUpgrade il rimpiazzo di un pezzo già indossato, {@code null} altrimenti
   * @param newJewel il gioiello indossato su un tipo prima scoperto, {@code null} altrimenti
   * @param jewelUpgrade il rimpiazzo di un gioiello già indossato, {@code null} altrimenti
   */
  public HeroProgress(Hero grownHero, Loot loot, WeaponSwap weaponSwap, ArmourResult newPiece,
      ArmourUpgrade armourUpgrade, NewJewel newJewel, JewelUpgrade jewelUpgrade,
      List<CharacteristicGain> characteristicGains) {
    this.grownHero = grownHero;
    this.loot = loot;
    this.weaponSwap = weaponSwap;
    this.newPiece = newPiece;
    this.armourUpgrade = armourUpgrade;
    this.newJewel = newJewel;
    this.jewelUpgrade = jewelUpgrade;
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
   * Il cambio d'arma, assente se l'arma trovata non batteva quella che già impugna.
   */
  public Optional<WeaponSwap> weaponSwap() {
    return Optional.ofNullable(weaponSwap);
  }

  /**
   * Il pezzo d'armatura raccolto su uno slot prima scoperto.
   */
  public Optional<ArmourResult> newPiece() {
    return Optional.ofNullable(newPiece);
  }

  /**
   * Il pezzo che ha rimpiazzato un'armatura già indossata perché difende di più.
   */
  public Optional<ArmourUpgrade> armourUpgrade() {
    return Optional.ofNullable(armourUpgrade);
  }

  /**
   * Il gioiello indossato su un tipo prima scoperto, con i punti caratteristica che vale.
   */
  public Optional<NewJewel> newJewel() {
    return Optional.ofNullable(newJewel);
  }

  /**
   * Il gioiello che ha rimpiazzato uno già indossato dello stesso tipo perché più raro, con i
   * punti caratteristica che vale.
   */
  public Optional<JewelUpgrade> jewelUpgrade() {
    return Optional.ofNullable(jewelUpgrade);
  }

  public List<CharacteristicGain> characteristicGains() {
    return characteristicGains;
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
   * Il gioiello preso su un tipo prima scoperto, e i punti caratteristica che vale secondo la sua
   * rarità.
   */
  public record NewJewel(JewelResult jewel, int points) {
  }

  /**
   * Il gioiello scartato e quello che ora occupa lo stesso tipo, con i punti caratteristica che il
   * gioiello preso vale.
   */
  public record JewelUpgrade(JewelResult dropped, JewelResult taken, int points) {
  }

  /**
   * I punti finiti su una singola caratteristica, già aggregati: tre punti sulla stessa
   * caratteristica sono una voce da 3, non tre voci da 1.
   */
  public record CharacteristicGain(Characteristic characteristic, int points) {
  }
}
