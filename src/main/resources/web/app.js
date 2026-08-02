'use strict';

// Riproduttore della cronaca di una partita. Carica la cronaca una sola volta all'apertura
// (unico "fetch" del file) e da lì in poi vive interamente nel browser: nessun'altra richiesta
// al server durante la riproduzione.
//
// Il file è diviso in quattro responsabilità distinte:
//   1. Costanti e frasi (registro linguistico della pagina, centralizzato in un punto solo);
//   2. Costruzione dei momenti (dati puri: dal JSON della cronaca a una lista piatta indicizzabile);
//   3. Stato della riproduzione (play, pausa, passo avanti/indietro, velocità, salto);
//   4. Rendering nel DOM (funzioni che leggono un momento e aggiornano la pagina) e avvio.
//
// Il testo che arriva dalla cronaca (nomi generati, descrizioni del motore) è codice generato,
// non input utente, ma si inserisce comunque con "textContent" e mai con "innerHTML": costruire
// stringhe HTML per testo che non controlliamo resterebbe un'abitudine sbagliata anche qui.

// ============================================================================
// 1. Costanti e frasi
// ============================================================================

const CHRONICLE_URL = '/api/chronicle';
const DEFAULT_STEP_INTERVAL_MS = 1200;

const SPEED_OPTIONS = [
  { label: '0.5x', intervalMs: 2400 },
  { label: '1x', intervalMs: DEFAULT_STEP_INTERVAL_MS },
  { label: '2x', intervalMs: 600 },
  { label: '4x', intervalMs: 300 },
];

const TRIAL_SHAPE_LABELS = {
  BATTLE: 'battaglia',
  DUEL: 'duello',
};

const TRIAL_OUTCOME_LABELS = {
  WON: 'superata',
  FELL: 'caduta',
  STOOD_WITHOUT_WINNING: 'nessun vincitore',
};

const RUN_OUTCOME_LABELS = {
  WON: 'trionfo',
  FELL: 'sconfitta',
  STOOD_WITHOUT_WINNING: 'nessun vincitore',
};

// Le etichette coprono i valori noti oggi; un valore futuro non tradotto qui torna comunque
// leggibile perché "describeAction" ricade sul valore grezzo invece di restituire una stringa vuota.
const ACTION_KIND_LABELS = {
  HIT: 'colpo a segno',
  MISS: 'mancato',
  DODGED: 'schivato',
  PARRIED: 'parato',
  REST: 'riposo',
};

// Le sette formule brevi della colonna centrale, nel registro di BattleSceneRenderer.formulaLabel:
// la formula vive due volte, in Java per la console e qui per la pagina, perché combat.io.render
// è fuori portata e la cronaca non porta stringhe di presentazione — ogni lettore compone le
// proprie frasi. È la continuazione della decisione già registrata per le frasi del destino del
// loot, qui sotto.
const ACTION_FORMULAS = {
  HIT: action => hitFormula(action),
  MISS: () => 'manca',
  PARRIED: action => `parato (${action.damage})`,
  DODGED: () => 'schivato',
  REST: action => `riposa (+${action.staminaRecovered})`,
};

// Le tre varianti del colpo a segno, come in BattleSceneRenderer.hitLabel: critico e colpo potente
// hanno la precedenza sul colpo semplice, e si escludono a vicenda nella formula.
function hitFormula(action) {
  if (action.critical) {
    return `critico (${action.damage})`;
  }
  if (action.powerStrike) {
    return `colpo potente (${action.damage})`;
  }
  return `colpisce (${action.damage})`;
}

// I due versi della freccia della colonna centrale, come caratteri di testo puro: entrano nel DOM
// con "textContent" come ogni altro dato della cronaca, mai con markup.
const ARROW_SYMBOLS = {
  right: '→',
  left: '←',
};

// Le due squadre, sempre dallo stesso lato: la 0 a sinistra e la 1 a destra, nelle colonne di
// schede come nella colonna centrale, esattamente come nella scena di console. Sono la convenzione
// con cui l'arena fotografa il roster (protagonista in squadra 0), non una scelta della pagina.
const TEAM_LEFT = 0;
const TEAM_RIGHT = 1;

// Le otto frasi del destino del loot, nel registro di HeroProgressFormatter ma scritte per la
// pagina: ognuna legge esattamente i campi che quel destino garantisce (il backend risolve il
// destino una volta sola in HeroProgress, la pagina non lo deduce da sé). Il gioiello non vale più
// punti caratteristica di suo: i suoi eventuali buff si leggono già dai bonus di "describeItem",
// accanto a rarità e potenza, come per arma e armatura.
const LOOT_FATE_MESSAGES = {
  WEAPON_TAKEN: progress =>
      `Arma: trova ${describeItem(progress.found)}, lascia ${describeItem(progress.dropped)} e la impugna.`,
  WEAPON_DISCARDED: progress =>
      `Arma: trova ${describeItem(progress.found)}, non batte la sua: la scarta.`,
  ARMOUR_WORN_ON_EMPTY_SLOT: progress =>
      `Armatura: trova ${describeItem(progress.found)}, copre una parte del corpo prima scoperta: la indossa.`,
  ARMOUR_REPLACED: progress =>
      `Armatura: trova ${describeItem(progress.found)}, sostituisce ${describeItem(progress.dropped)}.`,
  ARMOUR_DISCARDED: progress =>
      `Armatura: trova ${describeItem(progress.found)}, difende meno o quanto la sua: la scarta.`,
  JEWEL_WORN_ON_EMPTY_TYPE: progress =>
      `Gioiello: trova ${describeItem(progress.found)}, è un tipo che non portava ancora: lo indossa.`,
  JEWEL_REPLACED: progress =>
      `Gioiello: trova ${describeItem(progress.found)}, sostituisce ${describeItem(progress.dropped)}.`,
  JEWEL_DISCARDED: progress =>
      `Gioiello: trova ${describeItem(progress.found)}, non batte quello che porta: lo scarta.`,
};

// L'intestazione del riquadro del protagonista segue la corsa: nessuna prova ancora conclusa dice
// che il protagonista entra in arena, da lì in poi dice dopo quale prova si trova. Il numero non
// anticipa niente: è sempre il conteggio di prove già concluse, mai una previsione.
const PROTAGONIST_ENTRY_HEADING = 'Il protagonista entra nell’arena';

function protagonistHeadingFor(completedTrials) {
  return completedTrials === 0 ? PROTAGONIST_ENTRY_HEADING : `Il protagonista dopo la prova ${completedTrials}`;
}

