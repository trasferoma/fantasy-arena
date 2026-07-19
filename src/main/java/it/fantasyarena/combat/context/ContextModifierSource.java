package it.fantasyarena.combat.context;

import java.util.List;

import it.fantasyarena.combat.model.Fighter;

/**
 * Sorgente di {@link ContextModifier} (es. terreno, meteo, benedizioni). Punto di
 * estensione per gli obiettivi futuri: nessuna implementazione è registrata in v1,
 * quindi il {@link CombatContext} resta sempre vuoto.
 */
public interface ContextModifierSource {

  List<ContextModifier> provideModifiers(Fighter fighter, Fighter opponent);
}
