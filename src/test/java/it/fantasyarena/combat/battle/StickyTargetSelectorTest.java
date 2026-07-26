package it.fantasyarena.combat.battle;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.testsupport.CombatFixtures;

/**
 * Verifica {@link StickyTargetSelector}: mantenimento del bersaglio finché vivo e presente,
 * ricalcolo del più debole alla sua morte (con i tie-break dichiarati) e la scelta per identità,
 * non per nome, anche fra nemici omonimi.
 */
class StickyTargetSelectorTest {

  private final StickyTargetSelector selector = new StickyTargetSelector();

  @Test
  void selectTarget_bersaglioCorrenteVivoEPresente_lomantiene() {
    Fighter attacker = CombatFixtures.createFighter("Attacker", 15, 15, 15, 15, 15, 10, 10);
    Fighter currentTarget = CombatFixtures.createFighter("Current", 15, 15, 15, 15, 15, 10, 10);
    Fighter other = CombatFixtures.createFighter("Other", 15, 15, 15, 15, 15, 10, 10);
    other.state().applyDamage(1);

    Fighter chosen = selector.selectTarget(attacker, currentTarget, List.of(currentTarget, other));

    assertSame(currentTarget, chosen);
  }

  @Test
  void selectTarget_bersaglioCorrenteAssenteDaiVivi_scegliePerRapportoSaluteMinore() {
    Fighter attacker = CombatFixtures.createFighter("Attacker", 15, 15, 15, 15, 15, 10, 10);
    Fighter weaker = CombatFixtures.createFighter("Weaker", 15, 15, 15, 15, 15, 10, 10);
    Fighter stronger = CombatFixtures.createFighter("Stronger", 15, 15, 15, 15, 15, 10, 10);
    weaker.state().applyDamage(weaker.ratings().maxHealth() / 2);

    Fighter chosen = selector.selectTarget(attacker, null, List.of(stronger, weaker));

    assertSame(weaker, chosen);
  }

  @Test
  void selectTarget_pareggioDiSalute_sceglieStaminaCorrenteMinore() {
    Fighter attacker = CombatFixtures.createFighter("Attacker", 15, 15, 15, 15, 15, 10, 10);
    Fighter tiredEnemy = CombatFixtures.createFighter("Tired", 15, 15, 15, 15, 15, 10, 10);
    Fighter restedEnemy = CombatFixtures.createFighter("Rested", 15, 15, 15, 15, 15, 10, 10);
    tiredEnemy.state().consumeStamina(tiredEnemy.ratings().maxStamina() / 2);

    Fighter chosen = selector.selectTarget(attacker, null, List.of(restedEnemy, tiredEnemy));

    assertSame(tiredEnemy, chosen);
  }

  @Test
  void selectTarget_pareggioTotale_sceglieIlPrimoDellaLista() {
    Fighter attacker = CombatFixtures.createFighter("Attacker", 15, 15, 15, 15, 15, 10, 10);
    Fighter first = CombatFixtures.createFighter("Twin", 15, 15, 15, 15, 15, 10, 10);
    Fighter second = CombatFixtures.createFighter("Twin", 15, 15, 15, 15, 15, 10, 10);

    Fighter chosen = selector.selectTarget(attacker, null, List.of(first, second));

    assertSame(first, chosen);
  }

  @Test
  void selectTarget_listaVuota_lanciaEccezione() {
    Fighter attacker = CombatFixtures.createFighter("Attacker", 15, 15, 15, 15, 15, 10, 10);

    assertThrows(IllegalArgumentException.class, () -> selector.selectTarget(attacker, null, List.of()));
  }

  @Test
  void selectTarget_dueNemiciOmonimi_stickinessPerIdentitaNonPerNome() {
    Fighter attacker = CombatFixtures.createFighter("Attacker", 15, 15, 15, 15, 15, 10, 10);
    Fighter currentTarget = CombatFixtures.createFighter("Twin", 15, 15, 15, 15, 15, 10, 10);
    Fighter homonymEnemy = CombatFixtures.createFighter("Twin", 15, 15, 15, 15, 15, 10, 10);
    homonymEnemy.state().applyDamage(homonymEnemy.ratings().maxHealth() / 2);

    Fighter chosen = selector.selectTarget(attacker, currentTarget, List.of(homonymEnemy, currentTarget));

    assertSame(currentTarget, chosen, "il bersaglio corrente va mantenuto per identita', anche se un "
        + "omonimo piu' debole compare nella lista");
  }
}
