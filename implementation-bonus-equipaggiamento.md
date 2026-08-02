# IMPLEMENTATION — Bonus dell'equipaggiamento

**Specifica di riferimento:** `spec-bonus-equipaggiamento.md`  — nel resto del documento: «la SPEC».
**Stato:** `IN_PROGRESS`  <!-- NOT_STARTED | IN_PROGRESS | BLOCKED | COMPLETED -->

Documento di lavoro: la SPEC (il "cosa") resta stabile; qui vivono stato, piano, decisioni e problemi
(il "come").

## Regole per l'agente

- Leggere `CLAUDE.md` e la SPEC prima di toccare codice.
- Alla ripresa del lavoro, leggere prima questo file e riprendere dallo stato corrente.
- Prima di modificare, elencare i file che verranno toccati. Nessun refactoring fuori scope.
- Non modificare i requisiti della SPEC senza decisione esplicita.
- Dopo ogni fase: eseguire i test pertinenti e aggiornare questo file. Spuntare una voce solo dopo
  verifica reale, mai a priori. Vale anche per le fasi senza codice (analisi, documentazione).
- Scelta che **non** cambia il comportamento osservabile → procedi e annotala in *Decisioni*.
- Scelta che **cambia** comportamento o criteri di accettazione, o ambiguità non risolvibile dalla
  SPEC → **fermati**, imposta lo stato a `BLOCKED` e registra in *Problemi aperti* / *Deviazioni*.
- Le quattro voci di *Da decidere* della SPEC vanno risolte **prima** della fase che le tocca: se
  arrivano a quel punto senza risposta, lo stato va a `BLOCKED`.
- Non lanciare la modalità web a mano senza `mvn process-resources` e senza fermare la JVM
  precedente: sono le due trappole descritte in `CLAUDE.md`. Usare `start-web.ps1`/`stop-web.ps1`.

## Piano operativo

**Fase 1 — Analisi**

- [x] Confermare l'import e la forma reale di `BuffElement`/`StatusEffect` (package esatto,
      accessori `characteristic()`/`value()`) e che `buffs()` esiste su tutti e tre i result.
      → `it.fantasytoolkit.buffdebuffgenerator.result`, `buffs()` presente su tutti e tre.
- [x] Confermare che togliere `.noStatusEffect()` produce sempre almeno un buff, generando qualche
      oggetto in un test usa-e-getta o leggendo la doc del toolkit già indicizzata.
- [x] Confermare che `FighterAssembler.assemble` non ha overload che accettino i gioielli e che
      nessun altro punto del repo assembla `Fighter`. → tre overload, nessuno coi gioielli;
      `FighterFactory` è l'unico assemblatore del repo.
- [x] Rilevare lo stile dei test esistenti (JUnit Jupiter, solo `Assertions`, fixture dal `test-jar`
      del motore) e i punti che le fixture costruiscono con `List.of()` di buff.
- [x] Compilare "File coinvolti (effettivi)" confermando o correggendo l'elenco provvisorio.
      → elenco confermato; la classe nuova è `combat/hero/EquipmentBonus.java`.

**Fase 2 — Il punto unico della somma dei buff**

- [x] Introdurre in `combat.hero` la classe che, date le caratteristiche base e i buff degli oggetti
      equipaggiati, produce le caratteristiche effettive (vedi *Esempio* della SPEC), riusando la
      ricostruzione a mano del `CharacterResult` già presente in `HeroBrain.grow`.
      → `EquipmentBonus` (`applyTo`, `totalValueOf`), classe di soli metodi statici.
- [x] Esporre su `Hero` i buff dell'equipaggiamento e le caratteristiche effettive come **dato
      derivato risolto alla lettura**, senza custodirlo in un campo. → `Hero.effectiveCharacter()`,
      con `equippedBuffs()` privato; `character()` e `totalCharacteristicPoints()` restano base.
- [x] Verifica: nuovo test di unità sui criteri 1 e 4 della *Definition of done*; `mvn test` verde.
      → `EquipmentBonusTest` nuovo, `HeroTest` esteso.

**Fase 3 — I buff entrano nello scontro**

