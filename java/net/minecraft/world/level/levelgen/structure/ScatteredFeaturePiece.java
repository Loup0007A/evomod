package net.minecraft.world.level.levelgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public abstract class ScatteredFeaturePiece extends StructurePiece {
   protected final int width;
   protected final int height;
   protected final int depth;
   protected int heightPosition = -1;

   protected ScatteredFeaturePiece(StructurePieceType pType, int pX, int pY, int pZ, int pWidth, int pHeight, int pDepth, Direction pOrientation) {
      super(pType, 0, StructurePiece.makeBoundingBox(pX, pY, pZ, pOrientation, pWidth, pHeight, pDepth));
      this.width = pWidth;
      this.height = pHeight;
      this.depth = pDepth;
      this.setOrientation(pOrientation);
   }

   protected ScatteredFeaturePiece(StructurePieceType pType, CompoundTag pTag) {
      super(pType, pTag);
      this.width = pTag.getInt("Width");
      this.height = pTag.getInt("Height");
      this.depth = pTag.getInt("Depth");
      this.heightPosition = pTag.getInt("HPos");
   }

   @Override
   protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
      pTag.putInt("Width", this.width);
      pTag.putInt("Height", this.height);
      pTag.putInt("Depth", this.depth);
      pTag.putInt("HPos", this.heightPosition);
   }

   protected boolean updateAverageGroundHeight(LevelAccessor pLevel, BoundingBox pBounds, int pHeight) {
      if (this.heightPosition >= 0) {
         return true;
      } else {
         int $$3 = 0;
         int $$4 = 0;
         BlockPos.MutableBlockPos $$5 = new BlockPos.MutableBlockPos();

         for (int $$6 = this.boundingBox.minZ(); $$6 <= this.boundingBox.maxZ(); $$6++) {
            for (int $$7 = this.boundingBox.minX(); $$7 <= this.boundingBox.maxX(); $$7++) {
               $$5.set($$7, 64, $$6);
               if (pBounds.isInside($$5)) {
                  $$3 += pLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, $$5).getY();
                  $$4++;
               }
            }
         }

         if ($$4 == 0) {
            return false;
         } else {
            this.heightPosition = $$3 / $$4;
            this.boundingBox.move(0, this.heightPosition - this.boundingBox.minY() + pHeight, 0);
            return true;
         }
      }
   }

   protected boolean updateHeightPositionToLowestGroundHeight(LevelAccessor pLevel, int pHeight) {
      if (this.heightPosition >= 0) {
         return true;
      } else {
         int $$2 = pLevel.getMaxBuildHeight();
         boolean $$3 = false;
         BlockPos.MutableBlockPos $$4 = new BlockPos.MutableBlockPos();

         for (int $$5 = this.boundingBox.minZ(); $$5 <= this.boundingBox.maxZ(); $$5++) {
            for (int $$6 = this.boundingBox.minX(); $$6 <= this.boundingBox.maxX(); $$6++) {
               $$4.set($$6, 0, $$5);
               $$2 = Math.min($$2, pLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, $$4).getY());
               $$3 = true;
            }
         }

         if (!$$3) {
            return false;
         } else {
            this.heightPosition = $$2;
            this.boundingBox.move(0, this.heightPosition - this.boundingBox.minY() + pHeight, 0);
            return true;
         }
      }
   }
}
