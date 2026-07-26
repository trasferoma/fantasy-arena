package it.fantasyarena.combat.battle;

import java.util.ArrayList;
import java.util.List;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.context.CombatContext;
import it.fantasyarena.combat.dice.DiceRoller;
import it.fantasyarena.combat.dice.DiceThrow;
import it.fantasyarena.combat.engine.InitiativeDecision;
import it.fantasyarena.combat.engine.InitiativeResolver;
import it.fantasyarena.combat.engine.TurnOrchestrator;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.result.FighterVitals;
import it.fantasyarena.combat.result.InitiativeOverride;
import it.fantasyarena.combat.result.TurnLogEntry;
import it.fantasyarena.combat.result.TurnResult;

/**
 * Guscio attorno alla "scatola" {@link TurnOrchestrator#playTurn}: gioca UN scambio di UNO
 * scontro in UN round. Risolve l'iniziativa fra i partecipanti vivi dello scontro con gli stessi
 * override deterministici del duello 1v1 ({@link InitiativeOverride#DODGE_STEAL} e
 * {@link InitiativeOverride#REST_YIELD}), applica lo shift d'iniziativa a tutti i partecipanti
 * vivi dello scontro, sceglie il bersaglio fra i nemici vivi presenti nello SCONTRO (non fra
 * tutti i nemici della battaglia) e delega alla scatola. Non imposta le {@code staminaChanges}
 * della {@link TurnLogEntry} restituita: le imposta {@link BattleEngine} a fine round, quando
 * conosce tutti gli scambi giocati nel round.
 */
public final class EngagementTurnPlayer {

  private final DiceRoller diceRoller;
  private final InitiativeResolver initiativeResolver;
  private final TurnOrchestrator turnOrchestrator;
  private final TargetSelector targetSelector;
  private final CombatSettings settings;

  public EngagementTurnPlayer(DiceRoller diceRoller, InitiativeResolver initiativeResolver,
      TurnOrchestrator turnOrchestrator, TargetSelector targetSelector, CombatSettings settings) {
    this.diceRoller = diceRoller;
    this.initiativeResolver = initiativeResolver;
    this.turnOrchestrator = turnOrchestrator;
    this.targetSelector = targetSelector;
    this.settings = settings;
  }

  public PlayedExchange play(int roundNumber, Engagement engagement, BattleRoster roster,
      List<FighterVitals> startOfRoundVitals, CombatContext context) {
    InitiativeDecision initiativeDecision = resolveInitiative(engagement);
    Fighter actor = initiativeDecision.chosen();
    applyInitiativeShift(engagement, actor);

    List<Fighter> livingEnemies = livingEnemiesWithin(engagement, roster, actor);
    Fighter target = targetSelector.selectTarget(actor, engagement.currentTargetOf(actor), livingEnemies);

    TurnResult turnResult = turnOrchestrator.playTurn(roundNumber, actor, target, context);
    TurnLogEntry logEntry = turnResult.logEntry()
        .withVitals(startOfRoundVitals)
        .withInitiative(initiativeDecision.report());

    engagement.recordExchange(actor, target, turnResult.override());

    return new PlayedExchange(engagement, actor, target, logEntry);
  }

  private InitiativeDecision resolveInitiative(Engagement engagement) {
    InitiativeOverride override = engagement.pendingOverride();
    List<Fighter> order = engagement.initiativeOrder();

    return switch (override) {
      case NONE -> resolveByScore(order);
      case DODGE_STEAL -> resolveStolenTime(engagement, override);
      case REST_YIELD -> resolveYieldedTime(engagement, order, override);
    };
  }

  /**
   * Nessun override: il test a punteggio va davvero eseguito, un jitter per partecipante
   * nell'ordine di {@code order}. Estrazione con un {@code for} esplicito, mai con uno stream:
   * l'ordine di consumo dei dadi e' cio' su cui si regge l'equivalenza col duello 1v1.
   */
  private InitiativeDecision resolveByScore(List<Fighter> order) {
    List<DiceThrow> jitters = new ArrayList<>(order.size());
    for (int i = 0; i < order.size(); i++) {
      jitters.add(rollJitter());
    }
    return initiativeResolver.resolveInitiative(order, jitters);
  }

  /**
   * Il tempo e' rubato: agisce chi ha schivato, cioe' il bersaglio corrente dell'ultimo attore.
   * Zero dadi. Chi ha schivato non subisce danno per definizione, quindi deve essere vivo e
   * ancora presente nello scontro: se non lo fosse, e' un invariante del motore violato, non un
   * caso da coprire con un fallback silenzioso.
   */
  private InitiativeDecision resolveStolenTime(Engagement engagement, InitiativeOverride override) {
    Fighter thief = engagement.currentTargetOf(engagement.lastActor());
    if (thief == null || !isAliveInEngagement(engagement, thief)) {
      throw new IllegalStateException(
          "DODGE_STEAL richiede che chi ha schivato sia vivo e partecipe dello scontro " + engagement.id()
              + ", trovato: " + thief);
    }
    return initiativeResolver.stolenTime(thief, override);
  }

  /**
   * Il tempo e' ceduto: chi ha riposato e' escluso e il test si svolge fra i restanti candidati
   * ({@code order.size() - 1}). Con un solo candidato residuo (il caso 1v1) nessun jitter va
   * tirato: e' cio' che rende il duello binario a zero dadi, identico a oggi.
   */
  private InitiativeDecision resolveYieldedTime(Engagement engagement, List<Fighter> order,
      InitiativeOverride override) {
    Fighter yielder = engagement.lastActor();
    int candidateCount = order.size() - 1;

    List<DiceThrow> jitters = new ArrayList<>();
    if (candidateCount > 1) {
      for (int i = 0; i < candidateCount; i++) {
        jitters.add(rollJitter());
      }
    }
    return initiativeResolver.yieldedTime(yielder, order, jitters, override);
  }

  private DiceThrow rollJitter() {
    int jitterDiceFaces = settings.initiativeWeights().jitterDiceFaces();
    return diceRoller.roll(jitterDiceFaces);
  }

  /**
   * L'attore prosegue/avvia la propria catena di attacchi consecutivi, tutti gli altri
   * partecipanti vivi dello scontro la azzerano: esattamente lo shift d'iniziativa del duello
   * 1v1, generalizzato a N partecipanti.
   */
  private void applyInitiativeShift(Engagement engagement, Fighter actor) {
    actor.state().winInitiative();
    for (Fighter participant : engagement.livingParticipants()) {
      if (participant != actor) {
        participant.state().loseInitiative();
      }
    }
  }

  private List<Fighter> livingEnemiesWithin(Engagement engagement, BattleRoster roster, Fighter actor) {
    return engagement.livingParticipants().stream()
        .filter(participant -> !roster.areAllies(actor, participant))
        .toList();
  }

  private boolean isAliveInEngagement(Engagement engagement, Fighter fighter) {
    for (Fighter living : engagement.livingParticipants()) {
      if (living == fighter) {
        return true;
      }
    }
    return false;
  }
}
