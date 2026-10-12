package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

public class HugeFungusFeature extends Feature<HugeFungusConfiguration> {
   private static final float HUGE_PROBABILITY = 0.06F;

   public HugeFungusFeature(Codec<HugeFungusConfiguration> pCodec) {
      super(pCodec);
   }

   @Override
   public boolean place(FeaturePlaceContext<HugeFungusConfiguration> pContext) {
      WorldGenLevel $$1 = pContext.level();
      BlockPos $$2 = pContext.origin();
      RandomSource $$3 = pContext.random();
      ChunkGenerator $$4 = pContext.chunkGenerator();
      HugeFungusConfiguration $$5 = pContext.config();
      Block $$6 = $$5.validBaseState.getBlock();
      BlockPos $$7 = null;
      BlockState $$8 = $$1.getBlockState($$2.below());
      if ($$8.is($$6)) {
         $$7 = $$2;
      }

      if ($$7 == null) {
         return false;
      } else {
         int $$9 = Mth.nextInt($$3, 4, 13);
         if ($$3.nextInt(12) == 0) {
            $$9 *= 2;
         }

         if (!$$5.planted) {
            int $$10 = $$4.getGenDepth();
            if ($$7.getY() + $$9 + 1 >= $$10) {
               return false;
            }
         }

         boolean $$11 = !$$5.planted && $$3.nextFloat() < 0.06F;
         $$1.setBlock($$2, Blocks.AIR.defaultBlockState(), 4);
         this.placeStem($$1, $$3, $$5, $$7, $$9, $$11);
         this.placeHat($$1, $$3, $$5, $$7, $$9, $$11);
         return true;
      }
   }

   private static boolean isReplaceable(WorldGenLevel pLevel, BlockPos pPos, HugeFungusConfiguration pConfig, boolean pCheckConfig) {
      if (pLevel.isStateAtPosition(pPos, BlockBehaviour.BlockStateBase::canBeReplaced)) {
         return true;
      } else {
         return pCheckConfig ? pConfig.replaceableBlocks.test(pLevel, pPos) : false;
      }
   }

   private void placeStem(WorldGenLevel pLevel, RandomSource pRandom, HugeFungusConfiguration pConfig, BlockPos pPos, int pHeight, boolean pHuge) {
      BlockPos.MutableBlockPos $$6 = new BlockPos.MutableBlockPos();
      BlockState $$7 = pConfig.stemState;
      int $$8 = pHuge ? 1 : 0;

      for (int $$9 = -$$8; $$9 <= $$8; $$9++) {
         for (int $$10 = -$$8; $$10 <= $$8; $$10++) {
            boolean $$11 = pHuge && Mth.abs($$9) == $$8 && Mth.abs($$10) == $$8;

            for (int $$12 = 0; $$12 < pHeight; $$12++) {
               $$6.setWithOffset(pPos, $$9, $$12, $$10);
               if (isReplaceable(pLevel, $$6, pConfig, true)) {
                  if (pConfig.planted) {
                     if (!pLevel.getBlockState($$6.below()).isAir()) {
                        pLevel.destroyBlock($$6, true);
                     }

                     pLevel.setBlock($$6, $$7, 3);
                  } else if ($$11) {
                     if (pRandom.nextFloat() < 0.1F) {
                        this.setBlock(pLevel, $$6, $$7);
                     }
                  } else {
                     this.setBlock(pLevel, $$6, $$7);
                  }
               }
            }
         }
      }
   }

