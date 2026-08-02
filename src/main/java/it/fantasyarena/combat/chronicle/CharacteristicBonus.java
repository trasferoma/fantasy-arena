package it.fantasyarena.combat.chronicle;

import it.fantasytoolkitcore.core.model.Characteristic;

/**
 * Il bonus che un oggetto equipaggiato porta a una singola caratteristica: la coppia
 * caratteristica/valore con cui {@link ItemSnapshot} elenca i buff di arma, pezzo d'armatura e
 * gioiello. Non si riusa {@code BuffElement} del toolkit perché è {@code SNAPSHOT} e finirebbe nel
 * contratto JSON verso il JavaScript, che nessun test protegge oltre a {@code ChronicleJsonTest}.
 */
public record CharacteristicBonus(Characteristic characteristic, int value) {
}
