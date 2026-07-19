package it.fantasyarena.combat.rating;

import it.fantasyarena.combat.model.IntrinsicRatings;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;

/**
 * Strategia sostituibile per il calcolo dei Rating intrinseci di un combattente a partire
 * da caratteristiche, equipaggiamento, classe e razza. Nessun accesso all'avversario o al
 * {@code CombatContext}.
 */
public interface RatingStrategy {

  /**
   * Calcola i Rating intrinseci. Lo scudo è opzionale: passare {@code null} se assente.
   */
  IntrinsicRatings computeRatings(CharacterResult character, WeaponResult weapon, ArmourResult armour,
      ArmourResult shield);
}
