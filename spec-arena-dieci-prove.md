# SPEC — Arena a dieci prove, percorso disegnato nella pagina, freccia e iniziativa

**Obiettivo:** portare l'arena da tre a dieci prove descrivendo il percorso **in un punto solo, come
dato**, e dare alla pagina web tre cose che oggi non ha: il disegno dell'intero percorso senza
rivelare dove la corsa finisce, la freccia «chi attacca chi» fra le due colonne di schede, e il segno
di chi ha l'iniziativa nel passo corrente.

Il compito riguarda **solo** `fantasy-arena`. Il motore `fantasy-combat-system` e il toolkit non si
toccano: nessuna regola di combattimento nasce qui, e i dati che la pagina mostra sono quelli che il
motore ha già deciso.

Il motivo per cui le due metà del compito stanno insieme è uno: oggi il denominatore
dell'intestazione della pagina è `chronicle.trials.length`, cioè le prove **giocate**. Con tre prove
la perdita era tollerabile; con dieci diventa lo spoiler principale — al primo passo la pagina
scriverebbe «Prova 1/1» e rivelerebbe che la corsa finisce subito. Allungare il percorso obbliga
quindi la cronaca a portare la **lunghezza prevista**, e obbliga la pagina a distinguere «stazione
mai giocata» da «stazione non ancora raggiunta».

## Contesto

Tutto verificato leggendo il codice di questo repository.

**La progressione — `it.fantasyarena.combat`**

- `Arena` cabla le tre prove in tre punti diversi: tre costanti di descrizione
  (`FIRST_ROUND_DESCRIPTION` = «il primo avversario», `SECOND_ROUND_DESCRIPTION` = «due contro uno»,
  `FINAL_ROUND_DESCRIPTION` = «lo sfidante speculare, armato meglio»), tre costanti di numero, due di
  conteggio sfidanti (`LONE_CHALLENGER`, `CHALLENGER_PAIR`), e tre metodi privati
  (`fightLoneChallenger`, `fightChallengerPair`, `fightMirrorRival`) concatenati in `run()` con
  `RoundReport.entering(protagonist).andThen(...).andThen(...).andThen(...)`.
- La **scansione di una prova è già una sola**: `fightRound(int number, String description,
  RoundReport previous, List<Fighter> challengers, FightPlay play)` fa annuncio, materializzazione
  (`fighterFactory.summon`), fotografia del roster *prima* dello scontro, scontro, `outcomeOf`,
  procedura di fine scontro. I tre metodi privati differiscono **solo** per numero, descrizione,
  sfidanti e quale dei due `FightPlay` usare (`playAsBattle` / `playAsDuel`). È il punto su cui la
  piega si appoggia senza riscrivere niente.
- `RoundReport(boolean passed, Hero grownHero, List<TrialChronicle> trials)` è un record privato
  immutabile: `andThen` cortocircuita (`passed ? nextRound.apply(this) : this`) e `wonTrial` /
  `lostTrial` accumulano la cronaca costruendo un rapporto nuovo. La cortocircuitazione e
  l'accumulo che deve sopravvivere sono esattamente i due requisiti di una piega.
- `run()` legge la conclusione dall'**ultima voce** di `lastRound.trials()`
  (`lastTrial.outcome()`, `lastTrial.number()`): non dipende dal numero di prove.
- `outcomeOf` è l'unico punto del gioco che legge lo stato mutabile del `Fighter`; non cambia.
- `Arena` riceve **due** `MatchRunner` distinti (battaglia e duello) proprio perché ognuno costruisce
  alla prima chiamata il proprio `TurnPacer` col suggerimento adatto: il disegno regge già un
  percorso che alterna le due forme più volte.

**La generazione — `combat.factory.FighterFactory`** (file a 4 spazi, unica eccezione storica)

- `createChallengers(int count)` valida `count >= 1` e genera `count` avversari tutti a
  `TOTAL_CHARACTERISTIC_POINTS = 15` e `STANDARD_EQUIPMENT_RARITY = UNCOMMON`, un pezzo d'armatura
  ciascuno. **Non conosce il livello**: la firma non ha modo di far crescere gli sfidanti.
- `createMirrorRival(Hero hero)` rispecchia il protagonista *com'è cresciuto*
  (`hero.totalCharacteristicPoints()`, `hero.armourPieceCount()`) con un'arma
  `MIRROR_RIVAL_WEAPON_RARITY = RARE`. Va invocato **quando** quella prova arriva, non prima.
- `generateUniquelyNamed` tiene `usedNames` per l'intera vita dell'istanza e disambigua col suffisso
  `" (n)"`: dentro una singola corsa **i nomi dei combattenti sono unici**. È il fatto che rende
  praticabile l'inferenza per nome nel duello (vedi *Comportamento atteso*, punto 4).
- `rollLoot(RarityTable)` estrae tipo e rarità; la tabella gliela passa `Arena` chiedendola al
  cervello.

**Le scelte — `combat.hero.HeroBrain`**

- `lootRarityTable(int level)` ha **due soli scaglioni**: `case 1 -> OPENING_TRIAL_...` (pavimento
  `UNCOMMON`, pesi 50/24/16/10) e `default -> ADVANCED_TRIAL_...` (pavimento `RARE`, pesi 48/32/20).
  Tarati su un percorso di tre prove.
- `CHARACTERISTIC_POINTS_PER_VICTORY = 3`; `JEWEL_BONUS_POINTS` va da 1 (`COMMON`/`UNCOMMON`) a 4
  (`LEGENDARY`). Una vittoria vale quindi da 3 a 7 punti caratteristica.
- `HeroBrainTest` copre la tabella con due test: livello 2 e livello 3 sono la **stessa istanza**
  (`assertSame`), e il pavimento del livello 1 è `UNCOMMON` mentre dal 2 in poi è `RARE`.

**La cronaca — `combat.chronicle`**

- `ArenaChronicle(HeroSnapshot protagonist, List<TrialChronicle> trials, RunConclusion conclusion)`:
  **nessun campo dice quante prove erano previste**.
- `TrialChronicle(number, description, shape, roster, rounds, turns, outcome, progress)`, con
  `TrialShape` (`BATTLE`/`DUEL`) che dice quale delle due liste di passi leggere.
