# SPEC — Interfaccia web come seconda modalità di presentazione

**Obiettivo:** separare il *giocare* l'arena dal *mostrarla*, così che la stessa partita possa essere
riprodotta in un browser con i controlli che il terminale non può avere (play, pausa, passo avanti,
passo indietro, velocità, salto a un passo), lasciando la modalità console esattamente com'è oggi.

Il compito riguarda **solo** `fantasy-arena`. Il motore `fantasy-combat-system` non si tocca: i suoi
tipi di risultato restano la fonte dati della presentazione, e nessuna regola di combattimento nasce
qui.

Il fatto che regge tutto il disegno è che **il gioco non ha scelte del giocatore**: `Arena.run()`
genera, combatte e fa crescere il protagonista senza chiedere niente, e l'unica interazione è l'INVIO
che scandisce il ritmo (`TurnPacer`). Una partita è quindi **completamente determinata all'avvio**: la
UI non è un client interattivo, è il lettore di una cronaca già scritta. Da qui segue tutto il resto —
una passata che gioca e registra, e due presentazioni che leggono lo stesso registro.

## Contesto

**Il gioco — `C:/build/git/fantasy-arena`**

- `combat.Arena` — scandisce le tre prove. `run()` è `void`: gioca e stampa, non restituisce niente,
  quindi oggi non esiste alcun modo di ottenere «com'è andata» se non leggendo la console. Ha tre
  costruttori: due di comodo che si costruiscono i collaboratori da sé (`Arena(CombatSettings)`,
  `Arena(CombatSettings, ScreenRefresh)`) e uno con collaboratori espliciti (righe 91-99) già usato da
  `ArenaTest` — è la porta d'ingresso da riusare, non da duplicare. Riceve un `ConsoleArenaLogger`
  **concreto**, non un'interfaccia. `outcomeOf` (riga 185) è l'unico punto del gioco che legge lo
  stato mutabile del `Fighter` (`isDefeated()`, riga 186). `applyEndOfFightProcedure` produce lo
  `HeroProgress` e attende l'INVIO.
- `combat.MatchRunner` — gioca **un** singolo scontro e lo mette in scena; `playDuel` e `playBattle`
  sono `void` e **non restituiscono** l'esito che hanno appena chiesto al motore. Istanzia le proprie
  presentazioni al proprio interno: `new ConsoleCombatLogger()` alla riga 79 (campo `logger`) e
  `new ConsoleBattleLogger()` alla riga 123 (variabile locale di `playBattle`). `CombatLogger` è già
  un'interfaccia; `ConsoleBattleLogger` e `ConsoleArenaLogger` sono **classi concrete senza
  interfaccia**. Finché resta così, nessuna presentazione alternativa può sostituirsi alla console.
  Costruisce anche `TurnPacer`, `ScreenCleaner` e il `CombatReplay` (`buildReplay`, `duelReplay`).
- `combat.io.terminal.EnterKeyTurnPacer.awaitNextTurn()` — blocca il thread leggendo da `System.in`
  (`BufferedReader.readLine()`); su EOF prosegue senza bloccarsi. `TurnPacer` ha un solo metodo e in
  `ArenaTest` è già sostituito da una lambda vuota (riga 156-158).
- `combat.io.terminal.ScreenCleaner.clear()` — in `ScreenRefresh.SCROLL` non fa nulla; in `CLEAR`
  scrive la sequenza ANSI e 80 righe vuote su `System.out`.
- `combat.io.render` — **verificato**: nessun renderer del gioco legge lo stato mutabile del
  `Fighter`. `FighterCardFormatter` compone la scheda da `character()`, `weapon()`, `armourPieces()` e
  `ratings()` (vita e stamina **massime**, non correnti); `BattleSceneRenderer` legge le
  `FighterVitals` dei `RoundLogEntry`; `CombatScreenRenderer` la pagina dal log. La ricerca di
  `.state()` / `isDefeated()` in `src/main/java` trova una sola occorrenza, in `Arena`. Conseguenza
  che serve al disegno: **fotografare le schede o giocare prima lo scontro non cambia una riga di
  output**.
- `combat.io.render.FighterProfile(String name, int teamIndex, int maxHealth, int maxStamina)` — il
  precedente da imitare: un record di soli dati, costruito nell'ordine di roster di
  `BattleRoster#all()`, che ha già liberato `BattleSceneRenderer` dalla dipendenza dal `Fighter`.
- `combat.hero.HeroProgress` — resoconto in forma di dati, con il destino del loot spalmato su cinque
  campi nullabili che affiorano come `Optional` (`weaponSwap`, `newPiece`, `armourUpgrade`,
  `newJewel`, `jewelUpgrade`); il Javadoc dichiara che i destini sono «uno solo fra sei» e
  `HeroProgressFormatter` li discrimina con una catena di `isPresent()`.
