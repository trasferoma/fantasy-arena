package it.fantasyarena.combat.io.trace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Scrive le righe del log analitico sul file scelto dalla rotazione. Non compone niente e non
 * sceglie niente: riceve il file e le righe già pronte e si limita all'I/O.
 *
 * <p>Il file viene sempre <strong>troncato</strong> e non appeso, perché una corsa sta per intero
 * in un file solo: se il file esisteva già da una rotazione precedente, il suo contenuto vecchio non
 * deve mescolarsi con quello nuovo.
 *
 * <p><strong>Politica di errore, dichiarata in questo unico punto</strong>: un guasto di scrittura
 * (cartella non scrivibile, file non creabile) non deve far fallire la corsa né la richiesta HTTP
 * che l'ha generata. L'eccezione non si propaga al chiamante, ma non si silenzia nemmeno: viene
 * riportata su {@code System.err} con la sua causa e il percorso del file su cui si è tentata la
 * scrittura, così il guasto resta diagnosticabile senza interrompere la partita.
 */
public final class TraceFileWriter {

  private TraceFileWriter() {
  }

  public static void write(Path file, List<String> lines) {
    try {
      Files.write(file, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
      reportFailure(file, e);
    }
  }

  private static void reportFailure(Path file, IOException cause) {
    System.err.println("Scrittura del log analitico fallita su " + file + ": " + cause.getMessage());
    cause.printStackTrace();
  }
}
