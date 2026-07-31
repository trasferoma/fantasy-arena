- morte definitiva e salvataggio della progressione fra una partita e l'altra. La cronaca
  (`ArenaChronicle`) è già un registro di soli dati che si serializza in JSON senza annotazioni: è il
  formato di persistenza naturale, e non va inventato un secondo
- arena a lunghezza variabile. Le prove sono dieci e non più cablate nella scansione: vivono come dato in
  `TrialPlan`, una tabella di stazioni che `Arena` legge senza contenerla. Renderla parametrica —
  scelta dall'utente o da riga di comando — è quindi diventato un lavoro piccolo: la tabella si costruisce
  già da una regola, e il campo `plannedTrials` della cronaca arriva dalla sua dimensione, non da una
  costante scritta due volte
- **ribilanciare la profondità del percorso.** Misurato su quindici corse, undici cadono alla prima prova,
  una alla seconda, due alla terza, nessuna oltre: sei stazioni su dieci oggi non si vedono quasi mai, e
  fra queste la prova dello specchio. Non è una regressione — la prima prova è un uno-contro-uno alla pari
  come è sempre stata — ma su dieci stazioni il fatto pesa di più. Le leve stanno in due punti soli:
  `CHARACTERISTIC_POINTS_PER_VICTORY` in `HeroBrain` per la crescita del protagonista, e la curva del
  monte punti degli sfidanti in `TrialPlan` per la pressione (oggi `15 + 3 * (prova - 1)`). Da decidere
  guardando il gioco, non il codice
- uso degli slot ancora scoperti: scudo e pozioni del toolkit non entrano mai in gioco. I gioielli
  ci entrano come loot e si indossano uno per tipo, ma restano fuori dal combattimento: il motore
  non li sa montare sul `Fighter` e non conosce i loro buff/debuff, quindi il loro unico effetto
  sono i punti caratteristica extra della procedura di fine scontro
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
  punti che vale il gioiello indossato. Il criterio da non tradire: un lettore nuovo deve poter comporre
  frasi, non aggiungere campi
- **quello che la cronaca contiene e la pagina web non mostra**, e che quindi è lavoro di solo frontend,
  senza toccare una riga di Java: il dettaglio d'iniziativa (`InitiativeReport` coi suoi breakdown), le
  `Scorecard` della decisione ai punti, i `TurnHighlight` come marcatori dei momenti da ricordare, i
  punteggi di squadra. Sono stati lasciati fuori di proposito per tenere la prima pagina al minimo utile
- test automatici della pagina web. Oggi non ce ne sono, ed è una scelta dichiarata (nessun build step,
  nessun runner JS, nessuna dipendenza): il contratto verso il JavaScript è presidiato dal test sulle
  chiavi del JSON, il resto si verifica a mano. Da rifare da zero come decisione se la pagina cresce
