package it.fantasyarena.combat.chronicle;

import java.util.List;

import it.fantasyarena.combat.ChallengerBudget;
import it.fantasyarena.combat.hero.Hero;
import it.fantasyarena.combat.hero.HeroProgress;
import it.fantasyarena.combat.hero.Loot;
import it.fantasycombatsystem.model.Fighter;
import it.fantasycombatsystem.model.IntrinsicRatings;
import it.fantasytoolkit.armourgenerator.result.ArmourResult;
import it.fantasytoolkit.buffdebuffgenerator.result.BuffElement;
import it.fantasytoolkit.charactergenerator.result.CharacterResult;
import it.fantasytoolkit.jewelgenerator.result.JewelResult;
import it.fantasytoolkit.weapongenerator.result.WeaponResult;

/**
 * Il solo punto che traduce i dati vivi del gioco nei record della cronaca: {@link Fighter} in
 * {@link CombatantSnapshot}, {@link Hero} in {@link HeroSnapshot}, {@link HeroProgress} in
 * {@link ProgressChronicle}, {@link ChallengerBudget} in {@link ChallengerBudgetChronicle}. Nessuna
 * di queste conversioni si ripete altrove.
 *
 * <p>Fotografa invece di referenziare, lo stesso rimedio già applicato da
 * {@code combat.io.render.FighterProfile} per liberare {@code BattleSceneRenderer} dalla dipendenza
 * dal {@link Fighter}: {@link Fighter#state()} è mutabile, e un riferimento diretto letto a fine
 * partita racconterebbe solo com'è finito lo scontro, non com'è andato. Le fotografie di questa
 * classe si scattano invece nell'istante in cui contano — l'ingresso per il protagonista, la fine
 * dello scontro per il combattente, il momento della crescita per la procedura di fine scontro.
 *
 * <p>Il destino del loot si legge da {@link HeroProgress#lootFate()}, non lo rideriva: sarebbe la
 * stessa logica duplicata due volte, il difetto che questa fase toglie a {@code HeroProgress}.
 */
public class ChronicleMapper {

  public CombatantSnapshot snapshotCombatant(Fighter fighter, int rosterIndex, int teamIndex) {
    CharacterResult character = fighter.character();
    IntrinsicRatings ratings = fighter.ratings();

    return new CombatantSnapshot(rosterIndex, teamIndex, fighter.name(), character.race(), character.characterClass(),
        character.characteristics(), snapshotWeapon(fighter.weapon()), snapshotArmourPieces(fighter.armourPieces()),
        ratings.maxHealth(), ratings.maxStamina(), ratings.offensiveRating(), ratings.defensiveRating());
  }

  public HeroSnapshot snapshotHero(Hero hero) {
    CharacterResult character = hero.character();
    CharacterResult effectiveCharacter = hero.effectiveCharacter();

    return new HeroSnapshot(hero.name(), character.race(), character.characterClass(), character.characteristics(),
        effectiveCharacter.characteristics(), snapshotWeapon(hero.weapon()), snapshotArmourPieces(hero.armourPieces()),
        snapshotJewels(hero.jewels()));
  }

  public ProgressChronicle snapshotProgress(HeroProgress progress) {
    ItemSnapshot found = snapshotFound(progress);
    ItemSnapshot dropped = snapshotDropped(progress);

    return new ProgressChronicle(found, progress.lootFate(), dropped, progress.characteristicGains(),
        snapshotHero(progress.grownHero()));
  }

  public ChallengerBudgetChronicle snapshotChallengerBudget(ChallengerBudget budget) {
    return new ChallengerBudgetChronicle(budget.stationPoints(), budget.luckDiscount(), budget.squadPoints());
  }

  /**
   * L'oggetto trovato, estratto in base al tipo che il destino già risolto implica. La
   * discriminazione fra arma, armatura e gioiello resta un'unica volta in
   * {@link HeroProgress#lootFate()}: qui non si ri-ispeziona quale dei tre campi di {@link Loot} è
   * valorizzato, si estrae solo quello che il caso già deciso richiede.
   */
  private ItemSnapshot snapshotFound(HeroProgress progress) {
    Loot loot = progress.loot();
    return switch (progress.lootFate()) {
      case WEAPON_TAKEN, WEAPON_DISCARDED -> snapshotWeapon(loot.weapon().orElseThrow());
      case ARMOUR_WORN_ON_EMPTY_SLOT, ARMOUR_REPLACED, ARMOUR_DISCARDED ->
          snapshotArmourPiece(loot.armourPiece().orElseThrow());
      case JEWEL_WORN_ON_EMPTY_TYPE, JEWEL_REPLACED, JEWEL_DISCARDED -> snapshotJewel(loot.jewel().orElseThrow());
    };
  }

  /**
   * L'oggetto lasciato, presente solo quando il destino già risolto è un rimpiazzo: negli altri
   * casi (scartato, o indossato su una parte prima scoperta) non c'è niente da lasciare. Lo switch
   * elenca tutti gli otto casi invece di ricorrere a un {@code default}: così, se in futuro nasce un
   * nono destino con un oggetto lasciato, il compilatore lo segnala invece di far arrivare al
   * frontend un {@code null} silenzioso.
   */
  private ItemSnapshot snapshotDropped(HeroProgress progress) {
    return switch (progress.lootFate()) {
      case WEAPON_TAKEN -> snapshotWeapon(progress.weaponSwap().orElseThrow().dropped());
      case ARMOUR_REPLACED -> snapshotArmourPiece(progress.armourUpgrade().orElseThrow().dropped());
      case JEWEL_REPLACED -> snapshotJewel(progress.jewelUpgrade().orElseThrow().dropped());
      case WEAPON_DISCARDED, ARMOUR_WORN_ON_EMPTY_SLOT, ARMOUR_DISCARDED, JEWEL_WORN_ON_EMPTY_TYPE,
          JEWEL_DISCARDED -> null;
    };
  }

  private List<ItemSnapshot> snapshotArmourPieces(List<ArmourResult> pieces) {
    return pieces.stream().map(this::snapshotArmourPiece).toList();
  }

  private List<ItemSnapshot> snapshotJewels(List<JewelResult> jewels) {
    return jewels.stream().map(this::snapshotJewel).toList();
  }

  private ItemSnapshot snapshotWeapon(WeaponResult weapon) {
    return new ItemSnapshot(ItemKind.WEAPON, weapon.weapon().name(), weapon.rarity(), weapon.attack(),
        snapshotBonuses(weapon.buffs()));
  }

  private ItemSnapshot snapshotArmourPiece(ArmourResult piece) {
    return new ItemSnapshot(ItemKind.ARMOUR, piece.armour().name(), piece.rarity(), piece.defense(),
        snapshotBonuses(piece.buffs()));
  }

  private ItemSnapshot snapshotJewel(JewelResult jewel) {
    return new ItemSnapshot(ItemKind.JEWEL, jewel.jewel().name(), jewel.rarity(), null, snapshotBonuses(jewel.buffs()));
  }

  /**
   * I buff dell'oggetto tradotti nella forma di cronaca, senza fonderli fra loro: se un oggetto
   * porta più buff sulla stessa caratteristica, la lista li elenca entrambi così com'è generato.
   */
  private List<CharacteristicBonus> snapshotBonuses(List<BuffElement> buffs) {
    return buffs.stream().map(buff -> new CharacteristicBonus(buff.characteristic(), buff.value())).toList();
  }
}
