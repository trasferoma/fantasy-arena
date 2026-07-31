# IMPLEMENTATION — Interfaccia web come seconda modalità di presentazione

**Specifica di riferimento:** `spec-web-replay.md`  — nel resto del documento: «la SPEC».
**Stato:** `IN_PROGRESS`  <!-- NOT_STARTED | IN_PROGRESS | BLOCKED | COMPLETED -->
**Avanzamento:** Fasi 1-3 concluse. Sosta concordata con l'utente prima della Fase 4.

Documento di lavoro: la SPEC (il "cosa") resta stabile; qui vivono stato, piano, decisioni e problemi
(il "come").

## Regole per l'agente

- Leggere `CLAUDE.md` del repository e la SPEC prima di toccare codice. Non serve leggere il
  `CLAUDE.md` del motore: **il motore non si tocca** in questo compito.
- Alla ripresa del lavoro, leggere prima questo file e riprendere dallo stato corrente.
- Prima di modificare, elencare i file che verranno toccati. Nessun refactoring fuori scope.
- Non modificare i requisiti della SPEC senza decisione esplicita.
- Dopo ogni fase: eseguire i test pertinenti e aggiornare questo file. Spuntare una voce solo dopo
  verifica reale, mai a priori. **Anche le fasi senza codice** (analisi, revisione, documentazione,
  verifica manuale della pagina) si spuntano prima di passare alla successiva.
- Scelta che **non** cambia il comportamento osservabile → procedi e annotala in *Decisioni*.
- Scelta che **cambia** comportamento o criteri di accettazione, o ambiguità non risolvibile dalla
  SPEC → **fermati**, imposta lo stato a `BLOCKED` e registra in *Problemi aperti* / *Deviazioni*.
- Tre punti sono facili da tradire senza accorgersene, e vanno presidiati a ogni fase:
  **(a) la modalità console non si rompe in nessuna fase intermedia** — se una fase lascia l'output di
  console diverso da quello di partenza, la fase è sbagliata, non i test;
  **(b) la cronaca è di soli dati** — se ti trovi a mettere una stringa di presentazione dentro un
  record della cronaca, quella stringa appartiene a un renderer;
  **(c) la direzione delle dipendenze** — `combat.io.web` non importa `render`, `log`, `terminal` né
  `replay`, e nessun package del percorso console importa `web`.
