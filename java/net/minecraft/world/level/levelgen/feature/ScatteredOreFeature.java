package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

public class ScatteredOreFeature extends Feature<OreConfiguration> {
   private static final int MAX_DIST_FROM_ORIGIN = 7;

   ScatteredOreFeature(Codec<OreConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<OreConfiguration> pContext) {
      WorldGenLevel $$1 = pContext.level();
      RandomSource $$2 = pContext.random();
      OreConfiguration $$3 = pContext.config();
      BlockPos $$4 = pContext.origin();
      int $$5 = $$2.nextInt($$3.size + 1);
      BlockPos.MutableBlockPos $$6 = new BlockPos.MutableBlockPos();

      for (int $$7 = 0; $$7 < $$5; $$7++) {
         this.offsetTargetPos($$6, $$2, $$4, Math.min($$7, 7));
         BlockState $$8 = $$1.getBlockState($$6);

         for (OreConfiguration.TargetBlockState $$9 : $$3.targetStates) {
            if (OreFeature.canPlaceOre($$8, $$1::getBlockState, $$2, $$3, $$9, $$6)) {
               $$1.setBlock($$6, $$9.state, 2);
               break;
            }
         }
      }

      return true;
   }

   private void offsetTargetPos(BlockPos.MutableBlockPos pMutablePos, RandomSource pRandom, BlockPos pPos, int pMagnitude) {
      int $$4 = this.getRandomPlacementInOneAxisRelativeToOrigin(pRandom, pMagnitude);
      int $$5 = this.getRandomPlacementInOneAxisRelativeToOrigin(pRandom, pMagnitude);
      int $$6 = this.getRandomPlacementInOneAxisRelativeToOrigin(pRandom, pMagnitude);
      pMutablePos.setWithOffset(pPos, $$4, $$5, $$6);
   }

   private int getRandomPlacementInOneAxisRelativeToOrigin(RandomSource pRandom, int pMagnitude) {
      return Math.round((pRandom.nextFloat() - pRandom.nextFloat()) * (float)pMagnitude);
   }
}
