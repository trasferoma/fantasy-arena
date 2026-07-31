package it.fantasyarena.combat.chronicle;

/**
 * Il tipo di oggetto fotografato da {@link ItemSnapshot}: dice esplicitamente qual è il campo da
 * leggere, così il frontend non deve dedurlo dalla presenza di un campo piuttosto che di un altro.
 */
public enum ItemKind {
  WEAPON,
  ARMOUR,
  JEWEL
}
