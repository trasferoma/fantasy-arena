# IMPLEMENTATION — Bilanciamento della progressione

**Specifica di riferimento:** `spec-bilanciamento-progressione.md`  — nel resto del documento: «la
SPEC».
**Stato:** `COMPLETED`  <!-- NOT_STARTED | IN_PROGRESS | BLOCKED | COMPLETED -->

Documento di lavoro: la SPEC (il "cosa") resta stabile; qui vivono stato, piano, decisioni e problemi
(il "come").

## Regole per l'agente

- Leggere `CLAUDE.md` e la SPEC prima di toccare codice.
- Alla ripresa del lavoro, leggere prima questo file e riprendere dallo stato corrente.
- Prima di modificare, elencare i file che verranno toccati. Nessun refactoring fuori scope.
- Non modificare i requisiti della SPEC senza decisione esplicita.
- Dopo ogni fase: eseguire i test pertinenti e aggiornare questo file. Spuntare una voce solo dopo
  verifica reale, mai a priori. Vale anche per le fasi senza codice (analisi, documentazione, misura).
- Scelta che **non** cambia il comportamento osservabile → procedi e annotala in *Decisioni*.
- Scelta che **cambia** comportamento o criteri di accettazione, o ambiguità non risolvibile dalla
  SPEC → **fermati**, imposta lo stato a `BLOCKED` e registra in *Problemi aperti* / *Deviazioni*.
- Le due voci di *Da decidere* della SPEC sono state **risolte dall'utente il 2026-08-02** (vedi
  *Registro · Decisioni*): fortuna **effettiva**, calcolo in un collaboratore dedicato accanto a
  `TrialPlan`. La Fase 3 non è più bloccata.
- Il lavoro parte dall'albero di lavoro com'è ora, con le modifiche non committate di
  `bonus-equipaggiamento` già dentro: le caratteristiche effettive e i buff dell'equipaggiamento sono
  un presupposto, non qualcosa da rifare.
- Non lanciare la modalità web a mano senza `mvn process-resources` e senza fermare la JVM
  precedente: sono le due trappole di `CLAUDE.md`. Usare `start-web.ps1`/`stop-web.ps1`.

## Piano operativo

**Fase 1 — Analisi**

- [x] Pavimento di **7 punti per sfidante confermato**: la documentazione del toolkit dà
      `minCharacteristicValue` a `1` per difetto e sette `Characteristic`, e una ricerca su tutto
      `src` non trova **nessuna** chiamata a `minCharacteristicValue` — il difetto vale ovunque.
      `generate()` solleva `IllegalStateException` sotto soglia.
- [x] `RarityTable.build()` pretende la somma **esattamente 100**. Le quattro tabelle nuove la
      rispettano: `65+25+8+2`, `35+40+20+5`, `45+40+15`, `20+55+25`.
- [x] La fortuna è `Characteristic.LUCK`. `CharacterResult` **non** ha accessor per singola
      caratteristica: si scorre `characteristics()`, come già fa `EquipmentBonus`. Il motore ha un
      `Characteristics.valueOf(...)` (lo usa `HitResolver`) ma è **fuori dalla superficie pubblica
      dichiarata** del motore (`CombatSystem`, `FighterAssembler` e i tipi `result`/`battle`): non va
      usato. Serve un metodo privato di lettura in `ChallengerBudget`.
- [x] `createChallengers` ha **un solo chiamante di produzione**: `Arena:184`, nel ramo `GENERATED`
      di `challengersFor`. In test lo invocano `FighterFactoryTest` (righe 57, 84, 106, 140, 145) e
      `ArenaFighterFactoryTest` (righe 162-163, con `(1, 15)` e `(2, 18)`). Il cambio di semantica ha
      quindi una sola superficie di produzione da correggere.
- [x] La trappola del pareggio è **una sola riga**: `Arena.run()` alla riga 151 dichiara il trionfo
      con `if (lastRound.passed())`, che dopo la modifica sarebbe vero anche pareggiando la decima
      prova. `RunConclusion` **non** va toccata: la riga 157 la costruisce già da
      `lastTrial.outcome()`, cioè dall'esito dell'ultima prova, che è esattamente la fonte giusta.
      `app.js` regge già una prova senza `progress` (`buildMoments` la salta con `if (trial.progress)`),
      ma `trialStationStateOf` etichetta ogni stazione precedente come «superata»: lì il terzo stato
      va aggiunto.
