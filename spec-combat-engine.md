# SPEC — Combat Engine / Arena

**Obiettivo:** introdurre nel consumer `fantasy-arena` un motore di combattimento automatico turn-based (Arena) che fa scontrare due combattenti generati dal `fantasytoolkit` per validare il bilanciamento della libreria e produrre un esito, mantenendo separate le proprietà permanenti dallo stato temporaneo del combattimento.

**Contesto**
- Punti del codice interessati: nuovo sottosistema `it.fantasyarena.combat.*` (oggi inesistente) e riduzione di `it.fantasyarena.Main` alla sola invocazione della nuova facade `Arena`.
- Stato attuale verificato: unico sorgente `src/main/java/it/fantasyarena/Main.java`, thin, che consuma i tool con pattern `Tool.building()...generate()` / `.roll()` e legge i result con accessor senza `get` (`character.name()`, `character.race()`, `character.characterClass()`, `character.characteristics()`, `roll.total()`).
- Progetto: Maven, Java 21 (`maven.compiler.source/target=21`), applicazione console, entry point `it.fantasyarena.Main`. Unica dipendenza `it.fantasytoolkit:fantasytoolkit:1.0-SNAPSHOT` (risolta solo dal repo Maven locale). **Nessuna dipendenza di test** nel `pom.xml`, nessun test nel repo.
- Pattern/meccanismi da riusare: i generatori del toolkit come uniche sorgenti di dominio; lo stile "thin" del punto d'ingresso. La logica NUOVA del combattimento vive QUI nel consumer.
- API reali del toolkit da usare (dalla doc ufficiale `docs/agent/` del toolkit; **non** leggere sorgenti né decompilare il JAR):
  - `CharacterResult(Race race, CharacterClass characterClass, String name, List<CharacterCharacteristic> characteristics)` — **non contiene equipaggiamento**.
  - `CharacterCharacteristic(Characteristic characteristic, int value)`; enum `Characteristic`: STRENGTH, INTELLIGENCE, AGILITY, CHARISMA, RESISTANCE, STAMINA, LUCK.
  - `Race`: HUMAN, ELF, ORC, UNDEAD; `CharacterClass`: WARRIOR, MAGE, THIEF, RANGER.
  - `WeaponResult(Weapon weapon, Rarity rarity, List<BuffElement> buffs, List<DebuffElement> debuffs, int attack)`; `ArmourResult(Armour armour, Rarity rarity, List<BuffElement> buffs, List<DebuffElement> debuffs, int defense)`.
  - `Weapon` include SWORD, SHIELD…; `Armour` include SHIELD, CHESTPLATE…
  - Generatori: `WeaponGeneratorTool.building().weapon(...).rarity(...).generate()`, analoghi per armour; `DiceLauncherTool.building().dice(n,faces[,code]).roll()` → `DiceRollResult(List<DiceRoll> rolls, int total)`.
  - Il toolkit espone già un tipo `it.fantasytoolkit.dicelauncher.result.DiceRoll`: il nostro value object dei dadi si chiama `DiceThrow` per evitare collisioni/import ambigui.
  - La generazione del toolkit usa `new Random()` interno **non seedabile** (non riproducibile).

**Comportamento atteso**
- L'Arena costruisce due **Combattenti** (v1: guerrieri con spada), esegue lo scontro turno per turno secondo il flusso dell'handoff e restituisce un `CombatResult` (vincitore/esito, numero di turni, log turn-by-turn).
- Le proprietà intrinseche (Offensive Rating, Defensive Rating, MaxHealth, MaxStamina) sono calcolate una volta alla costruzione del combattente e **non cambiano** durante lo scontro.
- Lo stato di combattimento (Health, Current Stamina, Momentum, Status Effects) è mutabile e vive separato dai Rating.
- Il `CombatContext` produce **solo modificatori temporanei** applicati durante il turno; in v1 è vuoto e non altera nulla, ma esiste come punto di estensione.
- Casi limite: nessuno dei due muore entro il tetto di turni → esito di **timeout deciso ai punti sulla percentuale di Health** (mai loop infinito); azione con stamina insufficiente gestita senza eccezioni con penalità di affaticamento; danno minimo garantito perché lo scontro progredisca.
- Invariante: `Main` non contiene alcuna logica e si limita a invocare `Arena`; nessuna logica di dominio "di generazione" viene reimplementata nel consumer; i result del toolkit sono usati in sola lettura.

