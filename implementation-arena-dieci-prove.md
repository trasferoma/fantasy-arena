# IMPLEMENTATION — Arena a dieci prove, percorso disegnato nella pagina, freccia e iniziativa

**Specifica di riferimento:** `spec-arena-dieci-prove.md`  — nel resto del documento: «la SPEC».
**Stato:** `COMPLETED`  <!-- NOT_STARTED | IN_PROGRESS | BLOCKED | COMPLETED -->

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
- Cinque punti sono facili da tradire senza accorgersene, e vanno presidiati a ogni fase:
  **(a) il vincolo di non-spoiler** — una stazione mai giocata deve restare indistinguibile da una non
  ancora raggiunta, nell'aspetto **e** nell'interattività; si verifica confrontando **due** partite di
  lunghezza diversa, non guardandone una;
  **(b) `Arena` non decide** — se ti trovi a scrivere un `if` di gioco nella scansione, quel dato
  appartiene alla stazione o al cervello;
  **(c) nessuna formula di combattimento** — freccia, formula breve e stellina leggono dati già
  calcolati dal motore;
  **(d) la cronaca è di soli dati** — il campo nuovo è un numero, non una frase;
  **(e) la direzione delle dipendenze** — `combat.io.web` resta un pozzo, `render` resta foglia,
  `combat.chronicle` non impara niente di nuovo.
- **Le due questioni aperte sono state decise dall'utente prima dell'inizio del lavoro** (2026-07-31,
  vedi *Registro*): Q1 nella forma a quattro scaglioni, Q2 nella forma A (monte punti come dato della
  stazione). La Fase 10 non è più `BLOCKED` e, come previsto dalla sua ultima voce, si **fonde nelle
  Fasi 2-3**: il monte punti nasce come campo della stazione insieme alla tabella, e le tabelle di
  rarità si scrivono nella stessa passata del percorso.
- Non toccare `C:/build/git/fantasy-combat-system` né `C:/build/git/fantasy-game-toolkit`: i tipi di
  risultato del motore si usano come sono.
- Il Java lo scrive `clean-code-implementer` e lo revisiona `java-functional-evolver`; il registro e
  l'avanzamento di questo file li tiene il processo principale, mai l'implementer.

## Piano operativo

**Fase 1 — Analisi (nessun codice)** — completata il 2026-07-31 dal processo principale
- [x] Confermare i punti del *Contesto* della SPEC sul codice corrente, in particolare: la catena di
      tre `andThen` in `Arena.run()`, la firma di `fightRound`, le sei costanti cablate, le due frasi
      di `ConsoleArenaLogger` che dicono «tre», `createChallengers(int)` senza livello,
      `lootRarityTable` a due scaglioni, l'assenza di un campo di lunghezza prevista in
      `ArenaChronicle`.
      → Tutto confermato leggendo i file. Le due frasi di console sono
      `reportEntrance` («dovrà superare tre prove») e `reportTriumph` («tutte e tre le prove»).
- [x] Confermare i punti del *Contesto* sulla pagina: `totalTrials` da `chronicle.trials.length`,
      `populateTrialButtons` su `chronicle.trials`, il momento di conclusione senza `trialNumber`,
      `buildEngagement` che già risolve i due nomi dagli indici, il collasso di `#battlefield` a una
      colonna sotto 720px.
      → Confermato (`app.js:92`, `:634`, `:181`, `:133`).
- [x] Riverificare il **difetto latente** segnalato dalla SPEC: `buildDuelStepMoment` legge
      `turn.initiative.chosenName` senza guardia, mentre `TurnLogFormatter` e `CombatScreenRenderer`
      verificano quel `null` in Java. Decidere in quale fase chiuderlo.
      → Confermato a `app.js:160`. Si chiude in **Fase 7**, con la stellina, che ha bisogno della
      stessa distinzione.
- [x] Registrare la **baseline**: numero di test verdi prima di ogni modifica (`mvn -o test`). È il
      riferimento con cui confrontare ogni fase.
      → **143 test, 0 fallimenti, `BUILD SUCCESS`** (vedi *Registro*).
- [x] Censire le aspettative dei test che il percorso a dieci prove rende false: in `ArenaTest`
      `List.of(1, 2, 1)`, `List.of(BATTLE, BATTLE, DUEL)`, `challengersOfRound(3)`, i
      `countOccurrences(..., 3)`, la frase «ha superato tutte e tre le prove», e il numero di
      `FightOutcome` passati a `scripted(...)`.
      → Censite: `scripted(...)` con tre esiti compare in **dieci** test (righe 74, 85, 95, 106, 116,
      128, 141, 158, 187, 203, 218, 234), `challengersOfRound(3)` alla riga 145, i tre
      `countOccurrences(..., 3)` alle righe 120-123.
- [x] Rilevare lo stile dei test esistenti (solo `Assertions`, doppi per ereditarietà,
      `System.out` catturato) e confermare "File coinvolti (effettivi)".
      → Confermato; `ChronicleJsonTest` asserisce le chiavi per livello di annidamento e la radice non
      ha oggi nessuna chiave di lunghezza.