- [x] Togliere `.noStatusEffect()` dalle sei generazioni di `FighterFactory`.
- [x] Passare al `FighterAssembler` le caratteristiche effettive in `summon`, `createChallenger` e
      `createMirrorRival` (`FighterFactory` mantiene lo stile a 4 spazi del file).
      → helper privato `withEquipmentBonus` per i personaggi generati al volo, che non sono `Hero`.
- [x] Verifica: criteri 2 e 3; aggiornare `ArenaFighterFactoryTest`, in particolare
      `loSfidanteSpecularePareggiaPuntiEPezziMaImpugnaUnArmaRara`, che oggi confronta i punti **base**
      del protagonista con quelli — ora effettivi — dello specchio e fallirebbe: va riscritto sulla
      conseguenza dichiarata 2 della SPEC.

**Fase 4 — Il gioiello smette di valere punti**

- [x] Rimuovere `JEWEL_BONUS_POINTS` e la somma dei punti del gioiello in
      `HeroBrain.progressAfterVictory`; togliere `points` da `NewJewel` e `JewelUpgrade` e aggiornare
      `JewelDecision`.
- [x] Sostituire il comparatore `BY_JEWEL_VALUE` con «valore totale dei buff, rarità come spareggio»;
      lasciare invariati gli altri due.
- [x] Aggiornare i Javadoc di classe di `HeroBrain` e `HeroProgress`, che oggi descrivono i punti
      extra del gioiello come parte del disegno.
- [x] Verifica: criteri 5, 6, 7; `HeroBrainTest` aggiornato (via i tre test sui punti del gioiello,
      nuovi test sulla cernita per valore dei buff) e `HeroProgressTest` se tocca i due record.
- [x] Ponte provvisorio, da chiudere in Fase 5 e 6: `ChronicleMapper.snapshotJewelBonusPoints` e le
      due righe del gioiello di `HeroProgressFormatter` leggevano i `points` spariti e ora chiamano
      `EquipmentBonus.totalValueOf` sui buff dell'oggetto preso, così il repository resta
      compilabile e verde. `combat.io.render` non deve restare a calcolare regole di gioco.

**Fase 5 — La cronaca porta i bonus**

- [x] `ItemSnapshot`: campo dei bonus dell'oggetto, nella forma decisa (vedi *Da decidere* della
      SPEC), vuoto per un oggetto senza buff. → `List<CharacteristicBonus> bonuses`, record nuovo
      `combat/chronicle/CharacteristicBonus.java`.
- [x] `HeroSnapshot`: caratteristiche base **e** effettive.
- [x] `ProgressChronicle`: via `jewelBonusPoints`; `ChronicleMapper`: via `snapshotJewelBonusPoints`,
      i tre `snapshot*` portano i bonus, `snapshotHero` porta entrambe le liste. → tolto anche il
      ponte provvisorio di Fase 4 e l'import di `EquipmentBonus`.
- [x] Aggiornare i Javadoc dei record toccati (quello di `ItemSnapshot` spiega oggi perché il
      gioiello non ha numeri: non è più vero).
- [x] Verifica: criteri 8 e 9; `ChronicleMapperTest` aggiornato (i tre test sul bonus del gioiello) e
      `ChronicleJsonTest` — che è **l'unica rete a protezione del contratto verso il JavaScript** —
      con le chiavi nuove aggiunte e `jewelBonusPoints` tolta. → più `ArenaChronicleTest` e
      `ArenaWebServerTest`, che costruivano `HeroSnapshot` con la firma vecchia.

**Fase 6 — La console**