- [x] Stile dei test rilevato: JUnit Jupiter con sole `org.junit.jupiter.api.Assertions`, doppi
      `ScriptedFights`/`RecordingFighterFactory` in `ArenaTest`, fixture dal `test-jar` del motore.
- [x] "File coinvolti (effettivi)": l'elenco provvisorio regge. Unica correzione — `RunConclusion` non
      va toccata, per il motivo qui sopra.

**Fase 2 — Il monte punti diventa di squadra**

- [x] `TrialPlan`: i nove monti diventano `15, 18, 21, 31, 35, 39, 50, 54, 59`; il Javadoc di classe
      spiega la regola di derivazione (`monteEroe(N) = 15 + 3*(N-1)`, moltiplicatore `1.0`/`1.3`/`1.5`
      per uno, due, tre sfidanti) e dichiara che i numeri restano letterali, non calcolati a runtime.
- [x] `TrialStation`: aggiornare il Javadoc del campo, che oggi dice «monte punti caratteristica con
      cui nascono» gli sfidanti — ora è il monte dell'intero schieramento.
- [x] `FighterFactory`: rinominare il parametro di `createChallengers` (es.
      `squadCharacteristicPoints`), ripartire a parti uguali col resto ai primi, sostituire
      `validateCharacteristicPoints` con una validazione del pavimento
      `MINIMUM_CHARACTERISTIC_POINTS_PER_CHALLENGER * count` e messaggio diagnostico che nomina
      pavimento, monte ricevuto e numerosità. La costante del pavimento è **pubblica**: la userà anche
      chi calcola lo sconto (Fase 3). Mantenere lo stile a 4 spazi del file.
- [x] Verifica: criteri 1, 2, 3 della *Definition of done*. Tutti e tre coperti; `mvn test` verde
      (176 test). Riletti a valle: `TrialPlan` e `createChallengers` sono conformi, il Javadoc porta
      la regola di derivazione e la motivazione del moltiplicatore. Test da aggiornare, già letti e
      nominati:
      - `TrialPlanTest.ilMontePuntiCresceSecondoLaCurvaDichiarataESoloPerLeStazioniGenerate` —
        asserisce oggi `List.of(15, 18, 21, 24, 27, 30, 33, 36, 39)`;
      - `FighterFactoryTest.creaCinqueCombattentiConCinqueNomiDistintiEStessaRarita` — chiede
        `createChallengers(5, 15)`, che col pavimento nuovo (35) diventa un errore;
      - `FighterFactoryTest.ogniSfidanteRiceveAlmenoIlMontePuntiRichiestoPiuIBonusDiRazzaEClasse` e
        `FighterFactoryTest.gliSfidantiScendonoInCampoConLeCaratteristicheEffettiveComprensiveDeiBuffDellEquipaggiamento`
        — chiedono `createChallengers(3, 24)` e confrontano ogni sfidante con `24`, che ora è il monte
        di **squadra**;
      - `FighterFactoryTest.rifiutaUnMontePuntiMinoreDiUno` — diventa il test del pavimento;
      - `ArenaFighterFactoryTest.gliSfidantiHannoNomiDistintiDaTuttiQuelliGiaScesiInCampo` — resta
        verde (`(1, 15)` e `(2, 18)` stanno sopra il pavimento) ma va riletto: il secondo round ora
        genera due sfidanti da 9 punti ciascuno.
      - test nuovo sulla ripartizione col resto (31 su 2, 50 su 3) e sull'accettazione del pavimento
        esatto.

**Fase 3 — Lo sconto della fortuna** *(le due voci di* Da decidere *sono chiuse: vedi* Registro)

