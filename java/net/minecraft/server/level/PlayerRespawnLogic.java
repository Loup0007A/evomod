package net.minecraft.server.level;

import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

public class PlayerRespawnLogic {
   @Nullable
   protected static BlockPos getOverworldRespawnPos(ServerLevel pLevel, int pX, int pZ) {
      boolean $$3 = pLevel.dimensionType().hasCeiling();
      LevelChunk $$4 = pLevel.getChunk(SectionPos.blockToSectionCoord(pX), SectionPos.blockToSectionCoord(pZ));
      int $$5 = $$3 ? pLevel.getChunkSource().getGenerator().getSpawnHeight(pLevel) : $$4.getHeight(Heightmap.Types.MOTION_BLOCKING, pX & 15, pZ & 15);
      if ($$5 < pLevel.getMinBuildHeight()) {
         return null;
      } else {
         int $$6 = $$4.getHeight(Heightmap.Types.WORLD_SURFACE, pX & 15, pZ & 15);
         if ($$6 <= $$5 && $$6 > $$4.getHeight(Heightmap.Types.OCEAN_FLOOR, pX & 15, pZ & 15)) {
            return null;
         } else {
            BlockPos.MutableBlockPos $$7 = new BlockPos.MutableBlockPos();

            for (int $$8 = $$5 + 1; $$8 >= pLevel.getMinBuildHeight(); $$8--) {
               $$7.set(pX, $$8, pZ);
               BlockState $$9 = pLevel.getBlockState($$7);
               if (!$$9.getFluidState().isEmpty()) {
                  break;
               }

               if (Block.isFaceFull($$9.getCollisionShape(pLevel, $$7), Direction.UP)) {
                  return $$7.above().immutable();
               }
            }

            return null;
         }
      }
   }

   @Nullable
   public static BlockPos getSpawnPosInChunk(ServerLevel pLevel, ChunkPos pChunkPos) {
      if (SharedConstants.debugVoidTerrain(pChunkPos)) {
         return null;
      } else {
         for (int $$2 = pChunkPos.getMinBlockX(); $$2 <= pChunkPos.getMaxBlockX(); $$2++) {
            for (int $$3 = pChunkPos.getMinBlockZ(); $$3 <= pChunkPos.getMaxBlockZ(); $$3++) {
               BlockPos $$4 = getOverworldRespawnPos(pLevel, $$2, $$3);
               if ($$4 != null) {
                  return $$4;
               }
            }
         }

         return null;
      }
   }
}