- `RunConclusion(RoundOutcome outcome, int lastTrial)`; `triumph()` è un accessor derivato e
  **non compare nel JSON** (verificato da un test dedicato).
- La cronaca non porta annotazioni Jackson e non importa niente da `combat.io`.

**La console — `combat.io`**

- `ConsoleArenaLogger.reportEntrance` stampa «dovrà superare **tre** prove»;
  `reportTriumph` stampa «ha superato tutte e **tre** le prove dell'arena». `ArenaTest` asserisce su
  quella seconda frase. Sono le due sole frasi con il numero cablato.
- `ConsoleMatchPresentation.presentBattle` attende l'INVIO **a ogni round**;
  `presentDuel` passa da `ScreenCombatReplay`, che attende l'INVIO **a ogni turno**.
- `BattleSceneRenderer` è il precedente da cui prendere il registro linguistico della freccia:
  `arrowFor(turn)` orienta la freccia a destra quando `teamIndexes.get(turn.attackerIndex()) == 0`,
  `formulaMiddle` disegna la formula **sopra** `arrowMiddle`, e `formulaLabel` compone le sette
  etichette brevi con uno switch esaustivo su `ActionOutcome.Kind`: `colpisce (n)`, `critico (n)`,
  `colpo potente (n)`, `manca`, `parato (n)`, `schivato`, `riposa (+n)`; stringa vuota se
  `turn.action()` è `null`. La squadra 0 sta sempre a sinistra e la 1 sempre a destra, e la freccia
  dice solo **chi dei due** attacca.
- `CombatScreenRenderer` marca l'iniziativa col precedente `*nome*`: `chosenName(turnPosition)`
  legge `log.get(turnPosition).initiative()` e restituisce `null` quando il report d'iniziativa
  manca, `isChosen` confronta per nome. `TurnLogFormatter` verifica anch'esso
  `entry.initiative() != null`: **l'iniziativa del duello può mancare**, e in Java questo è già
  gestito in due punti.

**La pagina — `src/main/resources/web/`**

- `app.js`, `buildMoments`: `const totalTrials = chronicle.trials.length;` finisce dentro ogni
  momento, e `renderHeader` scrive `Prova ${moment.trialNumber}/${moment.totalTrials}`. È lo
  spoiler.
- `populateTrialButtons(chronicle.trials, player)` crea **un pulsante per prova giocata** in
  `#trial-jump`: secondo spoiler, dello stesso genere (l'insieme dei bersagli cliccabili dice dove
  finisce la corsa).
- Il momento di conclusione **non porta `trialNumber`** (`buildConclusionMoment` porta solo
  `conclusion`), e `renderHeader` lo tratta a parte: la stazione corrente in quell'istante si legge
  da `conclusion.lastTrial`.
- `buildEngagement(roster, engagementTurn)` risolve già attaccante e bersaglio da
  `attackerIndex`/`targetIndex` con `findFighterByRosterIndex`, e `buildEngagementItem` scrive
  `attaccante → bersaglio` nel pannello testuale. `describeAction` compone la frase verbosa
  («Esito: colpo a segno, danno 17, critico.»): resta dov'è.
- `buildDuelStepMoment` legge `turn.initiative.chosenName` **senza guardia**. Dato che il Java
  verifica quel `null` in due punti, è un difetto latente: un turno senza report d'iniziativa
  spegnerebbe la pagina con un `TypeError`. Va chiuso in questo compito, perché la stellina ha
  bisogno esattamente di quella distinzione.
- `buildCombatantCard(fighter, vital)` mette il nome in un `h3`; `renderTeam` filtra il roster per
  `teamIndex` e riempie `#team-0` e `#team-1`. `#battlefield` è una griglia `1fr 1fr` che collassa a
  una colonna sotto 720px (`app.css`).
- `alignVitalsToRoster` allinea per posizione e ricade sul nome, normalizzando a `null`;
  `appendVitalsBars` disegna la scheda anche senza vitali. Il precedente di ripiego onesto è già in
  casa.

**I test**

- `ArenaTest` pilota tutto con `ScriptedFights extends MatchRunner` (sovrascrive `playBattle` e
  `playDuel`, applica esiti a tavolino, registra `challengerCounts()` e `presentations()`) e
  `RecordingFighterFactory extends FighterFactory`. Le aspettative cablate su tre prove sono
  `List.of(1, 2, 1)`, `List.of(BATTLE, BATTLE, DUEL)`, `challengersOfRound(3)`, i tre
  `countOccurrences(..., 3)` e la frase del trionfo.
- `ChronicleJsonTest` è **l'unica rete** che protegge il contratto verso il JavaScript: asserisce le
  chiavi presenti a ogni livello di annidamento su una cronaca costruita a mano.
- `pom.xml`: Java 21, Jackson già presente. **Nessuna nuova dipendenza serve a questo compito.**

**Fatto verificato dall'utente nel motore, non riverificato qui** (il motore è dipendenza in sola
lettura): in `EngagementTurnPlayer.play` l'attore di uno scambio **è** chi ha vinto l'iniziativa.
Da qui segue che nella battaglia `attackerIndex` è già il portatore dell'iniziativa, e non serve
nessuna inferenza per nome.

## Comportamento atteso

### 1. Il percorso è un dato, in un punto solo

- Esiste un tipo nuovo nel package `combat` — il percorso come **tabella di stazioni** — che
  descrive le dieci prove: numero, descrizione italiana breve, numero di sfidanti, forma dello
  scontro, e come nascono gli sfidanti (generati, oppure lo specchio del protagonista).
- Il percorso è esattamente questo, e va verificato da un test:

  | Prova | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
  | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
  | Sfidanti | 1 | 1 | 1 | 2 | 2 | 2 | 3 | 3 | 3 | specchio (1) |
  | Forma | duello | duello | duello | battaglia | battaglia | battaglia | battaglia | battaglia | battaglia | duello |

- La **forma non è un secondo campo da tenere d'accordo col primo**: è duello quando l'avversario è
  uno solo, battaglia quando sono più di uno, e si risolve alla lettura dal numero di sfidanti. Vedi
  la decisione 2.
