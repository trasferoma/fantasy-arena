# IMPLEMENTATION — Cronaca del duello

**Specifica di riferimento:** `spec-cronaca-duello.md`  — nel resto del documento: «la SPEC».
**Stato:** `COMPLETED`  <!-- NOT_STARTED | IN_PROGRESS | BLOCKED | COMPLETED -->

Documento di lavoro: la SPEC (il "cosa") resta stabile; qui vivono stato, piano, decisioni e problemi (il "come").

## Regole per l'agente
- Leggere `CLAUDE.md` (se presente) e la SPEC prima di toccare codice.
- Alla ripresa del lavoro, leggere prima questo file e riprendere dallo stato corrente.
- Prima di modificare, elencare i file che verranno toccati. Nessun refactoring fuori scope.
- Non modificare i requisiti della SPEC senza decisione esplicita.
- Dopo ogni fase: eseguire i test pertinenti e aggiornare questo file. Spuntare una voce solo dopo verifica reale, mai a priori.
- Scelta che **non** cambia il comportamento osservabile → procedi e annotala in *Decisioni*.
- Scelta che **cambia** comportamento o criteri di accettazione, o ambiguità non risolvibile dalla SPEC → **fermati**, imposta lo stato a `BLOCKED` e registra in *Problemi aperti* / *Deviazioni*.

## Vincoli di progetto (da rispettare in ogni fase)
- Java 21; codice applicativo/presentazione sobrio. Identificatori in inglese; commenti, Javadoc e stringhe di output console in italiano; indentazione a 2 spazi.
- Test JUnit Jupiter 5 con **solo** `org.junit.jupiter.api.Assertions` (niente AssertJ). **Nessuna nuova dipendenza** (runtime o test).
- ASCII puro per barre/cornici/colonne; nessun colore ANSI, nessun box-drawing Unicode.
- **Motore invariato**: nessuna modifica a formule, esito, terminazione, immutabilità dei Rating. Le uniche modifiche additive al dominio sono la descrizione arricchita e la lista di highlight del turno.
- I `Fighter` **non** entrano in `CombatResult`: passaggio esplicito per parametro a renderer/replay/reportOutcome.
- LINEAR resta funzionante; SCREEN resta default.

## Ordine di lavoro (riduzione del rischio)
Prima il **dominio** (highlight + descrizione + favorito, con i relativi test), poi la **presentazione** (card formatter, renderer a 3 colonne, riepilogo finale). Il dominio è additivo e a basso rischio; la presentazione dipende dal dominio ma non viceversa.

## Piano operativo

**Fase 1 — Analisi** ✅
- [x] Confermare i punti del codice della SPEC leggendo le firme reali (`TurnOrchestrator.resolveHitLanded`, `HitOutcome`, `TurnLogEntry` + wither, `CombatSettings` + sotto-record, `CombatScreenRenderer`, `ScreenCombatReplay`/`CombatReplay`, `ConsoleCombatLogger`, `Arena.run`).
- [x] Confermare che `damage`, `varianceThrow`, `attackThrow` e `defender.isDefeated()` sono disponibili nel punto in cui si raccolgono gli highlight.
- [x] Rilevare lo stile dei test esistenti (`CombatFixtures`, `StubDiceRoller`, assert su substring della descrizione, assert su `render(...)`).
- [x] Decidere come `PERFECT_HIT` si distingue dal critico ordinario: CONFERMATO threading di `attackThrow` in `resolveHitLanded` (nessun campo su `HitOutcome`).
- [x] Decidere la collocazione della soglia colpo pesante: CONFERMATO nuovo sotto-record `ChronicleWeights(heavyBlowHealthRatio=0.25)`.
- [x] Confermare/aggiornare "File coinvolti (effettivi)" e l'elenco dei test esistenti da aggiornare.
- Nota: `TurnHighlight` collocato in `result` (non `engine`) per evitare il ciclo di package `result`↔`engine` (decisione tecnica, vedi Registro).

