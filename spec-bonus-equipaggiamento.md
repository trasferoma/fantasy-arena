# SPEC — Bonus dell'equipaggiamento

**Obiettivo:** far contare davvero gli oggetti che portano variazioni alle statistiche — i buff del
toolkit entrano nello scontro finché l'oggetto è impugnato o indossato — renderli visibili nel
riepilogo dell'oggetto e nella scheda del protagonista, e far seguire al riquadro in alto della
pagina lo stato corrente della corsa invece della sola configurazione d'ingresso.

**Contesto**

- Punti del codice interessati:
  - `combat.factory.FighterFactory` — genera arma, armatura e gioiello con `.noStatusEffect()`
    (sei chiamate: `generateWeapon`, `generateLootWeapon`, `generateArmour`, `generateLootArmour`,
    `generateArmourPiece`, `generateJewel`), e assembla i combattenti con
    `assembler.assemble(character, weapon, armourPieces, null)` in `summon`, `createChallenger`,
    `createMirrorRival`;
  - `combat.hero.HeroBrain` — `JEWEL_BONUS_POINTS`, il comparatore `BY_JEWEL_VALUE` (oggi sola
    rarità), `progressAfterVictory` (che somma i punti del gioiello ai tre della vittoria) e `grow`,
    che ricostruisce a mano un `CharacterResult` perché il toolkit non espone API di crescita;
  - `combat.hero.Hero` — custodisce arma, armatura per slot e gioielli per tipo; i gioielli oggi
    restano fuori dallo scontro, e `totalCharacteristicPoints()` misura le caratteristiche base;
  - `combat.hero.HeroProgress` — i record `NewJewel(jewel, points)` e
    `JewelUpgrade(dropped, taken, points)`;
  - `combat.chronicle` — `ItemSnapshot(kind, name, rarity, power)`, `HeroSnapshot`,
    `ProgressChronicle.jewelBonusPoints()`, `ChronicleMapper` (`snapshotJewelBonusPoints`,
    `snapshotWeapon`/`snapshotArmourPiece`/`snapshotJewel`, `snapshotHero`);
  - `combat.io.render.HeroProgressFormatter` (le due frasi del gioiello con «+N punti
    caratteristica») e `combat.io.render.FighterCardFormatter` (righe arma/armatura, larghezza
    massima 36 con troncamento);
  - `src/main/resources/web/app.js` — `LOOT_FATE_MESSAGES` (sezione 1), `buildMoments` e i
    costruttori dei momenti (sezione 2), `describeItem`, `buildHeroCard`, `renderProtagonistEntry`,
    `renderProgressPanel`, `renderMoment` (sezione 4); `renderProtagonistEntry` è invocata **una
    volta sola** nel `bootstrap`.
- Fatti verificati che la modifica dà per acquisiti:
  - i tre result del toolkit portano tutti i buff — `WeaponResult(weapon, rarity, buffs, debuffs,
    attack)`, `ArmourResult(armour, rarity, buffs, debuffs, defense)`, `JewelResult(jewel, rarity,
    buffs, debuffs)` — e `BuffElement(Characteristic characteristic, int value)` implementa
    `StatusEffect`; i debuff sono oggi sempre vuoti (lacuna dichiarata del toolkit);
  - `noStatusEffect()` è un **opt-out**: toglierlo fa nascere gli oggetti coi buff generati per
    rarità da `BuffDebuffGeneratorTool`, sempre almeno uno, con `count` e intervallo di valore
    crescenti col grado (`COMMON (1,1,2)` … `LEGENDARY (1,9,10)`/`(5,1,2)`);
  - il motore non legge i buff in nessun punto e non monta i gioielli: `FighterAssembler.assemble`
    calcola i Rating dalle caratteristiche del `CharacterResult` più attacco/difesa
    dell'equipaggiamento. Passargli caratteristiche già maggiorate è quindi l'unico modo di far
    contare i buff **senza toccare il motore**;
  - tutti i personaggi nascono con `allCharacteristics()`, quindi ogni `Characteristic` che un buff
    può colpire esiste già nella scheda;
  - la cronaca porta già `trial.progress.heroAfter` per ogni prova vinta: il riquadro in alto può
    seguire la corsa senza nessun campo nuovo dal backend.
