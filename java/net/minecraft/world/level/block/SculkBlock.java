package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

public class SculkBlock extends DropExperienceBlock implements SculkBehaviour {
   public static final MapCodec<SculkBlock> CODEC = simpleCodec(SculkBlock::new);

   @Override
   public MapCodec<SculkBlock> codec() {
      return CODEC;
   }

   public SculkBlock(BlockBehaviour.Properties p_222063_) {
      super(ConstantInt.of(1), p_222063_);
   }

   @Override
   public int attemptUseCharge(
      SculkSpreader.ChargeCursor pCursor, LevelAccessor pLevel, BlockPos pPos, RandomSource pRandom, SculkSpreader pSpreader, boolean pShouldConvertBlocks
   ) {
      int $$6 = pCursor.getCharge();
      if ($$6 != 0 && pRandom.nextInt(pSpreader.chargeDecayRate()) == 0) {
         BlockPos $$7 = pCursor.getPos();
         boolean $$8 = $$7.closerThan(pPos, (double)pSpreader.noGrowthRadius());
         if (!$$8 && canPlaceGrowth(pLevel, $$7)) {
            int $$9 = pSpreader.growthSpawnCost();
            if (pRandom.nextInt($$9) < $$6) {
               BlockPos $$10 = $$7.above();
               BlockState $$11 = this.getRandomGrowthState(pLevel, $$10, pRandom, pSpreader.isWorldGeneration());
               pLevel.setBlock($$10, $$11, 3);
               pLevel.playSound(null, $$7, $$11.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            return Math.max(0, $$6 - $$9);
         } else {
            return pRandom.nextInt(pSpreader.additionalDecayRate()) != 0 ? $$6 : $$6 - ($$8 ? 1 : getDecayPenalty(pSpreader, $$7, pPos, $$6));
         }
      } else {
         return $$6;
      }
   }

   private static int getDecayPenalty(SculkSpreader pSpreader, BlockPos pCursorPos, BlockPos pRootPos, int pCharge) {
      int $$4 = pSpreader.noGrowthRadius();
      float $$5 = Mth.square((float)Math.sqrt(pCursorPos.distSqr(pRootPos)) - (float)$$4);
      int $$6 = Mth.square(24 - $$4);
      float $$7 = Math.min(1.0F, $$5 / (float)$$6);
      return Math.max(1, (int)((float)pCharge * $$7 * 0.5F));
   }

   private BlockState getRandomGrowthState(LevelAccessor pLevel, BlockPos pPos, RandomSource pRandom, boolean pIsWorldGeneration) {
      BlockState $$4;
      if (pRandom.nextInt(11) == 0) {
         $$4 = Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, Boolean.valueOf(pIsWorldGeneration));
      } else {
         $$4 = Blocks.SCULK_SENSOR.defaultBlockState();
      }

      return $$4.hasProperty(BlockStateProperties.WATERLOGGED) && !pLevel.getFluidState(pPos).isEmpty()
         ? $$4.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(true))
         : $$4;
   }

   private static boolean canPlaceGrowth(LevelAccessor pLevel, BlockPos pPos) {
      BlockState $$2 = pLevel.getBlockState(pPos.above());
      if ($$2.isAir() || $$2.is(Blocks.WATER) && $$2.getFluidState().is(Fluids.WATER)) {
         int $$3 = 0;

         for (BlockPos $$4 : BlockPos.betweenClosed(pPos.offset(-4, 0, -4), pPos.offset(4, 2, 4))) {
            BlockState $$5 = pLevel.getBlockState($$4);
            if ($$5.is(Blocks.SCULK_SENSOR) || $$5.is(Blocks.SCULK_SHRIEKER)) {
               $$3++;
            }

            if ($$3 > 2) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean canChangeBlockStateOnSpread() {
      return false;
   }
}
