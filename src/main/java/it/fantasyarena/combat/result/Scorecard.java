package it.fantasyarena.combat.result;

import it.fantasyarena.combat.config.CombatSettings.ScoreWeights;

/**
 * Riepilogo del calcolo a punti di UN combattente per la decisione di timeout: percentuale di
 * Salute propria e dell'avversario, conteggi grezzi di colpi a segno/parate/schivate, i pesi
 * applicati e il punteggio di ciascuna voce già moltiplicato, oltre al totale. Nessuna formula
 * qui: ogni punteggio arriva già calcolato da {@code CombatFormulas}/{@code CombatEngine}, questo
 * record è solo un contenitore di dati pronti per la stampa (il logger si limita a leggerli).
 */
public record Scorecard(
    String fighterName,
    double healthRatio,
    double opponentHealthRatio,
    int healthPoints,
    int hitsLanded,
    int hitPoints,
    int parries,
    int parryPoints,
    int dodges,
    int dodgePoints,
    ScoreWeights weights,
    int total) {
}
