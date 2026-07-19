# Handoff - Combat Engine Arena

## Contesto

Sto sviluppando una libreria Java Open Source dedicata alla generazione
di contenuti fantasy.

Attualmente la libreria è in grado di generare elementi come:

-   Personaggi
-   Armi
-   Armature
-   Gioielli
-   Equipaggiamento

Per testare il bilanciamento della libreria voglio realizzare una
**Arena** nella quale i personaggi possano combattere automaticamente
tra loro.

L'obiettivo NON è realizzare un videogioco, ma un simulatore di
combattimento utile a:

-   verificare il bilanciamento della libreria;
-   assegnare ricompense ai vincitori;
-   far evolvere i personaggi nel tempo;
-   validare che le statistiche generate producano combattimenti
    credibili.

------------------------------------------------------------------------

# Modello attuale del personaggio

Ogni personaggio possiede almeno le seguenti caratteristiche:

-   Strength
-   Agility
-   Intelligence
-   Resistance
-   Stamina
-   Luck

Ogni caratteristica è rappresentata da un valore intero.

Inoltre ogni personaggio possiede:

-   Race
-   Character Class
-   Equipment

------------------------------------------------------------------------

# Filosofia del Combat Engine

L'obiettivo è mantenere il motore semplice, modulare ed estendibile.

Le caratteristiche permanenti del personaggio devono essere
completamente separate dagli eventi temporanei del combattimento.

------------------------------------------------------------------------

# Offensive Rating / Defensive Rating

È emersa l'idea di introdurre due valori "assoluti" del personaggio.

## Offensive Rating

Rappresenta la capacità offensiva permanente.

Dipende esclusivamente da:

-   caratteristiche
-   arma
-   classe
-   razza
-   bonus permanenti

NON dipende da:

-   avversario
-   terreno
-   meteo
-   fortuna
-   stato del combattimento

È una proprietà intrinseca del personaggio.

## Defensive Rating

Analogo all'Offensive Rating.

Dipende soltanto da:

-   caratteristiche
-   armatura
-   scudo
-   bonus permanenti
-   classe
-   razza

Anch'esso è indipendente dal contesto.

------------------------------------------------------------------------

# Combat Context

Il combattimento introduce modificatori temporanei.

Ad esempio:

-   terreno
-   meteo
-   benedizioni
-   maledizioni
-   veleni
-   effetti magici
-   stanchezza
-   fortuna
-   vantaggio tattico

Questi modificatori NON cambiano gli Offensive/Defensive Rating.

Producono invece valori temporanei utilizzati esclusivamente durante il
turno.

------------------------------------------------------------------------

# Stato del combattente

Durante un combattimento ogni personaggio possiede uno stato dinamico
composto almeno da:

-   Health
-   Current Stamina
-   Momentum
-   Status Effects

------------------------------------------------------------------------

# Momentum

Una delle idee principali è introdurre un indicatore chiamato
provvisoriamente **Momentum** (inizialmente era stato chiamato
"fiducia").

Il Momentum rappresenta l'inerzia psicologica e tattica del
combattimento.

Non è una caratteristica permanente del personaggio.

Può assumere valori, ad esempio, tra -100 e +100.

Aumenta quando il personaggio:

-   colpisce
-   effettua un colpo critico
-   para
-   schiva

Diminuisce quando:

-   subisce colpi
-   subisce critici
-   manca un attacco

L'effetto massimo dovrebbe essere limitato (ad esempio ±15%) per evitare
l'effetto "valanga".

------------------------------------------------------------------------

# Gestione della Stamina

La Stamina NON deve diminuire semplicemente perché passa il turno.

Deve essere consumata dalle azioni:

-   attaccare
-   parare
-   schivare

Subire un colpo potrebbe consumarne una piccola quantità a causa
dell'impatto.

Quando diminuisce devono comparire penalità progressive:

-   riduzione dell'attacco;
-   riduzione della difesa;
-   impossibilità di usare tecniche più impegnative.

La stamina non deve essere una seconda barra della vita.

------------------------------------------------------------------------

# Prima versione del combattimento

Il modello iniziale sarà volutamente semplice.

Due guerrieri armati di spada combattono uno contro l'altro.

Flusso del turno:

1.  Determinazione dell'iniziativa.
2.  Attacco del primo combattente.
3.  Tentativo di difesa (parata o schivata).
4.  Calcolo dell'esito.
5.  Applicazione del danno.
6.  Aggiornamento di stamina e momentum.
7.  Cambio del turno.
8.  Ripetizione fino alla sconfitta di uno dei due.

L'obiettivo iniziale è costruire un motore solido sul quale introdurre
successivamente meccaniche più evolute.

------------------------------------------------------------------------

# Obiettivi futuri

In futuro potranno essere introdotti:

-   abilità di classe;
-   magie;
-   effetti di stato;
-   combattimento a distanza;
-   terreno;
-   condizioni atmosferiche;
-   IA tattica;
-   attacchi speciali;
-   combattimenti con più partecipanti.

Queste funzionalità non fanno parte della prima versione ma
l'architettura dovrà consentirne l'introduzione senza riscrivere il
Combat Engine.
