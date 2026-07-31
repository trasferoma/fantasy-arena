package it.fantasyarena.combat.chronicle;

import java.util.List;

import it.fantasyarena.combat.hero.HeroProgress.CharacteristicGain;
import it.fantasyarena.combat.hero.LootFate;

/**
 * I dati della procedura di fine scontro di una prova vinta: l'oggetto trovato, il suo destino come
 * {@link LootFate}, l'eventuale oggetto lasciato, l'eventuale bonus del gioiello indossato, i punti
 * caratteristica guadagnati e la scheda del protagonista dopo la crescita.
 *
 * <p>{@link #dropped()} è {@code null} quando {@link #fate()} scarta l'oggetto trovato o lo indossa
 * su una parte prima scoperta: in nessuno dei due casi qualcosa lascia il posto a qualcos'altro.
 *
 * <p>{@link #jewelBonusPoints()} è {@code null} in sei degli otto destini: esiste solo quando
 * {@link #fate()} è {@code JEWEL_WORN_ON_EMPTY_TYPE} o {@code JEWEL_REPLACED}, gli unici due in cui
 * {@code HeroBrain} assegna un bonus di punti caratteristica per il gioiello indossato. {@link #gains()}
 * porta solo il totale già fuso con i tre punti fissi della vittoria, e da quel totale il bonus del
 * gioiello non si separa: il campo esiste perché un futuro lettore della cronaca possa comporre la
 * frase completa senza conoscere la costante di bilanciamento che l'ha prodotto.
 */
public record ProgressChronicle(ItemSnapshot found, LootFate fate, ItemSnapshot dropped, Integer jewelBonusPoints,
    List<CharacteristicGain> gains, HeroSnapshot heroAfter) {

  public ProgressChronicle {
    gains = List.copyOf(gains);
  }
}
