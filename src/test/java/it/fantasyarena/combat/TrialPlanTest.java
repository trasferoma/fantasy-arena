package it.fantasyarena.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.chronicle.TrialShape;

/**
 * Il percorso dell'arena come dato, in un punto solo: dieci stazioni fisse, verificate senza
 * giocare nessuno scontro. Quello che conta qui è la tabella, non la scansione — che è compito di
 * {@code ArenaTest}.
 */
class TrialPlanTest {

  private final TrialPlan plan = TrialPlan.standard();

  @Test
  void ilPercorsoHaDieciStazioniNumerateDaUnoADieci() {
    List<TrialStation> stations = plan.stations();

    assertEquals(10, plan.length());
    assertEquals(10, stations.size());
    for (int index = 0; index < stations.size(); index++) {
      assertEquals(index + 1, stations.get(index).number());
    }
  }

  @Test
  void ilNumeroDiSfidantiSeguePrimaUnoPoiDuePoiTrePoiLoSpecchio() {
    List<Integer> challengerCounts = plan.stations().stream().map(TrialStation::challengerCount).toList();

    assertEquals(List.of(1, 1, 1, 2, 2, 2, 3, 3, 3, 1), challengerCounts);
  }

  @Test
  void laFormaSiDerivaDalNumeroDiSfidanti() {
    List<TrialShape> shapes = plan.stations().stream().map(TrialStation::shape).toList();

    assertEquals(List.of(TrialShape.DUEL, TrialShape.DUEL, TrialShape.DUEL, TrialShape.BATTLE, TrialShape.BATTLE,
        TrialShape.BATTLE, TrialShape.BATTLE, TrialShape.BATTLE, TrialShape.BATTLE, TrialShape.DUEL), shapes);
  }

  @Test
  void loSpecchioCompareSoloAllaDecimaStazione() {
    List<TrialStation> stations = plan.stations();

    for (int index = 0; index < stations.size() - 1; index++) {
      assertEquals(ChallengerOrigin.GENERATED, stations.get(index).challengerOrigin(),
          "stazione " + stations.get(index).number() + ": lo specchio deve comparire solo all'ultima");
    }
    assertEquals(ChallengerOrigin.MIRROR, stations.getLast().challengerOrigin());
  }

  @Test
  void ilMonteDiSquadraCresceSecondoLaCurvaDichiarataESoloPerLeStazioniGenerate() {
    List<Integer> characteristicPoints = plan.stations().stream()
        .limit(9)
        .map(TrialStation::characteristicPoints)
        .toList();

    assertEquals(List.of(15, 18, 21, 31, 35, 39, 50, 54, 59), characteristicPoints);
    assertNull(plan.stations().getLast().characteristicPoints(), "lo specchio non dichiara un monte punti proprio");
  }

  @Test
  void leDieciDescrizioniSonoPresentiNonVuoteETutteDiverse() {
    Set<String> descriptions = plan.stations().stream()
        .map(TrialStation::description)
        .collect(Collectors.toCollection(HashSet::new));

    assertEquals(10, descriptions.size(), "le dieci descrizioni devono essere tutte diverse");
    assertFalse(descriptions.stream().anyMatch(String::isBlank), "nessuna descrizione deve essere vuota");
  }

  @Test
  void unaStazioneGenerataRifiutaUnMontePuntiNullo() {
    assertThrows(IllegalArgumentException.class,
        () -> new TrialStation(1, "test", 1, ChallengerOrigin.GENERATED, null));
  }

  @Test
  void unaStazioneASpecchioRifiutaUnMontePuntiDichiarato() {
    assertThrows(IllegalArgumentException.class,
        () -> new TrialStation(10, "test", 1, ChallengerOrigin.MIRROR, 15));
  }
}