- Pattern esistenti da riusare: la ricostruzione a mano del `CharacterResult` di `HeroBrain.grow`;
  la forma dei tre comparatori di cernita (valore numerico come criterio, rarità come spareggio); la
  fotografia uniforme di `ItemSnapshot`; la costruzione dei momenti come **dati puri** nella sezione
  2 di `app.js`; la duplicazione dichiarata delle frasi fra `combat.io.render` e `app.js`.
- File coinvolti: `FighterFactory`, `Hero`, `HeroBrain`, `HeroProgress`, `ItemSnapshot`,
  `HeroSnapshot`, `ProgressChronicle`, `ChronicleMapper`, `HeroProgressFormatter`,
  `FighterCardFormatter`, `web/app.js`, `web/app.css`, più una classe nuova per la somma dei buff.

**Comportamento atteso**

- **I buff esistono e contano.** Ogni arma, pezzo d'armatura e gioiello generato nasce coi buff del
  toolkit. I buff degli oggetti **equipaggiati** si sommano alle caratteristiche base nell'istante in
  cui il combattente viene assemblato: al `FighterAssembler` vanno le caratteristiche **effettive**.
  Vale per il protagonista (`summon`), per gli sfidanti generati e per lo specchio.
- **Il bonus vive con l'oggetto.** Sostituire un anello, un pezzo d'armatura o un'arma sostituisce i
  suoi bonus; l'oggetto lasciato non contribuisce più dal round successivo. Il gioiello, che il
  motore non monta, conta comunque attraverso questa somma.
- **La scheda base non cambia.** I buff non entrano mai in `Hero.character()`: la crescita di
  `HeroBrain` continua a distribuire i punti sulle caratteristiche base, e le caratteristiche
  effettive sono un **dato derivato, risolto alla lettura**, mai custodito accanto alle fonti.
- **Il gioiello non vale più punti caratteristica.** `JEWEL_BONUS_POINTS` sparisce: la vittoria vale
  i suoi tre punti e basta. Spariscono di conseguenza il campo `points` di `NewJewel` e
  `JewelUpgrade`, il campo `jewelBonusPoints` di `ProgressChronicle` (e la sua chiave nel JSON), e le
  due frasi del destino del gioiello si riscrivono nei due posti in cui vivono —
  `HeroProgressFormatter` per la console, `LOOT_FATE_MESSAGES` di `app.js` per la pagina.
- **Cernita del gioiello.** Il gioiello trovato si tiene se il **valore totale dei suoi buff** batte
  quello indossato dello stesso tipo, con la **rarità come spareggio**: la stessa forma dei
  comparatori di arma e armatura. A parità piena si tiene il proprio, come per le altre due
  categorie. Un tipo scoperto si occupa comunque.
- **Cernita di arma e armatura invariata**: attacco e difesa restano il criterio, la rarità lo
  spareggio.
- **La cronaca porta i bonus.** `ItemSnapshot` guadagna i bonus dell'oggetto come lista di coppie
  caratteristica/valore (vuota per un oggetto senza buff); `HeroSnapshot` porta sia le caratteristiche
  **base** sia quelle **effettive**, così nessun lettore deve ricalcolare una regola di gioco.
- **Come si mostrano i bonus.**
  - Pagina: nel riquadro dell'oggetto (arma, pezzo d'armatura, gioiello, sia nelle schede sia nella
    procedura di fine scontro) accanto a rarità e potenza; nella lista delle caratteristiche del
    protagonista il valore base col contributo dell'equipaggiamento accanto (es. `STRENGTH: 12 (+3)`),
    ricavato dalla differenza fra effettive e base — sottrazione, non regola di gioco.
  - Console: `HeroProgressFormatter` descrive l'oggetto trovato e quello lasciato coi loro bonus;
    `FighterCardFormatter` aggiunge i bonus alla scheda del combattente. `BattleSceneRenderer` non
    cambia: mostra nomi e vitali, non l'equipaggiamento.