- Le dieci descrizioni sono brevi e nel registro delle tre attuali, che restano al loro nuovo posto:
  1. «il primo avversario»
  2. «un altro sfidante, ancora uno solo»
  3. «il terzo scontro alla pari»
  4. «due contro uno»
  5. «di nuovo due insieme»
  6. «la terza coppia»
  7. «tre contro uno»
  8. «ancora tre insieme»
  9. «l'ultimo assalto in tre»
  10. «lo sfidante speculare, armato meglio»
- Lo **specchio si genera solo alla decima prova**, e solo quando quella prova viene giocata:
  `createMirrorRival` riceve la scheda com'è cresciuta nelle nove prove precedenti.

### 2. `Arena` scandisce il percorso, e continua a non decidere

- `run()` diventa una **piega sulle stazioni**: `RoundReport.entering(protagonist)` e poi, stazione
  per stazione, `andThen` — la stessa cortocircuitazione di oggi. Una prova non vinta chiude la
  corsa lì: nessuna voce di cronaca per le prove non giocate, e la conclusione dice esito e numero
  dell'ultima giocata, esattamente come adesso.
- I tre metodi privati per prova e le sei costanti cablate spariscono. Al loro posto: la stazione
  dice il numero, la descrizione, quanti sfidanti e con quale forma; `Arena` la traduce in una
  chiamata a `fightRound`, che non cambia.
- La scelta fra `createChallengers` e `createMirrorRival` è uno **switch esaustivo senza `default`**
  sull'origine degli sfidanti dichiarata dalla stazione; la scelta fra `playAsBattle` e `playAsDuel`
  è uno switch esaustivo su `TrialShape`. Nessun `if` di gioco entra in `Arena`: la stazione porta
  il dato, `Arena` dispaccia.
- Invariante: la procedura di fine scontro, la fotografia del roster prima dello scontro, il calcolo
  di `outcomeOf` e la struttura di `TrialChronicle` non cambiano.

### 3. La cronaca porta la lunghezza prevista del percorso

- `ArenaChronicle` guadagna un campo intero — la **lunghezza prevista** del percorso, dieci — che
  vale lo stesso anche quando la corsa si chiude alla prima prova. È un dato, non una stringa di
  presentazione, e rende la cronaca autosufficiente su un'informazione che oggi ogni lettore deve
  indovinare dal numero di voci.
- Il campo è **≥** del numero di voci giocate: è un invariante che il record verifica in
  costruzione, con un'eccezione esplicita se violato. Una cronaca con dodici prove giocate su dieci
  previste è un errore di programmazione, non un dato da servire alla pagina.
- Il valore arriva dalla dimensione della tabella delle stazioni: non è una costante scritta due
  volte.
- La chiave nuova entra nel JSON e nel test sulle chiavi. Se la pagina non la trovasse scriverebbe
  «Prova 1/undefined» in silenzio: quel test è la sola rete.

### 4. La pagina disegna tutto il percorso, senza rivelare dove finisce

- La pagina mostra il percorso come grafica, nello spirito di `O-----O-----X-----[]-----[]`: una
  stazione per ogni prova **prevista**, con le linee di collegamento, e tre stati visivamente
  distinti — passata, corrente, ancora da raggiungere.
- La stazione corrente si legge dal momento visualizzato: `trialNumber` nei momenti di prova e di
  procedura, `conclusion.lastTrial` nel momento di conclusione, che `trialNumber` non lo porta.
- **Vincolo di non-spoiler, il requisito centrale**: una stazione **mai giocata** deve essere
  indistinguibile da una **non ancora raggiunta**. Stessa grafica, stesso testo, stesso comportamento
  al passaggio del mouse, stessa interattività. Confrontando due partite — una persa alla prima prova
  e una arrivata in fondo — le stazioni oltre la corrente devono apparire identiche.
- Ne segue l'interattività: le stazioni sono cliccabili (per rivedere una prova) **solo fino a quella
  corrente**, mai in avanti. Un insieme di bersagli cliccabili che si fermasse dove finisce la corsa
  sarebbe lo stesso spoiler di prima, con un vestito nuovo.
- L'esito di una prova compare sulla stazione corrente **solo quando è già rivelato altrove**, cioè
  quando il momento porta già l'esito nell'intestazione (`showOutcome`, che è vero sull'ultimo passo
  della prova e sul momento di procedura) oppure nel momento di conclusione. Prima di allora la
  stazione corrente non anticipa niente.
- Il percorso **sostituisce e rimuove** i pulsanti `#trial-jump`, che perdono ragione d'essere e
  leggerebbero lo spoiler.
- Il denominatore dell'intestazione diventa la lunghezza prevista: «Prova 4/10» dal primo passo.
- Le stazioni sono **accessibili**: ogni stazione porta un'etichetta testuale leggibile (numero della
  prova, e il suo stato), non solo un simbolo grafico.

### 5. Chi attacca chi, fra le due schede

- `#battlefield` guadagna una **colonna centrale** fra le due colonne di schede. Per ogni scambio del
  passo corrente mostra: la **formula breve** dell'azione sopra, e sotto i due nomi con la freccia in
  mezzo. La squadra 0 resta a sinistra e la 1 a destra, come le colonne di schede e come la console:
  la freccia dice **chi dei due attacca**, non come sono disposti.
- L'orientamento segue la squadra dell'attaccante: attaccante in squadra 0 → freccia verso destra;
  in squadra 1 → verso sinistra. È la regola di `BattleSceneRenderer.arrowFor`.
- La formula breve **riusa il registro linguistico della console** — `colpisce (n)`, `critico (n)`,
  `colpo potente (n)`, `manca`, `parato (n)`, `schivato`, `riposa (+n)` — invece di inventarne un
  terzo. Il `describeAction` verboso del pannello testuale resta com'è: le due cose convivono, una
  nella colonna centrale e una nel pannello sotto.
- **Battaglia**: una voce per scambio, dagli `attackerIndex` e `targetIndex` di ogni
  `EngagementTurn` del round — gli stessi indici che `buildEngagement` già usa.
