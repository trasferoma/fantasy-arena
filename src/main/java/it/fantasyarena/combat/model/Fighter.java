package it.fantasyarena.combat.model;

import java.util.Optional;

import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;

/**
 * Aggregato dati del combattente: personaggio, equipaggiamento, Rating intrinseci (immutabili)
 * e stato mutabile di combattimento. È un aggregato di dati: non contiene formule, che restano
 * nei resolver del core.
 */
public final class Fighter {

  private final CharacterResult character;
  private final WeaponResult weapon;
  private final ArmourResult armour;
  private final ArmourResult shield;
  private final IntrinsicRatings ratings;
  private final FighterState state;

  public Fighter(CharacterResult character, WeaponResult weapon, ArmourResult armour, ArmourResult shield,
      IntrinsicRatings ratings) {
    this.character = character;
    this.weapon = weapon;
    this.armour = armour;
    this.shield = shield;
    this.ratings = ratings;
    this.state = new FighterState(ratings.maxHealth(), ratings.maxStamina());
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

  public ArmourResult armour() {
    return armour;
  }

  /**
   * Scudo opzionale: previsto dal modello ma non esercitato dai due spadaccini in v1.
   */
  public Optional<ArmourResult> shield() {
    return Optional.ofNullable(shield);
  }

  public IntrinsicRatings ratings() {
    return ratings;
  }

  public FighterState state() {
    return state;
  }

  public boolean isDefeated() {
    return state.isDefeated();
  }
}
