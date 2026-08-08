package it.fantasyarena.combat.io.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.RoundOutcome;
import it.fantasyarena.combat.chronicle.ArenaChronicle;
import it.fantasyarena.combat.chronicle.HeroSnapshot;
import it.fantasyarena.combat.chronicle.RunConclusion;
import it.fantasycombatsystem.config.CombatSettings;
import it.fantasytoolkitcore.core.model.CharacterClass;
import it.fantasytoolkitcore.core.model.Race;

/**
 * Verifica {@link ArenaWebServer} su una porta effimera ({@code 0}), mai su una porta fissa: la
 * suite deve restare verde anche sulle macchine dove una porta scelta a mano fosse già occupata.
 * Il client è {@link HttpClient} del JDK. La cronaca servita è costruita a mano: nessuna partita
 * viene giocata davvero, per non portare la casualità dello scontro in test che verificano solo il
 * trasporto.
 */
class ArenaWebServerTest {

  private static final String PROTAGONIST_NAME = "Protagonista di prova";

  private final HttpClient httpClient = HttpClient.newHttpClient();

  private ArenaWebServer server;

  @AfterEach
  void fermaIlServer() {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  void getApiCronaca_rispondeDuecentoConJsonUtf8ECorpoCoerenteConLaCronacaFornita()
      throws IOException, InterruptedException {
    startServer(ArenaWebServerTest::sampleChronicle);

    HttpResponse<String> response = get("/api/chronicle");

    assertEquals(200, response.statusCode());
    assertEquals("application/json; charset=utf-8", response.headers().firstValue("Content-Type").orElse(null));
    assertEquals("no-store", response.headers().firstValue("Cache-Control").orElse(null));
    assertTrue(response.body().contains(PROTAGONIST_NAME));
  }

  @Test
  void getPaginaPrincipale_rispondeDuecentoConHtmlUtf8() throws IOException, InterruptedException {
    startServer(ArenaWebServerTest::sampleChronicle);

    HttpResponse<String> response = get("/");

    assertEquals(200, response.statusCode());
    assertEquals("text/html; charset=utf-8", response.headers().firstValue("Content-Type").orElse(null));
  }

  @Test
  void percorsoInesistente_rispondeQuattrocentoQuattro() throws IOException, InterruptedException {
    startServer(ArenaWebServerTest::sampleChronicle);

    HttpResponse<String> response = get("/percorso/sconosciuto");

    assertEquals(404, response.statusCode());
  }

  /**
   * {@code StaticResourceHandler} è registrato su {@code /}, cioè è il ripiego per ogni percorso
   * non intercettato da {@code /api/chronicle}: un metodo diverso da {@code GET} deve restare
   * {@code 404} su un percorso che non esiste, e diventare {@code 405} solo su uno dei tre
   * percorsi della lista chiusa — le due asserzioni insieme dicono qual è la regola.
   */
  @Test
  void metodoDiversoDaGet_rispondeQuattrocentoQuattroSuPercorsoIgnotoEQuattrocentoCinqueSuPercorsoNoto()
      throws IOException, InterruptedException {
    startServer(ArenaWebServerTest::sampleChronicle);

    HttpResponse<String> onUnknownPath = post("/percorso/sconosciuto");
    HttpResponse<String> onKnownPath = post("/app.css");

    assertEquals(404, onUnknownPath.statusCode());
    assertEquals(405, onKnownPath.statusCode());
  }

  @Test
  void tentativoDiUscireDallaCartellaDelleRisorse_rispondeQuattrocentoQuattroENonLeggeNulla()
      throws IOException, InterruptedException {
    startServer(ArenaWebServerTest::sampleChronicle);

    for (String traversalPath : List.of("/../pom.xml", "/%2e%2e/pom.xml", "/../../pom.xml")) {
      HttpResponse<String> response = get(traversalPath);

      assertEquals(404, response.statusCode(), "percorso: " + traversalPath);
      assertTrue(response.body().isEmpty(), "corpo non vuoto per: " + traversalPath);
    }
  }

  @Test
  void ogniRichiestaAllaCronaca_invocaIlFornitoreUnaVoltaSolaConPartiteIndipendenti()
      throws IOException, InterruptedException {
    AtomicInteger invocations = new AtomicInteger();
    startServer(() -> {
      invocations.incrementAndGet();
      return sampleChronicle();
    });

    get("/api/chronicle");
    get("/api/chronicle");

    assertEquals(2, invocations.get());
  }

  /**
   * {@code HttpServer.createContext} registra {@code /api/chronicle} come prefisso: senza un
   * confronto esplicito per uguaglianza, un sottopercorso inventato arriverebbe comunque al
   * gestore. L'asserzione che conta è quella sul fornitore mai invocato: dice che la partita non è
   * nemmeno cominciata, non solo che la risposta ha lo status atteso.
   */
  @Test
  void sottopercorsoSottoApiCronaca_rispondeQuattrocentoQuattroENonInvocaIlFornitore()
      throws IOException, InterruptedException {
    AtomicInteger invocations = new AtomicInteger();
    startServer(() -> {
      invocations.incrementAndGet();
      return sampleChronicle();
    });

    HttpResponse<String> response = get("/api/chronicle/qualsiasi-cosa");

    assertEquals(404, response.statusCode());
    assertEquals(0, invocations.get());
  }

  @Test
  void serverSiLegaSoloALoopbackEAccettaLaPortaComeParametro() {
    startServer(ArenaWebServerTest::sampleChronicle);

    InetSocketAddress boundAddress = server.address();

    assertEquals(InetAddress.getLoopbackAddress(), boundAddress.getAddress());
    assertTrue(boundAddress.getPort() > 0);
  }

  @Test
  void portaGiaOccupata_falliceConEccezioneCheNominaLaPorta() {
    startServer(ArenaWebServerTest::sampleChronicle);
    int occupiedPort = server.address().getPort();

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> new ArenaWebServer(occupiedPort, ArenaWebServerTest::sampleChronicle));

    assertTrue(exception.getMessage().contains(String.valueOf(occupiedPort)));
  }

  private void startServer(Supplier<ArenaChronicle> chronicles) {
    server = new ArenaWebServer(0, chronicles);
    server.start();
  }

  private HttpResponse<String> get(String path) throws IOException, InterruptedException {
    URI uri = URI.create("http://127.0.0.1:" + server.address().getPort() + path);
    HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> post(String path) throws IOException, InterruptedException {
    URI uri = URI.create("http://127.0.0.1:" + server.address().getPort() + path);
    HttpRequest request = HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.noBody()).build();
    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private static ArenaChronicle sampleChronicle() {
    HeroSnapshot protagonist = new HeroSnapshot(PROTAGONIST_NAME, Race.HUMAN, CharacterClass.WARRIOR, List.of(),
        List.of(), null, List.of(), List.of());

    return new ArenaChronicle(CombatSettings.defaults(), protagonist, 10, List.of(), new RunConclusion(RoundOutcome.WON, 1));
  }
}