   private void placeHat(WorldGenLevel pLevel, RandomSource pRandom, HugeFungusConfiguration pConfig, BlockPos pPos, int pHeight, boolean pHuge) {
      BlockPos.MutableBlockPos $$6 = new BlockPos.MutableBlockPos();
      boolean $$7 = pConfig.hatState.is(Blocks.NETHER_WART_BLOCK);
      int $$8 = Math.min(pRandom.nextInt(1 + pHeight / 3) + 5, pHeight);
      int $$9 = pHeight - $$8;

      for (int $$10 = $$9; $$10 <= pHeight; $$10++) {
         int $$11 = $$10 < pHeight - pRandom.nextInt(3) ? 2 : 1;
         if ($$8 > 8 && $$10 < $$9 + 4) {
            $$11 = 3;
         }

         if (pHuge) {
            $$11++;
         }

         for (int $$12 = -$$11; $$12 <= $$11; $$12++) {
            for (int $$13 = -$$11; $$13 <= $$11; $$13++) {
               boolean $$14 = $$12 == -$$11 || $$12 == $$11;
               boolean $$15 = $$13 == -$$11 || $$13 == $$11;
               boolean $$16 = !$$14 && !$$15 && $$10 != pHeight;
               boolean $$17 = $$14 && $$15;
               boolean $$18 = $$10 < $$9 + 3;
               $$6.setWithOffset(pPos, $$12, $$10, $$13);
               if (isReplaceable(pLevel, $$6, pConfig, false)) {
                  if (pConfig.planted && !pLevel.getBlockState($$6.below()).isAir()) {
                     pLevel.destroyBlock($$6, true);
                  }

                  if ($$18) {
                     if (!$$16) {
                        this.placeHatDropBlock(pLevel, pRandom, $$6, pConfig.hatState, $$7);
                     }
                  } else if ($$16) {
                     this.placeHatBlock(pLevel, pRandom, pConfig, $$6, 0.1F, 0.2F, $$7 ? 0.1F : 0.0F);
                  } else if ($$17) {
                     this.placeHatBlock(pLevel, pRandom, pConfig, $$6, 0.01F, 0.7F, $$7 ? 0.083F : 0.0F);
                  } else {
                     this.placeHatBlock(pLevel, pRandom, pConfig, $$6, 5.0E-4F, 0.98F, $$7 ? 0.07F : 0.0F);
                  }
               }
            }
         }
      }
   }

   private void placeHatBlock(
      LevelAccessor pLevel,
      RandomSource pRandom,
      HugeFungusConfiguration pConfig,
      BlockPos.MutableBlockPos pPos,
      float pDecorationChance,
      float pHatChance,
      float pWeepingVineChance
   ) {
      if (pRandom.nextFloat() < pDecorationChance) {
         this.setBlock(pLevel, pPos, pConfig.decorState);
      } else if (pRandom.nextFloat() < pHatChance) {
         this.setBlock(pLevel, pPos, pConfig.hatState);
         if (pRandom.nextFloat() < pWeepingVineChance) {
            tryPlaceWeepingVines(pPos, pLevel, pRandom);
         }
      }
   }

   private void placeHatDropBlock(LevelAccessor pLevel, RandomSource pRandom, BlockPos pPos, BlockState pState, boolean pWeepingVines) {
      if (pLevel.getBlockState(pPos.below()).is(pState.getBlock())) {
         this.setBlock(pLevel, pPos, pState);
      } else if ((double)pRandom.nextFloat() < 0.15) {
         this.setBlock(pLevel, pPos, pState);
         if (pWeepingVines && pRandom.nextInt(11) == 0) {
            tryPlaceWeepingVines(pPos, pLevel, pRandom);
         }
      }
   }

   private static void tryPlaceWeepingVines(BlockPos pPos, LevelAccessor pLevel, RandomSource pRandom) {
      BlockPos.MutableBlockPos $$3 = pPos.mutable().move(Direction.DOWN);
      if (pLevel.isEmptyBlock($$3)) {
         int $$4 = Mth.nextInt(pRandom, 1, 5);
         if (pRandom.nextInt(7) == 0) {
            $$4 *= 2;
         }

         int $$5 = 23;
         int $$6 = 25;
         WeepingVinesFeature.placeWeepingVinesColumn(pLevel, pRandom, $$3, $$4, 23, 25);
      }
   }
}
