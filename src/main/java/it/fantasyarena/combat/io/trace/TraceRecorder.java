package it.fantasyarena.combat.io.trace;

import java.nio.file.Path;
import java.nio.file.Paths;

import it.fantasyarena.combat.chronicle.ArenaChronicle;

/**
 * Mette insieme le tre responsabilità del log analitico — comporre le righe
 * ({@link ChronicleTraceComposer}), scegliere il file della rotazione ({@link TraceFileSelector}),
 * scriverlo ({@link TraceFileWriter}) — nell'unico punto che le due modalità di gioco dovranno
 * agganciare. Orchestra soltanto: non compone righe, non decide il file e non scrive di suo.
 *
 * <p>La cartella è sempre {@code java.io.tmpdir}: è questa la sola classe che legge quella
 * proprietà di sistema, in modo che {@link TraceFileSelector} resti verificabile su una cartella
 * qualsiasi.
 */
public class TraceRecorder {

  private final ChronicleTraceComposer composer;
  private final Path directory;

  public TraceRecorder() {
    this(new ChronicleTraceComposer(), Paths.get(System.getProperty("java.io.tmpdir")));
  }

  TraceRecorder(ChronicleTraceComposer composer, Path directory) {
    this.composer = composer;
    this.directory = directory;
  }

  public void record(ArenaChronicle chronicle) {
    Path file = TraceFileSelector.selectFile(directory);
    TraceFileWriter.write(file, composer.compose(chronicle));
  }
}
