# Combat Engine / Arena — spiegazione semplice

Questo documento spiega, nel modo più semplice possibile, **cosa** è stato realizzato e **come** funziona. Non serve conoscere Java per leggerlo.

---

## In una frase

Abbiamo costruito un'**Arena**: un piccolo simulatore che fa combattere da soli due personaggi
generati dalla libreria `fantasytoolkit`, turno dopo turno, fino a quando uno dei due vince.
Serve a capire se i personaggi generati sono **equilibrati** (né troppo forti né troppo deboli).

Non è un videogioco: nessun giocatore preme tasti. Si lancia, i due combattenti se le danno da
soli, e alla fine viene stampato il resoconto del duello.

---

## Cosa succede quando lo avvii

Avviando il programma vedi una cosa così:

```
Turno 1: Draven attacca Cassius con SWORD, colpo a segno (20 danni).
Turno 2: Cassius attacca Draven con SWORD, colpo a segno (25 danni).
Turno 3: Draven attacca Cassius con SWORD, parato (5 danni).
...
=== Esito del duello ===
Vince: Cassius (10 turni)
```

Ogni riga è un turno. Alla fine c'è il vincitore e quanti turni sono serviti.

---

## L'idea di fondo: due mondi separati

La cosa più importante da capire è che teniamo separate **due categorie di cose**.

### 1) Quello che il personaggio "è" (permanente)

Sono le sue qualità fisse, decise una volta e mai più cambiate durante il duello:

