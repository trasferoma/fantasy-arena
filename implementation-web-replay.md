# IMPLEMENTATION — Interfaccia web come seconda modalità di presentazione

**Specifica di riferimento:** `spec-web-replay.md`  — nel resto del documento: «la SPEC».
**Stato:** `IN_PROGRESS`  <!-- NOT_STARTED | IN_PROGRESS | BLOCKED | COMPLETED -->
**Avanzamento:** Fasi 1-8, **10** e **9** concluse (la 10 era stata anticipata alla 9 su richiesta
dell'utente, così la pagina si prova dal punto d'ingresso vero). La Fase 9 è completa nel codice e
verificata funzionalmente; resta **in attesa del solo sguardo a schermo dell'utente** (criterio 13),
che nessuno strumento di questa sessione può sostituire. Il buco di autosufficienza trovato scrivendo
la pagina (i punti del gioiello) è stato **chiuso su decisione dell'utente**: la cronaca ha un campo in
più e la SPEC è aggiornata. **Fase 11 conclusa**: revisione in due passate, nessuna modifica di stile
applicata, e il codice morto che ha trovato è stato **rimosso su decisione dell'utente**. **Fase 12
conclusa**: i tre documenti sono aggiornati e i criteri 14 e 15 verificati sul codice. Nessun problema
aperto. **Fase 13 eseguita**: controlli strutturali passati, 143 test verdi, console e web provate da capo
a fondo. Manca **solo** lo sguardo dell'utente sulla pagina, e per questo lo stato non è `COMPLETED`.

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

**Fase 1 — Analisi (nessun codice)** — conclusa 2026-07-31
- [x] Confermati i punti del *Contesto* della SPEC: `new ConsoleCombatLogger()` a `MatchRunner:79` e
      `new ConsoleBattleLogger()` a `MatchRunner:123`, costruttore con collaboratori espliciti di
      `Arena` alle righe 91-99, `src/main/resources` assente, nessun `dependencyManagement` nel
      `pom.xml`.
- [x] Riverificato: la ricerca di `.state()` e `isDefeated()` in `src/main/java` trova **una sola**
      occorrenza, `Arena.java:186`. L'assunzione «fotografare le schede non cambia l'output» regge.
- [x] Consumatori censiti: `ConsoleArenaLogger` → campo e due costruttori di `Arena` più
      `ArenaTest:153`; `ConsoleBattleLogger` → solo `MatchRunner:123` e il proprio test;
      `CombatLogger` (già interfaccia) → campo di `MatchRunner` e `LinearCombatReplay`;
      `CombatReplay` → solo `MatchRunner`. Superficie da estrarre: piccola, nessun consumatore esterno
      a sorpresa.
- [x] Confermato: `CombatResult` e `BattleResult` **non** sono record (il vincitore è opzionale e vive
      come campo nullo) ma hanno costruttore pubblico che accetta `null` come vincitore e liste vuote.
      Il doppio `ScriptedFights` può fabbricarli.
- [x] `jackson-databind` **2.20.1** è già nel repository Maven locale: risolvibile anche offline, e
      `jackson-core`/`jackson-annotations` sono le sue sole transitive.
- [x] Stile dei test rilevato: solo `Assertions`, cattura di `System.out` in `ByteArrayOutputStream`
      (`ArenaTest`, `ConsoleBattleLoggerTest`), doppi per ereditarietà, dadi pilotati dal `test-jar`.
- [x] Baseline registrata: **102 test verdi** prima di ogni modifica. È il riferimento con cui
      confrontare ogni fase.
- [x] "File coinvolti (effettivi)" confermato, con l'aggiunta di `HeroProgress` e
      `HeroProgressFormatter` decisa insieme alla SPEC.
- [x] **Scostamento dal Contesto della SPEC rilevato e lasciato com'è**: `render` non è del tutto
      foglia. `CombatScreenRenderer:6` importa `ScreenCombatReplay` (da `replay`) e
      `HeroProgressFormatter:14` importa `ConsoleArenaLogger` (da `log`), entrambi **soltanto** per un
      `{@link}` di Javadoc: nessun uso nel codice. È pre-esistente e fuori scope; il vincolo operativo
      resta «non peggiorare», cioè nessun import nuovo in quella direzione, nemmeno per Javadoc.

**Fase 2 — Interfacce dei logger (console invariata)** — conclusa 2026-07-31
- [x] `ArenaLogger` estratta da `ConsoleArenaLogger` (cinque metodi) e `BattleLogger` da
      `ConsoleBattleLogger` (tre metodi), coi soli metodi già chiamati e il Javadoc dei metodi salito
      sull'interfaccia. Il commento di classe di `ConsoleBattleLogger` che spiegava perché *non* aveva
      un'interfaccia è stato rimosso: contraddiceva il codice.
- [x] `SilentArenaLogger`, `SilentBattleLogger`, `SilentCombatLogger`: implementazioni mute delle tre
      interfacce di log. **Nota aggiunta in Fase 11**: le ultime due sono state rimosse, perché la Fase 3
      le ha rese inutili introducendo `SilentMatchPresentation` e sono rimaste senza chiamanti. Resta solo
      `SilentArenaLogger`, che `SilentArenaRun` usa davvero.
- [x] `Arena`: campo e parametro del costruttore con collaboratori espliciti sono ora `ArenaLogger`;
      i costruttori di comodo passano ancora `new ConsoleArenaLogger()`.
- [x] `ConsoleBattleLogger` non si istanzia più dentro `playBattle`: vive come collaboratore, una sola
      istanza per l'intera corsa.
- [x] Criterio di completamento verificato: 102 test verdi, output di console invariato.

**Fase 3 — `MatchRunner` restituisce l'esito e delega la presentazione** — conclusa 2026-07-31
- [x] `playDuel` → `CombatResult`, `playBattle` → `BattleResult`.
- [x] `MatchPresentation` in `combat.io.replay` con `ConsoleMatchPresentation` (il codice di
      `MatchRunner` spostato di casa: logger, costruzione pigra del `CombatReplay`, `TurnPacer` unico
      condiviso dai due percorsi, `ScreenCleaner`, ciclo sui round, attesa di lettura degli
      schieramenti) e `SilentMatchPresentation`.
- [x] `MatchRunner` ridotto a due metodi simmetrici che chiedono l'esito al motore, lo consegnano alla
      presentazione e lo restituiscono; Javadoc di classe riscritto di conseguenza. Guadagna il
      costruttore `MatchRunner(CombatSettings, MatchPresentation)`; i quattro costruttori preesistenti
      restano utilizzabili con lo stesso comportamento.
- [x] `ArenaTest.ScriptedFights` adeguato alle nuove firme con due fabbriche private di risultati mai
      letti. Nessuna aspettativa di test cambiata.
- [x] Criterio di completamento verificato: 102 test verdi (identici alla baseline), ordine di stampa
      preservato in entrambi i percorsi.

**Fase 4 — I dati della cronaca** — conclusa 2026-07-31
- [x] Nuovo package `combat.chronicle`: `ArenaChronicle`, `TrialChronicle`, `TrialShape`,
      `HeroSnapshot`, `CombatantSnapshot`, `ItemSnapshot`, `ItemKind`, `ProgressChronicle`,
      `RunConclusion`, più il mapper. Nessuna annotazione Jackson, nessun import fra `combat.chronicle`
      e `combat.io` in nessuna direzione (i riferimenti incrociati sono solo `{@code}` di Javadoc).
- [x] `LootFate` (otto casi) e `HeroProgress.lootFate()` che li risolve in un punto solo;
      `HeroProgressFormatter.lootLine` è ora uno switch esaustivo sul destino già risolto, con un
      metodo per frase, al posto della catena di `isPresent()` su tre livelli. Output identico,
      `HeroProgressFormatterTest` non toccato e verde.
- [x] `ChronicleMapper`: unico punto di conversione `Fighter` → `CombatantSnapshot`, `Hero` →
      `HeroSnapshot`, `HeroProgress` → `ProgressChronicle`. Legge il destino, non lo rideriva.
- [x] Autosufficienza (criterio 14) coperta dai tipi: numero e descrizione della prova, roster,
      esito, procedura di fine scontro e conclusione sono tutti dati della cronaca. Nessuna stringa di
      presentazione nei record. La verifica finale in revisione resta alla Fase 13, quando `Arena` li
      popola davvero.
- [x] Javadoc di `ArenaChronicle` (perché la cronaca esiste: partita determinata all'avvio, la UI
      legge un registro) e di `ChronicleMapper` (perché fotografa invece di referenziare, citando
      `FighterProfile` come precedente in casa).
- [x] Test: `HeroProgressTest` (otto casi di `LootFate` costruiti a mano) e `ChronicleMapperTest`
      (fotografia del combattente senza vita corrente, gioielli solo nel protagonista, traduzione della
      procedura di fine scontro con e senza oggetto lasciato).
- [x] Criterio di completamento verificato: **114 test verdi** (102 di baseline + 12 nuovi); nessuno
      usa i tipi della cronaca fuori dal package e dai suoi test; output di console invariato.

**Fase 5 — `Arena` registra e restituisce la cronaca** — conclusa 2026-07-31
- [x] `Arena.run()` restituisce l'`ArenaChronicle`; la scansione delle tre prove è identica.
- [x] Registrati prova per prova numero, descrizione, forma, roster fotografato **prima** dello
      scontro, passi dal risultato del motore, `RoundOutcome` e procedura di fine scontro quando c'è;
      più l'ingresso del protagonista e la conclusione. `FightPlay` restituisce ora i passi
      (`TrialSteps`, record privato) invece di `void`, e `applyEndOfFightProcedure` restituisce lo
      `HeroProgress` invece del solo `Hero`.
- [x] Nessun `if` di gioco nuovo in `Arena`: la registrazione non decide niente.
- [x] Test in `ArenaTest`: criteri 1, 2, 3, 4 (quest'ultimo verificato sia sulla caduta sia sul
      pareggio). Il doppio `ScriptedFights` fabbrica ora un passo per scontro
      (`oneRoundBattleResult`/`oneTurnCombatResult`) invece di risultati vuoti, altrimenti nessun test
      poteva verificare che i passi finiscano nella lista giusta.
