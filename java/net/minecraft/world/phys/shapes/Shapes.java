package net.minecraft.world.phys.shapes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.Util;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class Shapes {
   public static final double EPSILON = 1.0E-7;
   public static final double BIG_EPSILON = 1.0E-6;
   private static final VoxelShape BLOCK = Util.make(() -> {
      DiscreteVoxelShape $$0 = new BitSetDiscreteVoxelShape(1, 1, 1);
      $$0.fill(0, 0, 0);
      return new CubeVoxelShape($$0);
   });
   public static final VoxelShape INFINITY = box(
      Double.NEGATIVE_INFINITY,
      Double.NEGATIVE_INFINITY,
      Double.NEGATIVE_INFINITY,
      Double.POSITIVE_INFINITY,
      Double.POSITIVE_INFINITY,
      Double.POSITIVE_INFINITY
   );
   private static final VoxelShape EMPTY = new ArrayVoxelShape(
      new BitSetDiscreteVoxelShape(0, 0, 0),
      new DoubleArrayList(new double[]{0.0}),
      new DoubleArrayList(new double[]{0.0}),
      new DoubleArrayList(new double[]{0.0})
   );

   public static VoxelShape empty() {
      return EMPTY;
   }

   public static VoxelShape block() {
      return BLOCK;
   }

   public static VoxelShape box(double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ) {
      if (!(pMinX > pMaxX) && !(pMinY > pMaxY) && !(pMinZ > pMaxZ)) {
         return create(pMinX, pMinY, pMinZ, pMaxX, pMaxY, pMaxZ);
      } else {
         throw new IllegalArgumentException("The min values need to be smaller or equals to the max values");
      }
   }

   public static VoxelShape create(double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ) {
      if (!(pMaxX - pMinX < 1.0E-7) && !(pMaxY - pMinY < 1.0E-7) && !(pMaxZ - pMinZ < 1.0E-7)) {
         int $$6 = findBits(pMinX, pMaxX);
         int $$7 = findBits(pMinY, pMaxY);
         int $$8 = findBits(pMinZ, pMaxZ);
         if ($$6 < 0 || $$7 < 0 || $$8 < 0) {
            return new ArrayVoxelShape(
               BLOCK.shape,
               DoubleArrayList.wrap(new double[]{pMinX, pMaxX}),
               DoubleArrayList.wrap(new double[]{pMinY, pMaxY}),
               DoubleArrayList.wrap(new double[]{pMinZ, pMaxZ})
            );
         } else if ($$6 == 0 && $$7 == 0 && $$8 == 0) {
            return block();
         } else {
            int $$9 = 1 << $$6;
            int $$10 = 1 << $$7;
            int $$11 = 1 << $$8;
            BitSetDiscreteVoxelShape $$12 = BitSetDiscreteVoxelShape.withFilledBounds(
               $$9,
               $$10,
               $$11,
               (int)Math.round(pMinX * (double)$$9),
               (int)Math.round(pMinY * (double)$$10),
               (int)Math.round(pMinZ * (double)$$11),
               (int)Math.round(pMaxX * (double)$$9),
               (int)Math.round(pMaxY * (double)$$10),
               (int)Math.round(pMaxZ * (double)$$11)
            );
            return new CubeVoxelShape($$12);
         }
      } else {
         return empty();
      }
   }

   public static VoxelShape create(AABB pAabb) {
      return create(pAabb.minX, pAabb.minY, pAabb.minZ, pAabb.maxX, pAabb.maxY, pAabb.maxZ);
   }

   @VisibleForTesting
   protected static int findBits(double pMinBits, double pMaxBits) {
      if (!(pMinBits < -1.0E-7) && !(pMaxBits > 1.0000001)) {
         for (int $$2 = 0; $$2 <= 3; $$2++) {
            int $$3 = 1 << $$2;
            double $$4 = pMinBits * (double)$$3;
            double $$5 = pMaxBits * (double)$$3;
            boolean $$6 = Math.abs($$4 - (double)Math.round($$4)) < 1.0E-7 * (double)$$3;
            boolean $$7 = Math.abs($$5 - (double)Math.round($$5)) < 1.0E-7 * (double)$$3;
            if ($$6 && $$7) {
               return $$2;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   protected static long lcm(int pAa, int pBb) {
      return (long)pAa * (long)(pBb / IntMath.gcd(pAa, pBb));
   }

   public static VoxelShape or(VoxelShape p_83111_, VoxelShape p_83112_) {
      return join(p_83111_, p_83112_, BooleanOp.OR);
   }

   public static VoxelShape or(VoxelShape pShape1, VoxelShape... pOthers) {
      return Arrays.stream(pOthers).reduce(pShape1, Shapes::or);
   }

   public static VoxelShape join(VoxelShape pShape1, VoxelShape pShape2, BooleanOp pFunction) {
      return joinUnoptimized(pShape1, pShape2, pFunction).optimize();
   }

   public static VoxelShape joinUnoptimized(VoxelShape pShape1, VoxelShape pShape2, BooleanOp pFunction) {
      if (pFunction.apply(false, false)) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
      } else if (pShape1 == pShape2) {
         return pFunction.apply(true, true) ? pShape1 : empty();
      } else {
         boolean $$3 = pFunction.apply(true, false);
         boolean $$4 = pFunction.apply(false, true);
         if (pShape1.isEmpty()) {
            return $$4 ? pShape2 : empty();
         } else if (pShape2.isEmpty()) {
            return $$3 ? pShape1 : empty();
         } else {
            IndexMerger $$5 = createIndexMerger(1, pShape1.getCoords(Direction.Axis.X), pShape2.getCoords(Direction.Axis.X), $$3, $$4);
            IndexMerger $$6 = createIndexMerger($$5.size() - 1, pShape1.getCoords(Direction.Axis.Y), pShape2.getCoords(Direction.Axis.Y), $$3, $$4);
            IndexMerger $$7 = createIndexMerger(
               ($$5.size() - 1) * ($$6.size() - 1), pShape1.getCoords(Direction.Axis.Z), pShape2.getCoords(Direction.Axis.Z), $$3, $$4
            );
            BitSetDiscreteVoxelShape $$8 = BitSetDiscreteVoxelShape.join(pShape1.shape, pShape2.shape, $$5, $$6, $$7, pFunction);
            return (VoxelShape)($$5 instanceof DiscreteCubeMerger && $$6 instanceof DiscreteCubeMerger && $$7 instanceof DiscreteCubeMerger
               ? new CubeVoxelShape($$8)
               : new ArrayVoxelShape($$8, $$5.getList(), $$6.getList(), $$7.getList()));
         }
      }
   }

   public static boolean joinIsNotEmpty(VoxelShape pShape1, VoxelShape pShape2, BooleanOp pResultOperator) {
      if (pResultOperator.apply(false, false)) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
      } else {
         boolean $$3 = pShape1.isEmpty();
         boolean $$4 = pShape2.isEmpty();
         if (!$$3 && !$$4) {
            if (pShape1 == pShape2) {
               return pResultOperator.apply(true, true);
            } else {
               boolean $$5 = pResultOperator.apply(true, false);
               boolean $$6 = pResultOperator.apply(false, true);

               for (Direction.Axis $$7 : AxisCycle.AXIS_VALUES) {
                  if (pShape1.max($$7) < pShape2.min($$7) - 1.0E-7) {
                     return $$5 || $$6;
                  }

                  if (pShape2.max($$7) < pShape1.min($$7) - 1.0E-7) {
                     return $$5 || $$6;
                  }
               }

               IndexMerger $$8 = createIndexMerger(1, pShape1.getCoords(Direction.Axis.X), pShape2.getCoords(Direction.Axis.X), $$5, $$6);
               IndexMerger $$9 = createIndexMerger($$8.size() - 1, pShape1.getCoords(Direction.Axis.Y), pShape2.getCoords(Direction.Axis.Y), $$5, $$6);
               IndexMerger $$10 = createIndexMerger(
                  ($$8.size() - 1) * ($$9.size() - 1), pShape1.getCoords(Direction.Axis.Z), pShape2.getCoords(Direction.Axis.Z), $$5, $$6
               );
               return joinIsNotEmpty($$8, $$9, $$10, pShape1.shape, pShape2.shape, pResultOperator);
            }
         } else {
            return pResultOperator.apply(!$$3, !$$4);
         }
      }
   }

   private static boolean joinIsNotEmpty(
      IndexMerger pMergerX,
      IndexMerger pMergerY,
      IndexMerger pMergerZ,
      DiscreteVoxelShape pPrimaryShape,
      DiscreteVoxelShape pSecondaryShape,
      BooleanOp pResultOperator
   ) {
      return !pMergerX.forMergedIndexes(
         (p_83100_, p_83101_, p_83102_) -> pMergerY.forMergedIndexes(
               (p_166046_, p_166047_, p_166048_) -> pMergerZ.forMergedIndexes(
                     (p_166036_, p_166037_, p_166038_) -> !pResultOperator.apply(
                           pPrimaryShape.isFullWide(p_83100_, p_166046_, p_166036_), pSecondaryShape.isFullWide(p_83101_, p_166047_, p_166037_)
                        )
                  )
            )
      );
   }

   public static double collide(Direction.Axis pMovementAxis, AABB pCollisionBox, Iterable<VoxelShape> pPossibleHits, double pDesiredOffset) {
      for (VoxelShape $$4 : pPossibleHits) {
         if (Math.abs(pDesiredOffset) < 1.0E-7) {
            return 0.0;
         }

         pDesiredOffset = $$4.collide(pMovementAxis, pCollisionBox, pDesiredOffset);
      }

      return pDesiredOffset;
   }

   public static boolean blockOccudes(VoxelShape pShape, VoxelShape pAdjacentShape, Direction pSide) {
      if (pShape == block() && pAdjacentShape == block()) {
         return true;
      } else if (pAdjacentShape.isEmpty()) {
         return false;
      } else {
         Direction.Axis $$3 = pSide.getAxis();
         Direction.AxisDirection $$4 = pSide.getAxisDirection();
         VoxelShape $$5 = $$4 == Direction.AxisDirection.POSITIVE ? pShape : pAdjacentShape;
         VoxelShape $$6 = $$4 == Direction.AxisDirection.POSITIVE ? pAdjacentShape : pShape;
         BooleanOp $$7 = $$4 == Direction.AxisDirection.POSITIVE ? BooleanOp.ONLY_FIRST : BooleanOp.ONLY_SECOND;
         return DoubleMath.fuzzyEquals($$5.max($$3), 1.0, 1.0E-7)
            && DoubleMath.fuzzyEquals($$6.min($$3), 0.0, 1.0E-7)
            && !joinIsNotEmpty(new SliceShape($$5, $$3, $$5.shape.getSize($$3) - 1), new SliceShape($$6, $$3, 0), $$7);
      }
   }

   public static VoxelShape getFaceShape(VoxelShape pVoxelShape, Direction pDirection) {
      if (pVoxelShape == block()) {
         return block();
      } else {
         Direction.Axis $$2 = pDirection.getAxis();
         boolean $$3;
         int $$4;
         if (pDirection.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            $$3 = DoubleMath.fuzzyEquals(pVoxelShape.max($$2), 1.0, 1.0E-7);
            $$4 = pVoxelShape.shape.getSize($$2) - 1;
         } else {
            $$3 = DoubleMath.fuzzyEquals(pVoxelShape.min($$2), 0.0, 1.0E-7);
            $$4 = 0;
         }

         return (VoxelShape)(!$$3 ? empty() : new SliceShape(pVoxelShape, $$2, $$4));
      }
   }

   public static boolean mergedFaceOccludes(VoxelShape pShape, VoxelShape pAdjacentShape, Direction pSide) {
      if (pShape != block() && pAdjacentShape != block()) {
         Direction.Axis $$3 = pSide.getAxis();
         Direction.AxisDirection $$4 = pSide.getAxisDirection();
         VoxelShape $$5 = $$4 == Direction.AxisDirection.POSITIVE ? pShape : pAdjacentShape;
         VoxelShape $$6 = $$4 == Direction.AxisDirection.POSITIVE ? pAdjacentShape : pShape;
         if (!DoubleMath.fuzzyEquals($$5.max($$3), 1.0, 1.0E-7)) {
            $$5 = empty();
         }

         if (!DoubleMath.fuzzyEquals($$6.min($$3), 0.0, 1.0E-7)) {
            $$6 = empty();
         }

         return !joinIsNotEmpty(
            block(), joinUnoptimized(new SliceShape($$5, $$3, $$5.shape.getSize($$3) - 1), new SliceShape($$6, $$3, 0), BooleanOp.OR), BooleanOp.ONLY_FIRST
         );
      } else {
         return true;
      }
   }

   public static boolean faceShapeOccludes(VoxelShape pVoxelShape1, VoxelShape pVoxelShape2) {
      if (pVoxelShape1 == block() || pVoxelShape2 == block()) {
         return true;
      } else {
         return pVoxelShape1.isEmpty() && pVoxelShape2.isEmpty()
            ? false
            : !joinIsNotEmpty(block(), joinUnoptimized(pVoxelShape1, pVoxelShape2, BooleanOp.OR), BooleanOp.ONLY_FIRST);
      }
   }

   @VisibleForTesting
   protected static IndexMerger createIndexMerger(int pSize, DoubleList pList1, DoubleList pList2, boolean pExcludeUpper, boolean pExcludeLower) {
      int $$5 = pList1.size() - 1;
      int $$6 = pList2.size() - 1;
      if (pList1 instanceof CubePointRange && pList2 instanceof CubePointRange) {
         long $$7 = lcm($$5, $$6);
         if ((long)pSize * $$7 <= 256L) {
            return new DiscreteCubeMerger($$5, $$6);
         }
      }

      if (pList1.getDouble($$5) < pList2.getDouble(0) - 1.0E-7) {
         return new NonOverlappingMerger(pList1, pList2, false);
      } else if (pList2.getDouble($$6) < pList1.getDouble(0) - 1.0E-7) {
         return new NonOverlappingMerger(pList2, pList1, true);
      } else {
         return (IndexMerger)($$5 == $$6 && Objects.equals(pList1, pList2)
            ? new IdenticalMerger(pList1)
            : new IndirectMerger(pList1, pList2, pExcludeUpper, pExcludeLower));
      }
   }

   public interface DoubleLineConsumer {
      void consume(double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ);
   }
}
