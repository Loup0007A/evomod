package net.minecraft.server.level;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;

public abstract class ChunkTracker extends DynamicGraphMinFixedPoint {
   protected ChunkTracker(int pFirstQueuedLevel, int pWidth, int pHeight) {
      super(pFirstQueuedLevel, pWidth, pHeight);
   }

   @Override
   protected boolean isSource(long pPos) {
      return pPos == ChunkPos.INVALID_CHUNK_POS;
   }

   @Override
   protected void checkNeighborsAfterUpdate(long pPos, int pLevel, boolean pIsDecreasing) {
      if (!pIsDecreasing || pLevel < this.levelCount - 2) {
         ChunkPos $$3 = new ChunkPos(pPos);
         int $$4 = $$3.x;
         int $$5 = $$3.z;

         for (int $$6 = -1; $$6 <= 1; $$6++) {
            for (int $$7 = -1; $$7 <= 1; $$7++) {
               long $$8 = ChunkPos.asLong($$4 + $$6, $$5 + $$7);
               if ($$8 != pPos) {
                  this.checkNeighbor(pPos, $$8, pLevel, pIsDecreasing);
               }
            }
         }
      }
   }

   @Override
   protected int getComputedLevel(long pPos, long pExcludedSourcePos, int pLevel) {
      int $$3 = pLevel;
      ChunkPos $$4 = new ChunkPos(pPos);
      int $$5 = $$4.x;
      int $$6 = $$4.z;

      for (int $$7 = -1; $$7 <= 1; $$7++) {
         for (int $$8 = -1; $$8 <= 1; $$8++) {
            long $$9 = ChunkPos.asLong($$5 + $$7, $$6 + $$8);
            if ($$9 == pPos) {
               $$9 = ChunkPos.INVALID_CHUNK_POS;
            }

            if ($$9 != pExcludedSourcePos) {
               int $$10 = this.computeLevelFromNeighbor($$9, pPos, this.getLevel($$9));
               if ($$3 > $$10) {
                  $$3 = $$10;
               }

               if ($$3 == 0) {
                  return $$3;
               }
            }
         }
      }

      return $$3;
   }

   @Override
   protected int computeLevelFromNeighbor(long pStartPos, long pEndPos, int pStartLevel) {
      return pStartPos == ChunkPos.INVALID_CHUNK_POS ? this.getLevelFromSource(pEndPos) : pStartLevel + 1;
   }

   protected abstract int getLevelFromSource(long pPos);

   public void update(long pPos, int pLevel, boolean pIsDecreasing) {
      this.checkEdge(ChunkPos.INVALID_CHUNK_POS, pPos, pLevel, pIsDecreasing);
   }
}
