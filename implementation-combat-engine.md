# IMPLEMENTATION — Combat Engine / Arena

**Specifica di riferimento:** `spec-combat-engine.md`  — nel resto del documento: «la SPEC».
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
- Java 21. **Naming**: identificatori (classi, metodi, variabili) in inglese; commenti, Javadoc e stringhe di output console in italiano.
- I result del toolkit in sola lettura (niente lettura sorgenti né decompilazione del JAR).
- **`Main` non contiene logica**: istanzia/invoca solo `Arena`.
- **Complessità di lettura a strati**: `Arena` e gli orchestratori (`CombatEngine`, `TurnOrchestrator`, `FighterFactory`) si leggono come metodi parlanti che chiamano metodi parlanti; formule e matematica vivono SOLO nel core (`DefaultRatingStrategy`, `DamageCalculator`, `MomentumRules`, `StaminaRules`, `HitResolver`, `DefenseResolver`, `InitiativeResolver`).
- **Functional core / imperative shell**: i dadi si lanciano nello shell (`TurnOrchestrator`/`CombatEngine`) via `DiceRoller` e i `DiceThrow` vengono passati come input ai resolver puri. I resolver del core sono funzioni pure: nessun lancio interno, nessun effetto collaterale. `DiceLauncherTool` è toccato solo da `DiceRoller` (shell).
- **Value object dei dadi**: `DiceThrow` (record `DiceThrow(int value, int faces)`), scelto al posto di `DiceRoll` per evitare il clash con `it.fantasytoolkit.dicelauncher.result.DiceRoll` del toolkit.

## Decisioni già prese (dai punti «DA DECIDERE» della SPEC)
- **A — JUnit 5**: CONFERMATO. Aggiungere JUnit 5 in scope `test` (Fase 1).
- **C — Modello di casualità**: RIFORMULATO. Pattern functional core / imperative shell; dadi nello shell via `DiceRoller`, passati come input ai resolver puri. `DiceLauncherTool` ammesso solo nello shell; `RandomGenerator` seedabile solo come eventuale sorgente alternativa dietro `DiceRoller`, non primario.
- **D — Esito timeout**: CONFERMATO. Vittoria ai punti su health%; `DRAW` a parità esatta.
- **F — Nome "Momentum"**: CONFERMATO.
- **G — Stamina insufficiente**: CONFERMATO. Azione con penalità di affaticamento, floor a 0.
- **H — Scudo**: CONFERMATO. `ArmourResult` opzionale (`Armour.SHIELD`), non esercitato in v1.
- **E — Batch**: CONFERMATO rinviato a v1.5 (Fase 7 opzionale).
- **Naming**: CONFERMATO. Identificatori in inglese; commenti/Javadoc/output in italiano; value object `DiceThrow`.
- **B — Pesi delle formule**: aperto ma non bloccante → taratura empirica in Fase 3/4.

## Piano operativo

**Fase 1 — Analisi, verifica API e setup test** ✅ COMPLETATA (2026-07-19)
- [x] Confermare le firme reali del toolkit usate dalla SPEC sulla doc `docs/agent/` della versione in uso (in particolare i builder di arma/armatura, lo scudo come `ArmourResult` e la firma di `DiceLauncherTool` usata da `DiceRoller`).
- [x] Confermare le assunzioni della SPEC su `CharacterResult`/`CharacterCharacteristic`/enum e sull'assenza di equipaggiamento nel personaggio.
- [x] Aggiungere JUnit 5 in scope `test` nel `pom.xml` (decisione A) e predisporre `src/test/java`.
- [x] Segnalare eventuali lacune documentali (API mancanti) invece di dedurle dai sorgenti.
- [x] Confermare/aggiornare "File coinvolti (effettivi)".

**Fase 2 — Modello dei dati (immutabile vs mutabile vs contestuale + value object del core)** ✅ COMPLETATA (2026-07-19)
- [x] `Characteristics` helper di lettura valori dal `CharacterResult`.
- [x] `IntrinsicRatings` (immutabile): offensiveRating, defensiveRating, maxHealth, maxStamina.
- [x] `FighterState` (mutabile): currentHealth, currentStamina, momentum, statusEffects (vuoto in v1).
- [x] `Fighter` aggregato di dati (nessuna formula; delega one-liner NON aggiunta — i resolver sono chiamati direttamente dal `TurnOrchestrator`, vedi Decisioni).
- [x] `DiceThrow` (`record(int value, int faces)`), `HitOutcome`, `DefenseOutcome`.
- [x] `CombatContext` + `ContextModifier` + `ContextModifierSource` (context vuoto in v1).

