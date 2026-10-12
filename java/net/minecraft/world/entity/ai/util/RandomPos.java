package net.minecraft.world.entity.ai.util;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.phys.Vec3;

public class RandomPos {
   private static final int RANDOM_POS_ATTEMPTS = 10;

   public static BlockPos generateRandomDirection(RandomSource pRandom, int pHorizontalDistance, int pVerticalDistance) {
      int $$3 = pRandom.nextInt(2 * pHorizontalDistance + 1) - pHorizontalDistance;
      int $$4 = pRandom.nextInt(2 * pVerticalDistance + 1) - pVerticalDistance;
      int $$5 = pRandom.nextInt(2 * pHorizontalDistance + 1) - pHorizontalDistance;
      return new BlockPos($$3, $$4, $$5);
   }

   @Nullable
   public static BlockPos generateRandomDirectionWithinRadians(
      RandomSource pRandom, int pMaxHorizontalDifference, int pYRange, int pY, double pX, double pZ, double pMaxAngleDelta
   ) {
      double $$7 = Mth.atan2(pZ, pX) - (float) (Math.PI / 2);
      double $$8 = $$7 + (double)(2.0F * pRandom.nextFloat() - 1.0F) * pMaxAngleDelta;
      double $$9 = Math.sqrt(pRandom.nextDouble()) * (double)Mth.SQRT_OF_TWO * (double)pMaxHorizontalDifference;
      double $$10 = -$$9 * Math.sin($$8);
      double $$11 = $$9 * Math.cos($$8);
      if (!(Math.abs($$10) > (double)pMaxHorizontalDifference) && !(Math.abs($$11) > (double)pMaxHorizontalDifference)) {
         int $$12 = pRandom.nextInt(2 * pYRange + 1) - pYRange + pY;
         return BlockPos.containing($$10, (double)$$12, $$11);
      } else {
         return null;
      }
   }

   @VisibleForTesting
   public static BlockPos moveUpOutOfSolid(BlockPos pPos, int pMaxY, Predicate<BlockPos> pPosPredicate) {
      if (!pPosPredicate.test(pPos)) {
         return pPos;
      } else {
         BlockPos $$3 = pPos.above();

         while ($$3.getY() < pMaxY && pPosPredicate.test($$3)) {
            $$3 = $$3.above();
         }

         return $$3;
      }
   }

   @VisibleForTesting
   public static BlockPos moveUpToAboveSolid(BlockPos pPos, int pAboveSolidAmount, int pMaxY, Predicate<BlockPos> pPosPredicate) {
      if (pAboveSolidAmount < 0) {
         throw new IllegalArgumentException("aboveSolidAmount was " + pAboveSolidAmount + ", expected >= 0");
      } else if (!pPosPredicate.test(pPos)) {
         return pPos;
      } else {
         BlockPos $$4 = pPos.above();

         while ($$4.getY() < pMaxY && pPosPredicate.test($$4)) {
            $$4 = $$4.above();
         }

         BlockPos $$5 = $$4;

         while ($$5.getY() < pMaxY && $$5.getY() - $$4.getY() < pAboveSolidAmount) {
            BlockPos $$6 = $$5.above();
            if (pPosPredicate.test($$6)) {
               break;
            }

            $$5 = $$6;
         }

         return $$5;
      }
   }

   @Nullable
   public static Vec3 generateRandomPos(PathfinderMob pMob, Supplier<BlockPos> pPosSupplier) {
      return generateRandomPos(pPosSupplier, pMob::getWalkTargetValue);
   }

   @Nullable
   public static Vec3 generateRandomPos(Supplier<BlockPos> pPosSupplier, ToDoubleFunction<BlockPos> pToDoubleFunction) {
      double $$2 = Double.NEGATIVE_INFINITY;
      BlockPos $$3 = null;

      for (int $$4 = 0; $$4 < 10; $$4++) {
         BlockPos $$5 = pPosSupplier.get();
         if ($$5 != null) {
            double $$6 = pToDoubleFunction.applyAsDouble($$5);
            if ($$6 > $$2) {
               $$2 = $$6;
               $$3 = $$5;
            }
         }
      }

      return $$3 != null ? Vec3.atBottomCenterOf($$3) : null;
   }

   public static BlockPos generateRandomPosTowardDirection(PathfinderMob pMob, int pRange, RandomSource pRandom, BlockPos pPos) {
      int $$4 = pPos.getX();
      int $$5 = pPos.getZ();
      if (pMob.hasRestriction() && pRange > 1) {
         BlockPos $$6 = pMob.getRestrictCenter();
         if (pMob.getX() > (double)$$6.getX()) {
            $$4 -= pRandom.nextInt(pRange / 2);
         } else {
            $$4 += pRandom.nextInt(pRange / 2);
         }

         if (pMob.getZ() > (double)$$6.getZ()) {
            $$5 -= pRandom.nextInt(pRange / 2);
         } else {
            $$5 += pRandom.nextInt(pRange / 2);
         }
      }

      return BlockPos.containing((double)$$4 + pMob.getX(), (double)pPos.getY() + pMob.getY(), (double)$$5 + pMob.getZ());
   }
}