**Fase 2 — Dominio: highlight tipizzati + soglia colpo pesante** ✅
- [x] `TurnHighlight` (enum `CRITICAL`, `PERFECT_HIT`, `KNOCKOUT`, `HEAVY_BLOW`) — in package `result`.
- [x] `CombatSettings`: nuovo sotto-record `ChronicleWeights` con `heavyBlowHealthRatio` (default 0.25) e `defaults()`, agganciato al record top-level e a `CombatSettings.defaults()`.
- [x] `TurnLogEntry`: aggiungere `List<TurnHighlight> highlights` (copia difensiva nel costruttore canonico, default lista vuota nel costruttore breve) + wither `withHighlights(...)`; i wither esistenti devono preservare gli highlight.
- [x] `TurnOrchestrator.resolveHitLanded(...)`: raccogliere gli highlight applicabili (fonte unica di verità) e propagarli sulla `TurnLogEntry`; `attackThrow` propagato fin qui.
- [x] Verificare che `CombatEngine.fight(...)` conservi gli highlight quando concatena `withVitals/withInitiative/withStaminaChanges`.

**Fase 3 — Dominio: cronista (wording) + stima del favorito (puri)** ✅
- [x] `TurnChronicler` (puro, package `engine`): `describeOutcome(...)` produce la coda descrittiva; senza highlight riproduce il testo attuale, con highlight applica la precedenza (20 naturale assorbe il critico ordinario; KO chiusa dominante; colpo pesante rafforzativo).
- [x] `TurnOrchestrator`: usa `TurnChronicler` per la coda del colpo a segno (miss/rest invariati).
- [x] `FavoriteEstimator` (puro, package `engine`): `favorite(Fighter, Fighter)` → `Optional<Fighter>` (empty = equilibrato), criterio `off+def` → `maxHealth` → `maxStamina` → nessun favorito (via `Comparator` a cascata dopo revisione funzionale).

**Fase 3.5 — Dominio: iniziativa senza test sotto override (Parte 4)** ✅
- [x] `InitiativeResolver`: nuovo `overriddenNextAttacker(currentAttacker, currentDefender, override)` con `chosen = currentDefender` e `InitiativeReport` con `breakdowns` vuoti; `resolveNextAttacker` rifattorizzato al solo test a punteggio (rimosso il parametro `override`); `resolveFirstMover` invariato.
- [x] `CombatEngine.resolveNextAttacker(...)`: sotto `override != NONE` NON lancia i jitter e chiama il ramo override-only; altrimenti comportamento invariato. Attaccante scelto identico.
- [x] `InitiativeReport`: Javadoc chiarito sul caso `breakdowns` vuoti; `scoreWinnerName = chosenName` come segnaposto non mostrato (nessun `null`).
- [x] `TurnLogFormatter.format`/`formatCompact`: sotto override (breakdowns vuoti) saltano la riga "vince l'iniziativa (punteggio)" e i breakdown, mostrando solo "primo ad agire: Y (motivo override)". (Nota: il rendering iniziativa vive in `TurnLogFormatter`, non in `ConsoleCombatLogger`.)

**Fase 4 — Test del dominio** ✅
- [x] `TurnOrchestratorHighlightsTest`: critico (CRITICAL, non PERFECT), 20 naturale (PERFECT_HIT + CRITICAL compresenti), KO (KNOCKOUT), colpo pesante (HEAVY_BLOW) e variazione con soglia diversa — dadi pilotati con `StubDiceRoller` (DoD 1–4).
- [x] `TurnChroniclerTest`: wording e precedenza; testo conciso; coincidenza col testo base senza highlight (DoD 5).
- [x] `FavoriteEstimatorTest`: favorito netto, spareggio su maxHealth, spareggio su maxStamina, equilibrato (DoD 6).
- [x] `InitiativeResolverTest`: ramo override (breakdowns vuoti, chosen = difensore) e ramo `NONE` invariato (DoD 12); riallineati `CombatEngineInitiativeTest`, `TurnOrchestratorRestTest`, `TurnOrchestratorDefenseTest`, `TurnLogFormatterTest` per firma/sequenza dadi/wording arricchito.

