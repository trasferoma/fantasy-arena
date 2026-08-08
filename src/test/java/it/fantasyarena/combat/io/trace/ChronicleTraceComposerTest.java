package it.fantasyarena.combat.io.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.fantasyarena.combat.RoundOutcome;
import it.fantasyarena.combat.chronicle.ArenaChronicle;
import it.fantasyarena.combat.chronicle.ChallengerBudgetChronicle;
import it.fantasyarena.combat.chronicle.CharacteristicBonus;
import it.fantasyarena.combat.chronicle.CombatantSnapshot;
import it.fantasyarena.combat.chronicle.HeroSnapshot;
import it.fantasyarena.combat.chronicle.ItemKind;
import it.fantasyarena.combat.chronicle.ItemSnapshot;
import it.fantasyarena.combat.chronicle.ProgressChronicle;
import it.fantasyarena.combat.chronicle.RunConclusion;
import it.fantasyarena.combat.chronicle.TrialChronicle;
import it.fantasyarena.combat.chronicle.TrialShape;
import it.fantasyarena.combat.hero.HeroProgress.CharacteristicGain;
import it.fantasyarena.combat.hero.LootFate;
import it.fantasycombatsystem.battle.EngagementTurn;
import it.fantasycombatsystem.battle.RoundLogEntry;
import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.result.ActionOutcome;
import it.fantasycombatsystem.result.FighterVitals;
import it.fantasycombatsystem.result.TurnLogEntry;
import it.fantasycombatsystem.telemetry.DamageTelemetry;
import it.fantasycombatsystem.telemetry.DefenseAttemptTelemetry;
import it.fantasycombatsystem.telemetry.DefenseTelemetry;
import it.fantasycombatsystem.telemetry.HitTelemetry;
import it.fantasycombatsystem.telemetry.TurnTelemetry;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkitcore.core.model.CharacterClass;
import it.fantasytoolkitcore.core.model.Characteristic;
import it.fantasytoolkitcore.core.model.Race;
import it.fantasytoolkitcore.core.model.Rarity;

/**
 * Verifica {@link ChronicleTraceComposer} su cronache costruite a mano, mai giocando una partita:
 * la composizione è una funzione pura e non tocca il filesystem.
 */
class ChronicleTraceComposerTest {

  private static final Instant FIXED_INSTANT = Instant.parse("2026-08-08T10:12:03Z");