- **Duello**: il motore non porta indici. L'attore si risolve **nella pagina**, confrontando
  `initiative.chosenName` coi nomi del roster (che nel duello è per costruzione
  `[protagonista, rivale]`), e il bersaglio è l'altro dei due. È la stessa inferenza che
  `CombatScreenRenderer` fa già in Java. Vedi la decisione 5, che ne dichiara il limite.
- **Ripieghi onesti, mai una freccia sbagliata**:
  - iniziativa assente, oppure nome che non trova corrispondenza nel roster, oppure nome che ne trova
    più di una → **nessuna freccia**, e la voce mostra la sola formula;
  - azione di **riposo** (`REST`): c'è un attore ma nessun bersaglio → nessuna freccia, solo il nome
    dell'attore e `riposa (+n)`;
  - `action` assente → nessuna formula, come la stringa vuota di `formulaLabel`;
  - tipo di azione sconosciuto (il motore è `SNAPSHOT`) → si mostra il valore grezzo, come già fa
    `describeAction`, invece di una voce vuota.
- Sotto la soglia in cui `#battlefield` collassa a una colonna, la colonna centrale resta leggibile
  e non si sovrappone alle schede.

### 6. La stellina a chi ha l'iniziativa

- Nella scheda del combattente, **vicino al nome**, un segno per chi ha l'iniziativa nel passo
  corrente.
- **Battaglia**: la stellina va sull'`attackerIndex` di ogni scambio del round — l'attore di uno
  scambio *è* chi ha vinto l'iniziativa (fatto del motore, sopra). Un round con due scambi marca
  quindi due combattenti, uno per scambio, e non c'è nessun nullo da gestire.
- **Duello**: sul combattente il cui nome è `initiative.chosenName`, quando l'iniziativa è presente;
  **niente stellina** altrimenti.
- L'inferenza per nome del duello vive in **un punto solo**, condiviso da freccia e stellina: due
  risoluzioni separate dello stesso nome sono due cose che devono restare d'accordo per disciplina.
- Il segno è **accessibile**: non un carattere muto, ma un marcatore con etichetta esplicita che dica
  che quel combattente ha l'iniziativa.

### 7. La console racconta dieci prove

- Le due frasi che dicono «tre» dicono il numero previsto dal percorso: quella dell'ingresso e quella
  del trionfo. Il numero arriva come dato a chi compone la frase; la frase resta del renderer.
- Le prove con un solo sfidante — le prime tre e la decima — passano dal **duello a schermate**; le
  altre sei dalla **battaglia NvN**. È una conseguenza voluta della regola sulla forma, e cambia la
  presentazione di console di prove che oggi esistono (vedi *Rischi*).

## Vincoli

- **Nessuna regola di combattimento qui.** Niente formule di danno, iniziativa, stamina o momentum.
  La freccia, la formula e la stellina **leggono** dati che il motore ha già deciso.
- **Il motore e il toolkit non si toccano.** Nessun `mvn install` di quei repository in questo
  compito.
- **Nessuna nuova dipendenza**, né di produzione né di test. In particolare nessun modulo Jackson
  aggiuntivo: se servisse un `Optional` nel JSON, la risposta è cambiare il dato.
- **`Arena` non decide, scandisce.** Il percorso è un dato letto da `Arena`; le scelte del
  protagonista restano tutte in `HeroBrain`. Se serve un `if` di gioco, appartiene al cervello o al
  dato del percorso, non alla scansione.
- **La cronaca resta di soli dati** e autosufficiente: il campo nuovo è un numero, non una frase, e
  nessuna stringa di presentazione entra nei suoi record. Il criterio di lettura resta quello: un
  lettore nuovo deve poter comporre frasi, non aggiungere campi.
- **Direzione delle dipendenze invariata**: `replay` → `log` → `render`, `terminal` sotto tutti,
  `render` foglia; `combat.io.web` è un pozzo e non importa `render`/`log`/`terminal`/`replay`;
  l'unico importatore di `combat.io.web` resta `UiMode`. Il tipo nuovo del percorso vive in `combat`
  e può leggere `combat.chronicle` come già fa `Arena`; `combat.chronicle` non impara niente di
  nuovo.
- **La casualità resta dov'è**: `FighterFactory` per la generazione, `HeroBrain` per i punti
  caratteristica (deroga già dichiarata), il `DiceRoller` del motore per lo scontro. La tabella del
  percorso è deterministica: dieci stazioni fisse, nessuna estrazione.
- **Switch sulle enum di dominio esaustivi, senza `default`**: vale per la forma della prova,
  l'origine degli sfidanti e il tipo di azione, così una costante nuova diventa un errore di
  compilazione.
- **Pagina vanilla**: HTML/CSS/JavaScript senza librerie, senza CDN, senza build step, senza npm.
  Il testo entra nel DOM con `textContent`, mai con `innerHTML`. Nessun `fetch` in più: la cronaca
  si carica una volta sola all'apertura, e il percorso, la freccia e la stellina si disegnano da
  dati già in memoria.
- Java 21; indentazione **2 spazi** (`FighterFactory` resta a 4 se lo si tocca); Javadoc e commenti
  in **italiano**, identificatori in inglese; record per i tipi valore, switch expression; niente
  sealed type, text block, pattern matching, preview feature.
- Test JUnit Jupiter con le sole assertion di `org.junit.jupiter.api.Assertions`, **niente AssertJ**.
  Nessun test lancia `Main`; nel confine web la porta è sempre effimera.

## Fuori scope

- Qualunque modifica a `fantasy-combat-system` e `fantasy-game-toolkit`.
- **Arena a lunghezza configurabile** dall'utente o da riga di comando: il percorso resta cablato,
  ma in un punto solo. Renderlo parametrico è un altro compito, e questa tabella è il suo
  presupposto.
- Trasformare la console in lettrice della cronaca; unificare i due percorsi di presentazione.
- Controlli nuovi della pagina oltre al percorso cliccabile: play, pausa, passo, velocità e timeline
  restano quelli di oggi.
- Riuso o generalizzazione di `BattleSceneRenderer`/`CombatScreenRenderer` per il web: dalla console
  si prende il **registro linguistico**, non il codice.
