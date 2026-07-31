package it.fantasyarena.combat.io.render;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import it.fantasyarena.combat.hero.HeroProgress;
import it.fantasyarena.combat.hero.HeroProgress.ArmourUpgrade;
import it.fantasyarena.combat.hero.HeroProgress.CharacteristicGain;
import it.fantasyarena.combat.hero.HeroProgress.JewelUpgrade;
import it.fantasyarena.combat.hero.HeroProgress.NewJewel;
import it.fantasyarena.combat.hero.HeroProgress.WeaponSwap;
import it.fantasyarena.combat.hero.LootFate;
import it.fantasyarena.combat.io.log.ConsoleArenaLogger;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.jewelgenerator.result.JewelResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;

/**
 * Racconta la procedura di fine scontro riga per riga: la cura, l'unico oggetto di loot trovato e
 * il suo destino, i punti caratteristica spesi. Puro, nessun I/O: la stampa è del
 * {@link ConsoleArenaLogger}.
 *
 * <p>Legge un {@link HeroProgress}, cioè esattamente i dati che il {@code HeroBrain} ha prodotto
 * decidendo: il racconto non può divergere da quello che è successo davvero, perché è la stessa
 * cosa letta due volte. Dice sempre qualcosa sull'oggetto trovato anche quando è stato scartato —
 * "non ti serve" è un'informazione, il silenzio è un dubbio. La frase si sceglie sul
 * {@link LootFate} già risolto da {@link HeroProgress#lootFate()}, non lo deduce da sé.
 */
public class HeroProgressFormatter {

  private static final String HEADING = "--- PROCEDURA DI FINE SCONTRO ---";

  public List<String> lines(HeroProgress progress) {
    List<String> lines = new ArrayList<>();
    lines.add(HEADING);
    lines.add(progress.grownHero().name() + " è ancora in piedi: vita e stamina tornano piene.");
    lines.add(lootLine(progress));
    lines.add(growthLine(progress.characteristicGains()));
    return lines;
  }

  private String lootLine(HeroProgress progress) {
    return switch (progress.lootFate()) {
      case WEAPON_TAKEN -> weaponTakenLine(progress);
      case WEAPON_DISCARDED -> weaponDiscardedLine(progress);
      case ARMOUR_WORN_ON_EMPTY_SLOT -> armourWornLine(progress);
      case ARMOUR_REPLACED -> armourReplacedLine(progress);
      case ARMOUR_DISCARDED -> armourDiscardedLine(progress);
      case JEWEL_WORN_ON_EMPTY_TYPE -> jewelWornLine(progress);
      case JEWEL_REPLACED -> jewelReplacedLine(progress);
      case JEWEL_DISCARDED -> jewelDiscardedLine(progress);
    };
  }

  private String weaponTakenLine(HeroProgress progress) {
    WeaponResult found = progress.loot().weapon().orElseThrow();
    WeaponSwap swap = progress.weaponSwap().orElseThrow();
    return "Arma: trovi " + describe(found) + ", lasci " + describe(swap.dropped()) + " e la impugni.";
  }

  private String weaponDiscardedLine(HeroProgress progress) {
    WeaponResult found = progress.loot().weapon().orElseThrow();
    return "Arma: trovi " + describe(found) + ", non batte la tua: la scarti.";
  }

  private String armourWornLine(HeroProgress progress) {
    ArmourResult found = progress.loot().armourPiece().orElseThrow();
    return "Armatura: trovi " + describe(found) + ", copre una parte del corpo prima scoperta: la indossi.";
  }

  private String armourReplacedLine(HeroProgress progress) {
    ArmourResult found = progress.loot().armourPiece().orElseThrow();
    ArmourUpgrade upgrade = progress.armourUpgrade().orElseThrow();
    return "Armatura: trovi " + describe(found) + ", sostituisce " + describe(upgrade.dropped()) + ".";
  }

  private String armourDiscardedLine(HeroProgress progress) {
    ArmourResult found = progress.loot().armourPiece().orElseThrow();
    return "Armatura: trovi " + describe(found) + ", difende meno o quanto la tua: la scarti.";
  }

  private String jewelWornLine(HeroProgress progress) {
    JewelResult found = progress.loot().jewel().orElseThrow();
    NewJewel newJewel = progress.newJewel().orElseThrow();
    return "Gioiello: trovi " + describe(found) + ", è un tipo che non portavi ancora: lo indossi, vale +"
        + newJewel.points() + " punti caratteristica.";
  }

  private String jewelReplacedLine(HeroProgress progress) {
    JewelResult found = progress.loot().jewel().orElseThrow();
    JewelUpgrade upgrade = progress.jewelUpgrade().orElseThrow();
    return "Gioiello: trovi " + describe(found) + ", sostituisce " + describe(upgrade.dropped())
        + " e vale +" + upgrade.points() + " punti caratteristica.";
  }

  private String jewelDiscardedLine(HeroProgress progress) {
    JewelResult found = progress.loot().jewel().orElseThrow();
    return "Gioiello: trovi " + describe(found) + ", non batte quello che porti: lo scarti.";
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