**Fase 5 — Presentazione: FighterCardFormatter + matchup** ✅
- [x] `FighterCardFormatter` (puro, package `io`): scheda compatta a 6 righe (`[i] Nome`, razza/classe, arma+rarità+atk, armatura+rarità+def, VIT/STA, ATK/DEF), larghezza max 36, troncamento con "...".
- [x] `ConsoleCombatLogger.reportMatchup(...)`: riusa `FighterCardFormatter`.
- [x] `FighterCardFormatterTest` (DoD 7).

**Fase 6 — Presentazione: renderer SCREEN a 3 colonne** ✅
- [x] `CombatScreenRenderer`: riceve i due `Fighter`; colonna cumulativa sostituita da col.2 (SOLO eventi del turno corrente, `buildCurrentTurnColumn`) + col.3 (schede via `FighterCardFormatter`, `buildFighterCardsColumn`). Barre col.1, marcatore `*Nome*` e segni +/- invariati.
- [x] `CombatReplay.replay(...)` e `ScreenCombatReplay`/`LinearCombatReplay`: nuova firma `replay(CombatResult, Fighter, Fighter)`; `ScreenCombatReplay` li passa al renderer; `LinearCombatReplay` invariato (li ignora).
- [x] `Arena.run(...)`: passa i `Fighter` a `replay.replay(...)`.
- [x] `CombatScreenRendererTest`: aggiornato per il layout a 3 colonne — presenza schede (col.3), colonna centrale col solo turno corrente, marcatore/segni invariati (DoD 8, 10).

**Fase 7 — Presentazione: riepilogo finale con il "motivo"** ✅
- [x] `CombatLogger.reportOutcome(...)`: nuova firma `reportOutcome(CombatResult, Fighter, Fighter)`.
- [x] `ConsoleCombatLogger.reportOutcome(...)`: "nell'ultimo scontro:" + schede compatte + narrazione (favorito via `FavoriteEstimator`, vincitore, ribaltone/pronostico rispettato/equilibrio, citazione di un highlight con numero di turno e nome, precedenza KO>perfetto>critico>pesante); gestisce `DRAW` e `TIMEOUT_DECISION`. Duplicazione stampa schede estratta in `printCards(...)` (revisione funzionale).
- [x] `Arena.run(...)`: passa i `Fighter` a `logger.reportOutcome(...)`.
- [x] `ConsoleCombatLoggerOutcomeTest`: ribaltone, pronostico rispettato, equilibrio, citazione evento con turno, `DRAW`, `TIMEOUT_DECISION` (DoD 9).

**Fase 8 — Aggiornamento test esistenti impattati e regressione** ✅
- [x] Aggiornati gli assert su descrizione dove la cronaca cambia il testo (nel batch dominio: `TurnOrchestratorDefenseTest`; i colpi normali invariati).
- [x] `TurnLogFormatterTest` e `CombatScreenRendererTest` aggiornati (override + cumulativo→turno corrente + nuove firme).
- [x] `mvn -o test`: intera suite verde (96/96).

**Fase 9 — Revisione** ✅
- [x] Coerenza con la SPEC; nessuna modifica non richiesta; motore invariato; `Fighter` non in `CombatResult`.
- [x] Rischio larghezza affrontato (schede compatte ~36 col, layout ~120); LINEAR funzionante; SCREEN default.
- [x] Revisione funzionale Java (`java-functional-evolver`) su dominio e presentazione, test ri-verificati verdi.
- [x] Aggiornati *Decisioni/Deviazioni*, *Registro* ed *Esito finale*; stato `COMPLETED`.