- Mostrare nella pagina ciò che la cronaca già porta e la pagina ignora (dettaglio d'iniziativa coi
  breakdown, `Scorecard`, `TurnHighlight`, punteggi di squadra): resta lavoro di frontend per
  un'altra volta. Qui entra della stellina **soltanto** chi ha l'iniziativa, non perché.
- Test automatici del JavaScript: scelta dichiarata del progetto, la pagina si verifica a mano.
- Buff dell'equipaggiamento e gioielli in campo (`spec-equipment-buffs.md`).
- Ridurre le attese dell'INVIO in console, o cambiare `ReplayMode`: il percorso più lungo rende la
  corsa di console più lenta, ed è un rischio dichiarato, non un problema risolto qui.
- **Le due decisioni di bilanciamento** qui sotto, finché l'utente non le prende: la SPEC le pone,
  non le sceglie.

## Decisioni di progettazione sciolte in questa SPEC

1. **Il percorso è un tipo nuovo in `combat`, non un metodo di `HeroBrain`.** `HeroBrain` è il punto
   delle scelte **del protagonista** — cosa tenere, quanto vale un gioiello, dove spendere i punti —
   e la struttura del percorso non è una sua scelta: il protagonista non decide quanti avversari gli
   si parano davanti. Metterla lì confonderebbe due assi di bilanciamento diversi. `Arena` continua a
   scandire: legge la tabella, non la contiene.
2. **La forma della prova si deriva dal numero di sfidanti, non si custodisce.** Duello se
   l'avversario è uno, battaglia se sono più di uno: è la regola che l'utente ha dichiarato, quindi
   un secondo campo sarebbe una copia da tenere d'accordo con la prima per disciplina. Il precedente
   in casa è `RunConclusion.triumph()`, derivato da `outcome()` con la stessa motivazione scritta nel
   suo Javadoc. La tabella della SPEC resta la verità da verificare con un test: se un giorno
   servisse una battaglia contro un avversario solo — come **oggi** avviene alla prima prova — la
   forma tornerebbe a essere un campo, e sarà una decisione da prendere allora, con la sua
   motivazione.
3. **L'origine degli sfidanti è un'enum a due costanti, non un booleano `mirror`.** Uno switch
   esaustivo senza `default` fa diventare un errore di compilazione l'eventuale terza origine (un
   campione ricorrente, uno sfidante scriptato); un booleano no. È la convenzione già dichiarata del
   repository per le enum di dominio.
4. **La lunghezza prevista è un campo della cronaca, non un dato derivabile dal lettore.** Il lettore
   della pagina ha solo il JSON: non può contare le stazioni di una tabella Java. E derivarla dal
   numero di voci giocate è precisamente il difetto da correggere.
5. **L'attore del duello si risolve nella pagina, per nome — e questo è in tensione con
   l'autosufficienza della cronaca.** La tensione va registrata, non nascosta: la regola dichiarata
   in `CLAUDE.md` è che un lettore nuovo deve poter comporre frasi, non aggiungere campi, e qui il
   lettore fa un'**inferenza**. L'alternativa — fabbricare indici di roster per i turni del duello —
   è esclusa dal Javadoc di `TrialChronicle`: significherebbe inventare un dato che il motore non ha
   deciso, e il motore dichiara il nome inaffidabile come identificatore. Fra un dato inventato in
   cronaca e un'inferenza dichiarata nel lettore, si scegli la seconda, con tre mitigazioni:
   l'inferenza è la stessa che `CombatScreenRenderer` fa già in Java (quindi non è un precedente
   nuovo); dentro una corsa i nomi sono unici per costruzione (`FighterFactory.generateUniquelyNamed`
   con `usedNames`, protagonista compreso); e il ripiego è **nessuna freccia**, mai una freccia
   sbagliata. Se un domani il motore desse gli indici anche nel duello, questa inferenza va rimossa.
6. **Nella battaglia la stellina va su `attackerIndex`, non su `initiative.chosenName`.** L'attore di
   uno scambio è chi ha vinto l'iniziativa, quindi l'indice dice già tutto: è un dato senza nulli e
   senza inferenze, mentre il nome ne richiederebbe una anche dove non serve. Il duello resta l'unico
   posto in cui si passa dal nome, perché lì non esiste un indice.
7. **La formula breve si duplica fra Java e JavaScript, consapevolmente.** `formulaLabel` vive in
   `combat.io.render`, che la pagina non può e non deve importare: la cronaca non porta stringhe di
   presentazione proprio perché ogni lettore compone le proprie. È la continuazione della decisione
   già registrata per le otto frasi del destino del loot, non uno scivolone. Le sette etichette
   stanno in un punto solo del file JavaScript, accanto alle altre costanti linguistiche.
8. **Il percorso si disegna sotto l'intestazione della prova, non nella barra dei controlli.** È
   prima di tutto una mappa — dice dove siamo — e solo in secondo luogo un comando. Sta accanto al
   testo che dice «Prova 4/10» perché è la stessa informazione in forma grafica. È la decisione più
   facile da rivedere di tutta questa SPEC: cambia una posizione nel DOM e qualche riga di CSS.
9. **La piega si scrive col ciclo, non con `reduce` a tre argomenti.** Una `reduce` sequenziale su
   `RoundReport` richiederebbe un combinatore mai invocato, cioè una riga che esiste per il
   compilatore e non per il lettore. Un ciclo che concatena `andThen` conserva la cortocircuitazione
   e si legge. È indicazione, non vincolo: se la revisione funzionale trova una forma migliore che
   mantiene la cortocircuitazione e non introduce codice morto, va bene.

## Questioni aperte — richiedono una decisione dell'utente

Nessuna delle due blocca la parte principale del compito, e per questo il piano le isola in una fase
propria. Entrambe hanno una proposta concreta, che resta una proposta.

### Q1 — Gli scaglioni della rarità del loot su dieci livelli

