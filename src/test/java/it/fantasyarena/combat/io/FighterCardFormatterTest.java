package it.fantasyarena.combat.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.testsupport.CombatFixtures;

/**
 * Verifica il contenuto della scheda compatta multi-riga prodotta da
 * {@link FighterCardFormatter}: intestazione, razza/classe, arma con rarità e attacco,
 * armatura con rarità e difesa, vita, stamina, ATK e DEF.
 */
class FighterCardFormatterTest {

  private final FighterCardFormatter formatter = new FighterCardFormatter();

  @Test
  void cardProduceSeiRigheConIntestazioneRazzaClasseEquipaggiamentoEVitaliStamina() {
    Fighter fighter = CombatFixtures.createFighter("Pertinax", 14, 12, 12, 40, 10, 6, 3);

    List<String> lines = formatter.card(1, fighter);

    assertEquals(6, lines.size());
    assertEquals("[1] Pertinax", lines.get(0));
    assertEquals("HUMAN WARRIOR", lines.get(1));
  }

  @Test
  void cardMostraArmaConRaritaEAttacco() {
    Fighter fighter = CombatFixtures.createFighter("Pertinax", 14, 12, 12, 40, 10, 6, 3);

    List<String> lines = formatter.card(1, fighter);

    assertTrue(lines.get(2).contains("SWORD"));
    assertTrue(lines.get(2).contains("COMMON"));
    assertTrue(lines.get(2).contains("atk 6"));
  }

  @Test
  void cardMostraArmaturaConRaritaEDifesa() {
    Fighter fighter = CombatFixtures.createFighter("Pertinax", 14, 12, 12, 40, 10, 6, 3);

    List<String> lines = formatter.card(1, fighter);

    assertTrue(lines.get(3).contains("CHESTPLATE"));
    assertTrue(lines.get(3).contains("COMMON"));
    assertTrue(lines.get(3).contains("def 3"));
  }

  @Test
  void cardMostraVitaEStamina() {
    Fighter fighter = CombatFixtures.createFighter("Pertinax", 14, 12, 12, 40, 10, 6, 3);

    List<String> lines = formatter.card(1, fighter);

    assertTrue(lines.get(4).contains("VIT " + fighter.ratings().maxHealth()));
    assertTrue(lines.get(4).contains("STA " + fighter.ratings().maxStamina()));
  }

  @Test
  void cardMostraAtkEDefFormattatiConUnDecimale() {
    Fighter fighter = CombatFixtures.createFighter("Pertinax", 14, 12, 12, 40, 10, 6, 3);
    TurnLogFormatter turnLogFormatter = new TurnLogFormatter();

    List<String> lines = formatter.card(1, fighter);

    assertTrue(lines.get(5).contains("ATK " + turnLogFormatter.formatRating(fighter.ratings().offensiveRating())));
    assertTrue(lines.get(5).contains("DEF " + turnLogFormatter.formatRating(fighter.ratings().defensiveRating())));
  }

  @Test
  void cardUsaLIndiceRicevutoNellIntestazione() {
    Fighter fighter = CombatFixtures.createFighter("Livia", 10, 14, 10, 35, 8, 5, 2);

    List<String> lines = formatter.card(2, fighter);

    assertEquals("[2] Livia", lines.get(0));
  }
}
