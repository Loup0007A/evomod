package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.ColumnFeatureConfiguration;

public class BasaltColumnsFeature extends Feature<ColumnFeatureConfiguration> {
   private static final ImmutableList<Block> CANNOT_PLACE_ON = ImmutableList.of(
      Blocks.LAVA,
      Blocks.BEDROCK,
      Blocks.MAGMA_BLOCK,
      Blocks.SOUL_SAND,
      Blocks.NETHER_BRICKS,
      Blocks.NETHER_BRICK_FENCE,
      Blocks.NETHER_BRICK_STAIRS,
      Blocks.NETHER_WART,
      Blocks.CHEST,
      Blocks.SPAWNER
   );
   private static final int CLUSTERED_REACH = 5;
   private static final int CLUSTERED_SIZE = 50;
   private static final int UNCLUSTERED_REACH = 8;
   private static final int UNCLUSTERED_SIZE = 15;

   public BasaltColumnsFeature(Codec<ColumnFeatureConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<ColumnFeatureConfiguration> pContext) {
      int $$1 = pContext.chunkGenerator().getSeaLevel();
      BlockPos $$2 = pContext.origin();
      WorldGenLevel $$3 = pContext.level();
      RandomSource $$4 = pContext.random();
      ColumnFeatureConfiguration $$5 = pContext.config();
      if (!canPlaceAt($$3, $$1, $$2.mutable())) {
         return false;
      } else {
         int $$6 = $$5.height().sample($$4);
         boolean $$7 = $$4.nextFloat() < 0.9F;
         int $$8 = Math.min($$6, $$7 ? 5 : 8);
         int $$9 = $$7 ? 50 : 15;
         boolean $$10 = false;

         for (BlockPos $$11 : BlockPos.randomBetweenClosed(
            $$4, $$9, $$2.getX() - $$8, $$2.getY(), $$2.getZ() - $$8, $$2.getX() + $$8, $$2.getY(), $$2.getZ() + $$8
         )) {
            int $$12 = $$6 - $$11.distManhattan($$2);
            if ($$12 >= 0) {
               $$10 |= this.placeColumn($$3, $$1, $$11, $$12, $$5.reach().sample($$4));
            }
         }

         return $$10;
      }
   }

   private boolean placeColumn(LevelAccessor pLevel, int pSeaLevel, BlockPos pPos, int pDistance, int pReach) {
      boolean $$5 = false;

      for (BlockPos $$6 : BlockPos.betweenClosed(
         pPos.getX() - pReach, pPos.getY(), pPos.getZ() - pReach, pPos.getX() + pReach, pPos.getY(), pPos.getZ() + pReach
      )) {
         int $$7 = $$6.distManhattan(pPos);
         BlockPos $$8 = isAirOrLavaOcean(pLevel, pSeaLevel, $$6) ? findSurface(pLevel, pSeaLevel, $$6.mutable(), $$7) : findAir(pLevel, $$6.mutable(), $$7);
         if ($$8 != null) {
            int $$9 = pDistance - $$7 / 2;

            for (BlockPos.MutableBlockPos $$10 = $$8.mutable(); $$9 >= 0; $$9--) {
               if (isAirOrLavaOcean(pLevel, pSeaLevel, $$10)) {
                  this.setBlock(pLevel, $$10, Blocks.BASALT.defaultBlockState());
                  $$10.move(Direction.UP);
                  $$5 = true;
               } else {
                  if (!pLevel.getBlockState($$10).is(Blocks.BASALT)) {
                     break;
                  }

                  $$10.move(Direction.UP);
               }
            }
         }
      }

      return $$5;
   }

   @Nullable
   private static BlockPos findSurface(LevelAccessor pLevel, int pSeaLevel, BlockPos.MutableBlockPos pPos, int pDistance) {
      while (pPos.getY() > pLevel.getMinBuildHeight() + 1 && pDistance > 0) {
         pDistance--;
         if (canPlaceAt(pLevel, pSeaLevel, pPos)) {
            return pPos;
         }

         pPos.move(Direction.DOWN);
      }

      return null;
   }

   private static boolean canPlaceAt(LevelAccessor pLevel, int pSeaLevel, BlockPos.MutableBlockPos pPos) {
      if (!isAirOrLavaOcean(pLevel, pSeaLevel, pPos)) {
         return false;
      } else {
         BlockState $$3 = pLevel.getBlockState(pPos.move(Direction.DOWN));
         pPos.move(Direction.UP);
         return !$$3.isAir() && !CANNOT_PLACE_ON.contains($$3.getBlock());
      }
   }

   @Nullable
   private static BlockPos findAir(LevelAccessor pLevel, BlockPos.MutableBlockPos pPos, int pDistance) {
      while (pPos.getY() < pLevel.getMaxBuildHeight() && pDistance > 0) {
         pDistance--;
         BlockState $$3 = pLevel.getBlockState(pPos);
         if (CANNOT_PLACE_ON.contains($$3.getBlock())) {
            return null;
         }

         if ($$3.isAir()) {
            return pPos;
         }

         pPos.move(Direction.UP);
      }

      return null;
   }

   private static boolean isAirOrLavaOcean(LevelAccessor pLevel, int pSeaLevel, BlockPos pPos) {
      BlockState $$3 = pLevel.getBlockState(pPos);
      return $$3.isAir() || $$3.is(Blocks.LAVA) && pPos.getY() <= pSeaLevel;
   }
}
