package it.fantasyarena.combat.io.log;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.ChallengerBudget;
import it.fantasyarena.combat.hero.Hero;
import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.testsupport.CombatFixtures;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.Weapon;

/**
 * Verifica {@link ConsoleArenaLogger} sull'annuncio del round: numero, descrizione e schieramento
 * si stampano sempre, mentre lo sconto della fortuna è raccontato solo quando c'è davvero qualcosa
 * da dire — non per la stazione dello specchio ({@link ChallengerBudget} nullo) e non quando lo
 * sconto applicato è zero. Cattura di {@code System.out} sullo stampo di
 * {@link ConsoleBattleLoggerTest}.
 */
class ConsoleArenaLoggerTest {

  private final ConsoleArenaLogger logger = new ConsoleArenaLogger();
  private final PrintStream originalOut = System.out;
  private ByteArrayOutputStream capturedOut;

  @BeforeEach
  void redirectConsole() {
    capturedOut = new ByteArrayOutputStream();
    System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
  }

  @AfterEach
  void restoreConsole() {
    System.setOut(originalOut);
  }

  @Test
  void annunciaIlMonteELoScontoQuandoLaFortunaScontaDavvero() {
    Hero hero = heroNamed("Protagonista");
    ChallengerBudget budget = new ChallengerBudget(31, 4, 27);

    logger.announceRound(4, "due contro uno", hero, challengers("Sfidante1", "Sfidante2"), budget);

    String output = capturedOutput();
    assertTrue(output.contains("ROUND 4 — due contro uno"));
    assertTrue(output.contains("monte di squadra dichiarato per questa prova è 31 punti"), output);
    assertTrue(output.contains("La fortuna di Protagonista ne sconta 4"), output);
    assertTrue(output.contains("scendono in campo con 27 punti"), output);
  }

  @Test
  void taceLoScontoPerLaStazioneDelloSpecchioSenzaBudget() {
    Hero hero = heroNamed("Protagonista");

    logger.announceRound(10, "lo sfidante speculare", hero, challengers("Specchio"), null);

    String output = capturedOutput();
    assertTrue(output.contains("ROUND 10 — lo sfidante speculare"));
    assertFalse(output.contains("sconta"), "senza budget non c'è nessuno sconto da raccontare");
  }

  @Test
  void taceLoScontoQuandoLoScontoApplicatoEZero() {
    Hero hero = heroNamed("Protagonista");
    ChallengerBudget budget = new ChallengerBudget(7, 0, 7);

    logger.announceRound(1, "un solo sfidante", hero, challengers("Sfidante"), budget);

    String output = capturedOutput();
    assertTrue(output.contains("ROUND 1 — un solo sfidante"));
    assertFalse(output.contains("sconta"), "uno sconto applicato pari a zero non va raccontato");
  }

  private List<Fighter> challengers(String... names) {
    return List.of(names).stream()
        .map(this::createChallenger)
        .toList();
  }

  private Fighter createChallenger(String name) {
    return CombatFixtures.createFighter(name, 10, 10, 10, 35, 8, 4, 2);
  }

  private Hero heroNamed(String name) {
    CharacterResult character = CombatFixtures.createWarrior(name, 10, 10, 10, 10, 10);
    WeaponResult weapon = new WeaponResult(Weapon.SWORD, Rarity.COMMON, List.of(), List.of(), 5);
    ArmourResult armour = new ArmourResult(Armour.CHESTPLATE, Rarity.COMMON, List.of(), List.of(), 4);
    return new Hero(character, weapon, List.of(armour));
  }

  private String capturedOutput() {
    return capturedOut.toString(StandardCharsets.UTF_8);
  }
}
