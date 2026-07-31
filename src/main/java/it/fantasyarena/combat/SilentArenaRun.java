package it.fantasyarena.combat;

import java.util.function.Supplier;

import it.fantasyarena.combat.chronicle.ArenaChronicle;
import it.fantasyarena.combat.factory.FighterFactory;
import it.fantasyarena.combat.hero.HeroBrain;
import it.fantasyarena.combat.io.log.SilentArenaLogger;
import it.fantasyarena.combat.io.replay.SilentMatchPresentation;
import it.fantasyarena.combat.io.terminal.TurnPacer;
import it.fantasycombatsystem.config.CombatSettings;

/**
 * Assembla un'{@link Arena} muta e la fa giocare: nessun byte su {@code System.out}, nessuna
 * lettura da {@code System.in}. Passa dal costruttore con collaboratori espliciti che {@link Arena}
 * già espone — non è una porta d'ingresso nuova, è una delle combinazioni possibili di quella
 * stessa porta: logger muto ({@link SilentArenaLogger}), presentazione muta
 * ({@link SilentMatchPresentation}) e un {@link TurnPacer#none()} che non attende.
 *
 * <p>Vive in {@code it.fantasyarena.combat} e non altrove, per due motivi distinti. Non sta in
 * {@code combat.chronicle}, che è un package di soli dati e non deve dipendere da {@code combat.io}
 * in nessuna direzione: l'assemblaggio muto invece dipende proprio da {@code combat.io.log},
 * {@code combat.io.replay} e {@code combat.io.terminal}, quindi in {@code chronicle} romperebbe
 * quel vincolo. Non sta nemmeno in {@code combat.io.web}, dichiarato un pozzo che dipende solo dai
 * dati della cronaca e da Jackson e che non importa {@code render}, {@code log}, {@code terminal}
 * né {@code replay}: se l'assemblaggio muto vivesse là, quel vincolo cadrebbe alla prima riga.
 * {@code it.fantasyarena.combat} — dove vive anche {@link Arena} — è l'unico posto che può
 * conoscere entrambi i mondi.
 *
 * <p>Implementa {@link Supplier} pensando a chi la userà: un gestore HTTP che deve invocarla una
 * volta per richiesta, ottenendo ogni volta una partita nuova. Questa classe non custodisce alcun
 * collaboratore fra una chiamata e l'altra: tiene solo i {@link CombatSettings}, e {@link #get()}
 * costruisce ogni volta da zero fabbrica, cervello, i due {@link MatchRunner} muti, il logger muto
 * e il pacer, prima di assemblare l'{@link Arena}. È l'unico modo che questa classe abbia un solo
 * contratto onesto: se i collaboratori fossero campi costruiti una volta, la stessa
 * {@link FighterFactory} — col suo stato interno dei nomi già assegnati — finirebbe condivisa da
 * tutte le partite prodotte da un'istanza, che è esattamente ciò che due richieste concorrenti non
 * devono contendersi.
 */
public class SilentArenaRun implements Supplier<ArenaChronicle> {

  private final CombatSettings settings;

  public SilentArenaRun(CombatSettings settings) {
    this.settings = settings;
  }

  @Override
  public ArenaChronicle get() {
    FighterFactory fighterFactory = FighterFactory.withDefaultRatings(settings);
    HeroBrain heroBrain = new HeroBrain();
    MatchRunner battleRunner = new MatchRunner(settings, new SilentMatchPresentation());
    MatchRunner duelRunner = new MatchRunner(settings, new SilentMatchPresentation());

    return new Arena(fighterFactory, heroBrain, battleRunner, duelRunner, new SilentArenaLogger(), TurnPacer.none())
        .run();
  }
}
