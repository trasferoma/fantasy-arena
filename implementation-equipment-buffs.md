# IMPLEMENTATION — Buff dell'equipaggiamento e caratteristiche efficaci

**Specifica di riferimento:** `spec-equipment-buffs.md`  — nel resto del documento: «la SPEC».
**Stato:** `NOT_STARTED`  <!-- NOT_STARTED | IN_PROGRESS | BLOCKED | COMPLETED -->

Documento di lavoro: la SPEC (il "cosa") resta stabile; qui vivono stato, piano, decisioni e problemi (il "come").

## Regole per l'agente

- Leggere il `CLAUDE.md` di **entrambi** i repository (`C:/build/git/fantasy-combat-system` e `C:/build/git/fantasy-arena`), più `combatSettings.md` del motore, e la SPEC prima di toccare codice.
- Alla ripresa del lavoro, leggere prima questo file e riprendere dallo stato corrente.
- Prima di modificare, elencare i file che verranno toccati. Nessun refactoring fuori scope.
- Non modificare i requisiti della SPEC senza decisione esplicita.
- Dopo ogni fase: eseguire i test pertinenti e aggiornare questo file. Spuntare una voce solo dopo verifica reale, mai a priori. **Anche le fasi senza codice** (analisi, revisione, documentazione) si spuntano prima di passare alla successiva.
- Scelta che **non** cambia il comportamento osservabile → procedi e annotala in *Decisioni*.
- Scelta che **cambia** comportamento o criteri di accettazione, o ambiguità non risolvibile dalla SPEC → **fermati**, imposta lo stato a `BLOCKED` e registra in *Problemi aperti* / *Deviazioni*.
- Due punti di questa modifica sono facili da tradire senza accorgersene, e vanno presidiati a ogni fase: **(a)** le caratteristiche efficaci si risolvono **alla lettura** e nessuno le passa a nessuno come parametro — se ti trovi a scrivere una firma che le trasporta, sei fuori dalla SPEC; **(b)** il gioiello contribuisce **solo** coi suoi buff — se sopravvive da qualche parte un punto caratteristica assegnato per rarità, il doppio canale è stato ridotto e non rimosso.
- Vincoli di sequenza non negoziabili: **il motore va prima**; nel motore si lavora su **branch dedicato**, si fa merge su `master` solo a test verdi e il branch **non si cancella**; `mvn install` del motore (jar **e** test-jar) è la condizione perché il gioco compili. Fino a quel momento `fantasy-arena` non compila: è atteso, non è un errore da diagnosticare.
- Non toccare `C:/build/git/fantasy-game-toolkit`: il toolkit resta com'è, buff e debuff arrivano da lì così come sono.

## Piano operativo

**Fase 1 — Analisi (nessun codice)**
- [ ] Rileggere i punti del codice elencati nel *Contesto* della SPEC e confermare che ciò che dovrebbe esistere esiste davvero (i 15 call site di `Characteristics.valueOf`, le tre `assemble(...)`, i due costruttori di `Fighter`, i `.noStatusEffect()` in `FighterFactory`, `Hero.jewels()`).
- [ ] Confermare sulla doc del toolkit il package di `BuffElement`/`DebuffElement`/`StatusEffect` e la firma del `DiceLauncherTool`.
- [ ] Confermare l'inventario del doppio canale del gioiello da rimuovere: `HeroBrain.JEWEL_BONUS_POINTS`, `jewelBonusPointsOf`, `JewelDecision.points`/`bonusPoints()`, `pointsToDistribute` come somma, `HeroProgress.NewJewel`/`JewelUpgrade.points`, le due righe di `HeroProgressFormatter`.
- [ ] Verificare quali test dei due repository costruiscono `Fighter` e con quale via (atteso: solo `CombatFixtures` e `FighterAssembler`; il gioco solo tramite le factory del `test-jar`).
- [ ] Rilevare lo stile dei test esistenti nei due repository (JUnit Jupiter, `Assertions`, dadi pilotati nel motore, verifiche statistiche su molte estrazioni nel gioco).
- [ ] Compilare/confermare "File coinvolti (effettivi)".

