package it.fantasyarena.combat.chronicle;

import java.util.List;

import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkitcore.core.model.CharacterClass;
import it.fantasytoolkitcore.core.model.Race;

/**
 * Fotografia di un combattente nello scontro, con esattamente i dati che
 * {@code it.fantasyarena.combat.io.render.FighterCardFormatter} legge da un {@code Fighter}: nome,
 * razza, classe, caratteristiche, arma, pezzi d'armatura indossati, vita e stamina massime, rating
 * offensivo e difensivo. Niente stato mutabile — nessuna vita corrente — e niente scudo: la console
 * non lo legge (vedi {@code Fighter.shield()}).
 *
 * <p>{@link #rosterIndex()} e {@link #teamIndex()} sono le posizioni nell'ordine di
 * {@code BattleRoster#all()} (membri della squadra 0, poi quelli della squadra 1): la stessa
 * convenzione con cui {@code EngagementTurn} riferisce attaccante e bersaglio.
 */
public record CombatantSnapshot(int rosterIndex, int teamIndex, String name, Race race, CharacterClass characterClass,
    List<CharacterCharacteristic> characteristics, ItemSnapshot weapon, List<ItemSnapshot> armourPieces,
    int maxHealth, int maxStamina, double offensiveRating, double defensiveRating) {

  public CombatantSnapshot {
    characteristics = List.copyOf(characteristics);
    armourPieces = List.copyOf(armourPieces);
  }
}
