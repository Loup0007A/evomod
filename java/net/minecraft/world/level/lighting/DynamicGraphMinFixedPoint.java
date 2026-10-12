package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.function.LongPredicate;
import net.minecraft.util.Mth;

public abstract class DynamicGraphMinFixedPoint {
   public static final long SOURCE = Long.MAX_VALUE;
   private static final int NO_COMPUTED_LEVEL = 255;
   protected final int levelCount;
   private final LeveledPriorityQueue priorityQueue;
   private final Long2ByteMap computedLevels;
   private volatile boolean hasWork;

   protected DynamicGraphMinFixedPoint(int pFirstQueuedLevel, int pWidth, final int pHeight) {
      if (pFirstQueuedLevel >= 254) {
         throw new IllegalArgumentException("Level count must be < 254.");
      } else {
         this.levelCount = pFirstQueuedLevel;
         this.priorityQueue = new LeveledPriorityQueue(pFirstQueuedLevel, pWidth);
         this.computedLevels = new Long2ByteOpenHashMap(pHeight, 0.5F) {
            protected void rehash(int p_75611_) {
               if (p_75611_ > pHeight) {
                  super.rehash(p_75611_);
               }
            }
         };
         this.computedLevels.defaultReturnValue((byte)-1);
      }
   }

   protected void removeFromQueue(long p_75601_) {
      int $$1 = this.computedLevels.remove(p_75601_) & 255;
      if ($$1 != 255) {
         int $$2 = this.getLevel(p_75601_);
         int $$3 = this.calculatePriority($$2, $$1);
         this.priorityQueue.dequeue(p_75601_, $$3, this.levelCount);
         this.hasWork = !this.priorityQueue.isEmpty();
      }
   }

   public void removeIf(LongPredicate pPredicate) {
      LongList $$1 = new LongArrayList();
      this.computedLevels.keySet().forEach(p_75586_ -> {
         if (pPredicate.test(p_75586_)) {
            $$1.add(p_75586_);
         }
      });
      $$1.forEach(this::removeFromQueue);
   }

   private int calculatePriority(int pOldLevel, int pNewLevel) {
      return Math.min(Math.min(pOldLevel, pNewLevel), this.levelCount - 1);
   }

   protected void checkNode(long pLevelPos) {
      this.checkEdge(pLevelPos, pLevelPos, this.levelCount - 1, false);
   }

   protected void checkEdge(long pFromPos, long pToPos, int pNewLevel, boolean pIsDecreasing) {
      this.checkEdge(pFromPos, pToPos, pNewLevel, this.getLevel(pToPos), this.computedLevels.get(pToPos) & 255, pIsDecreasing);
      this.hasWork = !this.priorityQueue.isEmpty();
   }

   private void checkEdge(long pFromPos, long pToPos, int pNewLevel, int pPreviousLevel, int pPropagationLevel, boolean pIsDecreasing) {
      if (!this.isSource(pToPos)) {
         pNewLevel = Mth.clamp(pNewLevel, 0, this.levelCount - 1);
         pPreviousLevel = Mth.clamp(pPreviousLevel, 0, this.levelCount - 1);
         boolean $$6 = pPropagationLevel == 255;
         if ($$6) {
            pPropagationLevel = pPreviousLevel;
         }

         int $$7;
         if (pIsDecreasing) {
            $$7 = Math.min(pPropagationLevel, pNewLevel);
         } else {
            $$7 = Mth.clamp(this.getComputedLevel(pToPos, pFromPos, pNewLevel), 0, this.levelCount - 1);
         }

         int $$9 = this.calculatePriority(pPreviousLevel, pPropagationLevel);
         if (pPreviousLevel != $$7) {
            int $$10 = this.calculatePriority(pPreviousLevel, $$7);
            if ($$9 != $$10 && !$$6) {
               this.priorityQueue.dequeue(pToPos, $$9, $$10);
            }

            this.priorityQueue.enqueue(pToPos, $$10);
            this.computedLevels.put(pToPos, (byte)$$7);
         } else if (!$$6) {
            this.priorityQueue.dequeue(pToPos, $$9, this.levelCount);
            this.computedLevels.remove(pToPos);
         }
      }
   }

   protected final void checkNeighbor(long pFromPos, long pToPos, int pSourceLevel, boolean pIsDecreasing) {
      int $$4 = this.computedLevels.get(pToPos) & 255;
      int $$5 = Mth.clamp(this.computeLevelFromNeighbor(pFromPos, pToPos, pSourceLevel), 0, this.levelCount - 1);
      if (pIsDecreasing) {
         this.checkEdge(pFromPos, pToPos, $$5, this.getLevel(pToPos), $$4, pIsDecreasing);
      } else {
         boolean $$6 = $$4 == 255;
         int $$7;
         if ($$6) {
            $$7 = Mth.clamp(this.getLevel(pToPos), 0, this.levelCount - 1);
         } else {
            $$7 = $$4;
         }

         if ($$5 == $$7) {
            this.checkEdge(pFromPos, pToPos, this.levelCount - 1, $$6 ? $$7 : this.getLevel(pToPos), $$4, pIsDecreasing);
         }
      }
   }

   protected final boolean hasWork() {
      return this.hasWork;
   }

   protected final int runUpdates(int pToUpdateCount) {
      if (this.priorityQueue.isEmpty()) {
         return pToUpdateCount;
      } else {
         while (!this.priorityQueue.isEmpty() && pToUpdateCount > 0) {
            pToUpdateCount--;
            long $$1 = this.priorityQueue.removeFirstLong();
            int $$2 = Mth.clamp(this.getLevel($$1), 0, this.levelCount - 1);
            int $$3 = this.computedLevels.remove($$1) & 255;
            if ($$3 < $$2) {
               this.setLevel($$1, $$3);
               this.checkNeighborsAfterUpdate($$1, $$3, true);
            } else if ($$3 > $$2) {
               this.setLevel($$1, this.levelCount - 1);
               if ($$3 != this.levelCount - 1) {
                  this.priorityQueue.enqueue($$1, this.calculatePriority(this.levelCount - 1, $$3));
                  this.computedLevels.put($$1, (byte)$$3);
               }

               this.checkNeighborsAfterUpdate($$1, $$2, false);
            }
         }

         this.hasWork = !this.priorityQueue.isEmpty();
         return pToUpdateCount;
      }
   }

   public int getQueueSize() {
      return this.computedLevels.size();
   }

   protected boolean isSource(long pPos) {
      return pPos == Long.MAX_VALUE;
   }

   protected abstract int getComputedLevel(long pPos, long pExcludedSourcePos, int pLevel);

   protected abstract void checkNeighborsAfterUpdate(long pPos, int pLevel, boolean pIsDecreasing);

   protected abstract int getLevel(long pChunkPos);

   protected abstract void setLevel(long pChunkPos, int pLevel);

   protected abstract int computeLevelFromNeighbor(long pStartPos, long pEndPos, int pStartLevel);
}
