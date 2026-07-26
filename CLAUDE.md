# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Cos'è questo progetto

`fantasy-arena` è il **gioco**: applicazione console Java 21 (Maven) che genera i combattenti, ne
dispone lo scontro e lo mostra a schermo. **Le regole di combattimento non vivono qui**: stanno nella
libreria `fantasy-combat-system`, usata come dipendenza Maven, che calcola l'intero scontro e
restituisce il log completo senza stampare niente. Questo repository possiede generazione,
orchestrazione e presentazione — e, in prospettiva, progressione e avanzamento.

Per la descrizione discorsiva del gioco e del confine col motore vedi `README.md`.

Il punto d'ingresso è `it.fantasyarena.Main`: chiede la numerosità delle due fazioni, genera i
combattenti equi-equipaggiati con `FighterFactory` e li affida ad `Arena`. Con 1 vs 1 usa il percorso
duello a schermo, con qualunque altra numerosità il percorso battaglia NvN.

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
| `it.fantasyarena` | `Main`: thin. Chiede gli input, genera i combattenti, delega ad `Arena`. |
| `combat` | `Arena`: match runner. Chiede lo scontro al `CombatSystem` e ne dispone la presentazione col ritmo giusto. Nessuna formula, nessuna regola. |
| `combat.factory` | `FighterFactory`: unico punto di contatto coi generatori del toolkit. Decide chi combatte e con che equipaggiamento, poi delega l'assemblaggio al `FighterAssembler` del motore. |
| `combat.io` | Presentazione: `CombatLogger`/`ConsoleCombatLogger`, `ConsoleBattleLogger`, `CombatReplay` con modalità `LINEAR`/`SCREEN`, `BattleSceneRenderer` (scena ASCII NvN), `CombatScreenRenderer`, `FighterCardFormatter`, `TurnPacer`, `ScreenRefresh`, `ScreenCleaner`, `CombatSetupPrompt`. |

Vincoli architetturali da rispettare quando modifichi:

- **Niente regole di combattimento qui.** Nessun calcolo di danno, iniziativa, stamina o momentum:
  quei numeri arrivano già calcolati dal motore. Se ti trovi a scrivere una formula, sei nel
  repository sbagliato.
- **I renderer producono righe di testo pure, senza I/O.** L'I/O vero (stampa, lettura dell'INVIO,
  pulizia dello schermo) sta nei logger, nel `TurnPacer` e nello `ScreenCleaner`. È questa
  separazione che rende i renderer testabili sul testo prodotto.
- **`Arena` non contiene logica di scontro**: chiede il log completo al motore e decide solo *quando*
  e *come* mostrarlo. Il duello 1v1 e la battaglia NvN sono due percorsi di **presentazione**, non
  due motori.
- **La casualità della generazione sta in `FighterFactory`.** Nel resto del gioco niente
  `Math.random()` o `new Random()`; la casualità del combattimento è del motore (`DiceRoller`).
- `Main` resta thin: chiede gli input, genera i combattenti, delega ad `Arena`.

## Test

Suite JUnit Jupiter 5.x sotto `src/test/java`, con surefire configurato in `pom.xml`. Copre i
formatter/renderer di `combat.io` e la generazione di `combat.factory`. I test del motore vivono nel
repository del motore.

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
