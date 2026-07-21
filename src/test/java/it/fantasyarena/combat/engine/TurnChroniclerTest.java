package it.fantasyarena.combat.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import it.fantasyarena.combat.engine.DefenseOutcome.DefenseResult;
import it.fantasyarena.combat.result.TurnHighlight;

/**
 * DoD 5 della SPEC cronaca-duello: senza highlight il testo del {@link TurnChronicler} coincide
 * esattamente con quello "piatto" di sempre (nessuna regressione sui colpi normali); con
 * highlight applica la precedenza di wording (il 20 naturale assorbe il critico ordinario, il
 * colpo pesante e' solo un rafforzativo, il colpo di grazia e' la chiusa dominante), restando
 * conciso. DoD 7 della SPEC colpo-potente (riveduta): il colpo potente e' un qualificatore
 * dell'attacco che si manifesta SOLO nel prefisso, qualunque sia l'esito (mancato, schivato,
 * parato o a segno); la coda dell'esito resta invariata e non ripete "potente".
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
    String description = chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 5, true,
        List.of(TurnHighlight.CRITICAL), "Difensore");

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
    String description = chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 30, true,
        List.of(TurnHighlight.HEAVY_BLOW), "Difensore");

    assertEquals(", colpo a segno (30 danni devastanti).", description);
  }

  @Test
  void colpoDiGrazia_eLaChiusaDominante() {
    String description = chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 5, true,
        List.of(TurnHighlight.KNOCKOUT), "Difensore");

    assertEquals(", colpo a segno (5 danni) e Difensore crolla a terra!", description);
  }

  @Test
  void colpoDiGrazia_siApplicaAQualunqueEsitoDiDifesa() {
    String description = chronicler.describeOutcome(DefenseResult.PARRIED, 5, true,
        List.of(TurnHighlight.KNOCKOUT), "Difensore");

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

  @Test
  void describeAttackPrefix_conESenzaColpoPotente() {
    assertEquals("Attaccante attacca Difensore con Spada",
        chronicler.describeAttackPrefix("Attaccante", "Difensore", "Spada", false));
    assertEquals("Attaccante tenta un colpo potente su Difensore con Spada",
        chronicler.describeAttackPrefix("Attaccante", "Difensore", "Spada", true));
  }

  @Test
  void colpoPotente_aSegno_codaIdenticaAUnColpoNormale() {
    String prefix = chronicler.describeAttackPrefix("Attaccante", "Difensore", "Spada", true);
    String description = prefix + chronicler.describeOutcome(DefenseResult.HIT_TAKEN, 5, true, List.of(), "Difensore");

    assertEquals("Attaccante tenta un colpo potente su Difensore con Spada, colpo a segno (5 danni).", description);
  }

  /**
   * DoD del FIX: un colpo potente SCHIVATO o PARATO ora cita "colpo potente" nel prefisso, cosa
   * che prima non accadeva perche' il qualificatore viveva solo nella coda del colpo a segno.
   */
  @Test
  void colpoPotente_schivatoOParato_citaIlColpoPotenteNelPrefisso() {
    String prefix = chronicler.describeAttackPrefix("Attaccante", "Difensore", "Spada", true);

    String dodged = prefix + chronicler.describeOutcome(DefenseResult.DODGED, 0, true, List.of(), "Difensore");
    assertEquals("Attaccante tenta un colpo potente su Difensore con Spada, schivato.", dodged);
    assertTrue(dodged.contains("tenta un colpo potente"));

    String parried = prefix + chronicler.describeOutcome(DefenseResult.PARRIED, 5, true, List.of(), "Difensore");
    assertEquals("Attaccante tenta un colpo potente su Difensore con Spada, parato (5 danni).", parried);
    assertTrue(parried.contains("tenta un colpo potente"));
  }

  @Test
  void colpoPotenteMancato_frasededicataConNomeAttaccanteEDifensore() {
    assertEquals("Attaccante tenta un colpo potente su Difensore con Spada ma manca il colpo.",
        chronicler.describeMiss("Attaccante", "Difensore", "Spada", true));
  }

  @Test
  void mancatoSenzaColpoPotente_includeOraIlNomeDellArma() {
    assertEquals("Attaccante attacca Difensore con Spada ma manca il colpo.",
        chronicler.describeMiss("Attaccante", "Difensore", "Spada", false));
  }
}