- **Riquadro in alto sempre aggiornato.** Mostra lo stato corrente: la scheda d'ingresso fino
  all'ultimo passo della prima prova, la scheda cresciuta **già dal momento della procedura** che
  quella crescita racconta, e da lì in poi quella dell'ultima prova conclusa, momento di conclusione
  compreso. Il dato si costruisce nella **sezione 2** di `app.js`, accumulando la scheda corrente
  mentre si scorrono le prove, e non si ricalcola durante il rendering. L'intestazione dice a quale
  punto della corsa si riferisce (all'ingresso, oppure dopo la prova N). La scheda «Scheda
  aggiornata» duplicata dentro il pannello della procedura sparisce: mostrerebbe esattamente la
  stessa scheda del riquadro in alto, nello stesso istante.
- Casi limite:
  - oggetto senza buff (fixture dei test, oggetti costruiti a mano): bonus vuoti, caratteristiche
    effettive uguali alle base, nessuna riga di bonus da nessuna parte;
  - buff su una caratteristica non presente nel personaggio: si **ignora**, non si inventa una voce
    nuova nella scheda (oggi non può accadere, perché tutti nascono con `allCharacteristics()`);
  - più buff sulla stessa caratteristica da oggetti diversi: si sommano;
  - prova non vinta: nessuna procedura, quindi il riquadro in alto resta sulla scheda che portava.
- Invarianti: forma degli scontri, percorso e bilanciamento della rarità del loot, cura di fine
  scontro, esiti, struttura dei momenti della pagina e vincolo di non-spoiler restano identici.

**Vincoli**

- **Nessuna modifica al motore** e nessuna regola di combattimento in questo repository: i buff
  entrano nello scontro solo attraverso le caratteristiche passate al `FighterAssembler`.
- **Un solo punto** somma i buff dell'equipaggiamento alle caratteristiche base, e serve sia a
  `combat.factory` (per `summon`, `createChallenger`, `createMirrorRival`) sia alla fotografia del
  protagonista in `combat.chronicle`. Va collocato in `combat.hero`, che entrambi già importano:
  `Hero` non deve dipendere da `combat.factory`, e `combat.chronicle` non deve cominciare a
  dipendere dalla factory per un calcolo che non è generazione. Nessuna duplicazione della somma.
- La cronaca resta **di soli dati**: nessuna stringa di presentazione, nessuna annotazione Jackson,
  nessun modulo Jackson aggiuntivo, campi facoltativi nullabili e non `Optional`.
- Le frasi restano **duplicate di proposito** fra `combat.io.render` e `app.js`: cambiate in un
  posto, vanno cambiate nell'altro.
- `combat.io.render` resta puro (nessun I/O); la pagina resta HTML/CSS/JS vanilla senza librerie né
  build step, col testo che entra nel DOM via `textContent`.
- La divisione in quattro responsabilità di `app.js` va rispettata: la scheda corrente di ogni
  momento è un dato della sezione 2, non un calcolo del rendering.
- Java 21 come nel resto del repo (record, switch expression esaustive senza `default` sulle enum di
  dominio), 2 spazi di indentazione — `FighterFactory` resta l'eccezione storica a 4.
- Nessuna nuova dipendenza.

**Fuori scope**

- I **debuff**: il toolkit non ne genera (lacuna dichiarata a monte). Nessuna infrastruttura
  preventiva per accoglierli.
- Ribilanciare la profondità del percorso (`CHARACTERISTIC_POINTS_PER_VICTORY`, curva del monte punti
  di `TrialPlan`): il problema aperto delle corse che finiscono presto resta aperto, e questa
  modifica non pretende di chiuderlo.
- Cambiare il criterio di cernita di arma e armatura per tenere conto dei buff.
- Rendere il gioiello montabile dal motore, scudi, pozioni e ogni altro slot ancora scoperto.
- Test automatici del JavaScript (scelta dichiarata del repo).

**Conseguenze dichiarate** (accettate, non da risolvere qui)

1. **Ribilanciamento voluto**: anche sfidanti e specchio nascono con equipaggiamento che porta buff,
   quindi tutti diventano più forti. L'effetto netto sulla profondità delle corse va misurato dopo,
   non previsto ora.
2. **Lo specchio resta indietro.** `Hero.totalCharacteristicPoints()` misura le caratteristiche
   **base**: alla decima prova il protagonista, che nel frattempo ha accumulato equipaggiamento
   pregiato, arriva più forte del suo specchio, che pareggia solo la base. Registrato come noto e non
   risolto qui: correggerlo significherebbe decidere se lo specchio debba pareggiare le
   caratteristiche effettive, cioè una scelta di bilanciamento separata.
3. **Il protagonista può scartare un'arma con buff notevoli ma attacco inferiore**, perché il
   criterio di cernita di arma e armatura non cambia. È una decisione di bilanciamento lasciata
   separata di proposito.
4. **Asimmetria fra le due fotografie.** `HeroSnapshot` porta base ed effettive; `CombatantSnapshot`
   fotografa un `Fighter` che a quel punto ha **già** le caratteristiche effettive, quindi la scheda
   di un combattente in campo mostra solo quelle. Va bene così: in campo conta il valore che il
   motore ha usato, e i bonus dei singoli oggetti restano comunque leggibili dagli `ItemSnapshot`
   della scheda. La pagina deve però **dire** che quelle sono le caratteristiche in campo,
   equipaggiamento compreso, per non far sembrare divergenti due schede dello stesso personaggio.
5. Il riquadro in alto che segue la corsa **non viola il non-spoiler**: mostra solo prove già
   concluse, non guarda mai le voci future di `chronicle.trials` e non rivela quante ne restano.

**Da decidere** (scelte che cambiano il comportamento osservabile: vanno confermate)

- **Forma dei bonus nella cronaca.** Proposta: un record nuovo di `combat.chronicle`,
  `CharacteristicBonus(Characteristic characteristic, int value)`, invece di esporre direttamente il
  `BuffElement` del toolkit — che è `SNAPSHOT` e affiorerebbe nel JSON come contratto verso il
  JavaScript. Alternativa scartata: riusare `BuffElement`, coerente con `CharacterCharacteristic` già
  esposto, ma più fragile.
- **Bonus nella scheda compatta della console.** Proposta: mostrarli in `card` e **non** in
  `compactCard`, che esiste proprio per il poco spazio verticale della pagina del duello e già omette
  le caratteristiche. Da confermare, perché è una perdita d'informazione nel duello.
- **Larghezza della console.** I bonus non si accodano alla riga dell'oggetto (verrebbe troncata a 36
  caratteri da `FighterCardFormatter`): proposta di una riga propria sotto l'oggetto.
