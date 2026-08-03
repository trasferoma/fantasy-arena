## Imposrtanti e strutturali
- HeroBrain ha dentro cose che non centrano con il cervello dell'eroe come ad esempio le rarity table

## Nuove idee
- morte definitiva e salvataggio della progressione fra una partita e l'altra. La cronaca
  (`ArenaChronicle`) è già un registro di soli dati che si serializza in JSON senza annotazioni: è il
  formato di persistenza naturale, e non va inventato un secondo
- arena a lunghezza variabile. Le prove sono dieci e non più cablate nella scansione: vivono come dato in
  `TrialPlan`, una tabella di stazioni che `Arena` legge senza contenerla. Renderla parametrica —
  scelta dall'utente o da riga di comando — è quindi diventato un lavoro piccolo: la tabella si costruisce
  già da una regola, e il campo `plannedTrials` della cronaca arriva dalla sua dimensione, non da una
  costante scritta due volte
- **ribilanciare la profondità del percorso** — *fatto, ma con due code aperte*. Prima: quindici corse,
  undici cadute alla prima prova, due alla terza, **nessuna oltre**. Dopo il ribilanciamento
  (`spec-bilanciamento-progressione.md`), misurato su cinquecento corse: **232 su 500 vanno oltre la
  terza prova** e 184 arrivano in fondo, con 171 trionfi. Le leve mosse sono state quattro — monte
  punti di squadra invece che per singolo sfidante, sconto legato alla fortuna, rarità del loot più
  conservativa presto, pareggio che non chiude più la corsa. Restano due cose da decidere guardando il
  gioco, non il codice:
  - **la distribuzione è bimodale**: 184 corse muoiono alla prima prova e 184 arrivano alla decima,
    con poco in mezzo. Delle corse che superano la prima prova, il 58% arriva in fondo. La corsa è di
    fatto decisa al primo scontro: è una forma difendibile — un filtro netto in apertura — ma è una
    scelta, e va fatta apposta
  - **lo sconto della fortuna è forte**: dodici punti medi tolti a monti che vanno da 15 a 59, col
    pavimento raggiunto in una prova su sette. È il rischio dichiarato quando si è scelto di leggere
    la fortuna **effettiva** (coi buff dell'equipaggiamento) invece di quella base, ora quantificato.
    Le vie sono tornare alla base, dimezzare lo sconto, metterci un tetto, o tenerlo così
- **ribilanciare lo sfidante speculare della prova 10.** Nasce dai punti **base** del protagonista, coi
  bonus di razza e classe disattivati e un'arma solo `RARE`, mentre a quel punto il protagonista ha
  equipaggiamento `EPIC` o `LEGENDARY` coi relativi buff: la prova finale è verosimilmente più facile
  delle prove 7-9, cioè il percorso si sgonfia proprio alla fine. Correggerlo vuol dire decidere se lo
  specchio debba pareggiare le caratteristiche **effettive**, che è una scelta di bilanciamento a sé
- uso degli slot ancora scoperti: scudo e pozioni del toolkit non entrano mai in gioco. I gioielli
  ci entrano come loot e si indossano uno per tipo; il motore continua a non saperli montare sul
  `Fighter`, ma i loro buff contano lo stesso, perché le caratteristiche passate all'assemblatore
  sono già quelle effettive — arma, armatura e gioielli compresi. Resta scoperto il **debuff**, che
  il toolkit dichiara di non generare ancora
- scelte del protagonista guidate dall'utente invece che da `HeroBrain`. Da qui in avanti costa più di
  prima: le scelte andrebbero chieste in *entrambe* le modalità di presentazione, e in quella web
  romperebbero il fatto su cui poggia tutto il disegno — che una partita è determinata nell'istante in
  cui viene giocata, quindi la pagina può leggere una cronaca già scritta invece di pilotare un motore.
  Con le scelte dell'utente la pagina diventerebbe un client interattivo e servirebbe un endpoint di
  scrittura
- modalità "tanti scontri in fila" con statistiche di bilanciamento. È il compito più vicino a essere
  fatto: `SilentArenaRun` già gioca un'arena intera senza stampare e senza attendere, e restituisce una
  cronaca. Mancano solo il ciclo, l'aggregazione e la presentazione dei numeri
- **la console come lettrice della cronaca.** Oggi il percorso console compone le sue frasi interrogando
  i propri collaboratori mentre la partita avanza, e la cronaca la costruisce in parallelo senza
  leggerla. Farlo leggere dalla cronaca unificherebbe i due percorsi e renderebbe la console sostituibile
  come la pagina. Il requisito che rende possibile questo lavoro è già stato rispettato: la cronaca è
  **autosufficiente**, cioè porta in forma di dati tutto quello che la console ricava da sé — numero e
  descrizione della prova, composizione degli schieramenti, esito, procedura di fine scontro fino ai
  bonus che ogni oggetto porta e alle caratteristiche del protagonista in doppia lettura, base ed
  effettive. Il criterio da non tradire: un lettore nuovo deve poter comporre frasi, non aggiungere
  campi
- **quello che la cronaca contiene e la pagina web non mostra**, e che quindi è lavoro di solo frontend,
  senza toccare una riga di Java: il dettaglio d'iniziativa (`InitiativeReport` coi suoi breakdown), le
  `Scorecard` della decisione ai punti, i `TurnHighlight` come marcatori dei momenti da ricordare, i
  punteggi di squadra. Sono stati lasciati fuori di proposito per tenere la prima pagina al minimo utile
- test automatici della pagina web. Oggi non ce ne sono, ed è una scelta dichiarata (nessun build step,
  nessun runner JS, nessuna dipendenza): il contratto verso il JavaScript è presidiato dal test sulle
  chiavi del JSON, il resto si verifica a mano. Da rifare da zero come decisione se la pagina cresce
