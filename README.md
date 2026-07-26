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

Il programma chiede quanti combattenti per fazione, poi genera i combattenti e mostra le loro schede.

Con **1 contro 1** vedi il duello turno per turno, a schermate. Con qualunque altra numerosità
(2 vs 2, 3 vs 1, ...) vedi la battaglia round per round, disegnata come una scena:

```
=========================== Round 5 ============================

Grubnar                                     Nazgrom [a terra]
Vita:    38/38           colpisce (15)      Vita:    0/41 (-15)
Stamina: 16/22 (-6)   ===================>  Stamina: 4/19 (-2)

=== Esito della battaglia ===
Vince: Squadra 1 (5 round)
```

---

## Com'è organizzato "dentro" (in parole povere)

```
Main                    →  chiede quanti combattenti, genera, delega all'Arena
  FighterFactory        →  "genera i combattenti": razza, nome, spada, corazza
  Arena                 →  chiede lo scontro al motore e lo mostra col ritmo giusto
    CombatSystem        →  ⟵ qui comincia l'altro progetto: le REGOLE
    io/...              →  logger, renderer, scene ASCII, attesa dell'INVIO
```

Il confine è netto: sopra la riga di `CombatSystem` c'è il gioco, sotto ci sono le regole. Il gioco
non sa come si calcola un danno; il motore non sa che esiste uno schermo.

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