**Fase 3 — Core funzionale (formule e regole pure, l'unico con matematica; riceve `DiceThrow`)** ✅ COMPLETATA (2026-07-19)
- [x] `CombatSettings` con i pesi/costi/soglie tarabili.
- [x] `RatingStrategy` (interfaccia) + `DefaultRatingStrategy` con le formule della SPEC.
- [x] `HitResolver.resolveHit(attacker, defender, DiceThrow)` → `HitOutcome` (puro).
- [x] `DefenseResolver.resolveDefense(defender, attacker, DiceThrow)` → `DefenseOutcome` (puro).
- [x] `InitiativeResolver`, `DamageCalculator`, `MomentumRules`, `StaminaRules` (puri; ricevono `DiceThrow` dove serve casualità).

**Fase 4 — Shell / orchestratori (parlanti, senza matematica; qui vivono i lanci)** ✅ COMPLETATA (2026-07-19)
- [x] `DiceRoller` — facade sottile su `DiceLauncherTool`; `d20()`, `d100()` → `DiceThrow`. Unico punto di contatto col toolkit per la casualità.
- [x] `FighterFactory.createSwordWarrior()` — costruisce i `Fighter` dai generatori del toolkit applicando la `RatingStrategy`.
- [x] `TurnOrchestrator.playTurn(...)` — lancia i dadi via `DiceRoller`, passa i `DiceThrow` ai resolver puri, applica danno e aggiorna stamina/momentum.
- [x] `CombatEngine.fight(...)` — ciclo del duello: iniziativa, turni, condizione di fine, tetto turni, esito (inclusa decisione ai punti su timeout).
- [x] `CombatOutcome` (VICTORY, TIMEOUT_DECISION, DRAW), `TurnLogEntry`, `CombatResult`.
- [x] `CombatLogger` + `ConsoleCombatLogger` (`reportOutcome(...)`).

**Fase 5 — Facade `Arena` e integrazione in `Main`** ✅ COMPLETATA (2026-07-19)
- [x] `Arena` (facade): `run()` → `prepareFighters()`, `runDuel()`, `reportOutcome()`; solo metodi parlanti, nessuna formula, nessun lancio diretto (composition root delle dipendenze).
- [x] Ridurre `Main` alla sola invocazione di `Arena` (`new Arena().run();`, nessuna logica residua).

**Fase 6 — Test (decisione A: JUnit 5)** ✅ COMPLETATA (2026-07-19)
- [x] Un test per ciascun criterio 1–9 della *Definition of done* della SPEC (+ `StubDiceRoller` e `CombatFixtures` di supporto, + test mirato sul critico da massimo naturale).
- [x] DoD 3: verificata la purezza dei resolver passando `DiceThrow` fissi; verificato un duello deterministico stubbando `DiceRoller` con una sequenza di dadi truccata.
- [x] Il criterio 10 (letture a strati + separazione core/shell) verificato in revisione (Fase 8), non con test unitario.
- [x] Eseguita la suite: `mvn -o test` → Tests run: 11, Failures: 0, Errors: 0. BUILD SUCCESS.

**Fase 7 — (Opzionale, v1.5) Modalità batch**
- [ ] `ArenaBatchRunner`: N combattimenti con logger silenzioso e statistiche aggregate. Rinviata a v1.5 (decisione E).

**Fase 8 — Revisione** ✅ COMPLETATA (2026-07-19)
- [x] Coerenza con la SPEC; nessuna modifica non richiesta; `Main` invoca solo `Arena`; Rating immutabili durante lo scontro.
- [x] Verifica dei vincoli: nessuna formula in `Arena`/`CombatEngine`/`TurnOrchestrator`; nessun lancio di dadi nel core; `DiceLauncherTool` solo dentro `DiceRoller` (DoD 10).
- [x] Revisione funzionale Java (`java-functional-evolver`) e ri-verifica dei test.
- [x] Aggiornare *Decisioni/Deviazioni*, *Registro* ed *Esito finale*; portare lo stato a `COMPLETED`.

## File coinvolti (effettivi)
Da confermare in Fase 1 — elenco provvisorio dall'analisi (package base `it.fantasyarena.combat`):

Punto d'ingresso e facade:
- `src/main/java/it/fantasyarena/Main.java` — ridotto alla sola invocazione di `Arena` (nessuna logica)
- `src/main/java/it/fantasyarena/combat/Arena.java` — facade/entry point del sottosistema (orchestrazione parlante)

Shell / orchestratori (senza matematica; qui i lanci):
- `src/main/java/it/fantasyarena/combat/dice/DiceRoller.java` — facade su `DiceLauncherTool`, `d20()`/`d100()` → `DiceThrow` (unico contatto col toolkit per la casualità)
- `src/main/java/it/fantasyarena/combat/factory/FighterFactory.java` — costruzione Fighter dai generatori toolkit
- `src/main/java/it/fantasyarena/combat/engine/CombatEngine.java` — orchestrazione del duello (`fight`)
- `src/main/java/it/fantasyarena/combat/engine/TurnOrchestrator.java` — orchestrazione del singolo turno (`playTurn`; lancia dadi, delega ai resolver)

Core funzionale / strato di calcolo (funzioni pure, ricevono `DiceThrow`):
- `src/main/java/it/fantasyarena/combat/rating/RatingStrategy.java` — strategia dei Rating
- `src/main/java/it/fantasyarena/combat/rating/DefaultRatingStrategy.java` — formule di base
- `src/main/java/it/fantasyarena/combat/engine/InitiativeResolver.java` — iniziativa
- `src/main/java/it/fantasyarena/combat/engine/HitResolver.java` — test per colpire (`resolveHit`)
- `src/main/java/it/fantasyarena/combat/engine/DefenseResolver.java` — schivata/parata (`resolveDefense`)
- `src/main/java/it/fantasyarena/combat/engine/DamageCalculator.java` — calcolo danno
- `src/main/java/it/fantasyarena/combat/engine/MomentumRules.java` — regole momentum
- `src/main/java/it/fantasyarena/combat/engine/StaminaRules.java` — regole stamina
- `src/main/java/it/fantasyarena/combat/config/CombatSettings.java` — pesi/costi/soglie tarabili

Modello dei dati:
- `src/main/java/it/fantasyarena/combat/model/Characteristics.java` — helper lettura caratteristiche
- `src/main/java/it/fantasyarena/combat/model/IntrinsicRatings.java` — Rating + pool massimi (immutabile)
- `src/main/java/it/fantasyarena/combat/model/FighterState.java` — stato mutabile del combattente
- `src/main/java/it/fantasyarena/combat/model/Fighter.java` — aggregato combattente (dati; delega one-liner `rollToHit`/`rollToDefend`)
- `src/main/java/it/fantasyarena/combat/dice/DiceThrow.java` — `record(int value, int faces)` (value object del core)
- `src/main/java/it/fantasyarena/combat/engine/HitOutcome.java` — esito puro del test per colpire
- `src/main/java/it/fantasyarena/combat/engine/DefenseOutcome.java` — esito puro del test per difendere
- `src/main/java/it/fantasyarena/combat/action/CombatAction.java` — azione (estensione)
- `src/main/java/it/fantasyarena/combat/action/AttackAction.java` — attacco base v1
- `src/main/java/it/fantasyarena/combat/context/CombatContext.java` — context temporaneo (vuoto in v1)
- `src/main/java/it/fantasyarena/combat/context/ContextModifier.java` — modificatore temporaneo
- `src/main/java/it/fantasyarena/combat/context/ContextModifierSource.java` — sorgente di modificatori (estensione)

Risultato e output:
- `src/main/java/it/fantasyarena/combat/result/CombatOutcome.java` — enum esito
- `src/main/java/it/fantasyarena/combat/result/TurnLogEntry.java` — voce di log turno
- `src/main/java/it/fantasyarena/combat/result/CombatResult.java` — risultato finale
- `src/main/java/it/fantasyarena/combat/io/CombatLogger.java` — output astratto
- `src/main/java/it/fantasyarena/combat/io/ConsoleCombatLogger.java` — logger console

Build e test:
- `pom.xml` — dipendenza JUnit 5 in scope `test` (decisione A)
- `src/test/java/it/fantasyarena/combat/...` — test della DoD (criteri 1–9); stub di `DiceRoller` per i duelli deterministici

## Registro
Voci datate (`YYYY-MM-DD`), append-only.

- **Decisioni tecniche** (non cambiano il comportamento) — `Decisione · Motivazione · Impatto`.
  - 2026-07-19 · Fasi 2–5 (`clean-code-implementer`): 31 file creati, `mvn -o compile` → BUILD SUCCESS al primo tentativo; `mvn -o exec:java` → duelli coerenti entro il tetto di 30 turni.
  - `Fighter.rollToHit/rollToDefend` NON aggiunti · la SPEC li dava come "ammessi al più"; i resolver sono invocati direttamente dal `TurnOrchestrator` (come nello pseudocodice SPEC) · `Fighter` resta aggregato di dati senza dipendenze verso l'engine.
  - Critico senza tiro dedicato · la SPEC illustra un solo tiro per il test di colpire · `HitResolver` riusa `attackThrow` (max naturale ⇒ critico, altrimenti confronto con `critChance`).
  - `parryChance`: divisore troncato con "…" nella SPEC · risolto con `parryDefenseDivisor = 200.0` in `CombatSettings.ChanceWeights` · tarabile (punto B).
  - Iniziativa: formula non specificata in SPEC · `d20 + Agilità`, tie-break deterministico al primo combattente · isolata in `InitiativeResolver`.
  - Costo stamina di difesa applicato per esito reale (DODGED/PARRIED/HIT_TAKEN); il tiro di difesa/danno avviene solo se l'attacco va a segno · coerente con "la stamina non è una seconda barra vita".
- **Deviazioni dalla SPEC** (da motivare) — `Descrizione · Motivazione · Impatto · Aggiorna la SPEC? sì/no`.
  - 2026-07-19 · Bug fix `HitResolver` (Fase 6): il "critico su massimo naturale" della SPEC non scattava mai (con roll-under, `normalized()=1.0` del massimo naturale non supera `hitChance` clampata a 0.95, quindi era sempre un colpo mancato). Fix: early return in `resolveHit` → massimo naturale = colpo garantito e critico garantito, resto invariato · conforma il codice al contratto SPEC già approvato · Aggiorna la SPEC? no (già prescritto). Test protetto `HitResolverNaturalMaximumTest` reso verde; fixture `alwaysMissSequence` di `CombatEngineTest` aggiornata da `DiceThrow(20,20)` a `DiceThrow(19,20)` (dipendeva implicitamente dal bug).
- **Problemi aperti** (bloccano l'avanzamento) — nessuno. Punti A/C/D/E/F/G/H e naming risolti; B (pesi) non bloccante, taratura empirica.
- **Test eseguiti** — `data · fase · comando · esito`.
  - 2026-07-19 · Fase 1 · `mvn -q -o compile` · baseline OK (toolkit risolto da `.m2`).
  - 2026-07-19 · Fase 1 · `mvn -q dependency:resolve-plugins dependency:resolve` · JUnit 5 (5.11.4) + surefire (3.5.2) risolti.
  - 2026-07-19 · Fase 6 · `mvn -o test` · **11 test, tutti verdi** (BUILD SUCCESS). Copertura DoD 1–9 + test mirato sul critico.
  - 2026-07-19 · Fase 8 · `mvn -o clean test` · **11 test verdi** dopo la revisione funzionale (BUILD SUCCESS).

- **Revisione funzionale** (`java-functional-evolver`, 2026-07-19) — INVOCATO. Esito: intervento unico su `Characteristics.valueOf` (`for` con return interno + throw finale → `stream().filter().findFirst().map(...).orElseThrow(...)`), comportamento invariato. Tutto il resto lasciato invariato con motivazione (shell imperativo con mutazione voluta in `CombatEngine`/`TurnOrchestrator`; core già puro e idiomatico; `CombatSettings` già immutabile). Test verdi dopo la modifica.

- **Note Fase 1** — firme del toolkit confermate sulla doc `docs/agent/` (character/weapon/armour/dice-launcher/core): `CharacterResult` senza equipaggiamento; `WeaponResult.attack()`/`ArmourResult.defense()` int per rarità; `Weapon.SWORD`, `Armour.SHIELD`, `Armour.CHESTPLATE` presenti; `DiceLauncherTool.building().dice(n,faces).roll()` → `DiceRollResult`. Nessuna lacuna documentale. Nessuna decompilazione del JAR.

## Esito finale
**Stato: COMPLETED (2026-07-19).** Combat Engine / Arena v1 implementato, compilante e testato.

- **Modifiche effettuate**: creato il sottosistema `it.fantasyarena.combat.**` (modello, core puro, shell/orchestratori, `Arena` facade, result, io, action, context, dice); `Main` ridotto alla sola `new Arena().run();`; `pom.xml` con JUnit 5 (`test`) + surefire; suite di 11 test JUnit 5 con `StubDiceRoller`/`CombatFixtures`. Un fix di produzione su `HitResolver` (critico su massimo naturale, vedi Deviazioni). Una revisione funzionale su `Characteristics`.
- **Verifiche**: `mvn -o compile` OK; `mvn -o exec:java` → duelli coerenti entro il tetto turni; `mvn -o clean test` → 11/11 verdi. Vincoli architetturali (Main thin, functional core / imperative shell, `DiceLauncherTool` solo in `DiceRoller`, formule solo nel core — DoD 10) verificati in revisione.
- **Note residue / follow-up**:
  - **B — taratura dei pesi**: i coefficienti di `CombatSettings` (Rating, Health, Stamina, momentum, chance, `parryDefenseDivisor`, iniziativa) sono provvisori; vanno tarati sul bilanciamento reale. È il naturale lavoro successivo.
  - **E — modalità batch (v1.5)**: N combattimenti + statistiche aggregate per la validazione del bilanciamento; `CombatLogger` è già astratto per abilitarla.
  - Convenzione roll-under: con `normalized() <= chance` il tiro basso è il successo; il massimo naturale è gestito come caso speciale (colpo+critico garantiti). Da riconsiderare se in futuro si vuole una meccanica roll-high coerente end-to-end.
  - `Fighter.rollToHit/rollToDefend` non implementati (i resolver sono chiamati dal `TurnOrchestrator`): eventuale zucchero sintattico futuro, non necessario.

## Esempio (concreto: catena core/shell e mappa test ↔ Definition of done)
```java
// Catena a strati (functional core / imperative shell, dall'alto in basso):
//   Main            -> invoca solo Arena
//   Arena (shell)   -> run(): prepareFighters(), runDuel(), reportOutcome()     [nessuna formula]
//   FighterFactory  -> createSwordWarrior()                                      [orchestra generatori]
//   CombatEngine    -> fight(): iniziativa, ciclo turni, fine, esito             [nessuna formula]
//   TurnOrchestrator-> playTurn(): lancia dadi via DiceRoller e li passa ai resolver [nessuna formula]
//   DiceRoller      -> d20()/d100() -> DiceThrow                                 [unico contatto DiceLauncherTool]
//   HitResolver / DefenseResolver / InitiativeResolver / DamageCalculator /
//   MomentumRules / StaminaRules / DefaultRatingStrategy                         [core PURO: matematica su DiceThrow]

// Flusso del turno (shell -> puro):
//   DiceThrow attackThrow  = diceRoller.d20();
//   DiceThrow defenseThrow = diceRoller.d20();
//   HitOutcome     hit     = hitResolver.resolveHit(attacker, defender, attackThrow);
//   DefenseOutcome defense = defenseResolver.resolveDefense(defender, attacker, defenseThrow);

// Test previsti: uno per criterio 1–9 della DoD (JUnit 5). Il criterio 10 è verificato in revisione.
@Test void ratingsImmutableDuringCombat()                    { /* DoD 1 */ }
@Test void maxHealthAndStaminaDerivedFromCharacteristics()   { /* DoD 2 */ }
@Test void pureResolver_sameDiceThrow_sameOutcome()          { /* DoD 3 (purezza) */ }
@Test void deterministicDuel_withStubbedDiceRoller()         { /* DoD 3 (duello via stub) */ }
@Test void everyCombatEnds_withinTurnCap()                   { /* DoD 4 */ }
@Test void momentumWithinRange_effectCappedAt15pct()         { /* DoD 5 */ }
@Test void staminaDropsOnlyOnActionsAndImpact_withPenalties(){ /* DoD 6 */ }
@Test void emptyContext_doesNotAlterRatingsNorOutcome()      { /* DoD 7 */ }
@Test void successfulDefense_reducesDamageAndUpdatesMomentum(){ /* DoD 8 */ }
@Test void combatResult_consistentWithProgress()             { /* DoD 9 */ }
```