// ============================================================================
// 2. Costruzione dei momenti — dati puri, nessun accesso al DOM
// ============================================================================
//
// Un "momento" è l'unità di avanzamento della riproduzione: un passo di scontro (round o turno),
// la procedura di fine scontro di una prova vinta, oppure la conclusione della corsa. Sono
// costruiti tutti all'apertura in una lista piatta, così avanti, indietro e salto sono solo
// indicizzazione di un array, non ricalcolo.

// Ogni momento porta con sé la scheda corrente del protagonista e il numero di prove concluse
// fino a quel momento: "progression" si accumula scorrendo le prove giocate, senza mai guardare
// quelle future di "chronicle.trials" (il vincolo di non-spoiler). La scheda cresce esattamente al
// momento della procedura che quella crescita racconta, che quindi la porta già nuova.
function buildMoments(chronicle) {
  const totalTrials = chronicle.plannedTrials;
  const moments = [];
  let progression = { hero: chronicle.protagonist, completedTrials: 0 };
  chronicle.trials.forEach((trial, trialIndex) => {
    moments.push(buildSetupMoment(trial, trialIndex, totalTrials, progression));
    buildTrialStepMoments(trial, trialIndex, totalTrials, progression).forEach(moment => moments.push(moment));
    if (trial.progress) {
      progression = { hero: trial.progress.heroAfter, completedTrials: progression.completedTrials + 1 };
      moments.push(buildProgressMoment(trial, trialIndex, totalTrials, progression));
    }
  });
  moments.push(buildConclusionMoment(chronicle.conclusion, progression, totalTrials));
  return moments;
}

// Associa a ogni numero di prova il suo esito, per il percorso disegnato della sezione 4: distingue
// una stazione già attraversata senza vittoria da una superata. Non rompe il vincolo di non-spoiler
// perché nasce da "chronicle.trials", che contiene solo le prove davvero giocate: una stazione
// futura semplicemente non compare in questa mappa.
function buildTrialOutcomesByNumber(trials) {
  const outcomesByNumber = {};
  trials.forEach(trial => {
    outcomesByNumber[trial.number] = trial.outcome;
  });
  return outcomesByNumber;
}

// I campi comuni a ogni momento di una prova (passo, procedura, o il passo zero di apertura):
// fattorizzati qui perché i quattro costruttori che seguono li ripeterebbero altrimenti tutti e
// sei identici, con l'unica differenza del tipo di momento e se l'esito va già rivelato.
function buildTrialMomentBase(kind, trial, trialIndex, totalTrials, showOutcome, progression) {
  return {
    kind,
    trialIndex,
    trialNumber: trial.number,
    totalTrials,
    description: trial.description,
    shape: trial.shape,
    outcome: trial.outcome,
    budget: trial.budget,
    showOutcome,
    hero: progression.hero,
    completedTrials: progression.completedTrials,
  };
}

// Il passo zero di ogni prova: schede, equipaggiamento e caratteristiche già pronti, ma vita e
// stamina piene e nessuno scambio ancora accaduto. Non è una finzione, è lo stato vero dell'istante
// che precede lo scontro: a ogni prova il protagonista viene materializzato di nuovo dalla sua
// scheda (è da lì che arriva la cura completa) e gli sfidanti nascono in quello stesso istante, mai
// feriti prima del primo passo.
function buildSetupMoment(trial, trialIndex, totalTrials, progression) {
  return {
    ...buildTrialMomentBase('setup', trial, trialIndex, totalTrials, false, progression),
    roster: trial.roster,
    vitals: fullVitalsOf(trial.roster),
    opponentCount: opponentCountOf(trial.roster),
  };
}

// Vita e stamina piene per l'intero roster, ricavate dai massimi che ogni voce del roster porta
// già: non serve una fotografia in più dal backend, il passo zero è interamente derivabile da dati
// che la cronaca ha comunque.
function fullVitalsOf(roster) {
  return roster.map(fighter => ({
    name: fighter.name,
    currentHealth: fighter.maxHealth,
    maxHealth: fighter.maxHealth,
    currentStamina: fighter.maxStamina,
    maxStamina: fighter.maxStamina,
  }));
}

// Il numero di avversari della prova, usato dalla scena per riproporzionare la colonna di destra
// (sezione 4): calcolato una sola volta qui, mai contando nodi nel DOM.
function opponentCountOf(roster) {
  return roster.filter(fighter => fighter.teamIndex === TEAM_RIGHT).length;
}

function buildTrialStepMoments(trial, trialIndex, totalTrials, progression) {
  const isBattle = trial.shape === 'BATTLE';
  const steps = isBattle ? trial.rounds : trial.turns;
  return steps.map((step, stepIndex) => {
    const isFinalStep = stepIndex === steps.length - 1;
    const vitalsAfter = vitalsAfterStep(trial, steps, stepIndex, isBattle);
    return isBattle
        ? buildBattleStepMoment(trial, trialIndex, totalTrials, step, isFinalStep, vitalsAfter, progression)
        : buildDuelStepMoment(trial, trialIndex, totalTrials, step, isFinalStep, vitalsAfter, progression);
  });
}

// Il motore fotografa i vitali in due istanti diversi nelle due forme, ed è una trappola che vale
// la pena documentare ogni volta che la si incontra: il round della battaglia porta già lo stato di
// fine round (BattleEngine.buildRoundLogEntry), ma il turno del duello porta lo stato di inizio
// turno (EngagementTurnPlayer.play, "startOfRoundVitals"). Questo è l'unico punto che risolve "lo
// stato dopo il passo" per entrambe le forme, così la pagina racconta lo stesso momento della
// console (vedi CombatScreenRenderer.vitalsAfter, che affronta esattamente lo stesso problema).
function vitalsAfterStep(trial, steps, stepIndex, isBattle) {
  return isBattle ? steps[stepIndex].vitals : duelVitalsAfterTurn(trial, steps, stepIndex);
}

// Il turno successivo porta lo stato dopo il passo corrente; per l'ultimo turno — o quando il
// turno successivo non ha vitali utilizzabili — si ripiega su "trial.finalVitals", l'unico posto in
// cui lo stato dopo l'ultimo turno del duello è disponibile (il log non lo contiene mai).
function duelVitalsAfterTurn(trial, turns, stepIndex) {
  const nextTurn = turns[stepIndex + 1];
  const hasUsableNextVitals = nextTurn && nextTurn.vitals && nextTurn.vitals.length > 0;
  return hasUsableNextVitals ? nextTurn.vitals : trial.finalVitals;
}

