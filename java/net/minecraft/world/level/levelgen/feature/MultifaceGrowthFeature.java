package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.MultifaceGrowthConfiguration;

public class MultifaceGrowthFeature extends Feature<MultifaceGrowthConfiguration> {
   public MultifaceGrowthFeature(Codec<MultifaceGrowthConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<MultifaceGrowthConfiguration> pContext) {
      WorldGenLevel $$1 = pContext.level();
      BlockPos $$2 = pContext.origin();
      RandomSource $$3 = pContext.random();
      MultifaceGrowthConfiguration $$4 = pContext.config();
      if (!isAirOrWater($$1.getBlockState($$2))) {
         return false;
      } else {
         List<Direction> $$5 = $$4.getShuffledDirections($$3);
         if (placeGrowthIfPossible($$1, $$2, $$1.getBlockState($$2), $$4, $$3, $$5)) {
            return true;
         } else {
            BlockPos.MutableBlockPos $$6 = $$2.mutable();

            for (Direction $$7 : $$5) {
               $$6.set($$2);
               List<Direction> $$8 = $$4.getShuffledDirectionsExcept($$3, $$7.getOpposite());

               for (int $$9 = 0; $$9 < $$4.searchRange; $$9++) {
                  $$6.setWithOffset($$2, $$7);
                  BlockState $$10 = $$1.getBlockState($$6);
                  if (!isAirOrWater($$10) && !$$10.is($$4.placeBlock)) {
                     break;
                  }

                  if (placeGrowthIfPossible($$1, $$6, $$10, $$4, $$3, $$8)) {
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   public static boolean placeGrowthIfPossible(
      WorldGenLevel pLevel, BlockPos pPos, BlockState pState, MultifaceGrowthConfiguration pConfig, RandomSource pRandom, List<Direction> pDirections
   ) {
      BlockPos.MutableBlockPos $$6 = pPos.mutable();

      for (Direction $$7 : pDirections) {
         BlockState $$8 = pLevel.getBlockState($$6.setWithOffset(pPos, $$7));
         if ($$8.is(pConfig.canBePlacedOn)) {
            BlockState $$9 = pConfig.placeBlock.getStateForPlacement(pState, pLevel, pPos, $$7);
            if ($$9 == null) {
               return false;
            }

            pLevel.setBlock(pPos, $$9, 3);
            pLevel.getChunk(pPos).markPosForPostprocessing(pPos);
            if (pRandom.nextFloat() < pConfig.chanceOfSpreading) {
               pConfig.placeBlock.getSpreader().spreadFromFaceTowardRandomDirection($$9, pLevel, pPos, $$7, pRandom, true);
            }

            return true;
         }
      }

      return false;
   }

   private static boolean isAirOrWater(BlockState pState) {
      return pState.isAir() || pState.is(Blocks.WATER);
   }
}
