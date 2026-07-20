package it.fantasyarena.combat.engine;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.InitiativeReport;

/**
 * Ponte fra la risoluzione dell'iniziativa e il resto del motore: {@code chosen} è il
 * {@link Fighter} mutabile scelto come prossimo attaccante, usato dal {@code CombatEngine} per
 * proseguire il duello; {@code report} è la versione loggabile della stessa decisione, senza
 * riferimenti al model mutabile, usata dal logger.
 */
public record InitiativeDecision(Fighter chosen, InitiativeReport report) {
}
