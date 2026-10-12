package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class BasaltPillarFeature extends Feature<NoneFeatureConfiguration> {
   public BasaltPillarFeature(Codec<NoneFeatureConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> pContext) {
      BlockPos $$1 = pContext.origin();
      WorldGenLevel $$2 = pContext.level();
      RandomSource $$3 = pContext.random();
      if ($$2.isEmptyBlock($$1) && !$$2.isEmptyBlock($$1.above())) {
         BlockPos.MutableBlockPos $$4 = $$1.mutable();
         BlockPos.MutableBlockPos $$5 = $$1.mutable();
         boolean $$6 = true;
         boolean $$7 = true;
         boolean $$8 = true;
         boolean $$9 = true;

         while ($$2.isEmptyBlock($$4)) {
            if ($$2.isOutsideBuildHeight($$4)) {
               return true;
            }

            $$2.setBlock($$4, Blocks.BASALT.defaultBlockState(), 2);
            $$6 = $$6 && this.placeHangOff($$2, $$3, $$5.setWithOffset($$4, Direction.NORTH));
            $$7 = $$7 && this.placeHangOff($$2, $$3, $$5.setWithOffset($$4, Direction.SOUTH));
            $$8 = $$8 && this.placeHangOff($$2, $$3, $$5.setWithOffset($$4, Direction.WEST));
            $$9 = $$9 && this.placeHangOff($$2, $$3, $$5.setWithOffset($$4, Direction.EAST));
            $$4.move(Direction.DOWN);
         }

         $$4.move(Direction.UP);
         this.placeBaseHangOff($$2, $$3, $$5.setWithOffset($$4, Direction.NORTH));
         this.placeBaseHangOff($$2, $$3, $$5.setWithOffset($$4, Direction.SOUTH));
         this.placeBaseHangOff($$2, $$3, $$5.setWithOffset($$4, Direction.WEST));
         this.placeBaseHangOff($$2, $$3, $$5.setWithOffset($$4, Direction.EAST));
         $$4.move(Direction.DOWN);
         BlockPos.MutableBlockPos $$10 = new BlockPos.MutableBlockPos();

         for (int $$11 = -3; $$11 < 4; $$11++) {
            for (int $$12 = -3; $$12 < 4; $$12++) {
               int $$13 = Mth.abs($$11) * Mth.abs($$12);
               if ($$3.nextInt(10) < 10 - $$13) {
                  $$10.set($$4.offset($$11, 0, $$12));
                  int $$14 = 3;

                  while ($$2.isEmptyBlock($$5.setWithOffset($$10, Direction.DOWN))) {
                     $$10.move(Direction.DOWN);
                     if (--$$14 <= 0) {
                        break;
                     }
                  }

                  if (!$$2.isEmptyBlock($$5.setWithOffset($$10, Direction.DOWN))) {
                     $$2.setBlock($$10, Blocks.BASALT.defaultBlockState(), 2);
                  }
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private void placeBaseHangOff(LevelAccessor pLevel, RandomSource pRandom, BlockPos pPos) {
      if (pRandom.nextBoolean()) {
         pLevel.setBlock(pPos, Blocks.BASALT.defaultBlockState(), 2);
      }
   }

   private boolean placeHangOff(LevelAccessor pLevel, RandomSource pRandom, BlockPos pPos) {
      if (pRandom.nextInt(10) != 0) {
         pLevel.setBlock(pPos, Blocks.BASALT.defaultBlockState(), 2);
         return true;
      } else {
         return false;
      }
   }
}
