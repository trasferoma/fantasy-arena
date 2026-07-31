package it.fantasyarena.combat.chronicle;

import java.util.List;

import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkitcore.core.model.CharacterClass;
import it.fantasytoolkitcore.core.model.Race;

/**
 * Fotografia del protagonista fra uno scontro e l'altro: quello che {@code Hero} custodisce,
 * compresi i gioielli. È una differenza voluta rispetto a {@link CombatantSnapshot}, che i gioielli
 * non li porta: nello scontro non ci sono, il motore non li monta (vedi {@code Hero}, Javadoc di
 * classe).
 */
public record HeroSnapshot(String name, Race race, CharacterClass characterClass,
    List<CharacterCharacteristic> characteristics, ItemSnapshot weapon, List<ItemSnapshot> armourPieces,
    List<ItemSnapshot> jewels) {

  public HeroSnapshot {
    characteristics = List.copyOf(characteristics);
    armourPieces = List.copyOf(armourPieces);
    jewels = List.copyOf(jewels);
  }
}
