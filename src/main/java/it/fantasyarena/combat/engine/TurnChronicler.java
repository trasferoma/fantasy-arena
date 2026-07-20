package it.fantasyarena.combat.engine;

import java.util.List;

import it.fantasyarena.combat.result.TurnHighlight;

/**
 * Wording da cronaca della coda descrittiva del colpo: dato l'esito di difesa e gli
 * highlight tracciati per il turno, produce una descrizione concisa. Senza highlight il
 * testo coincide esattamente con quello "piatto" prodotto oggi (nessuna regressione sui colpi
 * normali). Con highlight applica una precedenza di enfasi: il 20 naturale assorbe il wording
 * del critico ordinario, il colpo pesante è solo un rafforzativo, il colpo di grazia è la
 * chiusa dominante della frase.
 */
public final class TurnChronicler {

  public String describeOutcome(DefenseOutcome.DefenseResult result, int damage, boolean defenderCanDefend,
      List<TurnHighlight> highlights, String defenderName) {

    String phrase = describePhrase(result, damage, defenderCanDefend, highlights);
    return applyKnockout(phrase, highlights, defenderName);
  }

  private String describePhrase(DefenseOutcome.DefenseResult result, int damage, boolean defenderCanDefend,
      List<TurnHighlight> highlights) {
    return switch (result) {
      case DODGED -> ", schivato.";
      case PARRIED -> ", parato (" + damage + " danni).";
      case HIT_TAKEN -> describeHitTaken(damage, defenderCanDefend, highlights);
    };
  }

  private String describeHitTaken(int damage, boolean defenderCanDefend, List<TurnHighlight> highlights) {
    String qualifier = describeQualifier(highlights);
    String exhaustedLabel = (defenderCanDefend ? "" : "difensore esausto, ");
    String damageLabel = describeDamageLabel(damage, highlights);
    return ", colpo " + qualifier + "a segno (" + exhaustedLabel + damageLabel + ").";
  }

  private String describeQualifier(List<TurnHighlight> highlights) {
    if (highlights.contains(TurnHighlight.PERFECT_HIT)) {
      return "perfetto (20 naturale) ";
    }
    if (highlights.contains(TurnHighlight.CRITICAL)) {
      return "critico ";
    }
    return "";
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