- **Testo dell'intestazione del riquadro in alto.** Proposta: «Il protagonista entra nell'arena» a
  zero prove concluse, «Il protagonista dopo la prova N» da lì in avanti.

**Definition of done** — criteri verificabili, ognuno coperto da almeno un test (dove segnato,
verifica manuale, secondo la policy del repo sul JavaScript)

1. le caratteristiche effettive sono la somma delle base coi buff degli oggetti equipaggiati: più
   buff sulla stessa caratteristica si sommano, un oggetto senza buff non cambia nulla, un buff su
   una caratteristica non presente viene ignorato;
2. il combattente materializzato da una `Hero` con oggetti che portano buff scende in campo con le
   caratteristiche effettive, non con quelle base; anche sfidanti generati e specchio nascono con le
   proprie;
3. sostituire un oggetto sostituisce i suoi bonus: il combattente materializzato dopo il cambio
   riflette i buff dell'oggetto preso e non più quelli di quello lasciato;
4. la scheda base resta intatta: `Hero.character()` e `totalCharacteristicPoints()` non risentono dei
   buff, e la crescita continua a distribuire tre punti per vittoria;
5. il gioiello non frutta più punti caratteristica: dopo una vittoria con gioiello indossato i punti
   distribuiti sono esattamente tre;
6. il gioiello trovato si tiene se il valore totale dei suoi buff è maggiore, si scarta a parità di
   valore, e la rarità decide solo a parità di valore;
7. arma e armatura conservano il criterio per attacco/difesa con rarità come spareggio
   (regressione);
8. `ItemSnapshot` porta i bonus dell'oggetto e `HeroSnapshot` porta caratteristiche base ed
   effettive; `ProgressChronicle` non porta più `jewelBonusPoints`;
9. il JSON servito espone le chiavi nuove ai livelli in cui il JavaScript le legge e non espone più
   `jewelBonusPoints`;
