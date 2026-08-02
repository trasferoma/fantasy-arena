package it.fantasyarena.combat.io.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.model.IntrinsicRatings;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.buffdebuffgenerator.result.BuffElement;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;

/**
 * Formatta la scheda compatta multi-riga di un combattente: intestazione, razza/classe,
 * arma e un rigo per ogni pezzo d'armatura indossato (con rarità e valore), vita, stamina, ATK e
 * DEF. Puro (nessun I/O),
 * a larghezza contenuta (le righe troppo lunghe sono troncate con "..."), cosi' da poter
 * essere affiancata ad altre colonne. Riusato dal riepilogo pre-combattimento, dal renderer
 * a schermo e dal riepilogo finale: una sola sorgente di formattazione della scheda.
 *
 * <p>I bonus che arma e armatura portano finché restano equipaggiate compaiono su una riga
 * propria sotto l'oggetto, solo nella scheda piena ({@link #card}): la {@link #compactCard}
 * esiste per il poco spazio verticale del replay a schermo e già omette le caratteristiche, quindi
 * omette anche i bonus.
 */
public class FighterCardFormatter {

  private static final int MAX_WIDTH = 36;

  public List<String> card(int index, Fighter fighter) {
    return buildCard(index, fighter, true);
  }

  /**
   * Variante ridotta della scheda, senza l'elenco delle caratteristiche: pensata per i contesti
   * a spazio verticale limitato, come la colonna delle schede del replay a schermo, dove la
   * scheda completa dei due combattenti non entrerebbe nell'altezza della pagina.
   */
  public List<String> compactCard(int index, Fighter fighter) {
    return buildCard(index, fighter, false);
  }

  private List<String> buildCard(int index, Fighter fighter, boolean withEquipmentDetail) {
    CharacterResult character = fighter.character();
    IntrinsicRatings ratings = fighter.ratings();

    List<String> lines = new ArrayList<>();
    lines.add(truncate("[" + index + "] " + fighter.name()));
    lines.add(truncate(character.race() + " " + character.characterClass()));
    if (withEquipmentDetail) {
      appendCharacteristics(lines, character);
    }
    appendWeapon(lines, fighter.weapon(), withEquipmentDetail);
    appendArmourPieces(lines, fighter.armourPieces(), withEquipmentDetail);
    lines.add(truncate("VIT " + ratings.maxHealth() + "  STA " + ratings.maxStamina()));
    lines.add(truncate("ATK " + formatRating(ratings.offensiveRating())
        + "  DEF " + formatRating(ratings.defensiveRating())));
    return lines;
  }

  private void appendCharacteristics(List<String> lines, CharacterResult character) {
    character.characteristics().forEach(characteristic ->
        lines.add(truncate(characteristic.characteristic().name() + " " + characteristic.value())));
  }

  private void appendWeapon(List<String> lines, WeaponResult weapon, boolean withEquipmentDetail) {
    lines.add(truncate("Arma  " + weapon.weapon() + " (" + weapon.rarity() + ") atk "
        + weapon.attack()));
    if (withEquipmentDetail) {
      bonusLine(weapon.buffs()).ifPresent(line -> lines.add(truncate(line)));
    }
  }

  /**
   * Una riga per pezzo indossato: chi ne raccoglie di nuovi vede la scheda allungarsi, ed è
   * esattamente l'informazione che serve. Le colonne che ospitano la scheda si dimensionano
   * sull'altezza effettiva, quindi il numero variabile di righe non sfonda alcun layout.
   */
  private void appendArmourPieces(List<String> lines, List<ArmourResult> pieces,
      boolean withEquipmentDetail) {
    pieces.forEach(piece -> {
      lines.add(truncate(armourLine(piece)));
      if (withEquipmentDetail) {
        bonusLine(piece.buffs()).ifPresent(line -> lines.add(truncate(line)));
      }
    });
  }

  private String armourLine(ArmourResult piece) {
    return "Arm.  " + piece.armour() + " (" + piece.rarity() + ") def " + piece.defense();
  }

  private Optional<String> bonusLine(List<BuffElement> buffs) {
    if (buffs.isEmpty()) {
      return Optional.empty();
    }
    String formatted = buffs.stream()
        .map(buff -> "+" + buff.value() + " " + buff.characteristic())
        .collect(Collectors.joining(", "));
    return Optional.of("Bonus " + formatted);
  }

  private String formatRating(double rating) {
    return String.format(Locale.ITALY, "%.1f", rating);
  }

  private String truncate(String line) {
    if (line.length() <= MAX_WIDTH) {
      return line;
    }
    return line.substring(0, MAX_WIDTH - 3) + "...";
  }
}