**Fase 2 — Motore: branch e lettura delle efficaci**
- [ ] Creare il branch dedicato in `C:/build/git/fantasy-combat-system` (es. `feature/equipment-buffs`).
- [ ] Introdurre `model/EffectiveCharacteristics` come **vista**: custodisce personaggio e riferimenti all'equipaggiamento (arma, pezzi, scudo, gioielli), somma buff e sottrae debuff dentro `valueOf(Characteristic)`, applica lì il pavimento `MIN_EFFECTIVE_VALUE = 1`. Nessuna mappa di totali, nessuna memoizzazione.
- [ ] Javadoc di classe che spiega perché la risoluzione avviene alla lettura (lo stato incoerente non è rappresentabile; un pezzo rimosso perde il suo effetto per costruzione) e perché la somma sta nel `model` e non in `CombatFormulas`.
- [ ] Riscrivere il Javadoc di `model/Characteristics` come accesso al valore **naturale**, usato dalla vista.
- [ ] Criterio di completamento: il motore compila; nessun comportamento ancora cambiato.

**Fase 3 — Motore: gioielli e nuove firme**
- [ ] `Fighter`: campo `jewels` con accessor, e la propria `EffectiveCharacteristics` **costruita nel costruttore** dall'equipaggiamento appena ricevuto (non ricevuta da fuori), con accessor `effectiveCharacteristics()`.
- [ ] `Fighter`: costruttore canonico che guadagna i gioielli; costruttore di comodo a pezzo singolo **conservato** con la firma di oggi, delegando con `List.of()` di gioielli, e Javadoc che lo dichiara «un pezzo solo, nessun gioiello».
- [ ] `Fighter`: aggiornare il Javadoc di classe sul punto «aggregato di dati, nessuna formula» — la somma dei buff è estrazione del dato, i pesi restano in `CombatSettings`.
- [ ] `RatingStrategy` e `DefaultRatingStrategy`: firma che guadagna **solo i gioielli**; la strategia si costruisce la vista da sé e legge tutte le statistiche da lì.
- [ ] `FighterAssembler`: nuovo overload coi gioielli che li **inoltra** senza risolvere niente, gli altri tre delegano con lista vuota; validazione dei gioielli coerente con quella esistente (lista non nulla, nessun elemento nullo).
- [ ] Criterio di completamento: il motore compila; i breaking change sono confinati alla firma di `RatingStrategy.computeRatings` e al costruttore canonico di `Fighter`.

**Fase 4 — Motore: resolver sulle efficaci**
- [ ] Sostituire le letture in `InitiativeResolver`, `HitResolver`, `DefenseResolver`, `PowerStrikeResolver` con `fighter.effectiveCharacteristics().valueOf(...)`.
- [ ] Verificare che nessun resolver chiami più `Characteristics.valueOf`.
- [ ] Criterio di completamento: `mvn compile` verde e nessun call site residuo ai valori naturali nei resolver.

**Fase 5 — Motore: test**
- [ ] Aggiungere a `CombatFixtures` gli overload per equipaggiamento buffato e gioielli, **senza toccare le firme esistenti** (il gioco le consuma dal `test-jar`; grazie al costruttore di comodo conservato, `createFighter` non ha bisogno di cambiare). I buff si costruiscono col costruttore dei record del toolkit, non col generatore.
- [ ] Test per i criteri 1–5 della *Definition of done* della SPEC, compreso il criterio 2 (lo stesso personaggio senza il gioiello legge il valore di prima).
- [ ] Verificare verdi i test d'equivalenza e di purezza (`BattleEngineDuelEquivalenceTest`, `CombatSystemTest`, `ResolverPurityTest`) — criterio 6.
- [ ] Criterio di completamento: `mvn test` verde nel motore.

**Fase 6 — Motore: merge e pubblicazione**
- [ ] Merge del branch su `master` **solo a test verdi**, senza cancellare il branch.
- [ ] `mvn install` nel motore: jar **e** `test-jar` nel repository locale.
- [ ] Criterio di completamento: `mvn compile` in `fantasy-arena` vede le nuove firme (e fallisce solo dove il gioco va ancora adeguato).

**Fase 7 — Gioco: gioielli nello scontro**
- [ ] `FighterFactory.summon` passa `hero.jewels()` al nuovo overload dell'assemblatore; Javadoc aggiornato (non è più vero che il motore non li sa montare).
- [ ] Criterio di completamento: criterio 10 verde.

**Fase 8 — Gioco: loot buffato al 20%**
- [ ] `HeroBrain`: costante e accessor della probabilità come **percentuale**, accanto alle altre tabelle di bilanciamento.
- [ ] `FighterFactory`: tiro col `DiceLauncherTool`, `rollLoot` con la percentuale come parametro, i tre generatori di loot che applicano `noStatusEffect()` solo quando l'oggetto è liscio. Indentazione a **4 spazi**, come il resto del file.
- [ ] `Arena`: passa la percentuale del cervello insieme alla soglia di rarità, senza decidere nulla.
- [ ] Verificare che equipaggiamento di partenza, sfidanti e specchio restino su `noStatusEffect()`.
- [ ] Criterio di completamento: criteri 7, 8, 9 verdi.

