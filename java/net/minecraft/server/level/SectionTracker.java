package net.minecraft.server.level;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;

public abstract class SectionTracker extends DynamicGraphMinFixedPoint {
   protected SectionTracker(int pFirstQueuedLevel, int pWidth, int pHeight) {
      super(pFirstQueuedLevel, pWidth, pHeight);
   }

   @Override
   protected void checkNeighborsAfterUpdate(long pPos, int pLevel, boolean pIsDecreasing) {
      if (!pIsDecreasing || pLevel < this.levelCount - 2) {
         for (int $$3 = -1; $$3 <= 1; $$3++) {
            for (int $$4 = -1; $$4 <= 1; $$4++) {
               for (int $$5 = -1; $$5 <= 1; $$5++) {
                  long $$6 = SectionPos.offset(pPos, $$3, $$4, $$5);
                  if ($$6 != pPos) {
                     this.checkNeighbor(pPos, $$6, pLevel, pIsDecreasing);
                  }
               }
            }
         }
      }
   }

   @Override
   protected int getComputedLevel(long pPos, long pExcludedSourcePos, int pLevel) {
      int $$3 = pLevel;

      for (int $$4 = -1; $$4 <= 1; $$4++) {
         for (int $$5 = -1; $$5 <= 1; $$5++) {
            for (int $$6 = -1; $$6 <= 1; $$6++) {
               long $$7 = SectionPos.offset(pPos, $$4, $$5, $$6);
               if ($$7 == pPos) {
                  $$7 = Long.MAX_VALUE;
               }

               if ($$7 != pExcludedSourcePos) {
                  int $$8 = this.computeLevelFromNeighbor($$7, pPos, this.getLevel($$7));
                  if ($$3 > $$8) {
                     $$3 = $$8;
                  }

                  if ($$3 == 0) {
                     return $$3;
                  }
               }
            }
         }
      }

      return $$3;
   }

   @Override
   protected int computeLevelFromNeighbor(long pStartPos, long pEndPos, int pStartLevel) {
      return this.isSource(pStartPos) ? this.getLevelFromSource(pEndPos) : pStartLevel + 1;
   }

   protected abstract int getLevelFromSource(long pPos);

   public void update(long pPos, int pLevel, boolean pIsDecreasing) {
      this.checkEdge(Long.MAX_VALUE, pPos, pLevel, pIsDecreasing);
   }
}
