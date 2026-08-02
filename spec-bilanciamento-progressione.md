# SPEC — Bilanciamento della progressione

**Obiettivo:** far sì che progredire si senta — oggi la crescita del protagonista è annullata per
costruzione dalla crescita degli sfidanti, il loot pregiato arriva troppo presto e il pareggio chiude
la corsa come una sconfitta — spostando quattro leve di bilanciamento: monte punti di squadra invece
che individuale, sconto sul monte legato alla fortuna del protagonista, rarità del loot più
conservativa nella prima metà, e pareggio che fa proseguire senza premio.

**Contesto**

- Il problema, misurato e non supposto:
  - il monte punti degli sfidanti in `TrialPlan` è `15 + 3 * (prova - 1)` **per ciascuno sfidante**,
    ed è esattamente la stessa curva della crescita del protagonista
    (`HeroBrain.CHARACTERISTIC_POINTS_PER_VICTORY = 3`, partenza a `15` in
    `FighterFactory.TOTAL_CHARACTERISTIC_POINTS`). I tre punti che la vittoria vale sono quindi
    neutralizzati per costruzione, e il monte avversario viene per giunta **moltiplicato per il
    numero di sfidanti**: alla prova 4 sono 24 punti contro 48, con due azioni per turno contro una;
  - le quattro `RarityTable` di `HeroBrain` sono troppo generose presto: `LEGENDARY` al 10% per
    estrazione alle prove 1-2 e al 20% alle 3-5, cioè **35% di aver già visto un leggendario entro la
    prova 3** e 48% entro la 4. Un'arma `LEGENDARY` ha attacco 15-25 contro 3-6 di una `UNCOMMON` e
    porta buff per una decina di punti: un solo drop leggendario vale più di tre vittorie di
    progressione;
  - misurato su quindici corse (registrato in `daImplementare.md`): undici cadono alla prima prova,
    una alla seconda, due alla terza, **nessuna oltre**. Sei stazioni su dieci, fra cui lo specchio,
    oggi non si vedono quasi mai. Concorre il fatto che `RoundOutcome.STOOD_WITHOUT_WINNING` chiude
    la corsa come una caduta.
- Punti del codice interessati:
  - `combat.TrialPlan` / `combat.TrialStation` — la tabella delle dieci stazioni e il campo
    `characteristicPoints` (nullable, assente per la stazione dello specchio);
  - `combat.factory.FighterFactory` — `createChallengers(int count, int totalCharacteristicPoints)`,
    `createChallenger(int)`, `generateWarrior(int)` e le due validazioni `validateCount` /
    `validateCharacteristicPoints` (oggi solo `>= 1`);
  - `combat.Arena` — `challengersFor` (switch esaustivo su `ChallengerOrigin`), `fightRound` con
    `if (outcome != RoundOutcome.WON)`, `run()` che chiude con `if (lastRound.passed())
    logger.reportTriumph(...)`, il record privato `RoundReport` e la sua `andThen`;
  - `combat.hero.HeroBrain` — le quattro `RarityTable` e `lootRarityTable(int level)`;
  - `combat.chronicle` — `TrialChronicle` (dove vive il dato per prova), `RunConclusion.triumph()`,
    `ChronicleMapper`;
  - `combat.io.log.ArenaLogger` con `ConsoleArenaLogger` e `SilentArenaLogger` — `announceRound`
    e `reportEndOfRun`, che oggi distingue `FELL` da `STOOD_WITHOUT_WINNING` con due frasi;
  - `src/main/resources/web/app.js` — `TRIAL_OUTCOME_LABELS`/`RUN_OUTCOME_LABELS` (sezione 1),
    `buildMoments` col suo `if (trial.progress)` (sezione 2), `describeConclusion`,
    `trialStationStateOf` e `trialStationLabel` (sezione 4).
