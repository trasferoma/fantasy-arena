package it.fantasyarena;

import java.net.InetSocketAddress;
import java.util.function.Supplier;

import it.fantasyarena.combat.Arena;
import it.fantasyarena.combat.SilentArenaRun;
import it.fantasyarena.combat.chronicle.ArenaChronicle;
import it.fantasyarena.combat.io.terminal.ScreenRefresh;
import it.fantasyarena.combat.io.trace.TraceRecorder;
import it.fantasyarena.combat.io.trace.TracedChronicleSupplier;
import it.fantasyarena.combat.io.web.ArenaWebServer;
import it.fantasycombatsystem.config.CombatSettings;

/**
 * La modalità con cui l'applicazione presenta la partita, scelta dagli argomenti della riga di
 * comando: {@link #fromArgs(String[])} è l'unico punto in cui quegli argomenti vengono letti.
 *
 * <p>È modellata come due tipi distinti, {@link ConsoleMode} e {@link WebMode}, invece che come un
 * unico dato con un campo porta facoltativo. Un campo porta valorizzato a {@code 8080} anche quando
 * si gioca in console sarebbe un dato che mente sul proprio significato, e i tipi sealed non sono
 * disponibili in questo progetto (Java 21, ma la convenzione di repository li esclude): la porta ha
 * senso solo per la modalità web, quindi solo {@link WebMode} la porta con sé. Un enum non potrebbe
 * sostituire questi due tipi, perché le sue costanti sono singleton fissi e non possono portare un
 * valore che cambia a ogni esecuzione come la porta scelta dall'utente.
 *
 * <p>Ogni modalità sa avviarsi da sé con {@link #launch(CombatSettings)}: {@code ConsoleMode} apre
 * l'arena in console, {@code WebMode} avvia il server e stampa l'indirizzo. È polimorfismo, non
 * discriminazione a runtime del tipo concreto: questo repository non usa {@code instanceof} né uno
 * {@code switch} a pattern matching (vedi {@code CLAUDE.md}), e {@link Main} non deve conoscere
 * {@code ConsoleMode} e {@code WebMode} per scegliere la strada giusta: gli basta chiedere la
 * modalità e lasciarla avviarsi.
 */
public interface UiMode {

  /** Argomento che seleziona la modalità web. */
  String WEB_ARGUMENT = "web";

  /** Porta usata dalla modalità web quando l'argomento non la specifica. */
  int DEFAULT_WEB_PORT = 8080;

  /** Porta più bassa accettata per la modalità web. */
  int MIN_PORT = 1;

  /** Porta più alta accettata per la modalità web. */
  int MAX_PORT = 65535;

  /**
   * Analizza gli argomenti della riga di comando e restituisce la modalità scelta.
   *
   * <p>Nessun argomento seleziona la console. Il primo argomento uguale a {@code "web"} seleziona
   * la modalità web, con un secondo argomento opzionale per la porta ({@link #DEFAULT_WEB_PORT} se
   * assente). Qualunque altro primo argomento è un errore.
   *
   * @param args argomenti della riga di comando, non nullo
   * @return la modalità console oppure la modalità web con la sua porta
   * @throws IllegalArgumentException se il primo argomento non è una modalità ammessa, o se la
   *     porta indicata non è un numero intero fra {@value #MIN_PORT} e {@value #MAX_PORT}
   */
  static UiMode fromArgs(String[] args) {
    if (args.length == 0) {
      return new ConsoleMode();
    }

    String requestedMode = args[0];
    if (!WEB_ARGUMENT.equals(requestedMode)) {
      throw new IllegalArgumentException("Modalità \"" + requestedMode + "\" non riconosciuta. Modalità ammesse: "
          + "nessun argomento avvia la console, l'argomento \"" + WEB_ARGUMENT + "\" avvia il server.");
    }

    int port = args.length > 1 ? parsePort(args[1]) : DEFAULT_WEB_PORT;
    return new WebMode(port);
  }

  private static int parsePort(String rawPort) {
    int port;
    try {
      port = Integer.parseInt(rawPort);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "La porta \"" + rawPort + "\" non è un numero. Indicare un valore intero fra " + MIN_PORT + " e " + MAX_PORT
              + ".", e);
    }

    if (port < MIN_PORT || port > MAX_PORT) {
      throw new IllegalArgumentException(
          "La porta " + port + " non è valida. Indicare un valore fra " + MIN_PORT + " e " + MAX_PORT + ".");
    }
    return port;
  }

  /**
   * Avvia questa modalità di presentazione della partita.
   *
   * @param settings le regole di combattimento con cui generare e giocare la partita
   */
  void launch(CombatSettings settings);

  /**
   * La modalità console: nessun dato oltre alla propria esistenza. {@link #launch} apre l'arena a
   * schermo, esattamente come faceva {@code Main} prima che la modalità web esistesse, e consegna
   * la cronaca restituita da {@link Arena#run()} al tracciatore del log analitico.
   */
  record ConsoleMode() implements UiMode {

    @Override
    public void launch(CombatSettings settings) {
      // ScreenRefresh screenRefresh = new CombatSetupPrompt().askScreenRefresh();
      ScreenRefresh screenRefresh = ScreenRefresh.CLEAR;

      ArenaChronicle chronicle = new Arena(settings, screenRefresh).run();
      new TraceRecorder().record(chronicle);
    }
  }

  /**
   * La modalità web, con la porta su cui il server deve mettersi in ascolto. {@link #launch} avvia
   * {@link ArenaWebServer} con una partita muta e stampa soltanto l'indirizzo da aprire.
   *
   * <p>Il fornitore passato al server è la partita muta decorata da {@link
   * TracedChronicleSupplier}, non {@link SilentArenaRun} da sola: è così che ogni corsa giocata per
   * una richiesta finisce anche nel log analitico, senza che {@link ArenaWebServer} riceva niente di
   * più di un {@code Supplier<ArenaChronicle>}. Questa classe è l'unico punto che conosce sia il
   * server web sia il tracciatore, quindi è l'unico punto che può comporli.
   *
   * @param port porta richiesta dall'utente o {@link #DEFAULT_WEB_PORT}, già validata da {@link
   *     #fromArgs(String[])}
   */
  record WebMode(int port) implements UiMode {

    @Override
    public void launch(CombatSettings settings) {
      Supplier<ArenaChronicle> chronicles = new TracedChronicleSupplier(new SilentArenaRun(settings),
          new TraceRecorder());
      ArenaWebServer server = new ArenaWebServer(port, chronicles);
      server.start();

      InetSocketAddress boundAddress = server.address();
      String baseUrl = "http://" + boundAddress.getAddress().getHostAddress() + ":" + boundAddress.getPort() + "/";
      System.out.println(baseUrl);
    }
  }
}
