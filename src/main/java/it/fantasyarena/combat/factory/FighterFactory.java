package it.fantasyarena.combat.factory;

import it.fantasyarena.combat.hero.Hero;
import it.fantasyarena.combat.hero.Loot;
import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.factory.FighterAssembler;
import it.fantasycombatsystem.model.Fighter;
import it.fantasytoolkit.armourgenerator.ArmourGeneratorTool;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.charactergenerator.CharacterGeneratorTool;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.dicelauncher.DiceLauncherTool;
import it.fantasytoolkit.dicelauncher.result.DiceRollResult;
import it.fantasytoolkit.jewelgenerator.JewelGeneratorTool;
import it.fantasytoolkit.jewelgenerator.result.JewelResult;
import it.fantasytoolkit.weapongenerator.WeaponGeneratorTool;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;
import it.fantasytoolkitcore.core.model.Armour;
import it.fantasytoolkitcore.core.model.CharacterClass;
import it.fantasytoolkitcore.core.model.ClassBonusTable;
import it.fantasytoolkitcore.core.model.RaceBonusTable;
import it.fantasytoolkitcore.core.model.Rarity;
import it.fantasytoolkitcore.core.model.RarityTable;
import it.fantasytoolkitcore.core.model.Weapon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.IntStream;

/**
 * Genera i combattenti del gioco: decide <em>chi</em> scende nell'arena e con <em>quale</em>
 * equipaggiamento, pescando dai generatori del toolkit, e affida al {@link FighterAssembler} del
 * motore la traduzione in {@link Fighter} con i Rating intrinseci calcolati. Nessuno scudo in v1.
 *
 * <p>È l'unico punto di contatto del gioco coi generatori del toolkit: la casualità della
 * generazione (razza, nome, caratteristiche, rarità, e ora anche il tipo di loot) vive qui, non nel
 * motore, che i combattenti li riceve già formati.
 *
 * <p>Produce la {@link Hero} che sopravvive ai round e che a ogni round torna in campo come
 * combattente nuovo ({@link #summon}), più gli sfidanti che le si parano davanti — fino allo
 * specchio dell'ultimo round ({@link #createMirrorRival}). Tutti nascono equi-equipaggiati alla
 * stessa {@link #STANDARD_EQUIPMENT_RARITY}: le differenze se le conquista il protagonista dal
 * loot di fine livello ({@link #rollLoot}), non gliele regala la generazione.
 */
public class FighterFactory {

    private static final int TOTAL_CHARACTERISTIC_POINTS = 15;

    /**
     * Rarita' con cui nasce l'equipaggiamento di chiunque scenda nell'arena, protagonista compreso:
     * si parte tutti alla pari, e le differenze arrivano dopo, dal loot di fine livello.
     */
    private static final Rarity STANDARD_EQUIPMENT_RARITY = Rarity.UNCOMMON;

    /**
     * L'unico vantaggio dichiarato dello sfidante speculare dell'ultimo round: l'arma. Le
     * caratteristiche e il numero di pezzi indossati li ricalca dal protagonista.
     */
    private static final Rarity MIRROR_RIVAL_WEAPON_RARITY = Rarity.RARE;

    /**
     * Tetto di tentativi di rigenerazione onesta (nuovo personaggio, quindi nuova razza e nuovo
     * nome) prima di ricorrere al suffisso distintivo. Vedi {@link #generateUniquelyNamed}.
     */
    private static final int MAX_NAME_COLLISION_ATTEMPTS = 5;

    private final FighterAssembler assembler;
    private final Random random = new Random();

    /**
     * I nomi gia' assegnati da questa factory, per l'intera sua vita e non solo dentro una singola
     * chiamata: nell'arena i combattenti nascono un round alla volta, e il log li identifica per
     * nome. Vedi {@link #generateUniquelyNamed}.
     */
    private final Set<String> usedNames = new HashSet<>();

    public FighterFactory(FighterAssembler assembler) {
        this.assembler = assembler;
    }

    /**
     * Crea una factory i cui combattenti hanno i Rating intrinseci calcolati dalla strategia di
     * default del motore, tarata sugli stessi {@link CombatSettings} che l'arena userà poi per il
     * combattimento: usarne due diversi produrrebbe uno scontro incoerente, perché i Rating non
     * vengono ricalcolati durante la battaglia.
     */
    public static FighterFactory withDefaultRatings(CombatSettings settings) {
        return new FighterFactory(FighterAssembler.withDefaultRatings(settings));
    }