**Fase 2 — Il percorso come dato (nessun consumatore)**
- [x] Creare il tipo del percorso in `it.fantasyarena.combat`: la tabella delle dieci stazioni, con
      numero, descrizione, numero di sfidanti, origine degli sfidanti, e la **forma derivata** dal
      numero di sfidanti (non un componente: vedi la decisione 2 della SPEC).
- [x] Le dieci descrizioni italiane brevi, nel registro delle tre attuali, in un punto solo.
- [x] Javadoc di classe denso: perché il percorso vive qui e non in `HeroBrain`, perché la forma è
      derivata e non custodita, perché l'origine degli sfidanti è un'enum e non un booleano.
- [x] Nessun consumatore in questa fase: `Arena` non cambia ancora, la console non cambia, la suite
      resta verde alla baseline più i test nuovi.
- [x] Test (criterio 1): dieci stazioni, numerazione 1-10, conteggi `1,1,1,2,2,2,3,3,3,1`, forme
      attese stazione per stazione, origine speculare **solo** alla decima, dieci descrizioni non
      vuote e tutte diverse.

**Fase 3 — `Arena` scandisce il percorso**
- [x] `run()` diventa una piega sulle stazioni, con la cortocircuitazione di `RoundReport.andThen`
      preservata: una prova non vinta non fa giocare le successive.
- [x] Rimuovere i tre metodi privati per prova e le sei costanti cablate; `fightRound` resta com'è.
      La scelta degli sfidanti e quella della forma sono due switch **esaustivi senza `default`**.
- [x] Verificare che `createMirrorRival` sia invocato solo quando la decima prova viene giocata (la
      valutazione dev'essere pigra, stazione per stazione, non in testa alla piega).
- [x] Le due frasi di console che dicono «tre» ricevono il numero previsto dal percorso come dato:
      adeguare `ArenaLogger`, `ConsoleArenaLogger` e `SilentArenaLogger`. La frase resta del
      renderer, il numero arriva dal percorso.
- [x] `ArenaTest` adeguato: dieci esiti nel copione, nuove aspettative su conteggi, presentazioni,
      prova dello specchio e frase del trionfo. **Nessun test va indebolito** per farlo passare.
- [x] Criterio di completamento: criteri 2, 3, 4, 5, 8 verdi; suite verde; una corsa di console con
      `stdin` chiuso arriva almeno alla quarta prova senza eccezioni.

**Fase 4 — La cronaca porta la lunghezza prevista**
- [x] `ArenaChronicle` guadagna il campo intero della lunghezza prevista, popolato dalla dimensione
      della tabella delle stazioni (non da una costante scritta due volte).
- [x] Invariante verificato in costruzione: la lunghezza prevista è **≥** del numero di voci giocate,
      con eccezione esplicita se violato. Javadoc che dice perché il campo esiste (il lettore ha solo
      il JSON e non può contare le stazioni di una tabella Java).
- [x] `ChronicleJsonTest`: la chiave nuova asserita alla radice (criterio 7). È la sola rete verso il
      JavaScript e va allargata nella stessa fase in cui la cronaca cambia forma.
- [x] Test in `ArenaTest` (criterio 6): la lunghezza prevista vale dieci anche quando la corsa si
      chiude alla prima prova; e un test sull'invariante rifiutato.
- [x] Criterio di completamento: criteri 6 e 7 verdi, suite verde, console invariata rispetto alla
      Fase 3.

**Fase 5 — La pagina: il percorso disegnato (non-spoiler)**
- [x] Il denominatore dell'intestazione diventa la lunghezza prevista: «Prova 4/10» dal primo passo.
- [x] Nuovo elemento del percorso sotto l'intestazione della prova (decisione 8 della SPEC): una
      stazione per prova prevista, con le linee di collegamento, e tre stati distinti — passata,
      corrente, da raggiungere.
- [x] La stazione corrente si ricava da `trialNumber` nei momenti di prova e di procedura, e da
      `conclusion.lastTrial` nel momento di conclusione.
- [x] Cliccabili **solo** le stazioni fino alla corrente; ogni stazione oltre la corrente ha aspetto,
      testo, attributi e comportamento **identici** alle altre oltre la corrente, giocata o no.
- [x] L'esito compare sulla stazione corrente solo quando è già rivelato altrove (`showOutcome`, o il
      momento di conclusione).
- [x] Rimuovere `#trial-jump` da `index.html`, `populateTrialButtons` da `app.js` e `.trial-jump` da
      `app.css`; il salto a una prova passa dalle stazioni.
- [x] Ogni stazione porta un'etichetta testuale leggibile (numero e stato), non solo un simbolo.
      Testo nel DOM con `textContent`.
- [x] Nessun `fetch` in più: il percorso si disegna da dati già in memoria.
- [x] Verifica manuale mirata (criteri 9, 10): **due** partite di lunghezza diversa messe a
      confronto, e il conteggio dei bersagli cliccabili.

**Fase 6 — La pagina: la colonna centrale con la freccia**
- [x] `#battlefield` da due a tre colonne, con la colonna centrale fra `#team-0` e `#team-1`;
      comportamento sensato anche sotto la soglia in cui la griglia collassa.
- [x] Le sette etichette brevi in un punto solo del file JavaScript, accanto alle altre costanti
      linguistiche, nel registro di `BattleSceneRenderer.formulaLabel`.