  private final ChronicleTraceComposer composer =
      new ChronicleTraceComposer(Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
  private final ObjectMapper parser = new ObjectMapper();

  @Test
  void produceUnaRigaPerCiascunoDeiCinqueTipiDiEventoInUnaCorsaCompleta() throws JsonProcessingException {
    ArenaChronicle chronicle = completeChronicle();

    List<String> lines = composer.compose(chronicle);
    List<String> discriminants = discriminantsOf(lines);

    assertEquals(List.of("RUN_OPENED", "TRIAL_STARTED", "EXCHANGE", "EXCHANGE", "TRIAL_ENDED", "TRIAL_STARTED",
        "EXCHANGE", "TRIAL_ENDED", "RUN_CLOSED"), discriminants);
    assertEquals(Set.of("RUN_OPENED", "TRIAL_STARTED", "EXCHANGE", "TRIAL_ENDED", "RUN_CLOSED"),
        Set.copyOf(discriminants));
  }

  @Test
  void ogniRigaEUnJsonValidoEAutosufficiente() {
    List<String> lines = composer.compose(completeChronicle());

    for (String line : lines) {
      assertTrue(isValidJson(line), "riga non valida come JSON: " + line);
    }
  }

  @Test
  void laRigaDiAperturaPortaICombatSettingsConCuiLaCorsaEStataGiocataEListante() throws JsonProcessingException {
    List<String> lines = composer.compose(completeChronicle());

    JsonNode runOpened = parser.readTree(lines.get(0));

    assertEquals(FIXED_INSTANT.toString(), runOpened.get("startedAt").asText());
    assertTrue(runOpened.get("settings").has("maxTurns"));
    assertTrue(runOpened.get("settings").get("chanceWeights").has("baseHitChance"));
    assertTrue(runOpened.has("protagonist"));
    assertEquals(10, runOpened.get("plannedTrials").asInt());
  }

  @Test
  void unRoundConPiuScambiProduceUnEventoPerScambioENonUnoPerRound() {
    List<String> lines = composer.compose(completeChronicle());

    long exchangeCount = discriminantsOf(lines).stream().filter("EXCHANGE"::equals).count();

    assertEquals(3, exchangeCount, "2 scambi nel round di battaglia + 1 turno di duello");
  }

  @Test
  void loScambioDiBattagliaPortaRoundEIndiciDiRosterQuelloDiDuelloNo() throws JsonProcessingException {
    List<String> lines = composer.compose(completeChronicle());

    JsonNode battleExchange = parser.readTree(lines.get(2));
    assertTrue(battleExchange.get("round").isInt());
    assertTrue(battleExchange.get("attackerIndex").isInt());
    assertTrue(battleExchange.get("targetIndex").isInt());
    assertTrue(battleExchange.has("telemetry"));

    JsonNode duelExchange = parser.readTree(lines.get(6));
    assertTrue(duelExchange.get("round").isNull());
    assertTrue(duelExchange.get("attackerIndex").isNull());
    assertTrue(duelExchange.get("targetIndex").isNull());
  }

  @Test
  void unaCorsaChiusaAllaPrimaProvaProduceUnLogValidoECompleto() throws JsonProcessingException {
    ArenaChronicle chronicle = oneTrialFallenChronicle();

    List<String> lines = composer.compose(chronicle);

    assertEquals(List.of("RUN_OPENED", "TRIAL_STARTED", "EXCHANGE", "TRIAL_ENDED", "RUN_CLOSED"),
        discriminantsOf(lines));
    for (String line : lines) {
      assertTrue(isValidJson(line));
    }

    JsonNode trialEnded = parser.readTree(lines.get(3));
    assertEquals("FELL", trialEnded.get("outcome").asText());
    assertTrue(trialEnded.get("progress").isNull());

    JsonNode runClosed = parser.readTree(lines.get(4));
    assertEquals("FELL", runClosed.get("outcome").asText());
    assertEquals(1, runClosed.get("lastTrial").asInt());
  }

  private List<String> discriminantsOf(List<String> lines) {
    return lines.stream()
        .map(this::readEventField)
        .collect(Collectors.toList());
  }

  private String readEventField(String line) {
    try {
      return parser.readTree(line).get("event").asText();
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("riga non leggibile come JSON: " + line, e);
    }
  }

  private boolean isValidJson(String line) {
    try {
      parser.readTree(line);
      return true;
    } catch (JsonProcessingException e) {
      return false;
    }
  }

  private ArenaChronicle completeChronicle() {
    HeroSnapshot protagonist = heroSnapshot();
    TrialChronicle battleTrial = battleTrial(protagonist);
    TrialChronicle duelTrial = duelTrial();
    RunConclusion conclusion = new RunConclusion(RoundOutcome.WON, 2);

    return new ArenaChronicle(CombatSettings.defaults(), protagonist, 10, List.of(battleTrial, duelTrial), conclusion);
  }

  private ArenaChronicle oneTrialFallenChronicle() {
    HeroSnapshot protagonist = heroSnapshot();
    CombatantSnapshot heroCombatant = combatantSnapshot(protagonist, 0, 0, "Protagonista");
    CombatantSnapshot rivalCombatant = combatantSnapshot(protagonist, 1, 1, "Rivale");

    FighterVitals heroVitals = new FighterVitals("Protagonista", 0, 40, 20, 20);
    FighterVitals rivalVitals = new FighterVitals("Rivale", 12, 40, 20, 20);
    List<FighterVitals> vitals = List.of(heroVitals, rivalVitals);

    TurnLogEntry turnLog = hitTurnLog(1);
    EngagementTurn engagementTurn = new EngagementTurn(0, 1, 0, List.of(0, 1), turnLog);
    RoundLogEntry round = new RoundLogEntry(1, List.of(engagementTurn), vitals, List.of());

    TrialChronicle trial = new TrialChronicle(1, "il primo avversario", TrialShape.BATTLE,
        List.of(heroCombatant, rivalCombatant), new ChallengerBudgetChronicle(31, 4, 27), List.of(round), List.of(),
        vitals, RoundOutcome.FELL, null);

    return new ArenaChronicle(CombatSettings.defaults(), protagonist, 10, List.of(trial),
        new RunConclusion(RoundOutcome.FELL, 1));
  }

  private HeroSnapshot heroSnapshot() {
    List<CharacterCharacteristic> characteristics = List.of(new CharacterCharacteristic(Characteristic.STRENGTH, 12));
    List<CharacterCharacteristic> effectiveCharacteristics =
        List.of(new CharacterCharacteristic(Characteristic.STRENGTH, 15));
    ItemSnapshot weapon = new ItemSnapshot(ItemKind.WEAPON, "SWORD", Rarity.RARE, 9,
        List.of(new CharacteristicBonus(Characteristic.STRENGTH, 3)));
    List<ItemSnapshot> armourPieces = List.of(new ItemSnapshot(ItemKind.ARMOUR, "CHESTPLATE", Rarity.RARE, 6, List.of()));
    List<ItemSnapshot> jewels = List.of(new ItemSnapshot(ItemKind.JEWEL, "RING", Rarity.EPIC, null, List.of()));

    return new HeroSnapshot("Protagonista", Race.HUMAN, CharacterClass.WARRIOR, characteristics,
        effectiveCharacteristics, weapon, armourPieces, jewels);
  }

  private CombatantSnapshot combatantSnapshot(HeroSnapshot protagonist, int rosterIndex, int teamIndex, String name) {
    return new CombatantSnapshot(rosterIndex, teamIndex, name, Race.HUMAN, CharacterClass.WARRIOR,
        protagonist.characteristics(), protagonist.weapon(), protagonist.armourPieces(), 40, 20, 12.5, 8.5);
  }

  private TrialChronicle battleTrial(HeroSnapshot protagonist) {
    CombatantSnapshot heroCombatant = combatantSnapshot(protagonist, 0, 0, "Protagonista");
    CombatantSnapshot rivalCombatant = combatantSnapshot(protagonist, 1, 1, "Rivale");

    FighterVitals heroVitals = new FighterVitals("Protagonista", 40, 40, 20, 20);
    FighterVitals rivalVitals = new FighterVitals("Rivale", 23, 40, 20, 20);
    List<FighterVitals> vitals = List.of(heroVitals, rivalVitals);

    EngagementTurn firstExchange = new EngagementTurn(0, 0, 1, List.of(0, 1), hitTurnLog(1));
    EngagementTurn secondExchange = new EngagementTurn(0, 1, 0, List.of(0, 1), missTurnLog(1));
    RoundLogEntry round = new RoundLogEntry(1, List.of(firstExchange, secondExchange), vitals, List.of());

    List<FighterVitals> finalVitals = List.of(new FighterVitals("Protagonista", 40, 40, 20, 20),
        new FighterVitals("Rivale", 0, 40, 20, 20));

    ProgressChronicle progress = progressChronicle(protagonist);
    ChallengerBudgetChronicle budget = new ChallengerBudgetChronicle(31, 4, 27);

    return new TrialChronicle(1, "il primo avversario", TrialShape.BATTLE, List.of(heroCombatant, rivalCombatant),
        budget, List.of(round), List.of(), finalVitals, RoundOutcome.WON, progress);
  }

  private TrialChronicle duelTrial() {
    List<FighterVitals> finalVitals =
        List.of(new FighterVitals("Protagonista", 30, 40, 10, 20), new FighterVitals("Specchio", 0, 40, 8, 20));

    return new TrialChronicle(2, "lo sfidante speculare", TrialShape.DUEL, List.of(), null, List.of(),
        List.of(hitTurnLog(1)), finalVitals, RoundOutcome.WON, null);
  }

  private ProgressChronicle progressChronicle(HeroSnapshot heroAfter) {
    ItemSnapshot found = new ItemSnapshot(ItemKind.ARMOUR, "CHESTPLATE", Rarity.EPIC, 11, List.of());
    ItemSnapshot dropped = new ItemSnapshot(ItemKind.ARMOUR, "CHESTPLATE", Rarity.RARE, 6, List.of());
    List<CharacteristicGain> gains = List.of(new CharacteristicGain(Characteristic.STRENGTH, 3));

    return new ProgressChronicle(found, LootFate.ARMOUR_REPLACED, dropped, gains, heroAfter);
  }

  private TurnLogEntry hitTurnLog(int turnNumber) {
    ActionOutcome action = new ActionOutcome(ActionOutcome.Kind.HIT, 4, 0, 0, false, false);
    return new TurnLogEntry(turnNumber, "Il protagonista colpisce il rivale").withAction(action)
        .withTelemetry(fullTelemetry());
  }

  private TurnLogEntry missTurnLog(int turnNumber) {
    ActionOutcome action = new ActionOutcome(ActionOutcome.Kind.MISS, 0, 0, 0, false, false);
    return new TurnLogEntry(turnNumber, "Il rivale manca il protagonista").withAction(action)
        .withTelemetry(missTelemetry());
  }

  private TurnTelemetry fullTelemetry() {
    HitTelemetry hit = new HitTelemetry(14, 20, false, 0.79, 12, 10, 0.08, 3);
    DefenseTelemetry defenseRoll = new DefenseTelemetry(11, 20, 0.06, 0.41, true);
    DefenseAttemptTelemetry defenseAttempt =
        new DefenseAttemptTelemetry(defenseRoll, 4, DefenseAttemptTelemetry.Fallback.NONE);
    DamageTelemetry damage = new DamageTelemetry(41.5, 58.0, 1.02, 1.0, 1.0, 1.0, 1.0, 1.0, 42.3, 58.0, 13.3, 62, 100,
        13.6, 13.6, 13.6, 9.6, 4);
    return new TurnTelemetry(null, null, hit, defenseAttempt, damage);
  }

  private TurnTelemetry missTelemetry() {
    HitTelemetry hit = new HitTelemetry(3, 20, false, 0.79, 10, 12, 0.05, 1);
    return new TurnTelemetry(null, null, hit, null, null);
  }
}
