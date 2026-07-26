# fantasy-arena

Il gioco: combattenti generati a caso si affrontano nell'arena, round dopo round, fino a decretare
un vincitore. Le regole del combattimento vengono da `fantasy-combat-system`.

Questo documento spiega, nel modo più semplice possibile, **cosa** fa e **come** è organizzato. Non
serve conoscere Java per leggerlo. Le istruzioni operative per chi lavora sul codice stanno in
`CLAUDE.md`.

---

## In una frase

`fantasy-arena` è un piccolo **gioco da console**: genera dei combattenti, li manda a combattere e
racconta a schermo com'è andata, round per round, fino a decretare chi vince.

Nessun giocatore preme tasti per combattere: si scelgono quanti combattenti per parte, e poi si
guarda. L'INVIO serve solo ad avanzare di un turno o di un round, come sfogliare le pagine di un
racconto.

---

## Le regole del combattimento non stanno qui

Questa è la cosa più importante da capire di com'è fatto il progetto.

**Le regole di combattimento vivono in un progetto separato**, `fantasy-combat-system`, usato qui
come dipendenza. Là dentro c'è tutto ciò che riguarda il *come si combatte*: chi attacca per primo,
se il colpo va a segno, quanti danni fa, la stamina che si consuma, lo slancio, il colpo potente,
chi affronta chi in una battaglia a squadre.

Il motore ha una caratteristica precisa: **non stampa niente**. Gioca l'intero scontro e restituisce
il resoconto completo. Poi tocca a noi.

Quindi questo progetto si occupa di **tre cose**:

1. **Generare i combattenti** — pescare personaggi, armi e armature dalla libreria `fantasytoolkit`.
2. **Chiedere lo scontro** al motore.
3. **Mostrarlo** — schede dei combattenti, turno per turno, scene ASCII della battaglia, esito
   finale. Tutta la parte visiva è nostra.

Se cerchi la spiegazione delle regole (stamina, momentum, come si calcolano i danni), è nel
`README.md` di `fantasy-combat-system`. La guida alla taratura dei numeri è nel suo
`combatSettings.md`.

---

## Cosa succede quando lo avvii

Il programma genera **un protagonista** e gli fa affrontare tre prove in fila:

1. **un avversario solo**, equipaggiato come lui;
2. **due avversari insieme**, contro di lui;
3. **uno sfidante speculare**: i suoi stessi punti caratteristica, altrettanti pezzi d'armatura, ma
   un'arma rara in pugno.

Ogni prova va vinta per intero — restare in piedi non basta, gli avversari devono essere tutti a
terra — altrimenti l'arena si chiude lì.

Fra una prova e l'altra c'è la **procedura di fine scontro**, raccontata riga per riga: il
protagonista torna a vita e stamina piene, può sostituire la sua arma con quella di un avversario
caduto se è migliore, raccoglie i pezzi d'armatura che coprono parti del corpo scoperte o che
difendono più dei suoi, e riceve tre punti caratteristica distribuiti a caso.

```
--- PROCEDURA DI FINE SCONTRO ---
Morthas è ancora in piedi: vita e stamina tornano piene.
Arma: lascia STAFF (UNCOMMON, atk 3) e impugna BOW (UNCOMMON, atk 5).
Armatura: raccoglie BELT (UNCOMMON, def 2), parte del corpo prima scoperta.
Crescita: +2 STRENGTH, +1 LUCK.
```

Le prime due prove — anche quella contro un avversario solo — sono mostrate come una battaglia round
per round, disegnata come una scena:

```
=========================== Round 5 ============================

Grubnar                                     Nazgrom [a terra]
Vita:    38/38           colpisce (15)      Vita:    0/41 (-15)
Stamina: 16/22 (-6)   ===================>  Stamina: 4/19 (-2)

=== Esito della battaglia ===
Vince: Squadra 1 (5 round)
```

La prova finale no: essendo un uno-contro-uno, usa il duello a schermate, turno per turno. È la
presentazione più curata delle due, e serve anche a far capire a colpo d'occhio che quello è lo
scontro che chiude l'arena.

---

## Com'è organizzato "dentro" (in parole povere)

```
Main                    →  apre l'arena e si fa da parte
  Arena                 →  le tre prove in fila e la procedura di fine scontro
    FighterFactory      →  "genera i combattenti": razza, nome, arma, armatura
    Hero                →  la scheda del protagonista, quella che sopravvive ai round
    HeroBrain           →  tutte le sue scelte: cosa raccoglie, dove spende i punti
    MatchRunner         →  chiede un singolo scontro al motore e lo mette in scena
      CombatSystem      →  ⟵ qui comincia l'altro progetto: le REGOLE
      io/...            →  logger, renderer, scene ASCII, attesa dell'INVIO
```

Il confine è netto: sopra la riga di `CombatSystem` c'è il gioco, sotto ci sono le regole. Il gioco
non sa come si calcola un danno; il motore non sa che esiste uno schermo, né che esista una
progressione.

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
mvn compile        # compila
mvn exec:java      # avvia il gioco
mvn test           # esegue i test automatici
```

Serve che `fantasy-combat-system` e `fantasytoolkit` siano stati installati (`mvn install`) dai
rispettivi progetti: non si scaricano da internet.

---

## Cosa c'è e cosa (ancora) no

**C'è:**
- Generazione di combattenti equi-equipaggiati, con nomi univoci.
- Duello 1v1 a schermate e battaglia a squadre (NvN) con scena ASCII round per round.
- Schede dei combattenti, pronostico alla vigilia, esito finale e momenti da ricordare.

**Non c'è ancora:**
- **Progressione e avanzamento**: i combattenti nascono, combattono, vincono o muoiono, ma non
  imparano niente. Chi sopravvive dovrebbe migliorare, ed è il prossimo passo.
- **Modalità "tanti scontri in fila"** con statistiche, il vero strumento per misurare il
  bilanciamento.