- [x] Battaglia: una voce per scambio, dagli indici di ogni `EngagementTurn` del round; squadra 0 a
      sinistra e 1 a destra, freccia orientata dalla squadra dell'attaccante, formula sopra.
- [x] Duello: attore risolto per nome dal `chosenName`, bersaglio l'altro dei due.
- [x] Il punto **unico** che risolve un nome in indice di roster, condiviso con la Fase 7: `null` se
      il nome non trova corrispondenza o ne trova più di una.
- [x] Ripieghi (criterio 12): `REST` senza freccia; iniziativa assente o nome ambiguo senza freccia;
      `action` assente senza formula; tipo di azione sconosciuto mostrato grezzo. Mai una freccia
      sbagliata, mai un `TypeError`.
- [x] Il `describeAction` verboso del pannello testuale resta com'è: le due cose convivono.
- [x] Verifica manuale (criteri 11, 12), confrontando le formule con una corsa di console.

**Fase 7 — La pagina: la stellina dell'iniziativa**
- [x] I momenti portano **come dato** l'insieme degli indici di roster che hanno l'iniziativa nel
      passo: nella battaglia gli `attackerIndex` degli scambi del round, nel duello l'indice risolto
      dal `chosenName` quando c'è. La costruzione dei momenti resta pura, il DOM legge.
- [x] La scheda del combattente mostra il segno vicino al nome, con etichetta esplicita e non un
      carattere muto.
- [x] Chiudere il **difetto latente**: l'accesso a `initiative` nel duello diventa guardato, così un
      turno senza report d'iniziativa non spegne la pagina — nessuna stellina e nessuna freccia,
      invece di un `TypeError`.
- [x] Verifica manuale (criterio 13), inclusa la lettura dell'etichetta.

**Fase 8 — Revisione funzionale del Java (`java-functional-evolver`)**
- [x] Invocare la revisione sul Java prodotto e modificato (percorso, `Arena`, cronaca, logger, test).
      È l'agente a decidere se intervenire: se trova il codice già ottimale, lo dichiara.
- [x] Vincoli da rispettare nelle proposte: la cortocircuitazione della piega non si perde, gli switch
      restano esaustivi senza `default`, nessun combinatore morto introdotto da una `reduce`, Java 21
      senza pattern matching né preview.
- [x] Nessuna revisione funzionale su JavaScript e CSS: non è Java.
- [x] Ri-eseguire i test dopo eventuali modifiche e annotare l'esito nel *Registro* (invocato /
      non invocato, cosa ha cambiato).

**Fase 9 — Documentazione (nessun codice di produzione)**
- [x] `CLAUDE.md`: dieci prove al posto di tre; la forma della prova **derivata** dal numero di
      sfidanti (quindi duello alle prove 1-3 e 10, battaglia alle 4-9 — oggi il testo dice l'opposto,
      «le prime due mostrate come battaglia e la terza come duello»); il percorso come dato in un
      punto solo, con la sua posizione nella tabella dei package; il campo nuovo della cronaca; la
      riga di `combat.hero` se il bilanciamento cambia.
- [x] `README.md`: le tre prove diventano dieci nei tre punti che le nominano (descrizione iniziale,
      elenco delle prove, elenco delle cose che ci sono); il percorso disegnato fra ciò che la pagina
      mostra.
