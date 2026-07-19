package it.fantasyarena.combat.context;

import java.util.List;

/**
 * Contesto temporaneo del turno: aggrega i {@link ContextModifier} attivi in un unico
 * moltiplicatore per lato. In v1 è sempre vuoto ({@link #empty()}) e quindi neutro
 * (moltiplicatore 1.0): non altera Rating né esito dello scontro.
 */
public final class CombatContext {

  private final List<ContextModifier> modifiers;

  private CombatContext(List<ContextModifier> modifiers) {
    this.modifiers = modifiers;
  }

  public static CombatContext empty() {
    return new CombatContext(List.of());
  }

  public static CombatContext of(List<ContextModifier> modifiers) {
    return new CombatContext(List.copyOf(modifiers));
  }

  public double offensiveMultiplier() {
    return modifiers.stream()
        .mapToDouble(ContextModifier::offensiveMultiplier)
        .reduce(1.0, (a, b) -> a * b);
  }

  public double defensiveMultiplier() {
    return modifiers.stream()
        .mapToDouble(ContextModifier::defensiveMultiplier)
        .reduce(1.0, (a, b) -> a * b);
  }
}
