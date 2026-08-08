package it.fantasyarena.combat.io.trace;

import java.util.function.Supplier;

import it.fantasyarena.combat.chronicle.ArenaChronicle;

/**
 * Decora un fornitore di cronache aggiungendo, dopo ogni corsa, la registrazione del log analitico:
 * chi consuma questo {@link Supplier} vede solo una cronaca in arrivo, non sa che dietro c'è anche
 * un {@link TraceRecorder}.
 *
 * <p>È la forma scelta per agganciare il tracciatore alla modalità web restando fedeli al vincolo di
 * {@code CLAUDE.md}: il server web riceve un {@code Supplier<ArenaChronicle>} e nient'altro, quindi
 * il tracciatore non si inietta al suo interno ma si compone <em>attorno</em> al fornitore, prima
 * che il server lo riceva. Solo {@code it.fantasyarena.UiMode} — l'unico punto che conosce sia il
 * server web sia questo package — costruisce questa decorazione: {@code combat.io.trace} non
 * importa {@code combat.io.web} e non sa che esiste, esattamente come nessuno degli altri
 * sotto-package di {@code combat.io} lo sa.
 *
 * <p>La politica di errore della scrittura resta interamente in {@link TraceFileWriter}: un guasto
 * non si propaga mai fin qui, quindi {@link #get()} non ha bisogno di un proprio {@code try/catch}.
 */
public class TracedChronicleSupplier implements Supplier<ArenaChronicle> {

  private final Supplier<ArenaChronicle> delegate;
  private final TraceRecorder recorder;

  public TracedChronicleSupplier(Supplier<ArenaChronicle> delegate, TraceRecorder recorder) {
    this.delegate = delegate;
    this.recorder = recorder;
  }

  @Override
  public ArenaChronicle get() {
    ArenaChronicle chronicle = delegate.get();
    recorder.record(chronicle);
    return chronicle;
  }
}
