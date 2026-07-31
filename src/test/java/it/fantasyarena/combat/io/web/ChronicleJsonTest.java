package it.fantasyarena.combat.io.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import it.fantasyarena.combat.RoundOutcome;
import it.fantasyarena.combat.chronicle.ArenaChronicle;
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
import it.fantasycombatsystem.result.ActionOutcome;
import it.fantasycombatsystem.result.FighterVitals;
import it.fantasycombatsystem.result.InitiativeBreakdown;
import it.fantasycombatsystem.result.InitiativeOverride;
import it.fantasycombatsystem.result.InitiativeReport;
import it.fantasycombatsystem.result.StaminaChange;
import it.fantasycombatsystem.result.TurnHighlight;
import it.fantasycombatsystem.result.TurnLogEntry;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkitcore.core.model.CharacterClass;
import it.fantasytoolkitcore.core.model.Characteristic;
import it.fantasytoolkitcore.core.model.Race;
import it.fantasytoolkitcore.core.model.Rarity;

/**
 * Verifica {@link ChronicleJson} su una cronaca costruita a mano, mai giocando una partita: il
 * motore è {@code SNAPSHOT} e i suoi tipi di log affiorano nel JSON così come sono, quindi un campo
 * rinominato là deve far scattare questo test prima che il browser se ne accorga.
 */
class ChronicleJsonTest {

  private final ChronicleJson chronicleJson = new ChronicleJson();
  private final ObjectMapper parser = new ObjectMapper();

  @Test
  void ilJsonContieneLeChiaviCheIlFrontendLeggeAOgniLivelloDiAnnidamento() throws JsonProcessingException {
    ArenaChronicle chronicle = completeChronicle();

    JsonNode root = parser.readTree(chronicleJson.toJson(chronicle));

    assertTrue(root.has("plannedTrials"), "il denominatore dell'intestazione non deve indovinare la lunghezza");
    assertProtagonistKeys(root.get("protagonist"));
    assertBattleTrialKeys(root.get("trials").get(0));
    assertDuelTrialKeys(root.get("trials").get(1));
    assertProgressKeys(root.get("trials").get(0).get("progress"));
    assertConclusionKeys(root.get("conclusion"));
  }

  /**
   * {@code RunConclusion.triumph()} non è un componente del record ma un accessor derivato (vedi il
   * suo Javadoc), e il suo nome non segue la convenzione {@code get}/{@code is} che Jackson usa per
   * riconoscere un getter senza annotazioni: verificato qui che non compare nel JSON. Il frontend
   * deriva il trionfo da {@code outcome}, come da decisione della SPEC.
   */
  @Test
  void triumphNonCompareNelJsonNonEssendoUnGetterRiconosciutoDaJackson() throws JsonProcessingException {
    ArenaChronicle chronicle = completeChronicle();

    JsonNode conclusion = parser.readTree(chronicleJson.toJson(chronicle)).get("conclusion");

    assertFalse(conclusion.has("triumph"));
  }

  /**
   * Nessun {@code Fighter}, nessun {@code Team}, nessun {@code Optional} e nessuno stato mutabile
   * del motore deve comparire nel JSON. Invece di una lista di stringhe letterali da cercare nel
   * testo — che nessuno aggiornerebbe se il motore cambiasse forma altrove — si raccolgono tutti i
   * nomi di campo effettivamente presenti nell'albero JSON e si verifica che non contengano le
   * chiavi che solo quei tipi porterebbero: {@code state} e {@code shield} sono di {@code Fighter}
   * (rispettivamente lo stato mutabile e lo scudo opzionale), {@code members} è di {@code Team},
   * {@code winner} e {@code winningTeam} sono gli esiti opzionali di {@code CombatResult} e
   * {@code BattleResult}. Nessuna di queste chiavi ha un motivo di comparire nella cronaca: la
   * fotografia dei combattenti ({@link CombatantSnapshot}) non referenzia il {@code Fighter}, e i
   * passi della cronaca sono le sole liste di log ({@code RoundLogEntry}/{@code TurnLogEntry}), non
   * gli oggetti risultato del motore che le contengono insieme al vincitore opzionale.
   */
  @Test
  void nelJsonNonCompareNessunTipoMutabileDelMotore() throws JsonProcessingException {
    ArenaChronicle chronicle = completeChronicle();

    JsonNode root = parser.readTree(chronicleJson.toJson(chronicle));
    Set<String> fieldNames = new HashSet<>();
    collectFieldNames(root, fieldNames);

    Set<String> forbiddenFieldNames =
        Set.of("state", "shield", "isDefeated", "defeated", "members", "livingMembers", "eliminated", "winner",
            "winningTeam", "teams", "present", "empty");
    for (String forbidden : forbiddenFieldNames) {
      assertFalse(fieldNames.contains(forbidden), "chiave riservata al motore trovata nel JSON: " + forbidden);
    }
  }

