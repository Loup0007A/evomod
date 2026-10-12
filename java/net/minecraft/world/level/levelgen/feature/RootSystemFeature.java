package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.RootSystemConfiguration;

public class RootSystemFeature extends Feature<RootSystemConfiguration> {
   public RootSystemFeature(Codec<RootSystemConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<RootSystemConfiguration> pContext) {
      WorldGenLevel $$1 = pContext.level();
      BlockPos $$2 = pContext.origin();
      if (!$$1.getBlockState($$2).isAir()) {
         return false;
      } else {
         RandomSource $$3 = pContext.random();
         BlockPos $$4 = pContext.origin();
         RootSystemConfiguration $$5 = pContext.config();
         BlockPos.MutableBlockPos $$6 = $$4.mutable();
         if (placeDirtAndTree($$1, pContext.chunkGenerator(), $$5, $$3, $$6, $$4)) {
            placeRoots($$1, $$5, $$3, $$4, $$6);
         }

         return true;
      }
   }

   private static boolean spaceForTree(WorldGenLevel pLevel, RootSystemConfiguration pConfig, BlockPos pPos) {
      BlockPos.MutableBlockPos $$3 = pPos.mutable();

      for (int $$4 = 1; $$4 <= pConfig.requiredVerticalSpaceForTree; $$4++) {
         $$3.move(Direction.UP);
         BlockState $$5 = pLevel.getBlockState($$3);
         if (!isAllowedTreeSpace($$5, $$4, pConfig.allowedVerticalWaterForTree)) {
            return false;
         }
      }

      return true;
   }

   private static boolean isAllowedTreeSpace(BlockState pState, int pY, int pAllowedVerticalWater) {
      if (pState.isAir()) {
         return true;
      } else {
         int $$3 = pY + 1;
         return $$3 <= pAllowedVerticalWater && pState.getFluidState().is(FluidTags.WATER);
      }
   }

   private static boolean placeDirtAndTree(
      WorldGenLevel pLevel,
      ChunkGenerator pChunkGenerator,
      RootSystemConfiguration pConfig,
      RandomSource pRandom,
      BlockPos.MutableBlockPos pMutablePos,
      BlockPos pBasePos
   ) {
      for (int $$6 = 0; $$6 < pConfig.rootColumnMaxHeight; $$6++) {
         pMutablePos.move(Direction.UP);
         if (pConfig.allowedTreePosition.test(pLevel, pMutablePos) && spaceForTree(pLevel, pConfig, pMutablePos)) {
            BlockPos $$7 = pMutablePos.below();
            if (pLevel.getFluidState($$7).is(FluidTags.LAVA) || !pLevel.getBlockState($$7).isSolid()) {
               return false;
            }

            if (pConfig.treeFeature.value().place(pLevel, pChunkGenerator, pRandom, pMutablePos)) {
               placeDirt(pBasePos, pBasePos.getY() + $$6, pLevel, pConfig, pRandom);
               return true;
            }
         }
      }

      return false;
   }

   private static void placeDirt(BlockPos pPos, int pMaxY, WorldGenLevel pLevel, RootSystemConfiguration pConfig, RandomSource pRandom) {
      int $$5 = pPos.getX();
      int $$6 = pPos.getZ();
      BlockPos.MutableBlockPos $$7 = pPos.mutable();

      for (int $$8 = pPos.getY(); $$8 < pMaxY; $$8++) {
         placeRootedDirt(pLevel, pConfig, pRandom, $$5, $$6, $$7.set($$5, $$8, $$6));
      }
   }

   private static void placeRootedDirt(
      WorldGenLevel pLevel, RootSystemConfiguration pConfig, RandomSource pRandom, int pX, int pZ, BlockPos.MutableBlockPos pPos
   ) {
      int $$6 = pConfig.rootRadius;
      Predicate<BlockState> $$7 = p_204762_ -> p_204762_.is(pConfig.rootReplaceable);

      for (int $$8 = 0; $$8 < pConfig.rootPlacementAttempts; $$8++) {
         pPos.setWithOffset(pPos, pRandom.nextInt($$6) - pRandom.nextInt($$6), 0, pRandom.nextInt($$6) - pRandom.nextInt($$6));
         if ($$7.test(pLevel.getBlockState(pPos))) {
            pLevel.setBlock(pPos, pConfig.rootStateProvider.getState(pRandom, pPos), 2);
         }

         pPos.setX(pX);
         pPos.setZ(pZ);
      }
   }

   private static void placeRoots(
      WorldGenLevel pLevel, RootSystemConfiguration pConfig, RandomSource pRandom, BlockPos pBasePos, BlockPos.MutableBlockPos pMutablePos
   ) {
      int $$5 = pConfig.hangingRootRadius;
      int $$6 = pConfig.hangingRootsVerticalSpan;

      for (int $$7 = 0; $$7 < pConfig.hangingRootPlacementAttempts; $$7++) {
         pMutablePos.setWithOffset(
            pBasePos, pRandom.nextInt($$5) - pRandom.nextInt($$5), pRandom.nextInt($$6) - pRandom.nextInt($$6), pRandom.nextInt($$5) - pRandom.nextInt($$5)
         );
         if (pLevel.isEmptyBlock(pMutablePos)) {
            BlockState $$8 = pConfig.hangingRootStateProvider.getState(pRandom, pMutablePos);
            if ($$8.canSurvive(pLevel, pMutablePos) && pLevel.getBlockState(pMutablePos.above()).isFaceSturdy(pLevel, pMutablePos, Direction.DOWN)) {
               pLevel.setBlock(pMutablePos, $$8, 2);
            }
         }
      }
   }
}
