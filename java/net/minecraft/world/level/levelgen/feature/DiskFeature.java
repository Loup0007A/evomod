package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;

public class DiskFeature extends Feature<DiskConfiguration> {
   public DiskFeature(Codec<DiskConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<DiskConfiguration> pContext) {
      DiskConfiguration $$1 = pContext.config();
      BlockPos $$2 = pContext.origin();
      WorldGenLevel $$3 = pContext.level();
      RandomSource $$4 = pContext.random();
      boolean $$5 = false;
      int $$6 = $$2.getY();
      int $$7 = $$6 + $$1.halfHeight();
      int $$8 = $$6 - $$1.halfHeight() - 1;
      int $$9 = $$1.radius().sample($$4);
      BlockPos.MutableBlockPos $$10 = new BlockPos.MutableBlockPos();

      for (BlockPos $$11 : BlockPos.betweenClosed($$2.offset(-$$9, 0, -$$9), $$2.offset($$9, 0, $$9))) {
         int $$12 = $$11.getX() - $$2.getX();
         int $$13 = $$11.getZ() - $$2.getZ();
         if ($$12 * $$12 + $$13 * $$13 <= $$9 * $$9) {
            $$5 |= this.placeColumn($$1, $$3, $$4, $$7, $$8, $$10.set($$11));
         }
      }

      return $$5;
   }

   protected boolean placeColumn(DiskConfiguration pConfig, WorldGenLevel pLevel, RandomSource pRandom, int pMaxY, int pMinY, BlockPos.MutableBlockPos pPos) {
      boolean $$6 = false;

      for (int $$7 = pMaxY; $$7 > pMinY; $$7--) {
         pPos.setY($$7);
         if (pConfig.target().test(pLevel, pPos)) {
            BlockState $$8 = pConfig.stateProvider().getState(pLevel, pRandom, pPos);
            pLevel.setBlock(pPos, $$8, 2);
            this.markAboveForPostProcessing(pLevel, pPos);
            $$6 = true;
         }
      }

      return $$6;
   }
}
