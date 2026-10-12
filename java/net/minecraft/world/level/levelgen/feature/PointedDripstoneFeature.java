package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.configurations.PointedDripstoneConfiguration;

public class PointedDripstoneFeature extends Feature<PointedDripstoneConfiguration> {
   public PointedDripstoneFeature(Codec<PointedDripstoneConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<PointedDripstoneConfiguration> pContext) {
      LevelAccessor $$1 = pContext.level();
      BlockPos $$2 = pContext.origin();
      RandomSource $$3 = pContext.random();
      PointedDripstoneConfiguration $$4 = pContext.config();
      Optional<Direction> $$5 = getTipDirection($$1, $$2, $$3);
      if ($$5.isEmpty()) {
         return false;
      } else {
         BlockPos $$6 = $$2.relative($$5.get().getOpposite());
         createPatchOfDripstoneBlocks($$1, $$3, $$6, $$4);
         int $$7 = $$3.nextFloat() < $$4.chanceOfTallerDripstone && DripstoneUtils.isEmptyOrWater($$1.getBlockState($$2.relative($$5.get()))) ? 2 : 1;
         DripstoneUtils.growPointedDripstone($$1, $$2, $$5.get(), $$7, false);
         return true;
      }
   }

   private static Optional<Direction> getTipDirection(LevelAccessor pLevel, BlockPos pPos, RandomSource pRandom) {
      boolean $$3 = DripstoneUtils.isDripstoneBase(pLevel.getBlockState(pPos.above()));
      boolean $$4 = DripstoneUtils.isDripstoneBase(pLevel.getBlockState(pPos.below()));
      if ($$3 && $$4) {
         return Optional.of(pRandom.nextBoolean() ? Direction.DOWN : Direction.UP);
      } else if ($$3) {
         return Optional.of(Direction.DOWN);
      } else {
         return $$4 ? Optional.of(Direction.UP) : Optional.empty();
      }
   }

   private static void createPatchOfDripstoneBlocks(LevelAccessor pLevel, RandomSource pRandom, BlockPos pPos, PointedDripstoneConfiguration pConfig) {
      DripstoneUtils.placeDripstoneBlockIfPossible(pLevel, pPos);

      for (Direction $$4 : Direction.Plane.HORIZONTAL) {
         if (!(pRandom.nextFloat() > pConfig.chanceOfDirectionalSpread)) {
            BlockPos $$5 = pPos.relative($$4);
            DripstoneUtils.placeDripstoneBlockIfPossible(pLevel, $$5);
            if (!(pRandom.nextFloat() > pConfig.chanceOfSpreadRadius2)) {
               BlockPos $$6 = $$5.relative(Direction.getRandom(pRandom));
               DripstoneUtils.placeDripstoneBlockIfPossible(pLevel, $$6);
               if (!(pRandom.nextFloat() > pConfig.chanceOfSpreadRadius3)) {
                  BlockPos $$7 = $$6.relative(Direction.getRandom(pRandom));
                  DripstoneUtils.placeDripstoneBlockIfPossible(pLevel, $$7);
               }
            }
         }
      }
   }
}
