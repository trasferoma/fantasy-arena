package it.fantasyarena.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.RarityTable;

/**
 * Le quattro fasce dell'equipaggiamento degli sfidanti generati: confini, numero di pezzi, rarità
 * estraibili, e la funzione pura con cui l'arma dello specchio si innalza di un grado.
 */
class ChallengerEquipmentTest {

  private static final int RARITY_SAMPLE_SIZE = 500;

  @Test
  void leQuattroFasceCambianoAiConfiniDichiarati() {
    assertSame(ChallengerEquipment.forTrial(1), ChallengerEquipment.forTrial(2),
        "le prove 1 e 2 condividono la fascia d'apertura");
    assertSame(ChallengerEquipment.forTrial(3), ChallengerEquipment.forTrial(5),
        "le prove 3-5 condividono la stessa fascia");
    assertSame(ChallengerEquipment.forTrial(6), ChallengerEquipment.forTrial(8),
        "le prove 6-8 condividono la stessa fascia");
    assertSame(ChallengerEquipment.forTrial(9), ChallengerEquipment.forTrial(10),
        "le prove 9-10 condividono la stessa fascia");

    assertNotSame(ChallengerEquipment.forTrial(2), ChallengerEquipment.forTrial(3),
        "la seconda fascia comincia alla terza prova, non più alla seconda");
    assertNotSame(ChallengerEquipment.forTrial(5), ChallengerEquipment.forTrial(6),
        "la terza fascia comincia alla sesta prova");
    assertNotSame(ChallengerEquipment.forTrial(8), ChallengerEquipment.forTrial(9),
        "la quarta fascia comincia alla nona prova");
  }

  @Test
  void oltreLaDecimaProvaSiApplicaLUltimaFascia() {
    assertSame(ChallengerEquipment.forTrial(10), ChallengerEquipment.forTrial(11),
        "un percorso più lungo di dieci stazioni non deve restare senza fascia");
    assertSame(ChallengerEquipment.forTrial(10), ChallengerEquipment.forTrial(50));
  }

  @Test
  void ilNumeroDiPezziDArmaturaCresceConLaFascia() {
    assertEquals(1, ChallengerEquipment.forTrial(1).armourPieceCount());
    assertEquals(1, ChallengerEquipment.forTrial(3).armourPieceCount());
    assertEquals(1, ChallengerEquipment.forTrial(6).armourPieceCount());
    assertEquals(2, ChallengerEquipment.forTrial(9).armourPieceCount());
  }

  /**
   * Le prove 1-2 non devono diventare più dure di oggi: la fascia d'apertura pesca solo
   * {@code UNCOMMON} su entrambe le tabelle e veste un pezzo solo, esattamente come
   * l'equipaggiamento standard di prima di questo lavoro.
   */
  @Test
  void laPrimaFasciaRiproduceIdenticoLEquipaggiamentoDiOggi() {
    ChallengerEquipment opening = ChallengerEquipment.forTrial(1);

    assertEquals(1, opening.armourPieceCount());
    assertEquals(Set.of(Rarity.UNCOMMON), drawnRarities(opening.weaponRarityTable(), new Random(7)),
        "la tabella dell'arma della fascia d'apertura deve restare interamente UNCOMMON");
    assertEquals(Set.of(Rarity.UNCOMMON), drawnRarities(opening.armourRarityTable(), new Random(7)),
        "la tabella dell'armatura della fascia d'apertura deve restare interamente UNCOMMON");
  }

  @Test
  void laTabellaDellArmaEPiuGenerosaDiQuellaDellArmaturaDallaSecondaFasciaInPoi() {
    assertWeaponAheadOfArmour(3);
    assertWeaponAheadOfArmour(6);
    assertWeaponAheadOfArmour(9);
  }

  /**
   * Le due tabelle governano grandezze opposte del rating del motore — pericolosità l'arma,
   * resistenza l'armatura — e la tabella dell'arma resta sempre un passo avanti a quella
   * dell'armatura dalla seconda fascia in poi: verificato confrontando la rarità massima estraibile
   * da ciascuna.
   */
  private void assertWeaponAheadOfArmour(int trialNumber) {
    ChallengerEquipment equipment = ChallengerEquipment.forTrial(trialNumber);
    Rarity maxWeaponRarity = maxDrawnRarity(equipment.weaponRarityTable());
    Rarity maxArmourRarity = maxDrawnRarity(equipment.armourRarityTable());

    assertTrue(maxWeaponRarity.ordinal() >= maxArmourRarity.ordinal(),
        "alla prova " + trialNumber + " la tabella dell'arma non deve mai restare indietro rispetto "
            + "a quella dell'armatura");
  }

