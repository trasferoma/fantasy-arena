package it.fantasyarena.combat.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Chiede all'utente le preferenze di svolgimento del combattimento: numerosità di una fazione e
 * pulizia dello schermo fra un turno/round e il successivo. Legge da {@link System#in} un byte
 * alla volta fino al newline, senza bufferizzare oltre: {@link EnterKeyTurnPacer} legge lo stesso
 * stream più avanti (nel duello 1v1) e un reader bufferizzato qui gli sottrarrebbe l'input già
 * consumato.
 */
public class CombatSetupPrompt {

  private static final int MIN_FIGHTERS = 1;
  private static final int MAX_FIGHTERS = 8;
  private static final String CLEAR_SCREEN_PROMPT = "Pulire lo schermo a ogni turno? [S/n]: ";

  private final InputStream input;

  public CombatSetupPrompt() {
    this(System.in);
  }

  public CombatSetupPrompt(InputStream input) {
    this.input = input;
  }

  /**
   * Chiede quanti combattenti compongono la fazione {@code factionLabel} (un intero fra 1 e 8).
   * Su valore non valido richiede di nuovo. Su EOF o stream non disponibile stampa una nota e
   * restituisce {@code defaultCount}, senza bloccarsi e senza sollevare: la demo deve poter
   * girare anche non interattiva (stdin chiuso).
   */
  public int askFighterCount(String factionLabel, int defaultCount) {
    while (true) {
      promptFor(factionLabel, defaultCount);

      String line = readLine();
      if (line == null) {
        System.out.println("(input non disponibile: uso il default " + defaultCount + ")");
        return defaultCount;
      }

      // Riga vuota = accetta il default: e' quello che il prompt annuncia, e rifiutare INVIO
      // dopo aver scritto "default 1" sarebbe una bugia verso l'utente.
      if (line.isBlank()) {
        return defaultCount;
      }

      Integer count = parseCount(line);
      if (isValidCount(count)) {
        return count;
      }
      System.out.println("Valore non valido, inserisci un intero fra " + MIN_FIGHTERS + " e " + MAX_FIGHTERS + ".");
    }
  }

  private void promptFor(String factionLabel, int defaultCount) {
    System.out.print("Quanti combattenti per la fazione " + factionLabel + "? [" + MIN_FIGHTERS + "-" + MAX_FIGHTERS
        + ", default " + defaultCount + "]: ");
    System.out.flush();
  }

  /**
   * Chiede se pulire lo schermo tra un turno/round e il successivo (default {@link ScreenRefresh#CLEAR}).
   * A differenza di {@link #askFighterCount}, qui non esiste un valore "non valido" da far
   * ripetere: solo una risposta negativa esplicita ({@code n}/{@code no}, senza distinguere
   * maiuscole/minuscole) restituisce {@link ScreenRefresh#SCROLL}; riga vuota, EOF o qualsiasi
   * altro valore confermano il default, senza bloccarsi e senza sollevare.
   */
  public ScreenRefresh askScreenRefresh() {
    System.out.print(CLEAR_SCREEN_PROMPT);
    System.out.flush();

    String line = readLine();
    if (line == null) {
      System.out.println("(input non disponibile: uso il default pulizia schermo)");
      return ScreenRefresh.CLEAR;
    }

    return isNegativeAnswer(line) ? ScreenRefresh.SCROLL : ScreenRefresh.CLEAR;
  }

  private boolean isNegativeAnswer(String line) {
    String normalized = line.trim().toLowerCase(Locale.ITALY);
    return normalized.equals("n") || normalized.equals("no");
  }

  private boolean isValidCount(Integer count) {
    return count != null && count >= MIN_FIGHTERS && count <= MAX_FIGHTERS;
  }

  private Integer parseCount(String line) {
    try {
      return Integer.valueOf(line.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Legge una riga da {@link #input} un byte alla volta, senza alcun buffering a monte:
   * {@code \r} viene scartato, {@code \n} chiude la riga. Restituisce {@code null} sse lo stream
   * è già a EOF (o non leggibile) prima di aver letto alcun carattere.
   */
  private String readLine() {
    StringBuilder line = new StringBuilder();
    try {
      int character = input.read();
      if (character == -1) {
        return null;
      }

      while (character != -1 && character != '\n') {
        if (character != '\r') {
          line.append((char) character);
        }
        character = input.read();
      }
      return line.toString();
    } catch (IOException e) {
      return null;
    }
  }
}
