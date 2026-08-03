package it.fantasyarena.combat.factory;

import it.fantasyarena.combat.hero.EquipmentBonus;
import it.fantasyarena.combat.hero.Hero;
import it.fantasyarena.combat.hero.Loot;
import it.fantasycombatsystem.config.CombatSettings;
import it.fantasycombatsystem.factory.FighterAssembler;
import it.fantasycombatsystem.model.Fighter;
import it.fantasytoolkit.armourgenerator.ArmourGeneratorTool;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.buffdebuffgenerator.result.BuffElement;
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
 * specchio dell'ultimo round ({@link #createMirrorRival}). Solo il protagonista nasce alla rarità
 * fissa {@link #STANDARD_EQUIPMENT_RARITY}: gli sfidanti generati ({@link #createChallengers}) e lo
 * specchio pescano invece da due {@code RarityTable} distinte decise fuori da questa classe — una
 * per l'arma, una per i pezzi d'armatura — fascia per fascia lungo il percorso (vedi
 * {@code ChallengerEquipment} in {@code combat}): questa factory esegue l'estrazione, non decide le
 * tabelle. Lo stesso vale per il numero di pezzi d'armatura indossati. Il monte punti caratteristica
 * degli sfidanti generati non è fisso allo stesso modo: lo dichiara chi chiama, stazione per
 * stazione — non più per singolo sfidante ma per l'intero schieramento, che questa factory
 * ripartisce a parti uguali col resto ai primi. Equipaggiamento e monte punti sono così due leve
 * distinte con cui il percorso dell'arena fa crescere la difficoltà.
 */
public class FighterFactory {

    private static final int TOTAL_CHARACTERISTIC_POINTS = 15;

    /**
     * Il pavimento di punti caratteristica per singolo sfidante: sotto questa soglia
     * {@code CharacterGeneratorTool.generate()} solleva una {@code IllegalStateException} opaca dal
     * toolkit, perché le {@code Characteristic} sono sette e {@code minCharacteristicValue} vale
     * {@code 1} per difetto, quindi il monte minimo distribuibile è {@code 7 * 1}. È pubblica perché
     * la userà anche il calcolo dello sconto della fortuna: questa factory è l'unico punto del gioco
     * che conosce i vincoli del toolkit.
     */
    public static final int MINIMUM_CHARACTERISTIC_POINTS_PER_CHALLENGER = 7;

    /**
     * Rarita' con cui nasce l'equipaggiamento del solo protagonista: la sua crescita passa dal loot
     * di fine livello ({@link #rollLoot}), non da questa costante. Gli sfidanti generati e lo
     * specchio non la usano piu': la loro rarita' arriva da due {@code RarityTable} distinte decise
     * fuori da questa factory (una per l'arma, una per l'armatura), fascia per fascia lungo il
     * percorso (vedi {@code ChallengerEquipment} in {@code combat}).
     */
    private static final Rarity STANDARD_EQUIPMENT_RARITY = Rarity.UNCOMMON;

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

    private void validateSquadPoints(int count, int squadCharacteristicPoints) {
        int floor = MINIMUM_CHARACTERISTIC_POINTS_PER_CHALLENGER * count;
        if (squadCharacteristicPoints < floor) {
            throw new IllegalArgumentException(
                    "squadCharacteristicPoints must be >= " + floor + " (" + MINIMUM_CHARACTERISTIC_POINTS_PER_CHALLENGER
                            + " per challenger, " + count + " challengers), was: " + squadCharacteristicPoints);
        }
    }

    /**
     * Un avversario qualunque, col monte punti richiesto e l'equipaggiamento della fascia decisa da
     * chi chiama: l'arma pesca da {@code weaponRarityTable} e i pezzi d'armatura da
     * {@code armourRarityTable}, un'estrazione indipendente per oggetto — cosi' due sfidanti della
     * stessa prova possono nascere a rarita' diverse, ed e' voluto (una sola estrazione per l'intero
     * schieramento farebbe alternare le prove tarde fra banali e impossibili). Il nome non collide
     * con quelli gia' assegnati da questa factory.
     */
    private Fighter createChallenger(int characteristicPoints, RarityTable weaponRarityTable,
            RarityTable armourRarityTable, int armourPieceCount) {
        CharacterResult character = generateUniquelyNamed(() -> generateWarrior(characteristicPoints));
        WeaponResult weapon = generateWeapon(weaponRarityTable);
        List<ArmourResult> armourPieces = generateArmourSet(armourPieceCount, armourRarityTable);
        CharacterResult effectiveCharacter = withEquipmentBonus(character, weapon, armourPieces);
        return assembler.assemble(effectiveCharacter, weapon, armourPieces, null);
    }

    /**
     * Il protagonista dell'arena: nasce alla rarita' standard {@link #STANDARD_EQUIPMENT_RARITY},
     * con un solo pezzo d'armatura, e non cresce mai come equipaggiamento di partenza — sono gli
     * avversari ({@link #createChallengers}, {@link #createMirrorRival}) a scalare intorno a lui
     * lungo il percorso. Quello che lo distinguera' se lo dovra' conquistare round dopo round col
     * loot di fine livello ({@link #rollLoot}).
     *
     * <p>Torna una {@link Hero} e non un {@code Fighter} perche' e' una scheda destinata a
     * sopravvivere agli scontri: il combattente di ogni round lo materializza {@link #summon}.
     */
    public Hero createProtagonist() {
        CharacterResult character = generateUniquelyNamed(() -> generateWarrior(TOTAL_CHARACTERISTIC_POINTS));
        WeaponResult weapon = generateWeapon(STANDARD_EQUIPMENT_RARITY);
        ArmourResult armour = generateArmour();
        return new Hero(character, weapon, List.of(armour));
    }

    /**
     * Il combattente di questo round, materializzato dalla scheda del protagonista. Nasce con vita
     * e stamina piene perche' e' un {@code Fighter} nuovo: e' cosi' che la cura di fine scontro si
     * realizza senza che nessuno debba "guarire" nessuno. I Rating tengono conto di tutti i pezzi
     * indossati, ricalcolati qui a ogni discesa in campo. Il {@link FighterAssembler} non ha un
     * overload che accetti i gioielli, quindi restano fuori dall'assemblaggio diretto: contano
     * comunque, insieme ai buff di arma e armatura, perche' le caratteristiche passate sono gia'
     * quelle {@linkplain Hero#effectiveCharacter() effettive}.
     */
    public Fighter summon(Hero hero) {
        return assembler.assemble(hero.effectiveCharacter(), hero.weapon(), hero.armourPieces(), null);
    }

    /**
     * Gli sfidanti di una prova, equipaggiati con le due tabelle che chi chiama ha scartato per
     * quella stazione ({@code weaponRarityTable} per l'arma, {@code armourRarityTable} per i pezzi,
     * {@code armourPieceCount} per quanti indossarne) e col monte punti caratteristica che la
     * stazione del percorso dichiara per l'<em>intero schieramento</em>: la difficolta' cresce
     * stazione dopo stazione spostando tutti questi numeri. Il monte si ripartisce fra gli sfidanti
     * a parti uguali, col resto ai primi: con 31 punti su due sfidanti nascono con 16 e 15.
     */
    public List<Fighter> createChallengers(int count, int squadCharacteristicPoints, RarityTable weaponRarityTable,
            RarityTable armourRarityTable, int armourPieceCount) {
        validateCount(count);
        validateSquadPoints(count, squadCharacteristicPoints);
        int basePoints = squadCharacteristicPoints / count;
        int remainder = squadCharacteristicPoints % count;
        return IntStream.range(0, count)
                .mapToObj(index -> createChallenger(basePoints + (index < remainder ? 1 : 0), weaponRarityTable,
                        armourRarityTable, armourPieceCount))
                .toList();
    }

    /**
     * Estrae dalla tabella ricevuta un grado di rarita', usando il generatore casuale di questa
     * factory. Esiste per un solo scopo dichiarato: l'arma dello sfidante speculare deve nascere un
     * gradino sopra il grado che la tabella dell'arma della sua fascia estrae altrove
     * ({@code ChallengerEquipment.oneGradeAbove}, in {@code combat}), ma questa factory non importa
     * quel tipo per non chiudere un ciclo fra {@code combat.factory} e {@code combat} (quest'ultimo
     * importa gia' {@code combat.factory} per il pavimento di
     * {@link #MINIMUM_CHARACTERISTIC_POINTS_PER_CHALLENGER}). L'estrazione resta quindi qui, dove
     * vive tutta la casualita' di generazione, e chi chiama (l'{@code Arena}) applica l'innalzamento
     * del grado, che e' un calcolo puro e non una nuova estrazione.
     */
    public Rarity drawRarity(RarityTable rarityTable) {
        return rarityTable.draw(random);
    }

    /**
     * Lo sfidante dell'ultimo round: il protagonista guardato allo specchio, con la stessa somma di
     * caratteristiche e lo stesso numero di pezzi indossati del protagonista cresciuto. I pezzi
     * d'armatura vestono {@code armourRarityTable} — la tabella dell'armatura della fascia finale,
     * non quella dell'arma — come un qualunque sfidante generato della sua stessa fascia; l'arma
     * vince un vantaggio in piu': {@code weaponRarity} arriva gia' decisa da chi chiama, un gradino
     * sopra il grado che la tabella dell'arma della stessa fascia ha estratto altrove (vedi
     * {@link #drawRarity}), mai oltre {@code LEGENDARY}.
     *
     * <p>Gli slot d'armatura sono pescati distinti fra loro (non necessariamente gli stessi del
     * protagonista): conta il numero di parti del corpo coperte, non quali.
     */
    public Fighter createMirrorRival(Hero hero, RarityTable armourRarityTable, Rarity weaponRarity) {
        CharacterResult character = generateUniquelyNamed(() -> generateRival(hero.totalCharacteristicPoints()));
        WeaponResult weapon = generateWeapon(weaponRarity);
        List<ArmourResult> armourPieces = generateArmourSet(hero.armourPieceCount(), armourRarityTable);
        CharacterResult effectiveCharacter = withEquipmentBonus(character, weapon, armourPieces);
        return assembler.assemble(effectiveCharacter, weapon, armourPieces, null);
    }

    /**
     * Le caratteristiche di un personaggio con addosso i buff della sua stessa arma e armatura:
     * la stessa somma di {@link Hero#effectiveCharacter()}, ma per un personaggio generato al volo
     * che non e' ancora (e non sara' mai, nel caso di sfidanti e specchio) incapsulato in una
     * {@link Hero}.
     */
    private CharacterResult withEquipmentBonus(CharacterResult character, WeaponResult weapon,
            List<ArmourResult> armourPieces) {
        List<BuffElement> buffs = new ArrayList<>(weapon.buffs());
        armourPieces.forEach(piece -> buffs.addAll(piece.buffs()));
        return EquipmentBonus.applyTo(character, buffs);
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

    private CharacterResult generateWarrior(int totalCharacteristicPoints) {
        return CharacterGeneratorTool.building()
                //.race(Race.HUMAN)
                .randomRace()
                .characterClass(CharacterClass.WARRIOR)
                // .addNickname()
                .allCharacteristics()
                .totalPoints(totalCharacteristicPoints)
                .generate();
    }

    private WeaponResult generateWeapon(Rarity rarity) {
        return WeaponGeneratorTool.building()
                .weapon(pickMeleeWeapon())
                .rarity(rarity)
                .generate();
    }

    /**
     * L'arma di un avversario generato: la rarita' si estrae dalla fascia della sua stazione, non e'
     * fissata a priori. Stessa forma di {@link #generateLootWeapon}, pool di mischia compreso.
     */
    private WeaponResult generateWeapon(RarityTable rarityTable) {
        return WeaponGeneratorTool.building()
                .weapon(pickMeleeWeapon())
                .rarityTable(rarityTable)
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
     * {@code pieceCount} pezzi d'armatura, ciascuno con una rarita' estratta indipendentemente da
     * {@code rarityTable}, su slot tutti diversi: due pezzi dello stesso slot non si indossano
     * insieme, quindi gli slot si pescano da una lista mescolata invece che uno a uno a caso. Piu'
     * pezzi di quanti slot esistano non si possono coprire, e il limite tronca.
     */
    private List<ArmourResult> generateArmourSet(int pieceCount, RarityTable rarityTable) {
        List<Armour> slots = new ArrayList<>(List.of(Armour.values()));
        Collections.shuffle(slots, random);

        return slots.stream()
                .limit(pieceCount)
                .map(slot -> generateArmourPiece(slot, rarityTable))
                .toList();
    }

    private ArmourResult generateArmourPiece(Armour slot, RarityTable rarityTable) {
        return ArmourGeneratorTool.building()
                .armour(slot)
                .rarityTable(rarityTable)
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
