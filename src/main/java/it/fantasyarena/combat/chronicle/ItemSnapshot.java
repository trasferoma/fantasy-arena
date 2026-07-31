package it.fantasyarena.combat.chronicle;

import it.fantasytoolkitcore.core.model.Rarity;

/**
 * Fotografia uniforme di un oggetto equipaggiabile: arma, pezzo d'armatura o gioiello. Serve alla
 * procedura di fine scontro per portare sia l'oggetto trovato sia quello lasciato senza i tre campi
 * nullabili che questa fase toglie a {@code HeroProgress}, ed è riusata anche per l'arma, i pezzi
 * d'armatura e i gioielli delle fotografie di combattente e protagonista.
 *
 * <p>{@link #kind()} dichiara di che oggetto si tratta; {@link #name()} è il nome della costante
 * concreta ({@code Weapon}, {@code Armour} o {@code Jewel} a seconda di {@link #kind()}), non
 * l'enum stesso: qui, a differenza di {@link #rarity()}, l'enum concreto non serve a chi legge
 * questo DTO di confine, e i tre campi nullabili di un {@code weapon()}/{@code armourSlot()}/
 * {@code jewel()} separati sarebbero esattamente la forma che questa fase toglie a
 * {@code HeroProgress}. Non si perde informazione nella narrazione: {@code Weapon}, {@code Armour}
 * e {@code Jewel} sono enum di sole costanti senza {@code toString()} ridefinito (vedi
 * {@code core.md} del toolkit), quindi {@link #name()} è già il testo che la console stampa oggi
 * concatenando l'enum. {@link #power()} è l'attacco per un'arma e la difesa per un'armatura; è
 * {@code null} per un gioiello, l'unico dei tre a non avere un numero — solo la rarità.
 */
public record ItemSnapshot(ItemKind kind, String name, Rarity rarity, Integer power) {
}
