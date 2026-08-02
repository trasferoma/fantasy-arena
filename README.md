# fantasy-arena

Il gioco: combattenti generati a caso si affrontano nell'arena, round dopo round, fino a decretare
un vincitore. Le regole del combattimento vengono da `fantasy-combat-system`.

Questo documento spiega, nel modo più semplice possibile, **cosa** fa e **come** è organizzato. Non
serve conoscere Java per leggerlo. Le istruzioni operative per chi lavora sul codice stanno in
`CLAUDE.md`.

---

## In una frase

`fantasy-arena` è un piccolo **gioco da guardare**: genera un protagonista, gli fa affrontare tre
prove e racconta com'è andata, passo per passo, fino al trionfo o alla caduta.

Nessun giocatore preme tasti per combattere: si guarda. E si può guardare in **due modi** — nel
terminale, dove l'INVIO serve solo ad avanzare di un turno o di un round come sfogliare le pagine di
un racconto, oppure in un **browser**, dove ci sono i comandi che un terminale non può avere: play,
pausa, avanti, indietro, velocità, e il salto a un punto qualsiasi della partita.

---

## Le regole del combattimento non stanno qui

Questa è la cosa più importante da capire di com'è fatto il progetto.

**Le regole di combattimento vivono in un progetto separato**, `fantasy-combat-system`, usato qui
come dipendenza. Là dentro c'è tutto ciò che riguarda il *come si combatte*: chi attacca per primo,
se il colpo va a segno, quanti danni fa, la stamina che si consuma, lo slancio, il colpo potente,
chi affronta chi in una battaglia a squadre.

Il motore ha una caratteristica precisa: **non stampa niente**. Gioca l'intero scontro e restituisce
il resoconto completo. Poi tocca a noi.

Quindi questo progetto si occupa di **quattro cose**:

1. **Generare i combattenti** — pescare personaggi, armi e armature dalla libreria `fantasytoolkit`.
2. **Chiedere lo scontro** al motore.
3. **Far crescere il protagonista** fra una prova e l'altra: cosa tiene di ciò che trova, dove spende i
   punti. Il motore non sa niente di progressione.
4. **Mostrare** — schede dei combattenti, passo per passo, esito finale. Tutta la parte visiva è nostra,
   e da qui in poi ce n'è più di una: il terminale e la pagina web.

Se cerchi la spiegazione delle regole (stamina, momentum, come si calcolano i danni), è nel
`README.md` di `fantasy-combat-system`. La guida alla taratura dei numeri è nel suo
`combatSettings.md`.

---

## Cosa succede quando lo avvii

Il programma genera **un protagonista** e gli fa affrontare **dieci prove in fila**, un percorso che
stringe man mano:

1. **prove 1-3** — un avversario solo, equipaggiato come lui;
2. **prove 4-6** — due avversari insieme, contro di lui;
3. **prove 7-9** — tre insieme;
4. **prova 10** — **uno sfidante speculare**: i suoi stessi punti caratteristica, altrettanti pezzi
   d'armatura, ma un'arma rara in pugno.

E non stringe solo di numero: gli avversari generati nascono ogni volta con qualche punto
caratteristica in più del gruppo precedente, così la decima prova non è la prima ripetuta dieci volte.

Ma quei punti sono il monte di **tutto lo schieramento**, non di ciascun avversario: due nemici se li
dividono, tre pure. È la cosa che rende la progressione percepibile. Quando il monte era del singolo,
cresceva con la stessa curva del protagonista e veniva moltiplicato per il numero di avversari: i tre
punti che ogni vittoria vale erano annullati prima ancora di essere spesi, e alla quarta prova si
combatteva contro il doppio dei propri punti. E il monte dello schieramento non è nemmeno il numero
puro di avversari moltiplicato per la crescita del protagonista, ma qualcosa di meno: due avversari
attaccano due volte per turno mentre il protagonista attacca una volta sola, e quel vantaggio va
pagato in punti.