- [x] Introdurre il record `ChallengerBudget` in `it.fantasyarena.combat`, accanto a `TrialPlan`, che
      calcola il monte effettivo dello schieramento — monte della stazione, sconto applicato, monte
      effettivo. Legge la fortuna **effettiva** del protagonista (`hero.effectiveCharacter()`, coi
      buff dell'equipaggiamento addosso), la moltiplica per il numero di sfidanti e applica il
      pavimento riusando la costante pubblica di `FighterFactory`. Lo sconto va limitato **da
      entrambi i lati**: mai oltre `monte − pavimento`, mai sotto zero.
- [x] `Arena.challengersFor`: il ramo `GENERATED` chiede il monte effettivo e lo passa alla factory;
      il ramo `MIRROR` resta invariato. Lo switch resta esaustivo e senza `default`, e in `Arena` non
      entra nessun `if` di gioco.
- [x] Verifica: criteri 4, 5, 6. Test nuovi sul calcolo (fortuna che sconta, pavimento che tiene,
      sconto registrato pari a quello applicato) e su `Arena` (lo specchio non riceve né monte né
      sconto). `ArenaTest` gioca con la factory vera: verificare che i monti scontati restino sopra il
      pavimento per una fortuna plausibile, altrimenti il pavimento entra in gioco già nei test di
      scansione. Con la fortuna **effettiva** il pavimento non è un caso limite teorico ma un
      comportamento ordinario — un gioiello che porti molta `LUCK` ci arriva da solo: va coperto da un
      test che parte da una `Hero` con equipaggiamento generoso, non solo da un valore inventato.

**Fase 4 — Lo sconto entra nella cronaca**

- [x] Portare in `combat.chronicle` monte dichiarato, sconto e monte effettivo, come dato per prova
      su `TrialChronicle`, **nullable** per la stazione dello specchio (stessa forma di
      `progress`). Nessuna stringa di presentazione.
- [x] `ChronicleMapper`: unico punto di traduzione, come per gli altri snapshot. `Arena` costruisce la
      voce di cronaca passando anche questo dato. Forma scelta: record dedicato
      `ChallengerBudgetChronicle`, sul precedente di `HeroSnapshot`/`ItemSnapshot`/`ProgressChronicle`.
      In `Arena` il budget viaggia insieme agli sfidanti in un record privato `StationChallengers`,
      perché `challengersFor` lo calcolava e lo buttava via. Confine verificato: `combat.chronicle`
      importa già `it.fantasyarena.combat.RoundOutcome`, quindi importare `ChallengerBudget` dal
      package padre è un precedente esistente e non una violazione.
- [x] Aggiornare i Javadoc dei record toccati.
- [x] Verifica: criteri 7 e 8. Chiavi JSON nuove: `budget` dentro ogni voce di `trials[]` (oggetto o
      `null` per lo specchio), con `stationPoints`, `luckDiscount`, `squadPoints`. Test da aggiornare: `ChronicleJsonTest` (le chiavi nuove — è **l'unica
      rete** a protezione del contratto verso il JavaScript — più i due `new TrialChronicle(...)`
      costruiti a mano), `ArenaChronicleTest` (un `new TrialChronicle(...)` a mano),
      `ChronicleMapperTest`, `ArenaTest` sulla cronaca.

**Fase 5 — Lo sconto si legge in console**

- [x] `ArenaLogger`: portare il dato all'annuncio del round, nella forma del tipo di dominio (come già
      accade per `RoundOutcome`), non della fotografia di cronaca. Aggiornare `ConsoleArenaLogger`
      (frase nuova) e `SilentArenaLogger` (resta muto).
- [x] Non toccare `combat.io.render`: qui non c'è niente da formattare oltre a una riga.
- [x] Verifica: criterio 9, sull'output catturato come già fanno i test dei logger di console. Nuovo
      `ConsoleArenaLoggerTest` con tre casi: sconto raccontato, silenzio per lo specchio, silenzio a
      sconto zero (fortuna nulla o monte già al pavimento) — una frase che affermasse un taglio
      inesistente sarebbe peggio del silenzio.

**Fase 6 — La rarità del loot**

- [x] `HeroBrain`: i pesi nuovi delle quattro `RarityTable` (1-2: 65/25/8/2; 3-5: 35/40/20/5; 6-8:
      45/40/15; 9-10: 20/55/25). Gli scaglioni e i loro confini non cambiano. Verificato: ciascuna
      somma esattamente 100.
- [x] Riscrivere il Javadoc di `lootRarityTable` e delle quattro costanti: oggi raccontano un
      pavimento che **sale** da `UNCOMMON` a `EPIC`, mentre ora `UNCOMMON` resta estraibile fino alla
      prova 5 e il pavimento sale a `RARE` solo dalla 6. Riportare l'effetto atteso (leggendario entro
      la prova 3: dal 35% all'8,8%; sulla corsa intera: dal 93% al 72%).
- [x] Verifica: criterio 10. `ilPavimentoDellaRaritaSiAlzaAOgniScaglione` rinominato in
      `ilPavimentoDellaRaritaRestaUncommonFinoAllaProva5EPoiSaleARare` e riscritto sul comportamento
      nuovo. `laTabellaDiRaritaDelLootSeguiQuattroScaglioniSulPercorsoADieciProve` riletto e lasciato
      invariato (confronta identità di tabella, non pesi).

**Fase 7 — Il pareggio non chiude più la corsa**