function buildBattleStepMoment(trial, trialIndex, totalTrials, round, isFinalStep, vitalsAfter, progression) {
  const vitals = alignVitalsToRoster(trial.roster, vitalsAfter);
  const engagements = round.turns.map(engagementTurn => buildEngagement(trial.roster, engagementTurn));
  // Nella battaglia l'attore di uno scambio è, per costruzione del motore, chi ha vinto
  // l'iniziativa: l'indice non richiede nessuna inferenza per nome (decisione 6 della SPEC).
  const initiativeRosterIndexes = round.turns.map(engagementTurn => engagementTurn.attackerIndex);
  const engagedOpponentRosterIndexes = engagedOpponentIndexesOf(trial.roster, round.turns);
  return {
    ...buildTrialMomentBase('step', trial, trialIndex, totalTrials, isFinalStep, progression),
    roster: trial.roster,
    vitals,
    initiativeRosterIndexes,
    engagedOpponentRosterIndexes,
    opponentCount: opponentCountOf(trial.roster),
    battle: { roundNumber: round.roundNumber, engagements, events: round.events },
  };
}

// Gli avversari che partecipano ad almeno uno scambio del round si spostano verso il protagonista
// nella scena (sezione 4): l'insieme si limita alla squadra di destra, perché il protagonista è
// sempre solo e sempre coinvolto in ogni scambio (è l'unico membro della sua squadra), e uno
// scostamento costante non comunicherebbe nessun cambiamento. Il riposo non è uno scambio: chi
// riposa non ingaggia nessuno, come già per la colonna centrale (vedi "buildCenterExchange").
function engagedOpponentIndexesOf(roster, turns) {
  const engagedIndexes = new Set();
  turns.forEach(engagementTurn => {
    const action = engagementTurn.turn.action;
    const isExchange = action != null && action.kind !== 'REST';
    if (!isExchange) {
      return;
    }
    addOpponentRosterIndex(engagedIndexes, roster, engagementTurn.attackerIndex);
    addOpponentRosterIndex(engagedIndexes, roster, engagementTurn.targetIndex);
  });
  return Array.from(engagedIndexes);
}

function addOpponentRosterIndex(rosterIndexes, roster, rosterIndex) {
  const fighter = findFighterByRosterIndex(roster, rosterIndex);
  if (fighter && fighter.teamIndex === TEAM_RIGHT) {
    rosterIndexes.add(rosterIndex);
  }
}

function buildEngagement(roster, engagementTurn) {
  const attacker = findFighterByRosterIndex(roster, engagementTurn.attackerIndex);
  const target = findFighterByRosterIndex(roster, engagementTurn.targetIndex);
  const turn = engagementTurn.turn;
  return {
    attackerName: attacker.name,
    targetName: target.name,
    description: turn.description,
    action: turn.action,
    center: buildCenterExchange(attacker, target, turn.action),
  };
}

// La formula breve dell'azione, dal registro di ACTION_FORMULAS: stringa vuota se l'azione manca,
// il valore grezzo se il tipo non è fra i sette noti, come già fa "describeAction" col suo valore
// grezzo di ripiego.
function formulaLabel(action) {
  if (!action) {
    return '';
  }
  const formula = ACTION_FORMULAS[action.kind];
  return formula ? formula(action) : action.kind;
}

// Il verso della freccia della colonna centrale, dalla regola di BattleSceneRenderer.arrowFor:
// attaccante nella squadra di sinistra, freccia verso destra, e viceversa.
function arrowDirectionFor(attackerTeamIndex) {
  return attackerTeamIndex === TEAM_LEFT ? 'right' : 'left';
}

// La voce della colonna centrale per uno scambio: formula sopra, i due nomi con la freccia sotto.
// I nomi si dispongono per squadra e non per ruolo — squadra 0 a sinistra, squadra 1 a destra,
// come le colonne di schede e come la scena di console — e la freccia dice soltanto chi dei due
// attacca. Ordinarli per attaccante-bersaglio contraddirebbe le schede: un attaccante della
// squadra 1 comparirebbe a sinistra della freccia mentre la sua scheda sta a destra, e il verso
// finirebbe per raccontare lo scontro all'incontrario.
//
// Senza bersaglio non c'è freccia, e resta il solo nome di chi agisce: è il caso del riposo (un
// attore che non colpisce nessuno) e quello dell'attore non risolto. Nessuna freccia indovinata.
function buildCenterExchange(attacker, target, action) {
  const isRest = action != null && action.kind === 'REST';
  const hasTarget = !isRest && target != null;
  const involved = hasTarget ? [attacker, target] : [attacker];
  return {
    formula: formulaLabel(action),
    leftName: nameOfTeam(involved, TEAM_LEFT),
    rightName: nameOfTeam(involved, TEAM_RIGHT),
    arrowDirection: hasTarget ? arrowDirectionFor(attacker.teamIndex) : null,
  };
}

function nameOfTeam(fighters, teamIndex) {
  const found = fighters.find(fighter => fighter.teamIndex === teamIndex);
  return found ? found.name : null;
}

function emptyCenterExchange(action) {
  return { formula: formulaLabel(action), leftName: null, rightName: null, arrowDirection: null };
}

// Un solo punto risolve un nome in indice di roster, condiviso dalla colonna centrale e dalla
// stellina del duello: "null" se il nome non trova corrispondenza o ne trova più di una, mai una
// freccia o una stellina sbagliata.
function resolveRosterIndexByName(roster, name) {
  if (name === null || name === undefined) {
    return null;
  }
  const matches = roster.filter(fighter => fighter.name === name);
  return matches.length === 1 ? matches[0].rosterIndex : null;
}

function buildDuelStepMoment(trial, trialIndex, totalTrials, turn, isFinalStep, vitalsAfter, progression) {
  const vitals = alignVitalsToRoster(trial.roster, vitalsAfter);
  // Un turno di duello può non portare il report d'iniziativa: senza guardia, "chosenName" spegne
  // la pagina con un TypeError (il difetto latente segnalato dalla SPEC).
  const chosenName = turn.initiative ? turn.initiative.chosenName : null;
  const actorIndex = resolveRosterIndexByName(trial.roster, chosenName);
  return {
    ...buildTrialMomentBase('step', trial, trialIndex, totalTrials, isFinalStep, progression),
    roster: trial.roster,
    vitals,
    initiativeRosterIndexes: actorIndex === null ? [] : [actorIndex],
    // Il duello ha un solo avversario, sempre coinvolto in ogni turno: uno scostamento costante
    // non comunicherebbe nulla, e la scheda deve restare esattamente come oggi. Lo scostamento
    // verso il protagonista resta quindi una caratteristica solo della battaglia NvN.
    engagedOpponentRosterIndexes: [],
    opponentCount: opponentCountOf(trial.roster),
    duel: {
      turnNumber: turn.turnNumber,
      actingName: chosenName,
      description: turn.description,
      action: turn.action,
      center: buildDuelCenterExchange(trial.roster, actorIndex, turn.action),
    },
  };
}