## Decisioni proposte (da confermare in Fase 1, non bloccanti)
- **PERFECT_HIT vs CRITICAL**: propendere per **propagare `attackThrow`** (o un flag booleano) a `resolveHitLanded`, senza toccare il record `HitOutcome`. Motivo: `new HitOutcome(true, false)` è usato in `DamageCalculatorContextTest`; estendere il record impatterebbe quel test. Alternativa: aggiungere `boolean naturalMaximum` a `HitOutcome` (più "autorevole" ma con blast radius maggiore).
- **Soglia colpo pesante**: nuovo sotto-record `ChronicleWeights(double heavyBlowHealthRatio)` in `CombatSettings`, coerente con lo schema `*Weights` + `defaults()`, invece di appesantire `ChanceWeights` (concetto di cronaca, non di probabilità del colpo).
- **Cronista come componente puro**: `TurnChronicler` separato invece di if annidati in `TurnOrchestrator`, per contenere la complessità di lettura e testarlo in isolamento.

## File coinvolti (effettivi)
Elenco provvisorio dall'analisi — da confermare/aggiornare in Fase 1 (package base `it.fantasyarena.combat`).

Da creare:
- `src/main/java/it/fantasyarena/combat/engine/TurnHighlight.java` — enum degli eventi notevoli del turno
- `src/main/java/it/fantasyarena/combat/engine/TurnChronicler.java` — wording da cronaca (puro)
- `src/main/java/it/fantasyarena/combat/engine/FavoriteEstimator.java` — stima del favorito dai rating (puro)
- `src/main/java/it/fantasyarena/combat/io/FighterCardFormatter.java` — scheda compatta multi-riga (puro)
- `src/test/java/it/fantasyarena/combat/engine/TurnOrchestratorHighlightsTest.java` — DoD 1–4
- `src/test/java/it/fantasyarena/combat/engine/TurnChroniclerTest.java` — DoD 5
- `src/test/java/it/fantasyarena/combat/engine/FavoriteEstimatorTest.java` — DoD 6
- `src/test/java/it/fantasyarena/combat/io/FighterCardFormatterTest.java` — DoD 7
- `src/test/java/it/fantasyarena/combat/io/ConsoleCombatLoggerOutcomeTest.java` — narrazione finale, DoD 9

Da modificare:
- `src/main/java/it/fantasyarena/combat/result/TurnLogEntry.java` — campo `highlights` + `withHighlights`, wither che lo preservano
- `src/main/java/it/fantasyarena/combat/engine/TurnOrchestrator.java` — raccolta highlight + descrizione arricchita (Parti 2/3)
- `src/main/java/it/fantasyarena/combat/config/CombatSettings.java` — sotto-record `ChronicleWeights` (soglia colpo pesante)
- `src/main/java/it/fantasyarena/combat/io/ConsoleCombatLogger.java` — matchup e reportOutcome via `FighterCardFormatter`; narrazione con favorito/eventi; nuova firma reportOutcome
- `src/main/java/it/fantasyarena/combat/io/CombatLogger.java` — firma `reportOutcome(CombatResult, Fighter, Fighter)`
- `src/main/java/it/fantasyarena/combat/io/CombatReplay.java` — firma `replay(CombatResult, Fighter, Fighter)`
- `src/main/java/it/fantasyarena/combat/io/ScreenCombatReplay.java` — riceve e passa i `Fighter` al renderer
- `src/main/java/it/fantasyarena/combat/io/LinearCombatReplay.java` — adegua la firma (comportamento invariato)
- `src/main/java/it/fantasyarena/combat/io/CombatScreenRenderer.java` — layout a 3 colonne (barre / turno corrente / schede)
- `src/main/java/it/fantasyarena/combat/Arena.java` — passa i `Fighter` a replay e reportOutcome
- `src/main/java/it/fantasyarena/combat/engine/InitiativeResolver.java` — ramo override senza test (Parte 4): niente breakdown/scoreWinner, report con breakdowns vuoti
- `src/main/java/it/fantasyarena/combat/engine/CombatEngine.java` — sotto override niente lancio dei jitter; chiama il ramo override-only (Parte 4)
- `src/main/java/it/fantasyarena/combat/result/InitiativeReport.java` — Javadoc/chiarimento sul caso breakdowns vuoti sotto override
- `src/main/java/it/fantasyarena/combat/io/TurnLogFormatter.java` — sotto override salta la riga del punteggio e i breakdown
- (eventuale) `src/main/java/it/fantasyarena/combat/engine/HitOutcome.java` + `HitResolver.java` — solo se si sceglie l'alternativa "campo su HitOutcome"
- test di iniziativa/engine esistenti dipendenti dalla sequenza dei dadi (`InitiativeResolver*Test`, `CombatEngineTest`, ecc.) — riallineare per il mancato consumo dei jitter sotto override (Parte 4)
- `src/test/java/it/fantasyarena/combat/io/CombatScreenRendererTest.java` — layout a 3 colonne / turno corrente
- `src/test/java/it/fantasyarena/combat/engine/TurnOrchestratorDefenseTest.java` — assert sul wording del colpo a segno (da confermare)
- Altri test con assert su descrizione (`CombatEngineTest`, `TurnOrchestratorRestTest`, `TurnLogFormatterTest`) — da confermare in Fase 1

