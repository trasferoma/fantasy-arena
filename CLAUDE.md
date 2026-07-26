# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Cos'è questo progetto

`fantasy-arena` è un'applicazione console Java 21 (Maven) che simula scontri fra personaggi generati
dalla libreria esterna `fantasytoolkit`. La libreria fornisce la **materia prima** (personaggi, dadi,
armi, armature, nomi); il **motore di combattimento** è dominio locale di questo repository e
costituisce la gran parte del codice: rating intrinseci, iniziativa, colpo/difesa/danno, stamina,
momentum, colpo potente, battaglia NvN a squadre e resa a schermo round per round.

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

## Dipendenza critica: fantasytoolkit

- Unica dipendenza di produzione: `it.fantasytoolkit:fantasytoolkit:1.0-SNAPSHOT`.
- **Non è su Maven Central**: è risolta solo dal repository Maven locale (`~/.m2/repository/it/fantasytoolkit/...`). Deve essere buildata e installata (`mvn install`) dal suo progetto sorgente separato prima di poter compilare qui. Se `mvn compile` fallisce con artifact non risolto, il problema è quasi sempre questo, non il codice di `fantasy-arena`.
- Essendo `SNAPSHOT`, l'API della libreria può cambiare: verifica sempre le firme reali invece di assumerle.
- Per l'uso del toolkit consulta la documentazione indicizzata da
  `C:/build/git/fantasy-game-toolkit/docs/agent/INDEX.md` nel repository della versione utilizzata.
  **Non leggere i sorgenti e non decompilare il JAR.** Se l'API necessaria non è documentata, segnala
  la lacuna.

### Pattern d'uso dei tool della libreria

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
`building() → config → terminale → lettura del result`, non sparso nel motore.

Tool della libreria: `charactergenerator`, `dicelauncher`, `namegenerator`, `weapongenerator`,
`armourgenerator`, `jewelgenerator`, `potiongenerator`, `buffdebuffgenerator`, `dungeongenerator`.
Modelli ed enum condivise in `it.fantasytoolkitcore.core.model` (`Race`, `CharacterClass`, `Rarity`,
`Weapon`, `Armour`, ...).

## Architettura

Tutto sotto `it.fantasyarena.combat`, con separazione netta fra formule, orchestrazione e
presentazione.

| Package | Ruolo |
| --- | --- |
| `config` | `CombatSettings`: record annidati di pesi/costi/soglie, nessuna formula. `CombatFormulas`: fonte unica di verità delle formule, metodi statici puri e deterministici, dipende solo dai pesi e da primitivi. |
| `model` | `Fighter` (identità + equipaggiamento + `IntrinsicRatings` + `FighterState` mutabile), `Characteristics`. |
| `rating` | `RatingStrategy` / `DefaultRatingStrategy`: calcolano i rating intrinseci **una sola volta**, alla creazione del Fighter. |
| `dice` | `DiceRoller`, `DiceThrow`: unico punto di casualità, iniettabile (stubbabile nei test). |
| `engine` | Resolver puri (`HitResolver`, `DefenseResolver`, `DamageCalculator`, `InitiativeResolver`, `PowerStrikeResolver`), regole (`MomentumRules`, `StaminaRules`), `TurnOrchestrator` che gioca un singolo scambio, `TurnChronicler`, `FavoriteEstimator`. `CombatEngine` è un adapter sottile del duello 1v1 sopra `BattleEngine`. |
| `battle` | `BattleEngine`: orchestra la battaglia NvN round per round. Policy sostituibili: `EngagementPlanner`, `TargetSelector`, `FreeWinnerAssigner`. `BattleRoster`, `Engagement`, `RoundLogEntry`, `DuelResultAdapter`. |
| `result` | Viste read-only dell'esito (`CombatResult`, `TurnResult`, `TurnLogEntry`, `Scorecard`, ...): il motore calcola tutto e restituisce il log completo. |
| `io` | Presentazione: `CombatLogger`/`ConsoleCombatLogger`, `ConsoleBattleLogger`, `CombatReplay` con modalità `LINEAR`/`SCREEN`, `BattleSceneRenderer` (scena ASCII NvN), `CombatScreenRenderer`, `FighterCardFormatter`, `TurnPacer`, `ScreenRefresh`, `FactionSizePrompt`. |
| `action`, `context` | Punti di estensione: `CombatAction` (in v1 solo `AttackAction`), `CombatContext`/`ContextModifier` (in v1 sempre vuoto e neutro). |
| `Arena` | Facade del sottosistema: assembla i collaboratori, dispone lo scontro, stampa. Nessuna formula. |
| `factory` | `FighterFactory`: unico punto di contatto con i generatori del toolkit. |

Vincoli architetturali da rispettare quando modifichi:

- **Le formule vivono solo in `CombatFormulas`.** I resolver sono shell sottili: estraggono le stat, chiamano la formula, costruiscono l'esito di dominio. `ResolverPurityTest` presidia questo confine.
- **I pesi vivono solo in `CombatSettings`.** Niente costanti numeriche di bilanciamento sparse nei resolver.
- **Il motore non stampa.** Calcola l'intero log e lo restituisce; `io` lo rivela all'utente. I renderer producono righe di testo pure, senza I/O.
- **La casualità passa da `DiceRoller`**, mai da `Math.random()` o `new Random()` nel motore.
- Il duello 1v1 è il caso degenere della battaglia NvN, non un percorso parallelo: non duplicare logica di round in `CombatEngine`.

## Test

Suite JUnit Jupiter 5.x sotto `src/test/java`, con surefire configurato in `pom.xml`. Copre
soprattutto `combat.engine` e `combat.battle`, più i formatter/renderer di `combat.io`.

- Assertion solo con `org.junit.jupiter.api.Assertions` (**niente AssertJ**). Non aggiungere dipendenze di test se non strettamente necessario.
- Il supporto ai test vive in `it.fantasyarena.combat.testsupport`: `CombatFixtures` per costruire `Fighter` deterministici, `StubDiceRoller` e `RecordingStubDiceRoller` per pilotare i dadi.
- I test del motore sono deterministici: si pilotano i dadi, non si ripetono le esecuzioni sperando in una statistica.

## Documentazione nel repo

- `combatSettings.md` — guida parametro per parametro al bilanciamento di `CombatSettings`: cosa fa ogni peso e dove incide nel calcolo. Aggiornalo quando aggiungi o cambi il significato di un peso.
- `spiegazione.md` — descrizione non tecnica di cosa fa l'Arena e come funziona.
- `daImplementare.md` — elenco delle funzionalità non ancora realizzate.

## Convenzioni di questo repo

- Java 21 (`maven.compiler.source/target=21`). In uso: record per i tipi valore e di configurazione, switch expression, `List.getFirst()`. Non in uso: sealed types, text block, pattern matching (né `instanceof` né `switch`). Non introdurre preview feature.
- Indentazione a **2 spazi**. `FighterFactory` è l'unica eccezione storica a 4 spazi: se la modifichi, mantieni lo stile del file.
- Commenti e Javadoc in **italiano**. Il Javadoc di classe è denso e spiega le decisioni di design e i confini di responsabilità, non ripete la firma: mantieni questo registro quando aggiungi classi al motore.
- Nomi di classi, metodi e variabili in inglese.
- `Main` resta thin: chiede gli input, genera i combattenti, delega ad `Arena`.
