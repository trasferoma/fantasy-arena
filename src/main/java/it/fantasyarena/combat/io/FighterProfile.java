package it.fantasyarena.combat.io;

/**
 * Profilo stabile di un combattente per {@link BattleSceneRenderer}: nome, indice di squadra
 * (0 o 1) e i valori massimi di vita e stamina, nell'ordine di roster della battaglia. Non
 * dipende da {@code Fighter}: e' il solo dato che il renderer conosce per calcolare, una volta
 * per l'intera battaglia, larghezze di colonna deterministiche e indipendenti dal contenuto del
 * singolo round.
 */
public record FighterProfile(String name, int teamIndex, int maxHealth, int maxStamina) {
}
