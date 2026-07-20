# IMPLEMENTATION — Colpo potente

**Specifica di riferimento:** `spec-colpo-potente.md`  — nel resto del documento: «la SPEC».
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
- Java 21; codice di dominio/presentazione sobrio. Identificatori in inglese; commenti, Javadoc e stringhe di output console in italiano; indentazione a 2 spazi.
- Test JUnit Jupiter 5 con **solo** `org.junit.jupiter.api.Assertions` (niente AssertJ). **Nessuna nuova dipendenza** (runtime o test).
- ASCII puro nella presentazione; nessun colore ANSI, nessun box-drawing Unicode.
- **Motore invariato**: nessuna modifica a formule preesistenti (hit, critico, varianza, momentum, iniziativa, stamina base), esito, terminazione, immutabilità dei Rating. L'unica aggiunta è additiva (colpo potente).
- Resolver e modello di decisione **puri**: il jitter è iniettato dallo shell, nessun tiro interno.
- Pesi/soglie solo in `CombatSettings.PowerStrikeWeights` (schema `*Weights` + `defaults()`), nessuna formula in `CombatSettings`.

## Ordine di lavoro (riduzione del rischio)
Prima il **dominio** (config → resolver puro → integrazione orchestrator → danno → highlight/descrizione, con i relativi test), poi la **presentazione** (citazione nel riepilogo finale). Il modello di decisione è puro e a basso rischio; l'integrazione nell'orchestrator introduce lo spostamento della sequenza dei dadi (rischio da gestire con il riallineamento dei test scriptati).

## Piano operativo

**Fase 1 — Analisi**
- [x] Confermare le firme reali dei punti coinvolti (`TurnOrchestrator.resolveTurn/resolveMiss/resolveHitLanded`, `DamageCalculator.calculateDamage`, `StaminaRules.effectiveAttackCost`, `FighterState.canAfford/consumeStamina`, `CombatSettings` + sotto-record, `TurnHighlight`, `TurnLogEntry`, `TurnChronicler`, `ConsoleCombatLogger.printChronicle/describeHighlightLabel`).
- [x] Confermare l'accesso a intelligenza/momentum/vita/stamina e a `MomentumWeights.max()` per la normalizzazione.
- [x] Confermare l'ordine attuale dei dadi per turno d'attacco e il punto esatto in cui inserire il jitter di decisione.
- [x] Rilevare lo stile dei test esistenti (`CombatFixtures`, `StubDiceRoller` con sequenza programmata, assert su substring della descrizione e su highlight).
- [x] Elencare i test esistenti da riallineare per la nuova sequenza di dadi e per la nuova firma di `calculateDamage` (vedi "File coinvolti").
- [x] Confermare/aggiornare "File coinvolti (effettivi)".

**Fase 2 — Dominio: configurazione**
- [x] `CombatSettings`: nuovo sotto-record `PowerStrikeWeights` con i campi e i `defaults()` della SPEC (Parte 1), agganciato al record top-level e a `CombatSettings.defaults()`.

**Fase 3 — Dominio: resolver puro della decisione**
- [x] `PowerStrikeResolver` (puro, package `engine`): `decide(Fighter, DiceThrow)` + `score(Fighter, DiceThrow)`; modello di calcolo della SPEC (Parte 2), momentum normalizzato su `momentumWeights.max()`, `intelFactor` saturato su `intelligenceReference`. Nessun tiro interno, nessuna verifica di affordabilità (responsabilità dello shell).

**Fase 4 — Dominio: integrazione nell'orchestrator + danno**
- [x] `TurnOrchestrator.resolveTurn(...)`: dopo l'esclusione del riposo e la verifica dell'attacco base, calcolare `powerCost`; **solo se** `canAfford(powerCost)` tirare il jitter e chiamare `PowerStrikeResolver.decide(...)`; consumare `powerCost` o `attackCost` di conseguenza; propagare il flag `powerStrike`.
- [x] `DamageCalculator.calculateDamage(...)`: nuovo parametro `boolean powerStrike`; moltiplicatore separato e cumulativo col critico (Parte 4).
- [x] `resolveHitLanded(...)` / `resolveMiss(...)`: ricevere il flag `powerStrike`, propagarlo al calcolo danno e alla raccolta highlight/descrizione.