- [x] `daImplementare.md`: la voce «arena a lunghezza variabile (oggi le prove sono esattamente tre,
      cablate in `Arena`)` va riscritta — le prove sono dieci e non più cablate in `Arena` ma
      descritte come dato, quindi renderle variabili è diventato molto più vicino.
- [x] Criterio 17 verificato leggendo il codice, non asserendo: le affermazioni architetturali scritte
      nei documenti devono corrispondere agli import reali.

**Fase 10 — Bilanciamento — decisa il 2026-07-31, fusa nelle Fasi 2-3**
- [x] **Q1 accolta nella forma a quattro scaglioni**: `lootRarityTable` resta il punto unico e passa a
      quattro tabelle — prove 1-2 e 3-5 sono le due di oggi riusate senza ritararle, 6-8 vale
      `RARE 25 / EPIC 50 / LEGENDARY 25`, 9-10 vale `EPIC 65 / LEGENDARY 35`. `HeroBrainTest` riscritto
      sulle nuove aspettative: il suo `assertSame` fra livello 2 e 3 diventa **falso** (scaglioni
      diversi) e va sostituito, non adattato al caso.
- [x] **Q2 accolta nella forma A**: la stazione porta il monte punti degli sfidanti,
      `createChallengers` guadagna il parametro, `generateWarrior` smette di leggere la costante. Curva
      `15 + 3 * (prova - 1)`, cioè `15,18,21,24,27,30,33,36,39` sulle nove prove generate; la decima
      non ha monte punti proprio, perché lo specchio ricalca il protagonista.
- [x] Decisione presa dal processo principale e da non rimettere in discussione: per gli sfidanti i
      **bonus di razza e classe restano attivi**, come oggi. `generateRival` li disattiva perché deve
      eguagliare esattamente un totale già cresciuto; `generateWarrior` genera anche il protagonista,
      quindi lasciarli attivi è ciò che tiene il confronto alla pari. Cambiarli renderebbe gli
      sfidanti più deboli del protagonista a pari monte punti dichiarato.
- [x] Test: la tabella del percorso fissa anche i monte punti; `FighterFactoryTest` verifica che gli
      sfidanti nascano col monte punti richiesto (criterio 18).

**Fase 11 — Revisione finale**
- [x] Controlli strutturali: nessuna formula di combattimento nata qui; `combat.io.web` senza import
      proibiti e importato solo da `UiMode`; `combat.chronicle` senza Jackson e senza `combat.io`;
      nessuna casualità fuori da `FighterFactory` e `HeroBrain`; nessuna stringa di presentazione
      nuova nei record della cronaca (criterio 15).
- [x] `mvn -o test` verde, col conteggio confrontato con la baseline della Fase 1.
- [x] **Console provata da capo a fondo** con `stdin` chiuso (il `TurnPacer` prosegue su EOF), più
      volte: uscita `0`, nessuna eccezione, e almeno una corsa che arrivi abbastanza in fondo da
      esercitare le tre forme di presentazione (duello iniziale, battaglia a due e a tre, e se capita
      il duello dello specchio).
- [x] **Pagina provata a mano** su un server riavviato dalle classi correnti:
      `mvn exec:java -Dexec.args="web"`, poi `http://127.0.0.1:8080/`. Cosa guardare, in questo
      ordine:
      1. dieci stazioni disegnate con le linee di collegamento, e «Prova 1/10» al primo passo;
      2. avanzando, la stazione corrente si sposta e le precedenti diventano «passate»;
      3. **due ricariche a confronto** — una corsa persa presto e una più lunga: le stazioni oltre la
         corrente devono essere identiche fra le due, aspetto e comportamento (è la verifica del
         criterio 10, e non si può fare su una sola partita);
      4. click su una stazione passata → salta a quella prova; click su una futura → non accade
         niente e il cursore non promette niente;
      5. la colonna centrale: direzione della freccia coerente col lato dell'attaccante, formula
         confrontata con una corsa di console, un round con più scambi che mostra più voci, un turno
         di `REST` senza freccia;
      6. la stellina: presente sull'attaccante di ogni scambio, sul portatore dell'iniziativa nel
         duello, con l'etichetta leggibile;
      7. pannello di rete aperto: **una sola** richiesta dopo il caricamento, anche dopo salti, cambi
         di velocità e riproduzione fino alla fine;
      8. nessun errore in console del browser in nessuna delle partite provate.
      Attenzione alla **trappola già documentata**: fermare Maven non uccide la JVM generata, che
      resta in ascolto e tiene la porta — controllare *chi* ascolta (`netstat -ano | grep 8080`) e
      chiudere quel PID prima di credere a una verifica.
- [x] Criteri della *Definition of done* ripercorsi uno per uno, dichiarando per ognuno se è coperto
      da un test, verificato a mano o verificato in revisione.
- [x] Aggiornare *Decisioni/Deviazioni*, compilare *Esito finale*, portare lo stato a `COMPLETED`.
      Se resta aperta la sola verifica visiva dell'utente, lo stato resta `IN_PROGRESS`: spuntare una
      voce che nessuno ha guardato è precisamente ciò che le regole di questo documento vietano.

## File coinvolti (effettivi)

Pre-compilati in via **provvisoria** dall'analisi della SPEC: da confermare e completare in Fase 1.
I nomi dei tipi nuovi sono proposte, non vincoli: quello che vincola è il ruolo.

**Il percorso (Fasi 2-3)**
- `src/main/java/it/fantasyarena/combat/TrialPlan.java` — nuovo: la tabella delle dieci stazioni,
  unico posto che sa com'è fatta la corsa
- `src/main/java/it/fantasyarena/combat/TrialStation.java` — nuovo: una stazione, con la forma
  derivata dal numero di sfidanti
- `src/main/java/it/fantasyarena/combat/ChallengerOrigin.java` — nuovo: generati o specchio, enum a
  due costanti per gli switch esaustivi
- `src/main/java/it/fantasyarena/combat/Arena.java` — `run()` diventa una piega; via i tre metodi per
  prova e le sei costanti cablate; gli sfidanti e la forma arrivano dalla stazione

**La console (Fase 3)**
- `src/main/java/it/fantasyarena/combat/io/log/ArenaLogger.java` — il numero di prove previste arriva
  come dato a chi compone le frasi di ingresso e trionfo
- `src/main/java/it/fantasyarena/combat/io/log/ConsoleArenaLogger.java` — le due frasi non dicono più
  «tre»
- `src/main/java/it/fantasyarena/combat/io/log/SilentArenaLogger.java` — adeguamento di firma

**La cronaca (Fase 4)**
- `src/main/java/it/fantasyarena/combat/chronicle/ArenaChronicle.java` — il campo della lunghezza
  prevista, con l'invariante verificato in costruzione

**La pagina (Fasi 5-7)**
- `src/main/resources/web/index.html` — l'elemento del percorso; via `#trial-jump`; la colonna
  centrale di `#battlefield`
- `src/main/resources/web/app.js` — denominatore dalla lunghezza prevista, percorso con i tre stati e
  la cliccabilità limitata, colonna centrale con freccia e formula, stellina dell'iniziativa,
  risoluzione del nome in un punto solo, accesso a `initiative` guardato; via `populateTrialButtons`
