package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.InitiativeOverride;
import it.fantasyarena.combat.testsupport.CombatFixtures;

/**
 * Verifica la generalizzazione N-aria dell'{@link InitiativeResolver}: risoluzione fra un
 * numero qualunque di partecipanti, tie-break stabile (a parità vince il primo della lista,
 * esattamente come il confronto binario storico), furto e cessione del tempo. Le asserzioni
 * confrontano sempre l'identità del {@link Fighter} (non il nome), perché combattenti diversi
 * possono avere lo stesso nome.
 */
class InitiativeResolverNAryTest {

  private CombatSettings settings;
  private InitiativeResolver resolver;
  private DiceThrow jitter;

  @BeforeEach
  void setUp() {
    settings = CombatSettings.defaults();
    resolver = new InitiativeResolver(settings);

    int faces = settings.initiativeWeights().jitterDiceFaces();
    jitter = new DiceThrow(faces, faces);
  }

  @Test
  void resolveInitiative_trePartecipanti_vinceIlPunteggioPiuAlto_breakdownsNellOrdineDellaLista() {
    Fighter weak = CombatFixtures.createFighter("Weak", 10, 10, 10, 10, 10, 5, 5);
    Fighter strong = CombatFixtures.createFighter("Strong", 10, 20, 10, 10, 10, 5, 5);
    Fighter medium = CombatFixtures.createFighter("Medium", 10, 15, 10, 10, 10, 5, 5);
    List<Fighter> ordered = List.of(weak, strong, medium);
    List<DiceThrow> jitters = List.of(jitter, jitter, jitter);

    InitiativeDecision decision = resolver.resolveInitiative(ordered, jitters);

    assertSame(strong, decision.chosen());
    assertEquals(strong.name(), decision.report().scoreWinnerName());
    assertEquals(strong.name(), decision.report().chosenName());
    assertEquals(InitiativeOverride.NONE, decision.report().override());
    assertEquals(3, decision.report().breakdowns().size());
    assertEquals(weak.name(), decision.report().breakdowns().get(0).name());
    assertEquals(strong.name(), decision.report().breakdowns().get(1).name());
    assertEquals(medium.name(), decision.report().breakdowns().get(2).name());
  }

  @Test
  void resolveInitiative_pareggio_vinceIlPrimoDellaLista() {
    Fighter first = CombatFixtures.createFighter("Twin", 10, 10, 10, 10, 10, 5, 5);
    Fighter second = CombatFixtures.createFighter("Twin", 10, 10, 10, 10, 10, 5, 5);
    Fighter third = CombatFixtures.createFighter("Twin", 10, 10, 10, 10, 10, 5, 5);
    List<Fighter> ordered = List.of(first, second, third);
    List<DiceThrow> jitters = List.of(jitter, jitter, jitter);

    InitiativeDecision decision = resolver.resolveInitiative(ordered, jitters);

    assertSame(first, decision.chosen(), "a parita' di punteggio deve vincere l'elemento in posizione 0");
  }

  @Test
  void resolveInitiative_pareggio_ePosizioneADecidere_nonLIdentita() {
    Fighter first = CombatFixtures.createFighter("Twin", 10, 10, 10, 10, 10, 5, 5);
    Fighter second = CombatFixtures.createFighter("Twin", 10, 10, 10, 10, 10, 5, 5);
    Fighter third = CombatFixtures.createFighter("Twin", 10, 10, 10, 10, 10, 5, 5);
    List<DiceThrow> jitters = List.of(jitter, jitter, jitter);

    InitiativeDecision permuted = resolver.resolveInitiative(List.of(second, third, first), jitters);

    assertSame(second, permuted.chosen(), "spostando 'second' in posizione 0 deve vincere lui, non 'first'");
  }

