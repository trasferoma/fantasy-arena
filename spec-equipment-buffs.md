# SPEC — Buff dell'equipaggiamento e caratteristiche efficaci

**Obiettivo:** far pesare davvero in combattimento i buff dell'equipaggiamento, introducendo nel motore la lettura delle *caratteristiche efficaci* (valore naturale + buff − debuff dell'equipaggiamento indossato, risolti al momento della lettura) e dando al loot di fine livello il 20% di probabilità di nascere buffato; il gioiello diventa a sua volta un pezzo che buffa, e smette di valere punti caratteristica permanenti.

Il compito attraversa due repository: `fantasy-combat-system` (il motore, dipendenza Maven SNAPSHOT locale) e `fantasy-arena` (il gioco). Il motore va prima, e va installato (`mvn install`) prima che il gioco possa compilare.

## Contesto

**Motore — `C:/build/git/fantasy-combat-system`**

- Punti del codice interessati:
  - `it.fantasycombatsystem.model.Characteristics` — helper statico con l'unico metodo `valueOf(CharacterResult, Characteristic)`. È il collo di bottiglia di **tutte** le letture di statistica del motore: 15 call site, nessun resolver legge `character.characteristics()` per conto proprio.
  - `rating.DefaultRatingStrategy` — STRENGTH e AGILITY (Rating offensivo), RESISTANCE e AGILITY (Rating difensivo), RESISTANCE e STAMINA (`maxHealth`), STAMINA (`maxStamina`).
  - `engine.InitiativeResolver` — AGILITY, INTELLIGENCE; `engine.HitResolver` — AGILITY di attaccante e difensore, LUCK dell'attaccante; `engine.DefenseResolver` — AGILITY di difensore e attaccante; `engine.PowerStrikeResolver` — INTELLIGENCE dell'attaccante.
  - `factory.FighterAssembler` — tre `assemble(...)`: `(character, weapon, armour)`, `(character, weapon, armour, shield)`, `(character, weapon, List<ArmourResult>, shield)`. Calcola i Rating con `ratingStrategy.computeRatings(character, weapon, armourPieces, shield)` e costruisce `new Fighter(...)`. **Nessun overload accetta gioielli.**
  - `model.Fighter` — aggregato di dati (`character, weapon, armourPieces, shield, ratings, state`), nessuna formula; i Rating sono congelati alla costruzione. Ha due costruttori: quello canonico su `List<ArmourResult>` e uno di comodo a pezzo singolo.
  - `rating.RatingStrategy` — interfaccia **pubblica**: `computeRatings(CharacterResult, WeaponResult, List<ArmourResult>, ArmourResult)`.
  - `testsupport.CombatFixtures` — pubblicata come `test-jar` e consumata anche dal gioco in scope test: `createFighter(...)`, `createArmouredFighter(...)`, `createWarrior(...)`, `createSword(...)`, `createChestplate(...)`, `createArmourPiece(...)`.
- Pattern da riusare: la somma della difesa dei pezzi indossati in `DefaultRatingStrategy.wornArmourDefense`, dichiarata nel suo Javadoc «estrazione del dato, non bilanciamento» — è il precedente che autorizza a mettere la somma dei buff nel `model` invece che in `CombatFormulas`. Determinismo dei test con `StubDiceRoller`/`RecordingStubDiceRoller`. Validazione dei parametri solo sulla superficie d'ingresso (`CombatSystem`, `FighterAssembler`).
- Test d'equivalenza da non rompere: `BattleEngineDuelEquivalenceTest`, `CombatSystemTest`, `ResolverPurityTest`.

**Gioco — `C:/build/git/fantasy-arena`**