- `combat.hero.Hero` — scheda immutabile del protagonista (arma, armatura per slot, gioielli per
  tipo). `Hero` non è `Fighter`: la scheda sopravvive ai round, il combattente vive un round solo.
- `combat.factory.FighterFactory` — unico punto di contatto coi generatori del toolkit, con stato
  proprio: `usedNames` (nomi già assegnati, per l'intera vita dell'istanza) e `random`. Due partite
  concorrenti sulla stessa istanza si contenderebbero quello stato.
- `combat.io` a strati con dipendenze a senso unico: `replay` → `log` → `render`, e sia `replay` sia
  `log` → `terminal`. `render` è foglia e non importa dagli altri tre.
- `ArenaTest` — il modello di riproducibilità già in casa: `HeroBrain(new Random(7))`, una
  `RecordingFighterFactory extends FighterFactory`, un doppio `ScriptedFights extends MatchRunner` che
  **sovrascrive `playBattle` e `playDuel`** applicando esiti a tavolino, un `TurnPacer` lambda vuoto e
  `System.out` catturato in un `ByteArrayOutputStream`. Il record privato `SummonedChampion` esiste
  proprio perché lo stato del `Fighter` è mutabile e a fine partita direbbe solo com'è finita.
- `pom.xml` — Java 21, nessun `dependencyManagement`, tre dipendenze di produzione (motore, toolkit) +
  JUnit Jupiter 5.11.4, surefire 3.5.2, `exec.mainClass=it.fantasyarena.Main`.
  **`src/main/resources` non esiste**: va creata.
- `Main` — thin: `CombatSettings.defaults()`, uno `ScreenRefresh` cablato e `new Arena(...).run()`.

**Il motore (dipendenza, sola lettura) — `C:/build/git/fantasy-combat-system`**

- `CombatSystem.duel(...)` → `CombatResult`; `CombatSystem.battle(BattleSetup)` → `BattleResult`.
  Entrambi giocano lo scontro **per intero** e restituiscono il log completo: «questa libreria non
  stampa e non decide il ritmo».
- Tipi di log **puri**, senza riferimenti al `Fighter` e senza `Optional` (verificato leggendoli):
  `TurnLogEntry(turnNumber, description, List<FighterVitals>, InitiativeReport, List<StaminaChange>, List<TurnHighlight>, ActionOutcome)`,
  `RoundLogEntry(roundNumber, List<EngagementTurn>, List<FighterVitals>, List<String> events)`,
  `EngagementTurn(engagementId, attackerIndex, targetIndex, List<Integer> participantIndexes, TurnLogEntry)`,
  `FighterVitals(name, currentHealth, maxHealth, currentStamina, maxStamina)`, `InitiativeReport`,
  `InitiativeBreakdown`, `StaminaChange`, `ActionOutcome(Kind, damage, staminaRecovered, critical, powerStrike)`,
  `TurnHighlight` (enum), `Scorecard`, `TeamScore`. Sono record o enum: serializzabili da Jackson
  senza annotazioni.
- Tipi che **non** sono serializzabili così come sono: `CombatResult.winner()` → `Optional<Fighter>`,
  `BattleResult.winningTeam()` → `Optional<Team>` (e `Team` contiene `List<Fighter>`),
  `BattleSetup.teams()` → `Fighter`, `Fighter.shield()` → `Optional<ArmourResult>`, `Fighter.state()`
  → `FighterState` **mutabile**. Serializzare un `Fighter` a fine partita non descriverebbe lo
  scontro: racconterebbe soltanto com'è finito.
- `EngagementTurn.attackerIndex`/`targetIndex` sono posizioni in `BattleRoster#all()`, cioè i membri
  della squadra 0 seguiti da quelli della squadra 1, nell'ordine dei rispettivi roster — la stessa
  convenzione che `ConsoleBattleLogger.reportSetup` usa già per costruire l'elenco di
  `FighterProfile`. Nel duello **non esistono** indici equivalenti: `CombatResult.log()` è una lista
  di `TurnLogEntry` e l'unico modo di sapere chi ha agito è `initiative().chosenName()`, cioè il
  **nome** — che il motore dichiara esplicitamente inaffidabile come identificatore (due combattenti
  generati possono chiamarsi allo stesso modo).
- `testsupport` del `test-jar`: `CombatFixtures`, `StubDiceRoller`, `RecordingStubDiceRoller` per
  costruire `Fighter` deterministici e pilotare i dadi.

**Scelte tecnologiche già prese dall'utente, fuori discussione**

