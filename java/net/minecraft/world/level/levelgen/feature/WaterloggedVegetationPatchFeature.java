package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;

public class WaterloggedVegetationPatchFeature extends VegetationPatchFeature {
   public WaterloggedVegetationPatchFeature(Codec<VegetationPatchConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   protected Set<BlockPos> placeGroundPatch(
      WorldGenLevel pLevel, VegetationPatchConfiguration pConfig, RandomSource pRandom, BlockPos pPos, Predicate<BlockState> pState, int pXRadius, int pZRadius
   ) {
      Set<BlockPos> $$7 = super.placeGroundPatch(pLevel, pConfig, pRandom, pPos, pState, pXRadius, pZRadius);
      Set<BlockPos> $$8 = new HashSet<>();
      BlockPos.MutableBlockPos $$9 = new BlockPos.MutableBlockPos();

      for (BlockPos $$10 : $$7) {
         if (!isExposed(pLevel, $$7, $$10, $$9)) {
            $$8.add($$10);
         }
      }

      for (BlockPos $$11 : $$8) {
         pLevel.setBlock($$11, Blocks.WATER.defaultBlockState(), 2);
      }

      return $$8;
   }

   private static boolean isExposed(WorldGenLevel pLevel, Set<BlockPos> pPositions, BlockPos pPos, BlockPos.MutableBlockPos pMutablePos) {
      return isExposedDirection(pLevel, pPos, pMutablePos, Direction.NORTH)
         || isExposedDirection(pLevel, pPos, pMutablePos, Direction.EAST)
         || isExposedDirection(pLevel, pPos, pMutablePos, Direction.SOUTH)
         || isExposedDirection(pLevel, pPos, pMutablePos, Direction.WEST)
         || isExposedDirection(pLevel, pPos, pMutablePos, Direction.DOWN);
   }

   private static boolean isExposedDirection(WorldGenLevel pLevel, BlockPos pPos, BlockPos.MutableBlockPos pMutablePos, Direction pDirection) {
      pMutablePos.setWithOffset(pPos, pDirection);
      return !pLevel.getBlockState(pMutablePos).isFaceSturdy(pLevel, pMutablePos, pDirection.getOpposite());
   }

   @Override
   protected boolean placeVegetation(
      WorldGenLevel pLevel, VegetationPatchConfiguration pConfig, ChunkGenerator pChunkGenerator, RandomSource pRandom, BlockPos pPos
   ) {
      if (super.placeVegetation(pLevel, pConfig, pChunkGenerator, pRandom, pPos.below())) {
         BlockState $$5 = pLevel.getBlockState(pPos);
         if ($$5.hasProperty(BlockStateProperties.WATERLOGGED) && !$$5.getValue(BlockStateProperties.WATERLOGGED)) {
            pLevel.setBlock(pPos, $$5.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(true)), 2);
         }

         return true;
      } else {
         return false;
      }
   }
}
