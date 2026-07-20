package it.fantasyarena.combat.engine;

import java.util.List;

import it.fantasyarena.combat.result.TurnHighlight;

/**
 * Wording da cronaca della coda descrittiva del colpo: dato l'esito di difesa e gli
 * highlight tracciati per il turno, produce una descrizione concisa. Senza highlight il
 * testo coincide esattamente con quello "piatto" prodotto oggi (nessuna regressione sui colpi
 * normali). Con highlight applica una precedenza di enfasi: il 20 naturale assorbe il wording
 * del critico ordinario, il colpo pesante è solo un rafforzativo, il colpo di grazia è la
 * chiusa dominante della frase. Il colpo potente è un qualificatore dell'attacco, componibile
 * con perfetto/critico, sia sul colpo a segno sia sul mancato.
 */
public final class TurnChronicler {

  public String describeOutcome(DefenseOutcome.DefenseResult result, int damage, boolean defenderCanDefend,
      List<TurnHighlight> highlights, String defenderName, boolean powerStrike) {

    String phrase = describePhrase(result, damage, defenderCanDefend, highlights, powerStrike);
    return applyKnockout(phrase, highlights, defenderName);
  }

  /**
   * Descrizione del colpo mancato: piatta come oggi se non era un tentativo di colpo potente,
   * altrimenti segnala il rischio esplicito della doppia Stamina sprecata.
   */
  public String describeMiss(String attackerName, String defenderName, boolean powerStrike) {
    if (powerStrike) {
      return attackerName + " tenta un colpo potente su " + defenderName + " ma manca il colpo.";
    }
    return attackerName + " attacca " + defenderName + " ma manca il colpo.";
  }

  private String describePhrase(DefenseOutcome.DefenseResult result, int damage, boolean defenderCanDefend,
      List<TurnHighlight> highlights, boolean powerStrike) {
    return switch (result) {
      case DODGED -> ", schivato.";
      case PARRIED -> ", parato (" + damage + " danni).";
      case HIT_TAKEN -> describeHitTaken(damage, defenderCanDefend, highlights, powerStrike);
    };
  }

  private String describeHitTaken(int damage, boolean defenderCanDefend, List<TurnHighlight> highlights,
      boolean powerStrike) {
    String qualifier = describeQualifier(highlights, powerStrike);
    String exhaustedLabel = (defenderCanDefend ? "" : "difensore esausto, ");
    String damageLabel = describeDamageLabel(damage, highlights);
    return ", colpo " + qualifier + "a segno (" + exhaustedLabel + damageLabel + ").";
  }

  private String describeQualifier(List<TurnHighlight> highlights, boolean powerStrike) {
    String powerPrefix = (powerStrike ? "potente " : "");
    if (highlights.contains(TurnHighlight.PERFECT_HIT)) {
      return powerPrefix + "perfetto (20 naturale) ";
    }
    if (highlights.contains(TurnHighlight.CRITICAL)) {
      return powerPrefix + "critico ";
    }
    return powerPrefix;
  }

  private String describeDamageLabel(int damage, List<TurnHighlight> highlights) {
    if (highlights.contains(TurnHighlight.HEAVY_BLOW)) {
      return damage + " danni devastanti";
    }
    return damage + " danni";
  }

  private String applyKnockout(String phrase, List<TurnHighlight> highlights, String defenderName) {
    if (!highlights.contains(TurnHighlight.KNOCKOUT)) {
      return phrase;
    }
    String withoutTrailingDot = phrase.substring(0, phrase.length() - 1);
    return withoutTrailingDot + " e " + defenderName + " crolla a terra!";
  }
}
