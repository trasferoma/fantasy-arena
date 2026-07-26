package it.fantasyarena.combat.hero;

import java.util.List;
import java.util.Optional;

import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Characteristic;

/**
 * Il resoconto di una procedura di fine scontro: la scheda cresciuta e, in forma di dati, tutto
 * quello che è cambiato per arrivarci. Serve a due scopi in un colpo solo — l'arena prende
 * {@link #grownHero()} e prosegue, il logger legge il resto e lo racconta all'utente — così la
 * narrazione non può divergere da quello che è davvero successo: è la stessa cosa, letta due volte.
 *
 * <p>Nessuna stringa qui dentro: la formattazione è del logger. Il cambio d'arma vive come campo
 * nullo e affiora come {@link Optional} solo in lettura, perché un'arma migliore sul terreno può
 * non esserci.
 */
public final class HeroProgress {

  private final Hero grownHero;
  private final WeaponSwap weaponSwap;
  private final List<ArmourResult> newPieces;
  private final List<ArmourUpgrade> armourUpgrades;
  private final List<CharacteristicGain> characteristicGains;

  /**
   * @param weaponSwap il cambio d'arma, {@code null} se il protagonista ha tenuto la sua
   */
  public HeroProgress(Hero grownHero, WeaponSwap weaponSwap, List<ArmourResult> newPieces,
      List<ArmourUpgrade> armourUpgrades, List<CharacteristicGain> characteristicGains) {
    this.grownHero = grownHero;
    this.weaponSwap = weaponSwap;
    this.newPieces = List.copyOf(newPieces);
    this.armourUpgrades = List.copyOf(armourUpgrades);
    this.characteristicGains = List.copyOf(characteristicGains);
  }

  public Hero grownHero() {
    return grownHero;
  }

  /**
   * Il cambio d'arma, assente se sul terreno non c'era niente di meglio di quello che già impugna.
   */
  public Optional<WeaponSwap> weaponSwap() {
    return Optional.ofNullable(weaponSwap);
  }

  /**
   * I pezzi raccolti su slot prima scoperti: parti del corpo che da adesso sono protette.
   */
  public List<ArmourResult> newPieces() {
    return newPieces;
  }

  /**
   * I pezzi che hanno rimpiazzato un'armatura già indossata perché difendono di più.
   */
  public List<ArmourUpgrade> armourUpgrades() {
    return armourUpgrades;
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
   * I punti finiti su una singola caratteristica, già aggregati: tre punti sulla stessa
   * caratteristica sono una voce da 3, non tre voci da 1.
   */
  public record CharacteristicGain(Characteristic characteristic, int points) {
  }
}
