package it.fantasyarena.combat.io.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import it.fantasyarena.combat.RoundOutcome;
import it.fantasyarena.combat.chronicle.ArenaChronicle;
import it.fantasyarena.combat.chronicle.HeroSnapshot;
import it.fantasyarena.combat.chronicle.RunConclusion;
import it.fantasycombatsystem.config.CombatSettings;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkitcore.core.model.CharacterClass;
import it.fantasytoolkitcore.core.model.Characteristic;
import it.fantasytoolkitcore.core.model.Race;

/**
 * Verifica che {@link TraceRecorder} colleghi composizione, scelta del file e scrittura senza
 * aggiungere logica propria.
 */
class TraceRecorderTest {

  @Test
  void componeSceglieEScriveIlLogSuUnaCartellaData(@TempDir Path directory) throws IOException {
    Clock fixedClock = Clock.fixed(Instant.parse("2026-08-08T10:12:03Z"), ZoneOffset.UTC);
    TraceRecorder recorder = new TraceRecorder(new ChronicleTraceComposer(fixedClock), directory);

    recorder.record(minimalChronicle());

    Path expectedFile = directory.resolve("fantasy-arena-01.log");
    try (Stream<Path> files = Files.list(directory)) {
      assertEquals(1, files.count());
    }
    List<String> lines = Files.readAllLines(expectedFile, StandardCharsets.UTF_8);
    assertTrue(lines.get(0).contains("RUN_OPENED"));
  }

  private ArenaChronicle minimalChronicle() {
    HeroSnapshot protagonist = new HeroSnapshot("Protagonista", Race.HUMAN, CharacterClass.WARRIOR,
        List.of(new CharacterCharacteristic(Characteristic.STRENGTH, 12)),
        List.of(new CharacterCharacteristic(Characteristic.STRENGTH, 12)), null, List.of(), List.of());
    return new ArenaChronicle(CombatSettings.defaults(), protagonist, 10, List.of(),
        new RunConclusion(RoundOutcome.FELL, 0));
  }
}