    private void validateCount(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("count must be >= 1, was: " + count);
        }
    }

    /**
     * Un avversario qualunque: arma e armatura alla rarita' standard, un pezzo solo addosso, e un
     * nome che non collide con quelli gia' assegnati da questa factory.
     */
    private Fighter createChallenger() {
        CharacterResult character = generateUniquelyNamed(this::generateWarrior);
        WeaponResult weapon = generateWeapon(STANDARD_EQUIPMENT_RARITY);
        ArmourResult armour = generateArmour();
        return assembler.assemble(character, weapon, armour);
    }

    /**
     * Il protagonista dell'arena, che parte come chiunque altro: stessi punti caratteristica,
     * stessa rarita' d'equipaggiamento, un pezzo d'armatura solo. Quello che lo distinguera' se lo
     * dovra' conquistare round dopo round.
     *
     * <p>Torna una {@link Hero} e non un {@code Fighter} perche' e' una scheda destinata a
     * sopravvivere agli scontri: il combattente di ogni round lo materializza {@link #summon}.
     */
    public Hero createProtagonist() {
        CharacterResult character = generateUniquelyNamed(this::generateWarrior);
        WeaponResult weapon = generateWeapon(STANDARD_EQUIPMENT_RARITY);
        ArmourResult armour = generateArmour();
        return new Hero(character, weapon, List.of(armour));
    }

    /**
     * Il combattente di questo round, materializzato dalla scheda del protagonista. Nasce con vita
     * e stamina piene perche' e' un {@code Fighter} nuovo: e' cosi' che la cura di fine scontro si
     * realizza senza che nessuno debba "guarire" nessuno. I Rating tengono conto di tutti i pezzi
     * indossati, ricalcolati qui a ogni discesa in campo. I gioielli indossati non entrano in
     * questa chiamata: il {@link FighterAssembler} non ha un overload che li accetti, e restano
     * fuori dallo scontro anche se la {@link Hero} li custodisce.
     */
    public Fighter summon(Hero hero) {
        return assembler.assemble(hero.character(), hero.weapon(), hero.armourPieces(), null);
    }

    /**
     * Gli sfidanti di un round, equipaggiati come chiunque altro nell'arena.
     */
    public List<Fighter> createChallengers(int count) {
        validateCount(count);
        return IntStream.range(0, count)
                .mapToObj(challenger -> createChallenger())
                .toList();
    }

    /**
     * Lo sfidante dell'ultimo round: il protagonista guardato allo specchio, con la stessa somma di
     * caratteristiche e lo stesso numero di pezzi indossati, ma con un'arma {@code RARE} in pugno.
     * L'unico divario dichiarato e' quello, ed e' il senso della prova finale.
     *
     * <p>Gli slot d'armatura sono pescati distinti fra loro (non necessariamente gli stessi del
     * protagonista): conta il numero di parti del corpo coperte, non quali.
     */
    public Fighter createMirrorRival(Hero hero) {
        CharacterResult character = generateUniquelyNamed(() -> generateRival(hero.totalCharacteristicPoints()));
        WeaponResult weapon = generateWeapon(MIRROR_RIVAL_WEAPON_RARITY);
        List<ArmourResult> armourPieces = generateArmourSet(hero.armourPieceCount());
        return assembler.assemble(character, weapon, armourPieces, null);
    }

    /**
     * Il loot di fine livello: un tipo estratto a caso fra arma, armatura e gioiello, generato con
     * la rarita' pescata dalla {@code RarityTable} decisa dal {@code HeroBrain} per quel livello.
     * Qui vive solo l'estrazione e la generazione: se valga la pena tenere l'oggetto lo decide il
     * cervello, non questa factory.
     */
    public Loot rollLoot(RarityTable rarityTable) {
        LootKind kind = LootKind.values()[random.nextInt(LootKind.values().length)];
        return switch (kind) {
            case WEAPON -> Loot.ofWeapon(generateLootWeapon(rarityTable));
            case ARMOUR -> Loot.ofArmourPiece(generateLootArmour(rarityTable));
            case JEWEL -> Loot.ofJewel(generateJewel(rarityTable));
        };
    }

    /**
     * Genera un personaggio col nome che non collida con quelli gia' assegnati. Il generatore del
     * toolkit pesca il nome da una lista per razza (vedi {@code character-generator.md}): con piu'
     * guerrieri la collisione e' plausibile e l'ambiguita' sarebbe reale, perche' il log identifica
     * i combattenti per nome. Si tenta prima la via "onesta" (rigenerare l'intero personaggio, che
     * ripesca razza, caratteristiche e nome), fino a {@link #MAX_NAME_COLLISION_ATTEMPTS}
     * tentativi; se il nome resta occupato anche cosi', si ricostruisce il {@code CharacterResult}
     * con lo stesso identico personaggio ma un suffisso numerico distintivo nel nome, usando il
     * costruttore del record (il toolkit non espone alcuna API di rinomina).
     */
    private CharacterResult generateUniquelyNamed(Supplier<CharacterResult> generator) {
        CharacterResult character = generator.get();
        for (int attempt = 1; attempt < MAX_NAME_COLLISION_ATTEMPTS && usedNames.contains(character.name()); attempt++) {
            character = generator.get();
        }
        if (usedNames.contains(character.name())) {
            character = withDisambiguatedName(character, usedNames.size() + 1);
        }
        usedNames.add(character.name());
        return character;
    }

    private CharacterResult withDisambiguatedName(CharacterResult character, int disambiguator) {
        String disambiguatedName = character.name() + " (" + disambiguator + ")";
        return new CharacterResult(character.race(), character.characterClass(), disambiguatedName,
                character.characteristics());
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

    private WeaponResult generateWeapon(Rarity rarity) {
        return WeaponGeneratorTool.building()
                .weapon(pickMeleeWeapon())
                .rarity(rarity)
                .noStatusEffect()
                .generate();
    }

    /**
     * L'arma di loot pesca dallo stesso pool ristretto di mischia dell'equipaggiamento di partenza:
     * il toolkit contiene anche armi a distanza e bastoni che nell'arena non ci sono mai stati, e il
     * loot non deve introdurli di soppiatto.
     */
    private WeaponResult generateLootWeapon(RarityTable rarityTable) {
        return WeaponGeneratorTool.building()
                .weapon(pickMeleeWeapon())
                .rarityTable(rarityTable)
                .noStatusEffect()
                .generate();
    }

    /**
     * Il pool di mischia da cui pescano sia l'equipaggiamento di partenza sia il loot: il toolkit
     * genera anche armi a distanza e bastoni, deliberatamente esclusi dall'arena.
     */
    private Weapon pickMeleeWeapon() {
        List<Weapon> weapons = List.of(Weapon.SWORD, Weapon.AXE, Weapon.BATTLEAXE, Weapon.DAGGER, Weapon.HAMMER);
        DiceRollResult roll = DiceLauncherTool.building()
                .dice(1, 5)
                .roll();

        return weapons.get(roll.total() - 1);
    }

    /**
     * Un pezzo d'armatura su uno slot qualunque, alla rarita' standard: e' l'unico pezzo con cui
     * chiunque scende nell'arena la prima volta.
     */
    private ArmourResult generateArmour() {
        return ArmourGeneratorTool.building()
                //.armour(Armour.CHESTPLATE)
                .randomArmour()
                .rarity(STANDARD_EQUIPMENT_RARITY)
                .noStatusEffect()
                .generate();
    }

    /**
     * Il pezzo d'armatura di loot: slot casuale come l'equipaggiamento di partenza, ma con la
     * rarita' pescata dalla tabella del livello al posto di quella standard.
     */
    private ArmourResult generateLootArmour(RarityTable rarityTable) {
        return ArmourGeneratorTool.building()
                .randomArmour()
                .rarityTable(rarityTable)
                .noStatusEffect()
                .generate();
    }

    /**
     * Il gioiello di loot: non ha equivalente nell'equipaggiamento di partenza, perche' il gioiello
     * non e' mai stato equipaggiamento nell'arena prima del loot.
     */
    private JewelResult generateJewel(RarityTable rarityTable) {
        return JewelGeneratorTool.building()
                .randomJewel()
                .rarityTable(rarityTable)
                .noStatusEffect()
                .generate();
    }

    /**
     * Un personaggio che somma esattamente {@code totalCharacteristicPoints}. I bonus di razza e
     * classe sono disattivati con due tabelle vuote (l'opt-out previsto dal toolkit): lasciandoli
     * attivi si sommerebbero al monte punti richiesto, e lo sfidante "pari" nascerebbe
     * sistematicamente piu' forte del protagonista che dovrebbe eguagliare.
     */
    private CharacterResult generateRival(int totalCharacteristicPoints) {
        return CharacterGeneratorTool.building()
                .randomRace()
                .characterClass(CharacterClass.WARRIOR)
                .allCharacteristics()
                .totalPoints(totalCharacteristicPoints)
                .raceBonusTable(RaceBonusTable.builder().build())
                .classBonusTable(ClassBonusTable.builder().build())
                .generate();
    }

    /**
     * {@code pieceCount} pezzi d'armatura alla rarita' standard, su slot tutti diversi: due pezzi
     * dello stesso slot non si indossano insieme, quindi gli slot si pescano da una lista mescolata
     * invece che uno a uno a caso. Piu' pezzi di quanti slot esistano non si possono coprire, e il
     * limite tronca.
     */
    private List<ArmourResult> generateArmourSet(int pieceCount) {
        List<Armour> slots = new ArrayList<>(List.of(Armour.values()));
        Collections.shuffle(slots, random);

        return slots.stream()
                .limit(pieceCount)
                .map(this::generateArmourPiece)
                .toList();
    }

    private ArmourResult generateArmourPiece(Armour slot) {
        return ArmourGeneratorTool.building()
                .armour(slot)
                .rarity(STANDARD_EQUIPMENT_RARITY)
                .noStatusEffect()
                .generate();
    }

    /**
     * I tre tipi fra cui si estrae il loot di fine livello.
     */
    private enum LootKind {
        WEAPON,
        ARMOUR,
        JEWEL
    }

}