**Il fatto.** `lootRarityTable` ha due scaglioni tarati su tre prove: pavimento `UNCOMMON` alla
prima, `RARE` da lì in poi. Su dieci livelli il secondo scaglione coprirebbe **nove** prove su dieci,
cioè la progressione della rarità sparirebbe dopo il primo scontro. C'è anche un effetto meno
evidente: il protagonista tiene solo ciò che batte quel che ha già, e gli slot sono finiti (un pezzo
per parte del corpo, un gioiello per tipo — il limite è già gestito da `generateArmourSet`), quindi
un pavimento troppo basso a metà percorso produce loot **inerte**: trovato, scartato, e la vittoria
vale i suoi tre punti e nulla più.

**Proposta: quattro scaglioni, con il `LEGENDARY` che resta l'eccezione a ogni scaglione.** La
distribuzione pesata è il punto del disegno attuale — un pavimento espresso come rarità minima
renderebbe il `LEGENDARY` tanto probabile quanto il grado del pavimento — e alzare il pavimento non
deve tradirlo:

| Prove | UNCOMMON | RARE | EPIC | LEGENDARY |
| --- | --- | --- | --- | --- |
| 1-2 | 50 | 24 | 16 | 10 |
| 3-5 | — | 48 | 32 | 20 |
| 6-8 | — | 25 | 50 | 25 |
| 9-10 | — | — | 65 | 35 |

I primi due scaglioni sono le due tabelle di oggi, riusate senza ritararle; i due nuovi spostano il
peso verso l'alto mantenendo il grado più pregiato minoritario. `HeroBrainTest` va riscritto: i suoi
due test asseriscono l'`assertSame` fra livello 2 e 3 (che con questa proposta resta vero, entrambi
nel secondo scaglione — ma per ragione diversa, quindi il test va reso esplicito) e i pavimenti.

**Alternativa scartata:** tre scaglioni (1-3, 4-7, 8-10). Più semplice, ma il primo coprirebbe tutte
e tre le prove a un avversario, cioè tutta la prima fase, e la crescita si sentirebbe solo dopo.

### Q2 — Il monte punti degli sfidanti deve crescere col livello?

**Il fatto, con i numeri.** Il protagonista guadagna da 3 a 7 punti caratteristica per vittoria (tre
fissi più il bonus del gioiello, 1-4 secondo la rarità); dopo nove vittorie sono **circa 27-60 punti
in più** sui 15 di partenza, oltre a un'arma e un'armatura migliori. Gli sfidanti nascono
**sempre** a `TOTAL_CHARACTERISTIC_POINTS = 15` e `UNCOMMON`. Aumentare il solo numero di avversari
non compensa un divario che cresce così: dalla metà del percorso in poi le prove rischiano di
diventare una passeggiata.

C'è però un dato che tira nella direzione opposta, e va messo sul tavolo prima di decidere: il
registro di `implementation-web-replay.md` riporta che **su trentaquattro corse di console solo la
trentaquattresima è arrivata in fondo**, e il muro era la seconda prova — due contro uno con il
protagonista cresciuto una sola volta. Nel percorso nuovo il due-contro-uno arriva alla **quarta**
prova, col protagonista cresciuto **tre** volte: la curva è quindi più gentile di oggi nella prima
metà, e la domanda vera riguarda le prove 7-10, tre avversari contro un protagonista cresciuto sei
volte o più.

**Proposta A (preferita): il monte punti degli sfidanti è un dato della stazione.** La stazione
porta, accanto al numero di sfidanti, il monte punti con cui nascono; `createChallengers` guadagna
quel parametro. La difficoltà diventa così parte della **tabella del percorso**, cioè visibile in un
colpo d'occhio e ritarabile in un punto solo, coerente con l'obiettivo di questo compito. Una curva
di partenza plausibile: `15 + 3 * (numero della prova - 1)`, da 15 a 42 — sotto la crescita del
protagonista nel caso fortunato, sopra nel caso sfortunato, e con l'aumento del numero di avversari
che fa il resto.

- Impatto: `FighterFactory.createChallengers(int count)` → `createChallengers(int count, int
  totalCharacteristicPoints)`, con la validazione del monte punti; `generateWarrior` deve accettare
  il monte punti (oggi usa la costante). Attenzione a un dettaglio già risolto altrove:
  `generateRival` disattiva i bonus di razza e classe con due tabelle vuote proprio perché altrimenti
  si sommerebbero al monte punti richiesto, e lo sfidante «pari» nascerebbe più forte. La stessa
  cautela va valutata qui, altrimenti la curva scelta non è la curva ottenuta.
- Test: la tabella del percorso verifica anche i monte punti; `FighterFactoryTest` verifica che gli
  sfidanti sommino quello che è stato chiesto.

**Proposta B: il monte punti degli sfidanti si ricava dal protagonista.** Gli sfidanti nascono con i
punti del protagonista corrente, scalati per il loro numero (per esempio 100% se è uno, 70% ciascuno
se sono due, 55% ciascuno se sono tre), come già fa `createMirrorRival` con `generateRival`. Il
divario resta costante per costruzione e non va ritarato.

- Contro: la difficoltà smette di essere leggibile nella tabella e diventa una formula; una corsa
  fortunata si punisce da sé, che è un incentivo perverso; e la scelta dei coefficienti è comunque
  arbitraria, solo meno visibile.

**Terza via, non alternativa alle prime due:** far crescere col livello la **rarità
dell'equipaggiamento** degli sfidanti (`STANDARD_EQUIPMENT_RARITY` diventa una funzione della
stazione) invece o oltre il monte punti. Colpisce i Rating per una via diversa e mantiene i
personaggi «alla pari», che è il vocabolario del progetto. Si può combinare con A.

**Serve una risposta su:** quale proposta (A, B, terza via, combinazione, o nessuna), e se cambiare
qualcosa in Q1. Finché non arriva, il percorso resta a `15` punti per tutti gli sfidanti — cioè il
comportamento di oggi esteso a dieci prove — e la fase di bilanciamento del piano resta `BLOCKED`.

## Rischi noti

- **La corsa di console diventa molto più lunga.** Dieci prove invece di tre, con l'INVIO fra l'una e
  l'altra, l'INVIO a ogni round nelle sei battaglie e l'INVIO a **ogni turno** nei quattro duelli a
  schermate: le attese passano da decine a centinaia. Non si risolve qui (ridurre le attese o
  cambiare `ReplayMode` è fuori scope), ma la verifica manuale deve usare il trucco già documentato —
  `stdin` chiuso, su cui il `TurnPacer` prosegue senza bloccarsi.