- `combat.factory.FighterFactory` (4 spazi d'indentazione, eccezione storica) — unico punto di contatto coi generatori del toolkit. Chiama `.noStatusEffect()` su tutti e cinque i punti di generazione (`generateWeapon`, `generateLootWeapon`, `generateArmour`, `generateLootArmour`, `generateJewel`, `generateArmourPiece`). Ha già `private final Random random = new Random()` e già usa `DiceLauncherTool.building().dice(1, 5).roll()` in `pickMeleeWeapon()`. `rollLoot(Rarity rarityFloor)` estrae il tipo (`LootKind`) e delega ai tre generatori. `summon(Hero)` chiama `assembler.assemble(hero.character(), hero.weapon(), hero.armourPieces(), null)` — **i gioielli non li passa**.
- `combat.hero.Hero` — immutabile, `armourBySlot` e `jewelsByType` (`EnumMap`), con `jewels()`, `jewelOfType(Jewel)`, `wearing(...)`, `withWeapon`, `withCharacter`.
- `combat.hero.HeroBrain` — tabelle di bilanciamento (`CHARACTERISTIC_POINTS_PER_VICTORY = 3`, `JEWEL_BONUS_POINTS`, `lootRarityFloor(int)`), i tre comparatori `BY_OFFENSIVE_VALUE` (attack, poi `rarity().ordinal()`), `BY_DEFENSIVE_VALUE` (defense, poi rarità), `BY_JEWEL_VALUE` (solo rarità), il metodo `jewelBonusPointsOf(JewelResult)` e il record privato `JewelDecision` che porta i `points`. `pointsToDistribute` è oggi la somma dei punti della vittoria più il bonus del gioiello. `Random` iniettabile.
- `combat.hero.HeroProgress` — resoconto in forma di dati; i record annidati `NewJewel(JewelResult, int points)` e `JewelUpgrade(JewelResult dropped, JewelResult taken, int points)` portano i punti extra del gioiello, mentre `newPiece()` è già un `Optional<ArmourResult>` nudo.
- `combat.Arena.applyEndOfFightProcedure` — chiede la soglia di rarità al cervello e il loot alla factory.
- `combat.io.render.FighterCardFormatter` — scheda a larghezza massima 36 con troncamento a `...`; caratteristiche, riga arma `(rarità) atk N`, una riga per pezzo `(rarità) def N`, VIT/STA, ATK/DEF. **Non mostra i gioielli.**
- `combat.io.render.HeroProgressFormatter` — racconta la procedura di fine scontro leggendo `HeroProgress`; le due righe del gioiello dichiarano i punti caratteristica che vale.
- Test del gioco che dipendono dal `test-jar`: `HeroTest`, `HeroBrainTest`, `HeroProgressFormatterTest`, `ConsoleBattleLoggerTest`, `ConsoleCombatLoggerOutcomeTest`, `CombatScreenRendererTest`. Usano solo le factory di `CombatFixtures`, **mai `new Fighter(...)`**: mantenendo stabili quelle firme, il gioco non subisce rotture a cascata.
- Test che asseriscono i punti bonus del gioiello, e che questa modifica tocca per forza (verificato): `HeroBrainTest` righe 126, 131-132, 142, 152, più l'intestazione di classe; `HeroProgressFormatterTest` righe 110-116 e 126-131.

**Toolkit (fonte: doc indicizzata da `C:/build/git/fantasy-game-toolkit/docs/agent/INDEX.md`)**

- `WeaponResult(Weapon, Rarity, List<BuffElement> buffs, List<DebuffElement> debuffs, int attack)`, `ArmourResult(Armour, Rarity, buffs, debuffs, int defense)`, `JewelResult(Jewel, Rarity, buffs, debuffs)`.
- `BuffElement(Characteristic, int value)` e `DebuffElement(Characteristic, int value)`, entrambi `StatusEffect { characteristic(); value(); }`, package **`it.fantasytoolkit.buffdebuffgenerator.result`** (verificato: `.../buffdebuffgenerator/result/BuffElement.java`; il modulo è documentato come `it.fantasytoolkit.buffdebuffgenerator`, i result stanno nel suo sotto-package `result` come per gli altri tool).
- `Characteristic`: `STRENGTH, INTELLIGENCE, AGILITY, CHARISMA, RESISTANCE, STAMINA, LUCK`.
- I generatori hanno l'opzionale `noStatusEffect()`, interruttore binario che azzera buff **e** debuff. **Il default è CON buff**; non esiste alcun parametro di probabilità.
- Quando i buff si generano ce n'è **sempre almeno uno**, per qualunque rarità. Tabella `(quanti, min, max)`: COMMON `(1,1,2)`; UNCOMMON `(1,3,4)`/`(2,1,2)`; RARE `(1,5,6)`/`(2,3,4)`/`(3,1,2)`; EPIC fino a `(4,1,2)` o `(1,7,8)`; LEGENDARY fino a `(5,1,2)` o `(1,9,10)`.
- I `debuffs` sono **sempre `List.of()`**: lacuna dichiarata del toolkit. Nessun test può oggi costruirne uno *via generatore* (via costruttore del record sì).
- `DiceLauncherTool.building().dice(numberOfDice, numberOfFaces).roll()` → `DiceRollResult(rolls, total)`. **Nessun seme né `Random` iniettabile**: il dado del toolkit non è pilotabile dai test.

## Comportamento atteso

**Motore — le caratteristiche efficaci si risolvono alla lettura**

- Il valore di una statistica è `valore naturale + somma dei buff − somma dei debuff dell'equipaggiamento indossato`, calcolato **nel momento in cui la statistica viene letta**. Non esiste alcun totale pre-risolto e congelato.
- Esiste il tipo `it.fantasycombatsystem.model.EffectiveCharacteristics`, che **non è una mappa di numeri già risolti** ma una *vista* sul personaggio e sul suo equipaggiamento: custodisce i riferimenti a `CharacterResult`, arma, pezzi d'armatura, scudo e gioielli, non dei totali. `valueOf(Characteristic)` fa la somma al momento della chiamata, e applica dentro di sé il pavimento `MIN_EFFECTIVE_VALUE = 1`.
- **Perché alla lettura e non all'assemblaggio.** Nel disegno alternativo il `Fighter` riceveva le efficaci come parametro del costruttore, accanto all'equipaggiamento: due parametri tenuti a concordare fra loro, senza nulla che impedisse di costruire un `Fighter` in cui **non** concordano. Era un invariante mantenuto per disciplina. Risolvendo alla lettura quello stato non è più rappresentabile, e in più il requisito «un gioiello rimosso perde il suo buff come se non fosse mai stato indossato» diventa vero per costruzione invece di essere una cosa da ricordarsi.
- **Un solo algoritmo, due punti di costruzione, nessun passaggio di parametro.** Il `Fighter` si costruisce la propria `EffectiveCharacteristics` **da sé nel costruttore**, dall'equipaggiamento che ha appena ricevuto: nessuno può passargliene una incoerente. `DefaultRatingStrategy` se ne costruisce una dall'equipaggiamento che riceve lui, perché gira prima che il `Fighter` esista. Stessa classe, stessa somma, due siti di costruzione e nessun dato che viaggia fra i due.
- Più pezzi che buffano la stessa caratteristica **si sommano**. I debuff si sottraggono, anche se oggi il toolkit non li produce: la lettura è pronta per il giorno in cui li produrrà.
- Il valore efficace non scende sotto `1`: una caratteristica azzerata o negativa renderebbe degeneri i rapporti e le differenze su cui lavorano le formule, e un combattente in campo ha per definizione almeno un punto in ogni caratteristica. Il pavimento vale solo verso il basso: nessun tetto verso l'alto (vedi *Rischi noti*).
- **Ogni** calcolo del motore che usa il valore di una statistica usa le efficaci: Rating offensivo e difensivo, `maxHealth`, `maxStamina`, iniziativa, probabilità di colpire, di critico, di schivare, decisione del colpo potente. Il danno e la probabilità di parata ne beneficiano di conseguenza, perché derivano dai Rating. I resolver leggono `fighter.effectiveCharacteristics().valueOf(...)`.
- `Characteristics.valueOf` **resta**, come accesso dichiarato al valore **naturale**, ed è usato dalla vista. Nessun resolver lo chiama più.
- Il `Fighter` custodisce i gioielli indossati (`jewels()`, lista eventualmente vuota, immutabile), il `FighterAssembler` ha un overload che li accetta e **li inoltra** senza risolvere niente, e i loro buff pesano su Rating e resolver come quelli di arma e armatura. I gioielli non entrano in nessuna formula per conto proprio: non hanno né attacco né difesa, contano solo per i buff.
- Gli overload preesistenti di `assemble(...)` continuano a esistere e assemblano combattenti senza gioielli.
- I Rating restano congelati alla costruzione, come oggi: è la lettura delle statistiche a diventare dinamica, non il ricalcolo dei Rating durante lo scontro.

**Gioco — il loot buffato al 20%, e il gioiello che conta una volta sola**

- Il loot di fine livello ha il 20% di probabilità di nascere con buff. La probabilità è un numero di **bilanciamento** e vive in `HeroBrain` come percentuale (`buffedLootChancePercent()`), accanto a `lootRarityFloor`; il **tiro** vive in `FighterFactory`, che è dove sta la casualità di generazione, e si fa col `DiceLauncherTool` del toolkit. `HeroBrain` non sa nulla del numero di facce del dado.
- `Arena` passa alla factory la percentuale ricevuta dal cervello insieme alla soglia di rarità: continua a scandire, non decide.
- Quando il tiro dice «buffato», il generatore viene invocato **senza** `noStatusEffect()` e l'oggetto porta almeno un buff (garanzia del toolkit); quando dice «liscio», `noStatusEffect()` resta. Vale identico per tutti e tre i tipi di loot.
- Equipaggiamento di partenza (protagonista compreso), sfidanti di ogni round e specchio finale restano **sempre** lisci: si parte tutti alla pari, le differenze le conquista il protagonista dal loot.
- `FighterFactory.summon(Hero)` passa i gioielli indossati all'assemblatore: il combattente del round li porta addosso e le sue caratteristiche efficaci ne tengono conto.
- **Il gioiello conta una volta sola.** Contribuisce **solo** coi suoi buff, esattamente come arma e armatura: mentre lo indossa il buff c'è, se lo sostituisce il buff se ne va come se non l'avesse mai indossato. Il canale dei punti caratteristica permanenti per rarità viene **rimosso**, non ridotto:
  - `HeroBrain` perde la costante `JEWEL_BONUS_POINTS` e il metodo `jewelBonusPointsOf(JewelResult)`; `CHARACTERISTIC_POINTS_PER_VICTORY = 3` resta ed è l'unica fonte di punti, quindi la vittoria vale sempre esattamente tre punti e `pointsToDistribute` non è più una somma;
  - il record privato `JewelDecision` perde il parametro `points` dalle factory `wearing`/`replacing` e il metodo `bonusPoints()`;
  - `HeroProgress` perde il campo `points` dai record del gioiello: `JewelUpgrade` resta con `dropped` e `taken` (simmetrico ad `ArmourUpgrade`), mentre `NewJewel` sparisce e `newJewel()` diventa un `Optional<JewelResult>` nudo, simmetrico a `newPiece()`.
- I comparatori di cernita di `HeroBrain` pesano il buff. Criterio dichiarato, semplice e ritarabile in un punto solo: il **buff netto** di un oggetto è la somma dei valori dei suoi buff meno quella dei suoi debuff, e vale `BUFF_POINT_WEIGHT` punti di attacco o difesa (default `1`: un punto di buff vale come un punto di attacco).
  - arma: `attack + BUFF_POINT_WEIGHT × buffNetto`, poi la rarità come spareggio;
  - armatura: `defense + BUFF_POINT_WEIGHT × buffNetto`, poi la rarità come spareggio;
  - gioiello: `buffNetto` come criterio, la **rarità come spareggio** (prima era il criterio). Fra due gioielli senza buff l'ordine resta quello di oggi.
  - A parità di valore complessivo il protagonista tiene il suo, come oggi, in tutte e tre le categorie.
- La scheda del combattente (`FighterCardFormatter`) mostra la caratteristica **efficace** come numero principale, col contributo dell'equipaggiamento fra parentesi quando c'è (`STRENGTH 12 (+2)`), e una riga per ogni gioiello indossato. Arma, pezzi e gioielli portano il buff netto in coda alla loro riga (`+7`) quando ne hanno. Il renderer **legge** le efficaci dal `Fighter`, non le ricalcola: la regola è del motore.
- Il racconto di fine scontro (`HeroProgressFormatter`) dice il buff dell'oggetto trovato, caratteristica per caratteristica (`SWORD (RARE, atk 8, +5 STRENGTH)`), per tutti e tre i tipi di loot, e non parla più di punti caratteristica extra per il gioiello: è il buff l'informazione che spiega perché un oggetto è stato preso o scartato, e senza di essa una scelta contro-intuitiva del cervello sembrerebbe un bug.
- Invariante: senza buff e senza gioielli, scheda, racconto e scelte del cervello restano identici a oggi. L'unica differenza voluta è che un gioiello preso non gonfia più il monte punti della vittoria.

## Vincoli

- Retrocompatibile dove possibile: gli overload esistenti di `FighterAssembler.assemble(...)` restano e continuano ad assemblare senza gioielli.
- **Breaking change dichiarati e consapevoli** sulla superficie pubblica del motore, dimezzati dalla scelta di risolvere alla lettura:
  - `RatingStrategy.computeRatings` guadagna **solo i gioielli**: `computeRatings(CharacterResult, WeaponResult, List<ArmourResult>, ArmourResult shield, List<JewelResult> jewels)`. Non riceve alcuna caratteristica pre-risolta: se ne costruisce la vista da sé, perché gira prima che il `Fighter` esista.
  - Il costruttore canonico di `Fighter` guadagna **solo i gioielli**. Il costruttore di comodo a pezzo singolo **sopravvive** con la firma di oggi, delegando con lista di gioielli vuota (vedi *Decisioni*): un breaking change in meno, e `CombatFixtures` non ha bisogno di cambiare le proprie firme.
- Il gioco **non** ricalcola le caratteristiche efficaci: le legge dal `Fighter`. L'unica somma che il gioco fa è il buff netto scalare del suo criterio di cernita, che è bilanciamento di progressione e non regola di combattimento.
- Niente regole di combattimento nel gioco, niente presentazione né I/O nel motore. Nel motore le formule restano in `CombatFormulas`: la somma dei buff non è una formula di bilanciamento ma estrazione del dato, e sta nel `model` sullo stampo di `wornArmourDefense`. Vale anche per il Javadoc di `Fighter`, che si dichiara «aggregato di dati: non contiene formule»: un accessor che somma i buff non viola quel principio, perché in quella somma non c'è alcun peso tarabile — i pesi restano tutti in `CombatSettings`. Il Javadoc va aggiornato per dirlo, invece di lasciare al lettore il dubbio.
- I renderer restano puri (righe di testo, nessun I/O). `render` non importa dagli altri sotto-package di `combat.io`.
- La casualità: nel gioco il dado del loot è `DiceLauncherTool` in `FighterFactory` (mai `Math.random()`, mai `new Random()` fuori dalle deroghe già dichiarate); nel motore resta il `DiceRoller`.
- Java 21, Maven, indentazione 2 spazi (`FighterFactory` resta a 4). Javadoc e commenti in italiano, nomi di codice in inglese. Niente sealed type, text block, pattern matching, preview feature.
- Test JUnit Jupiter con `org.junit.jupiter.api.Assertions`, **niente AssertJ**, nessuna nuova dipendenza in nessuno dei due repository.
- Le firme delle factory esistenti di `CombatFixtures` non cambiano: si aggiungono overload. Il `test-jar` va reinstallato insieme al jar.
- Nessuna modifica a `CombatSettings` (nessun peso nuovo), al modello dati del toolkit, ad altri percorsi di presentazione.

## Decisioni di progettazione sciolte in questa SPEC

1. **Risoluzione alla lettura, non all'assemblaggio** (decisione dell'utente, motivata sopra): `EffectiveCharacteristics` è una vista sull'equipaggiamento, non una mappa di totali. Elimina la possibilità di rappresentare uno stato incoerente e rende automatico il fatto che togliersi un pezzo ne cancelli il buff.
2. **Nessun passaggio di parametro fra i due punti di costruzione**: se il `Fighter` ricevesse la vista da fuori tornerebbe il problema dei due parametri da tenere d'accordo. Se la costruisce da sé, l'unico modo di avere una vista sbagliata è avere l'equipaggiamento sbagliato — cioè nessun modo in più rispetto a oggi.
3. **Accesso dai resolver**: `fighter.effectiveCharacteristics().valueOf(AGILITY)`, non un `fighter.characteristic(AGILITY)` più corto. Il nome lungo dichiara al punto d'uso che il valore è quello *efficace*; un accessor ambiguo inviterebbe a rileggere i valori naturali per sbaglio.
4. **`Characteristics.valueOf` resta pubblica**, ridefinita nel Javadoc come lettura del valore **naturale**, e diventa un dettaglio usato dalla vista. Non sparisce: i valori naturali servono comunque (la scheda mostra il delta, e ha senso poter chiedere «quanto vale senza equipaggiamento»).
5. **Il costruttore di comodo a pezzo singolo di `Fighter` si conserva**, con la stessa firma di oggi, ridefinito nel Javadoc come «un pezzo solo e nessun gioiello» e delegando al canonico con `List.of()`. Motivazione: ora che il canonico guadagna un solo parametro, tenerlo costa una riga e vale un breaking change in meno; il caso «un pezzo, nessun gioiello» è quello storicamente più comune ed è esattamente ciò che serve alle fixture; e `CombatFixtures.createFighter` continua a compilare senza toccarne le firme, il che protegge a cascata sei classi di test del gioco. Il rischio dell'assenza implicita di gioielli è reale ma piccolo, ed è confinato a un costruttore che nel percorso di produzione non viene usato: chi assembla combattenti passa dal `FighterAssembler`.
6. **`NewJewel` sparisce**: rimasto con un solo campo dopo la rimozione dei punti, sarebbe un involucro che non aggiunge informazione. `newJewel()` diventa `Optional<JewelResult>`, simmetrico a `newPiece()` che è già un `Optional<ArmourResult>` nudo. `JewelUpgrade` invece resta, perché ha due campi e specchia `ArmourUpgrade`: la simmetria fra il ramo armatura e il ramo gioiello si conserva su entrambi i lati.
7. **Somma e pavimento**: buff sommati, debuff sottratti, minimo `1`, applicato dentro `valueOf`. Nessun tetto.
8. **Scheda**: efficace come numero principale + delta fra parentesi, e i gioielli finalmente mostrati. Ora che i gioielli pesano sui Rating, non mostrarli renderebbe ATK/DEF inspiegabili. Il budget di 36 caratteri resta: nei casi estremi (arma LEGENDARY con buff alto) la riga tronca come già succede oggi, e alzare `MAX_WIDTH` è un intervento separato da valutare a schermo, non un requisito di questa SPEC.
9. **Comparatori**: criterio scalare `attacco/difesa + peso × buff netto`, con `BUFF_POINT_WEIGHT` come unica manopola. Non pretende di essere giusto — pretende di essere dichiarato e ritarabile in un punto solo.
10. **Testabilità del 20%**: `rollLoot` riceve la percentuale come parametro, quindi con `0` e `100` i due bordi diventano **deterministici e verificabili** senza pilotare il dado. Resta non deterministico il valore intermedio: che su 200 estrazioni al 20% compaiano sia oggetti lisci sia buffati è una verifica statistica, sullo stampo di quelle già presenti in `FighterFactoryTest`, non una prova del tasso esatto. Nel motore invece tutto è deterministico: i buff nei test si costruiscono col costruttore dei record del toolkit, non col generatore.