10. la procedura di fine scontro in console descrive l'oggetto trovato e quello lasciato coi loro
    bonus, e le due frasi del gioiello non parlano più di punti caratteristica;
11. la scheda del combattente in console mostra i bonus dell'equipaggiamento;
12. *(verifica manuale)* il riquadro in alto della pagina mostra la scheda d'ingresso fino alla
    procedura della prima prova e poi la scheda dell'ultima prova conclusa, con l'intestazione
    coerente; la scheda duplicata nel pannello della procedura non c'è più; i bonus compaiono negli
    oggetti e nelle caratteristiche;
13. *(verifica manuale, confrontando due partite di lunghezza diversa)* il percorso continua a non
    anticipare dove finisce la corsa;
14. comportamento preesistente invariato dove richiesto: forma degli scontri, percorso, tabelle di
    rarità del loot, esiti, struttura dei momenti;
15. nessuna modifica non richiesta a motore, contratti o altri moduli.

**Esempio** (istanza concreta — solo illustrativo)

```java
// Il punto unico: caratteristiche base + buff dell'equipaggiamento = caratteristiche effettive.
// Vive in combat.hero perché lo usano sia combat.factory (per assemblare il Fighter) sia
// combat.chronicle (per fotografare il protagonista), e nessuno dei due deve dipendere dall'altro.
public final class EquipmentBonus {

  public static Map<Characteristic, Integer> totalOf(Collection<BuffElement> buffs) {
    Map<Characteristic, Integer> byCharacteristic = new EnumMap<>(Characteristic.class);
    buffs.forEach(buff -> byCharacteristic.merge(buff.characteristic(), buff.value(), Integer::sum));
    return byCharacteristic;
  }

  /**
   * Il personaggio con addosso i bonus: il CharacterResult si ricostruisce a mano, come già fa
   * HeroBrain.grow, perché il toolkit non espone API per far crescere un personaggio esistente.
   * Un bonus su una caratteristica che il personaggio non ha viene ignorato: la scheda resta la sua.
   */
  public static CharacterResult applyTo(CharacterResult base, Collection<BuffElement> buffs) {
    Map<Characteristic, Integer> bonusByCharacteristic = totalOf(buffs);
    List<CharacterCharacteristic> effective = base.characteristics().stream()
        .map(entry -> new CharacterCharacteristic(entry.characteristic(),
            entry.value() + bonusByCharacteristic.getOrDefault(entry.characteristic(), 0)))
        .toList();

    return new CharacterResult(base.race(), base.characterClass(), base.name(), effective);
  }
}

// Hero: dato derivato, risolto alla lettura e non custodito accanto alle fonti.
public CharacterResult effectiveCharacter() {
  return EquipmentBonus.applyTo(character, equippedBuffs());   // arma + armatura + gioielli
}

// FighterFactory.summon: al motore vanno le caratteristiche effettive, non quelle base.
// Il motore resta intatto: non sa che esistano i buff, riceve numeri già sommati.
public Fighter summon(Hero hero) {
    return assembler.assemble(hero.effectiveCharacter(), hero.weapon(), hero.armourPieces(), null);
}

// HeroBrain: il gioiello ha finalmente dei numeri, quindi il suo comparatore prende la forma
// degli altri due — valore come criterio, rarità come spareggio.
private static final Comparator<JewelResult> BY_JEWEL_VALUE = Comparator
    .comparingInt(jewel -> EquipmentBonus.totalValueOf(jewel.buffs()))
    .thenComparingInt(jewel -> jewel.rarity().ordinal());
```

```javascript
// app.js, sezione 2: la scheda corrente è un dato del momento, non un calcolo del rendering.
// Si accumula scorrendo le prove già concluse, senza mai guardare quelle future.
function buildMoments(chronicle) {
  let currentHero = chronicle.protagonist;
  let completedTrials = 0;
  // Per ogni prova: i momenti di setup e di passo nascono con { hero: currentHero, completedTrials }.
  // Il momento di procedura è quello in cui la crescita accade, quindi porta già la scheda nuova:
  //   currentHero = trial.progress.heroAfter; completedTrials += 1;
  // e solo dopo si costruisce quel momento. La conclusione porta l'ultima scheda conosciuta.
}
```