- `com.sun.net.httpserver.HttpServer` del JDK: nessun framework web, nessun server di gioco.
- Unica nuova dipendenza Maven ammessa: `com.fasterxml.jackson.core:jackson-databind`.
- Frontend: una pagina statica HTML/CSS/JavaScript vanilla servita dalle risorse del classpath.
  Nessuna libreria JS, nessuna CDN, nessun build step, nessun npm.

## Comportamento atteso

### 1. Giocare e mostrare diventano due cose distinte

- `MatchRunner.playDuel` e `MatchRunner.playBattle` **restituiscono** l'esito che chiedono al motore
  (`CombatResult`, `BattleResult`) invece di limitarsi a stamparlo. Il chiamante può ignorarlo: la
  console lo ignora, la cronaca lo raccoglie.
- Il *come* e il *quando* mostrare uno scontro escono da `MatchRunner` e diventano un collaboratore
  sostituibile: `MatchPresentation`, con due implementazioni — quella di console, che è il codice di
  oggi spostato di casa (logger, `CombatReplay`, `TurnPacer`, `ScreenCleaner`, l'attesa di lettura
  degli schieramenti), e una **muta**, che non mostra niente e non attende niente.
- `ConsoleBattleLogger` e `ConsoleArenaLogger` guadagnano un'interfaccia ciascuno (`BattleLogger`,
  `ArenaLogger`), con implementazione muta; `CombatLogger` ce l'ha già. `Arena` riceve
  l'**interfaccia** nel costruttore con collaboratori espliciti, non più la classe concreta.
- L'ordine in cui la console stampa non cambia, anche se lo scontro viene giocato prima di mostrare
  gli schieramenti: le schede si compongono da dati immutabili (vedi *Contesto*), quindi l'output è
  identico riga per riga.

### 2. La cronaca: una partita in forma di dati

- `Arena.run()` restituisce un `ArenaChronicle`: il registro completo della corsa, di soli dati e
  nessuna stringa di presentazione. Lo costruisce **sempre**, in entrambe le modalità: è la stessa
  partita, letta due volte, e non c'è un percorso «per il web» che possa divergere da quello della
  console.
- La cronaca contiene:
  - l'**ingresso** del protagonista: nome, razza, classe, caratteristiche, arma, pezzi indossati,
    gioielli;
  - una voce per ogni prova **giocata** (mai per quelle non giocate): numero, descrizione, forma
    della prova (battaglia NvN o duello 1v1), il **roster** dello scontro come schede fotografate
    *prima* dello scontro, i **passi** dello scontro nell'ordine, e l'esito della prova
    (`RoundOutcome`);
  - per ogni prova vinta, i dati della **procedura di fine scontro**: l'oggetto trovato, il suo
    destino come dato (non come frase), l'eventuale oggetto lasciato, i punti caratteristica
    caratteristica per caratteristica, e la scheda del protagonista dopo la crescita;
  - la **conclusione** della corsa: trionfo, oppure com'è finita e a quale prova.
- I passi sono l'unità di avanzamento della UI e arrivano dal motore senza reinterpretazioni: i
  `RoundLogEntry` per la battaglia, i `TurnLogEntry` per il duello. Ogni passo ha una posizione
  progressiva, così muoversi avanti e indietro è indicizzare una lista, non ricalcolare niente.
- Le schede del roster portano gli indici nell'ordine di `BattleRoster#all()` (squadra 0, poi squadra
  1), che è la convenzione con cui `EngagementTurn` riferisce attaccante e bersaglio.
- Nessuno stato mutabile del motore entra nella cronaca: né `Fighter`, né `FighterState`, né `Team`,
  né `BattleSetup`.

### 3. La passata muta

- Esiste un modo di giocare l'intera arena **senza stampare nulla e senza attendere nulla**:
  `Arena` costruita tramite il costruttore con collaboratori espliciti, con i logger muti, la
  presentazione muta e un `TurnPacer` che non attende. Nessun byte su `System.out`, nessuna lettura
  da `System.in`.
- La casualità resta dov'è: `FighterFactory` per la generazione, `HeroBrain` per i punti
  caratteristica, il `DiceRoller` del motore per lo scontro. La passata muta non ne aggiunge.

### 4. Il server

- In modalità web l'applicazione avvia un `HttpServer` in ascolto **solo su loopback**, su una porta
  configurabile, e stampa su console soltanto l'indirizzo da aprire. Nient'altro va a schermo: la
  partita si guarda nel browser.
- Tre risposte e nulla più:
  - `GET /` → la pagina, `text/html; charset=utf-8`;
  - `GET /app.css`, `GET /app.js` → le risorse statiche dal classpath, con il proprio content type;
  - `GET /api/chronicle` → la cronaca completa in JSON, `application/json; charset=utf-8`,
    `Cache-Control: no-store`.
