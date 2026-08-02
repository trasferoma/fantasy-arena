package it.fantasyarena.combat.chronicle;

import java.util.List;

import it.fantasyarena.combat.hero.HeroProgress.CharacteristicGain;
import it.fantasyarena.combat.hero.LootFate;

/**
 * I dati della procedura di fine scontro di una prova vinta: l'oggetto trovato, il suo destino come
 * {@link LootFate}, l'eventuale oggetto lasciato, i punti caratteristica guadagnati e la scheda del
 * protagonista dopo la crescita.
 *
 * <p>{@link #dropped()} è {@code null} quando {@link #fate()} scarta l'oggetto trovato o lo indossa
 * su una parte prima scoperta: in nessuno dei due casi qualcosa lascia il posto a qualcos'altro. Il
 * gioiello non vale più punti caratteristica di suo: i suoi eventuali buff si leggono già dai
 * {@link CharacteristicBonus} di {@link #found()} e di {@link #dropped()}, come per arma e
 * armatura.
 */
public record ProgressChronicle(ItemSnapshot found, LootFate fate, ItemSnapshot dropped,
    List<CharacteristicGain> gains, HeroSnapshot heroAfter) {

  public ProgressChronicle {
    gains = List.copyOf(gains);
  }
}
