package it.fantasyarena.combat.hero;

/**
 * Il destino dell'unico oggetto trovato a fine livello, come dato: otto casi mutuamente esclusivi,
 * derivati da {@link HeroProgress#lootFate()} incrociando il tipo del {@link Loot} con quale dei
 * cinque campi del destino è valorizzato. Chi legge sceglie sul caso già risolto, invece di dedurlo
 * dalla presenza di un campo — è il dato che sostituisce la vecchia catena di {@code isPresent()}.
 */
public enum LootFate {

  /**
   * L'arma trovata batteva quella impugnata per valore offensivo: è stata presa.
   */
  WEAPON_TAKEN,

  /**
   * L'arma trovata non batteva quella impugnata: è stata scartata.
   */
  WEAPON_DISCARDED,

  /**
   * Il pezzo trovato copriva uno slot d'armatura prima scoperto: è stato indossato.
   */
  ARMOUR_WORN_ON_EMPTY_SLOT,

  /**
   * Il pezzo trovato difendeva più di quello già indossato sullo stesso slot: lo ha sostituito.
   */
  ARMOUR_REPLACED,

  /**
   * Il pezzo trovato non difendeva più di quello già indossato: è stato scartato.
   */
  ARMOUR_DISCARDED,

  /**
   * Il gioiello trovato era di un tipo prima scoperto: è stato indossato.
   */
  JEWEL_WORN_ON_EMPTY_TYPE,

  /**
   * Il gioiello trovato era più raro di quello già indossato dello stesso tipo: lo ha sostituito.
   */
  JEWEL_REPLACED,

  /**
   * Il gioiello trovato non era più raro di quello già indossato: è stato scartato.
   */
  JEWEL_DISCARDED
}
