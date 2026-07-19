# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Cos'è questo progetto

`fantasy-arena` è un'applicazione console Java 21 (Maven) che fa da **consumatore dimostrativo** della libreria `fantasytoolkit`. Il codice applicativo qui è minimale: tutta la logica di dominio (generazione di personaggi, dadi, armi, armature, dungeon, pozioni, gioielli, buff/debuff, nomi) vive nella libreria esterna. Il punto d'ingresso è `it.fantasyarena.Main`.

## Comandi

```bash
mvn compile              # compila
mvn exec:java            # esegue it.fantasyarena.Main (mainClass da pom.xml, exec.mainClass)
mvn package              # produce il jar in target/
```

Non esistono test in questo repo: nessun comando di test è configurato.

## Dipendenza critica: fantasytoolkit

- Unica dipendenza: `it.fantasytoolkit:fantasytoolkit:1.0-SNAPSHOT`.
- **Non è su Maven Central**: è risolta solo dal repository Maven locale (`~/.m2/repository/it/fantasytoolkit/...`). Deve essere buildata e installata (`mvn install`) dal suo progetto sorgente separato prima di poter compilare qui. Se `mvn compile` fallisce con artifact non risolto, il problema è quasi sempre questo, non il codice di `fantasy-arena`.
- Essendo `SNAPSHOT`, l'API della libreria può cambiare: verifica sempre le firme reali nel jar installato invece di assumerle.

## Architettura e pattern d'uso

Ogni funzionalità della libreria è esposta come un **Tool** con un fluent builder che parte da un metodo statico `building()`, si configura con chiamate concatenate e termina con un metodo terminale che restituisce un *result*:

```java
CharacterResult hero = CharacterGeneratorTool.building()
        .randomRace().randomClass().addNickname()
        .allCharacteristics().totalPoints(50)
        .generate();                       // terminale

DiceRollResult roll = DiceLauncherTool.building()
        .dice(2, 6).dice(1, 20, "initiative")
        .roll();                           // terminale
```

I tipi `*Result` (in package `.result`) sono record-like: si leggono con accessor senza prefisso `get` (`character.name()`, `character.race()`, `roll.total()`). Quando aggiungi consumo di nuovi tool, segui questo stesso schema `building() → config → terminale → lettura del result`.

Tool disponibili nella libreria (package `it.fantasytoolkit.*`): `charactergenerator`, `dicelauncher`, `namegenerator`, `weapongenerator`, `armourgenerator`, `jewelgenerator`, `potiongenerator`, `buffdebuffgenerator`, `dungeongenerator`. I modelli e le enum condivise stanno in `it.fantasytoolkitcore.core.model` (`Race`, `CharacterClass`, `Rarity`, `Weapon`, ...).

Per utilizzare `fantasy-game-toolkit`, consulta la documentazione indicizzata
da `docs/agent/INDEX.md` nel repository della versione utilizzata.

Non leggere i sorgenti e non decompilare il JAR. Se l'API necessaria non è
documentata, segnala la lacuna.

## Convenzioni di questo repo

- Java 21 (`maven.compiler.source/target=21`), ma il codice applicativo attuale resta volutamente semplice (loop tradizionali, niente feature esotiche). Mantieni `Main` e le classi consumer *thin*: orchestrano chiamate ai tool e stampano, non contengono logica di dominio.
- Commenti e Javadoc del progetto sono in italiano.