## Rischi noti e accettati

- **Costo del ricalcolo a ogni lettura**: si scorrono da tre a sei pezzi con pochi buff ciascuno, per circa dieci-quindici letture di statistica per turno. In un gioco a console non è misurabile, e il costo è **accettato**. Se un giorno lo diventasse, la memoizzazione va **dentro** `EffectiveCharacteristics`, trasparente ai chiamanti: l'equipaggiamento del `Fighter` è `final`, quindi un valore memoizzato non può invecchiare. Non si implementa adesso: sarebbe complessità senza una misura che la giustifichi.
- **Sbilanciamento del loot buffato**: un `LEGENDARY` con combinazione `(3,5,6)` porta fino a 18 punti caratteristica, più dei 15 con cui il protagonista nasce. Il rischio è stato segnalato e **accettato**: nessun tetto all'entità del buff, si prende quello che dà il toolkit. Il punto di ritaratura è `HeroBrain` (probabilità, soglia di rarità, peso del buff nei comparatori), non il motore e non il toolkit.
- **La crescita permanente del protagonista rallenta** rispetto a oggi, perché il gioiello non aggiunge più punti: un protagonista che prende tre gioielli guadagna oggi fino a 9 punti permanenti in più di quanti ne guadagnerà dopo la modifica, compensati però dal buff finché li indossa. È l'effetto voluto della rimozione del doppio canale; la manopola per correggerlo, se servirà, è `CHARACTERISTIC_POINTS_PER_VICTORY`.
- **Breaking change** sulla superficie pubblica del motore: fino al `mvn install` del motore, `fantasy-arena` non compila. È una condizione attesa della sequenza di lavoro, non un errore.