// Il bersaglio del duello è l'altro dei due nel roster, che per costruzione è sempre
// [protagonista, rivale] (decisione 5 della SPEC). Se l'attore non si risolve, niente freccia e
// nessun nome: mai un bersaglio indovinato.
function buildDuelCenterExchange(roster, actorIndex, action) {
  if (actorIndex === null) {
    return emptyCenterExchange(action);
  }
  const attacker = findFighterByRosterIndex(roster, actorIndex);
  const target = roster.find(fighter => fighter.rosterIndex !== actorIndex) || null;
  return buildCenterExchange(attacker, target, action);
}

function buildProgressMoment(trial, trialIndex, totalTrials, progression) {
  return {
    ...buildTrialMomentBase('progress', trial, trialIndex, totalTrials, true, progression),
    progress: trial.progress,
  };
}

function buildConclusionMoment(conclusion, progression, totalTrials) {
  return {
    kind: 'conclusion',
    conclusion,
    totalTrials,
    hero: progression.hero,
    completedTrials: progression.completedTrials,
  };
}

function findFighterByRosterIndex(roster, rosterIndex) {
  return roster.find(fighter => fighter.rosterIndex === rosterIndex);
}

// Il motore dichiara i nomi dei combattenti inaffidabili come identificatore univoco (due
// combattenti generati possono chiamarsi allo stesso modo): quando l'ordine e la lunghezza di
// "vitals" coincidono con quelli del roster ci si allinea per posizione, e si ricorre al nome
// solo come ripiego, per un'eventuale divergenza futura fra le due liste. Se nemmeno il nome trova
// corrispondenza, il ripiego restituisce "null" invece del valore non definito di "Array.find":
// il resto del codice ha così un solo modo di riconoscere una vitale mancante.
function alignVitalsToRoster(roster, vitals) {
  if (roster.length === vitals.length) {
    return vitals;
  }
  return roster.map(fighter => vitals.find(vital => vital.name === fighter.name) || null);
}

// ============================================================================
// 3. Stato della riproduzione
// ============================================================================

function createPlayer(moments, onRender, onControlsChange) {
  const state = {
    moments,
    currentIndex: 0,
    playing: false,
    timerId: null,
    stepIntervalMs: DEFAULT_STEP_INTERVAL_MS,
  };

  function status() {
    return {
      currentIndex: state.currentIndex,
      totalMoments: state.moments.length,
      playing: state.playing,
      atStart: state.currentIndex === 0,
      atEnd: state.currentIndex === state.moments.length - 1,
    };
  }

  function render() {
    onRender(state.moments[state.currentIndex]);
    onControlsChange(status());
  }

  function goToIndex(index) {
    const clamped = Math.max(0, Math.min(state.moments.length - 1, index));
    state.currentIndex = clamped;
    render();
  }

  function stepForward() {
    goToIndex(state.currentIndex + 1);
  }

  function stepBack() {
    goToIndex(state.currentIndex - 1);
  }

  // Ferma il timer senza ridisegnare: la usano sia "pause" (che poi ridisegna una volta sola) sia
  // "jumpToIndex" (dove il ridisegno lo fa già "goToIndex" con la posizione di arrivo), così un
  // salto durante la riproduzione non produce due render consecutivi per lo stesso momento.
  function stopPlaybackTimer() {
    if (state.timerId !== null) {
      clearInterval(state.timerId);
      state.timerId = null;
    }
    state.playing = false;
  }

  function play() {
    if (state.playing || status().atEnd) {
      return;
    }
    state.playing = true;
    render();
    state.timerId = setInterval(() => {
      stepForward();
      if (status().atEnd) {
        pause();
      }
    }, state.stepIntervalMs);
  }

  function pause() {
    if (!state.playing) {
      return;
    }
    stopPlaybackTimer();
    render();
  }

  function togglePlay() {
    if (state.playing) {
      pause();
    } else {
      play();
    }
  }

  function setSpeed(stepIntervalMs) {
    const wasPlaying = state.playing;
    if (wasPlaying) {
      pause();
    }
    state.stepIntervalMs = stepIntervalMs;
    if (wasPlaying) {
      play();
    }
  }

  // Un salto è un'azione con cui l'utente prende il comando della riproduzione: la mette in pausa
  // prima di spostarsi, altrimenti il timer di "play" continuerebbe ad avanzare e supererebbe
  // subito la posizione appena scelta. Sia la timeline sia i pulsanti di prova passano da qui.
  function jumpToIndex(index) {
    stopPlaybackTimer();
    goToIndex(index);
  }

  function jumpToTrial(trialNumber) {
    const index = state.moments.findIndex(moment => moment.trialNumber === trialNumber);
    if (index >= 0) {
      jumpToIndex(index);
    }
  }

  render();

  return { stepForward, stepBack, play, pause, togglePlay, jumpToIndex, setSpeed, jumpToTrial };
}

// ============================================================================
// 4. Rendering nel DOM
// ============================================================================

function el(tag, className) {
  const element = document.createElement(tag);
  if (className) {
    element.className = className;
  }
  return element;
}

function textEl(tag, text, className) {
  const element = el(tag, className);
  element.textContent = text;
  return element;
}

function clearChildren(node) {
  while (node.firstChild) {
    node.removeChild(node.firstChild);
  }
}

function showSection(id) {
  document.getElementById(id).hidden = false;
}

function hideSection(id) {
  document.getElementById(id).hidden = true;
}

// I bonus dell'oggetto compaiono accanto a rarità e potenza, nello stesso gruppo fra parentesi:
// non serve una riga propria come in console, dove il troncamento a 36 caratteri li avrebbe
// mangiati (vedi FighterCardFormatter). Un gioiello senza potenza li porta comunque.
function describeItem(item) {
  const details = [item.rarity];
  if (item.kind === 'WEAPON') {
    details.push(`attacco ${item.power}`);
  } else if (item.kind === 'ARMOUR') {
    details.push(`difesa ${item.power}`);
  }
  item.bonuses.forEach(bonus => details.push(`+${bonus.value} ${bonus.characteristic}`));
  return `${item.name} (${details.join(', ')})`;
}

function describeGrowth(gains) {
  const parts = gains.map(gain => `+${gain.points} ${gain.characteristic}`);
  return `Crescita: ${parts.join(', ')}.`;
}