- Qualunque altro percorso risponde `404`. Le risorse statiche si servono da una **lista chiusa** di
  nomi noti sotto `web/` nel classpath: nessun percorso costruito dalla richiesta, quindi nessun
  traversal possibile.
- La cronaca arriva al gestore da un **fornitore** iniettato, invocato una volta per richiesta: ogni
  richiesta è una partita nuova e completa, giocata al momento (sono millisecondi), con collaboratori
  propri. Nessuno stato condiviso fra richieste — che è anche l'unico modo sano di convivere con lo
  stato interno di `FighterFactory` (`usedNames`).
- La lunghezza del corpo è la lunghezza in **byte UTF-8**, non in caratteri: la narrazione del motore
  è in italiano e accentata.

### 5. La pagina

- Carica la cronaca **una volta sola** all'apertura e la riproduce da sola: durante lo scontro non fa
  nessuna richiesta al server.
- Offre i controlli che il terminale non può avere: play, pausa, passo avanti, passo indietro,
  velocità di riproduzione, salto a un passo o a una prova. Ricaricare la pagina significa giocare
  una partita nuova.
- Mostra, per ogni passo: chi attacca chi, cosa è accaduto, vita e stamina di tutti i combattenti;
  fra una prova e l'altra il racconto della procedura di fine scontro; alla fine trionfo o caduta.
- È una pagina HTML con barre e riquadri, **non** una riproduzione dell'arte ASCII del terminale:
  `BattleSceneRenderer` e `CombatScreenRenderer` restano alla console e non vengono né riusati né
  generalizzati.

### 6. La scelta della modalità

- `Main` resta thin: legge la modalità dagli argomenti di riga di comando (nessun argomento →
  console, come oggi; l'argomento della modalità web → server), la traduce in un dato con un solo
  punto di parsing, e apre l'una o l'altra strada in due righe.
- Un argomento non riconosciuto è un errore esplicito, con l'elenco delle modalità ammesse: non un
  fallback silenzioso sulla console.
- Invariante: senza argomenti il gioco si comporta **esattamente** come oggi.

## Vincoli

- **Nessuna regola di combattimento qui.** La cronaca registra quello che il motore ha deciso; non
  calcola danni, iniziativa, stamina o esiti. L'unica cosa che il gioco stabilisce resta il
  `RoundOutcome`, come oggi e in un punto solo (`Arena`).
- **Il motore non si tocca.** Nessuna modifica a `fantasy-combat-system`, nessun `mvn install` di
  quel repository in questo compito.
- **Retrocompatibilità della console**: nessuna fase intermedia può degradare la modalità console.
  Alla fine del compito l'output di console è identico a quello di partenza.
- **Direzione delle dipendenze in `combat.io` preservata**: `replay` → `log` → `render`, `terminal`
  sotto tutti, `render` foglia. Il nuovo `combat.io.web` è un **pozzo** come `log`: dipende dai dati
  della cronaca e da Jackson, e **non importa** `render`, `log`, `terminal` né `replay`. Nessun
  package del percorso console importa `web`.
- Il package dei dati della cronaca non dipende da nessun package di `combat.io` e non conosce
  Jackson: nessuna annotazione di serializzazione sui suoi tipi. Jackson vive confinato in
  `combat.io.web`.
- **I renderer restano puri** (righe di testo, nessun I/O) e l'I/O nuovo (HTTP, lettura delle risorse
  dal classpath) sta in `combat.io.web`, non nei renderer.
- `Hero` non è `Fighter`: la cronaca fotografa entrambi, ma non li confonde e non introduce API di
  guarigione.
- Nessuna nuova dipendenza oltre a `jackson-databind`. In particolare **nessun** modulo Jackson
  aggiuntivo: se nel JSON servisse un `Optional`, la risposta è cambiare il dato, non aggiungere il
  modulo.
- Nessun framework web, nessun template engine, nessuna libreria JS, nessuna CDN, nessun build step
  frontend.
- Il server non serve nulla di scrivibile e non accetta nessun input dall'utente oltre al percorso
  della richiesta: non esistono `POST`, form, parametri di gioco.
- Java 21, Maven, indentazione **2 spazi** (`FighterFactory` resta a 4 se lo si tocca). Javadoc e
  commenti in **italiano**, nomi di codice in inglese. Niente sealed type, text block, pattern
  matching, preview feature. Record per i tipi valore, switch expression, `List.getFirst()` sono in
  uso.
- Test JUnit Jupiter con le sole assertion di `org.junit.jupiter.api.Assertions`, **niente AssertJ**.
  Nei test il server si lega a una **porta effimera**, mai a una porta fissa.
- HTML, CSS e JavaScript in UTF-8, con l'encoding dichiarato nella pagina.

