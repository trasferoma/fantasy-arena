package it.fantasyarena.combat.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.hero.Hero;
import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.model.Fighter;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.Rarity;

/**
 * La generazione al servizio dell'arena del protagonista: la scheda iniziale, la materializzazione
 * del combattente di ogni round e lo sfidante speculare dell'ultimo. Verifica le proprietà
 * dichiarate (pari punti, pari numero di pezzi, arma rara), non i valori estratti a caso.
 */
class ArenaFighterFactoryTest {

  private final FighterFactory factory = FighterFactory.withDefaultRatings(CombatSettings.defaults());

  @Test
  void ilProtagonistaParteConUnArmaEUnPezzoDArmatura() {
    Hero hero = factory.createProtagonist();

    assertEquals(1, hero.armourPieceCount());
    assertEquals(Rarity.UNCOMMON, hero.weapon().rarity());
    assertEquals(Rarity.UNCOMMON, hero.armourPieces().getFirst().rarity());
    assertTrue(hero.totalCharacteristicPoints() > 0);
  }

  @Test
  void ogniDiscesaInCampoProduceUnCombattenteNuovoAVitaPiena() {
    Hero hero = factory.createProtagonist();

    Fighter firstRound = factory.summon(hero);
    firstRound.state().applyDamage(firstRound.ratings().maxHealth());
    Fighter secondRound = factory.summon(hero);

    assertTrue(firstRound.isDefeated(), "il combattente del round precedente resta ferito");
    assertNotSame(firstRound, secondRound);
    assertEquals(secondRound.ratings().maxHealth(), secondRound.state().currentHealth(),
        "il round successivo comincia con la vita piena");
    assertEquals(secondRound.ratings().maxStamina(), secondRound.state().currentStamina());
  }

  @Test
  void ilCombattenteMaterializzatoIndossaTuttiIPezziDellaScheda() {
    Hero protagonist = factory.createProtagonist();
    Hero hero = protagonist.wearing(piece(anySlotUncoveredOf(protagonist), 5));

    Fighter fighter = factory.summon(hero);

    assertEquals(hero.armourPieces(), fighter.armourPieces());
    assertEquals(2, fighter.armourPieces().size());
  }

  @Test
  void loSfidanteSpecularePareggiaPuntiEPezziMaImpugnaUnArmaRara() {
    Hero hero = wearingTwoMorePieces(factory.createProtagonist());

    Fighter rival = factory.createMirrorRival(hero);

    int rivalPoints = rival.character().characteristics().stream()
        .mapToInt(CharacterCharacteristic::value)
        .sum();
    assertEquals(hero.totalCharacteristicPoints(), rivalPoints, "lo specchio deve pareggiare i punti caratteristica");
    assertEquals(hero.armourPieceCount(), rival.armourPieces().size());
    assertEquals(Rarity.RARE, rival.weapon().rarity(), "l'unico vantaggio dichiarato è l'arma");
  }

  @Test
  void loSfidanteSpecularePortaPezziSuSlotTuttiDiversi() {
    Hero hero = wearingTwoMorePieces(factory.createProtagonist());

    Fighter rival = factory.createMirrorRival(hero);

    Set<Armour> slots = rival.armourPieces().stream()
        .map(ArmourResult::armour)
        .collect(Collectors.toCollection(HashSet::new));
    assertEquals(rival.armourPieces().size(), slots.size(), "non si indossano due pezzi dello stesso slot");
  }

  @Test
  void gliSfidantiHannoNomiDistintiDaTuttiQuelliGiaScesiInCampo() {
    Hero hero = factory.createProtagonist();
    List<Fighter> firstRound = factory.createChallengers(1);
    List<Fighter> secondRound = factory.createChallengers(2);
    Fighter rival = factory.createMirrorRival(hero);

    Set<String> names = new HashSet<>();
    names.add(hero.name());
    firstRound.forEach(fighter -> names.add(fighter.name()));
    secondRound.forEach(fighter -> names.add(fighter.name()));
    names.add(rival.name());

    assertEquals(5, names.size(), "i nomi devono restare distinti su tutta l'arena, non solo dentro un round");
  }

  /**
   * Il pezzo con cui nasce il protagonista sta su uno slot <em>casuale</em>: aggiungergliene uno
   * scelto a tavolino lo rimpiazzerebbe una volta su sette invece di aggiungersi, e il test
   * fallirebbe di rado e senza un motivo apparente. Gli slot liberi vanno cercati, non assunti.
   */
  private Hero wearingTwoMorePieces(Hero hero) {
    Hero withOneMore = hero.wearing(piece(anySlotUncoveredOf(hero), 2));
    return withOneMore.wearing(piece(anySlotUncoveredOf(withOneMore), 2));
  }

  private Armour anySlotUncoveredOf(Hero hero) {
    return Arrays.stream(Armour.values())
        .filter(slot -> hero.pieceCovering(slot).isEmpty())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("nessuno slot libero: il protagonista è coperto ovunque"));
  }

  private ArmourResult piece(Armour slot, int defense) {
    return new ArmourResult(slot, Rarity.COMMON, List.of(), List.of(), defense);
  }
}
