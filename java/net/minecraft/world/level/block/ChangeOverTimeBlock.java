package net.minecraft.world.level.block;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public interface ChangeOverTimeBlock<T extends Enum<T>> {
   int SCAN_DISTANCE = 4;

   Optional<BlockState> getNext(BlockState pState);

   float getChanceModifier();

   default void changeOverTime(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      float $$4 = 0.05688889F;
      if (pRandom.nextFloat() < 0.05688889F) {
         this.getNextState(pState, pLevel, pPos, pRandom).ifPresent(p_153039_ -> pLevel.setBlockAndUpdate(pPos, p_153039_));
      }
   }

   T getAge();

   default Optional<BlockState> getNextState(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
      int $$4 = this.getAge().ordinal();
      int $$5 = 0;
      int $$6 = 0;

      for (BlockPos $$7 : BlockPos.withinManhattan(pPos, 4, 4, 4)) {
         int $$8 = $$7.distManhattan(pPos);
         if ($$8 > 4) {
            break;
         }

         if (!$$7.equals(pPos) && pLevel.getBlockState($$7).getBlock() instanceof ChangeOverTimeBlock<?> $$9) {
            Enum<?> $$10 = $$9.getAge();
            if (this.getAge().getClass() == $$10.getClass()) {
               int $$11 = $$10.ordinal();
               if ($$11 < $$4) {
                  return Optional.empty();
               }

               if ($$11 > $$4) {
                  $$6++;
               } else {
                  $$5++;
               }
            }
         }
      }

      float $$12 = (float)($$6 + 1) / (float)($$6 + $$5 + 1);
      float $$13 = $$12 * $$12 * this.getChanceModifier();
      return pRandom.nextFloat() < $$13 ? this.getNext(pState) : Optional.empty();
   }
}
