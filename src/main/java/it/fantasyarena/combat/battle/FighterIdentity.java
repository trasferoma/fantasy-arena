package it.fantasyarena.combat.battle;

import java.util.List;

import it.fantasyarena.combat.model.Fighter;

/**
 * Confronti fra combattenti per identità di riferimento, punto unico per tutto il package.
 *
 * <p>Esiste per rendere esplicita un'intenzione che altrimenti resterebbe implicita: due
 * combattenti diversi possono avere lo stesso nome, e {@link Fighter} non ridefinisce
 * {@code equals}, quindi l'unico criterio corretto per riconoscerli è il riferimento. Un
 * {@code List.contains(fighter)} farebbe già la stessa cosa, ma solo perché {@code equals} non
 * è ridefinito: si appoggerebbe a un dettaglio del model invece di dichiarare la regola. Se un
 * giorno {@code Fighter} guadagnasse un {@code equals} basato sul nome, quel codice comincerebbe
 * a confondere omonimi in silenzio, mentre questo continuerebbe a funzionare.
 */
final class FighterIdentity {

  private FighterIdentity() {
  }

  /**
   * Vero se {@code target} è presente in {@code fighters} come stessa istanza.
   */
  static boolean containsSame(List<Fighter> fighters, Fighter target) {
    for (Fighter fighter : fighters) {
      if (fighter == target) {
        return true;
      }
    }
    return false;
  }
}