**Fase 5 — Dominio: highlight + descrizione (cronista)**
- [x] `TurnHighlight`: nuovo valore `POWER_STRIKE`.
- [x] `TurnOrchestrator.collectHighlights/collectOffensiveHighlights`: emettere `POWER_STRIKE` quando il colpo potente va a segno (`HIT_TAKEN`).
- [x] `TurnChronicler`: wording del colpo potente come qualificatore dell'attacco (a segno) e wording dedicato per il colpo potente mancato; precedenza definita; testo conciso (troncamento SCREEN); `resolveMiss` passa dal cronista quando `powerStrike`.

**Fase 6 — Presentazione: riepilogo finale**
- [x] `ConsoleCombatLogger.describeHighlightLabel(...)`: inserire `POWER_STRIKE` nella precedenza `KNOCKOUT > PERFECT_HIT > CRITICAL > POWER_STRIKE > HEAVY_BLOW` e la relativa etichetta.

**Fase 7 — Test**
- [x] Un test per ciascun criterio della *Definition of done* della SPEC (vedi mappa in "Esempio").
- [x] Riallineare i test esistenti impattati (sequenza dadi / firma `calculateDamage` / wording).
- [x] Eseguire l'intera suite (`mvn -o test`).

**Fase 8 — Revisione**
- [x] Coerenza con la SPEC; nessuna modifica non richiesta; formule preesistenti invariate.
- [x] Revisione funzionale Java (`java-functional-evolver`) su dominio e presentazione; test ri-verificati verdi.
- [x] Aggiornare *Decisioni/Deviazioni*, *Registro* ed *Esito finale*; portare lo stato a `COMPLETED`.

## File coinvolti (effettivi)
Elenco provvisorio dall'analisi — da confermare/aggiornare in Fase 1 (package base `it.fantasyarena.combat`).

Da creare:
- `src/main/java/it/fantasyarena/combat/engine/PowerStrikeResolver.java` — decisione pura del colpo potente (`decide` + `score`)
- `src/test/java/it/fantasyarena/combat/engine/PowerStrikeResolverTest.java` — DoD 2
- `src/test/java/it/fantasyarena/combat/engine/TurnOrchestratorPowerStrikeTest.java` — DoD 3, 4, 6, 9 (orchestrator con `StubDiceRoller`)
- `src/test/java/it/fantasyarena/combat/config/CombatSettingsPowerStrikeTest.java` — DoD 1 (oppure asserzione dentro un test di settings esistente)

Da modificare:
- `src/main/java/it/fantasyarena/combat/config/CombatSettings.java` — sotto-record `PowerStrikeWeights` + aggancio a `defaults()`
- `src/main/java/it/fantasyarena/combat/engine/TurnOrchestrator.java` — decisione colpo potente, consumo `powerCost`, propagazione flag, highlight `POWER_STRIKE`, descrizione (Parti 3/5)
- `src/main/java/it/fantasyarena/combat/engine/DamageCalculator.java` — parametro `boolean powerStrike`, moltiplicatore cumulativo col critico (Parte 4)
- `src/main/java/it/fantasyarena/combat/engine/TurnChronicler.java` — wording colpo potente (a segno e mancato)
- `src/main/java/it/fantasyarena/combat/result/TurnHighlight.java` — nuovo valore `POWER_STRIKE`
- `src/main/java/it/fantasyarena/combat/io/ConsoleCombatLogger.java` — `POWER_STRIKE` nella precedenza del riepilogo finale (Parte 6)

