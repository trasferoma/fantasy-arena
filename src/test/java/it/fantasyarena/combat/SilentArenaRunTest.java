package it.fantasyarena.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.chronicle.ArenaChronicle;
import it.fantasyarena.combat.chronicle.TrialChronicle;
import it.fantasyarena.combat.factory.FighterFactory;
import it.fantasyarena.combat.hero.HeroBrain;
import it.fantasyarena.combat.io.log.SilentArenaLogger;
import it.fantasyarena.combat.io.terminal.TurnPacer;
import it.fantasycombatsystem.battle.BattleResult;
import it.fantasycombatsystem.battle.BattleSetup;
import it.fantasycombatsystem.battle.RoundLogEntry;
import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.result.CombatOutcome;
import it.fantasycombatsystem.result.CombatResult;
import it.fantasycombatsystem.result.TurnLogEntry;

/**
 * La passata muta verificata su due fronti distinti. I primi due test assemblano un'{@link Arena}
 * a mano con collaboratori muti e pilotati — come {@code ArenaTest} — per un controllo
 * deterministico di «niente su {@code System.out}» e «niente da {@code System.in}». L'ultimo passa
 * invece da {@link SilentArenaRun}, il percorso di produzione vero (dadi reali,
 * {@link it.fantasyarena.combat.io.replay.SilentMatchPresentation} vera, non un doppio): è il solo
 * che esercita quella classe, quindi verifica anche lì l'assenza di stampe, oltre alla completezza
 * della cronaca. L'esito dei dadi resta casuale, e si controlla solo quello che vale in ogni caso.
 */
class SilentArenaRunTest {

  private final CombatSettings settings = CombatSettings.defaults();
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
  void laPassataMutaNonScriveNienteSuSystemOut() {
    ScriptedVictories fights = new ScriptedVictories(settings);
    Arena silentArena = new Arena(FighterFactory.withDefaultRatings(settings), new HeroBrain(new Random(7)), fights,
        fights, new SilentArenaLogger(), TurnPacer.none());

    ArenaChronicle chronicle = silentArena.run();

    assertEquals(10, chronicle.trials().size(), "il copione deve far vincere tutte e dieci le prove");
    assertEquals("", output(), "la passata muta non stampa niente");
  }

  @Test
  void laPassataMutaNonLeggeDaSystemIn() {
    InputStream originalIn = System.in;
    System.setIn(new FailingInputStream());

    try {
      ScriptedVictories fights = new ScriptedVictories(settings);
      Arena silentArena = new Arena(FighterFactory.withDefaultRatings(settings), new HeroBrain(new Random(7)), fights,
          fights, new SilentArenaLogger(), TurnPacer.none());

      ArenaChronicle chronicle = silentArena.run();

      assertNotNull(chronicle.protagonist(), "la partita deve comunque essere stata giocata per intero");
    } finally {
      System.setIn(originalIn);
    }
  }

  @Test
  void laPassataMutaProduceUnaCronacaCompletaESenzaStampe() {
    ArenaChronicle chronicle = new SilentArenaRun(settings).get();

    assertEquals("", output(), "il percorso di produzione della passata muta non stampa niente");
    assertNotNull(chronicle.protagonist().name(), "la cronaca porta l'ingresso del protagonista");
    assertFalse(chronicle.trials().isEmpty(), "almeno una prova deve essere stata giocata");

    TrialChronicle lastTrial = chronicle.trials().getLast();
    assertEquals(lastTrial.outcome(), chronicle.conclusion().outcome(),
        "la conclusione racconta l'esito dell'ultima prova registrata");
    assertEquals(lastTrial.number(), chronicle.conclusion().lastTrial());
    assertFalse(lastTrial.finalVitals().isEmpty(), "il motore vero calcola sempre lo stato finale dei combattenti");
  }

  private String output() {
    return capturedOut.toString(StandardCharsets.UTF_8);
  }

  /**
   * Stream che fallisce se qualcuno prova a leggerlo: la passata muta non deve mai arrivare a
   * chiamare {@link #read()}.
   */
  private static final class FailingInputStream extends InputStream {

    @Override
    public int read() throws IOException {
      throw new AssertionError("la passata muta non deve leggere da System.in");
    }
  }

  /**
   * Doppio degli scontri che fa vincere sempre il protagonista, abbattendo gli sfidanti e lasciando
   * intatto il campione: basta a far scorrere tutte e tre le prove senza affidarsi ai dadi veri, e
   * senza che questa classe debba stampare o attendere niente.
   */
  private static final class ScriptedVictories extends MatchRunner {

    private static final int LETHAL_DAMAGE = 10_000;
    private static final int CHALLENGERS_TEAM = 1;

    private ScriptedVictories(CombatSettings settings) {
      super(settings);
    }

    @Override
    public BattleResult playBattle(BattleSetup setup) {
      strikeDown(setup.teams().get(CHALLENGERS_TEAM).members());
      return new BattleResult(CombatOutcome.DRAW, null, 1,
          List.of(new RoundLogEntry(1, List.of(), List.of(), List.of())), List.of(), List.of(), List.of());
    }

    @Override
    public CombatResult playDuel(Fighter champion, Fighter rival) {
      strikeDown(List.of(rival));
      return new CombatResult(CombatOutcome.DRAW, null, 1, List.of(new TurnLogEntry(1, "Scambio di prova")),
          List.of(), List.of());
    }

    private void strikeDown(List<Fighter> fighters) {
      fighters.forEach(fighter -> fighter.state().applyDamage(LETHAL_DAMAGE));
    }
  }
}