- Fatti verificati sulla documentazione del toolkit
  (`C:/build/git/fantasy-game-toolkit/docs/agent/`), che questa modifica dà per acquisiti:
  - `CharacterGeneratorTool.totalPoints(n)` è la somma **dopo la sola distribuzione**; ogni
    caratteristica parte da `minCharacteristicValue` (default `1`) e `generate()` solleva
    `IllegalStateException` se `totalPoints < minCharacteristicValue * count`. Le
    `Characteristic` sono **sette** (`STRENGTH`, `INTELLIGENCE`, `AGILITY`, `CHARISMA`,
    `RESISTANCE`, `STAMINA`, `LUCK`) e gli sfidanti nascono con `allCharacteristics()`: il
    **pavimento per singolo sfidante è 7**;
  - i bonus di razza e classe si sommano **sopra** `totalPoints` (+3 e +3 tipici per un `WARRIOR`) e
    restano attivi sia per il protagonista sia per gli sfidanti generati — non per lo specchio, che
    li disattiva con due tabelle vuote. Sono quindi già pari e non entrano in questo lavoro;
  - `RarityTable.build()` valida che i pesi siano positivi, le rarità uniche e la **somma esattamente
    100**, altrimenti `IllegalStateException`;
  - nel motore la `LUCK` vale oggi soltanto `+1%` di probabilità di critico per punto
    (`CombatFormulas.critChance`, `critChanceLuckFactor = 0.01`): è quasi una caratteristica morta, e
    il toolkit non espone alcuna API per abbassare una singola caratteristica di un personaggio
    generato.
- Pattern esistenti da riusare: il percorso come **dato** in `TrialPlan` (tabella di letterali, non
  formula a runtime); la forma di `RoundReport` e della sua cortocircuitazione; il campo facoltativo
  **nullable e non `Optional`** già usato da `TrialStation.characteristicPoints` e
  `TrialChronicle.progress`; gli switch esaustivi senza `default` sulle enum di dominio; la
  duplicazione dichiarata delle frasi fra `combat.io.render`/`combat.io.log` e `app.js`; il test
  sulle chiavi del JSON come unica rete a protezione del contratto verso il JavaScript.
- File coinvolti: `TrialPlan`, `FighterFactory`, `Arena`, `HeroBrain`, `TrialChronicle`,
  `ChronicleMapper`, `ArenaLogger` + le due implementazioni, `web/app.js`, più una classe nuova per
  il monte di squadra scontato e un record nuovo di cronaca; `CLAUDE.md`, `README.md`,
  `daImplementare.md`.

**Comportamento atteso**

- **Il monte punti è di squadra, non del singolo.** `TrialStation.characteristicPoints` cambia
  semantica: da monte del singolo sfidante a monte dell'**intero schieramento**. La curva nuova è
  `monteEroe(N) × moltiplicatore(numeroSfidanti)`, con `monteEroe(N) = 15 + 3 * (N - 1)` e
  moltiplicatore `1.0` con un sfidante, `1.3` con due, `1.5` con tre — la differenza rispetto al
  numero puro è il prezzo dell'economia di azioni. I valori risultanti restano **letterali cablati**
  in `TrialPlan`, con la regola di derivazione scritta nel Javadoc e non calcolata a runtime: la
  tabella resta un dato esplicito e leggibile.

  | prove | sfidanti | monte di squadra | per sfidante |
  | --- | --- | --- | --- |
  | 1, 2, 3 | 1 | 15, 18, 21 | 15, 18, 21 |
  | 4, 5, 6 | 2 | 31, 35, 39 | 15+16, 17+18, 19+20 |
  | 7, 8, 9 | 3 | 50, 54, 59 | 16+17+17, 18+18+18, 19+20+20 |
  | 10 | 1 (specchio) | — | ricalca il protagonista |

