package it.fantasyarena.combat.hero;

import java.util.Optional;

import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.jewelgenerator.result.JewelResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;

/**
 * L'unico oggetto trovato alla fine di un livello vinto: un'arma, un pezzo d'armatura o un
 * gioiello, mai più di uno alla volta. Sostituisce la vecchia raccolta dai caduti — qui non c'è
 * una lista da scremare, solo l'esito già deciso da {@link it.fantasyarena.combat.factory
 * FighterFactory} su quale dei tre tipi è toccato in sorte.
 *
 * <p>Le tre factory statiche sono le uniche vie di costruzione, e ciascuna valorizza un solo
 * campo: è così che l'invariante "esattamente un elemento presente" si mantiene senza bisogno di
 * un controllo a runtime. Chi legge scioglie l'ambiguità con gli accessor {@link Optional}, nello
 * stesso stile di {@link HeroProgress#weaponSwap()}.
 */
public final class Loot {

  private final WeaponResult weapon;
  private final ArmourResult armourPiece;
  private final JewelResult jewel;

  private Loot(WeaponResult weapon, ArmourResult armourPiece, JewelResult jewel) {
    this.weapon = weapon;
    this.armourPiece = armourPiece;
    this.jewel = jewel;
  }

  public static Loot ofWeapon(WeaponResult weapon) {
    if (weapon == null) {
      throw new IllegalArgumentException("weapon must not be null");
    }
    return new Loot(weapon, null, null);
  }

  public static Loot ofArmourPiece(ArmourResult armourPiece) {
    if (armourPiece == null) {
      throw new IllegalArgumentException("armourPiece must not be null");
    }
    return new Loot(null, armourPiece, null);
  }

  public static Loot ofJewel(JewelResult jewel) {
    if (jewel == null) {
      throw new IllegalArgumentException("jewel must not be null");
    }
    return new Loot(null, null, jewel);
  }

  public Optional<WeaponResult> weapon() {
    return Optional.ofNullable(weapon);
  }

  public Optional<ArmourResult> armourPiece() {
    return Optional.ofNullable(armourPiece);
  }

  public Optional<JewelResult> jewel() {
    return Optional.ofNullable(jewel);
  }
}