  private Rarity maxDrawnRarity(RarityTable rarityTable) {
    return drawnRarities(rarityTable, new Random(7)).stream()
        .max(Comparator.comparingInt(Rarity::ordinal))
        .orElseThrow();
  }

  @Test
  void laSecondaFasciaAggiungeRareAllArmaSenzaAbbandonareUncommon() {
    Set<Rarity> drawn = drawnRarities(ChallengerEquipment.forTrial(3).weaponRarityTable(), new Random(7));

    assertTrue(EnumSet.of(Rarity.UNCOMMON, Rarity.RARE).containsAll(drawn));
  }

  /**
   * I pesi delle quattro fasce sono il risultato di una taratura empirica e possono cambiare per
   * ritarare il percorso: quello che deve restare vero per costruzione, indipendentemente dai pesi
   * scelti in una particolare taratura, è che il percorso non torna mai indietro. Il confronto è a
   * coppie sulle quattro fasce, non quattro asserzioni slegate su una fascia alla volta.
   */
  @Test
  void laRaritaMassimaEstraibileNonDecresceFasciaDopoFascia() {
    List<ChallengerEquipment> fasce = List.of(
        ChallengerEquipment.forTrial(1),
        ChallengerEquipment.forTrial(3),
        ChallengerEquipment.forTrial(6),
        ChallengerEquipment.forTrial(9));

    for (int index = 1; index < fasce.size(); index++) {
      ChallengerEquipment previousTrial = fasce.get(index - 1);
      ChallengerEquipment currentTrial = fasce.get(index);

      assertTrue(maxDrawnRarity(currentTrial.weaponRarityTable()).ordinal()
              >= maxDrawnRarity(previousTrial.weaponRarityTable()).ordinal(),
          "la rarità massima estraibile sull'arma non deve mai scendere passando alla fascia successiva");
      assertTrue(maxDrawnRarity(currentTrial.armourRarityTable()).ordinal()
              >= maxDrawnRarity(previousTrial.armourRarityTable()).ordinal(),
          "la rarità massima estraibile sull'armatura non deve mai scendere passando alla fascia successiva");
    }
  }

  /**
   * Qualunque sia la taratura corrente dei pesi, l'ultima fascia deve continuare a rendere
   * possibile un'arma {@code EPIC}: è il grado che rende gli sfidanti pericolosi, e se anche la
   * taratura più prudente lo escludesse il lavoro di bilanciamento sarebbe vanificato.
   */
  @Test
  void laQuartaFasciaPuoAncoraPescareEpicSullArma() {
    Rarity maxWeaponRarity = maxDrawnRarity(ChallengerEquipment.forTrial(9).weaponRarityTable());

    assertTrue(maxWeaponRarity.ordinal() >= Rarity.EPIC.ordinal(),
        "l'ultima fascia deve poter pescare almeno EPIC sull'arma");
  }

  @Test
  void laQuartaFasciaRestaSuRareEUncommonSullArmatura() {
    Set<Rarity> drawn = drawnRarities(ChallengerEquipment.forTrial(9).armourRarityTable(), new Random(7));

    assertTrue(EnumSet.of(Rarity.UNCOMMON, Rarity.RARE).containsAll(drawn),
        "la tabella dell'armatura resta più prudente di quella dell'arma anche nell'ultima fascia");
  }

  @Test
  void oneGradeAboveInnalzaDiUnGradoOgniRaritaTranneLaMassima() {
    assertEquals(Rarity.UNCOMMON, ChallengerEquipment.oneGradeAbove(Rarity.COMMON));
    assertEquals(Rarity.RARE, ChallengerEquipment.oneGradeAbove(Rarity.UNCOMMON));
    assertEquals(Rarity.EPIC, ChallengerEquipment.oneGradeAbove(Rarity.RARE));
    assertEquals(Rarity.LEGENDARY, ChallengerEquipment.oneGradeAbove(Rarity.EPIC));
  }

  @Test
  void oneGradeAboveRestaSuLegendaryQuandoGiaAlMassimo() {
    assertEquals(Rarity.LEGENDARY, ChallengerEquipment.oneGradeAbove(Rarity.LEGENDARY));
  }

  @Test
  void oneGradeAboveRifiutaUnaRaritaNulla() {
    assertThrows(IllegalArgumentException.class, () -> ChallengerEquipment.oneGradeAbove(null));
  }

  private Set<Rarity> drawnRarities(RarityTable rarityTable, Random random) {
    Set<Rarity> rarities = EnumSet.noneOf(Rarity.class);
    for (int draw = 0; draw < RARITY_SAMPLE_SIZE; draw++) {
      rarities.add(rarityTable.draw(random));
    }
    return rarities;
  }
}
