# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Cos'è questo progetto

`fantasy-arena` è il **gioco**: applicazione Java 21 (Maven) che genera i combattenti, ne dispone lo
scontro e lo mostra. **Le regole di combattimento non vivono qui**: stanno nella libreria
`fantasy-combat-system`, usata come dipendenza Maven, che calcola l'intero scontro e restituisce il
log completo senza stampare niente. Questo repository possiede generazione, orchestrazione,
progressione e presentazione.

**Due modalità di presentazione, una sola partita.** La stessa corsa si può guardare in console, come
da sempre, oppure in un browser, con i controlli che un terminale non può avere: play, pausa, passo
avanti e indietro, velocità, salto a un passo, e il **percorso disegnato** a dieci stazioni — che è
insieme la mappa di dov'è arrivata la corsa e il comando per rivedere una prova già camminata. La
modalità si sceglie con un argomento di
riga di comando: nessun argomento → console, `web` → server. Regge tutto un fatto: **il gioco non ha
scelte del giocatore**, quindi una partita è completamente determinata nell'istante in cui viene
giocata, e la UI non pilota un motore turno per turno — legge una cronaca già scritta. Da qui seguono
`ArenaChronicle` (la partita in forma di dati) e le presentazioni sostituibili.

Per la descrizione discorsiva del gioco e del confine col motore vedi `README.md`.

Il punto d'ingresso è `it.fantasyarena.Main`, ridotto all'osso: costruisce i `CombatSettings`, chiede a
`UiMode.fromArgs` quale modalità è stata richiesta e le lascia avviarsi. Non conosce né `Arena` né il
server. `Arena` genera un protagonista e gli fa affrontare **dieci prove in fila**, con la
**procedura di fine scontro** fra l'una e l'altra: cura completa, un oggetto di loot, tre punti
caratteristica. Il percorso non è più cablato nella scansione: vive come **dato** in `TrialPlan`, una
tabella di dieci stazioni che dice per ognuna quanti sfidanti la popolano, con quale monte punti
nascono e come si generano. Un avversario solo alle prove 1-3, due alle 4-6, tre alle 7-9, e alla
decima lo sfidante speculare con arma rara. Il monte punti che una stazione dichiara è quello
dell'**intero schieramento**, non del singolo sfidante, e `FighterFactory` lo ripartisce fra loro a
parti uguali col resto ai primi: `15, 18, 21, 31, 35, 39, 50, 54, 59`. La curva è
`monteEroe(N) × moltiplicatore`, con `monteEroe(N) = 15 + 3 * (N - 1)` — la stessa crescita del
protagonista — e moltiplicatore `1.0` con un sfidante, `1.3` con due, `1.5` con tre. È **inferiore al
numero puro di sfidanti** perché sconta l'economia di azioni: `N` avversari attaccano `N` volte per
turno mentre il protagonista attacca una volta sola, quindi a parità di monte complessivo lo
schieramento numeroso vincerebbe comunque. Prima il monte era **per singolo sfidante** e seguiva la
stessa curva della crescita del protagonista: i tre punti della vittoria erano così neutralizzati per
costruzione, e moltiplicati per il numero di avversari. Su quel monte si applica poi lo sconto della
**fortuna** (`ChallengerBudget`, `fortuna × numeroSfidanti`, col pavimento di sette punti per
sfidante che il toolkit impone): è la sola cosa che dà peso a una caratteristica che nel motore vale
un punto percentuale di critico e nient'altro. Il loot non si
saccheggia dai caduti: a ogni livello vinto se ne genera **uno solo**, di tipo estratto a caso fra
arma, armatura e gioiello, con una rarità estratta da una tabella pesata che si fa più generosa col
livello (quattro scaglioni: prove 1-2, 3-5, 6-8, 9-10). Il pavimento **non sale a ogni scaglione**:
`UNCOMMON` resta estraibile fino alla prova 5 e sale a `RARE` solo dalla 6, perché un'arma
`LEGENDARY` ha attacco 15-25 contro 3-6 di una `UNCOMMON` e porta buff per una decina di punti,
mentre una vittoria ne vale tre — un solo drop leggendario valeva più di tre vittorie di
progressione. La distribuzione è pesata e non uniforme di proposito: una semplice rarità minima renderebbe
il `LEGENDARY` tanto probabile quanto il grado del pavimento, e il loot sopra il raro diventerebbe la
norma invece dell'eccezione. Arma, armatura e gioiello si tengono solo se battono quel che il protagonista ha già — il
gioiello uno per tipo, come l'armatura uno per slot, e per lui il criterio è il **valore totale dei
suoi buff**, con la rarità come spareggio: la stessa forma dei comparatori di arma e armatura, che
restano su attacco e difesa. Ogni oggetto generato porta i **buff del toolkit**, e i buff di ciò che è
equipaggiato si sommano alle caratteristiche **nell'istante in cui il combattente viene assemblato**:
al `FighterAssembler` vanno le caratteristiche effettive, così i bonus contano davvero nello scontro
senza che il motore sappia che esistano. Ne segue che valgono **finché l'oggetto è addosso** —
sostituirlo ne sostituisce i bonus — e che il gioiello conta pur restando non montabile dal motore. Il
gioiello non vale più punti caratteristica di suo: la vittoria vale i suoi tre punti e basta. Vale per
tutti, sfidanti e specchio compresi. La forma dello scontro **si deriva**
dal numero di sfidanti e non è un campo della stazione: uno-contro-uno → duello a schermate, più di
uno → battaglia NvN. Quindi in console le prove 1-3 e la decima passano dal duello, le prove 4-9 dalla
battaglia. Se un giorno servisse una battaglia contro un avversario solo — come avveniva quando le
prove erano tre — la forma tornerebbe a essere un campo, e sarà una decisione da prendere allora.

