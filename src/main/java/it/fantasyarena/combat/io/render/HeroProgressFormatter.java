package it.fantasyarena.combat.io.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import it.fantasyarena.combat.hero.HeroProgress;
import it.fantasyarena.combat.hero.HeroProgress.ArmourUpgrade;
import it.fantasyarena.combat.hero.HeroProgress.CharacteristicGain;
import it.fantasyarena.combat.hero.HeroProgress.JewelUpgrade;
import it.fantasyarena.combat.hero.HeroProgress.WeaponSwap;
import it.fantasyarena.combat.hero.LootFate;
import it.fantasyarena.combat.io.log.ConsoleArenaLogger;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.buffdebuffgenerator.result.BuffElement;
import it.fantasytoolkit.jewelgenerator.result.JewelResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;

/**
 * Racconta la procedura di fine scontro riga per riga: la cura, l'unico oggetto di loot trovato e
 * il suo destino, i bonus che l'oggetto trovato e quello lasciato portano, i punti caratteristica
 * spesi. Puro, nessun I/O: la stampa è del {@link ConsoleArenaLogger}.
 *
 * <p>Legge un {@link HeroProgress}, cioè esattamente i dati che il {@code HeroBrain} ha prodotto
 * decidendo: il racconto non può divergere da quello che è successo davvero, perché è la stessa
 * cosa letta due volte. Dice sempre qualcosa sull'oggetto trovato anche quando è stato scartato —
 * "non ti serve" è un'informazione, il silenzio è un dubbio. La frase si sceglie sul
 * {@link LootFate} già risolto da {@link HeroProgress#lootFate()}, non lo deduce da sé. Il bonus di
 * un oggetto compare su una riga propria sotto la frase che lo racconta, mai accodato: la riga
 * dell'oggetto ha già il suo formato, e il bonus è un'informazione a parte.
 */
public class HeroProgressFormatter {

  private static final String HEADING = "--- PROCEDURA DI FINE SCONTRO ---";

  public List<String> lines(HeroProgress progress) {
    List<String> lines = new ArrayList<>();
    lines.add(HEADING);
    lines.add(progress.grownHero().name() + " è ancora in piedi: vita e stamina tornano piene.");
    lines.addAll(lootLines(progress));
    lines.add(growthLine(progress.characteristicGains()));
    return lines;
  }

  private List<String> lootLines(HeroProgress progress) {
    return switch (progress.lootFate()) {
      case WEAPON_TAKEN -> weaponTakenLines(progress);
      case WEAPON_DISCARDED -> weaponDiscardedLines(progress);
      case ARMOUR_WORN_ON_EMPTY_SLOT -> armourWornLines(progress);
      case ARMOUR_REPLACED -> armourReplacedLines(progress);
      case ARMOUR_DISCARDED -> armourDiscardedLines(progress);
      case JEWEL_WORN_ON_EMPTY_TYPE -> jewelWornLines(progress);
      case JEWEL_REPLACED -> jewelReplacedLines(progress);
      case JEWEL_DISCARDED -> jewelDiscardedLines(progress);
    };
  }

  private List<String> weaponTakenLines(HeroProgress progress) {
    WeaponResult found = progress.loot().weapon().orElseThrow();
    WeaponSwap swap = progress.weaponSwap().orElseThrow();
    String narrative = "Arma: trovi " + describe(found) + ", lasci " + describe(swap.dropped())
        + " e la impugni.";
    return withBonusLines(narrative, found.buffs(), swap.dropped().buffs());
  }

  private List<String> weaponDiscardedLines(HeroProgress progress) {
    WeaponResult found = progress.loot().weapon().orElseThrow();
    String narrative = "Arma: trovi " + describe(found) + ", non batte la tua: la scarti.";
    return withBonusLines(narrative, found.buffs());
  }

  private List<String> armourWornLines(HeroProgress progress) {
    ArmourResult found = progress.loot().armourPiece().orElseThrow();
    String narrative = "Armatura: trovi " + describe(found)
        + ", copre una parte del corpo prima scoperta: la indossi.";
    return withBonusLines(narrative, found.buffs());
  }

