package it.fantasyarena.combat.io.trace;

import java.io.File;
import java.nio.file.Path;

/**
 * Sceglie, fra i tre nomi della rotazione, quale file ospiterà il log analitico della prossima
 * corsa. Interroga soltanto il filesystem — non scrive niente — e non conserva alcuno stato fra una
 * chiamata e l'altra: ogni scelta si ricava da zero da cosa trova nella cartella, perché ogni corsa
 * può essere una JVM diversa e uno stato in memoria si perderebbe o mentirebbe.
 *
 * <p>La scelta è il primo nome mancante fra {@code fantasy-arena-01.log}, {@code -02} e {@code -03};
 * quando tutti e tre esistono già, è quello con la data di ultima modifica più vecchia. Dopo il
 * terzo file la rotazione riparte dal primo — che chi scrive tronca invece di appendere — così nella
 * cartella non ce ne sono mai più di tre.
 *
 * <p>La cartella è un parametro e non un valore cablato qui dentro: è ciò che rende questa unità
 * verificabile su una cartella temporanea. In produzione è chi la usa a passare
 * {@code java.io.tmpdir}.
 *
 * <p><strong>Le corse concorrenti non si proteggono</strong>, per decisione esplicita dell'utente:
 * in modalità web due richieste simultanee possono leggere lo stesso stato del filesystem, scegliere
 * lo stesso file «più vecchio» e sovrascriversi a vicenda. Non c'è né un lock né una scrittura su
 * file temporaneo con rinomina, perché nessuno dei due chiuderebbe comunque la finestra fra due JVM
 * distinte — un lock proteggerebbe solo i thread della stessa JVM — e il caso d'uso reale di questo
 * progetto è guardare una partita alla volta. La perdita occasionale di una corsa concorrente è un
 * rischio accettato, non un difetto da correggere qui.
 */
public final class TraceFileSelector {

  private static final int ROTATION_SIZE = 3;
  private static final String FILE_NAME_FORMAT = "fantasy-arena-%02d.log";

  private TraceFileSelector() {
  }

  /**
   * @param directory cartella in cui cercare i file della rotazione
   * @return il primo nome mancante, oppure il più vecchio se i tre esistono già
   */
  public static Path selectFile(Path directory) {
    Path oldestFile = null;
    long oldestModifiedTime = Long.MAX_VALUE;

    for (int index = 1; index <= ROTATION_SIZE; index++) {
      Path candidate = directory.resolve(fileName(index));
      File candidateFile = candidate.toFile();
      if (!candidateFile.exists()) {
        return candidate;
      }
      if (candidateFile.lastModified() < oldestModifiedTime) {
        oldestModifiedTime = candidateFile.lastModified();
        oldestFile = candidate;
      }
    }
    return oldestFile;
  }

  private static String fileName(int index) {
    return String.format(FILE_NAME_FORMAT, index);
  }
}
