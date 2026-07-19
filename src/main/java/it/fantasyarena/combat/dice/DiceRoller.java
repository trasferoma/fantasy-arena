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

  private DiceThrow roll(int faces) {
    DiceRollResult result = DiceLauncherTool.building()
        .dice(1, faces)
        .roll();
    return new DiceThrow(result.total(), faces);
  }
}
