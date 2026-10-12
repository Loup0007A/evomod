package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.BlockPileConfiguration;

public class BlockPileFeature extends Feature<BlockPileConfiguration> {
   public BlockPileFeature(Codec<BlockPileConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<BlockPileConfiguration> pContext) {
      BlockPos $$1 = pContext.origin();
      WorldGenLevel $$2 = pContext.level();
      RandomSource $$3 = pContext.random();
      BlockPileConfiguration $$4 = pContext.config();
      if ($$1.getY() < $$2.getMinBuildHeight() + 5) {
         return false;
      } else {
         int $$5 = 2 + $$3.nextInt(2);
         int $$6 = 2 + $$3.nextInt(2);

         for (BlockPos $$7 : BlockPos.betweenClosed($$1.offset(-$$5, 0, -$$6), $$1.offset($$5, 1, $$6))) {
            int $$8 = $$1.getX() - $$7.getX();
            int $$9 = $$1.getZ() - $$7.getZ();
            if ((float)($$8 * $$8 + $$9 * $$9) <= $$3.nextFloat() * 10.0F - $$3.nextFloat() * 6.0F) {
               this.tryPlaceBlock($$2, $$7, $$3, $$4);
            } else if ((double)$$3.nextFloat() < 0.031) {
               this.tryPlaceBlock($$2, $$7, $$3, $$4);
            }
         }

         return true;
      }
   }

   private boolean mayPlaceOn(LevelAccessor pLevel, BlockPos pPos, RandomSource pRandom) {
      BlockPos $$3 = pPos.below();
      BlockState $$4 = pLevel.getBlockState($$3);
      return $$4.is(Blocks.DIRT_PATH) ? pRandom.nextBoolean() : $$4.isFaceSturdy(pLevel, $$3, Direction.UP);
   }

   private void tryPlaceBlock(LevelAccessor pLevel, BlockPos pPos, RandomSource pRandom, BlockPileConfiguration pConfig) {
      if (pLevel.isEmptyBlock(pPos) && this.mayPlaceOn(pLevel, pPos, pRandom)) {
         pLevel.setBlock(pPos, pConfig.stateProvider.getState(pRandom, pPos), 4);
      }
   }
}
