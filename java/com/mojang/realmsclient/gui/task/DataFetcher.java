package com.mojang.realmsclient.gui.task;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.util.TimeSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class DataFetcher {
   static final Logger LOGGER = LogUtils.getLogger();
   final Executor executor;
   final TimeUnit resolution;
   final TimeSource timeSource;

   public DataFetcher(Executor pExecutor, TimeUnit pResolution, TimeSource pTimeSource) {
      this.executor = pExecutor;
      this.resolution = pResolution;
      this.timeSource = pTimeSource;
   }

   public <T> DataFetcher.Task<T> createTask(String pId, Callable<T> pUpdater, Duration pPeriod, RepeatedDelayStrategy pRepeatStrategy) {
      long $$4 = this.resolution.convert(pPeriod);
      if ($$4 == 0L) {
         throw new IllegalArgumentException("Period of " + pPeriod + " too short for selected resolution of " + this.resolution);
      } else {
         return new DataFetcher.Task<>(pId, pUpdater, $$4, pRepeatStrategy);
      }
   }

   public DataFetcher.Subscription createSubscription() {
      return new DataFetcher.Subscription();
   }

   @OnlyIn(Dist.CLIENT)
   static record ComputationResult<T>(Either<T, Exception> value, long time) {
   }

   @OnlyIn(Dist.CLIENT)
   class SubscribedTask<T> {
      private final DataFetcher.Task<T> task;
      private final Consumer<T> output;
      private long lastCheckTime = -1L;

      SubscribedTask(final DataFetcher.Task<T> pTask, final Consumer<T> pOutput) {
         this.task = pTask;
         this.output = pOutput;
      }

      void update(long pTime) {
         this.task.updateIfNeeded(pTime);
         this.runCallbackIfNeeded();
      }

      void runCallbackIfNeeded() {
         DataFetcher.SuccessfulComputationResult<T> $$0 = this.task.lastResult;
         if ($$0 != null && this.lastCheckTime < $$0.time) {
            this.output.accept($$0.value);
            this.lastCheckTime = $$0.time;
         }
      }

      void runCallback() {
         DataFetcher.SuccessfulComputationResult<T> $$0 = this.task.lastResult;
         if ($$0 != null) {
            this.output.accept($$0.value);
            this.lastCheckTime = $$0.time;
         }
      }

      void reset() {
         this.task.reset();
         this.lastCheckTime = -1L;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class Subscription {
      private final List<DataFetcher.SubscribedTask<?>> subscriptions = new ArrayList<>();

      public <T> void subscribe(DataFetcher.Task<T> pTask, Consumer<T> pOutput) {
         DataFetcher.SubscribedTask<T> $$2 = DataFetcher.this.new SubscribedTask<>(pTask, pOutput);
         this.subscriptions.add($$2);
         $$2.runCallbackIfNeeded();
      }

      public void forceUpdate() {
         for (DataFetcher.SubscribedTask<?> $$0 : this.subscriptions) {
            $$0.runCallback();
         }
      }

      public void tick() {
         for (DataFetcher.SubscribedTask<?> $$0 : this.subscriptions) {
            $$0.update(DataFetcher.this.timeSource.get(DataFetcher.this.resolution));
         }
      }

      public void reset() {
         for (DataFetcher.SubscribedTask<?> $$0 : this.subscriptions) {
            $$0.reset();
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static record SuccessfulComputationResult<T>(T value, long time) {
   }

   @OnlyIn(Dist.CLIENT)
   public class Task<T> {
      private final String id;
      private final Callable<T> updater;
      private final long period;
      private final RepeatedDelayStrategy repeatStrategy;
      @Nullable
      private CompletableFuture<DataFetcher.ComputationResult<T>> pendingTask;
      @Nullable
      DataFetcher.SuccessfulComputationResult<T> lastResult;
      private long nextUpdate = -1L;

      Task(final String pId, final Callable<T> pUpdater, final long pPeriod, final RepeatedDelayStrategy pRepeatStrategy) {
         this.id = pId;
         this.updater = pUpdater;
         this.period = pPeriod;
         this.repeatStrategy = pRepeatStrategy;
      }

      void updateIfNeeded(long pTime) {
         if (this.pendingTask != null) {
            DataFetcher.ComputationResult<T> $$1 = this.pendingTask.getNow(null);
            if ($$1 == null) {
               return;
            }

            this.pendingTask = null;
            long $$2 = $$1.time;
            $$1.value().ifLeft(p_239691_ -> {
               this.lastResult = new DataFetcher.SuccessfulComputationResult<>((T)p_239691_, $$2);
               this.nextUpdate = $$2 + this.period * this.repeatStrategy.delayCyclesAfterSuccess();
            }).ifRight(p_239281_ -> {
               long $$2x = this.repeatStrategy.delayCyclesAfterFailure();
               DataFetcher.LOGGER.warn("Failed to process task {}, will repeat after {} cycles", new Object[]{this.id, $$2x, p_239281_});
               this.nextUpdate = $$2 + this.period * $$2x;
            });
         }

         if (this.nextUpdate <= pTime) {
            this.pendingTask = CompletableFuture.supplyAsync(() -> {
               try {
                  T $$0 = this.updater.call();
                  long $$1x = DataFetcher.this.timeSource.get(DataFetcher.this.resolution);
                  return new DataFetcher.ComputationResult<>(Either.left($$0), $$1x);
               } catch (Exception var4x) {
                  long $$3 = DataFetcher.this.timeSource.get(DataFetcher.this.resolution);
                  return new DataFetcher.ComputationResult<>(Either.right(var4x), $$3);
               }
            }, DataFetcher.this.executor);
         }
      }

      public void reset() {
         this.pendingTask = null;
         this.lastResult = null;
         this.nextUpdate = -1L;
      }
   }
}