function describeAction(action) {
  const kindLabel = ACTION_KIND_LABELS[action.kind] || action.kind;
  const parts = [`Esito: ${kindLabel}`];
  if (action.damage > 0) {
    parts.push(`danno ${action.damage}`);
  }
  if (action.staminaRecovered > 0) {
    parts.push(`stamina recuperata ${action.staminaRecovered}`);
  }
  if (action.critical) {
    parts.push('critico');
  }
  if (action.powerStrike) {
    parts.push('colpo potente');
  }
  return parts.join(', ') + '.';
}

function percentage(current, max) {
  if (max <= 0) {
    return 0;
  }
  const ratio = Math.max(0, Math.min(1, current / max));
  return Math.round(ratio * 100);
}

function createBar(label, current, max, cssClass) {
  const wrapper = el('div', 'bar');
  wrapper.appendChild(textEl('div', `${label}: ${current}/${max}`, 'bar-label'));
  const track = el('div', 'bar-track');
  const fill = el('div', `bar-fill ${cssClass}`);
  fill.style.width = percentage(current, max) + '%';
  track.appendChild(fill);
  wrapper.appendChild(track);
  return wrapper;
}

function buildCharacteristicsList(characteristics) {
  const list = el('ul', 'characteristics-list');
  characteristics.forEach(entry => list.appendChild(textEl('li', `${entry.characteristic}: ${entry.value}`)));
  return list;
}

// La lista del protagonista mostra il valore base col contributo dell'equipaggiamento accanto
// (es. "STRENGTH: 12 (+3)"), ricavato per sottrazione fra effettive e base: un'aritmetica, non una
// regola di gioco, che qui si può calcolare al momento del rendering senza violare la divisione fra
// dati puri (sezione 2) e DOM (questa sezione).
function buildHeroCharacteristicsList(characteristics, effectiveCharacteristics) {
  const list = el('ul', 'characteristics-list');
  characteristics.forEach(entry => {
    const bonus = equipmentBonusOf(entry, effectiveCharacteristics);
    const text = bonus > 0
        ? `${entry.characteristic}: ${entry.value} (+${bonus})`
        : `${entry.characteristic}: ${entry.value}`;
    list.appendChild(textEl('li', text));
  });
  return list;
}

function equipmentBonusOf(baseCharacteristic, effectiveCharacteristics) {
  const effectiveEntry = effectiveCharacteristics.find(
      entry => entry.characteristic === baseCharacteristic.characteristic);
  return effectiveEntry ? effectiveEntry.value - baseCharacteristic.value : 0;
}

function buildArmourList(armourPieces) {
  const list = el('ul', 'armour-list');
  if (armourPieces.length === 0) {
    list.appendChild(textEl('li', 'Nessuna armatura'));
    return list;
  }
  armourPieces.forEach(piece => list.appendChild(textEl('li', describeItem(piece))));
  return list;
}

function buildJewelsList(jewels) {
  const list = el('ul', 'jewels-list');
  if (jewels.length === 0) {
    list.appendChild(textEl('li', 'Nessun gioiello'));
    return list;
  }
  jewels.forEach(jewel => list.appendChild(textEl('li', describeItem(jewel))));
  return list;
}

// Scheda del protagonista fuori dallo scontro (ingresso in arena, oppure la crescita di fine
// scontro): porta i gioielli, che nel roster dello scontro non esistono perché il motore non li
// monta in campo.
function buildHeroCard(hero, heading) {
  const card = el('article', 'hero-card');
  card.appendChild(textEl('h3', heading));
  card.appendChild(textEl('p', `${hero.name} · ${hero.race} · ${hero.characterClass}`));
  card.appendChild(textEl('p', `Arma: ${describeItem(hero.weapon)}`));
  card.appendChild(buildArmourList(hero.armourPieces));
  card.appendChild(buildJewelsList(hero.jewels));
  card.appendChild(buildHeroCharacteristicsList(hero.characteristics, hero.effectiveCharacteristics));
  return card;
}

// Scheda di un combattente nello scontro: niente gioielli (il roster non li porta), rating
// offensivo e difensivo al posto della crescita, barre di vita e stamina aggiornate a ogni passo,
// e vicino al nome il segno di chi ha l'iniziativa in questo passo. Le caratteristiche sono già
// quelle in campo, equipaggiamento compreso: il "Fighter" del motore le porta effettive, non c'è
// una base da confrontare come nella scheda del protagonista fuori dallo scontro.
function buildCombatantCard(fighter, vital, hasInitiative, isEngaged) {
  const card = el('article', isEngaged ? 'fighter-card engaged' : 'fighter-card');
  card.appendChild(buildCombatantNameLine(fighter, hasInitiative, isEngaged));
  card.appendChild(textEl('p', `${fighter.race} · ${fighter.characterClass}`, 'fighter-subtitle'));
  card.appendChild(textEl('p', `Arma: ${describeItem(fighter.weapon)}`));
  card.appendChild(buildArmourList(fighter.armourPieces));
  card.appendChild(textEl('p', `Rating offensivo ${fighter.offensiveRating} · difensivo ${fighter.defensiveRating}`));
  card.appendChild(textEl('p', 'Caratteristiche in campo, equipaggiamento compreso:', 'fighter-subtitle'));
  card.appendChild(buildCharacteristicsList(fighter.characteristics));
  appendVitalsBars(card, vital);
  return card;
}

// Il nome del combattente, con vicino la stellina di chi ha l'iniziativa: non un carattere muto,
// un elemento con un'etichetta esplicita per chi legge con tecnologie assistive.
function buildCombatantNameLine(fighter, hasInitiative, isEngaged) {
  const heading = el('h3');
  heading.appendChild(document.createTextNode(fighter.name));
  if (hasInitiative) {
    heading.appendChild(buildInitiativeMarker());
  }
  if (isEngaged) {
    heading.appendChild(buildEngagedMarker());
  }
  return heading;
}

function buildInitiativeMarker() {
  const marker = textEl('span', '★', 'initiative-marker');
  marker.setAttribute('role', 'img');
  marker.setAttribute('aria-label', 'ha l’iniziativa in questo passo');
  return marker;
}

// Sullo stesso modello della stellina dell'iniziativa: lo scostamento della scheda verso il
// protagonista (vedi ".fighter-card.engaged" in app.css) non è l'unico segnale, perché non si può
// affidare l'informazione al solo spostamento visivo.
function buildEngagedMarker() {
  const marker = textEl('span', '⚔', 'engaged-marker');
  marker.setAttribute('role', 'img');
  marker.setAttribute('aria-label', 'ingaggiato in questo scambio');
  return marker;
}