**Convenzione di naming**
- Tutti gli identificatori (classi, metodi, variabili) sono in **inglese**. Commenti, Javadoc e stringhe di output su console sono in **italiano**.
- Il value object dei dadi si chiama `DiceThrow` (non `DiceRoll`) per evitare il clash con `it.fantasytoolkit.dicelauncher.result.DiceRoll` esposto dal toolkit.

**Architettura a strati — functional core / imperative shell (vincolo centrale)**
Il sottosistema è organizzato in strati con responsabilità nette. Lo **shell** (imperativo) esegue gli effetti — tra cui il lancio dei dadi — e passa i risultati come input al **core** (funzionale) di calcolo, fatto di funzioni pure e deterministiche a parità di input. La lettura scende dall'orchestrazione "parlante" al calcolo isolato:

1. **Punto d'ingresso** — `Main`: nessuna logica, istanzia/invoca solo `Arena`.
2. **Facade** — `Arena`: entry point del sottosistema. Prepara lo scenario dimostrativo e delega; non esegue calcoli, non contiene formule. Si legge come una narrazione di metodi parlanti (`run()` → `prepareFighters()`, `runDuel()`, `reportOutcome()`).
3. **Shell / orchestratori** (imperativi, coesi, senza matematica):
   - `FighterFactory` — costruisce i `Fighter` dai generatori del toolkit applicando la `RatingStrategy` (`createSwordWarrior()`).
   - `CombatEngine` — orchestra l'intero duello (`fight(...)`): iniziativa, ciclo dei turni, condizione di fine, esito. Delega ogni turno al `TurnOrchestrator`.
   - `TurnOrchestrator` — orchestra il singolo turno (`playTurn(...)`): lancio dadi → attacco → difesa → esito → danno → aggiornamento stamina/momentum → cambio turno. **Qui, e solo qui, si lanciano i dadi** (via `DiceRoller`) e i `DiceThrow` risultanti vengono passati ai resolver puri.
   - `DiceRoller` — facade sottile sopra `DiceLauncherTool`, unico punto che tocca il toolkit per la casualità; espone metodi parlanti (`d20()`, `d100()`) che restituiscono `DiceThrow`.