- [x] `HeroProgressFormatter`: i `describe(...)` dei tre tipi portano i bonus; le due frasi del
      gioiello si riscrivono senza i punti caratteristica. → il bonus sta su riga propria
      («Bonus dell'oggetto trovato/lasciato: …»), quindi `lootLine` è diventato `lootLines`.
- [x] `FighterCardFormatter`: bonus dell'equipaggiamento nella scheda, senza sfondare il troncamento
      a 36 caratteri (vedi *Da decidere*). → riga propria sotto l'oggetto, solo in `card`.
- [x] Non toccare `BattleSceneRenderer` (nomi e vitali, nessun equipaggiamento) né i logger, che
      restano l'unico punto di I/O.
- [x] Verifica: criteri 10 e 11; `HeroProgressFormatterTest` aggiornato ed esteso, test nuovo o
      esteso sulla scheda del combattente. → `FighterCardFormatterTest` nuovo.

**Fase 7 — La pagina**

- [x] Sezione 1: riscrivere le due frasi del gioiello in `LOOT_FATE_MESSAGES` (gemelle di quelle
      della fase 6) e aggiungere le eventuali frasi nuove dell'intestazione del riquadro in alto.
- [x] Sezione 2: accumulare la scheda corrente e il numero di prove concluse dentro ogni momento
      (dati puri, nessun DOM, nessuno sguardo alle prove future). → `progression = { hero,
      completedTrials }`, propagata da `buildTrialMomentBase` e da `buildConclusionMoment`.
- [x] Sezione 4: `describeItem` mostra i bonus; la lista delle caratteristiche del protagonista mostra
      base e contributo; `renderProtagonistEntry` diventa parte di `renderMoment` e legge il momento;
      togliere la scheda «Scheda aggiornata» duplicata dal pannello della procedura; dichiarare nella
      scheda del combattente che le caratteristiche sono quelle in campo.
- [x] `app.css` solo se serve una classe nuova per i bonus; niente librerie, niente build step.
      → nessuna modifica necessaria, i bonus riusano gli stili esistenti.
- [ ] Verifica: criteri 12 e 13, **a mano**, con `start-web.ps1` e ricaricando la pagina più volte per
      confrontare due corse di lunghezza diversa. Nessun test automatico sul JavaScript.
      → **in carico all'utente**, non ancora eseguita.

**Fase 8 — Documentazione**

- [x] `CLAUDE.md`: il paragrafo di apertura (il gioiello non vale più punti extra, gli oggetti
      portano buff che entrano nello scontro), la riga di `combat.hero` nella tabella dei package con
      la classe nuova, il vincolo sull'autosufficienza della cronaca che cita «i punti che il gioiello
      vale». → più la riga di `combat.chronicle` (base/effettive, bonus, asimmetria dichiarata col
      combattente), il vincolo di `HeroBrain` sul criterio di cernita, e un vincolo nuovo sul punto
      unico della somma dei buff.
- [x] `README.md`: la procedura di fine scontro (tre punti e basta), il blocco d'esempio della
      console se il formato delle righe cambia, e cosa fanno ora gli oggetti trovati.
- [x] `daImplementare.md`: la voce sui gioielli «fuori dal combattimento, unico effetto i punti extra»
      non è più vera; la voce sulla cronaca autosufficiente cita i punti del gioiello. → il debuff
      resta la sola parte scoperta, ed è una lacuna del toolkit.
- [x] Verifica: rilettura dei tre file; nessuna affermazione rimasta in contraddizione col codice.

**Fase 9 — Revisione**

- [x] Coerenza con la SPEC; nessuna modifica non richiesta; conseguenze dichiarate ancora vere.
- [x] Controllo dei confini architetturali: `combat.chronicle` non importa `combat.factory`,
      `combat.hero` non importa `combat.factory`, `combat.io.render` resta senza I/O. → verificato
      per `grep`: nessun import, solo due citazioni in Javadoc; `combat.io` non importa più
      `EquipmentBonus` (il ponte di Fase 4 è chiuso).
- [x] `mvn test` sull'intera suite, verde. → 174 test, 0 falliti.
- [x] Revisione funzionale del Java prodotto con `java-functional-evolver` e ri-verifica dei test.
      → invocato; **nessuna modifica applicata**: ha giudicato il codice già adeguato e ha respinto
      motivandole tre proposte (`Stream.concat` al posto dell'`ArrayList` locale in
      `Hero.equippedBuffs` e in `FighterFactory.withEquipmentBonus`, mutazione locale e contenuta;
      l'`Optional<String>` di `bonusLine` è l'idioma giusto al confine; il `for` con estrazione
      casuale di `distributeCharacteristicPoints` è più onesto di uno stream).
- [ ] Aggiornare *Decisioni/Deviazioni* ed *Esito finale*; portare lo stato a `COMPLETED`.
      → resta aperto in attesa della verifica manuale della pagina, criteri 12 e 13 (Fase 7).

## File coinvolti (effettivi)

Elenco **provvisorio**, dall'analisi in sola lettura: da confermare o correggere in Fase 1.

- `src/main/java/it/fantasyarena/combat/hero/<classe nuova>.java` — il punto unico della somma dei
  buff
- `src/main/java/it/fantasyarena/combat/hero/Hero.java` — buff equipaggiati e caratteristiche
  effettive come dato derivato
- `src/main/java/it/fantasyarena/combat/hero/HeroBrain.java` — via `JEWEL_BONUS_POINTS`, nuovo
  comparatore del gioiello
- `src/main/java/it/fantasyarena/combat/hero/HeroProgress.java` — `NewJewel`/`JewelUpgrade` senza
  `points`
- `src/main/java/it/fantasyarena/combat/factory/FighterFactory.java` — via `.noStatusEffect()`,
  caratteristiche effettive all'assemblatore
- `src/main/java/it/fantasyarena/combat/chronicle/ItemSnapshot.java` — bonus dell'oggetto
- `src/main/java/it/fantasyarena/combat/chronicle/HeroSnapshot.java` — caratteristiche base ed
  effettive
- `src/main/java/it/fantasyarena/combat/chronicle/ProgressChronicle.java` — via `jewelBonusPoints`
- `src/main/java/it/fantasyarena/combat/chronicle/ChronicleMapper.java` — traduzione dei buff, via
  `snapshotJewelBonusPoints`
- `src/main/java/it/fantasyarena/combat/chronicle/<record dei bonus>.java` — se si sceglie il record
  proprio della cronaca (vedi *Da decidere* della SPEC)
- `src/main/java/it/fantasyarena/combat/io/render/HeroProgressFormatter.java` — bonus negli oggetti,
  due frasi del gioiello riscritte
- `src/main/java/it/fantasyarena/combat/io/render/FighterCardFormatter.java` — bonus nella scheda
- `src/main/resources/web/app.js` — sezioni 1, 2 e 4
- `src/main/resources/web/app.css` — solo se serve una classe per i bonus
- `src/test/java/.../hero/HeroBrainTest.java`, `hero/HeroProgressTest.java`,
  `factory/ArenaFighterFactoryTest.java`, `chronicle/ChronicleMapperTest.java`,
  `io/web/ChronicleJsonTest.java`, `io/render/HeroProgressFormatterTest.java`, più il test nuovo del
  punto unico e quello sulla scheda del combattente
- `CLAUDE.md`, `README.md`, `daImplementare.md`

## Registro

Voci datate (`YYYY-MM-DD`), append-only.

- **Decisioni tecniche** (non cambiano il comportamento) — `Decisione · Motivazione · Impatto`:
  - 2026-08-02 · I bonus viaggiano nella cronaca come record proprio
    `combat.chronicle.CharacteristicBonus(Characteristic, int)` · il `BuffElement` del toolkit è
    `SNAPSHOT` e affiorerebbe nel contratto JSON verso il JavaScript, che nessun test protegge oltre
    a `ChronicleJsonTest` · chiude la prima voce di *Da decidere* della SPEC.
  - 2026-08-02 · In console i bonus stanno su una riga propria sotto l'oggetto · accodati alla riga
    dell'oggetto verrebbero troncati dal limite di 36 caratteri di `FighterCardFormatter` · chiude la
    terza voce di *Da decidere*.
  - 2026-08-02 · Intestazione del riquadro in alto: «Il protagonista entra nell'arena» a zero prove
    concluse, «Il protagonista dopo la prova N» da lì in avanti · dice a quale punto della corsa si
    riferisce senza nominare prove non ancora giocate · chiude la quarta voce di *Da decidere*.
- **Decisioni dell'utente** (cambiano il comportamento, prese prima della Fase 1):
  - 2026-08-02 · I buff sono veri e valgono **solo mentre l'oggetto è equipaggiato**, e l'aggiunta si
    estende ad arma e armatura oltre che al gioiello · è il modello che rende il bonus una proprietà
    dell'oggetto e non un premio una-tantum · è il presupposto dell'intera SPEC.
  - 2026-08-02 · Lo sfidante speculare **resta sulla base**: la conseguenza dichiarata 2 della SPEC
    non si chiude qui · il ribilanciamento va misurato dopo, non previsto ora · nessun cambio a
    `createMirrorRival` oltre alle caratteristiche effettive che gli spettano come a tutti.
  - 2026-08-02 · I bonus **non** compaiono nella `compactCard` del duello in console · la scheda
    compatta esiste per il poco spazio verticale e già omette le caratteristiche · chiude la seconda
    voce di *Da decidere*; perdita d'informazione accettata esplicitamente.
- **Deviazioni dalla SPEC** (da motivare) — `Descrizione · Motivazione · Impatto · Aggiorna la SPEC?
  sì/no`: nessuna.
- **Problemi aperti** (bloccano l'avanzamento) — `Descrizione · Impatto · Opzioni · Decisione
  richiesta`: nessuno; le quattro voci di *Da decidere* della SPEC sono chiuse qui sopra.
- **Test eseguiti** — `data · fase · comando · esito`:
  - 2026-08-02 · Fasi 1-4 · `mvn clean test` · verde, 169 test, 0 falliti.
  - 2026-08-02 · Fasi 5-7 · `mvn -o test` · verde, 174 test, 0 falliti.
  - 2026-08-02 · Fase 9 · `mvn -o test` dalla radice · verde, 174 test, 0 falliti.
  - 2026-08-02 · Fase 7 · verifica manuale della pagina · **non ancora eseguita**, in carico
    all'utente: criteri 12 e 13 della *Definition of done*.

## Esito finale

Da compilare a fine lavoro: stato finale, modifiche effettuate, test eseguiti, note residue.

## Esempio (concreto: file previsti e test previsti)

```java
// File previsti — produzione:
//   combat/hero/<somma dei buff>   — caratteristiche base + buff = caratteristiche effettive
//   combat/hero/Hero               — buff equipaggiati, caratteristiche effettive (derivate)
//   combat/hero/HeroBrain          — via JEWEL_BONUS_POINTS, cernita del gioiello per valore
//   combat/hero/HeroProgress       — NewJewel/JewelUpgrade senza points
//   combat/factory/FighterFactory  — buff generati, caratteristiche effettive all'assemblatore
//   combat/chronicle/*             — bonus sugli oggetti, base+effettive sull'eroe, via jewelBonusPoints
//   combat/io/render/*             — bonus nelle righe di console, frasi del gioiello riscritte
//   web/app.js, web/app.css        — bonus a schermo, riquadro in alto che segue la corsa

// Test previsti: uno per criterio della Definition of done della SPEC.
// I criteri 12 e 13 si verificano a mano sulla pagina (niente runner JS: scelta dichiarata del repo),
// il 15 in revisione.
@Test void iBuffDegliOggettiEquipaggiatiSiSommanoAlleCaratteristicheBase()      { /* DoD 1 */ }
@Test void unBuffSuUnaCaratteristicaNonPresenteVieneIgnorato()                  { /* DoD 1 */ }
@Test void ilCombattenteMaterializzatoScendeInCampoConLeCaratteristicheEffettive() { /* DoD 2 */ }
@Test void sostituireUnOggettoNeSostituisceIBonus()                             { /* DoD 3 */ }
@Test void laSchedaBaseNonRisenteDeiBuffDellEquipaggiamento()                   { /* DoD 4 */ }
@Test void ilGioielloIndossatoNonFruttaPiuPuntiOltreAiTreDellaVittoria()        { /* DoD 5 */ }
@Test void tieneIlGioielloDaiBuffPiuAltiEScartaAParitaDiValore()                { /* DoD 6 */ }
@Test void laRaritaDelGioielloDecideSoloAParitaDiValoreDeiBuff()                { /* DoD 6 */ }
@Test void armaEArmaturaConservanoLaCernitaPerAttaccoEDifesa()                  { /* DoD 7 */ }
@Test void laFotografiaPortaIBonusDellOggettoELeCaratteristicheBaseEdEffettive() { /* DoD 8 */ }
@Test void ilJsonPortaLeChiaviNuoveENonPiuJewelBonusPoints()                    { /* DoD 9 */ }
@Test void laProceduraDiFineScontroDescriveIBonusDellOggettoTrovato()           { /* DoD 10 */ }
@Test void laSchedaDelCombattenteMostraIBonusDellEquipaggiamento()              { /* DoD 11 */ }
@Test void ilPercorsoELeTabelleDelLootRestanoInvariati()                        { /* DoD 14 */ }
```
