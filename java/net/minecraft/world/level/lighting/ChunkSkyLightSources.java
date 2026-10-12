package net.minecraft.world.level.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.BitStorage;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ChunkSkyLightSources {
   private static final int SIZE = 16;
   public static final int NEGATIVE_INFINITY = Integer.MIN_VALUE;
   private final int minY;
   private final BitStorage heightmap;
   private final BlockPos.MutableBlockPos mutablePos1 = new BlockPos.MutableBlockPos();
   private final BlockPos.MutableBlockPos mutablePos2 = new BlockPos.MutableBlockPos();

   public ChunkSkyLightSources(LevelHeightAccessor pLevel) {
      this.minY = pLevel.getMinBuildHeight() - 1;
      int $$1 = pLevel.getMaxBuildHeight();
      int $$2 = Mth.ceillog2($$1 - this.minY + 1);
      this.heightmap = new SimpleBitStorage($$2, 256);
   }

   public void fillFrom(ChunkAccess pChunk) {
      int $$1 = pChunk.getHighestFilledSectionIndex();
      if ($$1 == -1) {
         this.fill(this.minY);
      } else {
         for (int $$2 = 0; $$2 < 16; $$2++) {
            for (int $$3 = 0; $$3 < 16; $$3++) {
               int $$4 = Math.max(this.findLowestSourceY(pChunk, $$1, $$3, $$2), this.minY);
               this.set(index($$3, $$2), $$4);
            }
         }
      }
   }

   private int findLowestSourceY(ChunkAccess pChunk, int pSectionIndex, int pX, int pZ) {
      int $$4 = SectionPos.sectionToBlockCoord(pChunk.getSectionYFromSectionIndex(pSectionIndex) + 1);
      BlockPos.MutableBlockPos $$5 = this.mutablePos1.set(pX, $$4, pZ);
      BlockPos.MutableBlockPos $$6 = this.mutablePos2.setWithOffset($$5, Direction.DOWN);
      BlockState $$7 = Blocks.AIR.defaultBlockState();

      for (int $$8 = pSectionIndex; $$8 >= 0; $$8--) {
         LevelChunkSection $$9 = pChunk.getSection($$8);
         if ($$9.hasOnlyAir()) {
            $$7 = Blocks.AIR.defaultBlockState();
            int $$10 = pChunk.getSectionYFromSectionIndex($$8);
            $$5.setY(SectionPos.sectionToBlockCoord($$10));
            $$6.setY($$5.getY() - 1);
         } else {
            for (int $$11 = 15; $$11 >= 0; $$11--) {
               BlockState $$12 = $$9.getBlockState(pX, $$11, pZ);
               if (isEdgeOccluded(pChunk, $$5, $$7, $$6, $$12)) {
                  return $$5.getY();
               }

               $$7 = $$12;
               $$5.set($$6);
               $$6.move(Direction.DOWN);
            }
         }
      }

      return this.minY;
   }

   public boolean update(BlockGetter pLevel, int pX, int pY, int pZ) {
      int $$4 = pY + 1;
      int $$5 = index(pX, pZ);
      int $$6 = this.get($$5);
      if ($$4 < $$6) {
         return false;
      } else {
         BlockPos $$7 = this.mutablePos1.set(pX, pY + 1, pZ);
         BlockState $$8 = pLevel.getBlockState($$7);
         BlockPos $$9 = this.mutablePos2.set(pX, pY, pZ);
         BlockState $$10 = pLevel.getBlockState($$9);
         if (this.updateEdge(pLevel, $$5, $$6, $$7, $$8, $$9, $$10)) {
            return true;
         } else {
            BlockPos $$11 = this.mutablePos1.set(pX, pY - 1, pZ);
            BlockState $$12 = pLevel.getBlockState($$11);
            return this.updateEdge(pLevel, $$5, $$6, $$9, $$10, $$11, $$12);
         }
      }
   }

   private boolean updateEdge(BlockGetter pLevel, int pIndex, int pMinY, BlockPos pPos1, BlockState pState1, BlockPos pPos2, BlockState pState2) {
      int $$7 = pPos1.getY();
      if (isEdgeOccluded(pLevel, pPos1, pState1, pPos2, pState2)) {
         if ($$7 > pMinY) {
            this.set(pIndex, $$7);
            return true;
         }
      } else if ($$7 == pMinY) {
         this.set(pIndex, this.findLowestSourceBelow(pLevel, pPos2, pState2));
         return true;
      }

      return false;
   }

   private int findLowestSourceBelow(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
      BlockPos.MutableBlockPos $$3 = this.mutablePos1.set(pPos);
      BlockPos.MutableBlockPos $$4 = this.mutablePos2.setWithOffset(pPos, Direction.DOWN);
      BlockState $$5 = pState;

      while ($$4.getY() >= this.minY) {
         BlockState $$6 = pLevel.getBlockState($$4);
         if (isEdgeOccluded(pLevel, $$3, $$5, $$4, $$6)) {
            return $$3.getY();
         }

         $$5 = $$6;
         $$3.set($$4);
         $$4.move(Direction.DOWN);
      }

      return this.minY;
   }

   private static boolean isEdgeOccluded(BlockGetter pLevel, BlockPos pPos1, BlockState pState1, BlockPos pPos2, BlockState pState2) {
      if (pState2.getLightBlock(pLevel, pPos2) != 0) {
         return true;
      } else {
         VoxelShape $$5 = LightEngine.getOcclusionShape(pLevel, pPos1, pState1, Direction.DOWN);
         VoxelShape $$6 = LightEngine.getOcclusionShape(pLevel, pPos2, pState2, Direction.UP);
         return Shapes.faceShapeOccludes($$5, $$6);
      }
   }

   public int getLowestSourceY(int pX, int pZ) {
      int $$2 = this.get(index(pX, pZ));
      return this.extendSourcesBelowWorld($$2);
   }

   public int getHighestLowestSourceY() {
      int $$0 = Integer.MIN_VALUE;

      for (int $$1 = 0; $$1 < this.heightmap.getSize(); $$1++) {
         int $$2 = this.heightmap.get($$1);
         if ($$2 > $$0) {
            $$0 = $$2;
         }
      }

      return this.extendSourcesBelowWorld($$0 + this.minY);
   }

   private void fill(int pValue) {
      int $$1 = pValue - this.minY;

      for (int $$2 = 0; $$2 < this.heightmap.getSize(); $$2++) {
         this.heightmap.set($$2, $$1);
      }
   }

   private void set(int pIndex, int pValue) {
      this.heightmap.set(pIndex, pValue - this.minY);
   }

   private int get(int pIndex) {
      return this.heightmap.get(pIndex) + this.minY;
   }

   private int extendSourcesBelowWorld(int pY) {
      return pY == this.minY ? Integer.MIN_VALUE : pY;
   }

   private static int index(int pX, int pZ) {
      return pX + pZ * 16;
   }
}
