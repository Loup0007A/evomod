package net.minecraft.server.packs.resources;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.Util;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ActiveProfiler;
import net.minecraft.util.profiling.ProfileResults;
import org.slf4j.Logger;

public class ProfiledReloadInstance extends SimpleReloadInstance<ProfiledReloadInstance.State> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Stopwatch total = Stopwatch.createUnstarted();

   public ProfiledReloadInstance(
      ResourceManager pResourceManager,
      List<PreparableReloadListener> pListeners,
      Executor pBackgroundExecutor,
      Executor pGameExecutor,
      CompletableFuture<Unit> pAlsoWaitedFor
   ) {
      super(pBackgroundExecutor, pGameExecutor, pResourceManager, pListeners, (p_10668_, p_10669_, p_10670_, p_10671_, p_10672_) -> {
         AtomicLong $$6 = new AtomicLong();
         AtomicLong $$7 = new AtomicLong();
         ActiveProfiler $$8 = new ActiveProfiler(Util.timeSource, () -> 0, false);
         ActiveProfiler $$9 = new ActiveProfiler(Util.timeSource, () -> 0, false);
         CompletableFuture<Void> $$10 = p_10670_.reload(p_10668_, p_10669_, $$8, $$9, p_143927_ -> p_10671_.execute(() -> {
               long $$2 = Util.getNanos();
               p_143927_.run();
               $$6.addAndGet(Util.getNanos() - $$2);
            }), p_143920_ -> p_10672_.execute(() -> {
               long $$2 = Util.getNanos();
               p_143920_.run();
               $$7.addAndGet(Util.getNanos() - $$2);
            }));
         return $$10.thenApplyAsync(p_143913_ -> {
            LOGGER.debug("Finished reloading " + p_10670_.getName());
            return new ProfiledReloadInstance.State(p_10670_.getName(), $$8.getResults(), $$9.getResults(), $$6, $$7);
         }, pGameExecutor);
      }, pAlsoWaitedFor);
      this.total.start();
      this.allDone = this.allDone.thenApplyAsync(this::finish, pGameExecutor);
   }

   private List<ProfiledReloadInstance.State> finish(List<ProfiledReloadInstance.State> p_215484_) {
      this.total.stop();
      long $$1 = 0L;
      LOGGER.info("Resource reload finished after {} ms", this.total.elapsed(TimeUnit.MILLISECONDS));

      for (ProfiledReloadInstance.State $$2 : p_215484_) {
         ProfileResults $$3 = $$2.preparationResult;
         ProfileResults $$4 = $$2.reloadResult;
         long $$5 = TimeUnit.NANOSECONDS.toMillis($$2.preparationNanos.get());
         long $$6 = TimeUnit.NANOSECONDS.toMillis($$2.reloadNanos.get());
         long $$7 = $$5 + $$6;
         String $$8 = $$2.name;
         LOGGER.info("{} took approximately {} ms ({} ms preparing, {} ms applying)", new Object[]{$$8, $$7, $$5, $$6});
         $$1 += $$6;
      }

      LOGGER.info("Total blocking time: {} ms", $$1);
      return p_215484_;
   }

   public static class State {
      final String name;
      final ProfileResults preparationResult;
      final ProfileResults reloadResult;
      final AtomicLong preparationNanos;
      final AtomicLong reloadNanos;

      State(String pName, ProfileResults pPreperationResult, ProfileResults pReloadResult, AtomicLong pPreperationNanos, AtomicLong pReloadNanos) {
         this.name = pName;
         this.preparationResult = pPreperationResult;
         this.reloadResult = pReloadResult;
         this.preparationNanos = pPreperationNanos;
         this.reloadNanos = pReloadNanos;
      }
   }
}
