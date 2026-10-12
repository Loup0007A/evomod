package net.minecraft.world.phys.shapes;

import java.util.BitSet;
import net.minecraft.core.Direction;

public final class BitSetDiscreteVoxelShape extends DiscreteVoxelShape {
   private final BitSet storage;
   private int xMin;
   private int yMin;
   private int zMin;
   private int xMax;
   private int yMax;
   private int zMax;

   public BitSetDiscreteVoxelShape(int pXSize, int pYSize, int pZSize) {
      super(pXSize, pYSize, pZSize);
      this.storage = new BitSet(pXSize * pYSize * pZSize);
      this.xMin = pXSize;
      this.yMin = pYSize;
      this.zMin = pZSize;
   }

   public static BitSetDiscreteVoxelShape withFilledBounds(int pX, int pY, int pZ, int pXMin, int pYMin, int pZMin, int pXMax, int pYMax, int pZMax) {
      BitSetDiscreteVoxelShape $$9 = new BitSetDiscreteVoxelShape(pX, pY, pZ);
      $$9.xMin = pXMin;
      $$9.yMin = pYMin;
      $$9.zMin = pZMin;
      $$9.xMax = pXMax;
      $$9.yMax = pYMax;
      $$9.zMax = pZMax;

      for (int $$10 = pXMin; $$10 < pXMax; $$10++) {
         for (int $$11 = pYMin; $$11 < pYMax; $$11++) {
            for (int $$12 = pZMin; $$12 < pZMax; $$12++) {
               $$9.fillUpdateBounds($$10, $$11, $$12, false);
            }
         }
      }

      return $$9;
   }

   public BitSetDiscreteVoxelShape(DiscreteVoxelShape pShape) {
      super(pShape.xSize, pShape.ySize, pShape.zSize);
      if (pShape instanceof BitSetDiscreteVoxelShape) {
         this.storage = (BitSet)((BitSetDiscreteVoxelShape)pShape).storage.clone();
      } else {
         this.storage = new BitSet(this.xSize * this.ySize * this.zSize);

         for (int $$1 = 0; $$1 < this.xSize; $$1++) {
            for (int $$2 = 0; $$2 < this.ySize; $$2++) {
               for (int $$3 = 0; $$3 < this.zSize; $$3++) {
                  if (pShape.isFull($$1, $$2, $$3)) {
                     this.storage.set(this.getIndex($$1, $$2, $$3));
                  }
               }
            }
         }
      }

      this.xMin = pShape.firstFull(Direction.Axis.X);
      this.yMin = pShape.firstFull(Direction.Axis.Y);
      this.zMin = pShape.firstFull(Direction.Axis.Z);
      this.xMax = pShape.lastFull(Direction.Axis.X);
      this.yMax = pShape.lastFull(Direction.Axis.Y);
      this.zMax = pShape.lastFull(Direction.Axis.Z);
   }

   protected int getIndex(int pX, int pY, int pZ) {
      return (pX * this.ySize + pY) * this.zSize + pZ;
   }

   @Override
   public boolean isFull(int pX, int pY, int pZ) {
      return this.storage.get(this.getIndex(pX, pY, pZ));
   }

   private void fillUpdateBounds(int pX, int pY, int pZ, boolean pUpdate) {
      this.storage.set(this.getIndex(pX, pY, pZ));
      if (pUpdate) {
         this.xMin = Math.min(this.xMin, pX);
         this.yMin = Math.min(this.yMin, pY);
         this.zMin = Math.min(this.zMin, pZ);
         this.xMax = Math.max(this.xMax, pX + 1);
         this.yMax = Math.max(this.yMax, pY + 1);
         this.zMax = Math.max(this.zMax, pZ + 1);
      }
   }

   @Override
   public void fill(int pX, int pY, int pZ) {
      this.fillUpdateBounds(pX, pY, pZ, true);
   }

   @Override
   public boolean isEmpty() {
      return this.storage.isEmpty();
   }

   @Override
   public int firstFull(Direction.Axis pAxis) {
      return pAxis.choose(this.xMin, this.yMin, this.zMin);
   }

   @Override
   public int lastFull(Direction.Axis pAxis) {
      return pAxis.choose(this.xMax, this.yMax, this.zMax);
   }

