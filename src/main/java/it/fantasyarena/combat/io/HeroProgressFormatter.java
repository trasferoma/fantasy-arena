package it.fantasyarena.combat.io;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import it.fantasyarena.combat.hero.HeroProgress;
import it.fantasyarena.combat.hero.HeroProgress.ArmourUpgrade;
import it.fantasyarena.combat.hero.HeroProgress.CharacteristicGain;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;

/**
 * Racconta la procedura di fine scontro riga per riga: la cura, l'arma raccolta o tenuta, ogni
 * pezzo d'armatura conquistato, i punti caratteristica spesi. Puro, nessun I/O: la stampa è del
 * {@link ConsoleArenaLogger}.
 *
 * <p>Legge un {@link HeroProgress}, cioè esattamente i dati che il {@code HeroBrain} ha prodotto
 * decidendo: il racconto non può divergere da quello che è successo davvero, perché è la stessa
 * cosa letta due volte. Dice sempre qualcosa anche quando non è cambiato niente — "non hai
 * raccolto nulla" è un'informazione, il silenzio è un dubbio.
 */
public class HeroProgressFormatter {

  private static final String HEADING = "--- PROCEDURA DI FINE SCONTRO ---";

  public List<String> lines(HeroProgress progress) {
    List<String> lines = new ArrayList<>();
    lines.add(HEADING);
    lines.add(progress.grownHero().name() + " è ancora in piedi: vita e stamina tornano piene.");
    lines.add(weaponLine(progress));
    lines.addAll(armourLines(progress));
    lines.add(growthLine(progress.characteristicGains()));
    return lines;
  }

  private String weaponLine(HeroProgress progress) {
    return progress.weaponSwap()
        .map(swap -> "Arma: lascia " + describe(swap.dropped()) + " e impugna " + describe(swap.taken()) + ".")
        .orElse("Arma: tiene " + describe(progress.grownHero().weapon()) + ", niente di meglio sul terreno.");
  }

  private List<String> armourLines(HeroProgress progress) {
    List<String> lines = new ArrayList<>();
    progress.newPieces().forEach(piece ->
        lines.add("Armatura: raccoglie " + describe(piece) + ", parte del corpo prima scoperta."));
    progress.armourUpgrades().forEach(upgrade -> lines.add(upgradeLine(upgrade)));

    if (lines.isEmpty()) {
      lines.add("Armatura: nessun pezzo a terra migliora quella che indossa.");
    }
    return lines;
  }

  private String upgradeLine(ArmourUpgrade upgrade) {
    return "Armatura: sostituisce " + describe(upgrade.dropped()) + " con " + describe(upgrade.taken()) + ".";
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
}
