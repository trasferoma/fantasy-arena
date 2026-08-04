package it.fantasyarena.combat;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.RarityTable;

/**
 * I quattro scaglioni della rarità del loot del protagonista lungo il percorso a dieci prove:
 * confini e pavimento estraibile.
 */
class TrialLootTest {

  private static final int RARITY_SAMPLE_SIZE = 500;

  @Test
  void laTabellaDiRaritaDelLootSeguiQuattroScaglioniSulPercorsoADieciProve() {
    assertSame(TrialLoot.forTrial(1), TrialLoot.forTrial(2),
        "le prove 1 e 2 condividono lo scaglione d'apertura");
    assertSame(TrialLoot.forTrial(3), TrialLoot.forTrial(5),
        "le prove 3-5 condividono lo stesso scaglione");
    assertSame(TrialLoot.forTrial(6), TrialLoot.forTrial(8),
        "le prove 6-8 condividono lo stesso scaglione");
    assertSame(TrialLoot.forTrial(9), TrialLoot.forTrial(10),
        "le prove 9-10 condividono lo stesso scaglione");

    assertNotSame(TrialLoot.forTrial(2), TrialLoot.forTrial(3),
        "il secondo scaglione comincia alla terza prova, non più alla seconda");
    assertNotSame(TrialLoot.forTrial(5), TrialLoot.forTrial(6),
        "il terzo scaglione comincia alla sesta prova");
    assertNotSame(TrialLoot.forTrial(8), TrialLoot.forTrial(9),
        "il quarto scaglione comincia alla nona prova");
  }

  @Test
  void ilPavimentoDellaRaritaRestaUncommonFinoAllaProva5EPoiSaleARare() {
    Set<Rarity> openingRarities = drawnRarities(TrialLoot.forTrial(1), new Random(7));
    Set<Rarity> earlyRarities = drawnRarities(TrialLoot.forTrial(3), new Random(7));
    Set<Rarity> midRarities = drawnRarities(TrialLoot.forTrial(6), new Random(7));
    Set<Rarity> lateRarities = drawnRarities(TrialLoot.forTrial(9), new Random(7));

    assertTrue(EnumSet.of(Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY).containsAll(openingRarities),
        "le prove 1-2 non devono produrre nulla sotto UNCOMMON");
    assertTrue(EnumSet.of(Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY).containsAll(earlyRarities),
        "le prove 3-5 restano con lo stesso pavimento UNCOMMON delle prove 1-2");
    assertTrue(EnumSet.of(Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY).containsAll(midRarities),
        "solo dalla prova 6 il pavimento sale a RARE");
    assertTrue(EnumSet.of(Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY).containsAll(lateRarities),
        "le prove 9-10 condividono il pavimento RARE con le prove 6-8, non lo alzano a EPIC");
  }

  private Set<Rarity> drawnRarities(RarityTable table, Random random) {
    Set<Rarity> rarities = EnumSet.noneOf(Rarity.class);
    for (int draw = 0; draw < RARITY_SAMPLE_SIZE; draw++) {
      rarities.add(table.draw(random));
    }
    return rarities;
  }
}