- **Offensive Rating** — quanto è bravo ad attaccare (dipende da forza, agilità, dall'arma, dalla classe, dalla razza).
- **Defensive Rating** — quanto è bravo a difendersi (dipende da resistenza, agilità, dall'armatura...).
- **Vita massima** e **Stamina massima** — calcolate dalle sue caratteristiche.

Questi valori sono come l'altezza o il colore degli occhi: non cambiano mentre combatte.

### 2) Quello che al personaggio "succede" (temporaneo)

È lo stato che cambia colpo dopo colpo:

- **Vita attuale** — scende quando prende danni; a zero, ha perso.
- **Stamina attuale** — l'energia; si consuma facendo azioni.
- **Momentum** — lo "slancio" psicologico del momento (spiegato sotto).

Mescolare questi due mondi sarebbe un errore: un personaggio stanco o sfiduciato **colpisce
peggio**, ma questo non cambia quanto **vale** davvero. Per questo li teniamo distinti.

---

## I tre ingredienti del combattimento

### La Stamina (l'energia)

Non è una seconda barra della vita. **Non cala** solo perché passa il tempo: cala perché
**fai azioni** (attaccare, parare, schivare) e un pochino quando incassi un colpo.

Quando la stamina scende sotto certe soglie, il personaggio si affatica e comincia a
**colpire e difendersi peggio**. È il modo per premiare chi gestisce bene le energie.

### Il Momentum (lo slancio)

È un numero che va da **-100 a +100** e rappresenta l'inerzia del combattimento.
- **Sale** quando le cose vanno bene: colpisci, fai un critico, pari, schivi.
- **Scende** quando vanno male: subisci colpi o manchi l'attacco.

Chi è in slancio positivo combatte un po' meglio, chi è in negativo un po' peggio. Ma l'effetto
è **volutamente limitato (massimo ±15%)**: così chi è in vantaggio non travolge automaticamente
l'altro (niente "effetto valanga").

### I dadi (il caso)

Come in un gioco di ruolo da tavolo, il caso decide se un colpo va a segno, se viene schivato o
parato, se è un critico. Questo lo fanno i **dadi** (es. un dado a 20 facce, il "d20").

---

## Come si svolge un turno (passo per passo)

Ad ogni turno, chi attacca prova a colpire e chi difende prova a evitare il colpo:

1. **Chi tocca?** All'inizio si tira per l'iniziativa: chi ha il tiro migliore attacca per primo.
2. **L'attaccante prova a colpire** — si tira un dado. Se il tiro riesce, il colpo va a segno
   (e un tiro perfetto è sempre un **colpo critico**).
3. **Il difensore prova a difendersi** — si tira un dado per **schivare** o **parare**.
4. **Si calcola il danno** — dall'attacco dell'uno meno la difesa dell'altro, aggiustato da
   momentum, stamina e da un pizzico di variabilità casuale. Una parata riduce molto il danno,
   un critico lo aumenta.
5. **Si applica il danno** alla vita del difensore.
6. **Si aggiornano stamina e momentum** di entrambi.
7. **Cambio turno**: ora tocca all'altro.
8. Si ripete finché uno finisce la vita. Se dopo tanti turni nessuno cade, vince chi ha più vita
   in percentuale (e se sono pari è **pareggio**). C'è un tetto massimo di turni, quindi il duello
   non può mai andare avanti all'infinito.

---

## Com'è organizzato "dentro" (in parole povere)

Il codice è costruito come una **catena di responsabilità**, dall'alto (che racconta la storia)
al basso (che fa i conti):

```
Main            →  accende tutto, non fa altro che chiamare l'Arena
  Arena         →  "prepara i combattenti", "disputa il duello", "riporta l'esito"
    CombatEngine   →  gestisce l'iniziativa e il susseguirsi dei turni
      TurnOrchestrator →  gestisce un singolo turno: tira i dadi e chiede i risultati
        ── qui vivono i CONTI veri ──
        HitResolver / DefenseResolver  →  "ha colpito?", "ha schivato/parato?"
        DamageCalculator               →  "quanti danni?"
        MomentumRules / StaminaRules   →  regole di slancio ed energia
        DefaultRatingStrategy          →  le formule che danno i valori del personaggio
```

Due principi guidano questa struttura:

- **In alto si legge come un racconto.** I livelli superiori (Arena, motore, turno) usano nomi
  parlanti — "prepara i combattenti", "gioca il turno" — e **non contengono formule**: si limitano
  a coordinare. Chi legge capisce *cosa* succede senza annegare nei numeri.

- **I conti stanno solo in fondo, e sono "puri".** Le formule vivono tutte nei livelli più bassi.
  Questi calcolatori sono **funzioni pure**: ricevono i risultati dei dadi **già lanciati** e
  restituiscono un esito, senza tirare dadi da soli e senza toccare nient'altro. Il vantaggio è
  enorme per **testare**: dando lo stesso tiro di dado in ingresso, ottieni sempre lo stesso
  risultato, in modo prevedibile.

In pratica: **i dadi si tirano "fuori"** (nel livello che orchestra) e il **risultato viene passato
dentro** ai calcolatori. Così il caso è isolato in un unico punto e tutto il resto è ripetibile.

---

## Un dettaglio utile: i combattenti se li costruisce l'Arena

La libreria `fantasytoolkit` genera un personaggio con nome, razza, classe e caratteristiche, ma
**senza equipaggiamento**. Quindi è l'Arena che, per ogni duellante, prende un personaggio e gli
assegna un'arma (in questa prima versione una **spada**) e un'armatura, mettendo insieme il
"combattente" completo.

---

## Come si lancia

Dalla cartella del progetto:

```bash
mvn compile        # compila
mvn exec:java      # lancia un duello e stampa il resoconto
mvn test           # esegue i test automatici (11, tutti verdi)
```

---

## Cosa c'è e cosa (ancora) no

**C'è (versione 1):**
- Due guerrieri con spada che duellano 1 contro 1.
- Rating permanenti separati dallo stato temporaneo.
- Momentum, stamina con affaticamento, colpi/critici/schivate/parate.
- Resoconto turno per turno ed esito finale.
- Test automatici che verificano le regole.

**Non c'è ancora (previsto per dopo):**
- **Taratura dei numeri**: i pesi delle formule (quanto conta la forza, quanti danni, ecc.) sono
  valori di partenza da affinare provando molti duelli. Sono tutti raccolti in un unico punto
  (`CombatSettings`) proprio per poterli ritoccare facilmente.
- **Modalità "tanti duelli in fila"** con statistiche, che è il vero strumento per misurare il
  bilanciamento (previsto come prossimo passo).
- Abilità di classe, magie, effetti di stato, terreno e meteo: l'architettura è già predisposta
  per aggiungerli in futuro **senza riscrivere il motore**.

---

## In sintesi

Abbiamo un motore di duello **semplice, ordinato e verificabile**: racconta chiaramente cosa
succede ad ogni turno, tiene separato ciò che un personaggio *è* da ciò che gli *succede*, isola
il caso (i dadi) in un punto solo e mette tutti i numeri regolabili in un posto solo. È la base
solida su cui, poco per volta, si potranno aggiungere meccaniche più ricche.
