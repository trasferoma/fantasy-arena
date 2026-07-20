package it.fantasyarena.combat.result;

/**
 * Breakdown loggabile del punteggio d'iniziativa di un combattente: i quattro contributi
 * (stamina, agilità, intelligenza, jitter) già pesati, il totale che ne risulta e i valori
 * grezzi (stamina corrente/massima, agilità, intelligenza, valore del dado del jitter) da
 * mostrare nel log accanto ai contributi pesati. Prodotto dall'{@code InitiativeResolver}
 * solo per scopi di log, mai per rifare il calcolo.
 */
public record InitiativeBreakdown(
    String name,
    double staminaComponent,
    double agilityComponent,
    double intelligenceComponent,
    double jitterComponent,
    double total,
    int currentStamina,
    int maxStamina,
    int agility,
    int intelligence,
    int jitterValue) {
}