`Arena.run()` **restituisce** l'`ArenaChronicle` della corsa, e la costruisce **sempre**, in entrambe le
modalità: è la stessa partita letta due volte, e non esiste un percorso «per il web» che possa divergere
da quello della console. Costruirla anche quando nessuno la legge costa una lista di record per partita
ed elimina la classe di bug in cui le due presentazioni raccontano cose diverse.

## Comandi

```bash
mvn compile                              # compila
mvn test                                 # esegue i test JUnit 5 (surefire)
mvn exec:java                            # gioca in console (mainClass da pom.xml, exec.mainClass)
mvn exec:java -Dexec.args="web"          # avvia il server sulla porta 8080 e stampa solo l'indirizzo
mvn exec:java -Dexec.args="web 9000"     # come sopra, su una porta scelta
mvn package                              # produce il jar in target/
```

Per la modalità web ci sono due script PowerShell nella radice, che sono il modo consigliato di avviarla
perché fanno da soli le due cose che a mano si dimenticano (vedi le trappole qui sotto):

```powershell
.\start-web.ps1                          # rigenera le risorse, avvia staccato, attende la porta
.\start-web.ps1 -Port 9000               # su una porta scelta
.\stop-web.ps1                           # ferma chi ascolta sulla porta 8080
.\stop-web.ps1 -Port 9000                # la porta va ripetuta anche allo stop
```

`start-web.ps1` restituisce il prompt e manda l'output del server in `target/web-server.log`, dove si
leggono gli errori se la porta non si apre; rifiuta di partire se la porta è già occupata, dicendo quale
PID la tiene. `stop-web.ps1` è idempotente: fermare ciò che è già fermo esce con successo. Sono UTF-8
**con BOM**, che è ciò che rende leggibili gli accenti a Windows PowerShell 5.1: se li riscrivi senza
BOM, i commenti e i messaggi si corrompono.

In modalità web su console va **soltanto** l'indirizzo da aprire: la partita si guarda nel browser, e
ogni ricarica della pagina è una partita nuova. La porta è opzionale e vale `8080` per difetto; se è già
occupata l'avvio **fallisce con un messaggio esplicito** che la nomina, invece di cercarne una libera in
silenzio — l'indirizzo stampato deve restare prevedibile. Un argomento non riconosciuto è un errore che
elenca le modalità ammesse, non un ripiego silenzioso sulla console.

Attenzione a **due** trappole quando si prova a mano — sono la ragione per cui i due script esistono.

La prima: **`exec:java` non aggiorna le risorse della pagina.** La pagina si serve dal classpath
(`target/classes/web/`), e `exec:java` da solo non ricopia `src/main/resources/web/`. Chi modifica
`index.html`, `app.js` o `app.css` e riavvia senza `mvn process-resources` (o `mvn compile`) guarda la
versione vecchia e non capisce perché la modifica «non c'è». Il sintomo è insidioso perché il server
risponde normalmente: serve solo un file diverso da quello che hai appena scritto.

La seconda: **fermare il processo Maven non uccide la JVM che ha
generato**, che resta in ascolto e continua a tenere la porta. Se una verifica sul server dà un
risultato inspiegabile, controllare *chi* ascolta (`netstat -ano | grep 8080`) e chiudere quel PID:
altrimenti si sta interrogando il processo vecchio con le classi di prima.

## Dipendenze critiche: due SNAPSHOT locali

Nessuna delle due è su Maven Central: sono risolte solo dal repository Maven locale
(`~/.m2/repository/...`) e vanno buildate e installate (`mvn install`) dai rispettivi progetti
sorgente prima di poter compilare qui. Se `mvn compile` fallisce con artifact non risolto, il
problema è quasi sempre questo, non il codice di `fantasy-arena`.

La terza dipendenza di produzione, `com.fasterxml.jackson.core:jackson-databind`, **è** su Maven Central
e non ha niente di critico: serve solo a serializzare la cronaca in JSON, vive confinata in
`combat.io.web` e non compare in nessun altro package. Regola dichiarata: **nessun modulo Jackson
aggiuntivo**. In particolare, se nel JSON servisse un `Optional`, la risposta è cambiare il dato, non
aggiungere `jackson-datatype-jdk8` — ed è il motivo per cui i campi facoltativi della cronaca sono tipi
nullabili e non `Optional`.

