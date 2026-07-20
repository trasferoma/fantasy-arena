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
 * conciso. DoD 7 della SPEC colpo-potente: il colpo potente e' un qualificatore componibile con
 * perfetto/critico sul colpo a segno, e ha un wording dedicato sul mancato; senza colpo potente
 * il testo resta identico a oggi.
 */
class TurnChroniclerTest {

  private final TurnChronicler chronicler = new TurnChronicler();

  @Test
  void senzaHighlight_testoIdenticoAQuelloDiSempre() {
    assertEquals(", schivato.",
        chronicler.describeOutcome(DefenseResult.DODGED, 0, true, List.of(), "Difensore", false));
    assertEquals(", parato (5 danni).",
        chronicler.describeOutcome(DefenseResult.PARRIED, 5, true, List.of(), "Difensore", false));
    assertEquals(", colpo a segno (5 danni).",
        chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 5, true, List.of(), "Difensore", false));
    assertEquals(", colpo a segno (difensore esausto, 5 danni).",
        chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 5, false, List.of(), "Difensore", false));
  }

  @Test
  void critico_aggiungeEnfasiSenzaAssumereIl20Naturale() {
    String description = chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 5, true,
        List.of(TurnHighlight.CRITICAL), "Difensore", false);

    assertEquals(", colpo critico a segno (5 danni).", description);
  }

  @Test
  void ventiNaturale_assorbeIlWordingDelCriticoOrdinario() {
    List<TurnHighlight> highlights = List.of(TurnHighlight.PERFECT_HIT, TurnHighlight.CRITICAL);
    String description = chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 5, true, highlights, "Difensore", false);

    assertEquals(", colpo perfetto (20 naturale) a segno (5 danni).", description);
    assertFalse(description.contains("critico"), "il 20 naturale assorbe il wording del critico ordinario");
  }

  @Test
  void colpoPesante_eSoloUnRafforzativoNonUnaFraseASe() {
    String description = chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 30, true,
        List.of(TurnHighlight.HEAVY_BLOW), "Difensore", false);

    assertEquals(", colpo a segno (30 danni devastanti).", description);
  }

  @Test
  void colpoDiGrazia_eLaChiusaDominante() {
    String description = chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 5, true,
        List.of(TurnHighlight.KNOCKOUT), "Difensore", false);

    assertEquals(", colpo a segno (5 danni) e Difensore crolla a terra!", description);
  }

  @Test
  void colpoDiGrazia_siApplicaAQualunqueEsitoDiDifesa() {
    String description = chronicler.describeOutcome(DefenseResult.PARRIED, 5, true,
        List.of(TurnHighlight.KNOCKOUT), "Difensore", false);

    assertEquals(", parato (5 danni) e Difensore crolla a terra!", description);
  }

  @Test
  void precedenzaCombinata_perfettoPesanteEKnockoutInsieme() {
    List<TurnHighlight> highlights =
        List.of(TurnHighlight.PERFECT_HIT, TurnHighlight.CRITICAL, TurnHighlight.HEAVY_BLOW, TurnHighlight.KNOCKOUT);
    String description = chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 40, true, highlights, "Difensore", false);

    assertEquals(", colpo perfetto (20 naturale) a segno (40 danni devastanti) e Difensore crolla a terra!",
        description);
  }

  @Test
  void colpoPotente_eQualificatoreComponibileConPerfettoECritico() {
    String plain = chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 5, true, List.of(), "Difensore", true);
    assertEquals(", colpo potente a segno (5 danni).", plain);

    String withCritical = chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 5, true,
        List.of(TurnHighlight.CRITICAL), "Difensore", true);
    assertEquals(", colpo potente critico a segno (5 danni).", withCritical);

    List<TurnHighlight> perfectAndCritical = List.of(TurnHighlight.PERFECT_HIT, TurnHighlight.CRITICAL);
    String withPerfect =
        chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 5, true, perfectAndCritical, "Difensore", true);
    assertEquals(", colpo potente perfetto (20 naturale) a segno (5 danni).", withPerfect);
  }

  @Test
  void colpoPotente_nonScelto_testoIdenticoAOggi() {
    String description = chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 5, true, List.of(), "Difensore", false);

    assertEquals(", colpo a segno (5 danni).", description);
  }

  @Test
  void colpoPotenteMancato_frasededicataConNomeAttaccanteEDifensore() {
    assertEquals("Attaccante tenta un colpo potente su Difensore ma manca il colpo.",
        chronicler.describeMiss("Attaccante", "Difensore", true));
  }

  @Test
  void mancatoSenzaColpoPotente_testoIdenticoAOggi() {
    assertEquals("Attaccante attacca Difensore ma manca il colpo.",
        chronicler.describeMiss("Attaccante", "Difensore", false));
  }
}