4. **Core funzionale / strato di calcolo** (funzioni pure; l'unico che contiene formule e matematica; riceve `DiceThrow`, non lancia nulla): `DefaultRatingStrategy`, `DamageCalculator`, `MomentumRules`, `StaminaRules`, `HitResolver`, `DefenseResolver`, `InitiativeResolver`.

Principio esplicito: **complessità di lettura minima** — orchestrazione parlante in alto, calcolo puro isolato in basso. Nessun blocco di formule dentro `Arena`, `CombatEngine` o `TurnOrchestrator`; nessun lancio di dadi nel core.

**Modello dei dati proposto**

*Tre livelli separati del combattente:*
1. **Intrinseco e immutabile** — `IntrinsicRatings`: `offensiveRating`, `defensiveRating`, `maxHealth`, `maxStamina`. Calcolati da una `RatingStrategy` a partire da caratteristiche + equipaggiamento + classe + razza. Nessun accesso all'avversario o al context.
2. **Stato mutabile** — `FighterState`: `currentHealth`, `currentStamina`, `momentum`, `statusEffects` (lista, vuota in v1).
3. **Contestuale e temporaneo** — `CombatContext` + `ContextModifier` prodotti da `ContextModifierSource` (lista vuota in v1); influenzano solo i valori effettivi del turno, mai i Rating.

Aggregato: `Fighter` = `CharacterResult` + arma (`WeaponResult`) + armatura (`ArmourResult`) + scudo opzionale (`ArmourResult` con `Armour.SHIELD`, previsto ma non esercitato in v1) + `IntrinsicRatings` + `FighterState`. Costruito dalla `FighterFactory`. `Fighter` è un **aggregato di dati: non contiene formule** (coerente col DoD 10). È ammessa al più una delega *one-liner* leggibile verso il resolver (es. `attacker.rollToHit(diceThrow)` che inoltra a `HitResolver`, `defender.rollToDefend(diceThrow)` verso `DefenseResolver`), pura delega senza matematica; la sede dei calcoli resta comunque nei resolver.

*Value object del core:*
- `DiceThrow` — `record DiceThrow(int value, int faces)`: esito tipizzato di un lancio; porta anche il numero di facce per riconoscere il "critico al massimo naturale" (es. `value == faces`). I resolver ricevono `DiceThrow`, mai `int` nudi.
- `HitOutcome` — risultato puro di `HitResolver.resolveHit(attacker, defender, DiceThrow)` (colpito/mancato, critico, dati per il danno).
- `DefenseOutcome` — risultato puro di `DefenseResolver.resolveDefense(defender, attacker, DiceThrow)` (schivata/parata/subìto e relativa riduzione).

**Formule di base (funzioni pure, deterministiche, tarabili — pesi provvisori, taratura empirica in implementazione)**
- Helper: `car(X)` = valore intero della caratteristica `X` del personaggio.
- Offensive Rating = `1.0*car(STRENGTH) + 0.5*car(AGILITY) + 2.0*weapon.attack() + classOffBonus + raceOffBonus`.
- Defensive Rating = `1.0*car(RESISTANCE) + 0.5*car(AGILITY) + 2.0*armour.defense() + 2.0*shield.defense()(se presente) + classDefBonus + raceDefBonus`.
- MaxHealth = `20 + 5*car(RESISTANCE) + 2*car(STAMINA)` (Health NON è una caratteristica: è derivata).
- MaxStamina = `10 + 3*car(STAMINA)` (pool massimo; distinto dalla Current Stamina di combattimento).
- Momentum ∈ `[-100, +100]`, iniziale 0. Guadagni: +8 colpo a segno, +15 critico inflitto, +10 parata riuscita, +10 schivata riuscita. Perdite: -8 colpo subito, -15 critico subito, -5 attacco mancato. Effetto sul danno/difesa limitato: `momentumMult = 1 + clamp(momentum/100, -1, 1) * 0.15` (max ±15%, anti-valanga).
- Costi Stamina (per azione, non per turno): attacco 6, parata 4, schivata 5, impatto subito 2. Penalità progressive sul rapporto `currentStamina/maxStamina`: `>50%` nessuna; `25–50%` -15% ad attacco e difesa (affaticato); `<25%` -30% ad attacco e difesa e preclusione di tecniche "impegnative" (rilevante da v2, enum già previsto). Stamina insufficiente per un'azione: azione consentita con penalità di affaticamento, `currentStamina` con floor a 0.
- Risoluzione colpo (dato il `DiceThrow` in input, nessun lancio nel core): il tiro `d20` è confrontato con la soglia `hitChance = clamp(0.75 + 0.02*(attAGI - defAGI), 0.40, 0.95)`; schivata con `dodgeChance = clamp(0.10 + 0.02*(defAGI - attAGI), 0.02, 0.50)`; parata con `parryChance = clamp(0.10 + defDefensive/…, 0.02, 0.50)` (guerriero con scudo più alta); critico su massimo naturale (`diceThrow.value() == diceThrow.faces()`) oppure `critChance = clamp(0.05 + 0.01*car(LUCK), 0.05, 0.40)`.
- Danno: `raw = max(1, effOff - 0.5*effDef)` con `effOff = OffensiveRating*momentumMult*staminaMult*ctxOffMult` e simmetrico per la difesa; variazione ±10% derivata da un `DiceThrow` passato in input; critico `*1.75`; parata riduce il danno del 70%. Il floor a 1 garantisce progresso.

**Modello di casualità (functional core / imperative shell)**
- La casualità NON vive nelle procedure di calcolo. I dadi si lanciano nello **shell** (`TurnOrchestrator`/`CombatEngine`) tramite `DiceRoller`, e i `DiceThrow` risultanti vengono passati come input ai resolver puri: a parità di `DiceThrow`, un resolver produce sempre lo stesso esito.
- `DiceLauncherTool` del toolkit è AMMESSO, ma solo nello shell e solo dietro `DiceRoller` (unico punto di contatto); non compare mai nel core di calcolo.
- Un `RandomGenerator` seedabile iniettato resta una possibile alternativa come sorgente dietro `DiceRoller`, ma **non è il meccanismo primario**: il meccanismo scelto è "dadi lanciati nello shell e passati come input".

**Punti di estensione (per gli obiettivi futuri, senza riscrivere il motore)**
- `RatingStrategy` — formula dei Rating sostituibile.
- `CombatAction` + `HitResolver` — in v1 solo `AttackAction`; futuro: abilità di classe, magie, attacchi speciali.
- `DefenseResolver` — risoluzione schivata/parata sostituibile.
- `DiceRoller` — sorgente dei dadi sostituibile (toolkit reale in produzione; stub deterministico nei test; eventuale `RandomGenerator` seedabile) senza toccare il core puro.
- `ContextModifierSource` — sorgenti di modificatori del context (terreno, meteo, benedizioni…); lista vuota in v1.
- `CombatLogger` — output astratto (`ConsoleCombatLogger` in v1); abilita modalità batch/silenziosa (v1.5).
- `TurnOrchestrator`/`CombatEngine` — l'aggancio di nuove azioni e passi di turno avviene qui senza toccare gli strati di calcolo.

**Vincoli**
- `Main` non contiene alcuna logica: istanzia/invoca solo `Arena` e nient'altro.
- Naming: identificatori in inglese; commenti, Javadoc e output console in italiano.
- Complessità di lettura a strati: `Arena` e gli orchestratori (`CombatEngine`, `TurnOrchestrator`, `FighterFactory`) si leggono come metodi parlanti che chiamano metodi parlanti; formule e matematica vivono solo nel core (`DefaultRatingStrategy`, `DamageCalculator`, `MomentumRules`, `StaminaRules`, `HitResolver`, `DefenseResolver`, `InitiativeResolver`). Nessun blocco di formule in `Arena` o negli orchestratori.
- Separazione core/shell: i resolver del core sono funzioni pure che ricevono `DiceThrow`; non lanciano dadi e non hanno effetti collaterali. Il lancio dei dadi e ogni contatto con `DiceLauncherTool` stanno solo nello shell, dietro `DiceRoller`.
- Nessuna logica di dominio "di generazione" reimplementata nel consumer; i result del toolkit sono usati in sola lettura, con le firme reali documentate (niente lettura sorgenti/decompilazione del JAR).
- Java 21, codice applicativo sobrio e leggibile.
- Nessuna nuova dipendenza runtime. Aggiunta di JUnit 5 in scope `test` (confermata).
- I Rating restano immutabili durante lo scontro; il `CombatContext` non li tocca mai.
- Nessuna modifica non necessaria a `pom.xml` oltre alla dipendenza di test concordata.

**Fuori scope (v1)**
- Abilità di classe, magie, effetti di stato attivi, combattimento a distanza, terreno/meteo con effetti reali, IA tattica, attacchi speciali, combattimenti multi-partecipante.
- Persistenza, ricompense, evoluzione dei personaggi nel tempo.
- Modalità batch con statistiche aggregate: rinviata a v1.5 (il `CombatLogger` astratto la predispone).
- Uso effettivo dello scudo (previsto come `ArmourResult` opzionale ma non esercitato dai due spadaccini v1).
- Qualsiasi modifica al `fantasytoolkit`.

**Definition of done** — criteri verificabili (ognuno coperto da almeno un test, tranne il n.10 verificato in revisione)
1. Un `Fighter` costruito dai generatori del toolkit espone Offensive/Defensive Rating deterministici e **immutabili**: mutare lo stato di combattimento non li altera.
2. `MaxHealth` e `MaxStamina` sono derivati dalle caratteristiche secondo le formule indicate.
3. **Purezza del core**: una procedura di calcolo (es. `HitResolver.resolveHit`, `DefenseResolver.resolveDefense`, `DamageCalculator`), dati gli stessi `DiceThrow` in input, produce sempre lo stesso esito (funzione pura, senza lanci interni). Nota: un duello intero deterministico resta ottenibile nei test fornendo una sequenza di dadi finta/truccata (stub di `DiceRoller`), senza bisogno di seme.
4. Terminazione: ogni scontro finisce o per sconfitta (`health <= 0`) o per timeout entro il tetto di turni; mai loop infinito.
5. Momentum resta sempre in `[-100, +100]` e il suo effetto su danno/difesa è limitato a ±15%.
6. La Stamina non cala per il solo passare del turno; cala per azioni e impatto; sotto le soglie si applicano le penalità progressive; con stamina insufficiente l'azione avviene con penalità e senza andare sotto 0.
7. Con `CombatContext` vuoto (v1) i Rating non cambiano e l'esito è invariante rispetto all'assenza di context.
8. La difesa riuscita (schivata/parata) riduce o annulla il danno e aggiorna il momentum secondo le regole.
9. `CombatResult` riporta vincitore/esito (inclusa la decisione ai punti su timeout) e numero di turni coerenti con lo svolgimento.
10. Complessità di lettura minima e separazione core/shell: `Main` invoca solo `Arena`; `Arena` e gli orchestratori non contengono formule/matematica né lanci di dadi nel core, che risiedono solo nel rispettivo strato (verificato in revisione).

**Decisioni risolte** (già confermate dall'utente)
- **A — JUnit 5:** RISOLTO. Aggiunta di JUnit 5 in scope `test`.
- **C — Modello di casualità:** RISOLTO (riformulato). Pattern *functional core / imperative shell*: i dadi si lanciano nello shell via `DiceRoller` (facade su `DiceLauncherTool`) e i `DiceThrow` sono passati come input ai resolver puri. `DiceLauncherTool` ammesso solo nello shell; il `RandomGenerator` seedabile è al più una sorgente alternativa dietro `DiceRoller`, non il meccanismo primario.
- **D — Esito del timeout:** RISOLTO. Vittoria ai punti sulla percentuale di Health; `DRAW` solo a parità esatta.
- **F — Nome "Momentum":** RISOLTO. Nome mantenuto.
- **G — Stamina insufficiente:** RISOLTO. Azione consentita con penalità di affaticamento e floor a 0.
- **H — Scudo:** RISOLTO. Modellato come `ArmourResult` opzionale (`Armour.SHIELD`); non esercitato in v1.
- **E — Modalità batch:** RISOLTO. Rinviata a v1.5.
- **Naming:** RISOLTO. Identificatori in inglese, commenti/Javadoc/output in italiano; value object dei dadi `DiceThrow` per evitare il clash col `DiceRoll` del toolkit.
- **B — Pesi delle formule:** APERTO ma non bloccante. I coefficienti sono provvisori; taratura empirica durante l'implementazione.

**Rischi e assunzioni**
- Assunzione: le firme del toolkit sopra elencate sono quelle documentate e stabili per questo `1.0-SNAPSHOT`. Rischio `SNAPSHOT`: l'API può cambiare; verificare la doc `docs/agent/` del toolkit prima dell'implementazione.
- Rischio bilanciamento: la generazione dei combattenti resta non riproducibile (RNG del toolkit non seedabile); mitigabile costruendo i `Fighter` una volta e riusandoli. La riproducibilità del duello nei test si ottiene stubbando `DiceRoller` con una sequenza di `DiceThrow` prefissata.
- Lacuna documentale possibile: se manca la firma di un generatore (es. builder di `ArmourResult`/scudo) va segnalata, non desunta dai sorgenti.

**Esempio** (istanza concreta — solo illustrativo)
```java
// Main: nessuna logica, invoca solo Arena.
public static void main(String[] args) {
    new Arena().run();
}

// Arena (shell): orchestrazione parlante, nessuna formula. Delega a factory e motore.
public void run() {
    Fighter first = fighterFactory.createSwordWarrior();
    Fighter second = fighterFactory.createSwordWarrior();
    CombatResult outcome = combatEngine.fight(first, second, CombatContext.empty());
    logger.reportOutcome(outcome);
}

// TurnOrchestrator (shell): lancia i dadi e li passa ai resolver puri (core).
DiceThrow attackThrow = diceRoller.d20();
DiceThrow defenseThrow = diceRoller.d20();
HitOutcome hitOutcome = hitResolver.resolveHit(attacker, defender, attackThrow);
DefenseOutcome defenseOutcome = defenseResolver.resolveDefense(defender, attacker, defenseThrow);
// ... applicazione danno, aggiornamento stamina/momentum, cambio turno.

// DiceRoller (shell): unico punto che tocca DiceLauncherTool; restituisce DiceThrow tipizzati.
public DiceThrow d20() {
    DiceRollResult result = DiceLauncherTool.building().dice(1, 20).roll();
    return new DiceThrow(result.total(), 20);
}

switch (outcome.result()) {
    case VICTORY -> System.out.println("Vince: " + outcome.winner().orElseThrow().name());
    case TIMEOUT_DECISION -> System.out.println("Timeout ai punti: " + outcome.winner().orElseThrow().name());
    case DRAW -> System.out.println("Pareggio dopo " + outcome.rounds() + " turni");
}
```