   static BitSetDiscreteVoxelShape join(
      DiscreteVoxelShape pMainShape, DiscreteVoxelShape pSecondaryShape, IndexMerger pMergerX, IndexMerger pMergerY, IndexMerger pMergerZ, BooleanOp pOperator
   ) {
      BitSetDiscreteVoxelShape $$6 = new BitSetDiscreteVoxelShape(pMergerX.size() - 1, pMergerY.size() - 1, pMergerZ.size() - 1);
      int[] $$7 = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
      pMergerX.forMergedIndexes((p_82670_, p_82671_, p_82672_) -> {
         boolean[] $$10 = new boolean[]{false};
         pMergerY.forMergedIndexes((p_165978_, p_165979_, p_165980_) -> {
            boolean[] $$13 = new boolean[]{false};
            pMergerZ.forMergedIndexes((p_165960_, p_165961_, p_165962_) -> {
               if (pOperator.apply(pMainShape.isFullWide(p_82670_, p_165978_, p_165960_), pSecondaryShape.isFullWide(p_82671_, p_165979_, p_165961_))) {
                  $$6.storage.set($$6.getIndex(p_82672_, p_165980_, p_165962_));
                  $$7[2] = Math.min($$7[2], p_165962_);
                  $$7[5] = Math.max($$7[5], p_165962_);
                  $$13[0] = true;
               }

               return true;
            });
            if ($$13[0]) {
               $$7[1] = Math.min($$7[1], p_165980_);
               $$7[4] = Math.max($$7[4], p_165980_);
               $$10[0] = true;
            }

            return true;
         });
         if ($$10[0]) {
            $$7[0] = Math.min($$7[0], p_82672_);
            $$7[3] = Math.max($$7[3], p_82672_);
         }

         return true;
      });
      $$6.xMin = $$7[0];
      $$6.yMin = $$7[1];
      $$6.zMin = $$7[2];
      $$6.xMax = $$7[3] + 1;
      $$6.yMax = $$7[4] + 1;
      $$6.zMax = $$7[5] + 1;
      return $$6;
   }

   protected static void forAllBoxes(DiscreteVoxelShape pShape, DiscreteVoxelShape.IntLineConsumer pConsumer, boolean pCombine) {
      BitSetDiscreteVoxelShape $$3 = new BitSetDiscreteVoxelShape(pShape);

      for (int $$4 = 0; $$4 < $$3.ySize; $$4++) {
         for (int $$5 = 0; $$5 < $$3.xSize; $$5++) {
            int $$6 = -1;

            for (int $$7 = 0; $$7 <= $$3.zSize; $$7++) {
               if ($$3.isFullWide($$5, $$4, $$7)) {
                  if (pCombine) {
                     if ($$6 == -1) {
                        $$6 = $$7;
                     }
                  } else {
                     pConsumer.consume($$5, $$4, $$7, $$5 + 1, $$4 + 1, $$7 + 1);
                  }
               } else if ($$6 != -1) {
                  int $$8 = $$5;
                  int $$9 = $$4;
                  $$3.clearZStrip($$6, $$7, $$5, $$4);

                  while ($$3.isZStripFull($$6, $$7, $$8 + 1, $$4)) {
                     $$3.clearZStrip($$6, $$7, $$8 + 1, $$4);
                     $$8++;
                  }

                  while ($$3.isXZRectangleFull($$5, $$8 + 1, $$6, $$7, $$9 + 1)) {
                     for (int $$10 = $$5; $$10 <= $$8; $$10++) {
                        $$3.clearZStrip($$6, $$7, $$10, $$9 + 1);
                     }

                     $$9++;
                  }

                  pConsumer.consume($$5, $$4, $$6, $$8 + 1, $$9 + 1, $$7);
                  $$6 = -1;
               }
            }
         }
      }
   }

   private boolean isZStripFull(int pZMin, int pZMax, int pX, int pY) {
      return pX < this.xSize && pY < this.ySize ? this.storage.nextClearBit(this.getIndex(pX, pY, pZMin)) >= this.getIndex(pX, pY, pZMax) : false;
   }

   private boolean isXZRectangleFull(int pXMin, int pXMax, int pZMin, int pZMax, int pY) {
      for (int $$5 = pXMin; $$5 < pXMax; $$5++) {
         if (!this.isZStripFull(pZMin, pZMax, $$5, pY)) {
            return false;
         }
      }

      return true;
   }

   private void clearZStrip(int pZMin, int pZMax, int pX, int pY) {
      this.storage.clear(this.getIndex(pX, pY, pZMin), this.getIndex(pX, pY, pZMax));
   }

   public boolean isInterior(int pX, int pY, int pZ) {
      boolean $$3 = pX > 0 && pX < this.xSize - 1 && pY > 0 && pY < this.ySize - 1 && pZ > 0 && pZ < this.zSize - 1;
      return $$3
         && this.isFull(pX, pY, pZ)
         && this.isFull(pX - 1, pY, pZ)
         && this.isFull(pX + 1, pY, pZ)
         && this.isFull(pX, pY - 1, pZ)
         && this.isFull(pX, pY + 1, pZ)
         && this.isFull(pX, pY, pZ - 1)
         && this.isFull(pX, pY, pZ + 1);
   }
}
