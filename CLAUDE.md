# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Cos'è questo progetto

`fantasy-arena` è il **gioco**: applicazione console Java 21 (Maven) che genera i combattenti, ne
dispone lo scontro e lo mostra a schermo. **Le regole di combattimento non vivono qui**: stanno nella
libreria `fantasy-combat-system`, usata come dipendenza Maven, che calcola l'intero scontro e
restituisce il log completo senza stampare niente. Questo repository possiede generazione,
orchestrazione e presentazione — e, in prospettiva, progressione e avanzamento.

Per la descrizione discorsiva del gioco e del confine col motore vedi `README.md`.

Il punto d'ingresso è `it.fantasyarena.Main`, ridotto all'osso: apre `Arena` e le lascia condurre la
partita. `Arena` genera un protagonista e gli fa affrontare tre prove in fila (un avversario, poi
due insieme, poi uno sfidante speculare con arma rara), con la **procedura di fine scontro** fra
l'una e l'altra: cura completa, un oggetto di loot, tre punti caratteristica. Il loot non si
saccheggia dai caduti: a ogni livello vinto se ne genera **uno solo**, di tipo estratto a caso fra
arma, armatura e gioiello, con una rarità estratta da una tabella pesata che si fa più generosa col
livello (alla prima prova si parte da `UNCOMMON`, dalla seconda il grado più basso estraibile è
`RARE`). La distribuzione è pesata e non uniforme di proposito: una semplice rarità minima renderebbe
il `LEGENDARY` tanto probabile quanto il grado del pavimento, e il loot sopra il raro diventerebbe la
norma invece dell'eccezione. Arma, armatura e gioiello si tengono solo se battono quel che il protagonista ha già — il
gioiello uno per tipo, come l'armatura uno per slot, e il criterio è la rarità perché è l'unico
numero che un gioiello ha — e quello preso vale anche punti caratteristica extra. Indossato o no,
il gioiello resta fuori dallo scontro: il motore non sa montarlo. Le prime due prove sono mostrate
col percorso battaglia NvN — anche quella contro un avversario solo — mentre la prova finale,
essendo un uno-contro-uno, usa il duello a schermate.

## Comandi

```bash
mvn compile              # compila
mvn test                 # esegue i test JUnit 5 (surefire)
mvn exec:java            # esegue it.fantasyarena.Main (mainClass da pom.xml, exec.mainClass)
mvn package              # produce il jar in target/
```

## Dipendenze critiche: due SNAPSHOT locali

