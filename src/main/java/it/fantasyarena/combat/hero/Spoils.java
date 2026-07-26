package it.fantasyarena.combat.hero;

import java.util.Collection;
import java.util.List;

import it.fantasycombatsystem.model.Fighter;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;

/**
 * Quel che resta sul terreno dopo uno scontro vinto: le armi e i pezzi d'armatura degli avversari
 * abbattuti, tutti insieme e senza gerarchia. È materiale grezzo offerto al {@link HeroBrain}, che
 * è l'unico a decidere cosa valga la pena raccogliere.
 *
 * <p>Si saccheggia solo chi è caduto: un avversario ancora in piedi a fine scontro conserva il suo
 * equipaggiamento. Filtrare è responsabilità di {@link #from}, così che il chiamante possa passare
 * l'intero schieramento avversario senza doversi ricordare la regola.
 */
public record Spoils(List<WeaponResult> weapons, List<ArmourResult> armourPieces) {

  public Spoils {
    weapons = List.copyOf(weapons);
    armourPieces = List.copyOf(armourPieces);
  }

  /**
   * Il bottino degli avversari effettivamente abbattuti fra quelli indicati.
   */
  public static Spoils from(Collection<Fighter> opponents) {
    List<Fighter> fallen = opponents.stream()
        .filter(Fighter::isDefeated)
        .toList();
    List<WeaponResult> weapons = fallen.stream()
        .map(Fighter::weapon)
        .toList();
    List<ArmourResult> armourPieces = fallen.stream()
        .flatMap(fighter -> fighter.armourPieces().stream())
        .toList();
    return new Spoils(weapons, armourPieces);
  }

  public boolean isEmpty() {
    return weapons.isEmpty() && armourPieces.isEmpty();
  }
}
