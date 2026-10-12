package net.minecraft.world.level.levelgen.structure.structures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class BuriedTreasurePieces {
   public static class BuriedTreasurePiece extends StructurePiece {
      public BuriedTreasurePiece(BlockPos pPos) {
         super(StructurePieceType.BURIED_TREASURE_PIECE, 0, new BoundingBox(pPos));
      }

      public BuriedTreasurePiece(CompoundTag pTag) {
         super(StructurePieceType.BURIED_TREASURE_PIECE, pTag);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
      }

      @Override
      public void postProcess(
         WorldGenLevel pLevel,
         StructureManager pStructureManager,
         ChunkGenerator pGenerator,
         RandomSource pRandom,
         BoundingBox pBox,
         ChunkPos pChunkPos,
         BlockPos pPos
      ) {
         int $$7 = pLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, this.boundingBox.minX(), this.boundingBox.minZ());
         BlockPos.MutableBlockPos $$8 = new BlockPos.MutableBlockPos(this.boundingBox.minX(), $$7, this.boundingBox.minZ());

         while ($$8.getY() > pLevel.getMinBuildHeight()) {
            BlockState $$9 = pLevel.getBlockState($$8);
            BlockState $$10 = pLevel.getBlockState($$8.below());
            if ($$10 == Blocks.SANDSTONE.defaultBlockState()
               || $$10 == Blocks.STONE.defaultBlockState()
               || $$10 == Blocks.ANDESITE.defaultBlockState()
               || $$10 == Blocks.GRANITE.defaultBlockState()
               || $$10 == Blocks.DIORITE.defaultBlockState()) {
               BlockState $$11 = !$$9.isAir() && !this.isLiquid($$9) ? $$9 : Blocks.SAND.defaultBlockState();

               for (Direction $$12 : Direction.values()) {
                  BlockPos $$13 = $$8.relative($$12);
                  BlockState $$14 = pLevel.getBlockState($$13);
                  if ($$14.isAir() || this.isLiquid($$14)) {
                     BlockPos $$15 = $$13.below();
                     BlockState $$16 = pLevel.getBlockState($$15);
                     if (($$16.isAir() || this.isLiquid($$16)) && $$12 != Direction.UP) {
                        pLevel.setBlock($$13, $$10, 3);
                     } else {
                        pLevel.setBlock($$13, $$11, 3);
                     }
                  }
               }

               this.boundingBox = new BoundingBox($$8);
               this.createChest(pLevel, pBox, pRandom, $$8, BuiltInLootTables.BURIED_TREASURE, null);
               return;
            }

            $$8.move(0, -1, 0);
         }
      }

      private boolean isLiquid(BlockState pState) {
         return pState == Blocks.WATER.defaultBlockState() || pState == Blocks.LAVA.defaultBlockState();
      }
   }
}
