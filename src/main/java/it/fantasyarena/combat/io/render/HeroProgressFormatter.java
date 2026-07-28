package it.fantasyarena.combat.io.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import it.fantasyarena.combat.hero.HeroProgress;
import it.fantasyarena.combat.hero.HeroProgress.ArmourUpgrade;
import it.fantasyarena.combat.hero.HeroProgress.CharacteristicGain;
import it.fantasyarena.combat.hero.HeroProgress.JewelUpgrade;
import it.fantasyarena.combat.hero.HeroProgress.NewJewel;
import it.fantasyarena.combat.hero.HeroProgress.WeaponSwap;
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
 * "non ti serve" è un'informazione, il silenzio è un dubbio.
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
    Optional<WeaponResult> weaponFound = progress.loot().weapon();
    if (weaponFound.isPresent()) {
      return weaponLootLine(weaponFound.get(), progress.weaponSwap());
    }

    Optional<ArmourResult> armourFound = progress.loot().armourPiece();
    if (armourFound.isPresent()) {
      return armourLootLine(armourFound.get(), progress.newPiece(), progress.armourUpgrade());
    }

    JewelResult jewelFound = progress.loot().jewel().orElseThrow();
    return jewelLootLine(jewelFound, progress.newJewel(), progress.jewelUpgrade());
  }

  private String weaponLootLine(WeaponResult found, Optional<WeaponSwap> swap) {
    return swap
        .map(taken -> "Arma: trovi " + describe(found) + ", lasci " + describe(taken.dropped()) + " e la impugni.")
        .orElse("Arma: trovi " + describe(found) + ", non batte la tua: la scarti.");
  }

  private String armourLootLine(ArmourResult found, Optional<ArmourResult> newPiece,
      Optional<ArmourUpgrade> upgrade) {
    if (newPiece.isPresent()) {
      return "Armatura: trovi " + describe(found) + ", copre una parte del corpo prima scoperta: la indossi.";
    }
    return upgrade
        .map(taken -> "Armatura: trovi " + describe(found) + ", sostituisce " + describe(taken.dropped()) + ".")
        .orElse("Armatura: trovi " + describe(found) + ", difende meno o quanto la tua: la scarti.");
  }

  private String jewelLootLine(JewelResult found, Optional<NewJewel> newJewel, Optional<JewelUpgrade> upgrade) {
    if (newJewel.isPresent()) {
      return "Gioiello: trovi " + describe(found) + ", è un tipo che non portavi ancora: lo indossi, vale +"
          + newJewel.get().points() + " punti caratteristica.";
    }
    return upgrade
        .map(taken -> "Gioiello: trovi " + describe(found) + ", sostituisce " + describe(taken.dropped())
            + " e vale +" + taken.points() + " punti caratteristica.")
        .orElse("Gioiello: trovi " + describe(found) + ", non batte quello che porti: lo scarti.");
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