**Fase 9 — Gioco: cernita col buff e fine del doppio canale del gioiello**
- [ ] `HeroBrain`: peso del buff, calcolo del buff netto e i tre comparatori aggiornati (rarità come spareggio anche per il gioiello).
- [ ] `HeroBrain`: rimuovere `JEWEL_BONUS_POINTS` e `jewelBonusPointsOf`; `pointsToDistribute` torna a essere i soli `CHARACTERISTIC_POINTS_PER_VICTORY`; `JewelDecision` perde `points` e `bonusPoints()`.
- [ ] `HeroProgress`: `JewelUpgrade` perde `points`, `NewJewel` sparisce e `newJewel()` diventa `Optional<JewelResult>`; aggiornare il Javadoc dei sei destini del loot.
- [ ] `HeroBrain`: Javadoc di classe riscritto sul nuovo criterio (il buff, non la rarità) e sulla fine del bonus del gioiello.
- [ ] Criterio di completamento: criteri 11 e 12 verdi, e i casi di parità continuano a far tenere il proprio oggetto.

**Fase 10 — Gioco: presentazione**
- [ ] `FighterCardFormatter`: caratteristiche efficaci con delta, riga per gioiello, buff netto sulle righe di equipaggiamento; nessun ricalcolo delle efficaci nel renderer.
- [ ] `HeroProgressFormatter`: il buff dell'oggetto trovato nelle tre `describe(...)`; le due righe del gioiello non parlano più di punti caratteristica.
- [ ] Criterio di completamento: criteri 13, 14, 15 verdi.

**Fase 11 — Gioco: test e suite**
- [ ] Adeguare i test che asseriscono i punti bonus del gioiello — **adeguamento previsto, non sorpresa**: `HeroBrainTest` righe 126, 131-132, 142, 152 e intestazione di classe; `HeroProgressFormatterTest` righe 110-116 e 126-131, più i due `import` di `NewJewel`/`JewelUpgrade`.
- [ ] Un test per ciascuno dei criteri 7–15 della *Definition of done*.
- [ ] `mvn test` verde in `fantasy-arena`, compresi i test preesistenti dei renderer e dei logger.

**Fase 12 — Revisione funzionale (`java-functional-evolver`)**
- [ ] Invocare la revisione sul codice Java prodotto nel **motore** e annotarne l'esito nel *Registro*.
- [ ] Invocare la revisione sul codice Java prodotto nel **gioco** e annotarne l'esito nel *Registro*.
- [ ] Ri-eseguire i test dei repository toccati dopo eventuali modifiche.

**Fase 13 — Documentazione (nessun codice di produzione)**
- [ ] `CLAUDE.md` del motore: le formule leggono le caratteristiche **efficaci**, risolte alla lettura; `Fighter` custodisce i gioielli e si costruisce la vista; `EffectiveCharacteristics` nella tabella dei package; i breaking change dichiarati.
- [ ] `CLAUDE.md` del gioco: rimuovere «il motore non sa montarlo» sui gioielli; rimuovere le due affermazioni ora false — che il gioiello «vale anche punti caratteristica extra» e che per il gioiello il criterio è la rarità «perché è l'unico numero che un gioiello ha»; aggiungere il loot buffato al 20% (percentuale in `HeroBrain`, dado in `FighterFactory`), i comparatori che pesano il buff, la scheda che mostra gioielli ed efficaci.
- [ ] `combatSettings.md` del motore: nelle formule di §1 e §6 le statistiche sono quelle efficaci.
- [ ] `README.md` del motore: la parte che descrive da cosa nascono Rating, vita e stamina.
- [ ] `daImplementare.md` del gioco: la voce sui gioielli «fuori dal combattimento» non è più vera.
- [ ] Criterio di completamento: criterio 16 verificato in revisione.

**Fase 14 — Revisione finale**
- [ ] Coerenza con la SPEC; nessuna modifica non richiesta ai due repository; nessuna firma che trasporta le efficaci; nessun punto caratteristica assegnato per rarità del gioiello.
- [ ] `mvn test` verde in entrambi i repository, `mvn install` del motore rifatto se il codice del motore è cambiato in Fase 12.
- [ ] Aggiornare *Decisioni/Deviazioni* ed *Esito finale*; portare lo stato a `COMPLETED`.

