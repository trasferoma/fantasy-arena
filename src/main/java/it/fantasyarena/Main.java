package it.fantasyarena;

import java.util.List;

import it.fantasytoolkit.charactergenerator.CharacterGeneratorTool;
import it.fantasytoolkit.charactergenerator.result.CharacterCharacteristic;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.dicelauncher.DiceLauncherTool;
import it.fantasytoolkit.dicelauncher.result.DiceRoll;
import it.fantasytoolkit.dicelauncher.result.DiceRollResult;

/**
 * Esempio minimale di consumo della libreria fantasy-game-toolkit:
 * genera un personaggio casuale e tira un gruppo di dadi.
 */
public class Main {

    public static void main(String[] args) {
        printCharacter(generateHero());
        printDiceRoll(rollInitiative());
    }

    private static CharacterResult generateHero() {
        return CharacterGeneratorTool.building()
                .randomRace()
                .randomClass()
                .addNickname()
                .allCharacteristics()
                .totalPoints(50)
                .generate();
    }

    private static DiceRollResult rollInitiative() {
        return DiceLauncherTool.building()
                .dice(2, 6)
                .dice(1, 20, "initiative")
                .roll();
    }

    private static void printCharacter(CharacterResult character) {
        System.out.println("=== Eroe generato ===");
        System.out.println("Nome:   " + character.name());
        System.out.println("Razza:  " + character.race());
        System.out.println("Classe: " + character.characterClass());

        List<CharacterCharacteristic> characteristics = character.characteristics();
        for (CharacterCharacteristic characteristic : characteristics) {
            System.out.println("  " + characteristic.characteristic() + ": " + characteristic.value());
        }
    }

    private static void printDiceRoll(DiceRollResult roll) {
        System.out.println();
        System.out.println("=== Lancio dadi ===");

        List<DiceRoll> groups = roll.rolls();
        for (DiceRoll group : groups) {
            String label = group.code() != null ? group.code() : (group.numberOfDice() + "d" + group.numberOfFaces());
            System.out.println("  " + label + " " + group.results() + " -> " + group.subtotal());
        }

        System.out.println("Totale: " + roll.total());
    }
}