Non previsto in modifica: `Main.java` (il wiring resta lo stesso), `pom.xml` (nessuna nuova dipendenza).

## Registro
Voci datate (`YYYY-MM-DD`), append-only.

- **Decisioni tecniche** (non cambiano il comportamento) — `Decisione · Motivazione · Impatto`.
  - 2026-07-20 · `TurnHighlight` collocato in `result` invece di `engine` · evita il ciclo di package `result`↔`engine` (`TurnLogEntry` è in `result`) · nessun impatto sul comportamento.
  - 2026-07-20 · `attackThrow` propagato a `resolveHitLanded` invece di estendere `HitOutcome` · minor blast radius (non tocca `DamageCalculatorContextTest`) · nessun impatto.
  - 2026-07-20 · soglia colpo pesante in nuovo `ChronicleWeights(0.25)` · concetto di cronaca, non di probabilità · additivo su `CombatSettings`.
  - 2026-07-20 · `FavoriteEstimator` con `Comparator` a cascata (post revisione funzionale) · rimuove duplicazione dei tre confronti · behavior-preserving.
- **Deviazioni dalla SPEC** (da motivare) — `Descrizione · Motivazione · Impatto · Aggiorna la SPEC? sì/no`.
  - 2026-07-20 · il rendering dell'iniziativa vive in `TurnLogFormatter`, non in `ConsoleCombatLogger.printInitiative` (inesistente) · adeguata la guardia override nel punto reale · nessuno · Aggiorna la SPEC? no.
