package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Column;
import net.minecraft.world.level.levelgen.feature.configurations.UnderwaterMagmaConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class UnderwaterMagmaFeature extends Feature<UnderwaterMagmaConfiguration> {
   public UnderwaterMagmaFeature(Codec<UnderwaterMagmaConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<UnderwaterMagmaConfiguration> pContext) {
      WorldGenLevel $$1 = pContext.level();
      BlockPos $$2 = pContext.origin();
      UnderwaterMagmaConfiguration $$3 = pContext.config();
      RandomSource $$4 = pContext.random();
      OptionalInt $$5 = getFloorY($$1, $$2, $$3);
      if ($$5.isEmpty()) {
         return false;
      } else {
         BlockPos $$6 = $$2.atY($$5.getAsInt());
         Vec3i $$7 = new Vec3i($$3.placementRadiusAroundFloor, $$3.placementRadiusAroundFloor, $$3.placementRadiusAroundFloor);
         BoundingBox $$8 = BoundingBox.fromCorners($$6.subtract($$7), $$6.offset($$7));
         return BlockPos.betweenClosedStream($$8)
               .filter(p_225310_ -> $$4.nextFloat() < $$3.placementProbabilityPerValidPosition)
               .filter(p_160584_ -> this.isValidPlacement($$1, p_160584_))
               .mapToInt(p_160579_ -> {
                  $$1.setBlock(p_160579_, Blocks.MAGMA_BLOCK.defaultBlockState(), 2);
                  return 1;
               })
               .sum()
            > 0;
      }
   }

   private static OptionalInt getFloorY(WorldGenLevel pLevel, BlockPos pPos, UnderwaterMagmaConfiguration pConfig) {
      Predicate<BlockState> $$3 = p_160586_ -> p_160586_.is(Blocks.WATER);
      Predicate<BlockState> $$4 = p_160581_ -> !p_160581_.is(Blocks.WATER);
      Optional<Column> $$5 = Column.scan(pLevel, pPos, pConfig.floorSearchRange, $$3, $$4);
      return $$5.map(Column::getFloor).orElseGet(OptionalInt::empty);
   }

   private boolean isValidPlacement(WorldGenLevel pLevel, BlockPos pPos) {
      if (!this.isWaterOrAir(pLevel, pPos) && !this.isWaterOrAir(pLevel, pPos.below())) {
         for (Direction $$2 : Direction.Plane.HORIZONTAL) {
            if (this.isWaterOrAir(pLevel, pPos.relative($$2))) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean isWaterOrAir(LevelAccessor pLevel, BlockPos pPos) {
      BlockState $$2 = pLevel.getBlockState(pPos);
      return $$2.is(Blocks.WATER) || $$2.isAir();
   }
}
