package net.minecraft.world.ticks;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class LevelTicks<T> implements LevelTickAccess<T> {
   private static final Comparator<LevelChunkTicks<?>> CONTAINER_DRAIN_ORDER = (p_193246_, p_193247_) -> ScheduledTick.INTRA_TICK_DRAIN_ORDER
         .compare(p_193246_.peek(), p_193247_.peek());
   private final LongPredicate tickCheck;
   private final Supplier<ProfilerFiller> profiler;
   private final Long2ObjectMap<LevelChunkTicks<T>> allContainers = new Long2ObjectOpenHashMap();
   private final Long2LongMap nextTickForContainer = Util.make(new Long2LongOpenHashMap(), p_193262_ -> p_193262_.defaultReturnValue(Long.MAX_VALUE));
   private final Queue<LevelChunkTicks<T>> containersToTick = new PriorityQueue<>(CONTAINER_DRAIN_ORDER);
   private final Queue<ScheduledTick<T>> toRunThisTick = new ArrayDeque<>();
   private final List<ScheduledTick<T>> alreadyRunThisTick = new ArrayList<>();
   private final Set<ScheduledTick<?>> toRunThisTickSet = new ObjectOpenCustomHashSet(ScheduledTick.UNIQUE_TICK_HASH);
   private final BiConsumer<LevelChunkTicks<T>, ScheduledTick<T>> chunkScheduleUpdater = (p_193249_, p_193250_) -> {
      if (p_193250_.equals(p_193249_.peek())) {
         this.updateContainerScheduling(p_193250_);
      }
   };

   public LevelTicks(LongPredicate pTickCheck, Supplier<ProfilerFiller> pProfiler) {
      this.tickCheck = pTickCheck;
      this.profiler = pProfiler;
   }

   public void addContainer(ChunkPos pChunkPos, LevelChunkTicks<T> pChunkTicks) {
      long $$2 = pChunkPos.toLong();
      this.allContainers.put($$2, pChunkTicks);
      ScheduledTick<T> $$3 = pChunkTicks.peek();
      if ($$3 != null) {
         this.nextTickForContainer.put($$2, $$3.triggerTick());
      }

      pChunkTicks.setOnTickAdded(this.chunkScheduleUpdater);
   }

   public void removeContainer(ChunkPos pChunkPos) {
      long $$1 = pChunkPos.toLong();
      LevelChunkTicks<T> $$2 = (LevelChunkTicks<T>)this.allContainers.remove($$1);
      this.nextTickForContainer.remove($$1);
      if ($$2 != null) {
         $$2.setOnTickAdded(null);
      }
   }

   @Override
   public void schedule(ScheduledTick<T> pTick) {
      long $$1 = ChunkPos.asLong(pTick.pos());
      LevelChunkTicks<T> $$2 = (LevelChunkTicks<T>)this.allContainers.get($$1);
      if ($$2 == null) {
         Util.pauseInIde(new IllegalStateException("Trying to schedule tick in not loaded position " + pTick.pos()));
      } else {
         $$2.schedule(pTick);
      }
   }

   public void tick(long pGameTime, int pMaxAllowedTicks, BiConsumer<BlockPos, T> pTicker) {
      ProfilerFiller $$3 = this.profiler.get();
      $$3.push("collect");
      this.collectTicks(pGameTime, pMaxAllowedTicks, $$3);
      $$3.popPush("run");
      $$3.incrementCounter("ticksToRun", this.toRunThisTick.size());
      this.runCollectedTicks(pTicker);
      $$3.popPush("cleanup");
      this.cleanupAfterTick();
      $$3.pop();
   }

   private void collectTicks(long pGameTime, int pMaxAllowedTicks, ProfilerFiller pProfiler) {
      this.sortContainersToTick(pGameTime);
      pProfiler.incrementCounter("containersToTick", this.containersToTick.size());
      this.drainContainers(pGameTime, pMaxAllowedTicks);
      this.rescheduleLeftoverContainers();
   }

   private void sortContainersToTick(long pGameTime) {
      ObjectIterator<Entry> $$1 = Long2LongMaps.fastIterator(this.nextTickForContainer);

      while ($$1.hasNext()) {
         Entry $$2 = (Entry)$$1.next();
         long $$3 = $$2.getLongKey();
         long $$4 = $$2.getLongValue();
         if ($$4 <= pGameTime) {
            LevelChunkTicks<T> $$5 = (LevelChunkTicks<T>)this.allContainers.get($$3);
            if ($$5 == null) {
               $$1.remove();
            } else {
               ScheduledTick<T> $$6 = $$5.peek();
               if ($$6 == null) {
                  $$1.remove();
               } else if ($$6.triggerTick() > pGameTime) {
                  $$2.setValue($$6.triggerTick());
               } else if (this.tickCheck.test($$3)) {
                  $$1.remove();
                  this.containersToTick.add($$5);
               }
            }
         }
      }
   }

   private void drainContainers(long pGameTime, int pMaxAllowedTicks) {
      LevelChunkTicks<T> $$2;
      while (this.canScheduleMoreTicks(pMaxAllowedTicks) && ($$2 = this.containersToTick.poll()) != null) {
         ScheduledTick<T> $$3 = $$2.poll();
         this.scheduleForThisTick($$3);
         this.drainFromCurrentContainer(this.containersToTick, $$2, pGameTime, pMaxAllowedTicks);
         ScheduledTick<T> $$4 = $$2.peek();
         if ($$4 != null) {
            if ($$4.triggerTick() <= pGameTime && this.canScheduleMoreTicks(pMaxAllowedTicks)) {
               this.containersToTick.add($$2);
            } else {
               this.updateContainerScheduling($$4);
            }
         }
      }
   }

   private void rescheduleLeftoverContainers() {
      for (LevelChunkTicks<T> $$0 : this.containersToTick) {
         this.updateContainerScheduling($$0.peek());
      }
   }

   private void updateContainerScheduling(ScheduledTick<T> pTick) {
      this.nextTickForContainer.put(ChunkPos.asLong(pTick.pos()), pTick.triggerTick());
   }

   private void drainFromCurrentContainer(
      Queue<LevelChunkTicks<T>> pContainersToTick, LevelChunkTicks<T> pLevelChunkTicks, long pGameTime, int pMaxAllowedTicks
   ) {
      if (this.canScheduleMoreTicks(pMaxAllowedTicks)) {
         LevelChunkTicks<T> $$4 = pContainersToTick.peek();
         ScheduledTick<T> $$5 = $$4 != null ? $$4.peek() : null;

         while (this.canScheduleMoreTicks(pMaxAllowedTicks)) {
            ScheduledTick<T> $$6 = pLevelChunkTicks.peek();
            if ($$6 == null || $$6.triggerTick() > pGameTime || $$5 != null && ScheduledTick.INTRA_TICK_DRAIN_ORDER.compare($$6, $$5) > 0) {
               break;
            }

            pLevelChunkTicks.poll();
            this.scheduleForThisTick($$6);
         }
      }
   }

   private void scheduleForThisTick(ScheduledTick<T> pTick) {
      this.toRunThisTick.add(pTick);
   }

   private boolean canScheduleMoreTicks(int pMaxAllowedTicks) {
      return this.toRunThisTick.size() < pMaxAllowedTicks;
   }

   private void runCollectedTicks(BiConsumer<BlockPos, T> pTicker) {
      while (!this.toRunThisTick.isEmpty()) {
         ScheduledTick<T> $$1 = this.toRunThisTick.poll();
         if (!this.toRunThisTickSet.isEmpty()) {
            this.toRunThisTickSet.remove($$1);
         }

         this.alreadyRunThisTick.add($$1);
         pTicker.accept($$1.pos(), $$1.type());
      }
   }

   private void cleanupAfterTick() {
      this.toRunThisTick.clear();
      this.containersToTick.clear();
      this.alreadyRunThisTick.clear();
      this.toRunThisTickSet.clear();
   }

   @Override
   public boolean hasScheduledTick(BlockPos pPos, T pType) {
      LevelChunkTicks<T> $$2 = (LevelChunkTicks<T>)this.allContainers.get(ChunkPos.asLong(pPos));
      return $$2 != null && $$2.hasScheduledTick(pPos, pType);
   }

   @Override
   public boolean willTickThisTick(BlockPos pPos, T pType) {
      this.calculateTickSetIfNeeded();
      return this.toRunThisTickSet.contains(ScheduledTick.probe(pType, pPos));
   }

   private void calculateTickSetIfNeeded() {
      if (this.toRunThisTickSet.isEmpty() && !this.toRunThisTick.isEmpty()) {
         this.toRunThisTickSet.addAll(this.toRunThisTick);
      }
   }

   private void forContainersInArea(BoundingBox pArea, LevelTicks.PosAndContainerConsumer<T> pAction) {
      int $$2 = SectionPos.posToSectionCoord((double)pArea.minX());
      int $$3 = SectionPos.posToSectionCoord((double)pArea.minZ());
      int $$4 = SectionPos.posToSectionCoord((double)pArea.maxX());
      int $$5 = SectionPos.posToSectionCoord((double)pArea.maxZ());

      for (int $$6 = $$2; $$6 <= $$4; $$6++) {
         for (int $$7 = $$3; $$7 <= $$5; $$7++) {
            long $$8 = ChunkPos.asLong($$6, $$7);
            LevelChunkTicks<T> $$9 = (LevelChunkTicks<T>)this.allContainers.get($$8);
            if ($$9 != null) {
               pAction.accept($$8, $$9);
            }
         }
      }
   }

   public void clearArea(BoundingBox pArea) {
      Predicate<ScheduledTick<T>> $$1 = p_193241_ -> pArea.isInside(p_193241_.pos());
      this.forContainersInArea(pArea, (p_193276_, p_193277_) -> {
         ScheduledTick<T> $$3 = p_193277_.peek();
         p_193277_.removeIf($$1);
         ScheduledTick<T> $$4 = p_193277_.peek();
         if ($$4 != $$3) {
            if ($$4 != null) {
               this.updateContainerScheduling($$4);
            } else {
               this.nextTickForContainer.remove(p_193276_);
            }
         }
      });
      this.alreadyRunThisTick.removeIf($$1);
      this.toRunThisTick.removeIf($$1);
   }

   public void copyArea(BoundingBox pArea, Vec3i pOffset) {
      this.copyAreaFrom(this, pArea, pOffset);
   }

   public void copyAreaFrom(LevelTicks<T> pLevelTicks, BoundingBox pArea, Vec3i pOffset) {
      List<ScheduledTick<T>> $$3 = new ArrayList<>();
      Predicate<ScheduledTick<T>> $$4 = p_200922_ -> pArea.isInside(p_200922_.pos());
      pLevelTicks.alreadyRunThisTick.stream().filter($$4).forEach($$3::add);
      pLevelTicks.toRunThisTick.stream().filter($$4).forEach($$3::add);
      pLevelTicks.forContainersInArea(pArea, (p_200931_, p_200932_) -> p_200932_.getAll().filter($$4).forEach($$3::add));
      LongSummaryStatistics $$5 = $$3.stream().mapToLong(ScheduledTick::subTickOrder).summaryStatistics();
      long $$6 = $$5.getMin();
      long $$7 = $$5.getMax();
      $$3.forEach(
         p_193260_ -> this.schedule(
               new ScheduledTick<>(
                  p_193260_.type(), p_193260_.pos().offset(pOffset), p_193260_.triggerTick(), p_193260_.priority(), p_193260_.subTickOrder() - $$6 + $$7 + 1L
               )
            )
      );
   }

   @Override
   public int count() {
      return this.allContainers.values().stream().mapToInt(TickAccess::count).sum();
   }

   @FunctionalInterface
   interface PosAndContainerConsumer<T> {
      void accept(long pPos, LevelChunkTicks<T> pContainer);
   }
}