// Il ripiego per nome di "alignVitalsToRoster" può non trovare corrispondenza per questo
// combattente: la scheda si disegna comunque, con un'indicazione onesta al posto delle barre
// invece di un valore inventato, che mentirebbe sul suo stato.
function appendVitalsBars(card, vital) {
  if (!vital) {
    card.appendChild(textEl('p', 'Dati di vita e stamina non disponibili per questo passo.', 'fighter-subtitle'));
    return;
  }
  card.appendChild(createBar('Vita', vital.currentHealth, vital.maxHealth, 'bar-health'));
  card.appendChild(createBar('Stamina', vital.currentStamina, vital.maxStamina, 'bar-stamina'));
}

// Segue la corsa invece di fotografare solo l'ingresso: ogni momento porta già la scheda corrente
// del protagonista (sezione 2), quindi qui non c'è altro da fare che leggerla e intitolarla.
function renderProtagonistEntry(moment) {
  const container = document.getElementById('protagonist-entry');
  clearChildren(container);
  container.appendChild(buildHeroCard(moment.hero, protagonistHeadingFor(moment.completedTrials)));
}

function renderHeader(moment) {
  const header = document.getElementById('trial-header');
  clearChildren(header);
  if (moment.kind === 'conclusion') {
    header.appendChild(textEl('span', 'Corsa conclusa', 'trial-position'));
    header.appendChild(textEl('span', RUN_OUTCOME_LABELS[moment.conclusion.outcome], 'trial-outcome'));
    return;
  }
  header.appendChild(textEl('span', `Prova ${moment.trialNumber}/${moment.totalTrials}`, 'trial-position'));
  header.appendChild(textEl('span', moment.description));
  header.appendChild(textEl('span', TRIAL_SHAPE_LABELS[moment.shape]));
  if (moment.showOutcome) {
    header.appendChild(textEl('span', TRIAL_OUTCOME_LABELS[moment.outcome], 'trial-outcome'));
  }
}

function renderTeam(containerId, roster, vitals, teamIndex, initiativeRosterIndexes, engagedRosterIndexes) {
  const container = document.getElementById(containerId);
  clearChildren(container);
  roster.forEach((fighter, position) => {
    if (fighter.teamIndex === teamIndex) {
      const hasInitiative = initiativeRosterIndexes.includes(fighter.rosterIndex);
      const isEngaged = engagedRosterIndexes.includes(fighter.rosterIndex);
      container.appendChild(buildCombatantCard(fighter, vitals[position], hasInitiative, isEngaged));
    }
  });
}

// Il numero di avversari guida la larghezza della colonna di destra in app.css: un attributo dato
// sul contenitore, letto dal foglio di stile, mai un conteggio di nodi del DOM. Solo gli avversari
// (squadra di destra) possono ingaggiare: il protagonista resta uno solo e non si sposta mai.
function renderBattlefield(roster, vitals, initiativeRosterIndexes, engagedOpponentRosterIndexes, opponentCount) {
  document.getElementById('battlefield').dataset.opponentCount = String(opponentCount);
  renderTeam('team-0', roster, vitals, TEAM_LEFT, initiativeRosterIndexes, []);
  renderTeam('team-1', roster, vitals, TEAM_RIGHT, initiativeRosterIndexes, engagedOpponentRosterIndexes);
}

// Le voci della colonna centrale del passo corrente: una per scambio nella battaglia, l'unica del
// turno nel duello. I dati sono già pronti nel momento (sezione 2): qui si legge, non si calcola.
function centerExchangesOf(moment) {
  return moment.shape === 'BATTLE'
      ? moment.battle.engagements.map(engagement => engagement.center)
      : [moment.duel.center];
}

function renderBattleCenter(centerExchanges) {
  const container = document.getElementById('battle-center');
  clearChildren(container);
  centerExchanges.forEach(exchange => container.appendChild(buildCenterExchangeCard(exchange)));
}

function buildCenterExchangeCard(exchange) {
  const card = el('div', 'center-exchange');
  if (exchange.formula) {
    card.appendChild(textEl('p', exchange.formula, 'center-formula'));
  }
  if (exchange.leftName || exchange.rightName) {
    card.appendChild(buildCenterNamesLine(exchange));
  }
  return card;
}

// I due nomi ciascuno dal lato della propria squadra, con la freccia in mezzo quando c'è un
// bersaglio da colpire: nessuna freccia per un riposo o per un'iniziativa non risolta, e in quei
// casi resta il solo nome di chi agisce, dal suo lato. Le tre celle si disegnano sempre, anche
// vuote, così la freccia resta incolonnata fra uno scambio e l'altro.
function buildCenterNamesLine(exchange) {
  const line = el('p', 'center-names');
  line.appendChild(textEl('span', exchange.leftName || '', 'center-name center-name-left'));
  line.appendChild(textEl('span', arrowSymbolOf(exchange.arrowDirection), 'center-arrow'));
  line.appendChild(textEl('span', exchange.rightName || '', 'center-name center-name-right'));
  return line;
}

function arrowSymbolOf(arrowDirection) {
  return arrowDirection ? ARROW_SYMBOLS[arrowDirection] : '';
}

function buildEngagementItem(engagement) {
  const item = el('li', 'engagement-item');
  item.appendChild(textEl('p', `${engagement.attackerName} → ${engagement.targetName}`, 'engagement-pair'));
  item.appendChild(textEl('p', engagement.description));
  item.appendChild(textEl('p', describeAction(engagement.action), 'action-detail'));
  return item;
}

function renderBattleStep(panel, battle) {
  panel.appendChild(textEl('h3', `Round ${battle.roundNumber}`));
  const list = el('ol', 'engagement-list');
  battle.engagements.forEach(engagement => list.appendChild(buildEngagementItem(engagement)));
  panel.appendChild(list);
  if (battle.events.length > 0) {
    const events = el('ul', 'event-list');
    battle.events.forEach(event => events.appendChild(textEl('li', event)));
    panel.appendChild(events);
  }
}

function renderDuelStep(panel, duel) {
  panel.appendChild(textEl('h3', `Turno ${duel.turnNumber}`));
  panel.appendChild(textEl('p', describeDuelActor(duel.actingName)));
  panel.appendChild(textEl('p', duel.description));
  panel.appendChild(textEl('p', describeAction(duel.action), 'action-detail'));
}

// Il report d'iniziativa del turno può mancare: il ripiego è dichiararlo con una frase onesta,
// mai un'interpolazione con "undefined" (il difetto latente chiuso in questa fase).
function describeDuelActor(actingName) {
  return actingName ? `Agisce: ${actingName}` : 'Agisce: iniziativa non riportata';
}