### it.fantasycombatsystem:fantasy-combat-system

- Il motore di combattimento, in `C:/build/git/fantasy-combat-system`. Superficie pubblica ridotta a
  due punti d'ingresso: `CombatSystem` (`duel`, `battle`) e `FighterAssembler` (dalla materia prima
  del toolkit al `Fighter` pronto), più i tipi `result`/`battle` che compongono il log.
- **Ogni modifica al motore richiede `mvn install` là prima che qui si veda.** Se stai tarando pesi o
  formule, lavora in quel repository con i suoi test, non replicando la logica qui.
- Il suo `test-jar` è usato in scope test: `it.fantasycombatsystem.testsupport.CombatFixtures` e
  `StubDiceRoller` servono ai test dei renderer per costruire `Fighter` deterministici. Non
  duplicarli qui.
- Non reintrodurre logica di combattimento in questo repository. Se serve una regola nuova, va nel
  motore; se serve un dato nuovo per la presentazione, va aggiunto ai tipi `result` del motore.

### it.fantasytoolkit:fantasytoolkit

- Fornisce la materia prima: personaggi, dadi, armi, armature, nomi.
- Essendo `SNAPSHOT`, l'API può cambiare: verifica sempre le firme reali invece di assumerle.
- Per l'uso del toolkit consulta la documentazione indicizzata da
  `C:/build/git/fantasy-game-toolkit/docs/agent/INDEX.md` nel repository della versione utilizzata.
  **Non leggere i sorgenti e non decompilare il JAR.** Se l'API necessaria non è documentata, segnala
  la lacuna.

### Pattern d'uso dei tool del toolkit

Ogni funzionalità è esposta come un **Tool** con fluent builder che parte da un metodo statico
`building()`, si configura con chiamate concatenate e termina con un metodo terminale che restituisce
un *result*:

```java
CharacterResult character = CharacterGeneratorTool.building()
        .randomRace().characterClass(CharacterClass.WARRIOR)
        .allCharacteristics().totalPoints(15)
        .generate();                       // terminale
```

I tipi `*Result` (package `.result`) sono record-like: accessor senza prefisso `get`
(`character.name()`, `weapon.attack()`). Nel repo il consumo dei tool è concentrato in
`FighterFactory`: quando serve un nuovo tool, aggiungilo lì seguendo lo stesso schema
`building() → config → terminale → lettura del result`, non sparso nel resto del gioco.

Tool della libreria: `charactergenerator`, `dicelauncher`, `namegenerator`, `weapongenerator`,
`armourgenerator`, `jewelgenerator`, `potiongenerator`, `buffdebuffgenerator`, `dungeongenerator`.
Modelli ed enum condivise in `it.fantasytoolkitcore.core.model` (`Race`, `CharacterClass`, `Rarity`,
`Weapon`, `Armour`, ...).

## Architettura

