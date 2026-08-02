package it.fantasyarena.combat.io.render;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.factory.FighterAssembler;
import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.testsupport.CombatFixtures;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.buffdebuffgenerator.result.BuffElement;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.Characteristic;
import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.Weapon;

/**
 * La scheda del combattente, verificata sul testo prodotto: i bonus che arma e armatura portano
 * finché restano equipaggiate compaiono su una riga propria sotto l'oggetto solo nella scheda
 * piena, mai in quella compatta, e un oggetto senza buff non produce nessuna riga.
 */
class FighterCardFormatterTest {

  private final FighterCardFormatter formatter = new FighterCardFormatter();
  private final FighterAssembler assembler = FighterAssembler.withDefaultRatings(CombatSettings.defaults());

  @Test
  void laSchedaPienaMostraIBonusDiArmaEArmaturaSuUnaRigaPropria() {
    Fighter fighter = buffedFighter();

    List<String> lines = formatter.card(0, fighter);

    assertTrue(lines.contains("Bonus +3 STRENGTH"), lines.toString());
    assertTrue(lines.contains("Bonus +1 RESISTANCE"), lines.toString());
  }

  @Test
  void laSchedaCompattaOmetteIBonusComeGiaOmetteLeCaratteristiche() {
    Fighter fighter = buffedFighter();

    List<String> lines = formatter.compactCard(0, fighter);

    assertTrue(lines.stream().noneMatch(line -> line.startsWith("Bonus")),
        "la scheda compatta non deve mostrare i bonus: " + lines);
  }

  @Test
  void unEquipaggiamentoSenzaBuffNonProduceNessunaRigaDiBonusNellaSchedaPiena() {
    Fighter fighter = CombatFixtures.createFighter("Senza Buff", 10, 10, 10, 10, 10, 8, 4);

    List<String> lines = formatter.card(0, fighter);

    assertTrue(lines.stream().noneMatch(line -> line.startsWith("Bonus")),
        "un equipaggiamento senza buff non deve produrre nessuna riga di bonus: " + lines);
  }

  private Fighter buffedFighter() {
    CharacterResult character = CombatFixtures.createWarrior("Protagonista", 10, 11, 12, 13, 14);
    WeaponResult weapon = new WeaponResult(Weapon.SWORD, Rarity.RARE,
        List.of(new BuffElement(Characteristic.STRENGTH, 3)), List.of(), 9);
    ArmourResult armour = new ArmourResult(Armour.CHESTPLATE, Rarity.RARE,
        List.of(new BuffElement(Characteristic.RESISTANCE, 1)), List.of(), 6);
    return assembler.assemble(character, weapon, List.of(armour), null);
  }
}