- [x] `Arena.fightRound`: sostituire `if (outcome != RoundOutcome.WON)` con una lettura esaustiva dei
      tre esiti — `WON` cresce e prosegue, `STOOD_WITHOUT_WINNING` prosegue senza procedura,
      `FELL` chiude.
- [x] `RoundReport`: aggiunto `crossedTrial(Hero, TrialChronicle)` — si prosegue con la **stessa**
      scheda, non una cresciuta. `andThen` non cambia nella forma, ma `passed()` è stato **rinominato
      in `continues()`**: dopo questa modifica il nome vecchio era una bugia, perché il segnale non
      significa più «ha vinto» ma «si gioca la prova successiva», vero anche dopo un pareggio.
- [x] `Arena.run()`: il trionfo si deriva ora da `lastTrial.outcome() == RoundOutcome.WON`.
      `RunConclusion` non toccata, come previsto dalla Fase 1.
- [x] `ArenaLogger`: metodo dedicato `reportTrialCrossed(Hero, int)` per il pareggio come passaggio,
      invece di un ramo in più dentro `reportEndOfRun`. Il contratto di `reportEndOfRun` è stato
      **ristretto**: ora rifiuta sia `WON` sia `STOOD_WITHOUT_WINNING`. Motivo: la frase vecchia per
      il pareggio («senza una vittoria piena non si passa al round successivo») era diventata falsa, e
      lasciarla raggiungibile anche solo per contratto avrebbe tenuto in vita un testo scorretto.
- [x] Verifica: criteri 11, 12, 13, 14. `laCadutaAlPrimoRoundChiudeLArena` e
      `dopoLaCadutaLaCronacaSiChiudeAllaProvaPersa` sono rimasti **invariati e verdi**, come
      regressione. Test da aggiornare, già letti e nominati:
      - `ArenaTest.restareInPiediSenzaAbbattereTuttiNonApreIlRoundSuccessivo` — ora il round
        successivo si gioca: va riscritto sul comportamento nuovo;
      - `ArenaTest.dopoUnPareggioLaCronacaSiChiudeAllaProvaInCuiNonHaVintoLoScontro` — la cronaca non
        si chiude più lì;
      - `ArenaTest.laCadutaAlPrimoRoundChiudeLArena` e `dopoLaCadutaLaCronacaSiChiudeAllaProvaPersa` —
        devono restare verdi come regressione;
      - test nuovi: pareggio che prosegue senza loot né punti (scheda identica alla prova dopo),
        pareggio alla decima prova che chiude senza trionfo, pareggi consecutivi.

**Fase 8 — La pagina**

- [x] Sezione 2: `buildMoments` regge già una prova senza `progress` non finale (il ramo
      `if (trial.progress)` la salta) e `progression.completedTrials` non viene incrementato da una
      prova pareggiata — verificato leggendo il codice, non assunto. Aggiunti `budget` ai momenti e la
      funzione pura `buildTrialOutcomesByNumber(trials)`.
- [x] Sezione 4: quarto stato `crossed` in `trialStationStateOf`, con **forma propria** — un rombo
      ottenuto con uno pseudo-elemento ruotato di 45° *dietro* al numero, così la cifra resta
      orizzontale e leggibile — ed etichetta esplicita «Prova N, attraversata senza vittoria».
      `describeConclusion` distingue ora «è arrivato in fondo al percorso, ma senza trionfare»
      (pareggio alla decima) da «si è fermato senza vincere alla prova N» (pareggio intermedio):
      arrivare in fondo non è una corsa interrotta.
- [x] Sezione 4: il monte di squadra e lo sconto compaiono nel passo zero della prova, con frase
      propria della pagina. Tace per lo specchio e a sconto zero.
- [x] `app.css`: forma a rombo per `.crossed` col relativo `:hover`, commenti di intestazione
      aggiornati (elencavano solo tre stati).
- [x] Non-spoiler riverificato sul codice: `trialStationStateOf` restituisce `'future'` **prima** di
      consultare la mappa degli esiti, che nasce solo dalle prove giocate, e le stazioni future
      restano `disabled`. Il vincolo regge.
- [ ] Verifica: criteri 15 e 16, **a mano**, con `start-web.ps1`, ricaricando la pagina più volte per
      confrontare due corse di lunghezza diversa. Nessun test automatico sul JavaScript.

**Fase 9 — Documentazione**

- [x] `CLAUDE.md`: il paragrafo di apertura (monte punti di squadra e non individuale, sconto della
      fortuna, quattro scaglioni di rarità descritti col pavimento che non parte più da `UNCOMMON` per
      salire, pareggio che prosegue), la riga di `combat` nella tabella dei package, il vincolo su
      `HeroBrain` «punto unico da toccare per ribilanciare la progressione» — che va allineato alla
      collocazione decisa per lo sconto.