C'è poi una cosa che la **fortuna** fa, e che nel motore non farebbe: toglie punti allo schieramento
avversario, tanti quanti la fortuna del protagonista, per ogni avversario in campo. Nel combattimento
la fortuna vale solo un punto percentuale di critico in più, cioè quasi niente; qui diventa la
statistica che rende gli avversari più deboli. C'è un limite sotto il quale il taglio non scende —
sette punti a testa, il minimo con cui un personaggio può esistere — e capita che lo si raggiunga.

Vincere una prova per intero — tutti gli avversari a terra — è l'unico modo di guadagnare l'oggetto e
i tre punti. Ma **restare in piedi senza vincere non chiude più la corsa**: si passa alla prova
successiva senza premio, con la scheda esattamente com'era. Cadere, quello sì, chiude tutto.

Fra una prova e l'altra c'è la **procedura di fine scontro**, raccontata riga per riga: il
protagonista torna a vita e stamina piene, trova **un oggetto** — un'arma, un pezzo d'armatura o un
gioiello, uno solo per prova vinta — e riceve tre punti caratteristica distribuiti a caso.

L'oggetto non si saccheggia dai caduti: si genera, e la sua qualità cresce col livello. Si tiene solo
se batte quello che il protagonista ha già, altrimenti si scarta — e anche lo scarto viene detto, perché
«non ti serve» è un'informazione, mentre il silenzio è un dubbio.

La qualità cresce, ma con misura: nella prima metà del percorso l'oggetto ordinario resta il caso più
frequente, e il leggendario è un'eccezione vera. Non è prudenza fine a sé stessa. Un'arma leggendaria
colpisce quattro o cinque volte più forte di quella con cui si comincia, e porta bonus per una decina
di punti caratteristica: trovarne una alla seconda prova valeva più di tre vittorie messe insieme, e
la corsa smetteva di essere una progressione per diventare un'estrazione.

Ogni oggetto porta con sé dei **bonus alle caratteristiche**, tanto più generosi quanto più è raro, e
valgono **finché lo si tiene addosso**: cambiare arma significa cambiare anche i suoi bonus, e il
gioiello — che il motore non sa montare sul combattente — conta comunque, perché i bonus entrano nelle
caratteristiche con cui il protagonista scende in campo. Vale per tutti, non solo per lui: anche gli
sfidanti nascono con l'equipaggiamento che portano.

```
--- PROCEDURA DI FINE SCONTRO ---
Morthas è ancora in piedi: vita e stamina tornano piene.
Arma: trovi HAMMER (EPIC, atk 13), lasci BATTLEAXE (UNCOMMON, atk 3) e la impugni.
Bonus dell'oggetto trovato: +5 STRENGTH, +3 RESISTANCE.
Bonus dell'oggetto lasciato: +2 AGILITY.
Crescita: +2 STRENGTH, +1 LUCK.
```

Nel terminale, le prove contro più di un avversario — dalla quarta alla nona — sono mostrate come una
battaglia round per round, disegnata come una scena:

```
=========================== Round 5 ============================

Grubnar                                     Nazgrom [a terra]
Vita:    38/38           colpisce (15)      Vita:    0/41 (-15)
Stamina: 16/22 (-6)   ===================>  Stamina: 4/19 (-2)

=== Esito della battaglia ===
Vince: Squadra 1 (5 round)
```

Le prove uno-contro-uno no — le prime tre e quella dello specchio: usano il duello a schermate, turno
per turno. È la presentazione più curata delle due. Quale delle due si vede non è una scelta scritta
prova per prova: dipende soltanto da quanti avversari ci sono, uno o più di uno.

---

## La stessa partita, nel browser

Lanciando il gioco con l'argomento `web` non si vede nessuno scontro sul terminale: esce **solo** un
indirizzo da aprire. La partita si guarda nella pagina, con due colonne per i due schieramenti e in
mezzo **chi attacca chi** — una freccia orientata come nella scena del terminale, con la formula breve
del colpo sopra — le barre di vita e stamina che si muovono a ogni passo, una **stellina** accanto al
nome di chi ha l'iniziativa in quel passo, e in basso una barra dei comandi con la timeline.