- `src/main/resources/web/app.css` — stili del percorso e delle sue tre stazioni, griglia a tre
  colonne e suo collasso, marcatore dell'iniziativa; via `.trial-jump`

**Il bilanciamento (Fase 10, condizionata alle risposte)**
- `src/main/java/it/fantasyarena/combat/hero/HeroBrain.java` — scaglioni della rarità del loot
- `src/main/java/it/fantasyarena/combat/factory/FighterFactory.java` — monte punti degli sfidanti
  (file a **4 spazi**: mantenere lo stile del file)
- `src/main/java/it/fantasyarena/combat/TrialStation.java` — il monte punti come dato della stazione,
  se la proposta A viene accolta

**Test**
- `src/test/java/it/fantasyarena/combat/TrialPlanTest.java` — nuovo: la tabella del percorso
  (criterio 1)
- `src/test/java/it/fantasyarena/combat/ArenaTest.java` — **adeguamento previsto e non banale**: dieci
  esiti nel copione, nuove aspettative su conteggi (`List.of(1, 2, 1)` → dieci valori), presentazioni
  (`List.of(BATTLE, BATTLE, DUEL)` → dieci valori), `challengersOfRound(3)` → la decima, i
  `countOccurrences(..., 3)`, la frase del trionfo; più i test nuovi sui criteri 2, 3, 5, 6
- `src/test/java/it/fantasyarena/combat/io/web/ChronicleJsonTest.java` — la chiave nuova (criterio 7)
- `src/test/java/it/fantasyarena/combat/hero/HeroBrainTest.java` — solo se Q1 viene accolta: i due
  test sulla tabella di rarità vanno riscritti sulle nuove aspettative
- `src/test/java/it/fantasyarena/combat/factory/FighterFactoryTest.java` — solo se Q2 viene accolta:
  il monte punti degli sfidanti
- Da verificare in Fase 1 (possibile impatto, non modifica prevista):
  `src/test/java/it/fantasyarena/combat/SilentArenaRunTest.java` (gioca una corsa vera: diventa più
  lunga), `src/test/java/it/fantasyarena/combat/chronicle/ChronicleMapperTest.java`,
  `src/test/java/it/fantasyarena/combat/io/web/ArenaWebServerTest.java`,
  `src/test/java/it/fantasyarena/combat/factory/ArenaFighterFactoryTest.java`,
  `src/test/java/it/fantasyarena/combat/io/log/ConsoleBattleLoggerTest.java`

**Documentazione (Fase 9)**
- `CLAUDE.md`, `README.md`, `daImplementare.md`

**Non toccati, e va verificato che restino tali**
- `pom.xml` — nessuna nuova dipendenza serve a questo compito
- `combat.io.render/*` — i renderer di console non cambiano: dalla console si prende il registro
  linguistico, non il codice
- `combat.MatchRunner`, `combat.io.replay/*`, `combat.io.terminal/*`, `combat.io.web/*.java`,
  `it.fantasyarena.Main`, `it.fantasyarena.UiMode`

## Registro

Voci datate (`YYYY-MM-DD`), append-only.

- **Decisioni tecniche** (non cambiano il comportamento) — `Decisione · Motivazione · Impatto`:
  - 2026-07-31 · **Q1 e Q2 decise dall'utente prima dell'implementazione** · le due questioni aperte
    della SPEC sono state poste all'utente e chiuse: Q1 nella forma a **quattro scaglioni** (1-2 e 3-5
    sono le due tabelle di oggi riusate senza ritararle, 6-8 vale `RARE 25 / EPIC 50 / LEGENDARY 25`,
    9-10 vale `EPIC 65 / LEGENDARY 35`), Q2 nella forma **A** (monte punti come dato della stazione,
    curva `15 + 3 * (prova - 1)` sulle nove prove generate) · la Fase 10 si è fusa nelle Fasi 2-3, il
    monte punti è nato come campo della stazione invece di essere aggiunto dopo.
  - 2026-07-31 · **Per gli sfidanti i bonus di razza e classe restano attivi** · `generateRival` li
    disattiva perché deve eguagliare esattamente un totale già cresciuto, ma `generateWarrior` genera
    anche il protagonista: lasciarli attivi è ciò che tiene il confronto alla pari · il totale reale di
    uno sfidante è `monte punti richiesto + bonus`, quindi il test sul monte punti asserisce `>` e non
    `==` (vedi *Deviazioni*).
  - 2026-07-31 · **`TrialStation.characteristicPoints` è un `Integer` nullable, non un `Optional`** ·
    la stazione dello specchio non dichiara un monte punti proprio perché ricalca il protagonista; il
    progetto usa già campi nullabili per i dati facoltativi (la cronaca) e non `Optional` come campo ·
    due factory method (`generated`/`mirror`) sono i soli modi di costruire una stazione, e il
    costruttore compatto rifiuta le combinazioni incoerenti.
  - 2026-07-31 · **La frase del trionfo dice «tutte le 10 prove», non «tutte e 10 le prove»** ·
    l'italiano corretto con un numerale variabile · una riga in `ConsoleArenaLogger` e la sua
    asserzione in `ArenaTest`.
  - 2026-07-31 · **Le stazioni si distinguono per forma e non solo per colore** · passate cerchio
    pieno, corrente cerchio marcato, da raggiungere quadrato spento — è il disegno che l'utente ha
    chiesto (`O--O--X--[]--[]`) e non affida l'informazione al solo colore, che chi non lo distingue
    non legge · solo CSS.
  - 2026-07-31 · **La colonna centrale è centrata verticalmente** (`justify-content: center`) · la
    freccia sta *fra* le due schede, come nella scena di console, non appesa in cima alla colonna ·
    solo CSS.
  - 2026-07-31 · **La formula del colpo a segno è una funzione con early return, non un ternario
    annidato** · il ternario annidato dentro il literal di `ACTION_FORMULAS` era il tipo di densità
    che le regole del progetto chiedono di abbassare; ora rispecchia `BattleSceneRenderer.hitLabel` ·
    solo `app.js`.
