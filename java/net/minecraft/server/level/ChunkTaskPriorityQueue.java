package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.world.level.ChunkPos;

public class ChunkTaskPriorityQueue<T> {
   public static final int PRIORITY_LEVEL_COUNT = ChunkLevel.MAX_LEVEL + 2;
   private final List<Long2ObjectLinkedOpenHashMap<List<Optional<T>>>> taskQueue = IntStream.range(0, PRIORITY_LEVEL_COUNT)
      .mapToObj(p_140520_ -> new Long2ObjectLinkedOpenHashMap())
      .collect(Collectors.toList());
   private volatile int firstQueue = PRIORITY_LEVEL_COUNT;
   private final String name;
   private final LongSet acquired = new LongOpenHashSet();
   private final int maxTasks;

   public ChunkTaskPriorityQueue(String pName, int pMaxTasks) {
      this.name = pName;
      this.maxTasks = pMaxTasks;
   }

   protected void resortChunkTasks(int pQueueLevel, ChunkPos pChunkPos, int pTicketLevel) {
      if (pQueueLevel < PRIORITY_LEVEL_COUNT) {
         Long2ObjectLinkedOpenHashMap<List<Optional<T>>> $$3 = this.taskQueue.get(pQueueLevel);
         List<Optional<T>> $$4 = (List<Optional<T>>)$$3.remove(pChunkPos.toLong());
         if (pQueueLevel == this.firstQueue) {
            while (this.hasWork() && this.taskQueue.get(this.firstQueue).isEmpty()) {
               this.firstQueue++;
            }
         }

         if ($$4 != null && !$$4.isEmpty()) {
            ((List)this.taskQueue.get(pTicketLevel).computeIfAbsent(pChunkPos.toLong(), p_140547_ -> Lists.newArrayList())).addAll($$4);
            this.firstQueue = Math.min(this.firstQueue, pTicketLevel);
         }
      }
   }

   protected void submit(Optional<T> pTask, long pChunkPos, int pChunkLevel) {
      ((List)this.taskQueue.get(pChunkLevel).computeIfAbsent(pChunkPos, p_140545_ -> Lists.newArrayList())).add(pTask);
      this.firstQueue = Math.min(this.firstQueue, pChunkLevel);
   }

   protected void release(long pChunkPos, boolean pFullClear) {
      for (Long2ObjectLinkedOpenHashMap<List<Optional<T>>> $$2 : this.taskQueue) {
         List<Optional<T>> $$3 = (List<Optional<T>>)$$2.get(pChunkPos);
         if ($$3 != null) {
            if (pFullClear) {
               $$3.clear();
            } else {
               $$3.removeIf(p_296577_ -> p_296577_.isEmpty());
            }

            if ($$3.isEmpty()) {
               $$2.remove(pChunkPos);
            }
         }
      }

      while (this.hasWork() && this.taskQueue.get(this.firstQueue).isEmpty()) {
         this.firstQueue++;
      }

      this.acquired.remove(pChunkPos);
   }

   private Runnable acquire(long pChunkPos) {
      return () -> this.acquired.add(pChunkPos);
   }

   @Nullable
   public Stream<Either<T, Runnable>> pop() {
      if (this.acquired.size() >= this.maxTasks) {
         return null;
      } else if (!this.hasWork()) {
         return null;
      } else {
         int $$0 = this.firstQueue;
         Long2ObjectLinkedOpenHashMap<List<Optional<T>>> $$1 = this.taskQueue.get($$0);
         long $$2 = $$1.firstLongKey();
         List<Optional<T>> $$3 = (List<Optional<T>>)$$1.removeFirst();

         while (this.hasWork() && this.taskQueue.get(this.firstQueue).isEmpty()) {
            this.firstQueue++;
         }

         return $$3.stream().map(p_140529_ -> p_140529_.map(Either::left).orElseGet(() -> Either.right(this.acquire($$2))));
      }
   }

   public boolean hasWork() {
      return this.firstQueue < PRIORITY_LEVEL_COUNT;
   }

   @Override
   public String toString() {
      return this.name + " " + this.firstQueue + "...";
   }

   @VisibleForTesting
   LongSet getAcquired() {
      return new LongOpenHashSet(this.acquired);
   }
}