Nessuna delle due è su Maven Central: sono risolte solo dal repository Maven locale
(`~/.m2/repository/...`) e vanno buildate e installate (`mvn install`) dai rispettivi progetti
sorgente prima di poter compilare qui. Se `mvn compile` fallisce con artifact non risolto, il
problema è quasi sempre questo, non il codice di `fantasy-arena`.

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
| `it.fantasyarena` | `Main`: thin. Apre `Arena` e basta. |
| `combat` | `Arena`: la progressione. Scandisce le tre prove del protagonista e la procedura di fine scontro; non decide niente e non calcola niente. Ogni prova le restituisce un rapporto (`RoundReport`, record privato: l'esito e la scheda cresciuta se c'è) invece di un `Optional`, e i rapporti si concatenano cortocircuitando alla prima prova non superata. `RoundOutcome`: com'è finita una prova (`WON`/`FELL`/`STOOD_WITHOUT_WINNING`), calcolato una volta sola e passato a chi lo deve raccontare. `MatchRunner`: fa giocare **un singolo scontro** e lo mette in scena, con `playDuel` (uno-contro-uno a schermate) o `playBattle` (NvN, scena ASCII round per round). Chiede l'esito al `CombatSystem` e decide solo con che ritmo rivelarlo. |
| `combat.hero` | Il protagonista: `Hero` (scheda immutabile che sopravvive ai round: arma, armatura per slot, gioielli per tipo), `HeroBrain` (**tutte** le sue scelte, compresi i tre comparatori di cernita e le due tabelle di bilanciamento del loot — distribuzione pesata della rarità per livello e punti extra del gioiello), `Loot` (l'unico oggetto trovato a fine livello: arma, armatura o gioiello, mai più di uno), `HeroProgress` (resoconto della crescita, dati e non stringhe). |
| `combat.factory` | `FighterFactory`: unico punto di contatto coi generatori del toolkit. Decide chi combatte e con che equipaggiamento, poi delega l'assemblaggio al `FighterAssembler` del motore. Genera anche il protagonista, gli sfidanti dei tre round e lo specchio finale, materializza il `Fighter` di ogni round da una `Hero` (`summon`) ed estrae il loot di fine livello (`rollLoot`: tipo a caso, rarità estratta dalla tabella pesata ricevuta dal cervello). |
| `combat.io` | Solo contenitore: la presentazione vive nei quattro sotto-package che seguono, disposti a strati con dipendenze a senso unico (`replay` → `log` → `render`, e sia `replay` sia `log` → `terminal`). Nessuna classe sta direttamente qui. |
| `combat.io.render` | Righe di testo pure, nessun I/O: `TurnLogFormatter` (il turno), `FighterCardFormatter` (la scheda del combattente), `BattleSceneRenderer` (scena ASCII NvN) col suo `FighterProfile`, `CombatScreenRenderer` (la pagina del duello a schermate), `HeroProgressFormatter` (la procedura di fine scontro). Strato foglia: non dipende da nessun altro sotto-package di `io`. |
| `combat.io.terminal` | Il terminale e basta: `ScreenCleaner` e `ScreenRefresh` (pulizia dello schermo), `TurnPacer`/`EnterKeyTurnPacer` (il ritmo fra un turno e l'altro), `CombatSetupPrompt` (lettura delle preferenze). Strato foglia, indipendente dal `render`: qui vive l'I/O che non ha niente da formattare. |
| `combat.io.log` | Chi stampa: `CombatLogger`/`ConsoleCombatLogger` (il duello), `ConsoleBattleLogger` (la battaglia NvN), `ConsoleArenaLogger` (la voce dell'arena fra uno scontro e l'altro). Compone le righe chiedendole al `render` e le manda a schermo. Racconta la fine della corsa leggendo il `RoundOutcome` che riceve, non interrogando il `Fighter`. |
| `combat.io.replay` | Il ritmo della rivelazione: `CombatReplay` con le due strategie `LinearCombatReplay`/`ScreenCombatReplay` e la `ReplayMode` (`LINEAR`/`SCREEN`) che le sceglie. Strato più alto: mette insieme logger, renderer e terminale, e decide solo *quando* mostrare cosa. |

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
- **`MatchRunner` non contiene logica di scontro**: chiede il log completo al motore e decide solo *quando*
  e *come* mostrarlo. Il duello 1v1 e la battaglia NvN sono due percorsi di **presentazione**, non
  due motori. Ne servono due istanze quando servono entrambi i percorsi: ognuna costruisce alla
  prima chiamata il proprio `TurnPacer`, e condividerne una sola porterebbe il suggerimento
  sbagliato nell'altro percorso.
- **`Arena` non decide, scandisce.** Le scelte del protagonista stanno tutte in `HeroBrain`, che è
  il punto unico da toccare per ribilanciare la progressione: se tenere l'oggetto trovato, quanto
  vale un gioiello, quanto pregiato può essere il loot di quel livello, dove spendere i punti. Se ti
  trovi a scrivere un `if` di gioco dentro `Arena`, appartiene al cervello. `Arena` gli passa il
  numero della prova, non un criterio.
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
- **La casualità della generazione sta in `FighterFactory`.** Nel resto del gioco niente
  `Math.random()` o `new Random()`; la casualità del combattimento è del motore (`DiceRoller`).
  Unica deroga dichiarata: `HeroBrain`, che estrae dove cadono i punti caratteristica — è casualità
  di progressione, non di generazione, e il `Random` è iniettabile perché i test la piloti.
- `Main` resta thin: apre l'arena e si fa da parte.

## Test

Suite JUnit Jupiter 5.x sotto `src/test/java`, con surefire configurato in `pom.xml`. Copre i
formatter/renderer di `combat.io` e la generazione di `combat.factory`. I test del motore vivono nel
repository del motore.

I test seguono i sotto-package di `combat.io`: il grosso sta in `render`, dove il testo prodotto è
verificabile senza far girare niente. `replay` è l'unico sotto-package senza test propri, e non è
una lacuna da colmare per completezza: i due `CombatReplay` sono orchestratori sottili, e il testo
che mostrano è già coperto dai test dei renderer.

- Assertion solo con `org.junit.jupiter.api.Assertions` (**niente AssertJ**). Non aggiungere dipendenze di test se non strettamente necessario.
- Il supporto ai test arriva dal `test-jar` del motore: `it.fantasycombatsystem.testsupport.CombatFixtures` per costruire `Fighter` deterministici, `StubDiceRoller` e `RecordingStubDiceRoller` per pilotare i dadi.
- I test dei renderer verificano il **testo prodotto**, non gli effetti a schermo: costruiscono un log di combattimento a mano e controllano le righe risultanti.

## Documentazione nel repo

- `README.md` — descrizione non tecnica del gioco e del confine col motore. È il posto dove spiegare
  il *perché*: non ripetere qui in `CLAUDE.md` quello che è già raccontato là.
- `daImplementare.md` — elenco delle funzionalità di gioco non ancora realizzate.
- La guida al bilanciamento (`combatSettings.md`) e la spiegazione delle regole di combattimento
  vivono nel repository `fantasy-combat-system`: non duplicarle qui, divergerebbero.

## Convenzioni di questo repo

- Java 21 (`maven.compiler.source/target=21`). In uso: record per i tipi valore, switch expression, `List.getFirst()`. Non in uso: sealed types, text block, pattern matching (né `instanceof` né `switch`). Non introdurre preview feature.
- Indentazione a **2 spazi**. `FighterFactory` è l'unica eccezione storica a 4 spazi: se la modifichi, mantieni lo stile del file.
- Commenti e Javadoc in **italiano**. Il Javadoc di classe è denso e spiega le decisioni di design e i confini di responsabilità, non ripete la firma: mantieni questo registro quando aggiungi classi.
- Nomi di classi, metodi e variabili in inglese.