- **La ripartizione fra gli sfidanti** avviene in `FighterFactory.createChallengers`, a parti uguali
  col resto ai primi. Il parametro va rinominato perché la semantica nuova si legga alla firma (es.
  `squadCharacteristicPoints`). La validazione è esplicita: un monte di squadra sotto
  `7 × numeroSfidanti` è un errore con messaggio diagnostico che nomina il pavimento, il monte
  ricevuto e il numero di sfidanti — mai un `IllegalStateException` opaco che arriva dal toolkit.
- **La fortuna del protagonista sconta il monte di squadra.** Il monte effettivo dello schieramento è
  `monteStazione − fortuna × numeroSfidanti`, con pavimento a `7 × numeroSfidanti`. La fortuna è
  quella **effettiva** (`hero.effectiveCharacter()`), coi buff dell'equipaggiamento addosso: vedi
  «Decise» 1. È la leva che dà
  un peso reale a una caratteristica che nel motore oggi non ne ha quasi, senza toccare il motore e
  senza abbassare singole caratteristiche generate — cosa per cui il toolkit non espone API e che
  richiederebbe di presidiare un pavimento per ogni caratteristica.
- **Lo sconto non tocca lo specchio**: la stazione 10 non dichiara un monte proprio e
  `createMirrorRival` continua a ricalcare il protagonista.
- **Lo sconto si vede.** Perché risolva il problema di partenza, il giocatore lo deve leggere. Il
  dato va portato in `combat.chronicle` **in forma di numeri** — monte dichiarato dalla stazione,
  sconto applicato, monte effettivo dello schieramento — e ogni lettore compone la propria frase:
  la console all'annuncio del round (`ArenaLogger`), la pagina nel pannello della prova. Il dato è
  assente (nullable) per la stazione dello specchio.
- **Il loot pregiato arriva più tardi.** Le quattro `RarityTable` diventano:

  | scaglione | UNCOMMON | RARE | EPIC | LEGENDARY |
  | --- | --- | --- | --- | --- |
  | prove 1-2 | 65 | 25 | 8 | 2 |
  | prove 3-5 | 35 | 40 | 20 | 5 |
  | prove 6-8 | — | 45 | 40 | 15 |
  | prove 9-10 | — | 20 | 55 | 25 |

  Gli scaglioni restano quattro e coi confini di oggi. Effetto atteso: probabilità di aver visto un
  leggendario entro la prova 3 dal 35% all'**8,8%**, e sulla corsa intera dal 93% al **72%**. Cambia
  anche la forma della curva: il pavimento non parte più da `UNCOMMON` per salire, **scende** —
  `UNCOMMON` resta estraibile fino alla prova 5 e il pavimento sale a `RARE` solo dalla 6.
- **Il pareggio non chiude più la corsa.** `RoundOutcome.STOOD_WITHOUT_WINNING` fa **proseguire** alla
  prova successiva, ma **senza loot e senza i tre punti caratteristica**: la scheda passa alla prova
  dopo esattamente com'era. Solo `FELL` chiude la corsa. Ne segue che:
  - la voce di cronaca della prova pareggiata esiste, porta `outcome = STOOD_WITHOUT_WINNING` e ha
    `progress` a `null` (il campo è già nullable);
  - il trionfo si dichiara **solo** se l'ultima prova è finita `WON`: non basta più «la catena non si
    è interrotta», perché ora anche un pareggio la lascia proseguire;
  - `RunConclusion.outcome()` resta l'esito dell'ultima prova giocata e `triumph()` resta derivato da
    esso; una corsa che arriva in fondo pareggiando la decima si chiude con
    `STOOD_WITHOUT_WINNING` alla prova 10 e **senza** trionfo;
  - `ArenaLogger.reportEndOfRun` viene invocato solo per `FELL`; il pareggio va raccontato come un
    passaggio, non come una fine.
