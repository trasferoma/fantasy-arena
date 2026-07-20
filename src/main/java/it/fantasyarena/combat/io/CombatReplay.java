package it.fantasyarena.combat.io;

import it.fantasyarena.combat.result.CombatResult;

/**
 * Strategia di presentazione del replay del combattimento: dato l'esito completo del duello
 * (log dei turni già calcolato dal motore), lo rivela all'utente turno dopo turno, scandito
 * da un {@link TurnPacer}. Consente di sostituire la modalità di presentazione (lineare su
 * console, a pagina con barre) senza toccare il motore di combattimento.
 */
public interface CombatReplay {

  void replay(CombatResult outcome);
}
