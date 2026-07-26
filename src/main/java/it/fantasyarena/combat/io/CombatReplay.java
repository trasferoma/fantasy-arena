package it.fantasyarena.combat.io;

import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.result.CombatResult;

/**
 * Strategia di presentazione del replay del combattimento: dato l'esito completo del duello
 * (log dei turni già calcolato dal motore) e i due combattenti, lo rivela all'utente turno
 * dopo turno, scandito da un {@link TurnPacer}. Consente di sostituire la modalità di
 * presentazione (lineare su console, a pagina con barre) senza toccare il motore di
 * combattimento.
 */
public interface CombatReplay {

  void replay(CombatResult outcome, Fighter first, Fighter second);
}