- **Le prove 1-3 cambiano presentazione in console**: oggi la prima prova, contro un avversario solo,
  passa dal percorso battaglia; con la regola sulla forma passerà dal duello a schermate. È una
  conseguenza voluta della richiesta, ma è un cambiamento visibile di prove che oggi esistono, e
  riscrive l'aspettativa di `ArenaTest.soloLaProvaFinaleUsaIlDuelloASchermate`.
- **Arrivare alla decima prova sarà raro**, più di quanto sia raro oggi arrivare alla terza. Ne segue
  un costo concreto sulla verifica manuale: gli stati «percorso quasi completo», la stazione dello
  specchio e il duello finale si vedono solo in una corsa lunga, e nella pagina ogni ricarica è una
  partita nuova che non si può pilotare. La verifica va fatta ricaricando finché non capita una corsa
  lunga — come già servì fare in `implementation-web-replay.md` — e la risposta a Q2 cambia
  direttamente quanto costa.
- **Il contratto verso il JavaScript passa da un solo test.** Aggiungere il campo alla cronaca senza
  aggiungere la chiave a `ChronicleJsonTest` lascerebbe la rete bucata proprio dove la si sta
  allargando.
- **Lo spoiler può rientrare dalla finestra.** Il vincolo di non-spoiler non riguarda solo il
  disegno: riguarda i bersagli cliccabili, i `title`, gli attributi, l'ordine del DOM. Il modo di
  verificarlo non è guardare una partita, è **confrontarne due** di lunghezza diversa.
- **Perdita residua e dichiarata:** il denominatore della timeline è il numero totale di *momenti*
  della registrazione, quindi una registrazione corta suggerisce comunque una corsa corta. Non si
  corregge qui: nasconderla vorrebbe dire rinunciare alla timeline, che è uno dei controlli per cui
  la modalità web esiste.
- **La griglia a tre colonne su schermo stretto.** `#battlefield` collassa a una colonna sotto 720px:
  la colonna centrale va pensata anche lì, altrimenti la freccia finisce fuori posto fra due schede
  impilate.
- **La trappola già documentata**: fermare il processo Maven non uccide la JVM generata, che resta in
  ascolto e tiene la porta. Prima di credere a una verifica sul server, controllare *chi* ascolta
  (`netstat -ano | grep 8080`) e chiudere quel PID.
- **Il ribilanciamento tocca `HeroBrainTest` e `FighterFactoryTest`**, cioè test che oggi passano: se
  le due questioni aperte si risolvono, va cambiata l'aspettativa, non il test per farlo passare.

## Definition of done

Criteri verificabili, ognuno coperto da almeno un test — tranne dove è dichiarato che la verifica è
in revisione o a mano.

1. La tabella del percorso produce **dieci** stazioni, numerate da 1 a 10, con i conteggi di sfidanti
   `1,1,1,2,2,2,3,3,3,1` e le forme `duello,duello,duello,battaglia,battaglia,battaglia,battaglia,
   battaglia,battaglia,duello`; l'origine speculare compare **solo** alla decima; le dieci
   descrizioni sono presenti, non vuote e tutte diverse.
2. `Arena.run()` gioca le dieci prove in fila quando sono tutte vinte: dieci voci di cronaca, coi
   conteggi di sfidanti del percorso prova per prova.
3. `createMirrorRival` è invocato **una volta sola**, alla decima prova, e riceve la scheda del
   protagonista **com'è cresciuta** nelle nove prove precedenti.
4. La cortocircuitazione resta: una prova non vinta chiude la corsa lì — nessuna voce per le prove
   non giocate, nessuna procedura di fine scontro su una prova non vinta, e la conclusione dice esito
   e numero dell'ultima prova giocata.
5. Ogni prova con un solo sfidante passa dal duello e ogni prova con più di uno dalla battaglia,
   verificato sulla presentazione effettivamente scelta.
6. La cronaca porta la lunghezza prevista del percorso e vale **dieci** anche quando la corsa si
   chiude alla prima prova; una cronaca con più voci giocate che prove previste è rifiutata in
   costruzione.
7. Il JSON contiene la chiave della lunghezza prevista, e il test sulle chiavi la asserisce.
8. Le frasi di console dell'ingresso e del trionfo dicono il numero previsto dal percorso e non più
   «tre».
9. La pagina disegna una stazione per ogni prova prevista, con le linee di collegamento e tre stati
   distinti; la stazione corrente si ricava da `trialNumber` nei momenti di prova e da
   `conclusion.lastTrial` nel momento di conclusione (a mano).
10. **Non-spoiler**: confrontando una corsa persa presto e una corsa lunga, le stazioni oltre quella
    corrente sono identiche nell'aspetto e nel comportamento; nessuna stazione oltre la corrente è
    cliccabile; i pulsanti `#trial-jump` non esistono più (a mano).
11. La colonna centrale di `#battlefield` mostra una voce per scambio con la formula breve sopra e i
    due nomi con la freccia sotto, orientata secondo la squadra dell'attaccante e coerente col
    registro di `BattleSceneRenderer` (a mano, confrontando con una corsa di console).
12. I ripieghi della colonna centrale: `REST` non produce freccia; iniziativa assente o nome del
    duello senza corrispondenza univoca non producono freccia; nessun caso produce una freccia
    sbagliata o spegne la pagina (a mano).
13. La stellina compare sull'attaccante di ogni scambio nella battaglia e sul portatore
    dell'iniziativa nel duello, non compare quando l'iniziativa manca, ed è accompagnata da
    un'etichetta testuale leggibile (a mano).
14. La pagina continua a fare **un solo** `fetch`, all'apertura (a mano, col pannello di rete).
15. Nessuna nuova dipendenza; nessuna formula di combattimento nata qui; direzione delle dipendenze
    di `combat.io` invariata; nessuna stringa di presentazione nuova nei record della cronaca
    (in revisione).
16. La suite è verde, con le aspettative adeguate dove il percorso le ha rese false
    (`ArenaTest`, `ChronicleJsonTest`, e `HeroBrainTest`/`FighterFactoryTest` solo se il
    ribilanciamento viene deciso).
