package net.minecraft;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntStack;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockUtil {
   public static BlockUtil.FoundRectangle getLargestRectangleAround(
      BlockPos pCenterPos, Direction.Axis pAxis1, int pMax1, Direction.Axis pAxis2, int pMax2, Predicate<BlockPos> pPosPredicate
   ) {
      BlockPos.MutableBlockPos $$6 = pCenterPos.mutable();
      Direction $$7 = Direction.get(Direction.AxisDirection.NEGATIVE, pAxis1);
      Direction $$8 = $$7.getOpposite();
      Direction $$9 = Direction.get(Direction.AxisDirection.NEGATIVE, pAxis2);
      Direction $$10 = $$9.getOpposite();
      int $$11 = getLimit(pPosPredicate, $$6.set(pCenterPos), $$7, pMax1);
      int $$12 = getLimit(pPosPredicate, $$6.set(pCenterPos), $$8, pMax1);
      int $$13 = $$11;
      BlockUtil.IntBounds[] $$14 = new BlockUtil.IntBounds[$$11 + 1 + $$12];
      $$14[$$11] = new BlockUtil.IntBounds(getLimit(pPosPredicate, $$6.set(pCenterPos), $$9, pMax2), getLimit(pPosPredicate, $$6.set(pCenterPos), $$10, pMax2));
      int $$15 = $$14[$$11].min;

      for (int $$16 = 1; $$16 <= $$11; $$16++) {
         BlockUtil.IntBounds $$17 = $$14[$$13 - ($$16 - 1)];
         $$14[$$13 - $$16] = new BlockUtil.IntBounds(
            getLimit(pPosPredicate, $$6.set(pCenterPos).move($$7, $$16), $$9, $$17.min),
            getLimit(pPosPredicate, $$6.set(pCenterPos).move($$7, $$16), $$10, $$17.max)
         );
      }

      for (int $$18 = 1; $$18 <= $$12; $$18++) {
         BlockUtil.IntBounds $$19 = $$14[$$13 + $$18 - 1];
         $$14[$$13 + $$18] = new BlockUtil.IntBounds(
            getLimit(pPosPredicate, $$6.set(pCenterPos).move($$8, $$18), $$9, $$19.min),
            getLimit(pPosPredicate, $$6.set(pCenterPos).move($$8, $$18), $$10, $$19.max)
         );
      }

      int $$20 = 0;
      int $$21 = 0;
      int $$22 = 0;
      int $$23 = 0;
      int[] $$24 = new int[$$14.length];

      for (int $$25 = $$15; $$25 >= 0; $$25--) {
         for (int $$26 = 0; $$26 < $$14.length; $$26++) {
            BlockUtil.IntBounds $$27 = $$14[$$26];
            int $$28 = $$15 - $$27.min;
            int $$29 = $$15 + $$27.max;
            $$24[$$26] = $$25 >= $$28 && $$25 <= $$29 ? $$29 + 1 - $$25 : 0;
         }

         Pair<BlockUtil.IntBounds, Integer> $$30 = getMaxRectangleLocation($$24);
         BlockUtil.IntBounds $$31 = (BlockUtil.IntBounds)$$30.getFirst();
         int $$32 = 1 + $$31.max - $$31.min;
         int $$33 = (Integer)$$30.getSecond();
         if ($$32 * $$33 > $$22 * $$23) {
            $$20 = $$31.min;
            $$21 = $$25;
            $$22 = $$32;
            $$23 = $$33;
         }
      }

      return new BlockUtil.FoundRectangle(pCenterPos.relative(pAxis1, $$20 - $$13).relative(pAxis2, $$21 - $$15), $$22, $$23);
   }

   private static int getLimit(Predicate<BlockPos> pPosPredicate, BlockPos.MutableBlockPos pCenterPos, Direction pDirection, int pMax) {
      int $$4 = 0;

      while ($$4 < pMax && pPosPredicate.test(pCenterPos.move(pDirection))) {
         $$4++;
      }

      return $$4;
   }

   @VisibleForTesting
   static Pair<BlockUtil.IntBounds, Integer> getMaxRectangleLocation(int[] pHeights) {
      int $$1 = 0;
      int $$2 = 0;
      int $$3 = 0;
      IntStack $$4 = new IntArrayList();
      $$4.push(0);

      for (int $$5 = 1; $$5 <= pHeights.length; $$5++) {
         int $$6 = $$5 == pHeights.length ? 0 : pHeights[$$5];

         while (!$$4.isEmpty()) {
            int $$7 = pHeights[$$4.topInt()];
            if ($$6 >= $$7) {
               $$4.push($$5);
               break;
            }

            $$4.popInt();
            int $$8 = $$4.isEmpty() ? 0 : $$4.topInt() + 1;
            if ($$7 * ($$5 - $$8) > $$3 * ($$2 - $$1)) {
               $$2 = $$5;
               $$1 = $$8;
               $$3 = $$7;
            }
         }

         if ($$4.isEmpty()) {
            $$4.push($$5);
         }
      }

      return new Pair(new BlockUtil.IntBounds($$1, $$2 - 1), $$3);
   }

   public static Optional<BlockPos> getTopConnectedBlock(BlockGetter pGetter, BlockPos pPos, Block pBaseBlock, Direction pDirection, Block pEndBlock) {
      BlockPos.MutableBlockPos $$5 = pPos.mutable();

      BlockState $$6;
      do {
         $$5.move(pDirection);
         $$6 = pGetter.getBlockState($$5);
      } while ($$6.is(pBaseBlock));

      return $$6.is(pEndBlock) ? Optional.of($$5) : Optional.empty();
   }

   public static class FoundRectangle {
      public final BlockPos minCorner;
      public final int axis1Size;
      public final int axis2Size;

      public FoundRectangle(BlockPos pMinCorner, int pAxis1Size, int pAxis2Size) {
         this.minCorner = pMinCorner;
         this.axis1Size = pAxis1Size;
         this.axis2Size = pAxis2Size;
      }
   }

   public static class IntBounds {
      public final int min;
      public final int max;

      public IntBounds(int pMin, int pMax) {
         this.min = pMin;
         this.max = pMax;
      }

      @Override
      public String toString() {
         return "IntBounds{min=" + this.min + ", max=" + this.max + "}";
      }
   }
}
