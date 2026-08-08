package it.fantasyarena.combat.io.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifica {@link TraceFileSelector} su una cartella temporanea: la scelta si deriva sempre dallo
 * stato reale del filesystem, mai da uno stato in memoria.
 */
class TraceFileSelectorTest {

  @Test
  void suCartellaVuotaSceglieIlPrimoFile(@TempDir Path directory) {
    Path selected = TraceFileSelector.selectFile(directory);

    assertEquals(directory.resolve("fantasy-arena-01.log"), selected);
  }

  @Test
  void conUnFilePresenteSceglieIlSecondo(@TempDir Path directory) throws IOException {
    Files.createFile(directory.resolve("fantasy-arena-01.log"));

    Path selected = TraceFileSelector.selectFile(directory);

    assertEquals(directory.resolve("fantasy-arena-02.log"), selected);
  }

  @Test
  void conDueFilePresentiSceglieIlTerzo(@TempDir Path directory) throws IOException {
    Files.createFile(directory.resolve("fantasy-arena-01.log"));
    Files.createFile(directory.resolve("fantasy-arena-02.log"));

    Path selected = TraceFileSelector.selectFile(directory);

    assertEquals(directory.resolve("fantasy-arena-03.log"), selected);
  }

  @Test
  void conTreFilePresentiSceglieQuelloConLaDataDiModificaPiuVecchia(@TempDir Path directory) throws IOException {
    Path first = directory.resolve("fantasy-arena-01.log");
    Path second = directory.resolve("fantasy-arena-02.log");
    Path third = directory.resolve("fantasy-arena-03.log");
    Files.createFile(first);
    Files.createFile(second);
    Files.createFile(third);
    Files.setLastModifiedTime(first, FileTime.fromMillis(3_000));
    Files.setLastModifiedTime(second, FileTime.fromMillis(1_000));
    Files.setLastModifiedTime(third, FileTime.fromMillis(2_000));

    Path selected = TraceFileSelector.selectFile(directory);

    assertEquals(second, selected);
  }

  @Test
  void dopoTreCorseNellaCartellaCiSonoEsattamenteTreFile(@TempDir Path directory) throws IOException {
    for (int run = 0; run < 3; run++) {
      Path selected = TraceFileSelector.selectFile(directory);
      TraceFileWriter.write(selected, List.of("riga della corsa " + run));
    }

    try (Stream<Path> files = Files.list(directory)) {
      assertEquals(3, files.count());
    }
  }

  @Test
  void dopoUnaQuartaCorsaLaRotazioneRipartendalPrimoESonoAncoraTreFile(@TempDir Path directory) throws IOException {
    for (int run = 0; run < 3; run++) {
      Path selected = TraceFileSelector.selectFile(directory);
      TraceFileWriter.write(selected, List.of("riga della corsa " + run));
      Files.setLastModifiedTime(selected, FileTime.fromMillis(1_000L * run));
    }

    Path fourthSelected = TraceFileSelector.selectFile(directory);
    TraceFileWriter.write(fourthSelected, List.of("riga della quarta corsa"));

    assertEquals(directory.resolve("fantasy-arena-01.log"), fourthSelected);
    try (Stream<Path> files = Files.list(directory)) {
      assertEquals(3, files.count());
    }
    assertTrue(Files.readString(fourthSelected).contains("quarta corsa"));
  }
}