17. `CLAUDE.md`, `README.md` e `daImplementare.md` sono aggiornati: dieci prove al posto di tre, la
    forma derivata dal numero di sfidanti, il percorso come dato, il campo nuovo della cronaca e la
    voce del percorso disegnato nella pagina (in revisione).
18. **Condizionato alle risposte di Q1/Q2**: gli scaglioni di rarità e/o il monte punti degli
    sfidanti sono quelli decisi, con un test che li fissa.

## Esempio (istanza concreta — solo illustrativo)

Il percorso come dato, nel package `combat`:

```java
/**
 * Il percorso dell'arena: dieci stazioni in fila, dalla prima prova allo specchio. È l'unico posto
 * che sa com'è fatta la corsa — quante prove, contro quanti, in quale forma — e {@code Arena} lo
 * legge senza contenerlo.
 */
public record TrialPlan(List<TrialStation> stations) {

  private static final int GENERATED_TRIALS_PER_CHALLENGER_COUNT = 3;
  private static final int MAX_CHALLENGERS = 3;

  public static TrialPlan standard() {
    List<TrialStation> stations = new ArrayList<>();
    for (int challengerCount = 1; challengerCount <= MAX_CHALLENGERS; challengerCount++) {
      for (int repetition = 0; repetition < GENERATED_TRIALS_PER_CHALLENGER_COUNT; repetition++) {
        int number = stations.size() + 1;
        stations.add(new TrialStation(number, DESCRIPTIONS.get(number - 1), challengerCount,
            ChallengerOrigin.GENERATED));
      }
    }
    int mirrorNumber = stations.size() + 1;
    stations.add(new TrialStation(mirrorNumber, DESCRIPTIONS.get(mirrorNumber - 1), 1, ChallengerOrigin.MIRROR));
    return new TrialPlan(stations);
  }

  public int length() {
    return stations.size();
  }
}

/**
 * Una stazione del percorso. La {@link #shape()} non è un componente: è duello quando l'avversario è
 * uno solo, battaglia quando sono più di uno, e custodirla accanto al numero di sfidanti creerebbe
 * due campi che devono restare d'accordo per disciplina di chi costruisce il record.
 */
public record TrialStation(int number, String description, int challengerCount, ChallengerOrigin challengerOrigin) {

  public TrialShape shape() {
    return challengerCount == 1 ? TrialShape.DUEL : TrialShape.BATTLE;
  }
}

/**
 * Come nascono gli sfidanti di una stazione. Enum e non booleano: una terza origine deve diventare
 * un errore di compilazione negli switch che la leggono, non un ramo dimenticato.
 */
public enum ChallengerOrigin {
  GENERATED,
  MIRROR
}
```

`Arena` piega il percorso, con la cortocircuitazione di oggi:

```java
public ArenaChronicle run() {
  Hero protagonist = enterTheArena();
  HeroSnapshot protagonistSnapshot = chronicleMapper.snapshotHero(protagonist);

  RoundReport lastRound = RoundReport.entering(protagonist);
  for (TrialStation station : plan.stations()) {
    lastRound = lastRound.andThen(previous -> fightStation(station, previous));
  }

  if (lastRound.passed()) {
    logger.reportTriumph(lastRound.grownHero(), plan.length());
  }

  TrialChronicle lastTrial = lastRound.trials().getLast();
  return new ArenaChronicle(protagonistSnapshot, plan.length(), lastRound.trials(),
      new RunConclusion(lastTrial.outcome(), lastTrial.number()));
}

private RoundReport fightStation(TrialStation station, RoundReport previous) {
  List<Fighter> challengers = challengersFor(station, previous.grownHero());
  return fightRound(station.number(), station.description(), previous, challengers, playFor(station));
}

private List<Fighter> challengersFor(TrialStation station, Hero hero) {
  return switch (station.challengerOrigin()) {                 // esaustivo, nessun default
    case GENERATED -> fighterFactory.createChallengers(station.challengerCount());
    case MIRROR -> List.of(fighterFactory.createMirrorRival(hero));
  };
}

private FightPlay playFor(TrialStation station) {
  return switch (station.shape()) {                            // esaustivo, nessun default
    case BATTLE -> this::playAsBattle;
    case DUEL -> this::playAsDuel;
  };
}
```

La forma del JSON, con la sola chiave nuova alla radice:

```
{ "protagonist": { ... },
  "plannedTrials": 10,
  "trials": [ { "number": 1, "shape": "DUEL", ... } ],
  "conclusion": { "outcome": "FELL", "lastTrial": 3 } }
```

E i tre punti della pagina, in `app.js`. Il momento porta **dati**, il DOM li disegna:

```javascript
// Le sette etichette brevi, nel registro di BattleSceneRenderer.formulaLabel: la formula vive due
// volte, in Java per la console e qui per la pagina, perché combat.io.render è fuori portata e la
// cronaca non porta stringhe di presentazione. Stessa decisione delle frasi del destino del loot.
const ACTION_FORMULAS = {
  HIT: action => action.critical ? `critico (${action.damage})`
      : action.powerStrike ? `colpo potente (${action.damage})` : `colpisce (${action.damage})`,
  MISS: () => 'manca',
  PARRIED: action => `parato (${action.damage})`,
  DODGED: () => 'schivato',
  REST: action => `riposa (+${action.staminaRecovered})`,
};

// Un solo punto risolve il nome in indice di roster, e lo usano sia la freccia sia la stellina:
// null se il nome non trova corrispondenza o ne trova più di una. Mai una freccia sbagliata.
function resolveRosterIndexByName(roster, name) {
  const matches = roster.filter(fighter => fighter.name === name);
  return matches.length === 1 ? matches[0].rosterIndex : null;
}

// La stazione oltre la corrente è disegnata e trattata sempre allo stesso modo, che sia stata
// giocata o no: è tutto il vincolo di non-spoiler, in una funzione.
function stationStateOf(stationNumber, currentTrialNumber) {
  if (stationNumber < currentTrialNumber) {
    return 'passed';
  }
  return stationNumber === currentTrialNumber ? 'current' : 'future';
}
```
