package it.fantasyarena.combat.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * Chiede all'utente le preferenze di svolgimento del combattimento: oggi soltanto la pulizia dello
 * schermo fra un round e il successivo. Legge da {@link System#in} un byte alla volta fino al
 * newline, senza bufferizzare oltre: {@link EnterKeyTurnPacer} legge lo stesso stream più avanti,
 * fra un round e l'altro, e un reader bufferizzato qui gli sottrarrebbe l'input già consumato.
 */
public class CombatSetupPrompt {

  private static final String CLEAR_SCREEN_PROMPT = "Pulire lo schermo a ogni turno? [S/n]: ";

  private final InputStream input;

  public CombatSetupPrompt() {
    this(System.in);
  }

  public CombatSetupPrompt(InputStream input) {
    this.input = input;
  }

  /**
   * Chiede se pulire lo schermo tra un round e il successivo (default {@link ScreenRefresh#CLEAR}).
   * Non esiste un valore "non valido" da far ripetere: solo una risposta negativa esplicita
   * ({@code n}/{@code no}, senza distinguere maiuscole/minuscole) restituisce
   * {@link ScreenRefresh#SCROLL}; riga vuota, EOF o qualsiasi altro valore confermano il default,
   * senza bloccarsi e senza sollevare, così che il gioco possa girare anche non interattivo (stdin
   * chiuso).
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
