package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public abstract class AbstractHugeMushroomFeature extends Feature<HugeMushroomFeatureConfiguration> {
   public AbstractHugeMushroomFeature(Codec<HugeMushroomFeatureConfiguration> pCodec) {
      super(pCodec);
   }

   protected void placeTrunk(
      LevelAccessor pLevel, RandomSource pRandom, BlockPos pPos, HugeMushroomFeatureConfiguration pConfig, int pMaxHeight, BlockPos.MutableBlockPos pMutablePos
   ) {
      for (int $$6 = 0; $$6 < pMaxHeight; $$6++) {
         pMutablePos.set(pPos).move(Direction.UP, $$6);
         if (!pLevel.getBlockState(pMutablePos).isSolidRender(pLevel, pMutablePos)) {
            this.setBlock(pLevel, pMutablePos, pConfig.stemProvider.getState(pRandom, pPos));
         }
      }
   }

   protected int getTreeHeight(RandomSource pRandom) {
      int $$1 = pRandom.nextInt(3) + 4;
      if (pRandom.nextInt(12) == 0) {
         $$1 *= 2;
      }

      return $$1;
   }

   protected boolean isValidPosition(
      LevelAccessor pLevel, BlockPos pPos, int pMaxHeight, BlockPos.MutableBlockPos pMutablePos, HugeMushroomFeatureConfiguration pConfig
   ) {
      int $$5 = pPos.getY();
      if ($$5 >= pLevel.getMinBuildHeight() + 1 && $$5 + pMaxHeight + 1 < pLevel.getMaxBuildHeight()) {
         BlockState $$6 = pLevel.getBlockState(pPos.below());
         if (!isDirt($$6) && !$$6.is(BlockTags.MUSHROOM_GROW_BLOCK)) {
            return false;
         } else {
            for (int $$7 = 0; $$7 <= pMaxHeight; $$7++) {
               int $$8 = this.getTreeRadiusForHeight(-1, -1, pConfig.foliageRadius, $$7);

               for (int $$9 = -$$8; $$9 <= $$8; $$9++) {
                  for (int $$10 = -$$8; $$10 <= $$8; $$10++) {
                     BlockState $$11 = pLevel.getBlockState(pMutablePos.setWithOffset(pPos, $$9, $$7, $$10));
                     if (!$$11.isAir() && !$$11.is(BlockTags.LEAVES)) {
                        return false;
                     }
                  }
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean place(FeaturePlaceContext<HugeMushroomFeatureConfiguration> pContext) {
      WorldGenLevel $$1 = pContext.level();
      BlockPos $$2 = pContext.origin();
      RandomSource $$3 = pContext.random();
      HugeMushroomFeatureConfiguration $$4 = pContext.config();
      int $$5 = this.getTreeHeight($$3);
      BlockPos.MutableBlockPos $$6 = new BlockPos.MutableBlockPos();
      if (!this.isValidPosition($$1, $$2, $$5, $$6, $$4)) {
         return false;
      } else {
         this.makeCap($$1, $$3, $$2, $$5, $$6, $$4);
         this.placeTrunk($$1, $$3, $$2, $$4, $$5, $$6);
         return true;
      }
   }

   protected abstract int getTreeRadiusForHeight(int p_65094_, int pHeight, int pFoliageRadius, int pY);

   protected abstract void makeCap(
      LevelAccessor pLevel,
      RandomSource pRandom,
      BlockPos pPos,
      int pTreeHeight,
      BlockPos.MutableBlockPos pMutablePos,
      HugeMushroomFeatureConfiguration pConfig
   );
}
