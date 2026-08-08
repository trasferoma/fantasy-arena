package it.fantasyarena.combat.io.trace;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifica {@link TraceFileWriter}: la scrittura in UTF-8, il troncamento del file esistente e la
 * politica di errore che non propaga il guasto al chiamante.
 */
class TraceFileWriterTest {

  private final PrintStream originalErr = System.err;
  private ByteArrayOutputStream capturedErr;

  @BeforeEach
  void captureSystemErr() {
    capturedErr = new ByteArrayOutputStream();
    System.setErr(new PrintStream(capturedErr, true, StandardCharsets.UTF_8));
  }

  @AfterEach
  void restoreSystemErr() {
    System.setErr(originalErr);
  }

  @Test
  void scriveLeRigheInUtf8(@TempDir Path directory) throws IOException {
    Path file = directory.resolve("fantasy-arena-01.log");

    TraceFileWriter.write(file, List.of("prima riga", "seconda riga con àccènti"));

    List<String> written = Files.readAllLines(file, StandardCharsets.UTF_8);
    assertEquals(List.of("prima riga", "seconda riga con àccènti"), written);
  }

  @Test
  void troncaIlFileEsistenteInvecediAppendere(@TempDir Path directory) throws IOException {
    Path file = directory.resolve("fantasy-arena-01.log");
    TraceFileWriter.write(file, List.of("riga della corsa precedente, molto più lunga della prossima"));

    TraceFileWriter.write(file, List.of("nuova riga"));

    List<String> written = Files.readAllLines(file, StandardCharsets.UTF_8);
    assertEquals(List.of("nuova riga"), written);
  }

  @Test
  void unGuastoDiScritturaNonPropagaLEccezioneEProduceLaDiagnosticaConLaCausa(@TempDir Path directory) {
    Path notWritable = directory.resolve("una-cartella-al-posto-del-file");
    assertDoesNotThrow(() -> Files.createDirectory(notWritable));

    assertDoesNotThrow(() -> TraceFileWriter.write(notWritable, List.of("riga qualunque")));

    String diagnostics = capturedErr.toString(StandardCharsets.UTF_8);
    assertTrue(diagnostics.contains(notWritable.toString()), "il percorso del file deve comparire nella diagnostica");
    assertTrue(diagnostics.contains("Exception") || diagnostics.contains("IOException"),
        "la causa dell'errore deve comparire nella diagnostica");
  }
}