  private List<String> armourReplacedLines(HeroProgress progress) {
    ArmourResult found = progress.loot().armourPiece().orElseThrow();
    ArmourUpgrade upgrade = progress.armourUpgrade().orElseThrow();
    String narrative = "Armatura: trovi " + describe(found) + ", sostituisce "
        + describe(upgrade.dropped()) + ".";
    return withBonusLines(narrative, found.buffs(), upgrade.dropped().buffs());
  }

  private List<String> armourDiscardedLines(HeroProgress progress) {
    ArmourResult found = progress.loot().armourPiece().orElseThrow();
    String narrative = "Armatura: trovi " + describe(found)
        + ", difende meno o quanto la tua: la scarti.";
    return withBonusLines(narrative, found.buffs());
  }

  private List<String> jewelWornLines(HeroProgress progress) {
    JewelResult found = progress.loot().jewel().orElseThrow();
    String narrative = "Gioiello: trovi " + describe(found)
        + ", è un tipo che non portavi ancora: lo indossi.";
    return withBonusLines(narrative, found.buffs());
  }

  private List<String> jewelReplacedLines(HeroProgress progress) {
    JewelResult found = progress.loot().jewel().orElseThrow();
    JewelUpgrade upgrade = progress.jewelUpgrade().orElseThrow();
    String narrative = "Gioiello: trovi " + describe(found) + ", sostituisce "
        + describe(upgrade.dropped()) + ".";
    return withBonusLines(narrative, found.buffs(), upgrade.dropped().buffs());
  }

  private List<String> jewelDiscardedLines(HeroProgress progress) {
    JewelResult found = progress.loot().jewel().orElseThrow();
    String narrative = "Gioiello: trovi " + describe(found)
        + ", non batte quello che porti: lo scarti.";
    return withBonusLines(narrative, found.buffs());
  }

  private List<String> withBonusLines(String narrative, List<BuffElement> foundBuffs) {
    return withBonusLines(narrative, foundBuffs, List.of());
  }

  /**
   * La riga narrativa seguita, quando ci sono buff da raccontare, dalla riga del bonus trovato e da
   * quella del bonus lasciato: mai accodati alla riga dell'oggetto, che il troncamento a 36
   * caratteri di {@link FighterCardFormatter} non tocca qui ma il cui formato non va comunque
   * sovraccaricato.
   */
  private List<String> withBonusLines(String narrative, List<BuffElement> foundBuffs,
      List<BuffElement> droppedBuffs) {
    List<String> lines = new ArrayList<>();
    lines.add(narrative);
    bonusLine("Bonus dell'oggetto trovato", foundBuffs).ifPresent(lines::add);
    bonusLine("Bonus dell'oggetto lasciato", droppedBuffs).ifPresent(lines::add);
    return lines;
  }

  private Optional<String> bonusLine(String label, List<BuffElement> buffs) {
    if (buffs.isEmpty()) {
      return Optional.empty();
    }
    String formatted = buffs.stream()
        .map(buff -> "+" + buff.value() + " " + buff.characteristic())
        .collect(Collectors.joining(", "));
    return Optional.of(label + ": " + formatted + ".");
  }

  private String growthLine(List<CharacteristicGain> characteristicGains) {
    String gains = characteristicGains.stream()
        .map(gain -> "+" + gain.points() + " " + gain.characteristic())
        .collect(Collectors.joining(", "));
    return "Crescita: " + gains + ".";
  }

  private String describe(WeaponResult weapon) {
    return weapon.weapon() + " (" + weapon.rarity() + ", atk " + weapon.attack() + ")";
  }

  private String describe(ArmourResult piece) {
    return piece.armour() + " (" + piece.rarity() + ", def " + piece.defense() + ")";
  }

  private String describe(JewelResult jewel) {
    return jewel.jewel() + " (" + jewel.rarity() + ")";
  }
}
