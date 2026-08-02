package it.fantasyarena.combat.chronicle;

/**
 * La fotografia del monte punti calato su questo protagonista in una stazione a sfidanti generati:
 * quanto la stazione dichiara per l'intero schieramento, lo sconto che la fortuna effettiva del
 * protagonista ha applicato davvero, e il monte che ne è risultato. Traduce {@code ChallengerBudget}
 * senza aggiungere né perdere nulla, come ogni altra fotografia di questo package: nessuna stringa
 * di presentazione, nessuna annotazione Jackson.
 *
 * <p>Esiste perché lo sconto, se non si vede, non risolve il problema da cui nasce: ogni lettore —
 * la console all'annuncio del round, la pagina nel pannello della prova — compone la propria frase a
 * partire da questi tre numeri, senza ricalcolare niente.
 *
 * <p>{@link TrialChronicle#budget()} lo porta {@code null} per la stazione dello specchio, che non
 * dichiara un monte proprio e non passa da nessuno sconto: stessa forma nullable di
 * {@link TrialChronicle#progress()}.
 *
 * @param stationPoints monte punti dichiarato dalla stazione per l'intero schieramento
 * @param luckDiscount sconto effettivamente applicato dalla fortuna del protagonista
 * @param squadPoints monte punti effettivo dello schieramento, {@code stationPoints - luckDiscount}
 */
public record ChallengerBudgetChronicle(int stationPoints, int luckDiscount, int squadPoints) {
}