function renderStepPanel(moment) {
  const panel = document.getElementById('step-panel');
  clearChildren(panel);
  if (moment.shape === 'BATTLE') {
    renderBattleStep(panel, moment.battle);
  } else {
    renderDuelStep(panel, moment.duel);
  }
}

// Il pannello del passo zero: nessuno scambio da raccontare, solo la dichiarazione che lo scontro
// non è ancora cominciato. Nel registro delle altre fasi (vedi "vita e stamina tornano piene" della
// procedura di fine scontro). Il monte di squadra e lo sconto della fortuna si dichiarano qui,
// una volta sola per prova, come fa ConsoleArenaLogger.announceRound in console.
function renderSetupStepPanel(budget) {
  const panel = document.getElementById('step-panel');
  clearChildren(panel);
  panel.appendChild(textEl('h3', 'Prima dello scontro'));
  panel.appendChild(textEl('p', 'Vita e stamina sono piene: lo scontro non è ancora cominciato.'));
  const budgetLine = describeChallengerBudget(budget);
  if (budgetLine) {
    panel.appendChild(textEl('p', budgetLine));
  }
}

// Racconta il monte di squadra dichiarato dalla stazione e lo sconto che la fortuna del
// protagonista vi applica davvero, nel registro proprio della pagina (frase gemella, non riusata,
// di ConsoleArenaLogger.announceLuckDiscount). Tace quando non c'è nulla da dire: la stazione dello
// specchio non passa da nessuno sconto ("budget" nullo), e uno sconto applicato pari a zero —
// fortuna nulla o monte già al pavimento — non merita una frase che affermerebbe un taglio
// inesistente.
function describeChallengerBudget(budget) {
  if (!budget || budget.luckDiscount === 0) {
    return null;
  }
  return `Il monte di squadra dichiarato per questa prova è ${budget.stationPoints} punti: `
      + `la fortuna del protagonista ne sconta ${budget.luckDiscount} e scendono in campo `
      + `con ${budget.squadPoints} punti.`;
}

// Non ripete la scheda del protagonista: al momento della crescita è esattamente quella che il
// riquadro in alto mostra già (vedi "renderProtagonistEntry", chiamata a ogni momento).
function renderProgressPanel(moment) {
  const panel = document.getElementById('progress-panel');
  clearChildren(panel);
  const progress = moment.progress;
  panel.appendChild(textEl('h3', 'Procedura di fine scontro'));
  panel.appendChild(textEl('p', `${progress.heroAfter.name} è ancora in piedi: vita e stamina tornano piene.`));
  panel.appendChild(textEl('p', LOOT_FATE_MESSAGES[progress.fate](progress)));
  panel.appendChild(textEl('p', describeGrowth(progress.gains)));
}

// Un pareggio non chiude più la corsa come una caduta (SPEC bilanciamento-progressione): la prova
// pareggiata fa proseguire, e solo la caduta o la fine del percorso fermano la corsa davvero. Un
// pareggio alla decima prova, quindi, non è una corsa interrotta a metà: il protagonista è arrivato
// in fondo al percorso, senza trionfo. Merita una frase distinta da quella di un pareggio che ha
// invece troncato il cammino prima della fine.
function describeConclusion(conclusion, totalTrials) {
  if (conclusion.outcome === 'WON') {
    return 'Il protagonista ha superato tutte le prove.';
  }
  if (conclusion.outcome === 'FELL') {
    return `Il protagonista è caduto alla prova ${conclusion.lastTrial}.`;
  }
  if (conclusion.lastTrial === totalTrials) {
    return 'Il protagonista è arrivato in fondo al percorso, ma senza trionfare.';
  }
  return `Il protagonista si è fermato senza vincere alla prova ${conclusion.lastTrial}.`;
}

function renderConclusionPanel(conclusion, totalTrials) {
  const panel = document.getElementById('conclusion-panel');
  clearChildren(panel);
  const triumph = conclusion.outcome === 'WON';
  panel.appendChild(textEl('h3', triumph ? 'Trionfo!' : 'La corsa finisce qui'));
  panel.appendChild(textEl('p', describeConclusion(conclusion, totalTrials)));
}

function renderMoment(moment, trialOutcomesByNumber) {
  renderProtagonistEntry(moment);
  renderHeader(moment);
  updateTrialPath(moment, trialOutcomesByNumber);
  if (moment.kind === 'setup') {
    showSection('battlefield');
    showSection('step-panel');
    hideSection('progress-panel');
    hideSection('conclusion-panel');
    // Nessuna iniziativa e nessuno scambio prima del primo passo: schieramenti pieni, colonna
    // centrale vuota, nessuno ingaggiato.
    renderBattlefield(moment.roster, moment.vitals, [], [], moment.opponentCount);
    renderBattleCenter([]);
    renderSetupStepPanel(moment.budget);
    return;
  }
  if (moment.kind === 'step') {
    showSection('battlefield');
    showSection('step-panel');
    hideSection('progress-panel');
    hideSection('conclusion-panel');
    renderBattlefield(
        moment.roster, moment.vitals, moment.initiativeRosterIndexes,
        moment.engagedOpponentRosterIndexes, moment.opponentCount);
    renderBattleCenter(centerExchangesOf(moment));
    renderStepPanel(moment);
    return;
  }
  if (moment.kind === 'progress') {
    hideSection('battlefield');
    hideSection('step-panel');
    showSection('progress-panel');
    hideSection('conclusion-panel');
    renderProgressPanel(moment);
    return;
  }
  hideSection('battlefield');
  hideSection('step-panel');
  hideSection('progress-panel');
  showSection('conclusion-panel');
  renderConclusionPanel(moment.conclusion, moment.totalTrials);
}

function updateControls(status) {
  document.getElementById('btn-step-back').disabled = status.atStart;
  document.getElementById('btn-step-forward').disabled = status.atEnd;

  const playPauseButton = document.getElementById('btn-play-pause');
  playPauseButton.disabled = status.atEnd && !status.playing;
  playPauseButton.textContent = status.playing ? 'Pausa' : 'Play';

  // La barra e il contatore restano nascosti in vista (".timeline-control[hidden]" in app.css),
  // non rimossi dal DOM: sono uno spoiler nascosto, non un comando tolto. Continuano quindi ad
  // aggiornarsi a ogni passo, pronti a ricomparire se un giorno la scelta di nasconderli cambiasse.
  const timeline = document.getElementById('timeline');
  timeline.max = String(status.totalMoments - 1);
  timeline.value = String(status.currentIndex);

  const position = document.getElementById('timeline-position');
  position.textContent = `Passo ${status.currentIndex + 1}/${status.totalMoments}`;
}