Test esistenti da riallineare (motivo tra parentesi) — da confermare in Fase 1:
- `src/test/java/it/fantasyarena/combat/engine/DamageCalculatorContextTest.java` — chiama `calculateDamage(...)` a 6 argomenti (nuova firma con `powerStrike`)
- `src/test/java/it/fantasyarena/combat/engine/TurnOrchestratorHighlightsTest.java` — turni con attacco pagabile-potente: jitter di decisione in più → sequenza `StubDiceRoller` da riallineare
- `src/test/java/it/fantasyarena/combat/engine/TurnOrchestratorDefenseTest.java` — idem sequenza dadi (assert sul costo attacco base e sull'esito difesa)
- `src/test/java/it/fantasyarena/combat/engine/TurnOrchestratorRestTest.java` — verificare: nel ramo riposo non si tira il jitter (nessun impatto atteso); confermare i turni non-riposo
- `src/test/java/it/fantasyarena/combat/engine/CombatEngineTest.java` e `CombatEngineInitiativeTest.java` — duelli completi scriptati: sequenza dadi spostata dai jitter di decisione dei turni pagabili-potente
- `src/test/java/it/fantasyarena/combat/engine/ResolverPurityTest.java` — verificare se invoca `calculateDamage(...)` (eventuale nuova firma)
- eventuali altri test con assert su descrizione o su sequenza dadi (da confermare in Fase 1)

Non previsto in modifica: `Main.java` (wiring invariato), `pom.xml` (nessuna nuova dipendenza), `TurnLogFormatter` e i replay LINEAR/SCREEN (leggono la stessa descrizione di dominio; nessuna modifica di layout).

## Decisioni proposte (da confermare in Fase 1, non bloccanti)
- **Firma del resolver**: `boolean decide(Fighter, DiceThrow)` + `double score(Fighter, DiceThrow)` pubblico per test deterministici sui casi borderline; costruttore `PowerStrikeResolver(CombatSettings settings)` (accede a `PowerStrikeWeights` e `MomentumWeights`).
- **Wiring del resolver**: istanziato dentro `TurnOrchestrator` come già fa per `TurnChronicler`/`AttackAction` (nessun cambio di firma del costruttore dell'orchestrator), salvo emerga la necessità di iniettarlo (per test) — da confermare in Fase 1.
- **Propagazione del flag**: `boolean powerStrike` passato per parametro a `resolveMiss`/`resolveHitLanded`/`calculateDamage` (nessun campo nuovo su `HitOutcome`, per minimizzare il blast radius come già fatto per `attackThrow` nella feature cronaca-duello).
- **Wording**: il "colpo potente" è un qualificatore prefissato all'attacco nel `TurnChronicler`, componibile con perfetto/critico; il mancato usa una frase dedicata ("tenta un colpo potente ma manca"). Da rifinire per rispettare il troncamento SCREEN.

## Registro
Voci datate (`YYYY-MM-DD`), append-only.

- **Decisioni tecniche** (non cambiano il comportamento) — `Decisione · Motivazione · Impatto`.
  - 2026-07-20 · resolver istanziato internamente al `TurnOrchestrator`, `score(...)` package-visible, flag `powerStrike` per parametro (no campo su `HitOutcome`) · minimizza il blast radius · come da decisioni proposte.
  - 2026-07-20 · `powerCost` calcolato una sola volta in `resolveTurn` (revisione funzionale) · rimuove ricalcolo duplicato · behavior-preserving.
- **Deviazioni dalla SPEC** (da motivare) — `Descrizione · Motivazione · Impatto · Aggiorna la SPEC? sì/no`.
  - 2026-07-20 · aggiunti a valle del primo giro il **cooldown** del colpo potente (Parte 7) e il **dimezzamento di `restRecovery`** 12→6 (Parte 8), su richiesta esplicita dell'utente per correggere il loop "colpo potente↔riposo" · bilanciamento · SPEC aggiornata (Parti 7-8, DoD 11-12) · Aggiorna la SPEC? sì (fatto).
- **Problemi aperti** (bloccano l'avanzamento) — `Descrizione · Impatto · Opzioni · Decisione richiesta`. Nessuno.
- **Test eseguiti** — `data · fase · comando · esito`.
  - 2026-07-20 · dominio (Fasi 2-5,7) · `mvn -o test` · 113/113 verdi.
  - 2026-07-20 · cooldown + tuning + Fase 6 (citazione finale) · `mvn -o test` · 117/117 verdi.
- **Revisione funzionale** (`java-functional-evolver`) — `invocato/non invocato · cosa ha cambiato`.
  - 2026-07-20 · dominio · invocato · nessuna modifica (codice già idiomatico: calcolo scalare del resolver, branching con side-effect/dadi imperativo, precedenza cronista a guardie).
  - 2026-07-20 · cooldown/tuning/finale · invocato · estratto `powerCost` in variabile locale (ricalcolo duplicato); resto imperativo per stato/side-effect/precedenza. `mvn -o test` 117/117.

## Punti "da decidere" residui
- **Wiring del `PowerStrikeResolver`**: istanziazione interna al `TurnOrchestrator` vs iniezione da costruttore (impatta la testabilità dell'orchestrator e le fixture). Proposta: interna, come `TurnChronicler`. Da confermare in Fase 1.
- **Esposizione di `score(...)`**: confermare se renderlo pubblico (per gli assert borderline) o package-visible.
- **Formulazione esatta del wording** del colpo potente a segno e mancato (deve restare entro il troncamento SCREEN): da rifinire in Fase 5 con i test del cronista.

## Esito finale
- **Stato:** `COMPLETED`. DoD 1-12 implementate; suite completa 117/117 verde.
- **Dominio:** `PowerStrikeResolver` puro (decisione con jitter dal `DiceRoller`), `PowerStrikeWeights` in `CombatSettings`, doppio costo/danno (danno cumulativo col critico in `DamageCalculator`), highlight `POWER_STRIKE`, wording cronista (a segno e mancato). Cooldown di 4 turni d'azione su `FighterState` (verifica-poi-decrementa, gate `canAfford && powerReady`). `restRecovery` 12→6.
- **Presentazione:** citazione `POWER_STRIKE` nel riepilogo finale con precedenza `KNOCKOUT > PERFECT_HIT > CRITICAL > POWER_STRIKE > HEAVY_BLOW`.
- **Test:** dominio 113/113, poi cooldown+tuning+finale 117/117. Riallineati i test impattati da firma `calculateDamage` e da `restRecovery`; test engine scriptati stabili grazie a `CombatFixtures.withPowerStrikeUnaffordable` e ad asserzioni relative sul riposo.
- **Note residue:** i valori di taratura (`decisionThreshold=0.6`, pesi, `cooldownTurns=4`, `restRecovery=6`) sono empirici e facilmente ritarabili in `CombatSettings`. Con jitter attivo i duelli demo si giocano diversamente rispetto a prima (dado di decisione consumato quando il colpo potente è pagabile e pronto), come atteso. Nessuna modifica al motore di calcolo preesistente né al `fantasytoolkit`.

## Esempio (concreto: componenti previsti e mappa test ↔ Definition of done)
```java
// File previsti (nuovi):
//   engine/PowerStrikeResolver  — decisione pura: decide(Fighter, DiceThrow) + score(...)
// File toccati (chiave):
//   config/CombatSettings       — + PowerStrikeWeights (costMultiplier=2, damageMultiplier=2.0, ...)
//   engine/TurnOrchestrator     — jitter di decisione solo se powerCost pagabile; consumo powerCost; flag powerStrike
//   engine/DamageCalculator     — + boolean powerStrike (moltiplicatore cumulativo col critico)
//   engine/TurnChronicler       — wording colpo potente (a segno e mancato)
//   result/TurnHighlight        — + POWER_STRIKE
//   io/ConsoleCombatLogger      — POWER_STRIKE nella precedenza del riepilogo finale

// Test previsti: uno o più per criterio della DoD (DoD 10 verificato in revisione)
@Test void powerStrikeWeights_neiDefaults()                       { /* DoD 1 */ }
@Test void decide_altoStaminaVitaIntelligenza_scegliePotente()    { /* DoD 2 */ }
@Test void decide_bassaStaminaVita_intelligenzaAlta_nonScegle()   { /* DoD 2 */ }
@Test void decide_bassaIntelligenzaMomentumAlto_overconfidence()  { /* DoD 2 */ }
@Test void decide_intelligenzaAltaAnnullaOverconfidence()         { /* DoD 2 */ }
@Test void decide_jitterSpostaIlBorderline()                      { /* DoD 2 */ }
@Test void nonPagabile_nessunColpoPotenteNessunJitter()           { /* DoD 3 */ }
@Test void colpoPotente_consumaDoppiaStamina()                    { /* DoD 4 */ }
@Test void colpoPotenteASegno_raddoppiaDanno_cumulativoColCritico() { /* DoD 5 */ }
@Test void colpoPotenteASegno_emettePowerStrike_mancato_soloDescrizione() { /* DoD 6 */ }
@Test void descrizionePotente_concisaEIdenticaLinearScreen()      { /* DoD 7 */ }
@Test void riepilogo_citaPowerStrike_conPrecedenza()              { /* DoD 8 */ }
@Test void colpoPotenteNonScelto_identicoAOggi()                  { /* DoD 9 */ }
```
