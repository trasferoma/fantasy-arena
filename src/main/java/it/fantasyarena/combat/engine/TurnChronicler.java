package it.fantasyarena.combat.engine;

import java.util.List;

import it.fantasyarena.combat.result.TurnHighlight;

/**
 * Wording da cronaca del turno: costruisce sia il prefisso dell'attacco sia la coda descrittiva
 * del colpo. Il colpo potente e' un qualificatore dell'attacco: si manifesta SOLO nel prefisso
 * ("X tenta un colpo potente su Y con ARMA"), qualunque sia l'esito (mancato, schivato, parato o
 * a segno), cosi' da non perderlo piu' quando il difensore si difende con successo. La coda resta
 * quella specifica dell'esito e non ripete "potente". Senza highlight e senza colpo potente il
 * testo coincide esattamente con quello "piatto" prodotto oggi (nessuna regressione sui colpi
 * normali). Con highlight applica una precedenza di enfasi: il 20 naturale assorbe il wording
 * del critico ordinario, il colpo pesante è solo un rafforzativo, il colpo di grazia è la
 * chiusa dominante della frase.
 */
public final class TurnChronicler {

  /**
   * Prefisso dell'attacco (senza punteggiatura finale: la coda dell'esito la aggiunge).
   * Segnala il colpo potente qui, una volta sola, indipendentemente dal successivo esito.
   */
  public String describeAttackPrefix(String attackerName, String defenderName, String weaponName,
      boolean powerStrike) {
    if (powerStrike) {
      return attackerName + " tenta un colpo potente su " + defenderName + " con " + weaponName;
    }
    return attackerName + " attacca " + defenderName + " con " + weaponName;
  }

  public String describeOutcome(DefenseOutcome.DefenseResult result, int damage, boolean defenderCanDefend,
      List<TurnHighlight> highlights, String defenderName) {

    String phrase = describePhrase(result, damage, defenderCanDefend, highlights);
    return applyKnockout(phrase, highlights, defenderName);
  }

  /**
   * Descrizione completa del colpo mancato: riusa il prefisso condiviso con gli altri esiti.
   */
  public String describeMiss(String attackerName, String defenderName, String weaponName, boolean powerStrike) {
    return describeAttackPrefix(attackerName, defenderName, weaponName, powerStrike) + " ma manca il colpo.";
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
