package it.fantasyarena.combat.hero;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.testsupport.CombatFixtures;
import it.fantasytoolkitcore.core.model.Armour;

/**
 * Il bottino raccoglie l'equipaggiamento dei soli avversari caduti: chi resta in piedi a fine
 * scontro se lo tiene.
 */
class SpoilsTest {

  private static final int LETHAL_DAMAGE = 10_000;

  @Test
  void raccoglieArmaEPezziDegliAvversariAbbattuti() {
    Fighter fallen = CombatFixtures.createArmouredFighter("Caduto", 10, 10, 10, 10, 10, 7,
        List.of(CombatFixtures.createArmourPiece(Armour.CHESTPLATE, 4),
            CombatFixtures.createArmourPiece(Armour.HELMET, 2)));
    fallen.state().applyDamage(LETHAL_DAMAGE);

    Spoils spoils = Spoils.from(List.of(fallen));

    assertEquals(1, spoils.weapons().size());
    assertEquals(7, spoils.weapons().getFirst().attack());
    assertEquals(2, spoils.armourPieces().size());
  }

  @Test
  void ignoraGliAvversariAncoraInPiedi() {
    Fighter survivor = CombatFixtures.createFighter("Superstite", 10, 10, 10, 10, 10, 7, 4);
    Fighter fallen = CombatFixtures.createFighter("Caduto", 10, 10, 10, 10, 10, 3, 2);
    fallen.state().applyDamage(LETHAL_DAMAGE);

    Spoils spoils = Spoils.from(List.of(survivor, fallen));

    assertEquals(1, spoils.weapons().size(), "solo l'equipaggiamento di chi è caduto finisce nel bottino");
    assertEquals(3, spoils.weapons().getFirst().attack());
    assertEquals(1, spoils.armourPieces().size());
    assertEquals(2, spoils.armourPieces().getFirst().defense());
  }

  @Test
  void senzaCadutiIlBottinoEVuoto() {
    Fighter survivor = CombatFixtures.createFighter("Superstite", 10, 10, 10, 10, 10, 7, 4);

    assertTrue(Spoils.from(List.of(survivor)).isEmpty());
  }
}
