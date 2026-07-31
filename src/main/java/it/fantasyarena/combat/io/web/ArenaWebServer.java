package it.fantasyarena.combat.io.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.function.Supplier;

import com.sun.net.httpserver.HttpServer;

import it.fantasyarena.combat.chronicle.ArenaChronicle;

/**
 * Il server HTTP della modalità web: espone la cronaca su {@code /api/chronicle} e le risorse
 * statiche della pagina, in ascolto solo sull'interfaccia di loopback ({@link
 * InetAddress#getLoopbackAddress()}, mai {@code 0.0.0.0}) e su una porta parametrica.
 *
 * <p>Le richieste sono gestite con l'esecutore predefinito di {@link HttpServer} (nessun {@code
 * setExecutor} esplicito, quindi gestione seriale, una richiesta alla volta sul thread di
 * dispatch): una partita dura millisecondi e questo è un gioco a uso locale, per cui un pool di
 * thread aggiungerebbe complessità senza un beneficio misurabile. La serialità ha anche un
 * vantaggio di correttezza: rende banalmente vera la garanzia di {@link ChronicleHandler} — «una
 * partita per richiesta, nessuno stato condiviso» — perché due richieste non possono mai essere in
 * corso nello stesso istante.
 *
 * <p>Se la porta richiesta è già occupata, la costruzione fallisce con un'eccezione che nomina la
 * porta: niente ricerca silenziosa di una porta libera, perché l'indirizzo stampato da chi userà
 * questa classe deve restare prevedibile.
 */
public class ArenaWebServer {

  private final HttpServer server;

  /**
   * Crea il server e vi registra i due gestori, ma non lo avvia: la partenza effettiva è {@link
   * #start()}.
   *
   * @param port porta su cui legarsi, {@code 0} per farsene assegnare una libera dal sistema
   *     operativo
   * @param chronicles fornitore della cronaca, invocato una volta per ogni richiesta a {@code
   *     /api/chronicle}
   * @throws IllegalStateException se la porta richiesta è già occupata o comunque non disponibile
   */
  public ArenaWebServer(int port, Supplier<ArenaChronicle> chronicles) {
    this.server = createServer(port);
    server.createContext("/api/chronicle", new ChronicleHandler(chronicles, new ChronicleJson()));
    server.createContext("/", new StaticResourceHandler());
  }

  /**
   * Avvia il server: da questo momento accetta connessioni sulla porta legata alla costruzione.
   */
  public void start() {
    server.start();
  }

  /**
   * Ferma il server senza attendere il completamento delle richieste in corso.
   */
  public void stop() {
    server.stop(0);
  }

  /**
   * Restituisce l'indirizzo effettivamente legato: utile quando la costruzione ha chiesto una
   * porta effimera ({@code 0}) e occorre scoprire quella assegnata dal sistema operativo.
   *
   * @return l'indirizzo di loopback e la porta su cui il server è in ascolto
   */
  public InetSocketAddress address() {
    return server.getAddress();
  }

  private static HttpServer createServer(int port) {
    try {
      return HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
    } catch (IOException e) {
      throw new IllegalStateException(
          "Impossibile avviare il server sulla porta " + port + ": porta già in uso o non disponibile", e);
    }
  }
}