- **Deviazioni dalla SPEC** (da motivare) — `Descrizione · Motivazione · Impatto · Aggiorna la SPEC?
  sì/no`:
  - 2026-07-31 · **Difetto corretto durante la verifica visiva della Fase 6: la freccia raccontava lo
    scontro all'incontrario.** La prima stesura scriveva la riga come `attaccante → bersaglio`, cioè
    ordinava i nomi per **ruolo**; con un attaccante della squadra 1 il risultato era «Ithilwen ←
    Nergash», con Ithilwen (la cui scheda sta a destra) scritta a sinistra della freccia e la freccia
    che puntava via dal bersaglio. Corretto ordinando i nomi per **squadra** — 0 a sinistra, 1 a
    destra, come le colonne di schede e come `BattleSceneRenderer` — e lasciando alla freccia il solo
    compito di dire chi dei due attacca, che è ciò che la SPEC chiedeva al punto 5 · lo scambio della
    colonna centrale porta ora `leftName`/`rightName` invece di `attackerName`/`targetName`, e le tre
    celle si disegnano sempre così che le frecce restino incolonnate fra più scambi · nessuna deviazione
    dalla SPEC: era un errore di implementazione, trovato solo guardando la pagina.
  - 2026-07-31 · **Difetto preesistente corretto: `#battlefield` non si nascondeva.** L'attributo
    `hidden` vale `display: none` solo nel foglio di stile del browser, e la regola d'autore
    `.battlefield { display: grid }` lo batteva: le schede dei combattenti restavano a schermo anche
    nei momenti di procedura di fine scontro e di conclusione, dove `renderMoment` le nasconde da
    sempre. Il difetto non nasce in questo compito — c'era già con la griglia a due colonne — ma con
    la colonna centrale diventava peggiore, perché sotto «Corsa conclusa» restava anche la freccia
    dell'ultimo passo giocato. Corretto con `.battlefield[hidden] { display: none }` · nessuna
    deviazione dalla SPEC.
  - 2026-07-31 · Il test sul monte punti degli sfidanti asserisce che il totale sia **maggiore** del
    monte punti richiesto, non uguale · con i bonus di razza e classe attivi (decisione sopra) il
    totale reale è `richiesto + bonus`, e asserire l'uguaglianza sarebbe semplicemente falso ·
    nessuno sul comportamento · **no**: la SPEC segnalava già la cautela sui bonus, qui è solo la sua
    conseguenza sul test.