- I punti «da decidere» della SPEC sono **tutti risolti** (sezione «Punti risolti dall'utente»):
  argomento `web` con porta opzionale e default `8080` che fallisce esplicitamente se occupata, nessuna
  apertura automatica del browser, UI al minimo utile, layout sobrio con barre e non arte ASCII,
  derivazione del destino del loot **unificata su `HeroProgress`**, cronaca **autosufficiente** in
  vista di una futura console che la legga, ricarica = partita nuova, gioielli fuori dal roster dello
  scontro. Nessuna fase è bloccata in partenza.
- Non toccare `C:/build/git/fantasy-combat-system` né `C:/build/git/fantasy-game-toolkit`: i tipi di
  risultato del motore si usano come sono.

## Piano operativo

**Fase 1 — Analisi (nessun codice)**
- [ ] Confermare i punti del *Contesto* della SPEC: i due punti di istanziazione dei logger di console
      in `MatchRunner` (righe 79 e 123), il costruttore con collaboratori espliciti di `Arena`
      (righe 91-99), l'unica lettura di stato mutabile in `Arena` (riga 186), l'assenza di
      `src/main/resources`, l'assenza di `dependencyManagement` nel `pom.xml`.
- [ ] Riverificare che nessun renderer/logger legga `Fighter.state()` (è l'assunzione su cui si regge
      «fotografare le schede non cambia l'output»): ricerca di `.state()` e `isDefeated()` in
      `src/main/java`.
- [ ] Elencare ciò che consuma `CombatLogger`, `ConsoleBattleLogger`, `ConsoleArenaLogger` e i
      `CombatReplay`, per dimensionare l'estrazione delle interfacce.
- [ ] Confermare che `BattleResult` e `CombatResult` hanno costruttori pubblici utilizzabili dal
      doppio `ScriptedFights` con liste vuote.
- [ ] Fissare la versione di `jackson-databind` da usare e verificare che sia risolvibile dal
      repository Maven locale (o dalla rete) senza trascinare altro.
- [ ] Rilevare lo stile dei test esistenti: `Assertions`, cattura di `System.out` (`ArenaTest`,
      `ConsoleBattleLoggerTest`), doppi per ereditarietà, dadi pilotati dal `test-jar` del motore.
- [ ] Compilare/confermare "File coinvolti (effettivi)".

**Fase 2 — Interfacce dei logger (console invariata)**
- [ ] Estrarre `ArenaLogger` da `ConsoleArenaLogger` e `BattleLogger` da `ConsoleBattleLogger`, con i
      soli metodi già chiamati; Javadoc che dichiara perché l'astrazione esiste ora (una seconda
      presentazione, non una previsione).
- [ ] Implementazioni mute delle tre interfacce di log (`CombatLogger` compresa), con un nome che dica
      cosa fanno: non mostrare niente.
- [ ] `Arena`: il costruttore con collaboratori espliciti riceve `ArenaLogger` invece della classe
      concreta; i costruttori di comodo continuano a passare quello di console.
- [ ] `MatchRunner`: `ConsoleBattleLogger` non si istanzia più dentro `playBattle` ma arriva dai
      collaboratori (verificato in Fase 1 che `reportSetup` ricostruisce il proprio
      `BattleSceneRenderer` a ogni chiamata, quindi una sola istanza regge le due battaglie di una
      corsa).
- [ ] Criterio di completamento: `mvn test` verde, output di console identico, nessun comportamento
      cambiato.

**Fase 3 — `MatchRunner` restituisce l'esito e delega la presentazione**
- [ ] `playDuel` → `CombatResult`, `playBattle` → `BattleResult`.
- [ ] Introdurre `MatchPresentation` (in `combat.io.replay`, che è lo strato che già compone logger,
      renderer, pacer e pulizia dello schermo) con l'implementazione di console — il codice di oggi
      spostato di casa, compresa l'attesa di lettura degli schieramenti — e quella muta.
- [ ] `MatchRunner` resta il posto che chiede l'esito al motore e non decide più *come* mostrarlo:
      aggiornarne il Javadoc di classe di conseguenza.
- [ ] Adeguare `ArenaTest.ScriptedFights` alle nuove firme (fabbrica un `BattleResult`/`CombatResult`
      con liste vuote): **adeguamento previsto, non sorpresa**, righe 207 e 217.
- [ ] Criterio di completamento: `mvn test` verde; l'output di console è identico riga per riga a
      quello di partenza; criterio 6 della *Definition of done* verificato.

**Fase 4 — I dati della cronaca**
- [ ] Nuovo package `combat.chronicle` con i record della SPEC: cronaca della corsa, voce di prova,
      forma della prova, fotografia del protagonista, fotografia del combattente, voce
      dell'equipaggiamento, procedura di fine scontro, destino del loot (gli otto casi), conclusione.
      Nessuna annotazione Jackson, nessuna dipendenza da `combat.io`.
- [ ] La derivazione del destino del loot sale su `HeroProgress` (un accessor che risolve il destino
      dai propri campi nullabili) e `HeroProgressFormatter` smette di dedurlo con la catena di
      `isPresent()`: scelta la frase sul destino già risolto. Output identico, `HeroProgressFormatterTest`
      è la rete.
- [ ] Il mapper che fotografa un `Fighter` (solo dati immutabili) e una `Hero`, e che legge il destino
      del loot da `HeroProgress` invece di riderivarlo.
- [ ] La cronaca è **autosufficiente** (criterio 14): porta come dati anche quello che oggi la console
      ricava da sé — numero e descrizione della prova, composizione degli schieramenti, accoppiamento
      del duello, esito, procedura di fine scontro. Le frasi restano dei renderer.
- [ ] Javadoc di package/classe che spiega perché la cronaca esiste (una partita è completamente
      determinata all'avvio: la UI legge un registro, non pilota un motore) e perché fotografa invece
      di referenziare (`Fighter.state()` è mutabile e a fine partita direbbe solo com'è finita).
- [ ] Criterio di completamento: il progetto compila; nessun comportamento cambiato; nessuno usa
      ancora questi tipi.

**Fase 5 — `Arena` registra e restituisce la cronaca**
- [ ] `Arena.run()` restituisce la cronaca; la scansione delle tre prove resta identica.
- [ ] Registrare, prova per prova: numero, descrizione, forma, roster fotografato **prima** dello
      scontro, passi dal risultato del motore, `RoundOutcome`, e la procedura di fine scontro quando
      c'è. Più l'ingresso del protagonista e la conclusione.
- [ ] Nessun `if` di gioco nuovo in `Arena`: la registrazione non decide niente (le decisioni restano
      di `HeroBrain`).
- [ ] Test della cronaca sullo stampo di `ArenaTest` (fabbrica e cervello pilotati, esiti a tavolino
      e/o `CombatSystem` su dadi pilotati dal `test-jar`): criteri 1, 2, 3, 4.
- [ ] Criterio di completamento: `mvn test` verde, console ancora identica.

**Fase 6 — La passata muta**
- [ ] Il punto che assembla l'arena muta passando dal costruttore con collaboratori espliciti che già
      esiste, con i logger muti, la presentazione muta e un `TurnPacer` che non attende. Nessuna porta
      d'ingresso nuova su `Arena`.
- [ ] Test: la passata muta non scrive nulla su `System.out` (cattura come in `ArenaTest`) e non legge
      da `System.in` — criterio 5.
- [ ] Criterio di completamento: criterio 5 verde; la partita muta produce una cronaca completa.

**Fase 7 — Serializzazione JSON**
- [ ] `jackson-databind` nel `pom.xml`, versione fissata in una property come le altre; nessun altro
      modulo Jackson.
- [ ] In `combat.io.web`, il traduttore della cronaca in JSON: `ObjectMapper` configurato in un punto
      solo, nessuna annotazione sui tipi della cronaca.
- [ ] Test su una cronaca costruita **a mano**: le chiavi che il frontend legge sono presenti
      (criterio 7) e nel JSON non compaiono `Fighter`, `Team`, `Optional` né stato mutabile
      (criterio 8).
- [ ] Criterio di completamento: criteri 7 e 8 verdi.

**Fase 8 — Il server**
- [ ] In `combat.io.web`: l'avvio dell'`HttpServer` su loopback e porta parametrica, il gestore della
      cronaca (fornitore iniettato, invocato una volta per richiesta), il gestore delle risorse
      statiche su **lista chiusa** di nomi noti sotto `web/` nel classpath.
- [ ] Content type e `Cache-Control` come da SPEC; lunghezza del corpo in **byte UTF-8**.
- [ ] Nessun import di `render`, `log`, `terminal`, `replay` da `combat.io.web`: verificarlo
      esplicitamente.
- [ ] Test con porta effimera e `HttpClient` del JDK: criteri 9, 10, 11 (compresi `404` sul percorso
      inesistente e sul tentativo di traversal, e il fornitore invocato una volta per richiesta con un
      doppio che conta).
- [ ] Criterio di completamento: criteri 9, 10, 11 verdi; nessuna porta fissa nei test.

**Fase 9 — La pagina (HTML/CSS/JavaScript vanilla)**
- [ ] `src/main/resources/web/` con pagina, foglio di stile e script; UTF-8 dichiarato nella pagina;
      nessuna libreria, nessuna CDN.
- [ ] Aspetto e ampiezza come deciso: **layout sobrio** a due colonne (i due schieramenti) con barre di
      vita e stamina e timeline in basso, **niente arte ASCII**; contenuto al **minimo utile** (schede,
      vita/stamina per passo, chi colpisce chi e cosa accade, procedura di fine scontro, esito).
      Iniziativa, scorecard, highlight e punteggi di squadra restano nella cronaca e fuori dalla pagina.
- [ ] Un solo `fetch` della cronaca all'apertura; la riproduzione vive interamente nel browser.
- [ ] Controlli: play, pausa, passo avanti, passo indietro, velocità, salto a un passo o a una prova.
- [ ] Le due forme di passo (round della battaglia, turni del duello) con la stessa macchina di
      riproduzione e due sole viste diverse.
- [ ] Fra una prova e l'altra: la procedura di fine scontro; alla fine trionfo o caduta.
- [ ] Verifica manuale a schermo (criterio 13). **Nessun test automatico sul JavaScript**: è la scelta
      dichiarata dalla SPEC, da annotare nel *Registro* come verificata a mano e non dimenticata.
- [ ] Criterio di completamento: la partita si guarda per intero nel browser, avanti e indietro, senza
      altre richieste al server (verificabile dal pannello di rete).

**Fase 10 — La scelta della modalità**
- [ ] Il dato della modalità con un solo punto di parsing degli argomenti, e la porta opzionale
      (default `8080`).
- [ ] `Main` resta thin: due righe, una per strada; argomento non riconosciuto → errore esplicito con
      le modalità ammesse; porta già occupata → errore esplicito, nessuna ricerca silenziosa di una
      porta libera.
- [ ] In modalità web su console va solo l'indirizzo: nessuno scontro stampato.
- [ ] Test del parsing e della modalità: criterio 12.
- [ ] Criterio di completamento: criterio 12 verde; `mvn exec:java` senza argomenti si comporta
      esattamente come oggi.

**Fase 11 — Revisione funzionale (`java-functional-evolver`)**
- [ ] Invocare la revisione sul codice Java prodotto (cronaca, mapper, `MatchRunner`/`Arena`,
      `combat.io.web`) e annotarne l'esito nel *Registro*.
- [ ] Ri-eseguire `mvn test` dopo eventuali modifiche.
- [ ] Nessuna revisione funzionale sul JavaScript e sul CSS: non è Java.

**Fase 12 — Documentazione (nessun codice di produzione)**
- [ ] `CLAUDE.md`: i nuovi package nella tabella (`combat.chronicle` come dati della partita,
      `combat.io.web` come pozzo che dipende solo dalla cronaca e da Jackson), la direzione delle
      dipendenze aggiornata, le due modalità di presentazione e come si scelgono, il fatto che
      `Arena.run()` restituisce una cronaca e che `MatchRunner` restituisce l'esito, la nuova
      dipendenza Jackson fra quelle critiche (con la differenza che questa **è** su Maven Central), il
      comando per lanciare la modalità web.
- [ ] `README.md`: il gioco si può guardare anche nel browser, e perché è possibile (la partita è
      decisa all'avvio).
- [ ] `daImplementare.md`: rileggere le voci che questa modifica rende obsolete o riscrivibili;
      aggiungere ciò che la pagina non copre e che resta nella cronaca (iniziativa, scorecard,
      highlight, punteggi di squadra) più la console-lettrice-della-cronaca come compito successivo.
- [ ] Criterio di completamento: criteri 14 e 15 verificati in revisione.

**Fase 13 — Revisione finale**
- [ ] Coerenza con la SPEC; nessuna modifica non richiesta; nessuna regola di combattimento nata qui;
      nessun import proibito in `combat.io.web`; nessuna stringa di presentazione nella cronaca.
- [ ] `mvn test` verde; `mvn exec:java` in console identico a oggi; `mvn exec:java` in modalità web
      apre la pagina e la partita si guarda per intero.
- [ ] Aggiornare *Decisioni/Deviazioni* ed *Esito finale*; portare lo stato a `COMPLETED`.

## File coinvolti (effettivi)

Pre-compilati in via **provvisoria** dall'analisi della SPEC: da confermare e completare in Fase 1.
I nomi dei tipi nuovi sono proposte, non vincoli: quello che vincola è il ruolo.

**Presentazione sostituibile (Fasi 2-3)**
- `src/main/java/it/fantasyarena/combat/io/log/ArenaLogger.java` — nuovo: interfaccia estratta
- `src/main/java/it/fantasyarena/combat/io/log/ConsoleArenaLogger.java` — implementa l'interfaccia
- `src/main/java/it/fantasyarena/combat/io/log/BattleLogger.java` — nuovo: interfaccia estratta
- `src/main/java/it/fantasyarena/combat/io/log/ConsoleBattleLogger.java` — implementa l'interfaccia
- `src/main/java/it/fantasyarena/combat/io/log/` — le tre implementazioni mute
- `src/main/java/it/fantasyarena/combat/io/replay/MatchPresentation.java` — nuovo: il seam della
  presentazione di uno scontro, con l'implementazione di console e quella muta
- `src/main/java/it/fantasyarena/combat/io/terminal/TurnPacer.java` — il pacer che non attende
- `src/main/java/it/fantasyarena/combat/MatchRunner.java` — restituisce l'esito, delega la
  presentazione, non istanzia più i logger
- `src/main/java/it/fantasyarena/combat/Arena.java` — riceve `ArenaLogger`, registra, restituisce la
  cronaca

**La cronaca (Fasi 4-6)**
- `src/main/java/it/fantasyarena/combat/chronicle/` — nuovo package: cronaca della corsa, voce di
  prova, forma della prova, fotografia del protagonista e del combattente, equipaggiamento,
  procedura di fine scontro, destino del loot, conclusione, più il mapper
- `src/main/java/it/fantasyarena/combat/hero/HeroProgress.java` — la derivazione del destino del loot
  sale qui, in un punto solo
- `src/main/java/it/fantasyarena/combat/io/render/HeroProgressFormatter.java` — legge il destino
  risolto invece di dedurlo dagli `Optional`; output identico

**Web (Fasi 7-8)**
- `pom.xml` — `jackson-databind` e la sua property di versione
- `src/main/java/it/fantasyarena/combat/io/web/` — nuovo package: avvio del server, gestore della
  cronaca, gestore delle risorse statiche, traduttore JSON
- `src/main/resources/web/index.html`, `app.css`, `app.js` — nuovi (la cartella `src/main/resources`
  oggi non esiste)

**Ingresso (Fase 10)**
- `src/main/java/it/fantasyarena/Main.java` — due strade, resta thin
- `src/main/java/it/fantasyarena/UiMode.java` — nuovo: unico punto di parsing degli argomenti

**Test**
- `src/test/java/it/fantasyarena/combat/chronicle/` — nuovo: la cronaca come dato (criteri 1-4)
- `src/test/java/it/fantasyarena/combat/ArenaTest.java` — **adeguamento previsto** del doppio
  `ScriptedFights` alle nuove firme (righe 207 e 217); nessun cambio di aspettative
- `src/test/java/it/fantasyarena/combat/` — nuovo: la passata muta non stampa niente (criterio 5)
- `src/test/java/it/fantasyarena/combat/io/web/` — nuovo: serializzazione JSON su cronaca costruita a
  mano (criteri 7-8) e server su porta effimera (criteri 9-11)
- `src/test/java/it/fantasyarena/UiModeTest.java` — nuovo: selezione della modalità (criterio 12)
- Da verificare in Fase 1 (possibile impatto, non modifica prevista): `ConsoleBattleLoggerTest`,
  `ConsoleCombatLoggerOutcomeTest`, `CombatScreenRendererTest`, `BattleSceneRendererTest`,
  `TurnLogFormatterTest`, `HeroProgressFormatterTest`, `ArenaFighterFactoryTest`,
  `FighterFactoryTest`, `CombatSetupPromptTest`, `ScreenCleanerTest`

**Documentazione (Fase 12)**
- `CLAUDE.md`, `README.md`, `daImplementare.md`

## Registro

Voci datate (`YYYY-MM-DD`), append-only.

- **Decisioni tecniche** (non cambiano il comportamento) — `Decisione · Motivazione · Impatto`:
  nessuna.
- **Deviazioni dalla SPEC** (da motivare) — `Descrizione · Motivazione · Impatto · Aggiorna la SPEC?
  sì/no`: nessuna.
- **Problemi aperti** (bloccano l'avanzamento) — `Descrizione · Impatto · Opzioni · Decisione
  richiesta`: nessuno. 2026-07-31 · gli otto punti «da decidere» della SPEC sono stati risolti
  dall'utente prima dell'inizio del lavoro; la SPEC li riporta chiusi.
- **Test eseguiti** — `data · fase · comando · esito`: nessuno.
- **Verifica manuale della pagina** — `data · cosa è stato provato · esito`: da compilare in Fase 9.
- **Revisione `java-functional-evolver`** — `data · invocato sì/no · cosa ha cambiato`: da compilare
  in Fase 11.
- **Output di console confrontato con quello di partenza** — `data · fase · identico sì/no`: da
  compilare alla fine di ogni fase da 2 a 6, ed è la rete di sicurezza del compito.

## Esito finale

Da compilare a fine lavoro: stato finale, modifiche effettuate, test eseguiti, verifica manuale della
pagina, note residue.

## Esempio (concreto: i file previsti e i test previsti)

```java
// File previsti — PRESENTAZIONE SOSTITUIBILE:
//   ArenaLogger / BattleLogger  — interfacce estratte dalle due classi concrete di console
//   ...SilentArenaLogger / ...  — implementazioni mute delle tre interfacce di log
//   MatchPresentation           — il seam: console (codice di oggi spostato) e muta
//   MatchRunner                 — restituisce CombatResult/BattleResult, non istanzia più i logger
//   Arena                       — riceve ArenaLogger, registra, restituisce la cronaca
//
// File previsti — LA CRONACA:
//   ArenaChronicle / TrialChronicle / TrialShape
//   HeroSnapshot / CombatantSnapshot / ItemSnapshot
//   ProgressChronicle / LootFate / RunConclusion
//   il mapper: fotografa Fighter e Hero, deriva il destino del loot in un punto solo
//
// File previsti — WEB:
//   ChronicleJson               — ObjectMapper configurato in un punto solo
//   ChronicleHandler            — fornitore iniettato, una partita per richiesta
//   StaticResourceHandler       — lista chiusa di nomi sotto web/, nessun percorso dalla richiesta
//   ArenaWebServer              — HttpServer su loopback, porta parametrica
//   resources/web/index.html, app.css, app.js
//
// File previsti — INGRESSO:
//   UiMode                      — unico punto di parsing degli argomenti
//   Main                        — due strade, resta thin

// Test: uno per criterio della DoD (il 13 a mano, il 14 e il 15 in revisione)
@Test void cronaca_registraIngressoTreProveEConclusione()        { /* criterio 1 */ }
@Test void cronaca_portaLaProceduraDiFineScontroDiOgniProvaVinta(){ /* criterio 2, destino del loot */ }
@Test void schedeDellaCronaca_nonPortanoLeFeriteDelloScontro()   { /* criterio 3 */ }
@Test void dopoUnaProvaNonVinta_laCronacaSiChiudeLi()            { /* criterio 4 */ }
@Test void passataMuta_nonStampaNientaENonLeggeStdin()           { /* criterio 5 */ }
@Test void modalitaConsole_outputInvariato()                     { /* criterio 6, suite preesistente */ }
@Test void json_contieneLeChiaviCheIlFrontendLegge()             { /* criterio 7, cronaca a mano */ }
@Test void json_nessunFighterNessunOptionalNessunoStatoMutabile() { /* criterio 8 */ }
@Test void getCronaca_duecentoJsonUtf8()                         { /* criterio 9, porta effimera */ }
@Test void getPagina_duecentoHtmlUtf8()                          { /* criterio 9 */ }
@Test void percorsoInesistenteETraversal_quattrocentoQuattro()   { /* criterio 9 */ }
@Test void ogniRichiesta_invocaIlFornitoreUnaVoltaSola()         { /* criterio 10 */ }
@Test void server_soloLoopbackEPortaParametrica()                { /* criterio 11 */ }
@Test void senzaArgomenti_console_conArgomentoWeb_server()       { /* criterio 12 */ }
@Test void argomentoNonRiconosciuto_erroreEsplicito()            { /* criterio 12 */ }
```
