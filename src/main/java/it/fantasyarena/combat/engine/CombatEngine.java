package it.fantasyarena.combat.engine;

import java.util.List;

import it.fantasyarena.combat.battle.BattleEngine;
import it.fantasyarena.combat.battle.BattleResult;
import it.fantasyarena.combat.battle.BattleSetup;
import it.fantasyarena.combat.battle.DuelResultAdapter;
import it.fantasyarena.combat.battle.OutnumberedAllyAssigner;
import it.fantasyarena.combat.battle.PairwiseEngagementPlanner;
import it.fantasyarena.combat.battle.StickyTargetSelector;
import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceRoller;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.CombatResult;

/**
 * Adapter sottile del duello 1v1 storico sopra {@link BattleEngine}: presenta la battaglia
 * degenere a due squadre da un solo membro nella vista {@link CombatResult} di sempre. Nessun
 * loop qui: il duello è l'esatto caso degenere della battaglia NvN, orchestrata da
 * {@link BattleEngine} e riportata al vecchio contratto da {@link DuelResultAdapter}.
 */
public class CombatEngine {

  private final BattleEngine battleEngine;

  public CombatEngine(DiceRoller diceRoller, InitiativeResolver initiativeResolver, TurnOrchestrator turnOrchestrator,
      CombatSettings settings) {
    StaminaRules staminaRules = new StaminaRules(settings);
    this.battleEngine = new BattleEngine(diceRoller, initiativeResolver, turnOrchestrator, staminaRules, settings,
        new PairwiseEngagementPlanner(), new StickyTargetSelector(), new OutnumberedAllyAssigner());
  }

  public CombatResult fight(Fighter first, Fighter second, CombatContext context) {
    BattleResult battle = battleEngine.fight(BattleSetup.duel(first, second), context);
    return DuelResultAdapter.toCombatResult(battle);
  }

  /**
   * Conservato: un test lo esercita direttamente sul duello.
   */
  void armInitialPowerStrikeCooldown(Fighter first, Fighter second) {
    battleEngine.armInitialPowerStrikeCooldown(List.of(first, second));
  }
}
