package it.fantasyarena.combat.dice;

import it.fantasytoolkit.dicelauncher.DiceLauncherTool;
import it.fantasytoolkit.dicelauncher.result.DiceRollResult;

/**
 * Facade sottile sopra {@link DiceLauncherTool}: unico punto di contatto con il toolkit
 * per la generazione di casualità. Restituisce sempre {@link DiceThrow} tipizzati, che lo
 * shell passa come input ai resolver puri del core.
 */
public class DiceRoller {

  public DiceThrow d20() {
    return roll(20);
  }

  public DiceThrow d100() {
    return roll(100);
  }

  /**
   * Tiro parametrico di un dado a {@code faces} facce. Usato per il micro-jitter
   * dell'iniziativa, il cui numero di facce è tarabile in {@code CombatSettings}: rompe pareggi
   * e simmetrie senza lasciare che il caso decida da solo l'ordine dei turni.
   */
  public DiceThrow roll(int faces) {
    DiceRollResult result = DiceLauncherTool.building()
        .dice(1, faces)
        .roll();
    return new DiceThrow(result.total(), faces);
  }
}