- [x] Criterio di completamento verificato: **119 test verdi**, console identica, nessuna aspettativa
      preesistente modificata.

**Fase 6 — La passata muta** — conclusa 2026-07-31
- [x] `SilentArenaRun` in `it.fantasyarena.combat`: implementa `Supplier<ArenaChronicle>`, custodisce
      solo i `CombatSettings` e a ogni `get()` assembla da zero fabbrica, cervello, i due `MatchRunner`
      con `SilentMatchPresentation`, il logger muto e `TurnPacer.none()`, passando dal costruttore con
      collaboratori espliciti che `Arena` già espone. Nessuna porta d'ingresso nuova su `Arena`.
- [x] `TurnPacer.none()`: pacer nominato che non attende e non tocca `System.in`. Serviva perché
      `EnterKeyTurnPacer.withoutHint()` tace il suggerimento ma continua a leggere l'INVIO.
- [x] Test (criterio 5): niente su `System.out` e niente letto da `System.in` (sostituito con uno
      stream che fallisce se qualcuno lo legge, ripristinato in `finally`), su un'arena assemblata nel
      test con collaboratori muti e pilotati; più un terzo test che passa dal percorso di produzione
      vero (`new SilentArenaRun(settings).get()`) e verifica insieme l'assenza di stampe e la
      completezza della cronaca — è il solo che esercita `SilentMatchPresentation` e i `MatchRunner`
      veri.
- [x] Criterio di completamento verificato: **122 test verdi**, criterio 5 verde, console invariata.

**Fase 7 — Serializzazione JSON** — conclusa 2026-07-31
- [x] `jackson-databind` **2.20.1** nel `pom.xml`, versione in una property accanto alle altre, nessun
      altro modulo Jackson (`jackson-core` e `jackson-annotations` restano transitive).
- [x] `ChronicleJson` in `combat.io.web`: un `ObjectMapper` costruito una volta e riusato, nessuna
      configurazione perché i due comportamenti richiesti sono già i predefiniti (null inclusi, enum
      come nomi) — constatato e documentato, non assunto. L'eccezione di serializzazione diventa un
      `IllegalStateException` con causa e messaggio, non viene silenziata.
- [x] Nessuna annotazione Jackson nei tipi di `combat.chronicle`; `ChronicleJson` importa solo Jackson e
      `combat.chronicle`; nessun import verso `combat.io.web` da fuori (verificato: l'unico riferimento
      esterno è un `{@code}` di Javadoc in `SilentArenaRun`).
- [x] Test su una cronaca costruita **a mano**, completa e realistica: chiavi presenti a ogni livello di
      annidamento (criterio 7) e assenza dei tipi mutabili del motore (criterio 8), asserita raccogliendo
      ricorsivamente i nomi di campo dell'albero JSON invece di cercare stringhe nel testo.
- [x] Verificato che `RunConclusion.triumph()` **non compare** nel JSON: non essendo un componente del
      record ma un accessor senza prefisso `get`/`is`, Jackson non lo riconosce come proprietà. C'è un
      test dedicato che lo constata, così un'eventuale comparsa futura non passerebbe inosservata.
- [x] Criterio di completamento verificato: **125 test verdi**, criteri 7 e 8 verdi.

**Fase 8 — Il server** — conclusa 2026-07-31
- [x] `ArenaWebServer` (loopback via `InetAddress.getLoopbackAddress()`, porta parametrica, `start()`,
      `stop()`, `address()`), `ChronicleHandler` e `StaticResourceHandler` — questi due package-private:
      sono dettagli che `ArenaWebServer` cabla, non hanno consumatori esterni.
- [x] Content type e `Cache-Control: no-store` come da SPEC; lunghezza del corpo in **byte UTF-8**,
      con il perché nel Javadoc (la narrazione del motore è accentata: la lunghezza in caratteri
      troncherebbe la risposta).
- [x] Risorse statiche da **lista chiusa** di tre chiavi letterali: il percorso della richiesta si
      confronta per uguaglianza, non si concatena mai in un nome di risorsa. Il traversal non è bloccato,
      è impossibile — testato su tre forme (`/../pom.xml`, `/%2e%2e/pom.xml`, `/../../pom.xml`).
- [x] `src/main/resources/web/` creata con `index.html`, `app.css`, `app.js` **segnaposto dichiarati**:
      servono adesso perché senza di essi il criterio 9 non è verificabile. La pagina vera è la Fase 9.
- [x] Verificato: `combat.io.web` importa solo JDK, `com.sun.net.httpserver`, Jackson e
      `combat.chronicle`. Nessun `render`/`log`/`terminal`/`replay`, e in particolare **non**
      `SilentArenaRun`: il fornitore arriva iniettato come `Supplier`.
- [x] Test con **porta effimera** (porta 0, poi si legge quella assegnata) e `HttpClient` del JDK,
      server fermato sempre: criteri 9, 10, 11, più la porta occupata, il sottopercorso sotto
      `/api/chronicle` e i metodi diversi da `GET`.
- [x] Criterio di completamento verificato: **134 test verdi**, criteri 9-11 verdi, nessuna porta fissa.

**Fase 9 — La pagina (HTML/CSS/JavaScript vanilla)** — conclusa 2026-07-31, salvo lo sguardo a schermo
- [x] `src/main/resources/web/` con `index.html`, `app.css`, `app.js` **veri** al posto dei segnaposto;
      `<meta charset="UTF-8">` nella pagina; nessuna libreria, nessuna CDN, nessun modulo, nessun build
      step. Verificato che i soli riferimenti esterni sono `/app.css` e `/app.js`, cioè due delle tre
      chiavi della lista chiusa del server: nessuna immagine, nessun font, nessuna favicon che
      prenderebbe un `404`.
- [x] Aspetto e ampiezza come deciso: `.battlefield` è una griglia a due colonne (`1fr 1fr`, che
      collassa a una sotto 720px) con una colonna per schieramento, barre di vita e stamina a `width`
      percentuale, controlli e timeline in una barra `sticky` in basso, nessuna arte ASCII. Contenuto al
      minimo utile; **verificato che iniziativa, highlight, scorecard e punteggi di squadra non sono
      letti dalla pagina** pur essendo nel JSON — l'unico campo d'iniziativa usato è `chosenName`, e
      solo nel duello, dove è il solo modo di sapere chi agisce.
- [x] Un solo `fetch`, all'apertura: verificato per lettura (`fetch` compare una volta, nessun
      `XMLHttpRequest`/`EventSource`/`WebSocket`/`import()`) e a esecuzione (il banco di verifica conta
      le chiamate e ne trova una sola anche a riproduzione finita, salti e cambi di velocità compresi).
- [x] Controlli: play, pausa, passo avanti, passo indietro, quattro velocità (0.5x-4x) e salto a un
      passo (timeline `<input type="range">`) o a una prova (un pulsante per prova). Ai bordi i pulsanti
      si disabilitano invece di non fare niente in silenzio; in play, arrivati alla fine, si ferma.
- [x] Le due forme di passo con **una sola** macchina di riproduzione: `buildMoments` appiattisce la
      cronaca in una lista di momenti indicizzabile, e le due viste diverse sono i soli
      `renderBattleStep` / `renderDuelStep`.
- [x] Fra una prova e l'altra il momento di procedura di fine scontro (una per prova vinta, con le otto
      frasi del destino del loot); in coda il momento di conclusione, trionfo o caduta.
- [x] Verificato che il testo della cronaca entra nel DOM solo via `textContent`: nessun `innerHTML`,
      `outerHTML`, `insertAdjacentHTML` né `eval` nel file.
- [x] **Verifica funzionale eseguita**, oltre alla lettura del codice: banco nello scratchpad (non nel
      repository) che esegue `app.js` in un contesto `vm` con un DOM finto e i due campioni JSON
      **reali** scaricati dal server, e pilota i controlli dagli eventi del DOM. Copre: un solo `fetch`;
      il numero di momenti atteso (passi + una progressione per prova vinta + la conclusione); un solo
      pannello visibile per momento; l'avanzamento su tutti i momenti; il ritorno indietro fino
      all'inizio con **confronto del testo reso momento per momento**; il salto dalla timeline che
      atterra sullo stesso testo del passo raggiunto a mano; un pulsante per prova che apre la prova
      giusta; il timer di play che avanza di un passo per battito; la pausa che non lascia timer attivi;
      la velocità scelta che diventa l'intervallo del timer; il ramo di ripiego con vitali mancanti; e
      il banner d'errore col codice HTTP quando la cronaca non si carica. **Verde su entrambi i
      campioni** (47 momenti su una corsa vinta con duello, 36 su una caduta alla seconda prova).
- [x] **Nessun test automatico sul JavaScript nel repository**: è la scelta dichiarata dalla SPEC
      (nessun build step, nessun runner JS), non una dimenticanza. Il banco di verifica qui sopra vive
      nello scratchpad di sessione e non entra nella suite: non è un test del progetto.
