package it.fantasyarena.combat.chronicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.RoundOutcome;
import it.fantasytoolkitcore.core.model.CharacterClass;
import it.fantasytoolkitcore.core.model.Race;

/**
 * L'invariante fra prove previste e prove giocate: {@link ArenaChronicle} deve poter valere sempre
 * la lunghezza intera del percorso, anche a corsa chiusa in anticipo, ma non può mai dichiarare
 * meno prove previste di quante ne abbia effettivamente registrate.
 */
class ArenaChronicleTest {

  @Test
  void laLunghezzaPrevistaPuoEccedereLeProveGiocate() {
    ArenaChronicle chronicle = new ArenaChronicle(protagonist(), 10, List.of(), new RunConclusion(RoundOutcome.FELL, 1));

    assertEquals(10, chronicle.plannedTrials());
    assertEquals(0, chronicle.trials().size());
  }

  @Test
  void rifiutaUnaLunghezzaPrevistaMinoreDelleProveGiaGiocate() {
    List<TrialChronicle> trials = List.of(trial(1), trial(2), trial(3));

    assertThrows(IllegalArgumentException.class,
        () -> new ArenaChronicle(protagonist(), 2, trials, new RunConclusion(RoundOutcome.WON, 3)));
  }

  private HeroSnapshot protagonist() {
    return new HeroSnapshot("Protagonista", Race.HUMAN, CharacterClass.WARRIOR, List.of(), null, List.of(),
        List.of());
  }

  private TrialChronicle trial(int number) {
    return new TrialChronicle(number, "prova " + number, TrialShape.DUEL, List.of(), List.of(), List.of(),
        RoundOutcome.WON, null);
  }
}