- Casi limite:
  - fortuna così alta da azzerare il monte: interviene il pavimento `7 × numeroSfidanti`, e lo sconto
    registrato nella cronaca è quello **effettivamente applicato**, non quello teorico;
  - monte di squadra non divisibile per il numero di sfidanti (31 su 2, 50 su 3): il resto va ai
    primi, quindi gli sfidanti di una stessa prova possono differire di un punto;
  - un sfidante solo: la ripartizione è l'identità e il monte di squadra coincide col suo;
  - pareggi consecutivi: si prosegue comunque, senza crescita, fino alla caduta o alla fine del
    percorso;
  - pareggio alla decima prova: la corsa finisce lì, senza trionfo e senza procedura;
  - `plannedTrials` resta la lunghezza del percorso e la cronaca continua a non avere voci per le
    prove non giocate.
- Invarianti: numero e descrizione delle dieci stazioni, `ChallengerOrigin` e derivazione della
  `TrialShape` dal numero di sfidanti, `CHARACTERISTIC_POINTS_PER_VICTORY = 3`, criteri di cernita di
  arma, armatura e gioiello, buff dell'equipaggiamento e caratteristiche effettive, cura di fine
  scontro, struttura dei momenti della pagina, vincolo di non-spoiler.

**Vincoli**

- **Nessuna modifica al motore** e nessuna regola di combattimento in questo repository: il peso
  nuovo della fortuna nasce dal monte punti degli avversari, non da una formula di combattimento.
- **`Arena` non decide, scandisce**: nessun `if` di gioco nuovo. Chiede il monte effettivo a chi lo
  sa calcolare e lo passa alla factory; la distinzione fra `FELL` e `STOOD_WITHOUT_WINNING` resta una
  lettura del `RoundOutcome` già calcolato una volta sola da `outcomeOf`.
- **Com'è finita una prova si stabilisce una volta sola**: nessun lettore rididuce l'esito
  interrogando il `Fighter`, e il trionfo si deriva dall'esito dell'ultima prova, non da un secondo
  segnale che potrebbe divergere.
- Il percorso resta **dato esplicito**: i nove monti di squadra sono letterali in `TrialPlan`, non una
  formula valutata a runtime. Il moltiplicatore è documentato, non implementato.
- La cronaca resta **di soli dati**: nessuna stringa di presentazione, nessuna annotazione Jackson,
  nessun modulo Jackson aggiuntivo, campi facoltativi nullabili e non `Optional`. Ogni chiave nuova
  del JSON va aggiunta a `ChronicleJsonTest`, che è l'unica rete a protezione del contratto verso il
  JavaScript.
- `combat.hero` non deve cominciare a dipendere da `combat.factory` (né viceversa oltre a quanto già
  accade); `combat.io.render` resta senza I/O; `combat.io.web` resta un pozzo.
- Le frasi restano **duplicate di proposito** fra console e `app.js`: cambiate in un posto, vanno
  cambiate nell'altro.
- Gli switch sulle enum di dominio restano **esaustivi e senza `default`**.
- Java 21 come nel resto del repo, 2 spazi di indentazione — `FighterFactory` resta l'eccezione
  storica a 4. Nessuna nuova dipendenza. Test con sole `org.junit.jupiter.api.Assertions`.

**Fuori scope**

- **Il ribilanciamento dello sfidante speculare della prova 10.** Oggi nasce dai punti **base** del
  protagonista (`Hero.totalCharacteristicPoints()`), con `RaceBonusTable`/`ClassBonusTable` vuote e
  arma solo `RARE`, mentre a quel punto il protagonista ha equipaggiamento `EPIC`/`LEGENDARY` coi
  relativi buff: la prova finale è verosimilmente **più facile** delle prove 7-9. È un'osservazione
  registrata qui, non un lavoro da fare adesso: correggerla vuol dire decidere se lo specchio debba
  pareggiare le caratteristiche effettive, cioè una scelta di bilanciamento separata.
- Toccare `CHARACTERISTIC_POINTS_PER_VICTORY`, il criterio di cernita degli oggetti, l'equipaggiamento
  di partenza o la rarità standard.