Sopra il campo c'è il **percorso**: dieci stazioni in fila, quelle camminate piene, quella corrente
marcata, quelle che mancano spente. Si clicca su una stazione già passata per rivedere quella prova. E
c'è una cosa che il percorso fa con cura: **non dice dove finisce la corsa**. Una stazione che il
protagonista non ha mai raggiunto è disegnata e si comporta esattamente come una che non ha *ancora*
raggiunto, perché sapere in anticipo di essere alla penultima prova toglierebbe il gusto di guardare.

Perché è possibile una cosa che il terminale non può fare? Perché **il gioco non ha scelte del
giocatore**. Il protagonista decide da sé cosa raccogliere e dove spendere i punti, quindi una partita è
**completamente determinata nell'istante in cui viene giocata**: non c'è niente da chiedere a nessuno
mentre va avanti. E allora la pagina non deve pilotare il gioco turno per turno — le basta ricevere in
un colpo solo la **cronaca** di una partita già finita, e leggerla avanti e indietro a piacere.

È il motivo per cui esistono cose che a un terminale non servirebbero:

- **La cronaca.** Ogni partita, in *entrambe* le modalità, produce il registro completo di com'è andata:
  chi è entrato in arena, cosa è accaduto a ogni passo di ogni prova, cosa ha trovato dopo ogni vittoria,
  come è finita. Sono soli dati, senza una parola di racconto dentro: le frasi le compone chi legge. Si
  costruisce sempre, anche quando nessuno la guarda, e questo è voluto — se ci fosse una strada «per il
  web» separata da quella della console, le due potrebbero raccontare cose diverse senza che nessuno se
  ne accorga.
- **Una partita giocata in silenzio.** Per servire la pagina serve un modo di giocare l'intera arena
  senza stampare niente e senza aspettare l'INVIO. Sono millisecondi, quindi ogni apertura della pagina
  è una **partita nuova**: il pulsante «gioca ancora» è il tasto di ricarica del browser. Due schede
  aperte guardano due partite diverse.

Il server è quello minimo del JDK, ascolta **solo** sul computer locale e risponde a quattro richieste e
nient'altro: la pagina, il suo foglio di stile, il suo script, e la cronaca della partita. Qualunque altro
indirizzo è un «non trovato». Non c'è niente da scrivere e niente da mandargli: la pagina si guarda.

La pagina è HTML, CSS e JavaScript scritti a mano — nessuna libreria, niente da scaricare, nessuna
compilazione. Non riproduce le scene ASCII del terminale: erano proprio il limite che questa modalità
esiste per superare.

---

## Com'è organizzato "dentro" (in parole povere)

```
Main                    →  chiede quale modalità e si fa da parte
  UiMode                →  console, oppure web (con la porta)
  Arena                 →  scandisce il percorso e la procedura di fine scontro,
                           e restituisce la CRONACA di com'è andata
    TrialPlan           →  il percorso come DATO: dieci stazioni, chi ti aspetta a ognuna
    FighterFactory      →  "genera i combattenti": razza, nome, arma, armatura
    Hero                →  la scheda del protagonista, quella che sopravvive ai round
    HeroBrain           →  tutte le sue scelte: cosa tiene, dove spende i punti
    MatchRunner         →  chiede un singolo scontro al motore e ne restituisce l'esito
      CombatSystem      →  ⟵ qui comincia l'altro progetto: le REGOLE
      MatchPresentation →  come mostrarlo: a schermo, oppure in silenzio

i due lettori della cronaca, che non si conoscono fra loro:
  io/...                →  console: logger, renderer, scene ASCII, attesa dell'INVIO
  io/web/...            →  server minimo + la pagina che riproduce la cronaca
```

Il confine è netto: sopra la riga di `CombatSystem` c'è il gioco, sotto ci sono le regole. Il gioco
non sa come si calcola un danno; il motore non sa che esiste uno schermo, né che esista una
progressione.