function wireControls(player) {
  document.getElementById('btn-step-back').addEventListener('click', player.stepBack);
  document.getElementById('btn-step-forward').addEventListener('click', player.stepForward);
  document.getElementById('btn-play-pause').addEventListener('click', player.togglePlay);

  const speedSelect = document.getElementById('speed-select');
  speedSelect.addEventListener('change', () => player.setSpeed(Number(speedSelect.value)));

  // Il listener resta agganciato anche a barra nascosta: un input di tipo "range" non smette di
  // generare eventi solo perché il suo contenitore è "hidden", e in futuro potrebbe tornare visibile.
  const timeline = document.getElementById('timeline');
  timeline.addEventListener('input', () => player.jumpToIndex(Number(timeline.value)));
}

function populateSpeedOptions() {
  const select = document.getElementById('speed-select');
  SPEED_OPTIONS.forEach(option => {
    const optionEl = el('option');
    optionEl.value = String(option.intervalMs);
    optionEl.textContent = option.label;
    optionEl.selected = option.intervalMs === DEFAULT_STEP_INTERVAL_MS;
    select.appendChild(optionEl);
  });
}

// Il percorso: una stazione per ogni prova prevista, costruita una sola volta con la lunghezza
// pianificata. Il numero di stazioni non cambia mai durante la riproduzione, quindi qui si
// costruiscono gli elementi; è "updateTrialPath" che, a ogni passo, aggiorna stato ed etichette.
function buildTrialPath(plannedTrials, onStationClick) {
  const path = document.getElementById('trial-path');
  clearChildren(path);
  for (let stationNumber = 1; stationNumber <= plannedTrials; stationNumber++) {
    path.appendChild(buildTrialStationItem(stationNumber, onStationClick));
  }
}

function buildTrialStationItem(stationNumber, onStationClick) {
  const item = el('li', 'trial-path-item');
  const station = el('button', 'trial-station');
  station.type = 'button';
  station.textContent = String(stationNumber);
  station.dataset.trialNumber = String(stationNumber);
  station.addEventListener('click', () => onStationClick(stationNumber));
  item.appendChild(station);
  return item;
}

// La stazione corrente si legge da "trialNumber" nei momenti di prova e di procedura, e da
// "conclusion.lastTrial" nel momento di conclusione, che "trialNumber" non lo porta.
function currentTrialNumberOf(moment) {
  return moment.kind === 'conclusion' ? moment.conclusion.lastTrial : moment.trialNumber;
}

// L'esito compare sulla stazione corrente solo quando è già rivelato altrove: nell'intestazione
// (showOutcome) o nel momento di conclusione. Prima di allora la stazione non anticipa niente.
function revealedOutcomeLabelOf(moment) {
  if (moment.kind === 'conclusion') {
    return RUN_OUTCOME_LABELS[moment.conclusion.outcome];
  }
  return moment.showOutcome ? TRIAL_OUTCOME_LABELS[moment.outcome] : null;
}

// La stazione oltre la corrente è disegnata e trattata sempre allo stesso modo, che quella prova
// sia stata giocata o no: è tutto il vincolo di non-spoiler, in una funzione che non guarda mai
// "chronicle.trials" per le stazioni future. Per una stazione già superata dal segnaposto corrente,
// invece, la prova è per forza già stata giocata: qui si distingue chi l'ha vinta ("passed") da chi
// l'ha solo attraversata senza vittoria ("crossed"), leggendo "trialOutcomesByNumber" — la mappa
// costruita una sola volta dalle sole prove davvero giocate.
function trialStationStateOf(stationNumber, currentTrialNumber, trialOutcomesByNumber) {
  if (stationNumber > currentTrialNumber) {
    return 'future';
  }
  if (stationNumber === currentTrialNumber) {
    return 'current';
  }
  return trialOutcomesByNumber[stationNumber] === 'STOOD_WITHOUT_WINNING' ? 'crossed' : 'passed';
}

function trialStationLabel(stationNumber, state, outcomeLabel) {
  if (state === 'passed') {
    return `Prova ${stationNumber}, superata`;
  }
  if (state === 'crossed') {
    return `Prova ${stationNumber}, attraversata senza vittoria`;
  }
  if (state === 'future') {
    return `Prova ${stationNumber}, da raggiungere`;
  }
  return outcomeLabel ? `Prova ${stationNumber}, corrente, ${outcomeLabel}` : `Prova ${stationNumber}, corrente`;
}

function updateTrialPath(moment, trialOutcomesByNumber) {
  const currentTrialNumber = currentTrialNumberOf(moment);
  const outcomeLabel = revealedOutcomeLabelOf(moment);
  document.querySelectorAll('#trial-path .trial-station').forEach(station => {
    const stationNumber = Number(station.dataset.trialNumber);
    const state = trialStationStateOf(stationNumber, currentTrialNumber, trialOutcomesByNumber);
    station.className = `trial-station ${state}`;
    station.disabled = state === 'future';
    station.setAttribute('aria-label', trialStationLabel(stationNumber, state, outcomeLabel));
    if (state === 'current') {
      station.setAttribute('aria-current', 'step');
    } else {
      station.removeAttribute('aria-current');
    }
  });
}

function showError(message) {
  const banner = document.getElementById('error-banner');
  banner.textContent = message;
  banner.hidden = false;
}

function bootstrap() {
  fetch(CHRONICLE_URL)
      .then(response => {
        if (!response.ok) {
          throw new Error(`il server ha risposto con stato ${response.status}`);
        }
        return response.json();
      })
      .then(chronicle => {
        const moments = buildMoments(chronicle);
        if (moments.length === 0) {
          showError('La cronaca non contiene alcun momento da riprodurre.');
          return;
        }
        const trialOutcomesByNumber = buildTrialOutcomesByNumber(chronicle.trials);
        populateSpeedOptions();
        // "player" nasce dopo il percorso ma il gestore di clic lo cattura per riferimento: al
        // clic, che può avvenire solo a bootstrap concluso, la variabile è già assegnata.
        let player;
        buildTrialPath(chronicle.plannedTrials, stationNumber => player.jumpToTrial(stationNumber));
        player = createPlayer(moments, moment => renderMoment(moment, trialOutcomesByNumber), updateControls);
        wireControls(player);
        document.getElementById('app').hidden = false;
      })
      .catch(error => {
        showError(`Impossibile caricare la cronaca della partita: ${error.message}.`);
      });
}

document.addEventListener('DOMContentLoaded', bootstrap);
