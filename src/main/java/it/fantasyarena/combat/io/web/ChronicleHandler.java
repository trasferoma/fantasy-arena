package it.fantasyarena.combat.io.web;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import it.fantasyarena.combat.chronicle.ArenaChronicle;

/**
 * Risponde a {@code GET /api/chronicle} con la cronaca di una partita nuova, giocata al momento
 * della richiesta.
 *
 * <p>La cronaca arriva da un {@link Supplier} iniettato e invocato <strong>una volta per
 * richiesta</strong>: ogni apertura della pagina rigioca la partita da zero, con collaboratori
 * propri (in particolare una nuova {@code FighterFactory}, che altrimenti condividerebbe fra
 * richieste indipendenti il proprio stato dei nomi già assegnati). Questo gestore non custodisce
 * mai la cronaca fra una richiesta e l'altra.
 *
 * <p>Il server non serve nulla di scrivibile: l'unica richiesta onesta è {@code GET}, e qualunque
 * altro metodo risponde {@code 405}.
 *
 * <p>{@code HttpServer.createContext} registra questo gestore su un <strong>prefisso</strong>, non
 * su un percorso esatto: senza un controllo esplicito, anche {@code GET /api/chronicle/xyz} o
 * {@code GET /api/chronicleXYZ} arriverebbero fin qui e farebbero giocare una partita intera su un
 * percorso che non deve esistere. Per questo {@link #handle} confronta il percorso della richiesta
 * con {@link #CHRONICLE_PATH} per uguaglianza, prima di invocare il fornitore: una partita non deve
 * nemmeno cominciare per un percorso che risponde {@code 404}.
 *
 * <p>La lunghezza dichiarata a {@link HttpExchange#sendResponseHeaders} è quella dei
 * <strong>byte</strong> UTF-8 del corpo, non dei caratteri della stringa: la narrazione del motore
 * è in italiano e accentata, e passare la lunghezza della stringa produrrebbe una risposta
 * troncata sui caratteri multi-byte.
 */
class ChronicleHandler implements HttpHandler {

  private static final String CHRONICLE_PATH = "/api/chronicle";
  private static final String CONTENT_TYPE = "application/json; charset=utf-8";

  private final Supplier<ArenaChronicle> chronicles;
  private final ChronicleJson chronicleJson;

  ChronicleHandler(Supplier<ArenaChronicle> chronicles, ChronicleJson chronicleJson) {
    this.chronicles = chronicles;
    this.chronicleJson = chronicleJson;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    if (!CHRONICLE_PATH.equals(exchange.getRequestURI().getPath())) {
      respondWithoutBody(exchange, 404);
      return;
    }

    if (!"GET".equals(exchange.getRequestMethod())) {
      respondWithoutBody(exchange, 405);
      return;
    }

    byte[] body = chronicleJson.toJson(chronicles.get()).getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
    exchange.getResponseHeaders().set("Cache-Control", "no-store");
    exchange.sendResponseHeaders(200, body.length);
    try (OutputStream responseBody = exchange.getResponseBody()) {
      responseBody.write(body);
    }
  }

  private void respondWithoutBody(HttpExchange exchange, int statusCode) throws IOException {
    exchange.sendResponseHeaders(statusCode, -1);
    exchange.close();
  }
}