- [x] **Chiuso il buco di autosufficienza trovato in questa fase** (opzione (a) scelta dall'utente):
      `ProgressChronicle` guadagna `jewelBonusPoints`, `Integer` nullabile popolato da
      `ChronicleMapper.snapshotJewelBonusPoints` con lo stesso schema di `snapshotDropped` — switch
      esaustivo sul destino già risolto, senza `default`, otto casi elencati. Le due frasi del gioiello
      indossato in `app.js` ora dicono il numero, come fa la console. `HeroProgress`, `HeroBrain` e
      `HeroProgressFormatter` **non** sono stati toccati: il dato esisteva già, mancava solo la
      fotografia. **143 test verdi** (140 di baseline + 3 nuovi in `ChronicleMapperTest`), e verificato
      sul server vero che il campo compare col valore giusto: su una corsa vinta,
      `JEWEL_WORN_ON_EMPTY_TYPE` con bonus 4 e 7 punti totali di crescita, un'altra con bonus 3 e 6
      totali, `WEAPON_TAKEN` con campo nullo e 3 totali — cioè sempre i tre punti fissi della vittoria
      più il bonus, che è la prova che il campo è davvero quel numero e non un altro.
- [ ] **Resta solo la verifica a schermo** (criterio 13): nessuno strumento di automazione del browser
      è disponibile in questa sessione, quindi il giudizio sull'aspetto e la prova col pannello di rete
      aperto sono dell'utente. Il server è stato lasciato in ascolto su `http://127.0.0.1:8080/` e le
      quattro risposte sono state verificate (`/` → `200 text/html; charset=utf-8`, `/app.css` →
      `200 text/css`, `/app.js` → `200 text/javascript` con il file vero e non il segnaposto,
      `/api/chronicle` → `200 application/json`, un percorso ignoto → `404`).

**Fase 10 — La scelta della modalità** — conclusa 2026-07-31, **anticipata alla Fase 9** su richiesta
dell'utente, così la pagina si prova dal punto d'ingresso vero invece che da un avvio temporaneo.
- [x] `UiMode`: interfaccia con due record annidati, `ConsoleMode()` senza dati e `WebMode(int port)`.
      `fromArgs` è l'unico punto che legge gli argomenti; la porta è opzionale, default `8080`,
      validata fra 1 e 65535.
- [x] `Main` è due righe: costruisce i `CombatSettings` e chiama `UiMode.fromArgs(args).launch(settings)`.
      Non conosce più `Arena`, `ArenaWebServer`, `SilentArenaRun` né i due tipi concreti.
- [x] Errori espliciti: modalità non riconosciuta (con l'elenco di quelle ammesse), porta non numerica
      o fuori intervallo, porta occupata (l'eccezione di `ArenaWebServer` nomina già la porta). Nessun
      `try/catch` in `Main`: i messaggi sono frasi leggibili e la causa non va persa.
- [x] In modalità web su console va **solo** l'indirizzo.
- [x] Test del parsing (criterio 12): sei casi in `UiModeTest`.
- [x] **Verificato empiricamente** che la JVM resta viva dopo `main`: `mvn -o exec:java -Dexec.args="web"`
      lascia il processo in ascolto, stampa solo `http://127.0.0.1:8080/`, e `GET /` e `GET /api/chronicle`
      rispondono `200`. `HttpServer` senza esecutore esplicito usa un thread non-daemon: nessun intervento
      necessario. Riverificato dopo lo spostamento del codice di avvio dentro `WebMode.launch`.
- [x] Criterio di completamento verificato: **140 test verdi**, e senza argomenti il gioco si comporta
      esattamente come prima.
- [x] **Lacuna dichiarata, e resta tale**: nessun test lancia `Main`. In console giocherebbe una partita
      vera in attesa di INVIO, in web lascerebbe un server acceso nella suite. La parte del criterio 12
      «con `web` si avvia il server, si stampa solo l'indirizzo e non si stampa nessuno scontro» è
      verificata a mano, e **riverificata in Fase 9 con la pagina vera**: `mvn -o exec:java
      -Dexec.args="web"` stampa su console esattamente la riga `http://127.0.0.1:8080/` e nient'altro,
      nessuno scontro, e i quattro percorsi rispondono come da SPEC.

**Fase 11 — Revisione funzionale (`java-functional-evolver`)** — conclusa 2026-07-31
- [x] Revisione invocata sul Java prodotto, in **due passate disgiunte** invece di una: la superficie
      finale (una trentina di file fra produzione e test) in una sola passata sarebbe stata guardata
      male. Disgiunte perché nessuna delle due può modificare i file dell'altra, e **sequenziali** perché
      due `mvn` sullo stesso `target/` si darebbero fastidio. Passata A: progressione e cronaca (`Arena`,
      `MatchRunner`, `RoundOutcome`, tutto `combat.chronicle`, `HeroProgress`/`LootFate`, i logger e le
      presentazioni, `HeroProgressFormatter`, più `ArenaTest`, `ChronicleMapperTest`,
      `HeroProgressTest`). Passata B: confine web e ingresso (`combat.io.web`, `SilentArenaRun`,
      `UiMode`, `Main`, `TurnPacer`, più i loro quattro test).
- [x] **Nessuna modifica applicata da nessuna delle due passate**, ed è l'esito che ci si aspetta a
      questo punto: le fasi erano già state revisionate una per una man mano, e ognuna aveva applicato o
      respinto quello che c'era. A entrambe le passate è stato dato l'elenco di ciò che era già stato
      deciso e respinto, così da non far rilitigare le stesse proposte; entrambe l'hanno rispettato e
      hanno confermato le decisioni nel merito invece di limitarsi a non toccarle.
- [x] `mvn -o test` eseguito da entrambe le passate: **143 test verdi** ognuna. Nessuna modifica da
      ri-verificare.
- [x] Nessuna revisione funzionale sul JavaScript e sul CSS: non è Java. La pagina resta verificata dal
      banco a DOM finto della Fase 9.
- [x] **Due segnalazioni dalla vista d'insieme**, che è il motivo per cui questa fase esiste anche dopo
      le revisioni di ogni fase. Entrambe correttamente **segnalate e non corrette** dall'agente, essendo
      pulizia di codice e non stile funzionale:
      **(a) `SilentBattleLogger` e `SilentCombatLogger` sono codice morto.** Verificato da me su tutto
      `src`: **zero** riferimenti, nemmeno in un `{@link}`. Li ha creati la Fase 2, e la Fase 3 li ha resi
      inutili introducendo `SilentMatchPresentation`, che implementa `presentDuel`/`presentBattle` con
      corpi vuoti invece di comporre due logger muti. Solo `SilentArenaLogger` ha ancora un chiamante
      vero (`SilentArenaRun`). È esattamente il tipo di residuo che si vede solo a compito finito, e
      nasce dalla sovrapposizione di due fasi entrambe previste dalla SPEC. **Rimosse su decisione
      dell'utente**, con la frase della SPEC corretta di conseguenza; le due interfacce restano.
      **(b) La riga commentata in `UiMode.ConsoleMode.launch`**
      (`// ScreenRefresh screenRefresh = new CombatSetupPrompt().askScreenRefresh();`) è debito
      **preesistente**, non nato qui: verificato con `git show HEAD:.../Main.java` che stava già in
      `Main` prima del compito ed è stata traslocata verbatim in Fase 10. Fuori scope, si lascia com'è.

**Fase 12 — Documentazione (nessun codice di produzione)** — conclusa 2026-07-31
- [x] `CLAUDE.md`: descrizione del progetto riscritta (non è più «applicazione console»: due modalità e il
      fatto che le regge — nessuna scelta del giocatore, quindi la partita è determinata all'avvio);
      `Main`/`UiMode` al posto di «apre `Arena`»; `Arena.run()` che restituisce la cronaca e la costruisce
      **sempre**; `MatchRunner` che restituisce l'esito e delega a `MatchPresentation`; i due package nuovi
      nella tabella (`combat.chronicle` come partita in forma di dati, `combat.io.web` come pozzo);
      aggiornate le righe di `combat`, `combat.io.log` e `combat.io.replay`; i comandi con `-Dexec.args`
      per la modalità web e il comportamento su porta occupata; Jackson accanto alle dipendenze critiche
      **con la differenza dichiarata che è su Maven Central** e il divieto di moduli aggiuntivi; quattro
      vincoli architetturali nuovi (il pozzo `web`, la cronaca di soli dati e autosufficiente, la lista
      chiusa delle risorse, la presentazione come collaboratore e non flag); la sezione test con le tre
      regole del confine web e le due lacune dichiarate; le convenzioni su switch esaustivi e sulla pagina
      vanilla; i due documenti `spec-`/`implementation-` fra la documentazione del repo.
- [x] `CLAUDE.md`: annotata anche la **trappola della JVM che sopravvive a Maven** e tiene la porta, che
      in questa sessione è costata una verifica falsa. Non è documentazione d'architettura, ma è
      esattamente il genere di cosa che chi prova la modalità web a mano ritroverà.
- [x] `README.md`: «gioco da guardare» invece di «gioco da console», e i due modi di guardarlo; nuova
      sezione «La stessa partita, nel browser» che spiega **perché** è possibile (nessuna scelta del
      giocatore → partita determinata all'avvio → la pagina legge una cronaca già scritta) e cosa ne
      segue (la cronaca costruita sempre in entrambe le modalità, la partita giocata in silenzio, una
      partita per apertura); il diagramma «com'è organizzato dentro» con la cronaca e i due lettori che si
      ignorano a vicenda, più il secondo confine fra il giocare e il mostrare; i comandi di lancio;
      «quattro cose» invece di tre, perché la progressione era assente dall'elenco.
- [x] `README.md`: **corrette tre affermazioni non più vere**, che non riguardano questo compito ma che
      stavano nei paragrafi che stavo toccando — vedi *Decisioni*.
- [x] `daImplementare.md`: riscritte tre voci che questo compito rende diverse (il salvataggio, che ha
      già nella cronaca il suo formato naturale; le scelte guidate dall'utente, che ora costano di più e
      **romperebbero** il fatto su cui poggia il disegno; i tanti scontri in fila, che sono vicinissimi
      perché `SilentArenaRun` fa già la parte difficile) e aggiunte tre voci nuove: la
      console-lettrice-della-cronaca, quello che la cronaca contiene e la pagina non mostra (iniziativa,
      scorecard, highlight, punteggi di squadra — lavoro di solo frontend), e i test automatici della
      pagina come decisione da rifare se la pagina cresce.
- [x] **Criterio 15 verificato**: i tre documenti dicono i package nuovi con la loro posizione nella
      direzione delle dipendenze, le due modalità e come si scelgono, la dipendenza Jackson e il fatto che
      `Arena.run()` restituisce una cronaca. Le affermazioni architetturali scritte in `CLAUDE.md` sono
      state **verificate sul codice**, non asserite: `combat.io.web` importa solo JDK,
      `com.sun.net.httpserver`, Jackson e `combat.chronicle`; l'unico importatore di `combat.io.web` in
      tutto `src/main` è `UiMode` (`SilentArenaRun` lo cita solo in un `{@code}` di Javadoc, senza
      import); `combat.chronicle` non importa né Jackson né `combat.io`; `com.fasterxml` non compare in
      nessun package fuori da `combat.io.web`.
- [x] **Criterio 14 verificato**, voce per voce: numero e descrizione della prova (`TrialChronicle`),
      composizione degli schieramenti e accoppiamento del duello (`roster` con `rosterIndex`/`teamIndex`,
      che nel duello sono i due contendenti), esito (`outcome`), procedura di fine scontro
      (`ProgressChronicle`, completa dei punti del gioiello dopo la correzione della Fase 9). Un lettore
      nuovo comporrebbe frasi senza aggiungere campi. Le stringhe nei record **propri** della cronaca sono
      tre e nessuna è una frase composta da un renderer: i due `name` di combattente ed eroe,
      `ItemSnapshot.name` (il nome della costante enum) e `TrialChronicle.description`. Su quest'ultima
      c'è una tensione residua, registrata in *Decisioni* e non risolta qui.

**Fase 13 — Revisione finale** — eseguita 2026-07-31
- [x] **Nessuna regola di combattimento nata qui**: la ricerca in `src/main/java` di aritmetica su
      danno, vita, stamina, iniziativa e momentum (assegnamenti composti e sottrazioni sui campi
      corrispondenti, esclusi i calcoli di percentuale delle barre, che sono presentazione) non trova
      **niente**. I numeri arrivano già calcolati dal motore.
- [x] **Nessun import proibito in `combat.io.web`**: nessun `render`, `log`, `terminal`, `replay` né
      `SilentArenaRun`. Nell'altra direzione, l'unico importatore di `combat.io.web` in tutto `src/main`
      è `UiMode`, che è l'ingresso.
- [x] **Nessuno stato mutabile del motore nei record della cronaca**: `Fighter` compare in
      `combat.chronicle` in un solo punto, come **parametro** di `ChronicleMapper.snapshotCombatant`,
      che è precisamente il suo mestiere — fotografarlo. Nessun record lo custodisce, e nelle loro firme
      non compaiono `Team`, `BattleSetup`, `FighterState` né `Optional`. Il criterio 8 è coperto anche da
      un test sul JSON prodotto.
- [x] **Casualità al suo posto**: nessun `Math.random()` né `new Random()` fuori da `FighterFactory` e
      `HeroBrain`, che è la deroga già dichiarata.
- [x] **Nessuna modifica non richiesta**: il `pom.xml` cambia di **due soli blocchi**, la property di
      versione e la dipendenza `jackson-databind` col commento che spiega perché `jackson-core` e
      `jackson-annotations` non si dichiarano. Ogni altro file toccato o creato corrisponde a una voce di
      *File coinvolti*. In Fase 11 sono state **rimosse** due classi che il compito aveva creato e reso
      inutili da sé.
- [x] **Stringhe di presentazione nella cronaca**: nessuna frase composta da un renderer. Resta la
      tensione su `TrialChronicle.description`, verificata in Fase 12 e registrata in *Decisioni*: è
      prosa italiana, ma è **richiesta dalla SPEC** fra i dati della cronaca, e non si tocca qui.
- [x] `mvn -o test` · **143 test verdi**.
- [x] **Modalità console provata da capo a fondo**, non solo dai test: `mvn -o exec:java` con `stdin`
      chiuso — il `TurnPacer` su EOF prosegue senza bloccarsi, quindi una corsa intera si gioca senza
      interazione. **Trentaquattro corse**, tutte con uscita `0` e **zero eccezioni**, da 223 a 4259 righe.
      Ne sono servite tante perché la seconda prova, due contro uno, è il muro: la trentaquattresima è la
      prima ad arrivare in fondo, ed è quella che conta, perché è la sola che esercita **tutti** i percorsi
      di console in un colpo — battaglia NvN alla prima e alla seconda prova, procedura di fine scontro
      dopo entrambe, e alla terza il **duello a schermate** («=== Esito del duello === Vince: Draconis (14
      turni)»). Le altre trentatré si fermavano prima. Due controlli aritmetici passati per strada: un
      gioiello da +1 con crescita di quattro punti totali e uno da +2 con crescita di cinque, cioè sempre
      i tre punti fissi della vittoria più il bonus — lo stesso numero che la cronaca ora porta come dato.
- [x] **Modalità web provata** su un server riavviato dalle classi correnti: le quattro risposte coi loro
      content type esatti, `404` sia su un percorso ignoto sia su un tentativo di traversal
      (`/../pom.xml`), `405` su un `POST` a un percorso noto, e sulla console **solo** l'indirizzo. Più la
      prova di «una partita per richiesta»: due `GET /api/chronicle` di fila restituiscono due partite
      **diverse**.
- [x] **Criteri della *Definition of done* ripercorsi uno per uno**: 1-5 coperti dai test di
      `ArenaTest`, `ChronicleMapperTest`, `HeroProgressTest` e `SilentArenaRunTest`; 6 dalla suite più le
      undici corse di console; 7-8 da `ChronicleJsonTest`; 9-11 da `ArenaWebServerTest` su porta
      effimera; 12 da `UiModeTest` più la prova a mano, con la lacuna dichiarata che nessun test lancia
      `Main`; 14 e 15 verificati in Fase 12. **Il 13 è l'unico non chiuso**, e solo nella sua parte
      visiva.
- [ ] **Manca soltanto lo sguardo a schermo dell'utente** (criterio 13, parte visiva): giudizio
      sull'aspetto e conferma col pannello di rete che dopo il caricamento non parte nessun'altra
      richiesta. Nessuno strumento di automazione del browser è disponibile in questa sessione, e la
      SPEC dichiara questa verifica manuale in partenza. Il comportamento della riproduzione è comunque
      già verificato eseguendo `app.js` su un DOM finto (Fase 9). **Fino ad allora lo stato resta
      `IN_PROGRESS`**: spuntare questa voce senza che nessuno abbia guardato sarebbe esattamente ciò che
      le regole di questo documento vietano.

## File coinvolti (effettivi)

Pre-compilati in via **provvisoria** dall'analisi della SPEC: da confermare e completare in Fase 1.
I nomi dei tipi nuovi sono proposte, non vincoli: quello che vincola è il ruolo.

**Presentazione sostituibile (Fasi 2-3)**
- `src/main/java/it/fantasyarena/combat/io/log/ArenaLogger.java` — nuovo: interfaccia estratta
- `src/main/java/it/fantasyarena/combat/io/log/ConsoleArenaLogger.java` — implementa l'interfaccia
- `src/main/java/it/fantasyarena/combat/io/log/BattleLogger.java` — nuovo: interfaccia estratta
- `src/main/java/it/fantasyarena/combat/io/log/ConsoleBattleLogger.java` — implementa l'interfaccia
- `src/main/java/it/fantasyarena/combat/io/log/SilentArenaLogger.java` — l'unica implementazione muta
  rimasta: le altre due, scritte in Fase 2, sono state rimosse in Fase 11 perché senza chiamanti
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
  - 2026-07-31 · Fase 9: rimosso dal *Piano operativo* un **blocco duplicato della Fase 3**, rimasto
    nella sua forma originale con le spunte vuote sotto la copia conclusa · il documento diceva insieme
    che la fase era fatta e che non lo era, e questo file è il posto da cui si riprende il lavoro: una
    contraddizione lì costa più di quanto valga la prudenza di non toccare le fasi passate. Nessuna voce
    di contenuto persa: la copia conclusa dice tutto quello che diceva l'altra, e in più com'è andata ·
    nessuno sul codice.
  - 2026-07-31 · Fase 12: **tensione residua sul criterio 14, verificata e non risolta.**
    `TrialChronicle.description` è l'unico campo dei record propri della cronaca che porta **prosa
    italiana**: le tre costanti di `Arena` («il primo avversario», «due contro uno», «lo sfidante
    speculare, armato meglio»). La SPEC la chiede esplicitamente fra i dati della cronaca («numero e
    descrizione della prova»), quindi è codice richiesto e non una svista; ma è anche l'unico posto dove
    la cronaca decide *come si dice* una cosa invece di *cos'è*, mentre per tutto il resto usa enum
    (`LootFate`, `RoundOutcome`, `TrialShape`) e lascia le parole a chi legge. La forma coerente sarebbe
    un `TrialKind` con la frase nei renderer — che è anche l'unica strada se un giorno servisse una
    seconda lingua. **Non cambiata qui**: toccherebbe la forma del JSON che la SPEC documenta come
    verificata e non serve a nessun criterio di questo compito. Va valutata insieme alla
    console-lettrice-della-cronaca, dove diventerebbe naturale · nessuno sul comportamento.
  - 2026-07-31 · Fase 12: nel `README.md` sono state **corrette tre affermazioni non più vere**, tutte
    precedenti a questo compito · erano nei paragrafi che stavo comunque riscrivendo, e un `README` che
    mente è peggio di uno più corto: (a) diceva che il protagonista «può sostituire la sua arma con quella
    di un avversario caduto» e «raccoglie i pezzi d'armatura», mentre il loot non si saccheggia dai caduti
    da quando è stato cambiato il meccanismo — se ne genera **uno** per prova vinta; (b) l'esempio della
    procedura di fine scontro mostrava righe che `HeroProgressFormatter` non stampa più in quella forma,
    sostituito con l'output reale; (c) l'elenco «non c'è ancora» dichiarava assente la **progressione**,
    che esiste. Sono correzioni di prosa e non toccano codice né criteri, quindi procedute e annotate qui
    invece di aperte come problema · nessuno sul comportamento.
  - 2026-07-31 · Fase 9: la forma del JSON che il frontend legge è stata **ricavata dal server vero**,
    non dedotta dai record Java né dall'esempio della SPEC · un `GET /api/chronicle` su un'istanza in
    ascolto dice anche quello che i tipi non dicono: quali chiavi Jackson produce davvero per i record
    di log del motore, dove arrivano i `null`, e che `triumph` non compare. Sono servite 21 partite per
    ottenerne una che arrivasse alla terza prova, cioè al **duello**: senza quel campione la seconda
    vista sarebbe stata scritta al buio · nessuno sul comportamento, ma i due campioni sono la base di
    ogni chiave letta dalla pagina.
  - 2026-07-31 · Fase 9: l'unità di avanzamento è il **momento**, non il passo di scontro · i passi da
    soli non bastano a raccontare la corsa: fra due prove va mostrata la procedura di fine scontro e in
    coda la conclusione, e trattarle come momenti nella stessa lista piatta le rende raggiungibili dagli
    stessi controlli invece di aggiungere due stati fuori banda alla macchina di riproduzione. Avanti,
    indietro e salto restano indicizzazione di un array · nessuno sul comportamento atteso.
  - 2026-07-31 · Fase 9: le vitali si allineano al roster **per posizione**, col nome come solo ripiego ·
    verificato sui campioni reali che `vitals` ha la stessa lunghezza e lo stesso ordine del roster in
    entrambe le forme di passo e anche per i caduti (`currentHealth: 0` resta in lista); il nome non può
    essere il criterio primario perché il motore lo dichiara inaffidabile come identificatore, due
    combattenti generati possono chiamarsi allo stesso modo · nessuno sul comportamento.
  - 2026-07-31 · Fase 9: **difetto corretto** — il ripiego per nome di `alignVitalsToRoster` usava
    `Array.find`, che restituisce `undefined` quando il nome non c'è, e quell'`undefined` arrivava a
    `buildCombatantCard` dove `vital.currentHealth` avrebbe sollevato un `TypeError` spegnendo la pagina
    intera. Il ramo che esiste *per difendersi* da una divergenza futura era quindi proprio quello che
    la rendeva fatale. Ora il ripiego normalizza a `null` e `appendVitalsBars` disegna comunque la
    scheda, sostituendo le barre con un'indicazione che quel dato non c'è: non si inventano valori
    pieni, che sarebbero un dato che mente · nessuno sul comportamento osservabile con i dati veri, è la
    correzione di un difetto latente.
  - 2026-07-31 · Fase 9: **difetto corretto** — un salto non metteva in pausa, quindi trascinando la
    timeline durante la riproduzione il timer di `play` continuava ad avanzare e superava subito la
    posizione appena scelta: l'utente non riusciva a fermarsi dove voleva. La pausa è stata messa nello
    stato della riproduzione e non nel gestore del DOM (`stopPlaybackTimer` + `jumpToIndex`), così
    timeline e pulsanti di prova la condividono invece di duplicarla, e `goToIndex` non è più esposto ·
    cambio di comportamento della sola pagina, voluto: un salto è l'azione con cui l'utente prende il
    comando.
  - 2026-07-31 · Fase 9: le frasi della pagina sono **sue**, non riusate dal Java · `HeroProgressFormatter`
    resta il narratore della console e vive in `combat.io.render`, che la pagina non può e non deve
    conoscere: la cronaca non porta stringhe di presentazione proprio perché ogni lettore compone le
    proprie. Le otto frasi del destino del loot sono state scritte nel registro di quel formatter, in un
    punto solo del file (`LOOT_FATE_MESSAGES`), e ognuna legge soltanto i campi che il suo destino
    garantisce — verificato su `ChronicleMapper.snapshotDropped` che `dropped` è non-nullo esattamente
    per i tre destini di rimpiazzo (`WEAPON_TAKEN`, `ARMOUR_REPLACED`, `JEWEL_REPLACED`) · nessuno sul
    comportamento della console.
  - 2026-07-31 · Fase 9: `jewelBonusPoints` è un **`Integer` nullabile**, non un `int` a zero né un
    `Optional` · un `Optional` in un record della cronaca romperebbe la serializzazione, perché la SPEC
    vieta il modulo `jackson-datatype-jdk8` e la regola dichiarata è «se nel JSON servisse un `Optional`,
    la risposta è cambiare il dato»; uno zero direbbe «vale zero punti» invece di «qui non c'è un bonus»,
    che è un'altra cosa. Nel package il `null` ha già esattamente questo significato (`dropped` quando non
    si lascia niente, `power` di un gioiello) · nessuno sul comportamento, e la chiave resta sempre
    presente nel JSON come le altre nullabili.
  - 2026-07-31 · Fase 9: il campo si chiama `jewelBonusPoints` e non `lootBonusPoints` · il bonus esiste
    solo per il gioiello, e un nome generico prometterebbe che un giorno arma e armatura potrebbero
    portarne uno; `HeroBrain` chiama già il concetto `jewelBonusPointsOf` · nessuno sul comportamento.
  - 2026-07-31 · Fase 9: **trappola operativa da ricordare per la Fase 13** — `TaskStop` su un
    `mvn exec:java` ferma Maven ma **non** la JVM che ha generato, che resta in ascolto e continua a
    tenere la porta. È costato una verifica falsa: dopo aver aggiunto il campo, il JSON sembrava non
    averlo, perché a rispondere era ancora il processo vecchio con le classi di prima, mentre il server
    nuovo era morto con «porta già in uso» in un log che non stavo guardando. Prima di credere a una
    verifica sul server, controllare **chi** ascolta sulla porta (`netstat -ano | grep 8080`) e chiuderlo
    per PID. L'episodio riconferma però un requisito: la porta occupata **fallisce con un messaggio
    esplicito** che nomina la porta, come la SPEC chiede · nessuno sul codice.
  - 2026-07-31 · Prefisso `Silent` per le implementazioni mute (`SilentArenaLogger`,
    `SilentBattleLogger`, `SilentCombatLogger`, `SilentMatchPresentation`) · coerente col vocabolario
    già usato dalla SPEC («passata muta») · nessuno sul comportamento.
  - 2026-07-31 · In `ConsoleMatchPresentation.presentBattle` il `TurnPacer` si ottiene *dopo*
    `reportSetup`, mentre in `MatchRunner.playBattle` lo si otteneva prima · verificato che il
    costruttore di `EnterKeyTurnPacer` non stampa niente (il suggerimento esce da `showHintOnce`, alla
    prima attesa) · nessuno sull'output.
  - 2026-07-31 · Fase 10: la modalità è modellata a **due tipi distinti** invece che con un record a
    porta facoltativa o un enum · un `port` valorizzato a 8080 mentre si gioca in console sarebbe un dato
    che mente, e le costanti di un enum non possono portare un valore che cambia a ogni esecuzione. Senza
    sealed type la chiusura dei casi resta documentale · nessuno sul comportamento.
  - 2026-07-31 · Fase 10: **difetto corretto** — la prima versione distingueva i due casi in `Main` con
    `instanceof` più cast, l'unico `instanceof` di tutto il repository, mentre `CLAUDE.md` dichiara che
    qui non si discrimina un tipo a runtime. Sostituito con dispatch polimorfico: `UiMode.launch` è
    implementato dai due record, e `Main` chiede alla modalità di avviarsi invece di interrogarne il tipo.
    Senza sealed type quell'`if/else` era anche un controllo che nessuno verificava · nessuno sul
    comportamento osservabile.
  - 2026-07-31 · Fase 8: esecutore predefinito dell'`HttpServer`, quindi richieste **seriali** · una
    partita dura millisecondi e il gioco è a uso locale: un pool di thread sarebbe complessità senza
    motivo, e la serialità rende banalmente vera la garanzia «una partita per richiesta» · nessuno sul
    comportamento atteso.
  - 2026-07-31 · Fase 8: un metodo diverso da `GET` risponde `405` su un percorso della lista chiusa e
    `404` su un percorso ignoto · il `405` dice «il percorso esiste, il metodo no»: usarlo anche sui
    percorsi inesistenti rivelerebbe una distinzione che non c'è. La SPEC non fissava questo punto ·
    scelta di comportamento, documentata nel Javadoc dei due gestori.
  - 2026-07-31 · Fase 8: **due difetti trovati in revisione e corretti**, entrambi nell'ordine dei
    controlli dei gestori. (a) `createContext` di `HttpServer` fa match per **prefisso**, e
    `ChronicleHandler` non confrontava il percorso: `GET /api/chronicle/qualunque-cosa` rispondeva `200`
    **e giocava una partita intera**. Ora il percorso si confronta per uguaglianza prima di invocare il
    fornitore, e il test asserisce anche che il fornitore non venga invocato. (b) `StaticResourceHandler`
    controllava il metodo prima del percorso, e siccome è registrato su `/` — il ripiego di tutto —
    `POST /percorso-inesistente` rispondeva `405` invece di `404`. Ordine invertito e test aggiunto con
    le due asserzioni che insieme dicono la regola. Il secondo l'ha trovato `java-functional-evolver`
    senza cercarlo, e correttamente non l'ha corretto da sé: era un cambio di comportamento osservabile.
  - 2026-07-31 · Fase 7: nessuna configurazione dell'`ObjectMapper` · i due comportamenti che la cronaca
    richiede — null inclusi nel JSON, enum come nomi — sono già i predefiniti di Jackson: configurarli
    esplicitamente darebbe l'impressione di una scelta reversibile, mentre il punto è che sono ciò che
    serve. Documentato nel Javadoc perché non sembri una dimenticanza · nessuno sul comportamento.
  - 2026-07-31 · Fase 7: il JSON diverge in due punti dall'esempio con cui la SPEC era stata scritta —
    la chiave è `armourPieces` e non `armour` (il JSON segue il nome che il codice usa già), e `triumph`
    non compare nella conclusione (accessor derivato, non componente del record: Jackson non lo vede) ·
    la SPEC è stata aggiornata con la forma verificata e con la spiegazione di entrambi gli scostamenti
    · il frontend deriva il trionfo da `outcome`.
  - 2026-07-31 · Fase 7: rinominato il test `triumphNonCompareNelJson...` per togliere le lettere
    accentate dal nome del metodo · era l'unico nome di metodo non-ASCII di tutta la suite · nessuno sul
    comportamento.
  - 2026-07-31 · Fase 6: `SilentArenaRun` vive in `it.fantasyarena.combat`, non in `combat.chronicle`
    né in `combat.io.web` · l'assemblaggio muto ha bisogno di `combat.io.log`, `combat.io.replay` e
    `combat.io.terminal`: in `chronicle` violerebbe il divieto di dipendere da `combat.io`, in `web`
    violerebbe il vincolo del pozzo che non importa `render`/`log`/`terminal`/`replay`. `combat` è
    l'unico posto che può conoscere entrambi i mondi · nessuno sul comportamento.
  - 2026-07-31 · Fase 6: `SilentArenaRun` ha **un solo costruttore** e ricostruisce i collaboratori a
    ogni `get()` · la prima versione li teneva come campi, e così la stessa `FighterFactory` — col suo
    `usedNames` — sarebbe stata condivisa da tutte le partite prodotte da un'istanza: col server della
    Fase 8 i nomi si accumulerebbero per la vita del processo e due richieste concorrenti si
    contenderebbero quello stato. Il costruttore con collaboratori espliciti è stato **rimosso**: i
    test deterministici assemblano l'`Arena` da sé, così la classe ha un solo contratto onesto invece
    di due, di cui uno una trappola · nessuno sul comportamento osservabile, ma è la correzione di un
    difetto vero.
  - 2026-07-31 · Fase 6: `TurnPacer.none()` è un factory statico sull'interfaccia, non una classe
    nominata · zero stato e un solo comportamento possibile: è l'idioma di `Function.identity()`, e una
    classe per «non fare niente» sarebbe cerimonia. Confermato dalla revisione funzionale · nessuno sul
    comportamento.
  - 2026-07-31 · Fase 5: l'accumulo delle voci di cronaca vive nel `RoundReport` che si concatena, non
    in un campo di `Arena` · un campo lista resterebbe sporco fra due `run()` sulla stessa istanza, e la
    Fase 8 costruirà una partita per richiesta HTTP: è un bug vero, non un'ipotesi. `andThen` passa ora
    l'intero rapporto invece del solo `Hero`, e `wonTrial`/`lostTrial` costruiscono il rapporto
    successivo senza mai mutare la lista che portavano · nessuno sul comportamento.
  - 2026-07-31 · Fase 5: la costruzione della cronaca resta dentro `Arena`, nessun builder in
    `combat.chronicle` · è `Arena` a conoscere la scansione, e con `TrialSteps` e `chronicleOf` ci sta
    senza appesantirla · nessuno sul comportamento.
  - 2026-07-31 · `LootFate` vive in `combat.hero`, non in `combat.chronicle` · la direzione naturale è
    che `chronicle` dipenda da `hero` (deve leggere `Hero` e `HeroProgress`), non il contrario ·
    nessuno sul comportamento.
  - 2026-07-31 · `ItemSnapshot` è `(ItemKind kind, String name, Rarity rarity, Integer power)`: un
    solo campo `String name` col nome della costante, non tre campi enum nullabili · tre enum del
    toolkit senza supertipo comune avrebbero portato due campi nulli per ogni oggetto di ogni roster,
    cioè la stessa forma che questa fase toglie a `HeroProgress`; verificato sui documenti del toolkit
    (`core.md`) che `Weapon`, `Armour` e `Jewel` sono enum di sole costanti senza `toString`
    ridefinito, quindi il nome è già il testo che la console stampa · nessuno sul comportamento.
  - 2026-07-31 · `RunConclusion` non custodisce `triumph`: è un accessor derivato da `outcome` · una
    prova vinta apre sempre la successiva, quindi la corsa si chiude con `WON` solo dopo l'ultima:
    tenerlo come componente accanto alla sua fonte sarebbe un invariante che regge sulla disciplina di
    chi costruisce il record · il campo non compare nel JSON, il frontend lo deriva da `outcome`.
  - 2026-07-31 · Gli switch sul `LootFate` nel mapper sono **esaustivi, senza `default`** · una nona
    costante deve diventare un errore di compilazione, non un `null` silenzioso al frontend · nessuno
    sul comportamento.
  - 2026-07-31 · La discriminazione a tre vie sul contenuto del `Loot` vive **solo** in
    `HeroProgress.lootFate()`: il mapper estrae l'oggetto trovato guidato dal destino già risolto
    (`snapshotFound`), non ri-ispeziona quale dei tre campi è valorizzato · `Loot` **non** è stato
    toccato: un discriminatore lì sposterebbe l'invariante a monte su un tipo pubblico, e lo switch sul
    destino risolve la duplicazione senza quel cambio · nessuno sul comportamento.
  - 2026-07-31 · Il costruttore `MatchRunner(CombatSystem, MatchPresentation)` è rimasto **privato**:
    nessun chiamante attuale lo richiede · **da riaprire in Fase 5 o 6**, dove serve esattamente la
    combinazione «dadi pilotati + presentazione muta» per una cronaca riproducibile senza stampe. È una
    riga, ma va ricordata.
- **Deviazioni dalla SPEC** (da motivare) — `Descrizione · Motivazione · Impatto · Aggiorna la SPEC?
  sì/no`: nessuna.
- **Problemi aperti** (bloccano l'avanzamento) — `Descrizione · Impatto · Opzioni · Decisione
  richiesta`: **nessuno aperto.** 2026-07-31 · gli otto punti «da decidere» della SPEC sono stati risolti
  dall'utente prima dell'inizio del lavoro; la SPEC li riporta chiusi.
  - 2026-07-31 · Fase 11 · **RISOLTO** con l'opzione (a), scelta dall'utente: le due classi sono state
    **rimosse** e la frase della SPEC corretta — l'implementazione muta serve solo ad `ArenaLogger`, e per
    duello e battaglia il silenzio lo produce `MatchPresentation` nella sua forma muta. Le due interfacce
    `BattleLogger` e `CombatLogger` **non** sono state toccate: sono loro la sostituibilità, non le loro
    implementazioni vuote. `mvn -o clean test` da zero: 143 verdi, quindi nessuna classe stantia stava
    coprendo l'assenza. Segue la descrizione del problema com'era.
    · **`SilentBattleLogger` e `SilentCombatLogger` erano codice morto: tenerli o
    toglierli?** Zero riferimenti in tutto `src`, nemmeno in Javadoc.
    · *Impatto*: due classi con metodi vuoti che nessuno chiama e nessun test copre, e un seme di dubbio
    per chi legge — suggeriscono un punto di sostituzione che in realtà nessuno usa. Non c'è impatto sul
    comportamento: sono inerti per definizione.
    · *Perché non l'ho deciso da sé*: **la SPEC le chiede**. La sezione «1. Giocare e mostrare diventano
    due cose distinte» dice «`ConsoleBattleLogger` e `ConsoleArenaLogger` guadagnano un'interfaccia
    ciascuno, **con implementazione muta**; `CombatLogger` ce l'ha già». Sono quindi codice richiesto, che
    una fase successiva — anch'essa prevista dalla SPEC — ha reso inutile: la tensione è dentro la SPEC,
    non fra codice e SPEC, e scioglierla in silenzio significherebbe scegliere quale metà della SPEC
    contraddire.
    · *Opzioni*: (a) **rimuoverle** e aggiornare quella frase della SPEC dicendo che la muta serve solo a
    `ArenaLogger`, perché `MatchPresentation` ha assorbito il ruolo per gli altri due — il codice resta
    senza residui e le due interfacce, che sono la cosa che conta davvero per la sostituibilità, non si
    toccano; (b) **tenerle** e aggiungere a ciascuna un Javadoc che dica perché esiste senza chiamanti
    (la muta di ogni interfaccia di log, pronta per un lettore futuro che componga logger invece di
    sostituire l'intera presentazione — per esempio la console-lettrice-della-cronaca già prevista come
    compito successivo), così il silenzio non si legge come dimenticanza.
    · *Decisione richiesta*: quale delle due. Non blocca niente: la Fase 12 può partire comunque, e in
    ogni caso questa voce va chiusa prima della Fase 13, che verifica «nessuna modifica non richiesta».
  Ne era emerso un altro in Fase 9, ora **risolto**:
  - 2026-07-31 · Fase 9 · **RISOLTO** con l'opzione (a), scelta dall'utente: il dato è stato aggiunto
    alla cronaca (`ProgressChronicle.jewelBonusPoints`) e la SPEC è stata aggiornata con la chiave nuova
    e il perché. Segue la descrizione del problema com'era, che resta utile a capire la forma del campo.
    · **Buco di autosufficienza: i punti caratteristica extra del gioiello non erano
    nella cronaca.** Trovato scrivendo le frasi della pagina, non cercandolo. La console dice «vale +N
    punti caratteristica» leggendo `HeroProgress.NewJewel#points()` / `JewelUpgrade#points()`, ma
    `ProgressChronicle` porta solo `gains`, cioè il **totale** già fuso: `HeroBrain` distribuisce
    `CHARACTERISTIC_POINTS_PER_VICTORY + jewelDecision.bonusPoints()` in un colpo solo, quindi dalla
    cronaca quel numero non si separa. La pagina infatti dice che il gioiello si indossa o sostituisce,
    ma **non** quanto vale: è l'unica informazione della procedura di fine scontro che la console dà e
    la pagina no.
    · *Impatto*: il criterio 14 chiede che un futuro lettore di cronaca debba «comporre frasi, non
    aggiungere campi». Qui dovrebbe aggiungere un campo, quindi il criterio 14 è **falso** su questo
    punto, per quanto piccolo.
    · *Opzioni*: (a) aggiungere alla cronaca il dato mancante — un `Integer` in `ProgressChronicle`
    valorizzato solo per i due destini di gioiello indossato, nullo altrove, popolato dal mapper dal
    destino già risolto come fa già per `dropped`; costa un campo, una riga di mapper, un caso di test e
    una chiave in più nel JSON documentato dalla SPEC, e la pagina guadagna la frase completa.
    (b) derivarlo nel frontend come `somma(gains) - 3`: **da scartare**, perché porterebbe una costante
    di bilanciamento di `HeroBrain` dentro la pagina, cioè una regola di gioco fuori dal suo posto.
    (c) accettarlo e correggere il criterio 14 della SPEC dichiarando l'eccezione.
    · *Decisione richiesta*: quale delle tre. Non l'ho presa da sé perché la (a) cambia la forma del JSON
    che la SPEC documenta come verificata, e le regole di questo documento dicono di fermarsi e
    registrare invece di cambiare in silenzio i criteri di accettazione. Non blocca la Fase 9: la pagina
    è completa rispetto a quello che la cronaca contiene oggi.
- **Test eseguiti** — `data · fase · comando · esito`:
  - 2026-07-31 · Fase 1 (baseline) · `mvn -o test` · 102 test verdi, `BUILD SUCCESS`.
  - 2026-07-31 · Fasi 2-3 · `mvn -o clean test` · 102 test verdi, `BUILD SUCCESS`.
  - 2026-07-31 · Fase 4 · `mvn -o clean test` · 114 test verdi, `BUILD SUCCESS`.
  - 2026-07-31 · Fase 5 · `mvn -o test` · 119 test verdi, `BUILD SUCCESS` (ri-eseguito dopo la
    revisione funzionale e dopo la rimozione dell'accessor ridondante).
  - 2026-07-31 · Fase 6 · `mvn -o test` · 122 test verdi, `BUILD SUCCESS` (ri-eseguito dopo la
    correzione dei collaboratori condivisi e dopo la revisione funzionale).
  - 2026-07-31 · Fase 7 · `mvn -o test` · 125 test verdi, `BUILD SUCCESS`.
  - 2026-07-31 · Fase 8 · `mvn -o test` · 134 test verdi, `BUILD SUCCESS` (ri-eseguito dopo ognuna delle
    due correzioni sui gestori).
  - 2026-07-31 · Fase 10 · `mvn -o test` · 140 test verdi, `BUILD SUCCESS`. Più la verifica manuale della
    modalità web, ripetuta dopo la correzione del dispatch.
  - 2026-07-31 · Fase 9 · `mvn -o test` · **140 test verdi**, `BUILD SUCCESS`: identici alla Fase 10,
    come dev'essere — la pagina non tocca una riga di Java. Più `mvn -o compile` per la copia delle
    risorse in `target/classes/web/`, e il banco di verifica della pagina (`node`, DOM finto, due
    campioni reali) verde su tutte le sue asserzioni.
  - 2026-07-31 · Fase 9, aggiunta di `jewelBonusPoints` · `mvn -o test` · **143 test verdi**,
    `BUILD SUCCESS` (ri-eseguito dopo il test del gioiello scartato e dopo la revisione funzionale).
    Nessuna aspettativa preesistente modificata; `HeroProgressFormatterTest` verde senza essere stato
    toccato, che è la rete sull'output di console. Il banco di verifica della pagina è stato ri-eseguito
    su **campioni nuovi** scaricati dal server ricompilato — uno con tre prove vinte, duello e due
    gioielli indossati, uno con la corsa interrotta — e le frasi rese sono state stampate per
    constatare che il numero c'è: «...lo indossa, vale +4 punti caratteristica.»
  - 2026-07-31 · Fase 11 · `mvn -o test` · **143 test verdi**, `BUILD SUCCESS`, eseguito da entrambe le
    passate della revisione. Nessuna modifica applicata da loro, quindi nessun ri-controllo dopo un
    cambiamento.
  - 2026-07-31 · Fase 11, rimozione del codice morto · `mvn -o clean test` · **143 test verdi**,
    `BUILD SUCCESS`. `clean` di proposito e non un `test` semplice: cancellando due classi serviva la prova
    che nessun `.class` già compilato in `target/` stesse coprendo l'assenza.
- **Verifica manuale della pagina** — `data · cosa è stato provato · esito`:
  - 2026-07-31 · Fase 9 · **provato da agente, senza browser**: nessuno strumento di automazione del
    browser è disponibile in questa sessione, quindi la parte visiva del criterio 13 non è stata
    eseguita. È stato invece verificato tutto ciò che non richiede occhi: le quattro risposte del server
    con la pagina vera in linea (`/` → `200 text/html; charset=utf-8`; `/app.css` → `200 text/css`;
    `/app.js` → `200 text/javascript`, 674 righe, zero occorrenze di «segnaposto»; `/api/chronicle` →
    `200 application/json`; percorso ignoto → `404`), la sola riga stampata su console
    (`http://127.0.0.1:8080/`), e il comportamento della riproduzione col banco a DOM finto descritto
    nella Fase 9.
  - **Da fare all'utente**, ed è l'unica cosa che manca alla fase: aprire `http://127.0.0.1:8080/`,
    giudicare l'aspetto del layout a due colonne e delle barre, scorrere la partita avanti e indietro
    coi controlli, e **confermare col pannello di rete aperto** che dopo il caricamento iniziale non
    parte nessun'altra richiesta (tre risorse più `/api/chronicle`, e poi silenzio). Ricaricare deve dare
    una partita diversa.
- **Revisione `java-functional-evolver`** — `data · invocato sì/no · cosa ha cambiato`:
  - 2026-07-31 · **Fase 11, passata A** (progressione e cronaca) · **invocata**, nessuna modifica
    applicata. Ha confermato nel merito, non per inerzia: le trasformazioni di lista del mapper sono già
    stream puri; il `for` con indice di `Arena.snapshotRoster` resta un `for` perché la dipendenza
    dall'indice lo giustifica, e `IntStream.range().mapToObj()` più `Stream.concat` non sarebbe più
    leggibile; `RoundReport.appending` con copia-poi-aggiungi resta com'è, coerente con `snapshotRoster`,
    perché `Stream.concat(...).toList()` non aggiunge chiarezza. Ha **giudicato giustificata** la
    simmetria duello/battaglia espressa in tre punti (`MatchRunner`, `MatchPresentation`, i due campi
    `rounds`/`turns` della cronaca): i tipi coinvolti sono davvero eterogenei, e unificarli chiederebbe
    un'astrazione generica prematura per togliere una duplicazione di *forma* e non di contenuto. Nessun
    difetto di comportamento trovato: nessuna divergenza fra l'esito che `Arena` calcola e quello che
    cronaca e logger raccontano. Ha trovato il codice morto (segnalazione (a) della Fase 11).
  - 2026-07-31 · **Fase 11, passata B** (confine web e ingresso) · **invocata**, nessuna modifica
    applicata. Ha respinto nel merito la trasformazione delle guardie dei due gestori HTTP in una catena
    di combinatori: sono validazioni con uscite anticipate ed effetti HTTP, non trasformazioni di dati, e
    una pipeline sepellirebbe proprio l'ordine dei controlli che il Javadoc si preoccupa di spiegare — ed
    è l'ordine su cui la Fase 8 aveva trovato due difetti veri. Ha respinto come **dannosa** la
    compattazione dei locali di `SilentArenaRun.get()` in una sola espressione, perché contraddirebbe la
    regola di bassa densità del progetto. Ha verificato che `SilentArenaRun` è **inusabile nel modo
    sbagliato per costruzione**: l'unico campo è `CombatSettings`, e ogni collaboratore — `FighterFactory`
    col suo `usedNames` in testa, che è il motivo per cui la classe esiste — si ricostruisce dentro
    `get()`; non c'è niente che un chiamante possa condividere fra due partite. Sul dispatch di `UiMode`
    ha cercato una crepa ora che l'insieme è finito e non l'ha trovata. Ha segnalato la riga commentata di
    `UiMode`, verificando da sé con `git diff` che è preesistente (segnalazione (b)).
  - 2026-07-31 · Fase 9, la pagina · **non invocato**, e non è una dimenticanza: produce solo HTML, CSS e
    JavaScript. La Fase 11 lo dichiara già («nessuna revisione funzionale sul JavaScript e sul CSS: non
    è Java») e nessun file Java è stato toccato — verificato che `git status` sui `.java` era identico a
    prima della fase.
  - 2026-07-31 · Fase 9, aggiunta di `jewelBonusPoints` · **invocato**, una modifica applicata, e solo
    nei test: ha estratto un `heroWithBasicGear()` privato in `ChronicleMapperTest`, dove cinque test su
    sette costruivano lo **stesso** `Hero` identico riga per riga — fixture ripetuto che non porta
    informazione, mentre quello che ogni test varia davvero (il `Loot` e quale dei campi di
    `HeroProgress` è valorizzato) resta inline sotto gli occhi di chi legge. Ha lasciato fuori il test
    dell'arma scartata, dove l'attacco 9 dell'arma già impugnata **non** è incidentale: è ciò che rende
    scartata quella trovata. Sul codice di produzione non ha trovato niente da cambiare, e ha
    **respinto** l'unificazione dei tre switch del mapper in una tabella di dispatch su `LootFate`:
    scambierebbe una triplicazione piccola, esplicita e controllata dal compilatore con un meccanismo
    generico più difficile da leggere, che è esattamente l'astrazione funzionale prematura da evitare.
  - 2026-07-31 · Fasi 2-3 · **invocato**, nessuna modifica applicata. Il diff è interamente
    strutturale (estrazione di interfacce e spostamento di codice a comportamento invariato): nessuna
    nuova logica di trasformazione dati su cui intervenire. Il ciclo sui round di `presentBattle` resta
    un `for` di proposito — i suoi effetti collaterali e il ritmo delle attese *sono* il suo scopo, e
    trasformarlo in stream violerebbe il vincolo di output identico. La Fase 11 resterà da fare sul
    codice delle fasi successive.
  - 2026-07-31 · Fase 4 · **invocato**, nessuna modifica applicata. Ha giudicato già adeguati lo switch
    esaustivo del formatter (una regola di dominio esplicita al posto di una catena `map`/`orElse` che
    la nascondeva), i record immutabili con `List.copyOf` nei costruttori canonici e le trasformazioni
    `stream().map().toList()` del mapper. Ha **respinto** come compattezza fine a sé stessa
    l'estrazione dei prefissi ripetuti negli otto metodi di frase: una frase per caso, leggibile senza
    saltare altrove, è il prezzo giusto. Ha invece **segnalato** due cose poi corrette: la mancanza di
    copertura su `snapshotProgress` e la discriminazione del `Loot` ancora duplicata fra
    `HeroProgress.lootFate()` e il mapper.
  - 2026-07-31 · Fase 10 · **invocato**, nessuna modifica applicata. Il parsing degli argomenti ha uscite
    anticipate ed errori per eccezione, non collezioni da trasformare: forzarlo in una pipeline
    nasconderebbe proprio il controllo di flusso che è il suo scopo. Interrogato sul fatto che `UiMode`
    sia insieme risultato del parsing, avvio della modalità e casa delle costanti, ha giudicato che è una
    responsabilità sola vista da tre lati: le costanti esistono solo per `fromArgs`, e `launch` si
    dispaccia sui valori che `fromArgs` ha appena prodotto. Un `UiModeParser` separato sposterebbe codice
    senza ridurre un rischio — da riconsiderare solo se nascesse una seconda fonte di configurazione.
  - 2026-07-31 · Fase 8 · **invocato**, nessuna modifica di stile applicata, ma ha trovato il difetto
    dell'ordine dei controlli in `StaticResourceHandler` (vedi *Decisioni*) e l'ha segnalato senza
    correggerlo, essendo un cambio di comportamento osservabile: la correzione è passata dall'implementer.
    Sullo stile ha respinto due proposte: fattorizzare lo schema comune dei due gestori (si somigliano in
    superficie, ma uno genera il corpo da un fornitore e l'altro lo legge dal classpath con un secondo
    punto di fallimento: l'astrazione dovrebbe esporre proprio la differenza) e mettere in cache le
    risorse statiche (aggiungerebbe stato condiviso e invalidazione per tre file piccoli con esecutore
    seriale).
  - 2026-07-31 · Fase 7 · **invocato**, nessuna modifica applicata. Ha confermato la raccolta ricorsiva
    con accumulatore passato per parametro nel test (l'API di `JsonNode` espone `fieldNames()` come
    `Iterator`, e forzarla in stream costerebbe boilerplate senza ridurre rischio) e la scomposizione in
    metodi `assertXxxKeys` (due sono riusati fra rami diversi dell'albero: è deduplicazione di una forma
    ripetuta, non frammentazione). Ha riverificato che Jackson è importato solo qui e che i tipi della
    cronaca non portano annotazioni.
  - 2026-07-31 · Fase 6 · **invocato**, nessuna modifica applicata. `SilentArenaRun.get()` è cablaggio
    di collaboratori, non trasformazione di dati: niente su cui intervenire. Ha confermato
    `TurnPacer.none()` come factory statico sull'interfaccia e ha giudicato corretto il `forEach` con
    effetto esplicito nel doppio del test (l'effetto *è* l'operazione, e `strikeDown` lo nomina). Ha
    segnalato una riga di 116 caratteri: **non corretta**, perché il progetto ha già 31 righe oltre 110
    caratteri e un massimo di 129, e in caso di conflitto prevale lo stile del progetto.
  - 2026-07-31 · Fase 5 · **invocato, due modifiche applicate**. Ha estratto un `chronicleOf` privato:
    la costruzione del `TrialChronicle` compariva due volte con sei argomenti su otto identici, e una
    modifica alla forma del record avrebbe avuto un punto da aggiornare e due chiamanti da tenere
    allineati a mano. Soprattutto ha **togliesto `outcome` da `RoundReport`**: la catena partiva con un
    `RoundOutcome.WON` finto — necessario perché `isPassed()` fosse vero al primo giro, benché nulla
    fosse ancora stato vinto — e l'esito era già dentro l'ultima voce di cronaca, quindi tenerne una
    seconda copia accanto alla fonte era custodia di un dato derivato. Ora il rapporto porta un
    `boolean passed` con un significato onesto («procedi alla prima prova») e l'esito della corsa si
    legge una volta sola da `trials().getLast().outcome()`. Ha **respinto** l'aggiunta di un
    `RoundOutcome.PENDING`, che avrebbe toccato ogni switch esaustivo sull'enum per rappresentare uno
    stato che non è un esito di prova. Dopo la revisione ho tolto io `isPassed()`, rimasto come
    delegato ridondante dell'accessor `passed()` del record.
- **Output di console confrontato con quello di partenza** — `data · fase · identico sì/no`:
  - 2026-07-31 · Fase 11 · sì, per costruzione: la revisione non ha applicato nessuna modifica, in nessuna
    delle due passate. I 143 test restano verdi, compresi i quattro che catturano `System.out`.
  - 2026-07-31 · Fase 9 · sì. La pagina tocca solo tre file sotto `src/main/resources/web/`, che il
    percorso console non legge nemmeno. L'aggiunta di `jewelBonusPoints` tocca invece del Java, ma non
    quello che stampa: `HeroProgressFormatter` non è stato modificato, la frase della console la
    componeva già da `HeroProgress` e continua a farlo, e `HeroProgressFormatterTest` (8 test) è verde
    senza essere stato toccato. La cronaca ha guadagnato un campo che nessun renderer di console legge.
  - 2026-07-31 · Fase 2 · sì. Nessuna logica toccata: solo `implements` e `@Override` aggiunti, e il
    logger di battaglia promosso a collaboratore.
  - 2026-07-31 · Fase 3 · sì. Lo scontro ora si gioca prima che la presentazione cominci, ma la
    chiamata al motore non stampa niente e la presentazione stampa accoppiamento/schieramenti come
    prima cosa: gli ordini `accoppiamento → turni → esito` e `schieramenti → attesa → round → esito`
    sono preservati. La rete di sicurezza sono i test che catturano `System.out` (`ArenaTest`,
    `ConsoleBattleLoggerTest`, `ConsoleCombatLoggerOutcomeTest`), tutti verdi senza modifiche alle
    aspettative.
  - 2026-07-31 · Fase 6 · sì. Il percorso di console non è stato toccato: la fase aggiunge solo un modo
    alternativo di assemblare l'arena e un pacer che non attende. `TurnPacer` guadagna un factory
    statico, nessuna implementazione esistente cambia.
  - 2026-07-31 · Fase 5 · sì. `Arena` registra in più, ma non stampa niente di nuovo e non cambia
    l'ordine di nessuna chiamata al logger. Le 12 aspettative preesistenti di `ArenaTest`, che cattura
    `System.out`, sono verdi senza essere state toccate.
  - 2026-07-31 · Fase 4 · sì. L'unico file di presentazione toccato è `HeroProgressFormatter`, che ora
    sceglie la frase sul destino già risolto invece di dedurlo: le otto frasi sono identiche parola per
    parola e `HeroProgressFormatterTest` (8 test) è verde senza essere stato toccato.

## Esito finale

**Stato:** tutte le tredici fasi sono state eseguite. Manca **una sola** cosa, dichiarata manuale dalla
SPEC in partenza: lo sguardo dell'utente sulla pagina (criterio 13, parte visiva). Per questo lo stato in
testa al documento resta `IN_PROGRESS` e non `COMPLETED`.

**Cos'è stato fatto.** Il compito ha separato il *giocare* dal *mostrare*, e la separazione ha una forma
precisa: `MatchRunner` restituisce l'esito che chiede al motore invece di buttarlo, il *come* e il
*quando* mostrarlo sono di un `MatchPresentation` sostituibile, e `Arena.run()` restituisce la cronaca
della corsa. Da lì nascono i due lettori: la console, che non è cambiata di una riga nel suo output, e una
pagina web che riproduce la stessa partita coi controlli che un terminale non può avere. Regge tutto un
fatto che era già vero e non è stato inventato qui: il gioco non ha scelte del giocatore, quindi una
partita è determinata nell'istante in cui viene giocata, e una UI non deve pilotare niente — le basta
leggere.

**Numeri.** Baseline 102 test; alla fine **143**, tutti verdi, con **nessuna aspettativa preesistente
modificata** in nessuna delle tredici fasi. Una sola dipendenza nuova, `jackson-databind`, confinata in
`combat.io.web`. Due classi create dal compito e poi rimosse dal compito stesso.

**Difetti trovati e corretti lungo la strada**, che sono la parte di questo registro che vale più della
cronologia:

- Fase 5: `RoundReport` custodiva l'esito accanto alla sua fonte e la catena partiva con un `WON` finto.
- Fase 6: `SilentArenaRun` teneva i collaboratori come campi, e col server della Fase 8 la stessa
  `FighterFactory` — col suo `usedNames` — sarebbe stata condivisa da tutte le partite del processo.
- Fase 8, due difetti veri nei gestori HTTP: `GET /api/chronicle/qualunque-cosa` rispondeva `200` **e
  giocava una partita intera**, perché `createContext` fa match per prefisso e il percorso non veniva
  confrontato; e `POST` su un percorso inesistente rispondeva `405` invece di `404`, perché il metodo si
  controllava prima del percorso su un gestore registrato come ripiego di tutto.
- Fase 9, il buco di autosufficienza: i punti che vale il gioiello indossato non erano nella cronaca, e
  il criterio 14 era quindi falso. Chiuso aggiungendo il dato.
- Fase 9, due difetti nella pagina: il ripiego di `alignVitalsToRoster` restituiva `undefined` e avrebbe
  spento la pagina intera — il ramo che esisteva per difendersi era quello che la rendeva fatale — e un
  salto non metteva in pausa, così il timer superava subito la posizione scelta dall'utente.
- Fase 10: la prima versione discriminava le due modalità con l'unico `instanceof` del repository.
- Fase 11: due classi mute rimaste senza chiamanti, viste solo dalla vista d'insieme.

**Test eseguiti**: vedi il registro, una riga per fase. L'ultima passata è `mvn -o test` a 143 verdi, più
trentaquattro corse di console giocate per intero con `stdin` chiuso — zero eccezioni, e la
trentaquattresima arriva alla terza prova, quindi esercita anche il duello a schermate — e le risposte del
server verificate su un'istanza riavviata dalle classi correnti.

**Verifica manuale della pagina**: eseguita per tutto ciò che non richiede occhi — le risposte del
server con la pagina vera in linea, la sola riga stampata su console, e il comportamento della
riproduzione provato eseguendo `app.js` su un DOM finto con due campioni JSON scaricati dal server vero
(un solo `fetch`, avanti e indietro con confronto del testo reso momento per momento, salti, velocità,
pausa, il ramo di ripiego, il banner d'errore). Resta la parte visiva.

**Note residue**, nessuna delle quali blocca:

- `TrialChronicle.description` porta prosa italiana ed è l'unico campo della cronaca che decide *come si
  dice* una cosa invece di *cos'è*. La SPEC la chiede così; la forma coerente sarebbe un `TrialKind` con
  la frase nei renderer, ed è la strada obbligata se servisse una seconda lingua. Da valutare insieme
  alla console-lettrice-della-cronaca.
- Nessun test lancia `Main`, e la pagina non ha test automatici: due lacune **dichiarate**, entrambe con
  la loro ragione, entrambe scritte anche in `CLAUDE.md` perché non vengano riscoperte come sorprese.
- La riga commentata in `UiMode.ConsoleMode.launch` è debito preesistente al compito, traslocato da
  `Main` in Fase 10 e lasciato com'era.
- I tipi di log del motore sono `SNAPSHOT` e affiorano nel JSON: un campo rinominato là romperebbe la
  pagina. Il rischio è mitigato dal test sulle chiavi, non eliminato — è il prezzo scelto per non
  mantenere due volte la stessa forma di dati.

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
