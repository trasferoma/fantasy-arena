package it.fantasyarena.combat.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.hero.Hero;
import it.fantasyarena.combat.hero.HeroProgress;
import it.fantasyarena.combat.hero.HeroProgress.ArmourUpgrade;
import it.fantasyarena.combat.hero.HeroProgress.CharacteristicGain;
import it.fantasyarena.combat.hero.HeroProgress.WeaponSwap;
import it.fantasycombatsystem.testsupport.CombatFixtures;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.Characteristic;
import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.Weapon;

/**
 * Il racconto della procedura di fine scontro, verificato sul testo prodotto: deve dire sempre
 * qualcosa su cura, arma, armatura e crescita, anche quando dal terreno non è arrivato niente.
 */
class HeroProgressFormatterTest {

  private final HeroProgressFormatter formatter = new HeroProgressFormatter();

  @Test
  void raccontaCuraArmaturaArmaECrescita() {
    HeroProgress progress = new HeroProgress(heroWith(sword(9, Rarity.RARE), piece(Armour.CHESTPLATE, 6)),
        new WeaponSwap(sword(4, Rarity.COMMON), sword(9, Rarity.RARE)),
        List.of(piece(Armour.HELMET, 3)),
        List.of(new ArmourUpgrade(piece(Armour.CHESTPLATE, 2), piece(Armour.CHESTPLATE, 6))),
        List.of(new CharacteristicGain(Characteristic.STRENGTH, 2),
            new CharacteristicGain(Characteristic.LUCK, 1)));

    List<String> lines = formatter.lines(progress);

    assertEquals("--- PROCEDURA DI FINE SCONTRO ---", lines.getFirst());
    assertTrue(lines.get(1).contains("vita e stamina tornano piene"), lines.get(1));
    assertTrue(lines.contains("Arma: lascia SWORD (COMMON, atk 4) e impugna SWORD (RARE, atk 9)."), lines.toString());
    assertTrue(lines.contains("Armatura: raccoglie HELMET (COMMON, def 3), parte del corpo prima scoperta."),
        lines.toString());
    assertTrue(lines.contains("Armatura: sostituisce CHESTPLATE (COMMON, def 2) con CHESTPLATE (COMMON, def 6)."),
        lines.toString());
    assertEquals("Crescita: +2 STRENGTH, +1 LUCK.", lines.getLast());
  }

  @Test
  void diceEsplicitamenteQuandoDalTerrenoNonArrivaNiente() {
    HeroProgress progress = new HeroProgress(heroWith(sword(6, Rarity.COMMON), piece(Armour.CHESTPLATE, 4)),
        null, List.of(), List.of(), List.of(new CharacteristicGain(Characteristic.AGILITY, 3)));

    List<String> lines = formatter.lines(progress);

    assertTrue(lines.contains("Arma: tiene SWORD (COMMON, atk 6), niente di meglio sul terreno."), lines.toString());
    assertTrue(lines.contains("Armatura: nessun pezzo a terra migliora quella che indossa."), lines.toString());
    assertEquals("Crescita: +3 AGILITY.", lines.getLast());
  }

  private Hero heroWith(WeaponResult weapon, ArmourResult armour) {
    return new Hero(CombatFixtures.createWarrior("Protagonista", 10, 10, 10, 10, 10), weapon, List.of(armour));
  }

  private WeaponResult sword(int attack, Rarity rarity) {
    return new WeaponResult(Weapon.SWORD, rarity, List.of(), List.of(), attack);
  }

  private ArmourResult piece(Armour slot, int defense) {
    return new ArmourResult(slot, Rarity.COMMON, List.of(), List.of(), defense);
  }
}