## File coinvolti (effettivi)

Pre-compilati in via **provvisoria** dall'analisi della SPEC: da confermare e completare in Fase 1.

**Motore — `C:/build/git/fantasy-combat-system`**
- `src/main/java/it/fantasycombatsystem/model/EffectiveCharacteristics.java` — nuovo: la vista che somma alla lettura
- `src/main/java/it/fantasycombatsystem/model/Characteristics.java` — Javadoc: accesso al valore naturale
- `src/main/java/it/fantasycombatsystem/model/Fighter.java` — gioielli custoditi, vista costruita nel costruttore, costruttore di comodo conservato
- `src/main/java/it/fantasycombatsystem/rating/RatingStrategy.java` — firma coi gioielli (breaking)
- `src/main/java/it/fantasycombatsystem/rating/DefaultRatingStrategy.java` — Rating, vita e stamina sulle efficaci
- `src/main/java/it/fantasycombatsystem/factory/FighterAssembler.java` — overload coi gioielli, che li inoltra
- `src/main/java/it/fantasycombatsystem/engine/InitiativeResolver.java` — AGILITY, INTELLIGENCE efficaci
- `src/main/java/it/fantasycombatsystem/engine/HitResolver.java` — AGILITY, LUCK efficaci
- `src/main/java/it/fantasycombatsystem/engine/DefenseResolver.java` — AGILITY efficaci
- `src/main/java/it/fantasycombatsystem/engine/PowerStrikeResolver.java` — INTELLIGENCE efficace
- `src/test/java/it/fantasycombatsystem/testsupport/CombatFixtures.java` — overload buffati e coi gioielli, firme esistenti intatte
- `src/test/java/it/fantasycombatsystem/model/EffectiveCharacteristicsTest.java` — nuovo
- `src/test/java/it/fantasycombatsystem/factory/FighterAssemblerTest.java` — gioielli, overload preesistenti, costruttore di comodo
- `src/test/java/it/fantasycombatsystem/rating/DefaultRatingStrategyTest.java` — Rating sulle efficaci
- test dei resolver (`HitResolver*`, `DefenseResolver*`, `InitiativeResolver*`, `PowerStrikeResolverTest`) — casi con buff
- `CLAUDE.md`, `combatSettings.md`, `README.md` — Fase 13

**Gioco — `C:/build/git/fantasy-arena`**
- `src/main/java/it/fantasyarena/combat/factory/FighterFactory.java` — `summon` coi gioielli, tiro del buff, loot buffabile (4 spazi)
- `src/main/java/it/fantasyarena/combat/hero/HeroBrain.java` — percentuale del buff, peso del buff, tre comparatori, rimozione del bonus del gioiello
- `src/main/java/it/fantasyarena/combat/hero/HeroProgress.java` — i record del gioiello perdono i punti, `NewJewel` eliminato
- `src/main/java/it/fantasyarena/combat/Arena.java` — passa la percentuale alla factory
- `src/main/java/it/fantasyarena/combat/io/render/FighterCardFormatter.java` — efficaci, delta, gioielli, buff netto
- `src/main/java/it/fantasyarena/combat/io/render/HeroProgressFormatter.java` — il buff dell'oggetto trovato, niente punti del gioiello
- `src/test/java/it/fantasyarena/combat/factory/FighterFactoryTest.java` — bordi 0%/100%, equipaggiamento liscio
- `src/test/java/it/fantasyarena/combat/hero/HeroBrainTest.java` — cernita col buff, percentuale di default, **adeguamento** delle asserzioni sui punti del gioiello
- `src/test/java/it/fantasyarena/combat/io/render/FighterCardFormatterTest.java` — nuovo: scheda con e senza buff
- `src/test/java/it/fantasyarena/combat/io/render/HeroProgressFormatterTest.java` — racconto del buff, **adeguamento** delle righe sui punti del gioiello
- `CLAUDE.md`, `daImplementare.md` — Fase 13
- Da verificare in Fase 1 (possibile impatto, non modifica prevista): `ArenaTest`, `ArenaFighterFactoryTest`, `HeroTest`, `CombatScreenRendererTest`, `BattleSceneRendererTest`, `ConsoleBattleLoggerTest`, `ConsoleCombatLoggerOutcomeTest`

## Registro

Voci datate (`YYYY-MM-DD`), append-only.