  @Test
  void resolveInitiative_duePartecipanti_coerenteConResolveNextAttackerBinario() {
    Fighter attacker = CombatFixtures.createFighter("Attacker", 10, 10, 10, 10, 10, 5, 5);
    Fighter defender = CombatFixtures.createFighter("Defender", 10, 20, 10, 10, 10, 5, 5);
    DiceThrow attackerJitter = new DiceThrow(1, jitter.faces());
    DiceThrow defenderJitter = new DiceThrow(jitter.faces(), jitter.faces());

    InitiativeDecision nAry =
        resolver.resolveInitiative(List.of(attacker, defender), List.of(attackerJitter, defenderJitter));
    InitiativeDecision binary = resolver.resolveNextAttacker(attacker, defender, attackerJitter, defenderJitter);

    assertSame(binary.chosen(), nAry.chosen());
  }

  @Test
  void stolenTime_sceglieIlLadro_senzaBreakdown() {
    Fighter thief = CombatFixtures.createFighter("Thief", 10, 10, 10, 10, 10, 5, 5);

    InitiativeDecision decision = resolver.stolenTime(thief, InitiativeOverride.DODGE_STEAL);

    assertSame(thief, decision.chosen());
    assertEquals(InitiativeOverride.DODGE_STEAL, decision.report().override());
    assertTrue(decision.report().breakdowns().isEmpty());
  }

  @Test
  void yieldedTime_duePartecipanti_unCandidatoResiduo_nessunBreakdown_jittersVuoto() {
    Fighter yielder = CombatFixtures.createFighter("Yielder", 10, 10, 10, 10, 10, 5, 5);
    Fighter other = CombatFixtures.createFighter("Other", 10, 10, 10, 10, 10, 5, 5);

    InitiativeDecision decision =
        resolver.yieldedTime(yielder, List.of(yielder, other), List.of(), InitiativeOverride.REST_YIELD);

    assertSame(other, decision.chosen());
    assertEquals(InitiativeOverride.REST_YIELD, decision.report().override());
    assertTrue(decision.report().breakdowns().isEmpty(), "con un solo candidato residuo non c'e' nulla da testare");
  }

  @Test
  void yieldedTime_trePartecipanti_dueCandidatiResidui_yielderEscluso_vinceIlMiglioreFraICandidati() {
    Fighter yielder = CombatFixtures.createFighter("Yielder", 10, 20, 10, 10, 10, 5, 5);
    Fighter weakCandidate = CombatFixtures.createFighter("WeakCandidate", 10, 10, 10, 10, 10, 5, 5);
    Fighter strongCandidate = CombatFixtures.createFighter("StrongCandidate", 10, 20, 10, 10, 10, 5, 5);
    List<Fighter> ordered = List.of(yielder, weakCandidate, strongCandidate);
    List<DiceThrow> jitters = List.of(jitter, jitter);

    InitiativeDecision decision = resolver.yieldedTime(yielder, ordered, jitters, InitiativeOverride.REST_YIELD);

    assertSame(strongCandidate, decision.chosen());
    assertEquals(2, decision.report().breakdowns().size());
    assertEquals(weakCandidate.name(), decision.report().breakdowns().get(0).name());
    assertEquals(strongCandidate.name(), decision.report().breakdowns().get(1).name());
  }

  @Test
  void resolveInitiative_jittersConDimensioneErrata_lanciaEccezione() {
    Fighter a = CombatFixtures.createFighter("A", 10, 10, 10, 10, 10, 5, 5);
    Fighter b = CombatFixtures.createFighter("B", 10, 10, 10, 10, 10, 5, 5);

    assertThrows(IllegalArgumentException.class,
        () -> resolver.resolveInitiative(List.of(a, b), List.of(jitter)));
  }

  @Test
  void resolveInitiative_listaVuota_lanciaEccezione() {
    assertThrows(IllegalArgumentException.class, () -> resolver.resolveInitiative(List.of(), List.of()));
  }

  @Test
  void yieldedTime_yielderNonPresenteInOrdered_lanciaEccezione() {
    Fighter yielder = CombatFixtures.createFighter("Yielder", 10, 10, 10, 10, 10, 5, 5);
    Fighter a = CombatFixtures.createFighter("A", 10, 10, 10, 10, 10, 5, 5);
    Fighter b = CombatFixtures.createFighter("B", 10, 10, 10, 10, 10, 5, 5);

    assertThrows(IllegalArgumentException.class,
        () -> resolver.yieldedTime(yielder, List.of(a, b), List.of(jitter), InitiativeOverride.REST_YIELD));
  }
}
