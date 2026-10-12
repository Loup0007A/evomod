package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ClampedNormalFloat;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.feature.configurations.DripstoneClusterConfiguration;

public class DripstoneClusterFeature extends Feature<DripstoneClusterConfiguration> {
   public DripstoneClusterFeature(Codec<DripstoneClusterConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<DripstoneClusterConfiguration> pContext) {
      WorldGenLevel $$1 = pContext.level();
      BlockPos $$2 = pContext.origin();
      DripstoneClusterConfiguration $$3 = pContext.config();
      RandomSource $$4 = pContext.random();
      if (!DripstoneUtils.isEmptyOrWater($$1, $$2)) {
         return false;
      } else {
         int $$5 = $$3.height.sample($$4);
         float $$6 = $$3.wetness.sample($$4);
         float $$7 = $$3.density.sample($$4);
         int $$8 = $$3.radius.sample($$4);
         int $$9 = $$3.radius.sample($$4);

         for (int $$10 = -$$8; $$10 <= $$8; $$10++) {
            for (int $$11 = -$$9; $$11 <= $$9; $$11++) {
               double $$12 = this.getChanceOfStalagmiteOrStalactite($$8, $$9, $$10, $$11, $$3);
               BlockPos $$13 = $$2.offset($$10, 0, $$11);
               this.placeColumn($$1, $$4, $$13, $$10, $$11, $$6, $$12, $$5, $$7, $$3);
            }
         }

         return true;
      }
   }

   private void placeColumn(
      WorldGenLevel pLevel,
      RandomSource pRandom,
      BlockPos pPos,
      int pX,
      int pZ,
      float pWetness,
      double pChance,
      int pHeight,
      float pDensity,
      DripstoneClusterConfiguration pConfig
   ) {
      Optional<Column> $$10 = Column.scan(
         pLevel, pPos, pConfig.floorToCeilingSearchRange, DripstoneUtils::isEmptyOrWater, DripstoneUtils::isNeitherEmptyNorWater
      );
      if (!$$10.isEmpty()) {
         OptionalInt $$11 = $$10.get().getCeiling();
         OptionalInt $$12 = $$10.get().getFloor();
         if (!$$11.isEmpty() || !$$12.isEmpty()) {
            boolean $$13 = pRandom.nextFloat() < pWetness;
            Column $$15;
            if ($$13 && $$12.isPresent() && this.canPlacePool(pLevel, pPos.atY($$12.getAsInt()))) {
               int $$14 = $$12.getAsInt();
               $$15 = $$10.get().withFloor(OptionalInt.of($$14 - 1));
               pLevel.setBlock(pPos.atY($$14), Blocks.WATER.defaultBlockState(), 2);
            } else {
               $$15 = $$10.get();
            }

            OptionalInt $$17 = $$15.getFloor();
            boolean $$18 = pRandom.nextDouble() < pChance;
            int $$22;
            if ($$11.isPresent() && $$18 && !this.isLava(pLevel, pPos.atY($$11.getAsInt()))) {
               int $$19 = pConfig.dripstoneBlockLayerThickness.sample(pRandom);
               this.replaceBlocksWithDripstoneBlocks(pLevel, pPos.atY($$11.getAsInt()), $$19, Direction.UP);
               int $$20;
               if ($$17.isPresent()) {
                  $$20 = Math.min(pHeight, $$11.getAsInt() - $$17.getAsInt());
               } else {
                  $$20 = pHeight;
               }

               $$22 = this.getDripstoneHeight(pRandom, pX, pZ, pDensity, $$20, pConfig);
            } else {
               $$22 = 0;
            }

            boolean $$24 = pRandom.nextDouble() < pChance;
            int $$26;
            if ($$17.isPresent() && $$24 && !this.isLava(pLevel, pPos.atY($$17.getAsInt()))) {
               int $$25 = pConfig.dripstoneBlockLayerThickness.sample(pRandom);
               this.replaceBlocksWithDripstoneBlocks(pLevel, pPos.atY($$17.getAsInt()), $$25, Direction.DOWN);
               if ($$11.isPresent()) {
                  $$26 = Math.max(
                     0, $$22 + Mth.randomBetweenInclusive(pRandom, -pConfig.maxStalagmiteStalactiteHeightDiff, pConfig.maxStalagmiteStalactiteHeightDiff)
                  );
               } else {
                  $$26 = this.getDripstoneHeight(pRandom, pX, pZ, pDensity, pHeight, pConfig);
               }
            } else {
               $$26 = 0;
            }

            int $$36;
            int $$35;
            if ($$11.isPresent() && $$17.isPresent() && $$11.getAsInt() - $$22 <= $$17.getAsInt() + $$26) {
               int $$29 = $$17.getAsInt();
               int $$30 = $$11.getAsInt();
               int $$31 = Math.max($$30 - $$22, $$29 + 1);
               int $$32 = Math.min($$29 + $$26, $$30 - 1);
               int $$33 = Mth.randomBetweenInclusive(pRandom, $$31, $$32 + 1);
               int $$34 = $$33 - 1;
               $$35 = $$30 - $$33;
               $$36 = $$34 - $$29;
            } else {
               $$35 = $$22;
               $$36 = $$26;
            }

            boolean $$39 = pRandom.nextBoolean() && $$35 > 0 && $$36 > 0 && $$15.getHeight().isPresent() && $$35 + $$36 == $$15.getHeight().getAsInt();
            if ($$11.isPresent()) {
               DripstoneUtils.growPointedDripstone(pLevel, pPos.atY($$11.getAsInt() - 1), Direction.DOWN, $$35, $$39);
            }

            if ($$17.isPresent()) {
               DripstoneUtils.growPointedDripstone(pLevel, pPos.atY($$17.getAsInt() + 1), Direction.UP, $$36, $$39);
            }
         }
      }
   }

