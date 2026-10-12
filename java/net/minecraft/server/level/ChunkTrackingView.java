package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Consumer;
import net.minecraft.world.level.ChunkPos;

public interface ChunkTrackingView {
   ChunkTrackingView EMPTY = new ChunkTrackingView() {
      @Override
      public boolean contains(int p_298894_, int p_300849_, boolean p_299623_) {
         return false;
      }

      @Override
      public void forEach(Consumer<ChunkPos> p_298868_) {
      }
   };

   static ChunkTrackingView of(ChunkPos pCenter, int pViewDistance) {
      return new ChunkTrackingView.Positioned(pCenter, pViewDistance);
   }

   static void difference(
      ChunkTrackingView pOldChunkTrackingView, ChunkTrackingView pNewChunkTrackingView, Consumer<ChunkPos> pChunkMarker, Consumer<ChunkPos> pChunkDropper
   ) {
      if (!pOldChunkTrackingView.equals(pNewChunkTrackingView)) {
         if (pOldChunkTrackingView instanceof ChunkTrackingView.Positioned $$4
            && pNewChunkTrackingView instanceof ChunkTrackingView.Positioned $$5
            && $$4.squareIntersects($$5)) {
            int $$6 = Math.min($$4.minX(), $$5.minX());
            int $$7 = Math.min($$4.minZ(), $$5.minZ());
            int $$8 = Math.max($$4.maxX(), $$5.maxX());
            int $$9 = Math.max($$4.maxZ(), $$5.maxZ());

            for (int $$10 = $$6; $$10 <= $$8; $$10++) {
               for (int $$11 = $$7; $$11 <= $$9; $$11++) {
                  boolean $$12 = $$4.contains($$10, $$11);
                  boolean $$13 = $$5.contains($$10, $$11);
                  if ($$12 != $$13) {
                     if ($$13) {
                        pChunkMarker.accept(new ChunkPos($$10, $$11));
                     } else {
                        pChunkDropper.accept(new ChunkPos($$10, $$11));
                     }
                  }
               }
            }

            return;
         }

         pOldChunkTrackingView.forEach(pChunkDropper);
         pNewChunkTrackingView.forEach(pChunkMarker);
      }
   }

   default boolean contains(ChunkPos pChunkPos) {
      return this.contains(pChunkPos.x, pChunkPos.z);
   }

   default boolean contains(int pX, int pZ) {
      return this.contains(pX, pZ, true);
   }

   boolean contains(int pX, int pZ, boolean pIncludeOuterChunksAdjacentToViewBorder);

   void forEach(Consumer<ChunkPos> pAction);

   default boolean isInViewDistance(int pX, int pZ) {
      return this.contains(pX, pZ, false);
   }

   static boolean isInViewDistance(int pCenterX, int pCenterZ, int pViewDistance, int pX, int pZ) {
      return isWithinDistance(pCenterX, pCenterZ, pViewDistance, pX, pZ, false);
   }

   static boolean isWithinDistance(int pCenterX, int pCenterZ, int pViewDistance, int pX, int pZ, boolean pIncludeOuterChunksAdjacentToViewBorder) {
      int $$6 = Math.max(0, Math.abs(pX - pCenterX) - 1);
      int $$7 = Math.max(0, Math.abs(pZ - pCenterZ) - 1);
      long $$8 = (long)Math.max(0, Math.max($$6, $$7) - (pIncludeOuterChunksAdjacentToViewBorder ? 1 : 0));
      long $$9 = (long)Math.min($$6, $$7);
      long $$10 = $$9 * $$9 + $$8 * $$8;
      int $$11 = pViewDistance * pViewDistance;
      return $$10 < (long)$$11;
   }

   public static record Positioned(ChunkPos center, int viewDistance) implements ChunkTrackingView {
      int minX() {
         return this.center.x - this.viewDistance - 1;
      }

      int minZ() {
         return this.center.z - this.viewDistance - 1;
      }

      int maxX() {
         return this.center.x + this.viewDistance + 1;
      }

      int maxZ() {
         return this.center.z + this.viewDistance + 1;
      }

      @VisibleForTesting
      protected boolean squareIntersects(ChunkTrackingView.Positioned pOther) {
         return this.minX() <= pOther.maxX() && this.maxX() >= pOther.minX() && this.minZ() <= pOther.maxZ() && this.maxZ() >= pOther.minZ();
      }

      @Override
      public boolean contains(int pX, int pZ, boolean pIncludeOuterChunksAdjacentToViewBorder) {
         return ChunkTrackingView.isWithinDistance(this.center.x, this.center.z, this.viewDistance, pX, pZ, pIncludeOuterChunksAdjacentToViewBorder);
      }

      @Override
      public void forEach(Consumer<ChunkPos> pAction) {
         for (int $$1 = this.minX(); $$1 <= this.maxX(); $$1++) {
            for (int $$2 = this.minZ(); $$2 <= this.maxZ(); $$2++) {
               if (this.contains($$1, $$2)) {
                  pAction.accept(new ChunkPos($$1, $$2));
               }
            }
         }
      }
   }
}