  private void assertProtagonistKeys(JsonNode protagonist) {
    assertTrue(protagonist.has("name"));
    assertTrue(protagonist.has("race"));
    assertTrue(protagonist.has("characterClass"));
    assertTrue(protagonist.get("characteristics").get(0).has("characteristic"));
    assertTrue(protagonist.get("characteristics").get(0).has("value"));
    assertItemSnapshotKeys(protagonist.get("weapon"));
    assertItemSnapshotKeys(protagonist.get("armourPieces").get(0));
    assertItemSnapshotKeys(protagonist.get("jewels").get(0));
  }

  private void assertItemSnapshotKeys(JsonNode item) {
    assertTrue(item.has("kind"));
    assertTrue(item.has("name"));
    assertTrue(item.has("rarity"));
    assertTrue(item.has("power"));
  }

  private void assertBattleTrialKeys(JsonNode battleTrial) {
    assertEquals("BATTLE", battleTrial.get("shape").asText());
    assertTrue(battleTrial.has("number"));
    assertTrue(battleTrial.has("description"));
    assertEquals("WON", battleTrial.get("outcome").asText());

    JsonNode rosterEntry = battleTrial.get("roster").get(0);
    assertTrue(rosterEntry.has("rosterIndex"));
    assertTrue(rosterEntry.has("teamIndex"));
    assertTrue(rosterEntry.has("maxHealth"));
    assertTrue(rosterEntry.has("maxStamina"));

    JsonNode engagementTurn = battleTrial.get("rounds").get(0).get("turns").get(0);
    assertTrue(engagementTurn.has("attackerIndex"));
    assertTrue(engagementTurn.has("targetIndex"));
    assertTrue(engagementTurn.has("participantIndexes"));

    JsonNode turnLog = engagementTurn.get("turn");
    assertVitalsKeys(turnLog.get("vitals").get(0));
    assertTrue(turnLog.get("action").has("damage"));
    assertTrue(battleTrial.get("rounds").get(0).has("events"));
    assertVitalsKeys(battleTrial.get("finalVitals").get(0));
  }

  private void assertDuelTrialKeys(JsonNode duelTrial) {
    assertEquals("DUEL", duelTrial.get("shape").asText());
    assertTrue(duelTrial.get("rounds").isEmpty(), "il duello lascia vuoti i round");

    JsonNode turnLog = duelTrial.get("turns").get(0);
    assertTrue(turnLog.has("turnNumber"));
    assertTrue(turnLog.has("description"));
    assertVitalsKeys(turnLog.get("vitals").get(0));
    assertTrue(turnLog.get("action").has("critical"));
    assertVitalsKeys(duelTrial.get("finalVitals").get(0));
  }

  private void assertVitalsKeys(JsonNode vitals) {
    assertTrue(vitals.has("currentHealth"));
    assertTrue(vitals.has("maxHealth"));
    assertTrue(vitals.has("currentStamina"));
    assertTrue(vitals.has("maxStamina"));
  }

  private void assertProgressKeys(JsonNode progress) {
    assertItemSnapshotKeys(progress.get("found"));
    assertEquals("ARMOUR_REPLACED", progress.get("fate").asText());
    assertItemSnapshotKeys(progress.get("dropped"));
    assertTrue(progress.has("jewelBonusPoints"));
    assertTrue(progress.get("gains").get(0).has("characteristic"));
    assertTrue(progress.get("gains").get(0).has("points"));
    assertProtagonistKeys(progress.get("heroAfter"));
  }

  private void assertConclusionKeys(JsonNode conclusion) {
    assertEquals("WON", conclusion.get("outcome").asText());
    assertTrue(conclusion.has("lastTrial"));
  }

  private void collectFieldNames(JsonNode node, Set<String> fieldNames) {
    if (node.isObject()) {
      node.fieldNames().forEachRemaining(fieldNames::add);
    }
    node.forEach(child -> collectFieldNames(child, fieldNames));
  }

  private ArenaChronicle completeChronicle() {
    HeroSnapshot protagonist = heroSnapshot();
    TrialChronicle battleTrial = battleTrial(protagonist);
    TrialChronicle duelTrial = duelTrial();
    RunConclusion conclusion = new RunConclusion(RoundOutcome.WON, 2);

    return new ArenaChronicle(protagonist, 10, List.of(battleTrial, duelTrial), conclusion);
  }