   private boolean isLava(LevelReader pLevel, BlockPos pPos) {
      return pLevel.getBlockState(pPos).is(Blocks.LAVA);
   }

   private int getDripstoneHeight(RandomSource pRandom, int pX, int pZ, float pChance, int pHeight, DripstoneClusterConfiguration pConfig) {
      if (pRandom.nextFloat() > pChance) {
         return 0;
      } else {
         int $$6 = Math.abs(pX) + Math.abs(pZ);
         float $$7 = (float)Mth.clampedMap((double)$$6, 0.0, (double)pConfig.maxDistanceFromCenterAffectingHeightBias, (double)pHeight / 2.0, 0.0);
         return (int)randomBetweenBiased(pRandom, 0.0F, (float)pHeight, $$7, (float)pConfig.heightDeviation);
      }
   }

   private boolean canPlacePool(WorldGenLevel pLevel, BlockPos pPos) {
      BlockState $$2 = pLevel.getBlockState(pPos);
      if (!$$2.is(Blocks.WATER) && !$$2.is(Blocks.DRIPSTONE_BLOCK) && !$$2.is(Blocks.POINTED_DRIPSTONE)) {
         if (pLevel.getBlockState(pPos.above()).getFluidState().is(FluidTags.WATER)) {
            return false;
         } else {
            for (Direction $$3 : Direction.Plane.HORIZONTAL) {
               if (!this.canBeAdjacentToWater(pLevel, pPos.relative($$3))) {
                  return false;
               }
            }

            return this.canBeAdjacentToWater(pLevel, pPos.below());
         }
      } else {
         return false;
      }
   }

   private boolean canBeAdjacentToWater(LevelAccessor pLevel, BlockPos pPos) {
      BlockState $$2 = pLevel.getBlockState(pPos);
      return $$2.is(BlockTags.BASE_STONE_OVERWORLD) || $$2.getFluidState().is(FluidTags.WATER);
   }

   private void replaceBlocksWithDripstoneBlocks(WorldGenLevel pLevel, BlockPos pPos, int pThickness, Direction pDirection) {
      BlockPos.MutableBlockPos $$4 = pPos.mutable();

      for (int $$5 = 0; $$5 < pThickness; $$5++) {
         if (!DripstoneUtils.placeDripstoneBlockIfPossible(pLevel, $$4)) {
            return;
         }

         $$4.move(pDirection);
      }
   }

   private double getChanceOfStalagmiteOrStalactite(int pXRadius, int pZRadius, int pX, int pZ, DripstoneClusterConfiguration pConfig) {
      int $$5 = pXRadius - Math.abs(pX);
      int $$6 = pZRadius - Math.abs(pZ);
      int $$7 = Math.min($$5, $$6);
      return (double)Mth.clampedMap(
         (float)$$7, 0.0F, (float)pConfig.maxDistanceFromEdgeAffectingChanceOfDripstoneColumn, pConfig.chanceOfDripstoneColumnAtMaxDistanceFromCenter, 1.0F
      );
   }

   private static float randomBetweenBiased(RandomSource pRandom, float pMin, float pMax, float pMean, float pDeviation) {
      return ClampedNormalFloat.sample(pRandom, pMean, pDeviation, pMin, pMax);
   }
}
