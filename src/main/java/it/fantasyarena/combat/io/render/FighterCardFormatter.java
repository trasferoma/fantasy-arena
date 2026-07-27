package it.fantasyarena.combat.io.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.model.IntrinsicRatings;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;

/**
 * Formatta la scheda compatta multi-riga di un combattente: intestazione, razza/classe,
 * arma e un rigo per ogni pezzo d'armatura indossato (con rarità e valore), vita, stamina, ATK e
 * DEF. Puro (nessun I/O),
 * a larghezza contenuta (le righe troppo lunghe sono troncate con "..."), cosi' da poter
 * essere affiancata ad altre colonne. Riusato dal riepilogo pre-combattimento, dal renderer
 * a schermo e dal riepilogo finale: una sola sorgente di formattazione della scheda.
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

  private List<String> buildCard(int index, Fighter fighter, boolean withCharacteristics) {
    CharacterResult character = fighter.character();
    WeaponResult weapon = fighter.weapon();
    IntrinsicRatings ratings = fighter.ratings();

    List<String> lines = new ArrayList<>();
    lines.add(truncate("[" + index + "] " + fighter.name()));
    lines.add(truncate(character.race() + " " + character.characterClass()));
    if (withCharacteristics) {
      character.characteristics()
          .forEach(characteristic -> lines.add(truncate(characteristic.characteristic().name() + " " + characteristic.value())));
    }
    lines.add(truncate("Arma  " + weapon.weapon() + " (" + weapon.rarity() + ") atk " + weapon.attack()));
    fighter.armourPieces().forEach(piece -> lines.add(truncate(armourLine(piece))));
    lines.add(truncate("VIT " + ratings.maxHealth() + "  STA " + ratings.maxStamina()));
    lines.add(truncate("ATK " + formatRating(ratings.offensiveRating())
        + "  DEF " + formatRating(ratings.defensiveRating())));
    return lines;
  }

  /**
   * Una riga per pezzo indossato: chi ne raccoglie di nuovi vede la scheda allungarsi, ed è
   * esattamente l'informazione che serve. Le colonne che ospitano la scheda si dimensionano
   * sull'altezza effettiva, quindi il numero variabile di righe non sfonda alcun layout.
   */
  private String armourLine(ArmourResult piece) {
    return "Arm.  " + piece.armour() + " (" + piece.rarity() + ") def " + piece.defense();
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
