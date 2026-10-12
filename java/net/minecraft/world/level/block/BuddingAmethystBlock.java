package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public class BuddingAmethystBlock extends AmethystBlock {
   public static final MapCodec<BuddingAmethystBlock> CODEC = simpleCodec(BuddingAmethystBlock::new);
   public static final int GROWTH_CHANCE = 5;
   private static final Direction[] DIRECTIONS = Direction.values();

   @Override
   public MapCodec<BuddingAmethystBlock> codec() {
      return CODEC;
   }

   public BuddingAmethystBlock(BlockBehaviour.Properties p_152726_) {
      super(p_152726_);
   }

   @Override
   protected void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      if (pRandom.nextInt(5) == 0) {
         Direction $$4 = DIRECTIONS[pRandom.nextInt(DIRECTIONS.length)];
         BlockPos $$5 = pPos.relative($$4);
         BlockState $$6 = pLevel.getBlockState($$5);
         Block $$7 = null;
         if (canClusterGrowAtState($$6)) {
            $$7 = Blocks.SMALL_AMETHYST_BUD;
         } else if ($$6.is(Blocks.SMALL_AMETHYST_BUD) && $$6.getValue(AmethystClusterBlock.FACING) == $$4) {
            $$7 = Blocks.MEDIUM_AMETHYST_BUD;
         } else if ($$6.is(Blocks.MEDIUM_AMETHYST_BUD) && $$6.getValue(AmethystClusterBlock.FACING) == $$4) {
            $$7 = Blocks.LARGE_AMETHYST_BUD;
         } else if ($$6.is(Blocks.LARGE_AMETHYST_BUD) && $$6.getValue(AmethystClusterBlock.FACING) == $$4) {
            $$7 = Blocks.AMETHYST_CLUSTER;
         }

         if ($$7 != null) {
            BlockState $$8 = $$7.defaultBlockState()
               .setValue(AmethystClusterBlock.FACING, $$4)
               .setValue(AmethystClusterBlock.WATERLOGGED, Boolean.valueOf($$6.getFluidState().getType() == Fluids.WATER));
            pLevel.setBlockAndUpdate($$5, $$8);
         }
      }
   }

   public static boolean canClusterGrowAtState(BlockState pState) {
      return pState.isAir() || pState.is(Blocks.WATER) && pState.getFluidState().getAmount() == 8;
   }
}