- **Problemi aperti** (bloccano l'avanzamento) — `Descrizione · Impatto · Opzioni · Decisione richiesta`. Nessuno.
- **Test eseguiti** — `data · fase · comando · esito`.
  - 2026-07-20 · Fasi 2-4 (dominio) · `mvn -o test` · 83/83 verdi.
  - 2026-07-20 · Fasi 5-8 (presentazione) · `mvn -o clean compile test` · 96/96 verdi.
- **Revisione funzionale** (`java-functional-evolver`) — `invocato/non invocato · cosa ha cambiato`.
  - 2026-07-20 · Fasi 2-4 (dominio) · invocato · `FavoriteEstimator` → `Comparator` a cascata + `totalRating` helper; Javadoc su parametro inutilizzato di `overriddenNextAttacker`; resto lasciato imperativo (accumulo highlight eterogeneo, precedenza cronista, loop `CombatEngine.fight`). `mvn -o test` 83/83 verdi post-modifica.
  - 2026-07-20 · Fasi 5-7 (presentazione) · invocato · estratta `ConsoleCombatLogger.printCards(...)` (duplicazione stampa schede tra matchup e outcome); resto lasciato imperativo (ASCII art 3 colonne, accumulo righe scheda, fallback `vitalsAfter`). `mvn -o test` 96/96 verdi post-modifica.

## Esito finale
- **Stato:** `COMPLETED`. Tutte le Parti (1-4) e le DoD 1-12 implementate; suite completa 96/96 verde.
- **Dominio:** `TurnHighlight` (in `result`), `TurnChronicler`, `FavoriteEstimator`; `TurnLogEntry.highlights`; `CombatSettings.ChronicleWeights(0.25)`; `TurnOrchestrator` raccoglie gli highlight e arricchisce la descrizione (colpi normali invariati); iniziativa senza test sotto override (`InitiativeResolver.overriddenNextAttacker`, `CombatEngine` non lancia i jitter sotto override).
- **Presentazione:** `FighterCardFormatter`; `CombatScreenRenderer` a 3 colonne (barre / solo turno corrente / schede); `reportOutcome` con favorito, ribaltone ed eventi citati; nuove firme `CombatReplay.replay`/`CombatLogger.reportOutcome` con i `Fighter`; LINEAR invariata.
- **Test:** dominio 83/83, poi presentazione 96/96 (`mvn -o clean compile test`). Riallineati i test impattati da firme/sequenza dadi/wording.
- **Note residue:** l'header SCREEN usa un em-dash ("—") non-ASCII già presente prima di questa feature: si vede correttamente su terminale UTF-8, può apparire come mojibake in output catturato non-UTF8 (non introdotto qui). Parametro `currentAttacker` di `overriddenNextAttacker` volutamente inutilizzato (simmetria di firma, documentato). Nessuna modifica al motore di calcolo né al `fantasytoolkit`.

## Esempio (concreto: componenti previsti e mappa test ↔ Definition of done)
```java
// File previsti (nuovi):
//   engine/TurnHighlight        — enum CRITICAL, PERFECT_HIT, KNOCKOUT, HEAVY_BLOW
//   engine/TurnChronicler       — wording da cronaca (puro), precedenza applicata
//   engine/FavoriteEstimator    — favorito dai rating fissi (puro): off+def -> maxHealth -> maxStamina
//   io/FighterCardFormatter     — scheda compatta multi-riga condivisa
// File toccati (chiave):
//   result/TurnLogEntry         — + List<TurnHighlight> highlights (+ withHighlights)
//   engine/TurnOrchestrator     — raccoglie highlight, descrizione arricchita
//   config/CombatSettings       — + ChronicleWeights(heavyBlowHealthRatio=0.25)
//   io/CombatScreenRenderer     — 3 colonne: barre / turno corrente / schede
//   io/{CombatLogger,CombatReplay,ScreenCombatReplay,ConsoleCombatLogger}, Arena — firme + Fighter

// Test previsti: uno o più per criterio della DoD (DoD 11 verificato in revisione)
@Test void colpoCritico_emetteCriticalNonPerfect()            { /* DoD 1 */ }
@Test void ventiNaturale_emettePerfectHit_conCriticalCompresente() { /* DoD 2 */ }
@Test void colpoDiGrazia_emetteKnockout()                     { /* DoD 3 */ }
@Test void dannoOltreSoglia_emetteHeavyBlow_soglieConfigurabili() { /* DoD 4 */ }
@Test void descrizioneArricchita_concisaEConPrecedenza()      { /* DoD 5 */ }
@Test void favorito_perRatingConSpareggiEdEquilibrato()       { /* DoD 6 */ }
@Test void schedaCompatta_riusataInMatchupSchermoERiepilogo() { /* DoD 7 */ }
@Test void screenTreColonne_soloTurnoCorrenteAlCentro()       { /* DoD 8 */ }
@Test void riepilogo_favoritoRibaltoneEventiPareggioTimeout() { /* DoD 9 */ }
@Test void linearReplay_restaFunzionante()                    { /* DoD 10 */ }
```