## Decisioni di progettazione sciolte in questa SPEC

1. **La cronaca si costruisce sempre, in entrambe le modalità.** L'alternativa — un percorso headless
   separato, usato solo dal web — avrebbe due strade che possono divergere: la console racconterebbe
   una cosa e il browser un'altra, e nessun test se ne accorgerebbe. Costruire la cronaca anche
   quando nessuno la legge costa una lista di record per partita ed elimina la classe di bug in cui
   le due presentazioni discordano.
2. **`MatchRunner` restituisce l'esito.** È la conseguenza minima e onesta del suo Javadoc («chiede
   l'esito al motore e decide solo con che ritmo rivelarlo»): oggi lo chiede e lo butta. Il prezzo è
   noto e previsto: il doppio `ScriptedFights` di `ArenaTest` sovrascrive entrambi i metodi e dovrà
   fabbricare un risultato (i costruttori di `BattleResult` e `CombatResult` sono pubblici e
   accettano liste vuote). È un adeguamento di firma, non un cambio di aspettative.
3. **La presentazione dello scontro diventa un collaboratore, non un flag.** Nessun `ReplayMode.NONE`
   e nessun booleano «silenzioso»: sarebbero un modo di dire «presenta» a chi non deve presentare
   niente. Un `MatchPresentation` muto, invece, rende la passata muta un caso ordinario e non un
   ramo speciale del codice di stampa.
4. **Le schede della cronaca sono fotografie di dati immutabili.** Verificato che nessun renderer del
   gioco legga `Fighter.state()`, la fotografia è integralmente derivabile da personaggio, arma,
   pezzi e rating, e non dipende dall'istante in cui la si scatta. È lo stesso rimedio che
   `ArenaTest` si è già costruito con `SummonedChampion` e che `FighterProfile` applica in `render`.
5. **Due forme di passo, non una unificata.** La battaglia dà `RoundLogEntry` con gli indici di
   roster; il duello dà `TurnLogEntry` senza indici, e l'unico modo di ricavare chi ha agito è il
   **nome**, che il motore dichiara inaffidabile per l'identificazione. Fabbricare indici per il
   duello significherebbe inventare un dato: la cronaca dichiara invece la forma della prova e porta
   i passi nella forma che il motore ha prodotto. La macchina di riproduzione della UI (avanti,
   indietro, salto) è comunque una sola, perché in entrambi i casi i passi sono una lista ordinata.
6. **Il destino del loot diventa un dato**, non una catena di `Optional` da interpretare: un enum con
   gli otto destini possibili (arma tenuta/scartata, armatura su slot nuovo/sostituita/scartata,
   gioiello su tipo nuovo/sostituito/scartato) più l'oggetto trovato e quello lasciato. Il frontend
   non deve dedurre il caso dalla presenza di un campo.

   La derivazione vive **su `HeroProgress`** (un accessor che risolve il destino dai propri campi
   nullabili) e la leggono entrambi: il mapper della cronaca e `HeroProgressFormatter`, che perde la
   catena di `isPresent()` e sceglie la frase sul destino già risolto. L'alternativa scartata era
   duplicare la derivazione nel mapper lasciando il formatter intatto: due letture della stessa cosa
   che devono restare d'accordo sono un invariante che regge sulla disciplina di chi le modifica,
   mentre una derivazione sola è un invariante strutturale. Il percorso console viene toccato, ma a
   output identico e con `HeroProgressFormatterTest` già in casa a fare da rete.
7. **DTO propri al confine, tipi del motore all'interno del passo.** Il JSON non espone mai
   `Fighter`, `Team`, `BattleSetup` né alcun `Optional`: lì la cronaca ha record propri di questo
   repository. Dentro il passo, invece, riusa i record di log del motore (`TurnLogEntry`,
   `RoundLogEntry`, `EngagementTurn`, `FighterVitals`, `InitiativeReport`, `ActionOutcome`, ...), che
   sono già documentati come «solo dati per il log», sono già il contratto di presentazione su cui
   vivono i renderer di console, e riscriverli significherebbe mantenere due volte la stessa forma.
   Il rischio residuo — sono tipi `SNAPSHOT`, un campo rinominato romperebbe il JavaScript in
   silenzio — si presidia con un test di serializzazione che asserisce le **chiavi** che il frontend
   legge: se il motore cambia forma, quel test si accende prima del browser.
8. **Una partita per richiesta, nessuna cache.** Una partita dura millisecondi e non ha stato da
   conservare; una cronaca tenuta in memoria all'avvio darebbe la stessa partita a ogni ricarica e
   servirebbe subito un modo per rigenerarla. Con una partita per richiesta il pulsante «gioca
   ancora» è la ricarica della pagina, e lo stato interno di `FighterFactory` non è mai condiviso.
