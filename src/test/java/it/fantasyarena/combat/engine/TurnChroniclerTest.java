package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.engine.DefenseOutcome.DefenseResult;
import it.fantasyarena.combat.result.TurnHighlight;

/**
 * DoD 5 della SPEC cronaca-duello: senza highlight il testo del {@link TurnChronicler} coincide
 * esattamente con quello "piatto" di sempre (nessuna regressione sui colpi normali); con
 * highlight applica la precedenza di wording (il 20 naturale assorbe il critico ordinario, il
 * colpo pesante e' solo un rafforzativo, il colpo di grazia e' la chiusa dominante), restando
 * conciso.
 */
class TurnChroniclerTest {

  private final TurnChronicler chronicler = new TurnChronicler();

  @Test
  void senzaHighlight_testoIdenticoAQuelloDiSempre() {
    assertEquals(", schivato.",
        chronicler.describeOutcome(DefenseResult.DODGED, 0, true, List.of(), "Difensore"));
    assertEquals(", parato (5 danni).",
        chronicler.describeOutcome(DefenseResult.PARRIED, 5, true, List.of(), "Difensore"));
    assertEquals(", colpo a segno (5 danni).",
        chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 5, true, List.of(), "Difensore"));
    assertEquals(", colpo a segno (difensore esausto, 5 danni).",
        chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 5, false, List.of(), "Difensore"));
  }

  @Test
  void critico_aggiungeEnfasiSenzaAssumereIl20Naturale() {
    String description =
        chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 5, true, List.of(TurnHighlight.CRITICAL), "Difensore");

    assertEquals(", colpo critico a segno (5 danni).", description);
  }

  @Test
  void ventiNaturale_assorbeIlWordingDelCriticoOrdinario() {
    List<TurnHighlight> highlights = List.of(TurnHighlight.PERFECT_HIT, TurnHighlight.CRITICAL);
    String description = chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 5, true, highlights, "Difensore");

    assertEquals(", colpo perfetto (20 naturale) a segno (5 danni).", description);
    assertFalse(description.contains("critico"), "il 20 naturale assorbe il wording del critico ordinario");
  }

  @Test
  void colpoPesante_eSoloUnRafforzativoNonUnaFraseASe() {
    String description =
        chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 30, true, List.of(TurnHighlight.HEAVY_BLOW), "Difensore");

    assertEquals(", colpo a segno (30 danni devastanti).", description);
  }

  @Test
  void colpoDiGrazia_eLaChiusaDominante() {
    String description =
        chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 5, true, List.of(TurnHighlight.KNOCKOUT), "Difensore");

    assertEquals(", colpo a segno (5 danni) e Difensore crolla a terra!", description);
  }

  @Test
  void colpoDiGrazia_siApplicaAQualunqueEsitoDiDifesa() {
    String description =
        chronicler.describeOutcome(DefenseResult.PARRIED, 5, true, List.of(TurnHighlight.KNOCKOUT), "Difensore");

    assertEquals(", parato (5 danni) e Difensore crolla a terra!", description);
  }

  @Test
  void precedenzaCombinata_perfettoPesanteEKnockoutInsieme() {
    List<TurnHighlight> highlights =
        List.of(TurnHighlight.PERFECT_HIT, TurnHighlight.CRITICAL, TurnHighlight.HEAVY_BLOW, TurnHighlight.KNOCKOUT);
    String description = chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 40, true, highlights, "Difensore");

    assertEquals(", colpo perfetto (20 naturale) a segno (40 danni devastanti) e Difensore crolla a terra!",
        description);
  }
}
