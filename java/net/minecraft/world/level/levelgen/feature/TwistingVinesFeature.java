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
import net.minecraft.world.level.levelgen.feature.configurations.TwistingVinesConfig;

public class TwistingVinesFeature extends Feature<TwistingVinesConfig> {
   public TwistingVinesFeature(Codec<TwistingVinesConfig> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<TwistingVinesConfig> pContext) {
      WorldGenLevel $$1 = pContext.level();
      BlockPos $$2 = pContext.origin();
      if (isInvalidPlacementLocation($$1, $$2)) {
         return false;
      } else {
         RandomSource $$3 = pContext.random();
         TwistingVinesConfig $$4 = pContext.config();
         int $$5 = $$4.spreadWidth();
         int $$6 = $$4.spreadHeight();
         int $$7 = $$4.maxHeight();
         BlockPos.MutableBlockPos $$8 = new BlockPos.MutableBlockPos();

         for (int $$9 = 0; $$9 < $$5 * $$5; $$9++) {
            $$8.set($$2).move(Mth.nextInt($$3, -$$5, $$5), Mth.nextInt($$3, -$$6, $$6), Mth.nextInt($$3, -$$5, $$5));
            if (findFirstAirBlockAboveGround($$1, $$8) && !isInvalidPlacementLocation($$1, $$8)) {
               int $$10 = Mth.nextInt($$3, 1, $$7);
               if ($$3.nextInt(6) == 0) {
                  $$10 *= 2;
               }

               if ($$3.nextInt(5) == 0) {
                  $$10 = 1;
               }

               int $$11 = 17;
               int $$12 = 25;
               placeWeepingVinesColumn($$1, $$3, $$8, $$10, 17, 25);
            }
         }

         return true;
      }
   }

   private static boolean findFirstAirBlockAboveGround(LevelAccessor pLevel, BlockPos.MutableBlockPos pPos) {
      do {
         pPos.move(0, -1, 0);
         if (pLevel.isOutsideBuildHeight(pPos)) {
            return false;
         }
      } while (pLevel.getBlockState(pPos).isAir());

      pPos.move(0, 1, 0);
      return true;
   }

   public static void placeWeepingVinesColumn(LevelAccessor pLevel, RandomSource pRandom, BlockPos.MutableBlockPos pPos, int pLength, int pMinAge, int pMaxAge) {
      for (int $$6 = 1; $$6 <= pLength; $$6++) {
         if (pLevel.isEmptyBlock(pPos)) {
            if ($$6 == pLength || !pLevel.isEmptyBlock(pPos.above())) {
               pLevel.setBlock(
                  pPos,
                  Blocks.TWISTING_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, Integer.valueOf(Mth.nextInt(pRandom, pMinAge, pMaxAge))),
                  2
               );
               break;
            }

            pLevel.setBlock(pPos, Blocks.TWISTING_VINES_PLANT.defaultBlockState(), 2);
         }

         pPos.move(Direction.UP);
      }
   }

   private static boolean isInvalidPlacementLocation(LevelAccessor pLevel, BlockPos pPos) {
      if (!pLevel.isEmptyBlock(pPos)) {
         return true;
      } else {
         BlockState $$2 = pLevel.getBlockState(pPos.below());
         return !$$2.is(Blocks.NETHERRACK) && !$$2.is(Blocks.WARPED_NYLIUM) && !$$2.is(Blocks.WARPED_WART_BLOCK);
      }
   }
}
