package it.fantasyarena.combat.io;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.IntrinsicRatings;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;

/**
 * Formatta la scheda compatta multi-riga di un combattente: intestazione, razza/classe,
 * arma e armatura (con rarità e valore), vita, stamina, ATK e DEF. Puro (nessun I/O),
 * a larghezza contenuta (le righe troppo lunghe sono troncate con "..."), cosi' da poter
 * essere affiancata ad altre colonne. Riusato dal riepilogo pre-combattimento, dal renderer
 * a schermo e dal riepilogo finale: una sola sorgente di formattazione della scheda.
 */
public class FighterCardFormatter {

  private static final int MAX_WIDTH = 36;

  public List<String> card(int index, Fighter fighter) {
    CharacterResult character = fighter.character();
    WeaponResult weapon = fighter.weapon();
    ArmourResult armour = fighter.armour();
    IntrinsicRatings ratings = fighter.ratings();

    List<String> lines = new ArrayList<>();
    lines.add(truncate("[" + index + "] " + fighter.name()));
    lines.add(truncate(character.race() + " " + character.characterClass()));
    lines.add(truncate("Arma  " + weapon.weapon() + " (" + weapon.rarity() + ") atk " + weapon.attack()));
    lines.add(truncate("Arm.  " + armour.armour() + " (" + armour.rarity() + ") def " + armour.defense()));
    lines.add(truncate("VIT " + ratings.maxHealth() + "  STA " + ratings.maxStamina()));
    lines.add(truncate("ATK " + formatRating(ratings.offensiveRating())
        + "  DEF " + formatRating(ratings.defensiveRating())));
    return lines;
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
