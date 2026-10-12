package net.minecraft.server.packs.resources;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.Util;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.InactiveProfiler;

public class SimpleReloadInstance<S> implements ReloadInstance {
   private static final int PREPARATION_PROGRESS_WEIGHT = 2;
   private static final int EXTRA_RELOAD_PROGRESS_WEIGHT = 2;
   private static final int LISTENER_PROGRESS_WEIGHT = 1;
   protected final CompletableFuture<Unit> allPreparations = new CompletableFuture<>();
   protected CompletableFuture<List<S>> allDone;
   final Set<PreparableReloadListener> preparingListeners;
   private final int listenerCount;
   private int startedReloads;
   private int finishedReloads;
   private final AtomicInteger startedTaskCounter = new AtomicInteger();
   private final AtomicInteger doneTaskCounter = new AtomicInteger();

   public static SimpleReloadInstance<Void> of(
      ResourceManager pResourceManager,
      List<PreparableReloadListener> pListeners,
      Executor pBackgroundExecutor,
      Executor pGameExecutor,
      CompletableFuture<Unit> pAlsoWaitedFor
   ) {
      return new SimpleReloadInstance<>(
         pBackgroundExecutor,
         pGameExecutor,
         pResourceManager,
         pListeners,
         (p_10829_, p_10830_, p_10831_, p_10832_, p_10833_) -> p_10831_.reload(
               p_10829_, p_10830_, InactiveProfiler.INSTANCE, InactiveProfiler.INSTANCE, pBackgroundExecutor, p_10833_
            ),
         pAlsoWaitedFor
      );
   }

   protected SimpleReloadInstance(
      Executor pBackgroundExecutor,
      final Executor pGameExecutor,
      ResourceManager pResourceManager,
      List<PreparableReloadListener> pListeners,
      SimpleReloadInstance.StateFactory<S> pStateFactory,
      CompletableFuture<Unit> pAlsoWaitedFor
   ) {
      this.listenerCount = pListeners.size();
      this.startedTaskCounter.incrementAndGet();
      pAlsoWaitedFor.thenRun(this.doneTaskCounter::incrementAndGet);
      List<CompletableFuture<S>> $$6 = Lists.newArrayList();
      CompletableFuture<?> $$7 = pAlsoWaitedFor;
      this.preparingListeners = Sets.newHashSet(pListeners);

      for (final PreparableReloadListener $$8 : pListeners) {
         final CompletableFuture<?> $$9 = $$7;
         CompletableFuture<S> $$10 = pStateFactory.create(new PreparableReloadListener.PreparationBarrier() {
            @Override
            public <T> CompletableFuture<T> wait(T p_10858_) {
               pGameExecutor.execute(() -> {
                  SimpleReloadInstance.this.preparingListeners.remove($$8);
                  if (SimpleReloadInstance.this.preparingListeners.isEmpty()) {
                     SimpleReloadInstance.this.allPreparations.complete(Unit.INSTANCE);
                  }
               });
               return SimpleReloadInstance.this.allPreparations.thenCombine((CompletionStage<? extends T>)$$9, (p_10861_, p_10862_) -> p_10858_);
            }
         }, pResourceManager, $$8, p_10842_ -> {
            this.startedTaskCounter.incrementAndGet();
            pBackgroundExecutor.execute(() -> {
               p_10842_.run();
               this.doneTaskCounter.incrementAndGet();
            });
         }, p_10836_ -> {
            this.startedReloads++;
            pGameExecutor.execute(() -> {
               p_10836_.run();
               this.finishedReloads++;
            });
         });
         $$6.add($$10);
         $$7 = $$10;
      }

      this.allDone = Util.sequenceFailFast($$6);
   }

   @Override
   public CompletableFuture<?> done() {
      return this.allDone;
   }

   @Override
   public float getActualProgress() {
      int $$0 = this.listenerCount - this.preparingListeners.size();
      float $$1 = (float)(this.doneTaskCounter.get() * 2 + this.finishedReloads * 2 + $$0 * 1);
      float $$2 = (float)(this.startedTaskCounter.get() * 2 + this.startedReloads * 2 + this.listenerCount * 1);
      return $$1 / $$2;
   }

   public static ReloadInstance create(
      ResourceManager pResourceManager,
      List<PreparableReloadListener> pListeners,
      Executor pBackgroundExecutor,
      Executor pGameExecutor,
      CompletableFuture<Unit> pAlsoWaitedFor,
      boolean pProfiled
   ) {
      return (ReloadInstance)(pProfiled
         ? new ProfiledReloadInstance(pResourceManager, pListeners, pBackgroundExecutor, pGameExecutor, pAlsoWaitedFor)
         : of(pResourceManager, pListeners, pBackgroundExecutor, pGameExecutor, pAlsoWaitedFor));
   }

   protected interface StateFactory<S> {
      CompletableFuture<S> create(
         PreparableReloadListener.PreparationBarrier pPreperationBarrier,
         ResourceManager pResourceManager,
         PreparableReloadListener pListener,
         Executor pBackgroundExecutor,
         Executor pGameExecutor
      );
   }
}
