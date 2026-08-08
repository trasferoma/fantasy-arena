package it.fantasyarena.combat.io.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
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
 * Verifica che {@link TracedChronicleSupplier} restituisca la cronaca del fornitore delegato e ne
 * registri il log, senza che il delegato sappia che il tracciamento esiste. È il punto in cui si
 * verifica l'aggancio della modalità web senza avviare {@code Main} né un server vero: il delegato
 * è un fornitore finto, e la registrazione si osserva sul filesystem temporaneo.
 */
class TracedChronicleSupplierTest {

  @Test
  void get_restituisceLaCronacaDelDelegatoENeRegistraIlLog(@TempDir Path directory) throws IOException {
    ArenaChronicle chronicle = minimalChronicle();
    Clock fixedClock = Clock.fixed(Instant.parse("2026-08-08T10:12:03Z"), ZoneOffset.UTC);
    TraceRecorder recorder = new TraceRecorder(new ChronicleTraceComposer(fixedClock), directory);
    TracedChronicleSupplier supplier = new TracedChronicleSupplier(() -> chronicle, recorder);

    ArenaChronicle result = supplier.get();

    assertSame(chronicle, result);
    try (Stream<Path> files = Files.list(directory)) {
      assertEquals(1, files.count());
    }
  }

  private ArenaChronicle minimalChronicle() {
    HeroSnapshot protagonist = new HeroSnapshot("Protagonista", Race.HUMAN, CharacterClass.WARRIOR,
        List.of(new CharacterCharacteristic(Characteristic.STRENGTH, 12)),
        List.of(new CharacterCharacteristic(Characteristic.STRENGTH, 12)), null, List.of(), List.of());
    return new ArenaChronicle(CombatSettings.defaults(), protagonist, 10, List.of(),
        new RunConclusion(RoundOutcome.FELL, 0));
  }
}
