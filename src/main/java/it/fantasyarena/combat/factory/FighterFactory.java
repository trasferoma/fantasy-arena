package it.fantasyarena.combat.factory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import it.fantasyarena.combat.config.CombatSettings;
import it.fantasyarena.combat.model.Fighter;
import it.fantasyarena.combat.model.IntrinsicRatings;
import it.fantasyarena.combat.rating.DefaultRatingStrategy;
import it.fantasyarena.combat.rating.RatingStrategy;
import it.fantasytoolkit.armourgenerator.ArmourGeneratorTool;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.CharacterGeneratorTool;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.weapongenerator.WeaponGeneratorTool;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.CharacterClass;
import it.fantasytoolkitcore.core.model.Race;
import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.Weapon;

/**
 * Costruisce i {@link Fighter} a partire dai generatori del toolkit, applicando la
 * {@link RatingStrategy} per calcolare i Rating intrinseci. Nessuno scudo in v1.
 */
public class FighterFactory {

    private static final int TOTAL_CHARACTERISTIC_POINTS = 15;

    /**
     * Tetto di tentativi di rigenerazione onesta (nuovo personaggio, quindi nuova razza e nuovo
     * nome) prima di ricorrere al suffisso distintivo. Vedi {@link #createUniquelyNamedSwordWarrior}.
     */
    private static final int MAX_NAME_COLLISION_ATTEMPTS = 5;

    private final RatingStrategy ratingStrategy;
    private final Random random = new Random();

    public FighterFactory(RatingStrategy ratingStrategy) {
        this.ratingStrategy = ratingStrategy;
    }

    /**
     * Crea una factory che calcola i Rating intrinseci con la strategia di default,
     * tarata sugli stessi {@link CombatSettings} usati poi dall'Arena per il combattimento.
     */
    public static FighterFactory withDefaultRatings(CombatSettings settings) {
        return new FighterFactory(new DefaultRatingStrategy(settings));
    }

    /**
     * Crea due combattenti equi-equipaggiati: la rarita' dell'arma e quella dell'armatura
     * vengono estratte una sola volta e condivise da entrambi, cosi' che nessuno dei due
     * parta con un vantaggio di equipaggiamento sull'altro.
     */
    public Duelists createMatchedSwordWarriors() {
        Rarity weaponRarity = Rarity.UNCOMMON; // randomRarity();
        Rarity armourRarity = Rarity.UNCOMMON; // randomRarity();
        Fighter first = createSwordWarrior(weaponRarity, armourRarity);
        Fighter second = createSwordWarrior(weaponRarity, armourRarity);
        return new Duelists(first, second);
    }

    /**
     * Crea {@code count} combattenti equi-equipaggiati per una battaglia NvN: rarita' dell'arma
     * e dell'armatura estratte una sola volta e condivise da tutti (stessa logica di
     * {@link #createMatchedSwordWarriors()}), cosi' che nessuno parta avvantaggiato. I nomi dei
     * combattenti restituiti sono garantiti univoci fra loro: vedi
     * {@link #createUniquelyNamedSwordWarrior} per come viene risolta un'eventuale collisione.
     */
    public List<Fighter> createMatchedSwordWarriors(int count) {
        validateCount(count);
        Rarity weaponRarity = Rarity.UNCOMMON; // randomRarity();
        Rarity armourRarity = Rarity.UNCOMMON; // randomRarity();

        List<Fighter> fighters = new ArrayList<>(count);
        Set<String> usedNames = new HashSet<>();
        for (int i = 0; i < count; i++) {
            fighters.add(createUniquelyNamedSwordWarrior(weaponRarity, armourRarity, usedNames));
        }
        return fighters;
    }

    private void validateCount(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("count must be >= 1, was: " + count);
        }
    }

    /**
     * Crea un guerriero il cui nome non collida con quelli gia' assegnati in {@code usedNames}.
     * Il generatore del toolkit pesca il nome da una lista per razza (vedi
     * {@code character-generator.md}): con piu' guerrieri la collisione e' plausibile e qui
     * l'ambiguita' sarebbe reale (il log identifica i combattenti per nome). Si tenta prima la
     * via "onesta" (rigenerare l'intero personaggio, che ripesca razza, caratteristiche e nome),
     * fino a {@link #MAX_NAME_COLLISION_ATTEMPTS} tentativi; se il nome resta occupato anche
     * cosi', si ricostruisce il {@code CharacterResult} con lo stesso identico personaggio ma un
     * suffisso numerico distintivo nel nome, usando il costruttore del record (il toolkit non
     * espone alcuna API di rinomina).
     */
    private Fighter createUniquelyNamedSwordWarrior(Rarity weaponRarity, Rarity armourRarity, Set<String> usedNames) {
        CharacterResult character = generateWarrior();
        for (int attempt = 1; attempt < MAX_NAME_COLLISION_ATTEMPTS && usedNames.contains(character.name()); attempt++) {
            character = generateWarrior();
        }
        if (usedNames.contains(character.name())) {
            character = withDisambiguatedName(character, usedNames.size() + 1);
        }
        usedNames.add(character.name());

        WeaponResult weapon = generateSword(weaponRarity);
        ArmourResult armour = generateChestplate(armourRarity);
        IntrinsicRatings ratings = ratingStrategy.computeRatings(character, weapon, armour, null);
        return new Fighter(character, weapon, armour, null, ratings);
    }

    private CharacterResult withDisambiguatedName(CharacterResult character, int disambiguator) {
        String disambiguatedName = character.name() + " (" + disambiguator + ")";
        return new CharacterResult(character.race(), character.characterClass(), disambiguatedName,
                character.characteristics());
    }

    /**
     * Crea un guerriero con spada e corazza della rarita' indicata.
     */
    public Fighter createSwordWarrior(Rarity weaponRarity, Rarity armourRarity) {
        CharacterResult character = generateWarrior();
        WeaponResult weapon = generateSword(weaponRarity);
        ArmourResult armour = generateChestplate(armourRarity);
        IntrinsicRatings ratings = ratingStrategy.computeRatings(character, weapon, armour, null);
        return new Fighter(character, weapon, armour, null, ratings);
    }

    private Rarity randomRarity() {
        Rarity[] rarities = Rarity.values();
        return rarities[random.nextInt(rarities.length)];
    }

    private CharacterResult generateWarrior() {
        return CharacterGeneratorTool.building()
                //.race(Race.HUMAN)
                .randomRace()
                .characterClass(CharacterClass.WARRIOR)
                // .addNickname()
                .allCharacteristics()
                .totalPoints(TOTAL_CHARACTERISTIC_POINTS)
                .generate();
    }

    private WeaponResult generateSword(Rarity rarity) {
        return WeaponGeneratorTool.building()
                .weapon(Weapon.SWORD)
                .rarity(rarity)
                .noStatusEffect()
                .generate();
    }

    private ArmourResult generateChestplate(Rarity rarity) {
        return ArmourGeneratorTool.building()
                .armour(Armour.CHESTPLATE)
                .rarity(rarity)
                .noStatusEffect()
                .generate();
    }

    /**
     * Coppia di combattenti equi-equipaggiati, pronti per disputare il duello.
     */
    public record Duelists(Fighter first, Fighter second) {
    }
}
