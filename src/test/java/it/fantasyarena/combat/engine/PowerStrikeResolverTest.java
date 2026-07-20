package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.testsupport.CombatFixtures;

/**
 * DoD 2 della SPEC colpo-potente: la decisione del {@link PowerStrikeResolver} e' pura (nessun
 * tiro interno, nessuna verifica di affordabilita'), e dipende da stamina/vita residue (parte
 * razionale), dall'overconfidence generata da un momentum positivo (attenuata da
 * un'Intelligenza alta) e da un micro-jitter iniettato dallo shell.
 */
class PowerStrikeResolverTest {

  private final PowerStrikeResolver resolver = new PowerStrikeResolver(CombatSettings.defaults());

  @Test
  void staminaEVitaAlte_conIntelligenzaAlta_scegliePotente() {
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 10, 5, 5, 5, 5, 20, 5, 0);

    assertTrue(resolver.decide(attacker, new DiceThrow(1, 6)),
        "stamina e vita piene, con Intelligenza alta, devono spingere verso il colpo potente");
  }

  @Test
  void staminaEVitaBasse_conIntelligenzaAlta_nonSceglePotente() {
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 10, 5, 5, 5, 5, 20, 5, 0);
    attacker.state().consumeStamina(attacker.ratings().maxStamina() - 2);
    attacker.state().applyDamage(attacker.ratings().maxHealth() - 5);

    assertFalse(resolver.decide(attacker, new DiceThrow(6, 6)),
        "stamina e vita basse non devono spingere verso il colpo potente, nemmeno col jitter massimo");
  }

  @Test
  void intelligenzaBassaEMomentumAlto_sceglieAncheInSituazioneSfavorevole() {
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 10, 5, 5, 5, 5, 1, 5, 0);
    attacker.state().consumeStamina(attacker.ratings().maxStamina() - 2);
    attacker.state().applyDamage(attacker.ratings().maxHealth() - 5);
    attacker.state().setMomentum(100);

    assertTrue(resolver.decide(attacker, new DiceThrow(3, 6)),
        "l'overconfidence da momentum alto, non attenuata da un'Intelligenza bassa, puo' far "
            + "scegliere il colpo potente anche in una situazione sfavorevole");
  }

  @Test
  void intelligenzaAlta_annullaLOverconfidence_stessoScenarioSfavorevole() {
    Fighter attacker = CombatFixtures.createFighter("Attaccante", 10, 5, 5, 5, 5, 20, 5, 0);
    attacker.state().consumeStamina(attacker.ratings().maxStamina() - 2);
    attacker.state().applyDamage(attacker.ratings().maxHealth() - 5);
    attacker.state().setMomentum(100);

    assertFalse(resolver.decide(attacker, new DiceThrow(3, 6)),
        "a parita' di scenario sfavorevole e momentum alto, un'Intelligenza alta deve annullare "
            + "l'overconfidence");
  }

  @Test
  void jitter_spostaUnCasoBorderline() {
    Fighter lowJitterAttacker = CombatFixtures.createFighter("Attaccante", 10, 5, 5, 5, 5, 10, 5, 0);
    lowJitterAttacker.state().consumeStamina(13);
    lowJitterAttacker.state().applyDamage(28);

    Fighter highJitterAttacker = CombatFixtures.createFighter("Attaccante", 10, 5, 5, 5, 5, 10, 5, 0);
    highJitterAttacker.state().consumeStamina(13);
    highJitterAttacker.state().applyDamage(28);

    double lowJitterScore = resolver.score(lowJitterAttacker, new DiceThrow(1, 6));
    double highJitterScore = resolver.score(highJitterAttacker, new DiceThrow(6, 6));

    assertEquals(0.5188, lowJitterScore, 0.0005, "score atteso appena sotto la soglia col jitter minimo");
    assertEquals(0.6855, highJitterScore, 0.0005, "score atteso sopra la soglia col jitter massimo");
    assertFalse(resolver.decide(lowJitterAttacker, new DiceThrow(1, 6)),
        "a parita' di scenario, il jitter minimo non deve superare la soglia");
    assertTrue(resolver.decide(highJitterAttacker, new DiceThrow(6, 6)),
        "a parita' di scenario, il jitter massimo deve superare la soglia");
  }
}