## Fuori scope

- Memoizzazione delle caratteristiche efficaci: prevista come evoluzione possibile, non da fare ora.
- Debuff generati (lacuna del toolkit): la lettura li sottrae, ma nessuno li produce.
- Scudo e pozioni come slot di gioco; buff a tempo, consumabili, effetti che cambiano durante lo scontro.
- Ricalcolo dei Rating durante lo scontro: restano congelati alla costruzione del `Fighter`.
- Nuovi pesi in `CombatSettings`, riequilibrio dei pesi esistenti, tetti o curve sull'entità del buff, ritaratura di `CHARACTERISTIC_POINTS_PER_VICTORY`.
- Buff sull'equipaggiamento di partenza, degli sfidanti o dello specchio finale.
- Scelte del protagonista guidate dall'utente; salvataggio della progressione.
- Rifacimento del layout della scheda oltre a quanto serve per mostrare efficaci e gioielli.

## Definition of done

Criteri verificabili, ognuno coperto da almeno un test (l'ultimo in revisione).

1. Il valore efficace di una caratteristica è naturale + somma dei buff − somma dei debuff dell'equipaggiamento indossato, risolto alla lettura; più fonti sulla stessa caratteristica si sommano; il risultato non scende sotto 1.
2. Un pezzo non più indossato non lascia traccia: lo stesso personaggio senza quel gioiello legge il valore efficace di prima.
3. Rating offensivo, Rating difensivo, `maxHealth` e `maxStamina` sono calcolati sulle efficaci.
4. Iniziativa, probabilità di colpire, di critico, di schivare e decisione del colpo potente leggono le efficaci (AGILITY, INTELLIGENCE, LUCK).
5. `FighterAssembler` accetta e inoltra i gioielli, il `Fighter` li custodisce e si costruisce da sé la vista; gli overload preesistenti e il costruttore di comodo assemblano senza gioielli.
6. Con equipaggiamento senza buff né debuff il motore si comporta esattamente come oggi, dado per dado (test d'equivalenza verdi).
7. `rollLoot` con probabilità 0 non produce mai un oggetto buffato; con 100 lo produce sempre buffato con almeno un buff, per tutti e tre i tipi; la soglia di rarità resta rispettata in entrambi i casi.
8. La probabilità di default è 20, esposta da `HeroBrain` come percentuale, e il tiro avviene in `FighterFactory` col `DiceLauncherTool`.
9. Equipaggiamento di partenza, sfidanti e specchio finale non hanno mai buff.
10. `summon` passa i gioielli indossati: il combattente del round li porta e le sue efficaci ne tengono conto.
11. I comparatori di cernita pesano il buff netto: un'arma che colpisce meno ma buffa più viene impugnata, un gioiello meno raro ma più buffato viene indossato, a parità di valore complessivo si tiene il proprio.
12. Il gioiello non dà più punti caratteristica: la vittoria vale sempre esattamente tre punti, qualunque sia il loot e qualunque ne sia il destino.
13. La scheda mostra le caratteristiche efficaci con il contributo dell'equipaggiamento, una riga per gioiello indossato e il buff netto sulle righe di arma, pezzi e gioielli.
14. Il racconto di fine scontro riporta il buff dell'oggetto trovato, caratteristica per caratteristica, e non menziona punti extra per il gioiello.
15. Senza buff e senza gioielli scheda, racconto e scelte del cervello restano identici a oggi.
16. `CLAUDE.md` di entrambi i repository, `daImplementare.md` del gioco, `combatSettings.md` e `README.md` del motore non affermano più che il motore non monta i gioielli, che le formule leggono le caratteristiche di base, che il gioiello vale punti caratteristica extra o che per il gioiello il criterio è la rarità «perché è l'unico numero che ha» (verificato in revisione, non con un test).

## Esempio (istanza concreta — solo illustrativo)

```java
// MOTORE — nessuno passa le efficaci a nessuno: chi ne ha bisogno se le costruisce
// dall'equipaggiamento che ha già in mano.
public Fighter assemble(CharacterResult character, WeaponResult weapon, List<ArmourResult> armourPieces,
    ArmourResult shield, List<JewelResult> jewels) {
  validateEquipment(character, weapon, armourPieces, jewels);
  IntrinsicRatings ratings = ratingStrategy.computeRatings(character, weapon, armourPieces, shield, jewels);
  return new Fighter(character, weapon, armourPieces, shield, jewels, ratings);
}

// Fighter: la vista nasce nel costruttore, dall'equipaggiamento appena ricevuto. Uno stato in cui
// le efficaci non concordano con l'equipaggiamento non è rappresentabile.
this.effectiveCharacteristics =
    new EffectiveCharacteristics(character, weapon, this.armourPieces, shield, this.jewels);

// EffectiveCharacteristics: custodisce riferimenti, non totali. La somma avviene qui, a ogni lettura.
public int valueOf(Characteristic characteristic) {
  int natural = Characteristics.valueOf(character, characteristic);
  int modifiers = wornItems().stream()
      .mapToInt(item -> netEffectOn(item, characteristic))
      .sum();
  return Math.max(MIN_EFFECTIVE_VALUE, natural + modifiers);
}

// I resolver dichiarano al punto d'uso che stanno leggendo le efficaci.
int attackerAgility = attacker.effectiveCharacteristics().valueOf(Characteristic.AGILITY);

// GIOCO — la percentuale è bilanciamento (HeroBrain), il dado è generazione (FighterFactory).
// HeroBrain
private static final int BUFFED_LOOT_CHANCE_PERCENT = 20;   // nessun accenno alle facce del dado

public int buffedLootChancePercent() {
  return BUFFED_LOOT_CHANCE_PERCENT;
}

// FighterFactory (4 spazi: eccezione storica del file)
private static final int BUFF_ROLL_DICE_FACES = 100;

public Loot rollLoot(Rarity rarityFloor, int buffChancePercent) {
    boolean buffed = rollsWithinChance(buffChancePercent);
    LootKind kind = LootKind.values()[random.nextInt(LootKind.values().length)];
    return switch (kind) {
        case WEAPON -> Loot.ofWeapon(generateLootWeapon(rarityFloor, buffed));
        case ARMOUR -> Loot.ofArmourPiece(generateLootArmour(rarityFloor, buffed));
        case JEWEL -> Loot.ofJewel(generateJewel(rarityFloor, buffed));
    };
}

/** Con 0 non buffa mai, con 100 buffa sempre: i due bordi restano verificabili senza pilotare il dado. */
private boolean rollsWithinChance(int chancePercent) {
    DiceRollResult roll = DiceLauncherTool.building()
            .dice(1, BUFF_ROLL_DICE_FACES)
            .roll();
    return roll.total() <= chancePercent;
}

// HeroBrain — il buff entra nella cernita con un peso solo, e il gioiello non vale più punti:
// i tre punti della vittoria sono l'unica fonte.
private static final int BUFF_POINT_WEIGHT = 1;

private static final Comparator<WeaponResult> BY_OFFENSIVE_VALUE = Comparator
    .comparingInt(weapon -> weapon.attack() + BUFF_POINT_WEIGHT * netStatusEffectValue(weapon.buffs(), weapon.debuffs()))
    .thenComparingInt(weapon -> weapon.rarity().ordinal());

List<CharacteristicGain> characteristicGains =
    distributeCharacteristicPoints(hero.character(), CHARACTERISTIC_POINTS_PER_VICTORY);
```