9. **Ascolto solo su loopback.** È un gioco a uso locale che serve una pagina generata da esecuzione
   di codice: esporlo sull'interfaccia di rete non porta alcun beneficio e apre una superficie che
   nessuno ha chiesto.
10. **La modalità si sceglie con un argomento di riga di comando**, non con una property di sistema:
    `mvn exec:java -Dexec.args="web"` è già il canale con cui questo progetto si lancia, e un
    argomento è visibile nel comando invece di nascondersi nell'ambiente. Il parsing sta in un punto
    solo, così `Main` resta thin.

## Punti risolti dall'utente — nessuno resta aperto

Erano i punti «da decidere» di questa SPEC. Sono chiusi, e le fasi che li aspettavano non sono più
bloccate.

1. **Nome del comando e porta.** Primo argomento `web` (assente → console), secondo argomento
   opzionale la porta, default `8080`. Una porta già occupata **fallisce con un messaggio
   esplicito**: non si cerca una porta libera in silenzio, perché l'indirizzo stampato smetterebbe di
   essere prevedibile.
2. **Aprire il browser da solo: no.** Si stampa soltanto l'URL. `Desktop.browse` non funziona in ogni
   ambiente e aggiungerebbe un effetto collaterale a `Main`.
3. **Ampiezza della UI: il minimo utile.** Schede dei combattenti, barre di vita e stamina a ogni
   passo, chi colpisce chi con cosa è accaduto, la procedura di fine scontro, l'esito. Restano
   **fuori dalla pagina** per ora — ma dentro la cronaca, quindi aggiungerli dopo è lavoro solo di
   frontend: il dettaglio d'iniziativa (`InitiativeReport` con i breakdown), le `Scorecard` della
   decisione ai punti, i `TurnHighlight` come marcatori, i punteggi di squadra.
4. **Aspetto: layout sobrio con barre.** Due colonne per i due schieramenti, barre di vita e stamina,
   timeline in basso, nessuna dipendenza grafica. **Non** si riproduce l'arte ASCII del terminale: era
   il limite che questa modalità esiste per superare.
5. **La derivazione del destino del loot si unifica su `HeroProgress`**, non si duplica nel mapper.
   Vedi la decisione 6 qui sopra: `HeroProgressFormatter` viene toccato, a output identico.
6. **In prospettiva la console diventerà anch'essa un lettore della cronaca.** Resta **fuori scope
   qui** — questo compito non rifà il percorso console — ma cambia un requisito della cronaca da
   subito: deve essere **autosufficiente**, cioè portare in forma di dati ogni informazione che oggi
   la console ricava per conto proprio dai suoi collaboratori (il numero e la descrizione della prova,
   la composizione degli schieramenti, l'accoppiamento del duello, l'esito, la procedura di fine
   scontro). Il criterio operativo: un futuro lettore di console non deve dover aggiungere campi alla
   cronaca, solo comporre frasi. Le **frasi** restano dei renderer — la cronaca non contiene stringhe
   di presentazione.
7. **Ricarica = partita nuova.** Una partita per richiesta, nessun identificatore di partita e nessuna
   cache lato server. Due schede del browser guardano due partite diverse: è coerente e accettato.
8. **I gioielli si mostrano nella fotografia d'ingresso e in quella di fine scontro, non nel roster
   dello scontro**, perché nello scontro non ci sono: il `Fighter` non li porta in campo (il motore non
   li monta — vedi `spec-equipment-buffs.md`, non ancora implementata).

## Rischi noti e accettati

- **Il JavaScript non è coperto da test automatici.** È una scelta dichiarata (nessun build step,
  nessun runner JS, nessuna dipendenza): il contratto verso il frontend è presidiato dal test sulle
  chiavi del JSON, il resto si verifica a mano. Il rischio è una regressione silenziosa nella pagina,
  accettato in cambio di zero infrastruttura.
- **I tipi di log del motore sono `SNAPSHOT` e affiorano nel JSON.** Mitigato dal test sulle chiavi,
  non eliminato: una modifica del motore può richiedere un adeguamento del frontend. È il prezzo
  scelto per non mantenere due volte la stessa forma di dati.
- **Superficie di `MatchRunner` e `Arena` toccata** per far posto alla presentazione sostituibile: è
  la parte del compito con più rischio di regressione sulla console, ed è per questo che nel piano
  viene prima di tutto il resto, a test verdi e con l'output di console come rete di sicurezza.
- **Una partita per richiesta** significa che due schede del browser aperte insieme guardano due
  partite diverse. È coerente con «ricarica = nuova partita», ma può sorprendere.
- **Il server non ha autenticazione.** Su loopback è accettato; se un giorno servisse esporlo, questa
  scelta va rifatta da zero.