  private HeroSnapshot heroSnapshot() {
    List<CharacterCharacteristic> characteristics = List.of(new CharacterCharacteristic(Characteristic.STRENGTH, 12));
    ItemSnapshot weapon = new ItemSnapshot(ItemKind.WEAPON, "SWORD", Rarity.RARE, 9);
    List<ItemSnapshot> armourPieces = List.of(new ItemSnapshot(ItemKind.ARMOUR, "CHESTPLATE", Rarity.RARE, 6));
    List<ItemSnapshot> jewels = List.of(new ItemSnapshot(ItemKind.JEWEL, "RING", Rarity.EPIC, null));

    return new HeroSnapshot("Protagonista", Race.HUMAN, CharacterClass.WARRIOR, characteristics, weapon,
        armourPieces, jewels);
  }

  private TrialChronicle battleTrial(HeroSnapshot protagonist) {
    CombatantSnapshot heroSnapshot = new CombatantSnapshot(0, 0, "Protagonista", Race.HUMAN, CharacterClass.WARRIOR,
        protagonist.characteristics(), protagonist.weapon(), protagonist.armourPieces(), 40, 20, 12.5, 8.5);
    CombatantSnapshot rivalSnapshot = new CombatantSnapshot(1, 1, "Rivale", Race.ORC, CharacterClass.WARRIOR,
        protagonist.characteristics(), protagonist.weapon(), protagonist.armourPieces(), 40, 20, 10.0, 9.0);

    FighterVitals heroVitals = new FighterVitals("Protagonista", 40, 40, 20, 20);
    FighterVitals rivalVitals = new FighterVitals("Rivale", 23, 40, 20, 20);
    List<FighterVitals> vitals = List.of(heroVitals, rivalVitals);

    ActionOutcome action = new ActionOutcome(ActionOutcome.Kind.HIT, 17, 0, false, false);
    TurnLogEntry turnLog = new TurnLogEntry(1, "Il protagonista colpisce il rivale").withVitals(vitals)
        .withAction(action);
    EngagementTurn engagementTurn = new EngagementTurn(0, 0, 1, List.of(0, 1), turnLog);
    RoundLogEntry round = new RoundLogEntry(1, List.of(engagementTurn), vitals, List.of("il round è iniziato"));

    ProgressChronicle progress = progressChronicle(protagonist);

    List<FighterVitals> finalVitals = List.of(new FighterVitals("Protagonista", 40, 40, 20, 20),
        new FighterVitals("Rivale", 0, 40, 20, 20));

    return new TrialChronicle(1, "il primo avversario", TrialShape.BATTLE, List.of(heroSnapshot, rivalSnapshot),
        List.of(round), List.of(), finalVitals, RoundOutcome.WON, progress);
  }

  private TrialChronicle duelTrial() {
    FighterVitals heroVitals = new FighterVitals("Protagonista", 30, 40, 10, 20);
    FighterVitals mirrorVitals = new FighterVitals("Specchio", 0, 40, 8, 20);
    List<FighterVitals> vitals = List.of(heroVitals, mirrorVitals);

    InitiativeBreakdown breakdown =
        new InitiativeBreakdown("Protagonista", 1.0, 2.0, 3.0, 0.5, 6.5, 10, 20, 12, 14, 15);
    InitiativeReport initiative =
        new InitiativeReport(List.of(breakdown), "Protagonista", "Protagonista", InitiativeOverride.NONE);
    StaminaChange staminaChange = new StaminaChange("Protagonista", 2, 0);
    ActionOutcome action = new ActionOutcome(ActionOutcome.Kind.HIT, 30, 0, true, false);

    TurnLogEntry turnLog = new TurnLogEntry(1, "Il protagonista abbatte lo specchio").withVitals(vitals)
        .withInitiative(initiative)
        .withStaminaChanges(List.of(staminaChange))
        .withHighlights(List.of(TurnHighlight.CRITICAL, TurnHighlight.KNOCKOUT))
        .withAction(action);

    return new TrialChronicle(3, "lo sfidante speculare", TrialShape.DUEL, List.of(), List.of(), List.of(turnLog),
        vitals, RoundOutcome.WON, null);
  }

  private ProgressChronicle progressChronicle(HeroSnapshot heroAfter) {
    ItemSnapshot found = new ItemSnapshot(ItemKind.ARMOUR, "CHESTPLATE", Rarity.EPIC, 11);
    ItemSnapshot dropped = new ItemSnapshot(ItemKind.ARMOUR, "CHESTPLATE", Rarity.RARE, 6);
    List<CharacteristicGain> gains = List.of(new CharacteristicGain(Characteristic.STRENGTH, 3));

    return new ProgressChronicle(found, LootFate.ARMOUR_REPLACED, dropped, null, gains, heroAfter);
  }
}
