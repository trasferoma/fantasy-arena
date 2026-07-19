package it.fantasyarena.combat.result;

/**
 * Voce di log di un singolo turno: numero del turno e descrizione testuale di ciò che è
 * accaduto (in italiano, pronta per la stampa su console).
 */
public record TurnLogEntry(int turnNumber, String description) {
}