- Rendere il percorso parametrico o variabile.
- La modalità «tanti scontri in fila» con aggregazione statistica: resta in `daImplementare.md`. Qui
  si prevede solo una **misura sul campo** manuale, con quello che esiste già.
- Dare alla `LUCK` un peso maggiore dentro il motore (è un lavoro dell'altro repository).
- Test automatici del JavaScript (scelta dichiarata del repo).

**Conseguenze dichiarate** (accettate, non da risolvere qui)

1. **Gli sfidanti diventano molto più deboli nelle prove a più avversari**: alla prova 7 si passa da
   99 punti complessivi a 50, meno lo sconto. È l'effetto voluto, ma la profondità delle corse dopo
   la modifica va **misurata**, non prevista.
2. **La fortuna diventa una caratteristica desiderabile** anche fuori dal critico, e i tre punti della
   vittoria che vi cadono a caso valgono ora più degli altri. Nessuna correzione: è l'effetto voluto.
3. **Gli sfidanti di una stessa prova non sono più identici per monte punti** (resto della divisione):
   una differenza di un punto, dichiarata e non nascosta.
4. **La prova pareggiata è una prova «attraversata» ma non superata**, e questa terza condizione
   compare in ogni lettore: le stazioni del percorso disegnato non sono più solo «passate / corrente /
   da raggiungere».

**Decise** (2026-08-02, dall'utente: erano le due voci «da decidere» di questa SPEC)

1. **Fortuna effettiva, `hero.effectiveCharacter()`** — coi buff di arma, armatura e gioielli
   addosso. Scelta dall'utente **contro** la raccomandazione di questa SPEC, che proponeva la base
   (`hero.character()`), e la motivazione contraria resta registrata perché è quella da verificare in
   Fase 10: con l'effettiva, un gioiello che porti molta `LUCK` può schiacciare da solo uno
   schieramento fino al pavimento, e il bilanciamento torna in parte a dipendere dalla lotteria del
   loot — il difetto che questo lavoro corregge. A favore della scelta fatta: è coerente col principio
   già dichiarato nel progetto per cui l'equipaggiamento conta **sempre** e le caratteristiche
   effettive sono l'unica lettura che descrive chi è davvero in campo, e rende la `LUCK` sui gioielli
   una statistica appetibile invece che inerte. Il pavimento `7 × numeroSfidanti` diventa quindi la
   difesa principale contro gli scarti, non un caso limite teorico: va testato come comportamento
   ordinario.
2. **Un collaboratore dedicato accanto a `TrialPlan`** (record `ChallengerBudget` in
   `it.fantasyarena.combat`), non `HeroBrain`. Le due letture che si toccavano, e perché ha vinto
   questa, restano qui sotto per memoria. Conseguenza sulla documentazione: la riga di `CLAUDE.md` su
   `HeroBrain` «punto unico da toccare per ribilanciare la progressione» non è più vera, e va
   corretta dicendo che le leve sono **due e dichiarate** — `HeroBrain` per la crescita e il loot,
   `TrialPlan` più il monte di squadra per la pressione.

**Come si era arrivati alle due scelte** (argomenti conservati, non più aperti)

1. **Fortuna base o effettiva?** Proposta: la **base**, `hero.character()`. Motivazione: leggere
   l'effettiva (`hero.effectiveCharacter()`, coi buff dell'equipaggiamento) rimetterebbe il
   bilanciamento nelle mani della lotteria del loot, che è esattamente il difetto che questo lavoro
   corregge — un gioiello leggendario con `+10 LUCK` azzererebbe da solo uno schieramento fino al
   pavimento. Con la base, lo sconto cresce **solo** per merito della progressione, cioè dei punti
   che la vittoria assegna. *Non accolta: vedi «Decise» 1.*
2. **Dove vive lo sconto?** Due letture di `CLAUDE.md` si toccano: `HeroBrain` è dichiarato «il punto
   unico da toccare per ribilanciare la progressione», ma è anche «tutte le **scelte** del
   protagonista» — e uno sconto non è una scelta.
   - *A favore di `HeroBrain`*: ospita già `lootRarityTable(level)`, che non è una scelta ma una leva
     di bilanciamento parametrata sul numero della prova; il precedente esiste.
   - *A favore di un collaboratore dedicato* (proposta: un record `ChallengerBudget` in
     `it.fantasyarena.combat`, accanto a `TrialPlan`): la pressione del percorso è già dichiarata
     **fuori** dal cervello — `CLAUDE.md` dice che `TrialPlan` «è il punto unico da toccare per
     allungare o ribilanciare la pressione del percorso» — e il monte di squadra scontato è
     esattamente quella pressione, calata su questo protagonista. Vive dove vive il percorso, non
     dove vivono le scelte. In più `it.fantasyarena.combat` può importare `combat.factory`, quindi il
     pavimento può restare **una sola costante pubblica** di `FighterFactory` (l'unico punto che
     conosce il toolkit) invece di essere riscritto anche in `combat.hero`, che da `combat.factory`
     non deve dipendere.

   **Raccomandazione: il collaboratore dedicato.** `HeroBrain` resta il posto delle scelte, `Arena`
   continua a non decidere (chiede il monte e lo passa alla factory), e la documentazione va corretta
   dicendo che le leve di bilanciamento sono **due e dichiarate**: `HeroBrain` per la crescita e il
   loot, `TrialPlan` + il monte di squadra per la pressione.

**Definition of done** — criteri verificabili, ognuno coperto da almeno un test (dove segnato,
verifica manuale, secondo la policy del repo sul JavaScript)

1. `TrialPlan` dichiara i nove monti di squadra `15, 18, 21, 31, 35, 39, 50, 54, 59` e la stazione
   dello specchio continua a non dichiararne uno;
2. `createChallengers` ripartisce il monte di squadra a parti uguali col resto ai primi: la somma dei
   monti individuali richiesti è esattamente il monte di squadra ricevuto;
3. `createChallengers` rifiuta un monte di squadra sotto `7 × numeroSfidanti` con un
   `IllegalArgumentException` diagnostico, e accetta esattamente il pavimento;
4. il monte effettivo dello schieramento è `monteStazione − fortuna × numeroSfidanti`, calcolato sulla
   fortuna **effettiva** del protagonista, coi buff dell'equipaggiamento addosso;
5. lo sconto non scende mai sotto il pavimento: con fortuna alta il monte effettivo si ferma a
   `7 × numeroSfidanti` e lo sconto registrato è quello effettivamente applicato;
6. la stazione dello specchio non riceve né monte né sconto;
7. la cronaca di ogni prova a sfidanti generati porta monte dichiarato, sconto e monte effettivo, e
   non li porta per la prova dello specchio;
8. il JSON servito espone le chiavi nuove al livello in cui il JavaScript le legge;
9. la console dichiara lo sconto all'annuncio del round;
10. le quattro tabelle di rarità hanno i pesi nuovi (somma 100 ciascuna), gli scaglioni restano quattro
    con gli stessi confini, `UNCOMMON` resta estraibile fino alla prova 5 e il pavimento sale a
    `RARE` dalla prova 6;
11. una prova chiusa `STOOD_WITHOUT_WINNING` fa giocare la prova successiva e **non** produce né loot
    né punti caratteristica: la scheda della prova dopo è identica a quella di prima;
12. una prova chiusa `FELL` chiude la corsa, come oggi;
13. il trionfo si dichiara solo se l'ultima prova è finita `WON`: una decima prova pareggiata chiude
    la corsa senza trionfo, con `RunConclusion` a `STOOD_WITHOUT_WINNING` sulla prova 10;
14. la voce di cronaca di una prova pareggiata esiste, ha `progress` a `null` e non interrompe le voci
    successive;
15. *(verifica manuale)* la pagina disegna una prova attraversata senza procedura di fine scontro e
    distingue le stazioni pareggiate da quelle superate, senza affidarsi al solo colore;
16. *(verifica manuale, confrontando due partite di lunghezza diversa)* il percorso continua a non
    anticipare dove finisce la corsa;
17. comportamento preesistente invariato dove richiesto: numero e forma delle prove, punti per
    vittoria, cernita degli oggetti, buff dell'equipaggiamento, cura di fine scontro;
18. nessuna modifica non richiesta a motore, contratti o altri moduli.

**Esempio** (istanza concreta — solo illustrativo)

```java
// TrialPlan: la tabella resta un dato esplicito. La regola di derivazione sta nel Javadoc,
// i numeri stanno qui: monteEroe(N) = 15 + 3*(N-1), moltiplicatore 1.0 / 1.3 / 1.5.
TrialStation.generated(4, "due contro uno", 2, 31),        // 24 * 1.3 = 31,2 -> 31
TrialStation.generated(7, "tre contro uno", 3, 50),        // 33 * 1.5 = 49,5 -> 50
TrialStation.mirror(10, "lo sfidante speculare, armato meglio")

// Il monte di squadra calato su questo protagonista: la pressione del percorso, scontata dalla
// fortuna. Vive accanto a TrialPlan e non in HeroBrain, perche' non e' una scelta del protagonista
// (vedi "Decise" 2). Il pavimento e' una costante pubblica di FighterFactory: e' l'unico punto
// che conosce il vincolo del toolkit (7 caratteristiche, minimo 1 ciascuna).
public record ChallengerBudget(int stationPoints, int luckDiscount, int squadPoints) {

  public static ChallengerBudget of(int stationPoints, Hero hero, int challengerCount) {
    int floor = FighterFactory.MINIMUM_CHARACTERISTIC_POINTS_PER_CHALLENGER * challengerCount;
    int requestedDiscount = luckOf(hero.effectiveCharacter()) * challengerCount;
    int affordableDiscount = Math.max(0, stationPoints - floor);
    int appliedDiscount = Math.min(requestedDiscount, affordableDiscount);
    return new ChallengerBudget(stationPoints, appliedDiscount, stationPoints - appliedDiscount);
  }
}
```

```java
// FighterFactory (4 spazi, eccezione storica del file): la ripartizione e la validazione esplicita.
// Il messaggio nomina pavimento, monte e numerosita': l'alternativa e' un IllegalStateException
// opaco sollevato dal toolkit dentro generate().
public List<Fighter> createChallengers(int count, int squadCharacteristicPoints) {
    validateCount(count);
    validateSquadPoints(count, squadCharacteristicPoints);

    int basePoints = squadCharacteristicPoints / count;
    int remainder = squadCharacteristicPoints % count;
    return IntStream.range(0, count)
            .mapToObj(index -> createChallenger(basePoints + (index < remainder ? 1 : 0)))
            .toList();
}
```

```java
// Arena: il pareggio prosegue senza premio, la caduta chiude. Nessun if di gioco nuovo: si legge
// il RoundOutcome gia' calcolato una volta sola da outcomeOf.
RoundOutcome outcome = outcomeOf(champion, challengers);
return switch (outcome) {
  case FELL -> { ... logger.reportEndOfRun(hero, outcome, number); yield previous.lostTrial(...); }
  case STOOD_WITHOUT_WINNING -> previous.crossedTrial(hero, chronicleOf(..., outcome, null));
  case WON -> previous.wonTrial(progress.grownHero(), chronicleOf(..., outcome, progressChronicle));
};

// run(): il trionfo non e' piu' "la catena non si e' interrotta", perche' ora anche il pareggio
// la lascia proseguire. Si legge l'esito dell'ultima prova giocata.
TrialChronicle lastTrial = lastRound.trials().getLast();
if (lastTrial.outcome() == RoundOutcome.WON) {
  logger.reportTriumph(lastRound.grownHero(), plan.length());
}
```