- **La cronaca cresce con la lunghezza dello scontro** e viaggia tutta in una risposta. Per tre prove
  al massimo dei turni sono decine di kilobyte: irrilevante, e comunque il prezzo di non avere
  round-trip durante la riproduzione.

## Fuori scope

- Qualunque modifica a `fantasy-combat-system` e a `fantasy-game-toolkit`.
- Trasformare la console in un lettore della cronaca; unificare i due percorsi di presentazione.
- Riuso o generalizzazione di `BattleSceneRenderer` e `CombatScreenRenderer` per il web.
- Scelte del giocatore, in console o nel browser: la partita resta interamente decisa da `HeroBrain`.
  Nessun endpoint di scrittura, nessuna interazione oltre la riproduzione.
- Salvataggio della progressione, storico delle partite, condivisione di una partita per link,
  identificatori di partita, cache lato server.
- WebSocket, server-sent events, streaming: la cronaca viaggia in una risposta e basta.
- Autenticazione, HTTPS, esposizione fuori da loopback, deploy.
- Buff dell'equipaggiamento e caratteristiche efficaci: sono un altro compito
  (`spec-equipment-buffs.md`). Se quello arriva prima, la cronaca ne mostrerà i dati senza cambiare
  disegno.
- Rifacimento della presentazione di console, dell'arte ASCII o del layout delle schede.
- Arena a lunghezza variabile, modalità «tanti scontri in fila» e le altre voci di
  `daImplementare.md`.

## Definition of done

Criteri verificabili, ognuno coperto da almeno un test — tranne dove è dichiarato che la verifica è
in revisione.

1. `Arena.run()` restituisce la cronaca completa della corsa: l'ingresso del protagonista, una voce
   per ogni prova giocata (numero, descrizione, forma, roster, passi in ordine, esito) e la
   conclusione.
2. Per ogni prova vinta la cronaca porta i dati della procedura di fine scontro: oggetto trovato,
   destino del loot come dato fra gli otto possibili, eventuale oggetto lasciato, punti caratteristica
   per caratteristica, scheda del protagonista dopo la crescita.
3. Le schede della cronaca contengono solo dati immutabili (personaggio, arma, pezzi, rating, vita e
   stamina massime): il loro valore non dipende dall'istante in cui la cronaca viene letta, e in
   particolare non porta le ferite dello scontro appena giocato.
4. Dopo una prova non vinta la cronaca si chiude lì: nessuna voce per le prove non giocate, e la
   conclusione dice com'è finita e a quale prova.
5. La passata muta non scrive nulla su `System.out` e non legge nulla da `System.in`.
6. Modalità console invariata: stessa scansione, stesso output, stesse attese. I test preesistenti
   restano verdi, con il solo adeguamento di firma del doppio `ScriptedFights`.
7. La cronaca si serializza in JSON con `jackson-databind` senza annotazioni sui propri tipi, e il
   JSON contiene le chiavi che il frontend legge (verificato su una cronaca costruita a mano).
8. Nel JSON non compare nessun `Fighter`, nessun `Team`, nessun `Optional` e nessuno stato mutabile
   del motore.
9. `GET /api/chronicle` risponde `200` con `application/json; charset=utf-8` e un corpo coerente con
   la cronaca fornita; `GET /` risponde `200` con `text/html; charset=utf-8`; un percorso
   inesistente risponde `404`; un tentativo di uscire dalla cartella delle risorse risponde `404` e
   non legge nulla.
10. Il gestore della cronaca invoca il fornitore **una volta per richiesta**: due richieste
    producono due partite indipendenti e non condividono stato.
11. Il server accetta la porta come parametro (nei test una porta effimera) e si lega solo a
    loopback.
12. Selezione della modalità: senza argomenti si gioca in console come oggi; con l'argomento della
    modalità web si avvia il server, si stampa solo l'indirizzo e non si stampa nessuno scontro; un
    argomento non riconosciuto produce un errore esplicito che elenca le modalità ammesse.
13. La pagina carica la cronaca una volta sola e la riproduce con play, pausa, passo avanti, passo
    indietro, velocità e salto a un passo, senza altre richieste al server (verificato a mano: il
    JavaScript non ha test automatici, ed è una scelta dichiarata, non una dimenticanza).
14. La cronaca è **autosufficiente**: ogni dato che la console ricava oggi per conto proprio (numero e
    descrizione della prova, composizione degli schieramenti, accoppiamento del duello, esito,
    procedura di fine scontro) è presente in forma di dati, e nessuna stringa di presentazione è
    entrata nei suoi record (verificato in revisione, criterio di lettura: un futuro lettore di
    console dovrebbe comporre frasi, non aggiungere campi).
