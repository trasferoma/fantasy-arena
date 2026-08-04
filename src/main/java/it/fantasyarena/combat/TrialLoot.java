package it.fantasyarena.combat;

import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.RarityTable;

/**
 * La rarità estraibile dal loot di una stazione del percorso. Non è una scelta del protagonista:
 * lui non decide quanto sia pregiato l'oggetto che trova, lo trova e basta — semmai decide se
 * tenerlo, ed è quella la scelta che resta in {@code HeroBrain}. È la generosità del percorso
 * calata su quella prova, esattamente come {@link ChallengerBudget} è la pressione del percorso
 * sul monte punti degli sfidanti e {@link ChallengerEquipment} lo è sul loro equipaggiamento: per
 * questo vive accanto a {@link TrialPlan} e non nel cervello del protagonista. È il gemello di
 * {@link ChallengerEquipment} sul lato del protagonista, e come quello è indicizzato dal numero
 * della prova invece di essere un campo di {@link TrialStation}, che sarebbe un dato custodito
 * ricavabile dalla stazione stessa.
 *
 * <p>La distribuzione è pesata e non uniforme di proposito: un pavimento espresso come rarità
 * minima renderebbe {@code LEGENDARY} tanto probabile quanto il grado del pavimento stesso, e il
 * loot sopra il raro diventerebbe la norma invece dell'eccezione. Ogni scaglione di questa tabella
 * corregge proprio questo, mantenendo {@code LEGENDARY} minoritario per tutto il percorso.
 *
 * <p>Il pavimento non sale a ogni scaglione: {@code UNCOMMON} resta estraibile fino alla prova 5 e
 * sale a {@code RARE} solo dalla prova 6 in poi, perché un'arma {@code LEGENDARY} ha attacco 15-25
 * contro 3-6 di una {@code UNCOMMON} e porta buff per una decina di punti, mentre una vittoria di
 * progressione ne vale solo tre di caratteristica: un solo drop leggendario valeva quindi più di
 * tre vittorie, e arrivava troppo presto nella corsa con la distribuzione precedente a questa
 * ritaratura.
 */
public final class TrialLoot {

  /**
   * La distribuzione della rarità del loot alle prove 1-2: {@code LEGENDARY} resta accessibile ma
   * marginale, e il pavimento è {@code UNCOMMON}.
   */
  private static final RarityTable OPENING_TRIALS_LOOT_RARITY_TABLE = RarityTable.builder()
      .entry(Rarity.UNCOMMON, 70)
      .entry(Rarity.RARE, 20)
      .entry(Rarity.EPIC, 5)
      .entry(Rarity.LEGENDARY, 5)
      .build();

  /**
   * La distribuzione della rarità del loot alle prove 3-5: il pavimento resta {@code UNCOMMON}
   * come in {@link #OPENING_TRIALS_LOOT_RARITY_TABLE}, ma il peso si sposta verso {@code RARE}.
   */
  private static final RarityTable EARLY_TRIALS_LOOT_RARITY_TABLE = RarityTable.builder()
      .entry(Rarity.UNCOMMON, 65)
      .entry(Rarity.RARE, 20)
      .entry(Rarity.EPIC, 10)
      .entry(Rarity.LEGENDARY, 5)
      .build();

  /**
   * La distribuzione della rarità del loot alle prove 6-8: qui, e non prima, il pavimento sale a
   * {@code RARE}, e il peso principale resta su {@code RARE} stesso, con {@code EPIC} minoritario.
   */
  private static final RarityTable MID_TRIALS_LOOT_RARITY_TABLE = RarityTable.builder()
      .entry(Rarity.RARE, 85)
      .entry(Rarity.EPIC, 10)
      .entry(Rarity.LEGENDARY, 5)
      .build();

  /**
   * La distribuzione della rarità del loot alle prove 9-10, l'ultimo tratto del percorso: pesi
   * identici a {@link #MID_TRIALS_LOOT_RARITY_TABLE}. Il pavimento resta {@code RARE} e
   * {@code LEGENDARY} non cresce oltre il suo peso dello scaglione precedente: la taratura
   * empirica non ha richiesto di inasprire ulteriormente il loot dell'eroe in questo tratto.
   */
  private static final RarityTable LATE_TRIALS_LOOT_RARITY_TABLE = RarityTable.builder()
      .entry(Rarity.RARE, 85)
      .entry(Rarity.EPIC, 10)
      .entry(Rarity.LEGENDARY, 5)
      .build();

  private TrialLoot() {
  }

  /**
   * La tabella con cui viene estratta la rarità del loot di quella prova, a quattro scaglioni sul
   * percorso a dieci prove: 1-2, 3-5, 6-8, 9-10. Oltre la decima prova si applica l'ultima fascia,
   * come già fa {@link ChallengerEquipment#forTrial(int)} col suo {@code default}: un percorso più
   * lungo di dieci stazioni non deve restare senza scaglione.
   *
   * @param trialNumber il numero della prova, quello dichiarato da {@link TrialStation#number()}
   * @return la tabella pesata da cui estrarre la rarità del loot di quella prova
   */
  public static RarityTable forTrial(int trialNumber) {
    return switch (trialNumber) {
      case 1, 2 -> OPENING_TRIALS_LOOT_RARITY_TABLE;
      case 3, 4, 5 -> EARLY_TRIALS_LOOT_RARITY_TABLE;
      case 6, 7, 8 -> MID_TRIALS_LOOT_RARITY_TABLE;
      default -> LATE_TRIALS_LOOT_RARITY_TABLE;
    };
  }
}
