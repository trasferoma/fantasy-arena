package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.factory.FighterFactory;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.InitiativeOverride;

/**
 * Verifica la scelta dell'iniziativa dell'{@link InitiativeResolver}: senza override vince chi
 * ha il rapporto Stamina corrente/massima più alto (peso dominante della formula), mentre un
 * override da schivata o riposo forza deterministicamente il difensore corrente come prossimo
 * attaccante, ignorando il punteggio. Le asserzioni confrontano sempre l'identità del
 * {@link Fighter} (non il nome), perché due combattenti generati possono avere lo stesso nome.
 */
class InitiativeResolverTest {

  private CombatSettings settings;
  private InitiativeResolver resolver;
  private Fighter a;
  private Fighter b;
  private DiceThrow lowJitter;
  private DiceThrow highJitter;

  @BeforeEach
  void setUp() {
    settings = CombatSettings.defaults();
    resolver = new InitiativeResolver(settings);
    FighterFactory factory = FighterFactory.withDefaultRatings(settings);
    FighterFactory.Duelists duelists = factory.createMatchedSwordWarriors();
    a = duelists.first();
    b = duelists.second();

    int faces = settings.initiativeWeights().jitterDiceFaces();
    lowJitter = new DiceThrow(1, faces);
    highJitter = new DiceThrow(faces, faces);
  }

  @Test
  void resolveNextAttacker_conDodgeSteal_sceglieIlDifensoreCorrenteIgnorandoIlPunteggio() {
    b.state().consumeStamina(b.ratings().maxStamina());

    InitiativeDecision decision =
        resolver.resolveNextAttacker(a, b, InitiativeOverride.DODGE_STEAL, highJitter, lowJitter);

    assertSame(b, decision.chosen());
    assertEquals(InitiativeOverride.DODGE_STEAL, decision.report().override());
  }

  @Test
  void resolveNextAttacker_conRestYield_sceglieIlDifensoreCorrente() {
    b.state().consumeStamina(b.ratings().maxStamina());

    InitiativeDecision decision =
        resolver.resolveNextAttacker(a, b, InitiativeOverride.REST_YIELD, highJitter, lowJitter);

    assertSame(b, decision.chosen());
  }

  @Test
  void resolveNextAttacker_senzaOverride_laStaminaPienaVinceSuQuellaEsaurita() {
    a.state().consumeStamina(a.ratings().maxStamina());

    InitiativeDecision decision =
        resolver.resolveNextAttacker(a, b, InitiativeOverride.NONE, lowJitter, highJitter);

    assertSame(b, decision.chosen());
    assertEquals(b.name(), decision.report().scoreWinnerName());
    assertEquals(b.name(), decision.report().chosenName());
  }

  @Test
  void resolveFirstMover_laStaminaPienaVinceSuQuellaEsaurita() {
    a.state().consumeStamina(a.ratings().maxStamina());

    InitiativeDecision decision = resolver.resolveFirstMover(a, b, lowJitter, highJitter);

    assertSame(b, decision.chosen());
    assertEquals(InitiativeOverride.NONE, decision.report().override());
  }
}
