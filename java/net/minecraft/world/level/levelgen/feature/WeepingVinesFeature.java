package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class WeepingVinesFeature extends Feature<NoneFeatureConfiguration> {
   private static final Direction[] DIRECTIONS = Direction.values();

   public WeepingVinesFeature(Codec<NoneFeatureConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> pContext) {
      WorldGenLevel $$1 = pContext.level();
      BlockPos $$2 = pContext.origin();
      RandomSource $$3 = pContext.random();
      if (!$$1.isEmptyBlock($$2)) {
         return false;
      } else {
         BlockState $$4 = $$1.getBlockState($$2.above());
         if (!$$4.is(Blocks.NETHERRACK) && !$$4.is(Blocks.NETHER_WART_BLOCK)) {
            return false;
         } else {
            this.placeRoofNetherWart($$1, $$3, $$2);
            this.placeRoofWeepingVines($$1, $$3, $$2);
            return true;
         }
      }
   }

   private void placeRoofNetherWart(LevelAccessor pLevel, RandomSource pRandom, BlockPos pPos) {
      pLevel.setBlock(pPos, Blocks.NETHER_WART_BLOCK.defaultBlockState(), 2);
      BlockPos.MutableBlockPos $$3 = new BlockPos.MutableBlockPos();
      BlockPos.MutableBlockPos $$4 = new BlockPos.MutableBlockPos();

      for (int $$5 = 0; $$5 < 200; $$5++) {
         $$3.setWithOffset(pPos, pRandom.nextInt(6) - pRandom.nextInt(6), pRandom.nextInt(2) - pRandom.nextInt(5), pRandom.nextInt(6) - pRandom.nextInt(6));
         if (pLevel.isEmptyBlock($$3)) {
            int $$6 = 0;

            for (Direction $$7 : DIRECTIONS) {
               BlockState $$8 = pLevel.getBlockState($$4.setWithOffset($$3, $$7));
               if ($$8.is(Blocks.NETHERRACK) || $$8.is(Blocks.NETHER_WART_BLOCK)) {
                  $$6++;
               }

               if ($$6 > 1) {
                  break;
               }
            }

            if ($$6 == 1) {
               pLevel.setBlock($$3, Blocks.NETHER_WART_BLOCK.defaultBlockState(), 2);
            }
         }
      }
   }

   private void placeRoofWeepingVines(LevelAccessor pLevel, RandomSource pRandom, BlockPos pPos) {
      BlockPos.MutableBlockPos $$3 = new BlockPos.MutableBlockPos();

      for (int $$4 = 0; $$4 < 100; $$4++) {
         $$3.setWithOffset(pPos, pRandom.nextInt(8) - pRandom.nextInt(8), pRandom.nextInt(2) - pRandom.nextInt(7), pRandom.nextInt(8) - pRandom.nextInt(8));
         if (pLevel.isEmptyBlock($$3)) {
            BlockState $$5 = pLevel.getBlockState($$3.above());
            if ($$5.is(Blocks.NETHERRACK) || $$5.is(Blocks.NETHER_WART_BLOCK)) {
               int $$6 = Mth.nextInt(pRandom, 1, 8);
               if (pRandom.nextInt(6) == 0) {
                  $$6 *= 2;
               }

               if (pRandom.nextInt(5) == 0) {
                  $$6 = 1;
               }

               int $$7 = 17;
               int $$8 = 25;
               placeWeepingVinesColumn(pLevel, pRandom, $$3, $$6, 17, 25);
            }
         }
      }
   }

   public static void placeWeepingVinesColumn(LevelAccessor pLevel, RandomSource pRandom, BlockPos.MutableBlockPos pPos, int pHeight, int pMinAge, int pMaxAge) {
      for (int $$6 = 0; $$6 <= pHeight; $$6++) {
         if (pLevel.isEmptyBlock(pPos)) {
            if ($$6 == pHeight || !pLevel.isEmptyBlock(pPos.below())) {
               pLevel.setBlock(
                  pPos,
                  Blocks.WEEPING_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, Integer.valueOf(Mth.nextInt(pRandom, pMinAge, pMaxAge))),
                  2
               );
               break;
            }

            pLevel.setBlock(pPos, Blocks.WEEPING_VINES_PLANT.defaultBlockState(), 2);
         }

         pPos.move(Direction.DOWN);
      }
   }
}