C'è un secondo confine, più recente: fra il **giocare** e il **mostrare**. `Arena` e `MatchRunner`
giocano e restituiscono i fatti; *come* e *quando* mostrarli è di qualcun altro, che si può sostituire —
ed è per questo che la stessa partita può finire su un terminale o in una pagina web. I due lettori si
ignorano a vicenda di proposito: la parte web non sa niente di logger e scene ASCII, e la parte console
non sa che esista un server. Così si può cambiare l'uno senza rompere l'altro.

Una distinzione che vale la pena capire: il **protagonista** (`Hero`) e il **combattente**
(`Fighter`) non sono la stessa cosa. `Hero` è la scheda — chi è, cosa impugna, cosa indossa — e
sopravvive a tutte le prove. `Fighter` è chi scende in campo in un singolo scontro, e ne esce
ferito. A ogni prova il protagonista viene *rimandato in campo* come combattente nuovo costruito
dalla stessa scheda: è da lì che arriva la cura completa promessa dalla procedura di fine scontro.
Nessuno guarisce nessuno, si torna in campo interi.

---

## Un dettaglio utile: chi costruisce i combattenti

La libreria `fantasytoolkit` genera un personaggio con nome, razza, classe e caratteristiche, ma
**senza equipaggiamento**. È `FighterFactory` che gli assegna un'arma (in questa versione una
**spada**) e un'armatura, con una regola di equità: la rarità dell'equipaggiamento è estratta una
volta sola e condivisa da tutti, così nessuno parte avvantaggiato. I nomi vengono resi univoci,
perché il resoconto identifica i combattenti per nome.

Il passaggio finale — tradurre caratteristiche ed equipaggiamento nei valori di combattimento — è
del motore (`FighterAssembler`): quanto conta la forza rispetto all'arma è una regola di
combattimento, non un dettaglio di generazione.

---

## Come si lancia

Dalla cartella del progetto:

```bash
mvn compile                            # compila
mvn exec:java                          # gioca nel terminale, come sempre
mvn exec:java -Dexec.args="web"        # apre il server e stampa l'indirizzo (porta 8080)
mvn exec:java -Dexec.args="web 9000"   # come sopra, su una porta scelta
mvn test                               # esegue i test automatici
```

Senza argomenti il gioco si comporta **esattamente** come prima che la modalità web esistesse. Con `web`
non si apre nessun browser da solo: si stampa l'indirizzo e lo si apre a mano. Se la porta è già occupata
l'avvio si ferma e lo dice, invece di spostarsi in silenzio su un'altra: l'indirizzo stampato deve
restare quello che ti aspetti.

Serve che `fantasy-combat-system` e `fantasytoolkit` siano stati installati (`mvn install`) dai
rispettivi progetti: non si scaricano da internet. La terza dipendenza, Jackson, che traduce la cronaca
in JSON per la pagina, si scarica normalmente.

---

## Cosa c'è e cosa (ancora) no

**C'è:**
- Generazione di combattenti equi-equipaggiati, con nomi univoci.
- Duello 1v1 a schermate e battaglia a squadre (NvN) con scena ASCII round per round.
- Schede dei combattenti, esito finale e momenti da ricordare.
- **Progressione**: dieci prove in fila — descritte come un dato, non cablate nel codice che le scandisce
  — e fra l'una e l'altra cura completa, un oggetto trovato e tre punti caratteristica. Chi sopravvive
  migliora davvero.
- **Le due modalità di presentazione**: il terminale e la pagina web, che leggono la stessa partita.

**Non c'è ancora:**
- **Modalità "tanti scontri in fila"** con statistiche, il vero strumento per misurare il
  bilanciamento. Ora è vicina: giocare una partita intera senza stamparla è già possibile.
- **Salvataggio della progressione** fra una partita e l'altra: ogni avvio riparte da zero.
- **Scelte del giocatore**: le decisioni del protagonista sono tutte del suo "cervello", in entrambe le
  modalità. La pagina web si guarda, non si gioca.

L'elenco completo di ciò che manca sta in `daImplementare.md`.