| Package | Ruolo |
| --- | --- |
| `it.fantasyarena` | `Main`: thin. Chiede la modalità a `UiMode` e la lascia avviarsi. `UiMode`: **unico punto di parsing** degli argomenti, con due record annidati (`ConsoleMode` senza dati, `WebMode` con la porta) e `launch` implementato da entrambi — `Main` non discrimina il tipo a runtime, chiede alla modalità di avviarsi. |
| `combat` | `TrialPlan`: **il percorso come dato**, dieci `TrialStation` in fila. Ogni stazione porta numero, descrizione, quanti sfidanti e la loro `ChallengerOrigin` (`GENERATED`/`MIRROR`, enum e non booleano: una terza origine deve fermare la compilazione); la **forma** dello scontro non è un campo ma si deriva dal numero di sfidanti, e il monte punti c'è solo per gli sfidanti generati — lo specchio ricalca il protagonista, quindi la sua stazione non lo dichiara. Quel monte è quello dell'**intero schieramento**, non del singolo: `FighterFactory` lo ripartisce. Insieme a `ChallengerBudget` è il punto da toccare per allungare o ribilanciare la pressione del percorso. `ChallengerBudget`: il monte di squadra **calato su questo protagonista** — monte dichiarato, sconto della sua fortuna (`fortuna × numeroSfidanti`, letta dalle caratteristiche **effettive**), monte effettivo, col pavimento di sette punti per sfidante che il toolkit impone. Vive qui e non in `HeroBrain` perché uno sconto sugli avversari non è una **scelta** del protagonista: è la pressione del percorso calata su di lui. `Arena`: la progressione. Scandisce le stazioni del percorso e la procedura di fine scontro; non decide niente e non calcola niente, e in più **registra**: `run()` restituisce l'`ArenaChronicle`. La scansione è un ciclo che concatena `RoundReport.andThen` stazione per stazione: resta **pigra**, così gli sfidanti di una prova si generano solo se la precedente è stata vinta e `createMirrorRival` si invoca una volta sola, quando la decima prova viene davvero raggiunta. Ogni prova le restituisce un rapporto (`RoundReport`, record privato: se procedere, la scheda cresciuta e le voci di cronaca accumulate) invece di un `Optional`, e i rapporti si concatenano cortocircuitando alla **caduta** — l'accumulo vive nel rapporto e non in un campo, che resterebbe sporco fra due `run()` sulla stessa istanza. Il segnale di via libera si chiama `continues` e non `passed` di proposito: significa «si gioca la prova successiva», che è vero anche dopo un pareggio, non «si è vinto». `RoundOutcome`: com'è finita una prova (`WON`/`FELL`/`STOOD_WITHOUT_WINNING`), calcolato una volta sola e passato a chi lo deve raccontare. Solo `FELL` chiude la corsa: **il pareggio fa proseguire senza premio**, cioè senza loot e senza i tre punti, e la scheda passa alla prova dopo esattamente com'era. Ne segue che il trionfo si dichiara leggendo l'esito dell'ultima prova giocata, non dal fatto che la catena non si sia interrotta — che ora è vero anche di una corsa arrivata in fondo pareggiando. `MatchRunner`: fa giocare **un singolo scontro**, lo consegna a chi lo deve mostrare e ne **restituisce l'esito** (`CombatResult`/`BattleResult`); il *come* e il *quando* mostrarlo sono di un `MatchPresentation` sostituibile. `SilentArenaRun`: un `Supplier<ArenaChronicle>` che gioca l'intera arena senza stampare e senza attendere, assemblando da zero i collaboratori a ogni `get()` — è l'unico posto che può conoscere sia il mondo della cronaca sia quello di `combat.io`. |
| `combat.hero` | Il protagonista: `Hero` (scheda immutabile che sopravvive ai round: arma, armatura per slot, gioielli per tipo; `character()` resta la scheda **base**, `effectiveCharacter()` è la stessa coi buff dell'equipaggiamento addosso — **dato derivato risolto alla lettura**, mai custodito accanto alle fonti), `EquipmentBonus` (il **punto unico** che somma i buff alle caratteristiche base: lo usano sia `combat.factory`, per assemblare il combattente, sia `combat.chronicle`, per fotografare il protagonista, e nessuno dei due deve dipendere dall'altro), `HeroBrain` (**tutte** le sue scelte, compresi i tre comparatori di cernita e i quattro scaglioni di rarità del loot lungo i dieci livelli), `Loot` (l'unico oggetto trovato a fine livello: arma, armatura o gioiello, mai più di uno), `HeroProgress` (resoconto della crescita, dati e non stringhe). |
| `combat.chronicle` | **La partita in forma di dati**, e niente altro: `ArenaChronicle` (ingresso del protagonista, **quante prove prevedeva il percorso** (`plannedTrials`), una voce per ogni prova giocata, conclusione — la lunghezza prevista esiste perché il lettore ha solo il JSON e non può contare le stazioni di una tabella Java: senza di essa dovrebbe dedurla dal numero di voci giocate, che è precisamente lo spoiler da evitare), `TrialChronicle` con `TrialShape` (`BATTLE`/`DUEL`) e i passi nella forma che il motore ha prodotto — `rounds` per la battaglia, `turns` per il duello — `HeroSnapshot` (caratteristiche **base ed effettive**, così nessun lettore ricalcola una regola di gioco per mostrare il contributo dell'equipaggiamento)/`CombatantSnapshot` (le sue caratteristiche sono già quelle in campo: il `Fighter` nasce con le effettive, e l'asimmetria è voluta)/`ItemSnapshot` (con i `CharacteristicBonus` che l'oggetto porta, lista vuota se non ne ha), `ProgressChronicle`, `RunConclusion`, più `ChronicleMapper`, unico punto che traduce `Fighter` e `Hero` nelle loro fotografie. **Nessuna stringa di presentazione**, nessuna annotazione Jackson, nessun import da `combat.io` in nessuna direzione. |
| `combat.io.web` | Il secondo lettore della cronaca: `ArenaWebServer` (`HttpServer` del JDK, **solo loopback**, porta parametrica), `ChronicleHandler` (il fornitore della cronaca arriva **iniettato** come `Supplier` e viene invocato una volta per richiesta: ogni apertura della pagina è una partita nuova, e nessuno stato viaggia fra due richieste), `StaticResourceHandler` (lista **chiusa** di tre nomi letterali), `ChronicleJson` (l'unico `ObjectMapper`). Le risorse della pagina stanno in `src/main/resources/web/`. |
| `combat.factory` | `FighterFactory`: unico punto di contatto coi generatori del toolkit. Decide chi combatte e con che equipaggiamento, poi delega l'assemblaggio al `FighterAssembler` del motore. Genera anche il protagonista, gli sfidanti di ogni round col monte punti che la stazione dichiara (`createChallengers(count, totalCharacteristicPoints)`) e lo specchio finale, materializza il `Fighter` di ogni round da una `Hero` (`summon`) ed estrae il loot di fine livello (`rollLoot`: tipo a caso, rarità estratta dalla tabella pesata ricevuta dal cervello). |
| `combat.io` | Solo contenitore: la presentazione vive nei quattro sotto-package che seguono, disposti a strati con dipendenze a senso unico (`replay` → `log` → `render`, e sia `replay` sia `log` → `terminal`). Nessuna classe sta direttamente qui. |
| `combat.io.render` | Righe di testo pure, nessun I/O: `TurnLogFormatter` (il turno), `FighterCardFormatter` (la scheda del combattente), `BattleSceneRenderer` (scena ASCII NvN) col suo `FighterProfile`, `CombatScreenRenderer` (la pagina del duello a schermate), `HeroProgressFormatter` (la procedura di fine scontro). Strato foglia: non dipende da nessun altro sotto-package di `io`. |
| `combat.io.terminal` | Il terminale e basta: `ScreenCleaner` e `ScreenRefresh` (pulizia dello schermo), `TurnPacer`/`EnterKeyTurnPacer` (il ritmo fra un turno e l'altro), `CombatSetupPrompt` (lettura delle preferenze). Strato foglia, indipendente dal `render`: qui vive l'I/O che non ha niente da formattare. |
| `combat.io.log` | Chi stampa: tre interfacce — `CombatLogger` (il duello), `BattleLogger` (la battaglia NvN), `ArenaLogger` (la voce dell'arena fra uno scontro e l'altro) — con le rispettive `Console*`. Compongono le righe chiedendole al `render` e le mandano a schermo. Raccontano la fine della corsa leggendo il `RoundOutcome` che ricevono, non interrogando il `Fighter`. La sola implementazione muta è `SilentArenaLogger`: per duello e battaglia il silenzio lo produce `SilentMatchPresentation`, che tace l'intera presentazione invece di comporre logger che non stampano. |
| `combat.io.replay` | Il ritmo della rivelazione: `MatchPresentation` — il punto in cui si sostituisce *come* uno scontro si mostra — con `ConsoleMatchPresentation` (logger, `CombatReplay`, `TurnPacer`, `ScreenCleaner`, l'attesa di lettura degli schieramenti) e `SilentMatchPresentation`. Più `CombatReplay` con le due strategie `LinearCombatReplay`/`ScreenCombatReplay` e la `ReplayMode` (`LINEAR`/`SCREEN`) che le sceglie. Strato più alto: mette insieme logger, renderer e terminale, e decide solo *quando* mostrare cosa. |

Vincoli architetturali da rispettare quando modifichi:

- **Niente regole di combattimento qui.** Nessun calcolo di danno, iniziativa, stamina o momentum:
  quei numeri arrivano già calcolati dal motore. Se ti trovi a scrivere una formula, sei nel
  repository sbagliato.
- **I renderer producono righe di testo pure, senza I/O.** L'I/O vero (stampa, lettura dell'INVIO,
  pulizia dello schermo) sta nei logger, nel `TurnPacer` e nello `ScreenCleaner`. È questa
  separazione che rende i renderer testabili sul testo prodotto. Dalla suddivisione di `combat.io`
  in sotto-package la regola è anche visibile: un `System.out` dentro `combat.io.render` è fuori
  posto per definizione, e `render` non deve importare niente dagli altri tre. Le dipendenze vanno
  in una direzione sola — `replay` → `log` → `render`, `terminal` sotto tutti — e non vanno chiuse
  in ciclo: se un renderer ha bisogno di sapere qualcosa da un logger, il dato va passato come
  argomento, non importato.
- **`combat.io.web` è un pozzo, come `log`.** Dipende dal JDK, da `com.sun.net.httpserver`, da Jackson e
  da `combat.chronicle`, e **non importa** `render`, `log`, `terminal` né `replay` — e in particolare non
  `SilentArenaRun`: il fornitore della cronaca arriva iniettato come `Supplier`. Simmetricamente, nessun
  package del percorso console importa `web`: l'**unico** importatore di `combat.io.web` in tutto il
  progetto è `UiMode`, che è l'ingresso e deve conoscere entrambe le strade. È questa doppia ignoranza
  che permette di cambiare la pagina senza sfiorare la console e viceversa.
- **La cronaca è di soli dati.** Se ti trovi a mettere una stringa di presentazione dentro un record di
  `combat.chronicle`, quella stringa appartiene a un renderer: ogni lettore compone le proprie frasi. E
  la cronaca deve restare **autosufficiente** — ogni dato che una presentazione ricava oggi per conto
  proprio dai suoi collaboratori (numero e descrizione della prova, composizione degli schieramenti,
  esito, procedura di fine scontro fino ai bonus che ogni oggetto porta) va portato in forma di dati. Il
  criterio di lettura: un lettore nuovo deve poter **comporre frasi, non aggiungere campi**. Un calcolo
  che è **aritmetica** e non regola di gioco può restare al lettore: la pagina ricava il contributo
  dell'equipaggiamento per sottrazione fra caratteristiche effettive e base, ma non le somma da sé.
- **Le risorse statiche si servono da una lista chiusa di nomi letterali**, e il percorso della richiesta
  si confronta per uguaglianza. Non concatenare mai un pezzo di richiesta in un nome di risorsa, nemmeno
  «sanificato»: il traversal qui non è bloccato, è impossibile, e va tenuto impossibile.
- **La pagina non anticipa dove finisce la corsa.** Il percorso disegna una stazione per ogni prova
  **prevista**, e una stazione mai giocata deve restare **indistinguibile** da una non ancora raggiunta:
  stesso aspetto, stesso testo, stessa reazione al mouse, stessa interattività. Ne segue che si può
  cliccare solo fino alla stazione corrente — un insieme di bersagli cliccabili che si fermasse dove
  finisce la corsa sarebbe lo stesso spoiler con un vestito nuovo — e che il codice che disegna una
  stazione futura non deve nemmeno guardare `chronicle.trials`. È anche il motivo per cui il denominatore
  dell'intestazione è `plannedTrials` e non il numero di voci giocate, e per cui i vecchi pulsanti
  «Prova N» non ci sono più. Si verifica **confrontando due partite** di lunghezza diversa, non
  guardandone una. Perdita residua e dichiarata: il denominatore della timeline conta i *momenti*
  registrati, quindi una registrazione corta suggerisce comunque una corsa corta; nasconderla vorrebbe
  dire rinunciare alla timeline.
- **`MatchRunner` non contiene logica di scontro**: chiede il log completo al motore, lo consegna alla
  presentazione e lo restituisce. Il duello 1v1 e la battaglia NvN sono due percorsi di
  **presentazione**, non due motori. Ne servono due istanze quando servono entrambi i percorsi: ognuna
  costruisce alla prima chiamata il proprio `TurnPacer`, e condividerne una sola porterebbe il
  suggerimento sbagliato nell'altro percorso.
- **Una presentazione alternativa è un collaboratore, non un flag.** Niente `ReplayMode.NONE`, niente
  booleano «silenzioso»: sarebbero un modo di dire «presenta» a chi non deve presentare niente. La
  passata muta è un `MatchPresentation` muto più i logger muti, cioè un caso ordinario e non un ramo
  speciale dentro il codice di stampa.
- **`Arena` non decide, scandisce.** Le scelte del protagonista stanno tutte in `HeroBrain`: se
  tenere l'oggetto trovato, con quale criterio confrontarlo con quello che porta già, quanto pregiato
  può essere il loot di quel livello, dove spendere i punti. Se ti
  trovi a scrivere un `if` di gioco dentro `Arena`, appartiene al cervello. `Arena` gli passa il
  numero della prova, non un criterio.
- **Le leve di bilanciamento sono due, e dichiarate.** `HeroBrain` governa la **crescita** del
  protagonista e la qualità del loot; `TrialPlan` più `ChallengerBudget` governano la **pressione**
  del percorso — quanti punti ha lo schieramento e quanto la fortuna gliene toglie. Lo sconto vive
  accanto al percorso e non nel cervello proprio perché non è una *scelta* del protagonista: è la
  pressione del percorso calata su di lui. Cercare una leva sola porta a metterle nel posto sbagliato.
- **Com'è finita una prova si stabilisce una volta sola.** `Arena` guarda il campo alla fine dello
  scontro e ne ricava un `RoundOutcome`; da lì in poi quel dato viaggia. I logger non devono
  ridedurre l'esito interrogando il `Fighter`: due letture dello stesso campo possono divergere, e
  la narrazione finirebbe per dire una cosa diversa da quella che ha deciso la progressione. Vale
  anche per le prove superate: `Arena` non restituisce mai un `Optional` per dire «è andata male»,
  restituisce sempre un rapporto che contiene l'esito.
- **`Hero` non è `Fighter`.** La scheda sopravvive ai round, il combattente vive un round solo. La
  cura di fine scontro non è un metodo: è la conseguenza del fatto che ogni round il protagonista
  viene materializzato di nuovo (`FighterFactory.summon`). Non aggiungere API di guarigione, né qui
  né nel motore.
- **I buff entrano nello scontro in un punto solo, e non sono una regola di combattimento.** La somma
  vive in `EquipmentBonus` e arriva al motore come caratteristiche già maggiorate: nessun renderer, e
  nessun altro punto del gioco, deve rifarla per conto proprio. Le caratteristiche effettive non si
  custodiscono mai accanto alle base — si risolvono alla lettura — altrimenti si aprirebbe la classe
  di bug in cui la scheda e i suoi bonus raccontano cose diverse.
- **La casualità della generazione sta in `FighterFactory`.** Nel resto del gioco niente
  `Math.random()` o `new Random()`; la casualità del combattimento è del motore (`DiceRoller`).
  Unica deroga dichiarata: `HeroBrain`, che estrae dove cadono i punti caratteristica — è casualità
  di progressione, non di generazione, e il `Random` è iniettabile perché i test la piloti.
- `Main` resta thin: chiede la modalità e si fa da parte. Gli argomenti si leggono **solo** in
  `UiMode.fromArgs`, e gli errori sono eccezioni con messaggi leggibili — nessun `try/catch` in `Main`,
  la causa non va persa.

## Test

Suite JUnit Jupiter 5.x sotto `src/test/java`, con surefire configurato in `pom.xml`. Copre i
formatter/renderer di `combat.io`, la generazione di `combat.factory`, la cronaca come dato
(`combat.chronicle`), la passata muta, il confine web (`combat.io.web`: serializzazione JSON e server),
la selezione della modalità (`UiModeTest`) e **il percorso come dato** (`TrialPlanTest`: le dieci
stazioni, i conteggi di sfidanti, le forme, il monte punti, lo specchio solo alla decima). I test del
motore vivono nel repository del motore.

I test seguono i sotto-package di `combat.io`: il grosso sta in `render`, dove il testo prodotto è
verificabile senza far girare niente. `replay` è l'unico sotto-package senza test propri, e non è
una lacuna da colmare per completezza: i due `CombatReplay` sono orchestratori sottili, e il testo
che mostrano è già coperto dai test dei renderer.

Tre cose sul confine web, che vanno rispettate scrivendo test nuovi:

- Il server si lega a una **porta effimera** (porta 0, poi si legge quella assegnata), **mai** a una
  porta fissa, e va fermato sempre.
- Il test sulle **chiavi del JSON** è la sola rete che protegge il contratto verso il JavaScript: i tipi
  di log del motore sono `SNAPSHOT` e affiorano nel JSON, quindi un campo rinominato là romperebbe la
  pagina in silenzio. Se aggiungi un campo alla cronaca, aggiungi la sua chiave a quel test.
- **Nessun test automatico sul JavaScript**, ed è una scelta dichiarata (nessun build step, nessun runner
  JS, nessuna dipendenza), non una dimenticanza: la pagina si verifica a mano.

Due lacune dichiarate, da non scoprire di nuovo: nessun test lancia `Main` (in console giocherebbe una
partita vera in attesa di INVIO, in web lascerebbe un server acceso nella suite), e la pagina non ha
test propri come sopra.

- Assertion solo con `org.junit.jupiter.api.Assertions` (**niente AssertJ**). Non aggiungere dipendenze di test se non strettamente necessario.
- Il supporto ai test arriva dal `test-jar` del motore: `it.fantasycombatsystem.testsupport.CombatFixtures` per costruire `Fighter` deterministici, `StubDiceRoller` e `RecordingStubDiceRoller` per pilotare i dadi.
- I test dei renderer verificano il **testo prodotto**, non gli effetti a schermo: costruiscono un log di combattimento a mano e controllano le righe risultanti.

## Documentazione nel repo

- `README.md` — descrizione non tecnica del gioco e del confine col motore. È il posto dove spiegare
  il *perché*: non ripetere qui in `CLAUDE.md` quello che è già raccontato là.
- `daImplementare.md` — elenco delle funzionalità di gioco non ancora realizzate.
- `spec-web-replay.md` e `implementation-web-replay.md` — la specifica e il piano della seconda modalità
  di presentazione. Il secondo porta il registro delle decisioni: perché la cronaca esiste, perché i suoi
  campi facoltativi sono nullabili e non `Optional`, perché i due percorsi duello/battaglia restano due.
  Da leggere prima di rimettere in discussione una di quelle scelte.
- `spec-arena-dieci-prove.md` — il passaggio da tre a dieci prove col percorso come dato, e le tre
  aggiunte alla pagina (percorso disegnato senza spoiler, freccia «chi attacca chi», stellina
  dell'iniziativa). Il piano che l'accompagnava è stato rimosso a lavoro concluso.
- `spec-bilanciamento-progressione.md` e `implementation-bilanciamento-progressione.md` — il
  ribilanciamento che ha reso la progressione percepibile: monte punti di squadra invece che per
  singolo sfidante, sconto legato alla fortuna, rarità del loot più conservativa nella prima metà,
  pareggio che fa proseguire senza premio. **Da leggere prima di ritoccare i numeri della
  progressione**: il registro porta la misura su 500 corse prima e dopo, e due problemi aperti che
  sono decisioni dell'utente — la distribuzione bimodale (metà delle corse muore alla prima prova,
  metà arriva in fondo) e l'entità dello sconto della fortuna (12 punti medi, pavimento raggiunto in
  una prova su sette).
- La guida al bilanciamento (`combatSettings.md`) e la spiegazione delle regole di combattimento
  vivono nel repository `fantasy-combat-system`: non duplicarle qui, divergerebbero.

## Convenzioni di questo repo

- Java 21 (`maven.compiler.source/target=21`). In uso: record per i tipi valore, switch expression, `List.getFirst()`. Non in uso: sealed types, text block, pattern matching (né `instanceof` né `switch`). Non introdurre preview feature.
- Gli **switch sulle enum di dominio sono esaustivi, senza `default`** (`LootFate` in `ChronicleMapper` e in `HeroProgressFormatter`, `ChallengerOrigin` e `TrialShape` in `Arena`): una costante nuova deve diventare un errore di compilazione, non un `null` silenzioso che arriva alla pagina. Non aggiungere un `default` per «sicurezza»: toglie proprio la sicurezza.
- La pagina web è **HTML, CSS e JavaScript vanilla** in `src/main/resources/web/`: nessuna libreria, nessuna CDN, nessun build step, nessun npm. UTF-8 dichiarato nella pagina, commenti in italiano, identificatori in inglese, 2 spazi come nel Java. Il testo che arriva dalla cronaca entra nel DOM con `textContent`, mai con `innerHTML`.
- `app.js` è diviso in quattro responsabilità dichiarate nel commento di testa — costanti e frasi, costruzione dei momenti (**dati puri, nessun accesso al DOM**), stato della riproduzione, rendering — e la divisione va rispettata: un dato che serve al disegno si calcola nel momento, il DOM lo legge. Vale anche per i dati nuovi: gli indici di chi ha l'iniziativa nel passo, quelli degli avversari **ingaggiati** nello scambio, l'indice dell'avversario **attivo** e le voci dei segnalini di tutte e due le squadre, e la voce della colonna centrale (formula breve, i due nomi *ordinati per squadra*, verso della freccia) nascono tutti nella sezione 2.
- Fra Java e JavaScript **le formule brevi dell'azione sono duplicate di proposito**: `BattleSceneRenderer.formulaLabel` per la console, `ACTION_FORMULAS` per la pagina. `combat.io.render` è fuori portata della pagina e la cronaca non porta stringhe di presentazione, perché ogni lettore compone le proprie frasi — è la stessa decisione delle otto frasi del destino del loot. Se cambi il registro in un posto, cambialo anche nell'altro.
- Nella pagina, l'informazione **non si affida al solo colore**: le stazioni del percorso si distinguono anche per forma (cerchio pieno le passate, rombo quelle attraversate senza vittoria, cerchio marcato la corrente, quadrato spento quelle da raggiungere) e la stellina dell'iniziativa, come la spada di chi è ingaggiato nello scambio, porta un'etichetta esplicita e non è un carattere muto. Stessa regola per i segnalini dei combattenti: l'abbattuto si riconosce dal nome barrato e dall'etichetta accessibile, non dalla sola barra vuota.
- **Nella battaglia si vede una sola scheda avversaria per volta.** Un passo della riproduzione è **uno scambio**, non un round intero: `buildBattleStepMoments` espande `round.turns` in un momento ciascuno, così l'avversario «attivo» è sempre uno solo e la colonna di destra ne mostra la sola scheda. Attivo è chi agisce, quando ad agire è un avversario; il **bersaglio**, quando ad agire è il protagonista; e quello del passo precedente quando lo scambio non coinvolge nessun avversario — il riposo del protagonista non deve far sparire la scheda per un passo. Sopra ogni scheda sta la sua fila di segnalini — nome, stato e mini-barra di vita — **non cliccabili**, che non promettono nessuna interazione e riassumono lo stato di chi in quel passo non si vede per intero. La fila si disegna **sempre e in tutte e due le colonne**, anche nel duello e anche sopra il protagonista, che di segnalini ne ha sempre uno solo: è ciò che tiene le due schede **allineate per costruzione**, invece di far partire quella di destra più in basso di quanto misura la fila. Ne segue che i segnalini di una fila si spartiscono la larghezza della colonna e non hanno larghezza fissa, e che il duello **non** è più identico a com'era — l'allineamento ha avuto la precedenza, ed è una scelta esplicita dell'utente. Prima di tutto questo le schede avversarie stavano a schermo affiancate, il campo si allargava fino a 1320px e la scheda ingaggiata si scostava verso il protagonista: erano tre righe di CSS pilotate da un `data-opponent-count` che non esiste più. Un vincolo da non tradire: la ⚔ dell'ingaggio va lasciata dov'è, perché ora distingue l'avversario che partecipa davvero allo scambio da quello rimasto a schermo per trascinamento.
- **Le barre calano all'ultimo scambio del round, non prima.** Il motore fotografa i vitali una volta sola, a fine round (`RoundLogEntry.vitals`): uno scambio che non chiude il suo round mostra quindi lo stato di **fine round precedente** (vita e stamina piene nel primo round), e solo l'ultimo scambio rivela `round.vitals`. È la stessa trappola già documentata per il duello in `duelVitalsAfterTurn`, e la sola alternativa — mostrare subito lo stato di fine round — anticiperebbe il danno di scambi non ancora giocati.
- Attenzione a una trappola del CSS: l'attributo `hidden` vale `display: none` solo nel foglio di stile del browser, e una regola d'autore che dichiara un `display` lo batte. Per questo `.battlefield[hidden]` esiste: senza quella riga le schede dei combattenti resterebbero a schermo nei momenti in cui la pagina le nasconde. Se dai un `display` a un elemento che `renderMoment` nasconde, aggiungi la regola gemella.
- Indentazione a **2 spazi**. `FighterFactory` è l'unica eccezione storica a 4 spazi: se la modifichi, mantieni lo stile del file.
- Commenti e Javadoc in **italiano**. Il Javadoc di classe è denso e spiega le decisioni di design e i confini di responsabilità, non ripete la firma: mantieni questo registro quando aggiungi classi.
- Nomi di classi, metodi e variabili in inglese.
