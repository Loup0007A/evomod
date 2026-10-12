package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;

public class LeveledPriorityQueue {
   private final int levelCount;
   private final LongLinkedOpenHashSet[] queues;
   private int firstQueuedLevel;

   public LeveledPriorityQueue(int pLevelCount, final int pExpectedSize) {
      this.levelCount = pLevelCount;
      this.queues = new LongLinkedOpenHashSet[pLevelCount];

      for (int $$2 = 0; $$2 < pLevelCount; $$2++) {
         this.queues[$$2] = new LongLinkedOpenHashSet(pExpectedSize, 0.5F) {
            protected void rehash(int p_278313_) {
               if (p_278313_ > pExpectedSize) {
                  super.rehash(p_278313_);
               }
            }
         };
      }

      this.firstQueuedLevel = pLevelCount;
   }

   public long removeFirstLong() {
      LongLinkedOpenHashSet $$0 = this.queues[this.firstQueuedLevel];
      long $$1 = $$0.removeFirstLong();
      if ($$0.isEmpty()) {
         this.checkFirstQueuedLevel(this.levelCount);
      }

      return $$1;
   }

   public boolean isEmpty() {
      return this.firstQueuedLevel >= this.levelCount;
   }

   public void dequeue(long pValue, int pLevelIndex, int pEndIndex) {
      LongLinkedOpenHashSet $$3 = this.queues[pLevelIndex];
      $$3.remove(pValue);
      if ($$3.isEmpty() && this.firstQueuedLevel == pLevelIndex) {
         this.checkFirstQueuedLevel(pEndIndex);
      }
   }

   public void enqueue(long pValue, int pLevelIndex) {
      this.queues[pLevelIndex].add(pValue);
      if (this.firstQueuedLevel > pLevelIndex) {
         this.firstQueuedLevel = pLevelIndex;
      }
   }

   private void checkFirstQueuedLevel(int pEndLevelIndex) {
      int $$1 = this.firstQueuedLevel;
      this.firstQueuedLevel = pEndLevelIndex;

      for (int $$2 = $$1 + 1; $$2 < pEndLevelIndex; $$2++) {
         if (!this.queues[$$2].isEmpty()) {
            this.firstQueuedLevel = $$2;
            break;
         }
      }
   }
}
