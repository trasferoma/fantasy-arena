# SPEC — Cronaca del duello

**Obiettivo:** arricchire la presentazione del combattimento — layout SCREEN a tre colonne con schede combattenti curate, descrizioni di turno "da cronaca" per gli eventi notevoli (dato di dominio) e un riepilogo finale che spiega il "motivo" dell'esito citando favorito, ribaltone ed eventi salienti — senza alterare il motore di calcolo del duello.

**Contesto**
- Punti del codice interessati: sottosistema `it.fantasyarena.combat.*` già esistente e maturo (SPEC precedente `spec-combat-engine.md`, stato `COMPLETED`). La feature tocca il dominio del turno (`it.fantasyarena.combat.engine`), il modello di log (`it.fantasyarena.combat.result`), la configurazione (`it.fantasyarena.combat.config`) e soprattutto la presentazione (`it.fantasyarena.combat.io`).
- Stato attuale verificato leggendo il codice:
  - `TurnOrchestrator.resolveHitLanded(...)` costruisce la descrizione del turno come `attackAction.describe(attacker, defender, context)` + `describeDefense(...)`. Oggi il **critico** (`HitOutcome.critical()`) e il **20 naturale** (`DiceThrow.isNaturalMaximum()`, già gestito in `HitResolver.resolveHit` come colpo+critico garantiti) **non compaiono nel testo**. Il `varianceThrow` e `damage` sono noti in `resolveHitLanded`; `defender.isDefeated()` è verificabile dopo `defender.state().applyDamage(damage)`.
  - `HitOutcome` è `record HitOutcome(boolean hit, boolean critical)`: **non** distingue il 20 naturale dal critico ordinario. Il `DiceThrow attackThrow` che lo distingue vive in `TurnOrchestrator.resolveTurn(...)` e non è oggi propagato a `resolveHitLanded(...)`.
  - `TurnLogEntry` è un `record(turnNumber, description, vitals, initiative, staminaChanges)` con costruttore breve a 2 argomenti e wither `withVitals/withInitiative/withStaminaChanges` (ognuno ricrea l'intero record). `CombatEngine.fight(...)` concatena i wither per arricchire la voce prodotta dal `TurnOrchestrator`.
  - `TurnResult(TurnLogEntry logEntry, InitiativeOverride override)` è il veicolo turno→motore.
  - `IntrinsicRatings(offensiveRating, defensiveRating, maxHealth, maxStamina)` è immutabile e già disponibile su `Fighter.ratings()`; `Fighter` espone `name()`, `character()` (razza/classe), `weapon()` (`weapon()`, `rarity()`, `attack()`), `armour()` (`armour()`, `rarity()`, `defense()`).
  - `ConsoleCombatLogger.reportMatchup(Fighter, Fighter)` stampa già la scheda dei due combattenti su 3 righe (~oltre 60 caratteri, non affiancabili a 80 colonne). `reportOutcome(CombatResult)` mostra oggi solo vincitore/pareggio e stato finale, **senza** i due `Fighter` (il `CombatResult.winner()` è però `Optional<Fighter>`).
  - `CombatScreenRenderer(List<TurnLogEntry>, List<FighterVitals>)` compone la pagina SCREEN a **due zone**: pannelli barre VITA/STAMINA a sinistra (marcatore `*Nome*` per l'iniziativa, segno +/- di variazione) e a destra la **coda del log cumulativo** (`buildRightLog`, `RIGHT_LOG_WIDTH=70`, `formatter.formatCompact` su `log.subList(0, turnPosition+1)`). Non riceve i `Fighter`.
  - `ScreenCombatReplay.replay(CombatResult)` e `LinearCombatReplay.replay(CombatResult)` implementano `CombatReplay.replay(CombatResult)`: nessuno riceve i `Fighter`. `Arena.run(Fighter first, Fighter second)` ha già entrambi i combattenti e cabla logger + replay.
  - `TurnLogFormatter.formatCompact(entry)` produce le righe compatte "-> vince l'iniziativa...", "-> primo ad agire..." e la descrizione; `format(entry)` è la versione lineare completa.
  - `CombatSettings` è un `record` con sotto-record dedicati (`RatingWeights`, `MomentumWeights`, `StaminaWeights`, `ChanceWeights`, `InitiativeWeights`) ciascuno con `defaults()`: è il punto naturale dove aggiungere una nuova soglia configurabile.
- Pattern/meccanismi da riusare: separazione core puro / shell già in essere; i resolver puri ricevono `DiceThrow`; il `TurnLogFormatter` puro condiviso tra le modalità; i wither di `TurnLogEntry`; lo schema "un sotto-record di pesi con `defaults()`" di `CombatSettings`; le fixture deterministiche `CombatFixtures` e lo `StubDiceRoller` (che pilota la sequenza dei dadi) per i test.
- Progetto: Maven, Java 21, applicazione console. Test JUnit Jupiter 5 con **solo** `org.junit.jupiter.api.Assertions` (niente AssertJ, niente nuove dipendenze). Commenti/Javadoc in italiano, indentazione a 2 spazi. Identificatori in inglese; stringhe di output in italiano.

**Comportamento atteso**

*Parte 1 — Layout SCREEN a 3 colonne + scheda curata (sola presentazione).*
- La pagina SCREEN è composta da tre colonne affiancate:
  - Colonna 1 (sinistra): i due pannelli barre verticali VITA/STAMINA, **invariati** (marcatore `*Nome*` per l'iniziativa del turno corrente, segni +/- di variazione).
  - Colonna 2 (centro): **solo** gli eventi del TURNO CORRENTE (le righe compatte "-> vince l'iniziativa...", "-> primo ad agire...", descrizione arricchita). Niente più log cumulativo.
  - Colonna 3 (destra): schede COMPATTE multi-riga dei due combattenti (razza/classe, arma con rarità e attacco, armatura con rarità e difesa, vita, stamina, ATK, DEF) con etichette brevi, così da stare affiancate.
- La formattazione della scheda è estratta in un nuovo `FighterCardFormatter` (package `io`), puro (nessun I/O), riusato da `ConsoleCombatLogger.reportMatchup`, dal renderer SCREEN e dal riepilogo finale.
- Il renderer SCREEN e `ScreenCombatReplay` ricevono anche i due `Fighter` (thread esplicito dei parametri; i `Fighter` **non** entrano in `CombatResult`).

*Parte 2 — Cronista: eventi inaspettati nella descrizione del turno (dominio).*
- `TurnOrchestrator` traccia, in modo tipizzato, gli eventi notevoli del turno e arricchisce la descrizione con tono da cronaca. Eventi gestiti:
  1. `CRITICAL` — colpo critico (`hitOutcome.critical()`) non dovuto al 20 naturale.
  2. `PERFECT_HIT` — colpo perfetto = 20 naturale (`attackThrow.isNaturalMaximum()`); sottocaso di critico ma con enfasi maggiore, distinto.
  3. `KNOCKOUT` — colpo di grazia: porta la vita del difensore a 0 (`defender.isDefeated()` dopo l'applicazione del danno).
  4. `HEAVY_BLOW` — colpo pesante: `damage >= soglia% * maxHealth` del bersaglio, con soglia **configurabile** (default 25%) aggiunta in `CombatSettings`.
- Gli highlight sono la **fonte unica di verità**: vengono aggiunti a `TurnLogEntry` come lista tipizzata; da lì li leggono narrazione finale e (eventualmente) la presentazione.
- Un singolo colpo può essere contemporaneamente `PERFECT_HIT`, `CRITICAL`, `KNOCKOUT` e `HEAVY_BLOW`: tutti gli highlight applicabili sono tracciati. Nel **testo** vale una precedenza per restare concisi: il "20 naturale" assorbe il wording del "critico ordinario" (non si dicono entrambe le cose), il colpo di grazia (KO) è la chiusa dominante, il colpo pesante è un rafforzativo. Il testo resta breve: non deve rompere il troncamento della colonna SCREEN.
- La descrizione arricchita è dato di dominio: compare **identica** in entrambe le modalità (LINEAR e SCREEN).

*Parte 3 — Riepilogo finale con il "motivo".*
- `reportOutcome` mostra di nuovo le schede compatte ("nell'ultimo scontro:") e poi una narrazione che: indica il **favorito** pre-scontro, chi ha vinto, se è stato un **ribaltone** (favorito ≠ vincitore), e cita gli eventi notevoli tracciati scandendo `result.log()` (es. "il colpo critico di X al turno 5 ha ribaltato il pronostico"). Gestisce anche pareggio (`DRAW`) e vittoria ai punti (`TIMEOUT_DECISION`).
- Il **favorito** è deterministico dai rating fissi, calcolato da un componente puro (`FavoriteEstimator`) indipendente dall'RNG: favorito = combattente con `offensiveRating + defensiveRating` più alto; a parità vince `maxHealth`, poi `maxStamina`; se ancora pari nessun favorito netto → "equilibrato".
- `CombatLogger.reportOutcome` e `CombatReplay.replay` cambiano firma per ricevere i due `Fighter` (interfacce interne, usate solo da `Arena` e dalle impl); `Arena.run` ha già entrambi i combattenti.

*Parte 4 — Iniziativa: nessun test di vittoria sotto override (dominio).*
- Quando il turno precedente ha prodotto un override (`DODGE_STEAL` per schivata riuscita del difensore, `REST_YIELD` per riposo dell'attaccante), la scelta dell'iniziativa del turno successivo è **già deterministica** (il difensore corrente diventa attaccante). In questo caso **non si esegue il test a punteggio**: nessun lancio del jitter per i due contendenti, nessun calcolo dei `InitiativeBreakdown`, nessuno `scoreWinner`.
- L'`InitiativeReport` prodotto sotto override contiene **solo** il motivo dell'override e chi agisce: `breakdowns` vuoto, nessun vincitore per punteggio. Il primo mover del duello (`resolveFirstMover`) e i turni **senza** override (`NONE`) continuano a usare il test a punteggio completo, invariato.
- La scelta di chi attacca **non cambia** rispetto a oggi (sotto override era già il difensore corrente): cambia solo che non si consumano più i due dadi di jitter e che il report non riporta più il test.
- La presentazione (LINEAR e SCREEN/compact) sotto override mostra solo "primo ad agire: Y (motivo override)", **senza** la riga "vince l'iniziativa (punteggio)" e senza i breakdown.

- Invariante: le formule del motore (Rating, hit/critico, danno, stamina, momentum, esito) restano **identiche**; la scelta di chi attacca resta identica turno per turno (vedi Parte 4: sotto override il test non viene più eseguito, ma l'attaccante scelto è lo stesso di prima); la modalità LINEAR resta disponibile e funzionante; SCREEN resta default; il numero e la matematica delle barre restano invariati.

**Vincoli**
- Retrocompatibile sul motore: nessuna modifica a formule, esito, terminazione, immutabilità dei Rating. L'unico effetto osservabile nuovo sul dominio è la **descrizione arricchita** e la **lista di highlight** (additive).
- I `Fighter` non entrano in `CombatResult`: vanno passati per parametro esplicito a renderer/replay/reportOutcome.
- Riuso del `FighterCardFormatter` in tutti e tre i punti (matchup, SCREEN col.3, riepilogo finale): una sola sorgente di formattazione della scheda.
- ASCII puro per barre, cornici e colonne; nessuna sequenza colore. Commenti/Javadoc in italiano, indentazione a 2 spazi, identificatori in inglese.
- Nessuna nuova dipendenza (né runtime né test). Test con solo `org.junit.jupiter.api.Assertions`.
- La soglia del colpo pesante vive in `CombatSettings` come dato di configurazione, coerente con lo schema dei sotto-record `*Weights` esistenti; nessuna formula in `CombatSettings`.
- Complessità di lettura: il testo del cronista è prodotto da un componente puro dedicato (non un blocco di if annidati dentro `TurnOrchestrator`); la stima del favorito è pura e testabile in isolamento.
- **Rischio larghezza**: due pannelli barre (~17 col. ciascuno) + colonna eventi + colonna schede difficilmente stanno in 80 colonne. Le schede vanno progettate compatte (etichette brevi, multi-riga) e le colonne dimensionate per una larghezza ragionevole (~120 col.); il rischio va segnalato e mitigato, non ignorato.
- **Sequenza dei dadi sotto override (Parte 4)**: saltare i due lanci di jitter quando l'override non è `NONE` cambia la sequenza di `DiceThrow` consumata dal duello. Con il `DiceRoller` reale l'esito resta casuale e valido; i test deterministici basati su `StubDiceRoller` che si affidano alla sequenza vanno riallineati. L'esito "chi attacca" per il turno con override resta identico; possono cambiare i valori dei dadi consumati dai turni successivi nei test scriptati.

**Fuori scope**
- Qualsiasi modifica alle formule/bilanciamento del motore, all'iniziativa, alla stamina, al momentum.
- Nuovi tipi di evento oltre ai quattro highlight elencati; abilità/magie; modalità batch.
- Colori ANSI, box-drawing Unicode, ridisegni animati diversi dall'attuale clear-and-redraw.
- Persistenza, statistiche aggregate, più di due combattenti.
- Qualsiasi modifica al `fantasytoolkit`.

**Definition of done** — criteri verificabili (ognuno coperto da almeno un test, tranne l'ultimo verificato in revisione)
1. Su un colpo critico non dovuto al 20 naturale, il turno emette l'highlight `CRITICAL` (e non `PERFECT_HIT`).
2. Su un 20 naturale (`attackThrow.isNaturalMaximum()`) il turno emette `PERFECT_HIT`; l'highlight `CRITICAL` resta comunque tracciato come compresente.
3. Quando il colpo porta il difensore a 0, il turno emette `KNOCKOUT`.
4. Quando `damage >= soglia% * maxHealth` del bersaglio (soglia configurabile, default 25%) il turno emette `HEAVY_BLOW`; cambiando la soglia in `CombatSettings` cambia coerentemente l'emissione.
5. La descrizione del turno è arricchita con tono da cronaca coerente con gli highlight e con la precedenza definita (il 20 naturale assorbe il wording del critico ordinario; il KO è la chiusa dominante; il colpo pesante è rafforzativo), resta concisa ed è identica tra LINEAR e SCREEN.
6. `FavoriteEstimator` sceglie il favorito per `offensiveRating + defensiveRating`; spareggio su `maxHealth`, poi `maxStamina`; a parità totale dichiara "equilibrato" (nessun favorito).
7. `FighterCardFormatter` produce una scheda compatta multi-riga (razza/classe, arma+rarità+attacco, armatura+rarità+difesa, vita, stamina, ATK, DEF) riusata da `reportMatchup`, dal renderer SCREEN e dal riepilogo finale.
8. Il renderer SCREEN dispone tre colonne: barre invariate (col.1), **solo** gli eventi del turno corrente (col.2, non il log cumulativo), schede dei due combattenti (col.3); il marcatore `*Nome*` e i segni +/- restano invariati.
9. Il riepilogo finale mostra le schede compatte e una narrazione che cita il favorito pre-scontro, il vincitore, il ribaltone se favorito ≠ vincitore e almeno un evento notevole tracciato (con il numero di turno); gestisce sia `DRAW` sia `TIMEOUT_DECISION`.
10. La modalità LINEAR resta funzionante e strutturalmente invariata (unica differenza attesa: la descrizione ora arricchita).
11. Nessuna nuova dipendenza; ASCII puro; formule del motore invariate (verificato in revisione).
12. Sotto override (`DODGE_STEAL`/`REST_YIELD`) il resolver dell'iniziativa non calcola i breakdown né lo `scoreWinner` e non consuma i dadi di jitter; l'`InitiativeReport` ha `breakdowns` vuoti; la presentazione (LINEAR e SCREEN) non mostra la riga "vince l'iniziativa (punteggio)" né i breakdown, ma solo chi agisce e il motivo dell'override. Il primo mover e i turni `NONE` restano col test a punteggio completo, e l'attaccante scelto sotto override è identico a prima.

**Esempio** (istanza concreta — solo illustrativo)
```java
// Parte 2 — TurnOrchestrator: raccoglie gli highlight del turno (fonte unica di verità)
// e delega il wording a un cronista puro. attackThrow è propagato per distinguere il 20 naturale.
List<TurnHighlight> highlights = new ArrayList<>();
if (attackThrow.isNaturalMaximum()) {
  highlights.add(TurnHighlight.PERFECT_HIT);
}
if (hitOutcome.critical()) {
  highlights.add(TurnHighlight.CRITICAL);
}
if (damage >= chronicleWeights.heavyBlowHealthRatio() * defender.ratings().maxHealth()) {
  highlights.add(TurnHighlight.HEAVY_BLOW);
}
if (defender.isDefeated()) {
  highlights.add(TurnHighlight.KNOCKOUT);
}
String description = turnChronicler.describe(attacker, defender, damage, highlights); // testo conciso, precedenza applicata
TurnLogEntry entry = new TurnLogEntry(turnNumber, description).withHighlights(highlights);

// Parte 3 — FavoriteEstimator puro (nessun RNG): dai rating fissi.
Optional<Fighter> favorite = favoriteEstimator.favorite(first, second); // empty => "equilibrato"

// Parte 3 — reportOutcome riceve i Fighter, mostra le schede e spiega il "motivo".
logger.reportOutcome(result, first, second);
//   "Favorito alla vigilia: Alice. Vince Bob: ribaltone!"
//   "Il colpo perfetto di Bob al turno 7 ha ribaltato il pronostico."
```
