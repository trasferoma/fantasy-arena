# SPEC — Colpo potente

**Obiettivo:** dare all'attaccante la possibilità di sferrare un "colpo potente" che consuma il doppio della stamina e infligge il doppio del danno; la scelta è una valutazione guidata dall'intelligenza, dove il momentum positivo genera overconfidence (che può portare a valutare male) mentre un'intelligenza alta protegge dall'errore.

**Contesto**
- Sottosistema interessato: `it.fantasyarena.combat.*`, motore locale con separazione core puro / shell già matura (SPEC precedenti `spec-combat-engine.md` e `spec-cronaca-duello.md`, entrambe `COMPLETED`). La feature tocca il dominio del turno (`engine`), la configurazione (`config`), il modello di log (`result`) e la presentazione del riepilogo finale (`io`).
- Stato attuale verificato leggendo il codice:
  - `TurnOrchestrator.resolveTurn(...)` è il punto d'ingresso del turno d'attacco. Oggi: calcola `attackCost = staminaRules.effectiveAttackCost(consecutiveInitiativeWins)`; se `shouldRest(...)` o `!canAfford(attackCost)` → `resolveRest(...)`; altrimenti `consumeStamina(attackCost)`, poi `attackThrow = diceRoller.d20()`, `hitResolver.resolveHit(...)`, e infine `resolveMiss(...)` (miss) o `resolveHitLanded(...)` (colpo a segno).
  - **Ordine dei dadi per turno d'attacco, oggi** (nel `TurnOrchestrator`): (1) `d20()` tiro d'attacco; (2) `d20()` tiro di difesa — **solo** se il colpo va a segno e `canDefend(...)`; (3) `d100()` varianza danno — **solo** se il colpo va a segno. Su un miss si consuma solo il `d20()` d'attacco. I due jitter d'iniziativa sono lanciati a monte da `CombatEngine` (2 al primo mover, 2 tra un turno e l'altro salvo override), non dal `TurnOrchestrator`.
  - `resolveHitLanded(...)` calcola `damage = damageCalculator.calculateDamage(attacker, defender, context, hitOutcome, defenseOutcome, varianceThrow)`, applica il danno, aggiorna momentum/stamina, raccoglie gli highlight (`collectHighlights` → `collectOffensiveHighlights` su `HIT_TAKEN` + `KNOCKOUT` se `defender.isDefeated()`) e compone la descrizione: `attackAction.describe(...)` + `turnChronicler.describeOutcome(...)`.
  - `resolveMiss(...)` produce la descrizione piatta `"<attaccante> attacca <difensore> ma manca il colpo."` (non passa dal `TurnChronicler`).
  - `DamageCalculator.calculateDamage(attacker, defender, context, hitOutcome, defenseOutcome, varianceThrow)`: `rawDamage → applyVariance → applyCritical → * (1 - defenseOutcome.damageReduction())`. Il critico è un moltiplicatore separato (`chanceWeights().criticalDamageMultiplier()`) applicato in `applyCritical`.
  - `StaminaRules` espone `attackCost()`, `effectiveAttackCost(consecutiveInitiativeWins)` (costo base + malus di catena, con cap) e i vari costi; `FighterState.canAfford(amount)` / `consumeStamina(amount)`, `currentStamina()`, `currentHealth()`, `momentum()`; `IntrinsicRatings.maxHealth()`/`maxStamina()`.
  - Accesso all'intelligenza: `Characteristics.valueOf(fighter.character(), Characteristic.INTELLIGENCE)` (già usato in `InitiativeResolver`).
  - `DiceRoller.roll(int faces)` restituisce un `DiceThrow(value, faces)`; `DiceThrow.normalized()` ∈ (0,1], `value()`, `faces()`, `isNaturalMaximum()`. Il jitter d'iniziativa usa già `roll(jitterDiceFaces)` con le facce tarate in `CombatSettings.InitiativeWeights`.
  - `MomentumWeights.max()` (default 100) è il riferimento per normalizzare il momentum, come già fa `MomentumRules.effectMultiplier`.
  - `CombatSettings` è un `record` con sotto-record `*Weights` (`RatingWeights`, `MomentumWeights`, `StaminaWeights`, `ChanceWeights`, `InitiativeWeights`, `ChronicleWeights`), ciascuno con `defaults()`, agganciati al record top-level e a `CombatSettings.defaults()`. È lo schema in cui va aggiunto un nuovo sotto-record di pesi.
  - `TurnHighlight` (package `result`) è un enum con `CRITICAL`, `PERFECT_HIT`, `KNOCKOUT`, `HEAVY_BLOW`; `TurnLogEntry` porta `List<TurnHighlight> highlights` con wither `withHighlights(...)`. `TurnChronicler` (package `engine`, puro) produce il wording con una precedenza (KO chiusa dominante, perfetto assorbe il critico ordinario, pesante rafforzativo).
  - `ConsoleCombatLogger.printChronicle(...)` cita nel finale un evento notevole con `describeHighlightLabel(...)`, precedenza attuale `KNOCKOUT > PERFECT_HIT > CRITICAL > HEAVY_BLOW`.
  - `TurnLogFormatter` (`format`/`formatCompact`) e le due modalità di replay (LINEAR e SCREEN) leggono la stessa descrizione di dominio: la descrizione arricchita compare identica in entrambe.
- Pattern/meccanismi da riusare: resolver puri che ricevono `DiceThrow` dallo shell (come `InitiativeResolver` per il jitter); schema "sotto-record `*Weights` con `defaults()`" di `CombatSettings`; highlight come fonte unica di verità su `TurnLogEntry`; `TurnChronicler` come unico autore del wording; le fixture deterministiche `CombatFixtures` e lo `StubDiceRoller` (sequenza di dadi programmata) per i test.
- Progetto: Maven, Java 21, applicazione console. Test JUnit Jupiter 5 con **solo** `org.junit.jupiter.api.Assertions` (niente AssertJ, niente nuove dipendenze). Commenti/Javadoc in italiano, indentazione a 2 spazi; identificatori in inglese, stringhe di output in italiano; ASCII puro nella presentazione.

**Comportamento atteso**

*Parte 1 — Configurazione.*
- Nuovo sotto-record `PowerStrikeWeights` in `CombatSettings`, con `defaults()`, agganciato al record top-level e a `CombatSettings.defaults()`. Campi e default (empirici, provvisori, tarabili come gli altri):
  - `costMultiplier` (int, `2`) — moltiplicatore del costo stamina d'attacco.
  - `damageMultiplier` (double, `2.0`) — moltiplicatore del danno.
  - `staminaWeight` (double, `0.5`) — peso della stamina nella parte razionale.
  - `healthWeight` (double, `0.5`) — peso della vita nella parte razionale.
  - `overconfidenceWeight` (double, `0.5`) — peso dell'overconfidence da momentum.
  - `intelligenceReference` (double, `18.0`) — intelligenza di riferimento per saturare `intelFactor`.
  - `jitterWeight` (double, `0.2`) — peso del micro-jitter casuale.
  - `jitterDiceFaces` (int, `6`) — facce del dado di jitter di decisione.
  - `decisionThreshold` (double, `0.6`) — soglia dello score sopra la quale il colpo potente viene scelto.
  - `cooldownTurns` (int, `4`) — turni di ricarica (turni d'azione del combattente) durante i quali il colpo potente non è ripetibile dopo essere stato eseguito.

*Parte 2 — Decisione pura del colpo potente (nuovo `PowerStrikeResolver` in `engine`).*
- Componente puro che, dato il combattente e un `DiceThrow` di jitter iniettato dallo shell, decide se tentare il colpo potente. Modello di calcolo:
  - `staminaRatio  = currentStamina / maxStamina` ∈ [0,1]
  - `healthRatio   = currentHealth / maxHealth` ∈ [0,1]
  - `momentumNorm  = clamp(momentum / momentumWeights.max(), 0, 1)` (solo momentum positivo)
  - `intelFactor   = clamp(intelligence / intelligenceReference, 0, 1)`
  - `rational      = staminaWeight*staminaRatio + healthWeight*healthRatio`
  - `overconfidence= overconfidenceWeight*momentumNorm`
  - `score         = rational + (1 - intelFactor)*overconfidence + jitterWeight*jitterNormalized`
  - decisione `= score >= decisionThreshold`
- `jitterNormalized = DiceThrow.normalized()` del dado di jitter a `jitterDiceFaces` facce.
- Firma: `boolean decide(Fighter attacker, DiceThrow jitterThrow)` (puro, nessun tiro interno). Espone anche `double score(Fighter attacker, DiceThrow jitterThrow)` per i test (assert deterministici sui casi borderline).
- Effetto dell'intelligenza: intelligenza alta → `intelFactor≈1` → `score≈rational` (decisione ben calibrata); intelligenza bassa + momentum alto → l'overconfidence si somma e può superare la soglia anche in situazione sfavorevole (la "prevaricazione" richiesta).
- Il prerequisito di **affordabilità** (`canAfford(costMultiplier*effectiveAttackCost)`) NON è responsabilità del resolver: è verificato dallo shell (`TurnOrchestrator`) prima di tirare il jitter e chiamare `decide(...)`.

*Parte 3 — Integrazione nel `TurnOrchestrator`.*
- In `resolveTurn(...)`, dopo aver escluso il riposo e verificato che l'attacco base è pagabile:
  - calcolare `powerCost = powerStrikeWeights.costMultiplier() * attackCost` (con `attackCost = effectiveAttackCost(...)`);
  - **solo se** `canAfford(powerCost)`: tirare `powerJitter = diceRoller.roll(powerStrikeWeights.jitterDiceFaces())` e invocare `powerStrikeResolver.decide(attacker, powerJitter)`;
  - se il colpo potente è scelto: consumare `powerCost` (invece del costo base) e propagare il flag `powerStrike = true` a valle;
  - altrimenti (declinato, o colpo potente non pagabile): consumare il costo base `attackCost`, `powerStrike = false`, comportamento identico a oggi.
- **Il dado di jitter va tirato SOLO quando il colpo potente è pagabile**: quando non lo è, non si consuma alcun dado in più e la sequenza dei dadi del turno resta identica a oggi.
- **Ordine dei dadi per turno d'attacco, nuovo**: (0) `roll(jitterDiceFaces)` jitter di decisione — **solo** se `canAfford(powerCost)`; (1) `d20()` attacco; (2) `d20()` difesa se applicabile; (3) `d100()` varianza se colpo a segno.

*Parte 4 — Danno.*
- `DamageCalculator.calculateDamage(...)` riceve un nuovo parametro `boolean powerStrike` e, se attivo, applica `damageMultiplier` come **ulteriore** moltiplicatore, di natura analoga al critico e **cumulativo** con esso (un colpo potente e critico moltiplica per entrambi). Il moltiplicatore del colpo potente è uno step separato da `applyCritical`.

*Parte 5 — Esito del colpo, highlight e descrizione.*
- `resolveHitLanded(...)` e `resolveMiss(...)` ricevono il flag `powerStrike` per: (a) raddoppiare il danno (via il parametro a `calculateDamage`); (b) raccogliere l'highlight `POWER_STRIKE`; (c) arricchire la descrizione via `TurnChronicler` sia sul colpo a segno sia sul mancato.
- Nuovo valore `POWER_STRIKE` in `TurnHighlight`. Scoping: emesso **quando il colpo potente va a segno** (esito di difesa `HIT_TAKEN`), coerente con gli altri highlight offensivi. Su un colpo potente mancato **nessun** highlight, ma descrizione dedicata (la doppia stamina sprecata è il rischio esplicito: "tenta un colpo potente ma manca").
- `TurnChronicler` estende il wording per il colpo potente: il "colpo potente" è un **qualificatore dell'attacco**, componibile con perfetto/critico; la precedenza resta: KO chiusa dominante, poi perfetto (che assorbe il critico ordinario)/critico, con il colpo potente come qualificatore che precede l'esito. Il testo resta conciso: non deve rompere il troncamento della colonna SCREEN. Anche il mancato passa dal cronista (wording dedicato). La descrizione è dato di dominio: identica in LINEAR e SCREEN.

*Parte 6 — Riepilogo finale.*
- `ConsoleCombatLogger` può citare `POWER_STRIKE` tra gli eventi notevoli. Precedenza dell'etichetta aggiornata: `KNOCKOUT > PERFECT_HIT > CRITICAL > POWER_STRIKE > HEAVY_BLOW`.

*Parte 7 — Cooldown del colpo potente (bilanciamento).*
- Dopo aver ESEGUITO un colpo potente (scelto e pagato, a segno o mancato), il combattente non può rieseguirlo per `cooldownTurns` turni d'azione (default 4). Questo evita il loop deggenere "colpo potente → riposo → colpo potente → riposo".
- Stato del cooldown su `FighterState` (per combattente): un contatore `powerStrikeCooldown` (0 = pronto). Semantica precisa: nel turno d'azione del combattente si verifica prima la prontezza (`ready = powerStrikeCooldown == 0`), poi si decrementa il contatore se > 0; all'esecuzione di un colpo potente si imposta il contatore a `cooldownTurns`. Con `cooldownTurns=4` il colpo potente resta indisponibile per i 4 turni d'azione successivi ed è di nuovo disponibile al 5°.
- Il gating è nello shell (`TurnOrchestrator`): il jitter di decisione è tirato solo se il colpo potente è **pagabile E pronto** (cooldown a 0). Il resolver puro resta invariato (non conosce il cooldown).

*Parte 8 — Taratura del recupero da riposo (bilanciamento).*
- `StaminaWeights.restRecovery` passa da `12` a `6` (dimezzato): il riposo recuperava troppa stamina e alimentava il loop col colpo potente. Valore provvisorio, tarabile.

- Invariante: le formule del motore (Rating, hit/critico, varianza, stamina, momentum, iniziativa, esito, terminazione) restano **identiche**; le uniche aggiunte sono additive (il colpo potente e il suo cooldown) più una ritaratura di `restRecovery`. Quando il colpo potente NON è scelto, gli effetti di combattimento (costo pagato = base, danno singolo, descrizione senza wording di colpo potente) sono identici a oggi a parità di `restRecovery`. LINEAR resta disponibile, SCREEN resta default.

**Vincoli**
- Retrocompatibile: nessuna modifica a formule, esito, terminazione, immutabilità dei Rating. L'unico effetto nuovo è additivo (colpo potente: costo/danno raddoppiati, highlight `POWER_STRIKE`, descrizione arricchita).
- **Sequenza dei dadi**: quando il colpo potente è pagabile si consuma un dado di jitter di decisione in più, **prima** del `d20` d'attacco. Con il `DiceRoller` reale l'esito resta casuale e valido; i test deterministici basati su `StubDiceRoller` che attraversano turni con attacco pagabile-potente vanno riallineati. Quando il colpo potente non è pagabile la sequenza è identica a oggi.
- Il resolver e il modello di decisione sono **puri** e testabili in isolamento (jitter iniettato, nessun tiro interno). Il jitter è tirato solo dallo shell.
- I pesi/soglie vivono in `CombatSettings.PowerStrikeWeights` come dati di configurazione, coerenti con lo schema `*Weights` + `defaults()`; **nessuna formula** in `CombatSettings`.
- Nessuna nuova dipendenza (runtime o test); test con solo `org.junit.jupiter.api.Assertions`. ASCII puro nella presentazione; identificatori in inglese, commenti/stringhe in italiano, indentazione a 2 spazi.
- Il colpo potente NON altera momentum né iniziativa: l'eventuale maggiore perdita di stamina di chi incassa è solo la conseguenza naturale del danno maggiore (via `impactStaminaLoss(damage)`), non una regola nuova.

**Fuori scope**
- Qualsiasi modifica alle formule/bilanciamento del motore diverse dall'aggiunta additiva del colpo potente (hit, critico, varianza, momentum, iniziativa, stamina base).
- Effetti del colpo potente su momentum, iniziativa, status effect; colpi potenti in difesa o su azioni diverse dall'attacco pieno.
- Nuovi highlight oltre a `POWER_STRIKE`; abilità/magie; layout SCREEN (colonne, barre) diverso da quello attuale; colori ANSI/box-drawing Unicode.
- Persistenza, statistiche aggregate, più di due combattenti; qualsiasi modifica al `fantasytoolkit`.

**Definition of done** — criteri verificabili (ognuno coperto da almeno un test, tranne l'ultimo verificato in revisione)
1. `PowerStrikeWeights` è presente in `CombatSettings.defaults()` con i default indicati (`costMultiplier=2`, `damageMultiplier=2.0`, `staminaWeight=0.5`, `healthWeight=0.5`, `overconfidenceWeight=0.5`, `intelligenceReference=18.0`, `jitterWeight=0.2`, `jitterDiceFaces=6`, `decisionThreshold=0.6`).
2. `PowerStrikeResolver.decide(...)` puro: stamina+vita alte con intelligenza alta → sceglie il colpo potente; stamina/vita basse con intelligenza alta → NON lo sceglie; intelligenza bassa + momentum alto → lo sceglie anche in situazione sfavorevole (overconfidence); a parità di scenario sfavorevole, l'intelligenza alta annulla l'overconfidence (non lo sceglie); il jitter sposta i casi borderline (stesso scenario, jitter min vs max → decisione diversa).
3. Prerequisito di affordabilità: se non si può pagare `costMultiplier*effectiveAttackCost`, il colpo potente non è tentato e **nessun** dado di jitter viene tirato (sequenza dadi identica a oggi).
4. Il colpo potente scelto consuma `costMultiplier×` la stamina d'attacco effettiva (non il costo base).
5. Il colpo potente a segno infligge `damageMultiplier×` il danno; il moltiplicatore è cumulativo con il critico (colpo potente + critico → entrambi i moltiplicatori).
6. Un colpo potente a segno emette l'highlight `POWER_STRIKE`; un colpo potente mancato non emette highlight ma produce una descrizione dedicata.
7. La descrizione è arricchita dal cronista sia sul colpo potente a segno sia sul mancato, resta concisa, applica la precedenza definita (KO dominante; colpo potente come qualificatore componibile con perfetto/critico) ed è identica tra LINEAR e SCREEN.
8. Il riepilogo finale può citare `POWER_STRIKE` con nome del combattente e numero di turno, secondo la precedenza `KNOCKOUT > PERFECT_HIT > CRITICAL > POWER_STRIKE > HEAVY_BLOW`.
9. Quando il colpo potente non è scelto (declinato o non pagabile) gli effetti di combattimento sono identici a oggi a parità di `restRecovery`: costo pagato = base, danno singolo, descrizione senza wording di colpo potente. (Unica differenza di sequenza ammessa e documentata: il dado di jitter di decisione consumato quando il colpo potente è pagabile e pronto.)
10. Nessuna nuova dipendenza; ASCII puro; formule del motore preesistenti invariate (verificato in revisione).
11. Cooldown: dopo un colpo potente eseguito, il combattente non può rieseguirlo per `cooldownTurns` turni d'azione; il jitter di decisione non viene tirato mentre il cooldown è attivo (non pronto). Trascorso il cooldown torna disponibile.
12. `restRecovery` di default è `6` (dimezzato da 12).

**Esempio** (istanza concreta — solo illustrativo)
```java
// TurnOrchestrator.resolveTurn: il jitter di decisione è tirato SOLO se il colpo potente
// è pagabile; il resolver puro riceve il DiceThrow dallo shell (nessun tiro interno).
int attackCost = staminaRules.effectiveAttackCost(attacker.state().consecutiveInitiativeWins());
if (staminaRules.shouldRest(attacker.state().currentStamina()) || !attacker.state().canAfford(attackCost)) {
  return resolveRest(turnNumber, attacker);
}

int powerCost = settings.powerStrikeWeights().costMultiplier() * attackCost;
boolean powerStrike = false;
if (attacker.state().canAfford(powerCost)) {
  DiceThrow powerJitter = diceRoller.roll(settings.powerStrikeWeights().jitterDiceFaces());
  powerStrike = powerStrikeResolver.decide(attacker, powerJitter);
}

attacker.state().consumeStamina(powerStrike ? powerCost : attackCost);

DiceThrow attackThrow = diceRoller.d20();      // ordine dei dadi invariato a valle
// ... hit/miss come oggi, propagando 'powerStrike' a resolveMiss / resolveHitLanded

// DamageCalculator: il colpo potente è un moltiplicatore separato, cumulativo col critico.
double criticalDamage = applyCritical(variedDamage, hitOutcome);
double poweredDamage = powerStrike
    ? criticalDamage * settings.powerStrikeWeights().damageMultiplier()
    : criticalDamage;
double finalDamage = poweredDamage * (1.0 - defenseOutcome.damageReduction());
```