- **Decisioni tecniche** (non cambiano il comportamento) — `Decisione · Motivazione · Impatto`: nessuna.
- **Deviazioni dalla SPEC** (da motivare) — `Descrizione · Motivazione · Impatto · Aggiorna la SPEC? sì/no`: nessuna.
- **Problemi aperti** (bloccano l'avanzamento) — `Descrizione · Impatto · Opzioni · Decisione richiesta`: nessuno.
- **Test eseguiti** — `data · fase · comando · esito`: nessuno.
- **Revisione `java-functional-evolver`** — `data · repository · invocato sì/no · cosa ha cambiato`: da compilare in Fase 12.
- **Git sul repository-dipendenza** — `data · branch creato · merge su master · mvn install`: da compilare in Fase 2, 6 e 14.

## Esito finale

Da compilare a fine lavoro: stato finale, modifiche effettuate nei due repository, test eseguiti, note residue.

## Esempio (concreto: i file previsti e i test previsti)

```java
// File previsti — MOTORE:
//   EffectiveCharacteristics   — nuovo: vista che somma buff e sottrae debuff ALLA LETTURA, pavimento 1
//   Characteristics            — declassato a lettura del valore naturale
//   Fighter                    — custodisce i gioielli e si costruisce da sé la vista
//   RatingStrategy/Default...  — firma coi soli gioielli (breaking); legge le efficaci
//   FighterAssembler           — overload coi gioielli: li inoltra, non risolve niente
//   Hit/Defense/Initiative/PowerStrike Resolver — leggono le efficaci
//   CombatFixtures             — overload buffati e coi gioielli, firme esistenti intatte
//
// File previsti — GIOCO:
//   FighterFactory             — summon coi gioielli, tiro d100 del buff, loot buffabile
//   HeroBrain                  — percentuale del buff, peso del buff, comparatori, via il bonus del gioiello
//   HeroProgress               — i record del gioiello senza punti, NewJewel eliminato
//   Arena                      — passa la percentuale, non decide
//   FighterCardFormatter       — efficaci con delta, riga gioiello, buff netto
//   HeroProgressFormatter      — racconta il buff, non i punti del gioiello

// Test: uno per criterio della DoD (il criterio 16, la documentazione, si verifica in revisione)
// MOTORE
@Test void efficaci_sommanoIBuffDiTuttiIPezziIndossati()        { /* criterio 1 */ }
@Test void efficaci_sottraggonoIDebuffENonScendonoSottoUno()    { /* criterio 1 */ }
@Test void senzaIlGioiello_ilValoreEfficaceTornaQuelloDiPrima() { /* criterio 2 */ }
@Test void ratingEVitaMassima_calcolatiSulleEfficaci()          { /* criterio 3 */ }
@Test void colpireSchivareIniziativaColpoPotente_sulleEfficaci(){ /* criterio 4, dadi pilotati */ }
@Test void assemblerConGioielli_iLoroBuffPesano()               { /* criterio 5 */ }
@Test void assemblerSenzaGioielliECostruttoreDiComodo_comeOggi(){ /* criterio 5 */ }
@Test void equipaggiamentoLiscio_comportamentoIdenticoAOggi()   { /* criterio 6, equivalenza */ }

// GIOCO
@Test void lootConProbabilitaZero_maiBuffato()                  { /* criterio 7 */ }
@Test void lootConProbabilitaCento_sempreAlmenoUnBuff()         { /* criterio 7, tutti e tre i tipi */ }
@Test void lootBuffato_rispettaComunqueLaSogliaDiRarita()       { /* criterio 7 */ }
@Test void probabilitaDiDefault_venti()                         { /* criterio 8 */ }
@Test void partenzaSfidantiESpecchio_senzaBuff()                { /* criterio 9 */ }
@Test void summon_portaInCampoIGioielliIndossati()              { /* criterio 10 */ }
@Test void impugnaLArmaCheColpisceMenoMaBuffaPiu()              { /* criterio 11 */ }
@Test void indossaIlGioielloMenoRaroMaPiuBuffato()              { /* criterio 11 */ }
@Test void aParitaDiValoreComplessivo_tieneIlSuo()              { /* criterio 11 */ }
@Test void laVittoriaValeSempreTrePunti_ancheConUnGioiello()    { /* criterio 12 */ }
@Test void scheda_mostraEfficaciConDeltaEGioielli()             { /* criterio 13 */ }
@Test void fineScontro_raccontaIlBuffENonIPuntiDelGioiello()    { /* criterio 14 */ }
@Test void senzaBuffNeGioielli_schedaERaccontoInvariati()       { /* criterio 15 */ }
```