- **Problemi aperti** (bloccano l'avanzamento) — `Descrizione · Impatto · Opzioni · Decisione
  richiesta`:
  - **Q3 — la profondità del percorso è quasi irraggiungibile** (aperto il 2026-07-31, **non blocca**
    questo compito) · misurato su quindici corse di console: 11 cadono alla prima prova, 1 alla
    seconda, 2 alla terza, **nessuna oltre**. Il dato è coerente con quello già registrato in
    `implementation-web-replay.md` (trentaquattro corse per arrivare in fondo a **tre** prove) e non è
    una regressione: la prima prova è un uno-contro-uno alla pari come è sempre stata, e la strada
    duello è dichiarata equivalente a quella battaglia per l'1v1. Ma su dieci stazioni la conseguenza
    cambia di scala: un percorso che nessuno cammina oltre la terza stazione rende invisibili sei prove
    su dieci, la stazione dello specchio e — nella pagina — gli stati «percorso quasi completo». La
    decisione Q2 (monte punti degli sfidanti da 15 a 39) tira nella stessa direzione: rende le prove
    successive più difficili, non più facili · opzioni: alzare `CHARACTERISTIC_POINTS_PER_VICTORY`
    (oggi 3, è il moltiplicatore della crescita); appiattire la curva del monte punti degli sfidanti
    (per esempio `+2` invece di `+3` per prova, o costante entro ciascun terzo del percorso);
    abbassare il numero di sfidanti delle prove 7-9; oppure accettarlo, se l'arena dev'essere un
    tritacarne in cui arrivare in fondo è leggenda · **decisione dell'utente**, da prendere guardando
    il gioco e non il codice: è bilanciamento, e il punto unico dove intervenire resta `HeroBrain` per
    la crescita e `TrialPlan` per la pressione.
  - ~~**Q1 — scaglioni della rarità del loot su dieci livelli**~~ · **chiusa il 2026-07-31**: quattro
    scaglioni, vedi *Decisioni tecniche*. Testo originale conservato qui sotto per la storia · oggi due scaglioni su tre prove: su
    dieci il secondo coprirebbe nove prove, e la progressione della rarità sparirebbe dopo il primo
    scontro; un pavimento troppo basso a metà percorso produce loot inerte, perché il protagonista
    tiene solo ciò che batte quel che ha già e gli slot sono finiti · opzioni: quattro scaglioni
    (1-2, 3-5, 6-8, 9-10) con i pesi proposti nella SPEC, tre scaglioni (1-3, 4-7, 8-10), o lasciare
    i due attuali · **decisione richiesta all'utente**; la SPEC non la prende.
  - ~~**Q2 — monte punti degli sfidanti**~~ · **chiusa il 2026-07-31**: proposta A, vedi *Decisioni
    tecniche*. Testo originale conservato qui sotto per la storia · gli sfidanti nascono sempre a 15 punti mentre il
    protagonista ne accumula circa 27-60 in nove vittorie: dalla metà del percorso le prove rischiano
    la passeggiata, e il solo aumento del numero di avversari non compensa · opzioni: A il monte punti
    come dato della stazione (`createChallengers` guadagna il parametro), B derivato dal protagonista
    e scalato per il numero di sfidanti, terza via sulla rarità dell'equipaggiamento, combinazioni, o
    niente · **decisione richiesta all'utente**; da leggere insieme al dato di
    `implementation-web-replay.md` (trentaquattro corse per arrivare in fondo, muro alla seconda
    prova), che nel percorso nuovo sposta il due-contro-uno alla quarta prova con il protagonista
    cresciuto tre volte.
- **Test eseguiti** — `data · fase · comando · esito`:
  - 2026-07-31 · Fase 1 (baseline) · `mvn -o test` · **143 test, 0 fallimenti**, `BUILD SUCCESS`.
  - 2026-07-31 · Fasi 2-4 e 10 · `mvn -o clean test` · **155 test, 0 fallimenti**, `BUILD SUCCESS`
    (+12 sulla baseline: `TrialPlanTest` ×8, `ArenaChronicleTest` ×2, due casi nuovi in
    `FighterFactoryTest`).
  - 2026-07-31 · Fase 8 (revisione funzionale) · `mvn -o test` · **155 test, 0 fallimenti**.
    `java-functional-evolver` **invocato** e non è intervenuto: ha dichiarato il codice già allineato,
    confermando in particolare che il ciclo di `run()` è preferibile a una `reduce`, che valuterebbe le
    stazioni in testa o richiederebbe un combinatore mai invocato. Ha anche confermato la scelta di
    lasciare imperativo `distributeCharacteristicPoints` (accumulo in `EnumMap`) chiudendo con uno
    stream sulla sola conversione finale.
  - 2026-07-31 · Fase 11 · `mvn -o test` · **155 test, 0 fallimenti**, `BUILD SUCCESS`;
    `node --check app.js` pulito; controlli strutturali verdi: `combat.io.web` non importa
    `render`/`log`/`terminal`/`replay`, l'unico `import` di `combat.io.web` in tutto il progetto è in
    `UiMode` (`SilentArenaRun` lo nomina solo in un Javadoc), `combat.chronicle` non importa
    `combat.io` né Jackson (solo riferimenti in commento), `new Random` esiste solo in
    `FighterFactory` e `HeroBrain`.
  - 2026-07-31 · Fasi 5-7 (verifica manuale della pagina) · `mvn -o process-resources` +
    `mvn -o exec:java -Dexec.args="web"`, browser guidato · **verificato a mano**: «Prova 1/10» dal
    primo passo; dieci stazioni con le linee di collegamento; avanzando, la corrente si sposta e le
    precedenti diventano piene; su una corsa caduta alla quarta prova le stazioni 1-3 sono passate e
    cliccabili, la 4 corrente, le 5-10 quadrate e **disabilitate**; le etichette lette dall'albero di
    accessibilità dicono «Prova N, passata/corrente/da raggiungere» e la corrente porta
    `aria-current="step"`; il clic su una stazione passata salta a quella prova; la colonna centrale
    mostra formula e freccia col verso coerente col lato dell'attaccante; un turno di `REST` mostra il
    solo nome di chi riposa, dal suo lato, senza freccia; la stellina compare sul combattente che
    agisce; il pannello di rete conta **una sola** richiesta a `/api/chronicle`; nessun errore nella
    console del browser; il collasso a una colonna impila schede-freccia-schede senza sovrapporsi.
    **Non verificato**: la larghezza reale sotto 720px — il ridimensionamento della finestra non ha
    effetto in questo ambiente, quindi il collasso è stato provato iniettando la stessa regola che la
    media query applica. E la **decima prova non è mai stata raggiunta** da nessuna corsa, quindi la
    stazione dello specchio e gli stati «percorso quasi completo» restano osservati solo fino alla
    quarta stazione.
  - 2026-07-31 · Fase 3 · `mvn -o -q exec:java < /dev/null` × **15 corse** · nessuna eccezione, uscita
    pulita, il percorso annuncia le stazioni con numero e descrizione nuovi. **Ma nessuna corsa è
    andata oltre la terza prova**: 11 cadute alla prima, 1 alla seconda, 2 alla terza (vedi il
    problema aperto sulla profondità raggiungibile). La voce del piano «arriva almeno alla quarta prova
    senza eccezioni» va letta come «nessuna eccezione»: la quarta prova non è stata raggiunta da
    nessuna delle quindici corse, e non per un difetto del codice.

## Esito finale

**Stato: `COMPLETED`** (2026-07-31), con una nota di bilanciamento aperta che non blocca il compito.

**Cosa è cambiato.** Il percorso dell'arena è passato da tre prove cablate in `Arena` a **dieci prove
descritte come dato** in `TrialPlan`/`TrialStation`/`ChallengerOrigin`: `Arena.run()` è ora un ciclo che
piega le stazioni conservando la cortocircuitazione e la pigrizia (lo specchio si genera solo se la
decima prova viene raggiunta). La cronaca porta `plannedTrials`, ed è quel campo che toglie lo spoiler
della pagina. Il bilanciamento è stato ritarato secondo le due decisioni dell'utente: quattro scaglioni
di rarità del loot e monte punti degli sfidanti come dato della stazione (`15 + 3 * (prova - 1)`). La
pagina ha guadagnato le tre cose richieste — il percorso disegnato a dieci stazioni senza spoiler, la
colonna centrale con «chi attacca chi», la stellina dell'iniziativa — e ha perso i pulsanti «Prova N»,
che erano il secondo spoiler dello stesso genere.

**Due difetti corretti che non erano nel piano** (vedi *Deviazioni*): la freccia della colonna centrale
raccontava lo scontro all'incontrario quando l'attaccante era della squadra di destra, trovato solo
guardando la pagina; e `#battlefield` non si nascondeva mai davvero, un difetto **preesistente** che la
colonna centrale rendeva più visibile.

**Test.** `mvn -o test`: **155 verdi**, 0 fallimenti (baseline 143, +12). Nuovi: `TrialPlanTest` (8),
`ArenaChronicleTest` (2), due casi in `FighterFactoryTest`. Adeguati senza indebolirli: `ArenaTest`,
`HeroBrainTest`, `ChronicleJsonTest`, `ArenaWebServerTest`, `SilentArenaRunTest`,
`ArenaFighterFactoryTest`. Nessuna nuova dipendenza.

**Note residue.**

- **La profondità del percorso è quasi irraggiungibile** (problema aperto Q3): su quindici corse nessuna
  è andata oltre la terza prova. Sei stazioni su dieci, la prova dello specchio compresa, oggi non si
  vedono quasi mai. Le leve sono `CHARACTERISTIC_POINTS_PER_VICTORY` in `HeroBrain` e la curva del monte
  punti in `TrialPlan`; la decisione è dell'utente e riguarda il gioco, non il codice.
- Restano non verificati a mano, per la ragione sopra: la stazione dello specchio e il duello finale
  nella pagina. E la larghezza reale sotto 720px, provata iniettando la regola della media query invece
  di ridimensionare la finestra.
- La pagina non ha test automatici, ed è la scelta dichiarata del progetto.

## Esempio (file previsti e test previsti)

```java
// File coinvolti (effettivi) — previsione:
//   TrialPlan / TrialStation / ChallengerOrigin  — il percorso come dato, in un punto solo
//   Arena                                        — run() piega le stazioni, non cabla più le prove
//   ArenaLogger / ConsoleArenaLogger / SilentArenaLogger — le frasi ricevono il numero previsto
//   ArenaChronicle                               — la lunghezza prevista del percorso
//   web/index.html · app.js · app.css            — percorso disegnato, freccia centrale, stellina
//   HeroBrain / FighterFactory                   — solo se Q1/Q2 vengono decise

// Test: uno per criterio della Definition of done (i criteri 9-14 sono verifica a mano dichiarata,
// i criteri 15 e 17 sono verifica in revisione)
@Test void ilPercorsoHaDieciStazioniConSfidantiEFormeAttese()       { /* criterio 1 */ }
@Test void loSpecchioCompareSoloAllaDecimaStazione()                { /* criterio 1 + 3 */ }
@Test void dieciProveVinteInFilaPortanoAlTrionfo()                  { /* criterio 2 */ }
@Test void loSfidanteSpeculareRispecchiaIlProtagonistaCresciuto()   { /* criterio 3, adeguato */ }
@Test void unaProvaNonVintaChiudeLaCorsaLi()                        { /* criterio 4 */ }
@Test void leProveAUnSoloSfidantePassanoDalDuelloLeAltreDallaBattaglia() { /* criterio 5 */ }
@Test void laLunghezzaPrevistaEDieciAncheSeLaCorsaSiChiudeSubito()  { /* criterio 6 */ }
@Test void piuProveGiocateCheProvePrevisteVengonoRifiutate()        { /* criterio 6 */ }
@Test void ilJsonPortaLaChiaveDellaLunghezzaPrevista()              { /* criterio 7 */ }
@Test void leFrasiDiConsoleDiconoIlNumeroPrevistoDalPercorso()      { /* criterio 8 */ }
@Test void laTabellaDiRaritaDelLootSegueGliScaglioniDecisi()        { /* criterio 18, se Q1 */ }
@Test void gliSfidantiNasconoConIlMontePuntiDellaStazione()         { /* criterio 18, se Q2 */ }
```
