package net.minecraft.world.level.block;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class NetherVines {
   private static final double BONEMEAL_GROW_PROBABILITY_DECREASE_RATE = 0.826;
   public static final double GROW_PER_TICK_PROBABILITY = 0.1;

   public static boolean isValidGrowthState(BlockState pState) {
      return pState.isAir();
   }

   public static int getBlocksToGrowWhenBonemealed(RandomSource pRandom) {
      double $$1 = 1.0;

      int $$2;
      for ($$2 = 0; pRandom.nextDouble() < $$1; $$2++) {
         $$1 *= 0.826;
      }

      return $$2;
   }
}
