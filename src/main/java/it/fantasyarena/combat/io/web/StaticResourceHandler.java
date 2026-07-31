package it.fantasyarena.combat.io.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Serve le risorse statiche della pagina da una lista chiusa di percorsi noti, ciascuno legato a
 * un nome di risorsa fisso sotto {@code web/} nel classpath: il percorso della richiesta non entra
 * mai nella costruzione del nome della risorsa da leggere. Non è quindi un controllo che impedisce
 * il traversal della cartella, è un percorso che non esiste — non c'è concatenazione di stringhe
 * che possa produrre un nome di risorsa diverso da quelli elencati in {@link
 * #RESOURCES_BY_PATH}.
 *
 * <p>Qualunque percorso non presente nella lista risponde {@code 404}, compresi i tentativi di
 * uscire dalla cartella delle risorse, in qualunque forma siano scritti (letterale o percentuale):
 * nessuno di loro corrisponde a una chiave della mappa.
 *
 * <p>Il server non serve nulla di scrivibile: l'unica richiesta onesta su un percorso della lista
 * chiusa è {@code GET}, e qualunque altro metodo vi risponde {@code 405}. Il controllo sul
 * percorso viene prima di quello sul metodo: questo gestore è registrato su {@code /}, cioè è il
 * ripiego per ogni percorso non intercettato da {@code /api/chronicle}, quindi un percorso
 * inesistente deve restare {@code 404} qualunque sia il metodo — il {@code 405} ha senso solo su
 * uno dei tre percorsi che esistono davvero.
 */
class StaticResourceHandler implements HttpHandler {

  private static final Map<String, StaticResource> RESOURCES_BY_PATH = Map.of(
      "/", new StaticResource("web/index.html", "text/html; charset=utf-8"),
      "/app.css", new StaticResource("web/app.css", "text/css; charset=utf-8"),
      "/app.js", new StaticResource("web/app.js", "text/javascript; charset=utf-8"));

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    StaticResource resource = RESOURCES_BY_PATH.get(exchange.getRequestURI().getPath());
    if (resource == null) {
      respondWithoutBody(exchange, 404);
      return;
    }

    if (!"GET".equals(exchange.getRequestMethod())) {
      respondWithoutBody(exchange, 405);
      return;
    }

    serveResource(exchange, resource);
  }

  private void serveResource(HttpExchange exchange, StaticResource resource) throws IOException {
    try (InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resource.classpathName())) {
      if (resourceStream == null) {
        respondWithoutBody(exchange, 404);
        return;
      }

      byte[] body = resourceStream.readAllBytes();
      exchange.getResponseHeaders().set("Content-Type", resource.contentType());
      exchange.sendResponseHeaders(200, body.length);
      try (OutputStream responseBody = exchange.getResponseBody()) {
        responseBody.write(body);
      }
    }
  }

  private void respondWithoutBody(HttpExchange exchange, int statusCode) throws IOException {
    exchange.sendResponseHeaders(statusCode, -1);
    exchange.close();
  }

  /**
   * Una voce della lista chiusa: il nome fisso della risorsa nel classpath e il content type con
   * cui va servita.
   */
  private record StaticResource(String classpathName, String contentType) {
  }
}