- [x] `README.md`: riscritti il monte di squadra e il perché del moltiplicatore, lo sconto della
      fortuna, il pareggio che non chiude più («altrimenti l'arena si chiude lì» era diventato falso),
      e la misura nella qualità del loot col motivo (un'arma leggendaria vale più di tre vittorie).
- [x] `daImplementare.md`: la voce «ribilanciare la profondità del percorso» riscritta col prima e il
      dopo (`0/15` oltre la terza prova → `232/500`) e con le due code aperte — bimodalità e entità
      dello sconto. Aggiunta la voce nuova sullo **sfidante speculare** della prova 10, che era
      un'osservazione fuori scope della SPEC e senza una voce sua sarebbe andata persa.
- [x] Verifica: riletti i tre file; nessuna affermazione rimasta in contraddizione col codice. In
      `CLAUDE.md` corretto anche il rimando a `implementation-arena-dieci-prove.md`, che non esiste
      più (rimosso nel commit `f502c30`).

**Fase 10 — Misura sul campo**

- [x] Misurata la profondità su **500 corse** con `SilentArenaRun`. Risultati completi nel *Registro*.
      In sintesi: **232 corse su 500 (46,4%) vanno oltre la terza prova**, dove prima erano **zero su
      quindici**, e 184 (36,8%) arrivano in fondo. Il problema aperto che motivava questo lavoro è
      chiuso.
- [x] **Il ciclo di aggregazione non esiste** e non è stato introdotto: la misura è passata da un test
      usa-e-getta, poi rimosso. Suite di nuovo verde a 190 test dopo la rimozione, verificato.
- [x] Registrare il risultato nel *Registro*. Due osservazioni sono state promosse a *Problemi
      aperti*, perché ritoccare i numeri è una decisione dell'utente e non dell'implementazione: la
      distribuzione **bimodale** e l'**entità dello sconto della fortuna**.

**Fase 11 — Revisione**

- [x] Coerenza con la SPEC; nessuna modifica non richiesta; conseguenze dichiarate ancora vere. Tutte
      e quattro le *Conseguenze dichiarate* si sono verificate, e la 1 («gli sfidanti diventano molto
      più deboli, la profondità va misurata non prevista») è stata effettivamente misurata.
- [x] Controllo dei confini architetturali, verificato con ricerca sugli import:
      `combat.hero` **non** importa `combat.factory` (nessuna occorrenza); `combat.chronicle` **non**
      importa `combat.io` né `combat.factory` e non contiene `System.out`; `combat.io.web` **non**
      importa `render`/`log`/`terminal`/`replay` né `SilentArenaRun`; gli switch sulle enum di dominio
      restano esaustivi e senza `default`. Un rilievo **preesistente e non introdotto qui**:
      `combat.io.render` ha due import verso `log` e `replay` (`HeroProgressFormatter` →
      `ConsoleArenaLogger`, `CombatScreenRenderer` → `ScreenCombatReplay`), ma sono riferimenti
      **solo Javadoc** (`{@link}`), non dipendenze di codice, e risalgono al refactoring `7d50a6f`.
- [x] `mvn -o test` sull'intera suite: **190 test, BUILD SUCCESS**.
- [x] Revisione funzionale con `java-functional-evolver`: **invocato, nessuna modifica applicata**.
      Ha esaminato tutti i file toccati e ha classificato `NEUTRAL` i quattro soli candidati
      (`FighterFactory.withEquipmentBonus`, `RoundReport.appending`, la validazione di
      `reportEndOfRun`, gli `IntStream.range` di zip nei test): riscriverli in forma di stream non
      li renderebbe più chiari, e in un progetto che dichiara di voler **abbassare** la densità
      sarebbe un peggioramento. Suite ri-verificata verde dopo la revisione.
- [x] Aggiornare *Decisioni/Deviazioni* ed *Esito finale*; portare lo stato a `COMPLETED`.

## File coinvolti (effettivi)

Elenco **provvisorio**, dall'analisi in sola lettura: da confermare o correggere in Fase 1.

- `src/main/java/it/fantasyarena/combat/TrialPlan.java` — i nove monti di squadra e la regola di
  derivazione nel Javadoc
- `src/main/java/it/fantasyarena/combat/TrialStation.java` — Javadoc del campo, semantica nuova
- `src/main/java/it/fantasyarena/combat/<monte di squadra scontato>.java` — classe nuova, se si
  conferma la collocazione raccomandata dalla SPEC (altrimenti il calcolo va in `HeroBrain`)
- `src/main/java/it/fantasyarena/combat/Arena.java` — monte effettivo agli sfidanti generati, tre
  esiti distinti, trionfo derivato dall'esito dell'ultima prova, terzo costruttore di `RoundReport`
- `src/main/java/it/fantasyarena/combat/factory/FighterFactory.java` — ripartizione del monte di
  squadra, pavimento come costante pubblica, validazione diagnostica
- `src/main/java/it/fantasyarena/combat/hero/HeroBrain.java` — pesi nuovi delle quattro
  `RarityTable` e Javadoc
- `src/main/java/it/fantasyarena/combat/chronicle/TrialChronicle.java` — il dato del monte di squadra
  per prova, nullable per lo specchio
- `src/main/java/it/fantasyarena/combat/chronicle/<record del monte di squadra>.java` — se si sceglie
  il record proprio invece di tre campi sciolti
- `src/main/java/it/fantasyarena/combat/chronicle/ChronicleMapper.java` — traduzione del monte
- `src/main/java/it/fantasyarena/combat/io/log/ArenaLogger.java`,
  `ConsoleArenaLogger.java`, `SilentArenaLogger.java` — sconto all'annuncio, pareggio come passaggio
- `src/main/resources/web/app.js` — sezioni 2 e 4
- `src/main/resources/web/app.css` — solo se serve una forma per la stazione attraversata
- `src/test/java/.../combat/TrialPlanTest.java`, `combat/ArenaTest.java`,
  `factory/FighterFactoryTest.java`, `factory/ArenaFighterFactoryTest.java`,
  `hero/HeroBrainTest.java`, `chronicle/ArenaChronicleTest.java`,
  `chronicle/ChronicleMapperTest.java`, `io/web/ChronicleJsonTest.java`, più i test nuovi del monte
  scontato e della ripartizione
- `CLAUDE.md`, `README.md`, `daImplementare.md`

## Registro

Voci datate (`YYYY-MM-DD`), append-only.

- **Decisioni** — `Decisione · Motivazione · Impatto`:
  - `2026-08-02` · **Le quattro leve del lavoro** (monte di squadra col moltiplicatore `1.0`/`1.3`/`1.5`,
    sconto della fortuna, rarità ritarata, pareggio che prosegue) · scelte dall'utente fra le opzioni
    proposte, dopo l'analisi quantitativa del divario: alla prova 4 sono 24 punti contro 48, e la
    curva del monte sfidanti è la copia esatta della crescita del protagonista · è l'intero scopo di
    questo lavoro.
  - `2026-08-02` · **La fortuna letta è quella effettiva** (`hero.effectiveCharacter()`) · scelta
    dall'utente **contro** la raccomandazione della SPEC, che proponeva la base: l'argomento accolto è
    la coerenza col principio per cui l'equipaggiamento conta sempre, e il fatto che rende la `LUCK`
    sui gioielli appetibile invece che inerte · il pavimento `7 × numeroSfidanti` diventa
    comportamento ordinario e non caso limite; l'effetto va **misurato** in Fase 10, perché il rischio
    dichiarato è che un gioiello con molta `LUCK` schiacci da solo uno schieramento.
  - `2026-08-02` · **Il calcolo vive in un record `ChallengerBudget` in `it.fantasyarena.combat`**,
    accanto a `TrialPlan`, non in `HeroBrain` · la pressione del percorso è già dichiarata fuori dal
    cervello, e uno sconto non è una scelta del protagonista; in più `combat` può importare
    `combat.factory`, quindi il pavimento resta una costante sola · la riga di `CLAUDE.md` su
    `HeroBrain` «punto unico da toccare per ribilanciare la progressione» va corretta in Fase 9: le
    leve diventano due e dichiarate.
- **Deviazioni dalla SPEC** (da motivare) — `Descrizione · Motivazione · Impatto · Aggiorna la SPEC?
  sì/no`: nessuna.
- **Misura sul campo** (`2026-08-02`, Fase 10, 500 corse con `SilentArenaRun`, `CombatSettings.defaults()`):

  | prova finale | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
  | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
  | corse | 184 | 61 | 23 | 29 | 7 | 4 | 4 | 2 | 2 | 184 |

  Esiti: `FELL` 318, `STOOD_WITHOUT_WINNING` 11, `WON` 171 (tutti alla prova 10, cioè trionfi).
  Oltre la terza prova: **232/500 (46,4%)**, contro **0/15** prima del ribilanciamento. In fondo al
  percorso: 184/500 (36,8%). Trionfi: 171/500 (34,2%).

  Pareggi: 97 su 2.782 prove giocate, media 0,194 per corsa, 50 corse su 500 (10%) ne contengono
  almeno uno.

  Sconto della fortuna, su 2.268 prove a sfidanti generati: monte schiacciato sul pavimento
  **338 volte (14,9%)**, sconto medio **12,02 punti**, massimo osservato **38**.

- **Problemi aperti** (non bloccano il lavoro: sono decisioni di bilanciamento dell'utente) —
  `Descrizione · Impatto · Opzioni · Decisione richiesta`:
  - **La distribuzione è bimodale**: 184 corse muoiono alla prova 1 e 184 arrivano alla 10, con
    poco in mezzo. Delle 316 corse che superano la prima prova, 184 (58%) arrivano in fondo · la
    corsa è di fatto decisa alla prima prova: superata quella, la crescita del protagonista e lo
    sconto della fortuna compongono un vantaggio che il percorso non riprende più · opzioni: alzare
    il moltiplicatore delle prove a più sfidanti, oppure ridurre lo sconto della fortuna, oppure
    accettarla come forma voluta (una prima prova che è un filtro netto) · **decisione dell'utente**.
  - **Lo sconto della fortuna è molto forte**: 12 punti medi tolti a monti che vanno da 15 a 59, col
    pavimento raggiunto in una prova su sette. È esattamente il rischio dichiarato quando si è scelta
    la fortuna **effettiva** invece della base, ora quantificato: alla prova 1 un monte di 15 può
    scendere a 7, cioè l'avversario dimezzato · opzioni: tornare alla fortuna base, oppure dividere lo
    sconto (es. `fortuna / 2 × numeroSfidanti`), oppure metterci un tetto, oppure lasciarlo così ·
    **decisione dell'utente**.
  - Le due voci di *Da decidere* della SPEC sono invece chiuse (vedi *Decisioni*).
- **Test eseguiti** — `data · fase · comando · esito`:
  - `2026-08-02` · Fase 2 · `mvn -o test` · **verde**, 176 test.
  - `2026-08-02` · Fase 3 · `mvn -o test` · **verde**, 181 test (5 nuovi: 4 in `ChallengerBudgetTest`,
    1 in `ArenaTest`). Il pavimento è esercitato partendo da una `Hero` con un gioiello generoso di
    `LUCK`, come chiesto: con la fortuna effettiva non è un caso limite teorico.
  - `2026-08-02` · Fase 4 · `mvn -o test` · **verde**, 183 test.
  - `2026-08-02` · Fasi 5 e 6 · `mvn -o test` · **verde**, 186 test (3 nuovi in
    `ConsoleArenaLoggerTest`).
  - `2026-08-02` · Fase 7 · `mvn -o test` · **verde**, 190 test (4 nuovi in `ArenaTest`, 2 riscritti).
  - `2026-08-02` · Fase 10 · misura su 500 corse con un test usa-e-getta, poi rimosso · risultati
    sopra; `mvn -o test` dopo la rimozione **verde**, 190 test.
  - `2026-08-02` · Fase 11 · `mvn -o test` · **verde**, 190 test, `BUILD SUCCESS`.

## Esito finale

**Completato il 2026-08-02.** Undici fasi su undici, con **una sola verifica ancora da fare**: i
criteri 15 e 16 della *Definition of done* (la pagina, a mano, confrontando due partite di lunghezza
diversa) — non sono automatizzabili per scelta dichiarata del repo, che non ha runner JavaScript.

**Cosa è cambiato.** Quattro leve, nell'ordine in cui sono state mosse:

1. il monte punti di una stazione è passato da **per singolo sfidante** a **dell'intero
   schieramento**, con la curva `15, 18, 21, 31, 35, 39, 50, 54, 59` — cioè la crescita del
   protagonista moltiplicata per `1.0`/`1.3`/`1.5` a uno, due, tre sfidanti. Prima il monte seguiva la
   stessa curva della crescita del protagonista **e** veniva moltiplicato per il numero di avversari:
   alla prova 4 erano 24 punti contro 48;
2. la **fortuna** del protagonista sconta quel monte di `fortuna × numeroSfidanti`, col pavimento di
   sette punti per sfidante. È la sola cosa che dà peso a una caratteristica che nel motore vale un
   punto percentuale di critico;
3. le quattro `RarityTable` del loot sono più conservative nella prima metà: il leggendario entro la
   prova 3 passa dal 35% all'8,8%;
4. il **pareggio non chiude più la corsa**: si prosegue senza loot e senza punti. Solo la caduta
   chiude.

**Il risultato, misurato.** Prima: quindici corse, nessuna oltre la terza prova. Dopo, su cinquecento
corse: **232 vanno oltre la terza prova**, 184 arrivano in fondo, 171 trionfano. Il problema aperto
che motivava il lavoro è chiuso.

**Test**: da 176 a **190**, tutti verdi. Nuovi file di test: `ChallengerBudgetTest`,
`ConsoleArenaLoggerTest`. Nessuna dipendenza aggiunta.

**Note residue** — due decisioni di bilanciamento restano all'utente e sono registrate in *Problemi
aperti*, più una fuori scope:

- la distribuzione è **bimodale**: 184 corse muoiono alla prima prova, 184 arrivano alla decima;
- lo sconto della fortuna è **forte**: dodici punti medi, pavimento raggiunto in una prova su sette.
  È il rischio dichiarato quando si è scelta la fortuna effettiva invece della base, ora quantificato;
- lo **sfidante speculare** della prova 10 resta più debole delle prove 7-9, come la SPEC aveva
  osservato fuori scope. Ha ora una voce propria in `daImplementare.md`, così non va persa.

## Esempio (concreto: file previsti e test previsti)

```java
// File previsti — produzione:
//   combat/TrialPlan                — monti di squadra 15,18,21,31,35,39,50,54,59
//   combat/TrialStation             — Javadoc: monte dello schieramento, non del singolo
//   combat/<monte scontato>         — monte stazione - fortuna * sfidanti, con pavimento
//   combat/Arena                    — monte effettivo alla factory, tre esiti distinti, trionfo da WON
//   combat/factory/FighterFactory   — ripartizione col resto ai primi, pavimento pubblico e validato
//   combat/hero/HeroBrain           — quattro RarityTable ritarate
//   combat/chronicle/*              — monte, sconto e monte effettivo per prova; mapper
//   combat/io/log/*                 — sconto all'annuncio, pareggio come passaggio
//   web/app.js, web/app.css         — stazione attraversata, monte e sconto a schermo

// Test previsti: uno per criterio della Definition of done della SPEC.
// I criteri 15 e 16 si verificano a mano sulla pagina (niente runner JS: scelta dichiarata del repo),
// il 18 in revisione.
@Test void ilPercorsoDichiaraIMontiDiSquadraELoSpecchioNessuno()                  { /* DoD 1 */ }
@Test void ilMonteDiSquadraSiRipartisceAPartiUgualiColRestoAiPrimi()              { /* DoD 2 */ }
@Test void rifiutaUnMonteDiSquadraSottoIlPavimentoDiSettePuntiPerSfidante()       { /* DoD 3 */ }
@Test void accettaEsattamenteIlPavimento()                                        { /* DoD 3 */ }
@Test void laFortunaBaseScontaIlMonteDelloSchieramentoPerOgniSfidante()           { /* DoD 4 */ }
@Test void loScontoSiFermaAlPavimentoEQuelloRegistratoEQuelloApplicato()          { /* DoD 5 */ }
@Test void laStazioneDelloSpecchioNonRiceveNeMonteNeSconto()                      { /* DoD 6 */ }
@Test void laCronacaDiOgniProvaPortaMonteScontoEMonteEffettivo()                  { /* DoD 7 */ }
@Test void ilJsonPortaLeChiaviDelMonteDiSquadra()                                 { /* DoD 8 */ }
@Test void laConsoleDichiaraLoScontoAllAnnuncioDelRound()                         { /* DoD 9 */ }
@Test void leQuattroTabelleDiRaritaHannoIPesiNuoviEIlPavimentoSaleSoloDallaSesta() { /* DoD 10 */ }
@Test void ilPareggioFaProseguireSenzaLootESenzaPuntiCaratteristica()             { /* DoD 11 */ }
@Test void laCadutaChiudeLaCorsaComePrima()                                       { /* DoD 12 */ }
@Test void unPareggioAllaDecimaProvaChiudeLaCorsaSenzaTrionfo()                   { /* DoD 13 */ }
@Test void laProvaPareggiataHaVoceDiCronacaSenzaProceduraENonInterrompeLeSuccessive() { /* DoD 14 */ }
@Test void formaDelleProveEPuntiPerVittoriaRestanoInvariati()                     { /* DoD 17 */ }
```