15. `CLAUDE.md`, `README.md` e `daImplementare.md` sono aggiornati: i nuovi package con la loro
    posizione nella direzione delle dipendenze, le due modalità di presentazione, la nuova dipendenza
    Jackson, e il fatto che `Arena.run()` ora restituisce una cronaca (verificato in revisione, non
    con un test).

## Esempio (istanza concreta — solo illustrativo)

```java
// combat.chronicle — soli dati, nessuna stringa di presentazione, nessuna annotazione Jackson.
public record ArenaChronicle(HeroSnapshot protagonist, List<TrialChronicle> trials, RunConclusion conclusion) {
}

public record TrialChronicle(int number, String description, TrialShape shape,
    List<CombatantSnapshot> roster, List<RoundLogEntry> rounds, List<TurnLogEntry> turns,
    RoundOutcome outcome, ProgressChronicle progress) {
}

/**
 * Il destino dell'unico oggetto trovato, come dato: il frontend fa uno switch, non deduce il caso
 * dalla presenza di un campo.
 */
public enum LootFate {
  WEAPON_TAKEN, WEAPON_DISCARDED,
  ARMOUR_WORN_ON_EMPTY_SLOT, ARMOUR_REPLACED, ARMOUR_DISCARDED,
  JEWEL_WORN_ON_EMPTY_TYPE, JEWEL_REPLACED, JEWEL_DISCARDED
}

// combat.MatchRunner — chiede l'esito al motore, lo consegna a chi lo deve mostrare, e lo
// restituisce a chi lo deve registrare.
public BattleResult playBattle(BattleSetup setup) {
  BattleResult result = combatSystem.battle(setup);
  presentation.presentBattle(setup, result);
  return result;
}

// combat.Arena — la scansione è quella di oggi; in più registra. Il rapporto di round porta già
// l'esito: qui si aggiunge la voce di cronaca.
public ArenaChronicle run() {
  Hero protagonist = enterTheArena();
  // ... le tre prove, invariate ...
  return chronicle.build();
}

// Il percorso muto passa dal costruttore con collaboratori espliciti che già esiste: nessuna
// porta d'ingresso nuova.
Arena silentArena = new Arena(FighterFactory.withDefaultRatings(settings), new HeroBrain(),
    new MatchRunner(settings, MatchPresentation.silent()),
    new MatchRunner(settings, MatchPresentation.silent()),
    ArenaLogger.silent(), TurnPacer.none());
ArenaChronicle chronicle = silentArena.run();

// combat.io.web — il fornitore è iniettato e invocato una volta per richiesta: ogni apertura della
// pagina è una partita nuova, e nessuno stato viaggia fra due richieste.
public ChronicleHandler(Supplier<ArenaChronicle> chronicles, ChronicleJson json) {
  this.chronicles = chronicles;
  this.json = json;
}

@Override
public void handle(HttpExchange exchange) throws IOException {
  byte[] body = json.toJson(chronicles.get()).getBytes(StandardCharsets.UTF_8);
  exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
  exchange.getResponseHeaders().set("Cache-Control", "no-store");
  exchange.sendResponseHeaders(HTTP_OK, body.length);   // byte, non caratteri
  try (OutputStream out = exchange.getResponseBody()) {
    out.write(body);
  }
}

// it.fantasyarena.Main — thin: un solo punto di parsing, due strade.
public static void main(String[] args) {
  CombatSettings settings = CombatSettings.defaults();
  UiMode mode = UiMode.fromArgs(args);

  switch (mode) {
    case CONSOLE -> new Arena(settings, ScreenRefresh.CLEAR).run();
    case WEB -> new ArenaWebServer(UiMode.portFrom(args), () -> silentArena(settings).run()).start();
  }
}
```

Forma del JSON servito da `GET /api/chronicle`, per fissare le chiavi che il frontend legge:

```
{ "protagonist": { "name": ..., "race": ..., "characteristics": [ { "characteristic": ..., "value": ... } ],
                   "weapon": {...}, "armour": [...], "jewels": [...] },
  "trials": [ { "number": 1, "description": ..., "shape": "BATTLE",
                "roster": [ { "rosterIndex": 0, "teamIndex": 0, "name": ..., "maxHealth": ..., ... } ],
                "rounds": [ { "roundNumber": 1, "turns": [ { "attackerIndex": 0, "targetIndex": 1,
                                                             "turn": { "description": ..., "action": {...} } } ],
                              "vitals": [...], "events": [...] } ],
                "turns": [],
                "outcome": "WON",
                "progress": { "found": {...}, "fate": "ARMOUR_REPLACED", "dropped": {...},
                              "gains": [...], "heroAfter": {...} } } ],
  "conclusion": { "triumph": true, "outcome": "WON", "lastTrial": 3 } }
```
