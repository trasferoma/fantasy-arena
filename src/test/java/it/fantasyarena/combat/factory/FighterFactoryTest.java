package it.fantasyarena.combat.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.model.Fighter;

/**
 * Verifica {@link FighterFactory#createChallengers(int)}: numerosità richiesta, nomi tutti
 * distinti (il generatore del toolkit pesca da liste per razza: con più guerrieri la collisione è
 * plausibile) e rarità di arma/armatura condivise da tutti, cosi' che nessuno parta avvantaggiato.
 */
class FighterFactoryTest {

  private final FighterFactory factory = FighterFactory.withDefaultRatings(CombatSettings.defaults());

  @Test
  void creaCinqueCombattentiConCinqueNomiDistintiEStessaRarita() {
    List<Fighter> fighters = factory.createChallengers(5);

    assertEquals(5, fighters.size());

    Set<String> names = fighters.stream().map(Fighter::name).collect(Collectors.toCollection(HashSet::new));
    assertEquals(5, names.size(), "i 5 combattenti devono avere 5 nomi tutti distinti");

    Set<String> weaponRarities = fighters.stream().map(fighter -> fighter.weapon().rarity().name())
        .collect(Collectors.toCollection(HashSet::new));
    Set<String> armourRarities = fighters.stream()
        .flatMap(fighter -> fighter.armourPieces().stream())
        .map(piece -> piece.rarity().name())
        .collect(Collectors.toCollection(HashSet::new));
    assertEquals(1, weaponRarities.size(), "l'arma deve avere la stessa rarità per tutti");
    assertEquals(1, armourRarities.size(), "l'armatura deve avere la stessa rarità per tutti");
  }

  @Test
  void rifiutaUnaNumerositaMinoreDiUno() {
    assertThrows(IllegalArgumentException.class, () -> factory.createChallengers(0));
  }
}
