package net.minecraft.world.phys.shapes;

import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;

public abstract class DiscreteVoxelShape {
   private static final Direction.Axis[] AXIS_VALUES = Direction.Axis.values();
   protected final int xSize;
   protected final int ySize;
   protected final int zSize;

   protected DiscreteVoxelShape(int pXSize, int pYSize, int pZSize) {
      if (pXSize >= 0 && pYSize >= 0 && pZSize >= 0) {
         this.xSize = pXSize;
         this.ySize = pYSize;
         this.zSize = pZSize;
      } else {
         throw new IllegalArgumentException("Need all positive sizes: x: " + pXSize + ", y: " + pYSize + ", z: " + pZSize);
      }
   }

   public boolean isFullWide(AxisCycle pAxis, int pX, int pY, int pZ) {
      return this.isFullWide(pAxis.cycle(pX, pY, pZ, Direction.Axis.X), pAxis.cycle(pX, pY, pZ, Direction.Axis.Y), pAxis.cycle(pX, pY, pZ, Direction.Axis.Z));
   }

   public boolean isFullWide(int pX, int pY, int pZ) {
      if (pX < 0 || pY < 0 || pZ < 0) {
         return false;
      } else {
         return pX < this.xSize && pY < this.ySize && pZ < this.zSize ? this.isFull(pX, pY, pZ) : false;
      }
   }

   public boolean isFull(AxisCycle pRotation, int pX, int pY, int pZ) {
      return this.isFull(
         pRotation.cycle(pX, pY, pZ, Direction.Axis.X), pRotation.cycle(pX, pY, pZ, Direction.Axis.Y), pRotation.cycle(pX, pY, pZ, Direction.Axis.Z)
      );
   }

   public abstract boolean isFull(int pX, int pY, int pZ);

   public abstract void fill(int pX, int pY, int pZ);

   public boolean isEmpty() {
      for (Direction.Axis $$0 : AXIS_VALUES) {
         if (this.firstFull($$0) >= this.lastFull($$0)) {
            return true;
         }
      }

      return false;
   }

   public abstract int firstFull(Direction.Axis pAxis);

   public abstract int lastFull(Direction.Axis pAxis);

   public int firstFull(Direction.Axis pAxis, int pY, int pZ) {
      int $$3 = this.getSize(pAxis);
      if (pY >= 0 && pZ >= 0) {
         Direction.Axis $$4 = AxisCycle.FORWARD.cycle(pAxis);
         Direction.Axis $$5 = AxisCycle.BACKWARD.cycle(pAxis);
         if (pY < this.getSize($$4) && pZ < this.getSize($$5)) {
            AxisCycle $$6 = AxisCycle.between(Direction.Axis.X, pAxis);

            for (int $$7 = 0; $$7 < $$3; $$7++) {
               if (this.isFull($$6, $$7, pY, pZ)) {
                  return $$7;
               }
            }

            return $$3;
         } else {
            return $$3;
         }
      } else {
         return $$3;
      }
   }

   public int lastFull(Direction.Axis pAxis, int pY, int pZ) {
      if (pY >= 0 && pZ >= 0) {
         Direction.Axis $$3 = AxisCycle.FORWARD.cycle(pAxis);
         Direction.Axis $$4 = AxisCycle.BACKWARD.cycle(pAxis);
         if (pY < this.getSize($$3) && pZ < this.getSize($$4)) {
            int $$5 = this.getSize(pAxis);
            AxisCycle $$6 = AxisCycle.between(Direction.Axis.X, pAxis);

            for (int $$7 = $$5 - 1; $$7 >= 0; $$7--) {
               if (this.isFull($$6, $$7, pY, pZ)) {
                  return $$7 + 1;
               }
            }

            return 0;
         } else {
            return 0;
         }
      } else {
         return 0;
      }
   }

   public int getSize(Direction.Axis pAxis) {
      return pAxis.choose(this.xSize, this.ySize, this.zSize);
   }

   public int getXSize() {
      return this.getSize(Direction.Axis.X);
   }

   public int getYSize() {
      return this.getSize(Direction.Axis.Y);
   }

   public int getZSize() {
      return this.getSize(Direction.Axis.Z);
   }

   public void forAllEdges(DiscreteVoxelShape.IntLineConsumer pConsumer, boolean pCombine) {
      this.forAllAxisEdges(pConsumer, AxisCycle.NONE, pCombine);
      this.forAllAxisEdges(pConsumer, AxisCycle.FORWARD, pCombine);
      this.forAllAxisEdges(pConsumer, AxisCycle.BACKWARD, pCombine);
   }

   private void forAllAxisEdges(DiscreteVoxelShape.IntLineConsumer pLineConsumer, AxisCycle pAxis, boolean pCombine) {
      AxisCycle $$3 = pAxis.inverse();
      int $$4 = this.getSize($$3.cycle(Direction.Axis.X));
      int $$5 = this.getSize($$3.cycle(Direction.Axis.Y));
      int $$6 = this.getSize($$3.cycle(Direction.Axis.Z));

      for (int $$7 = 0; $$7 <= $$4; $$7++) {
         for (int $$8 = 0; $$8 <= $$5; $$8++) {
            int $$9 = -1;

            for (int $$10 = 0; $$10 <= $$6; $$10++) {
               int $$11 = 0;
               int $$12 = 0;

               for (int $$13 = 0; $$13 <= 1; $$13++) {
                  for (int $$14 = 0; $$14 <= 1; $$14++) {
                     if (this.isFullWide($$3, $$7 + $$13 - 1, $$8 + $$14 - 1, $$10)) {
                        $$11++;
                        $$12 ^= $$13 ^ $$14;
                     }
                  }
               }

               if ($$11 == 1 || $$11 == 3 || $$11 == 2 && ($$12 & 1) == 0) {
                  if (pCombine) {
                     if ($$9 == -1) {
                        $$9 = $$10;
                     }
                  } else {
                     pLineConsumer.consume(
                        $$3.cycle($$7, $$8, $$10, Direction.Axis.X),
                        $$3.cycle($$7, $$8, $$10, Direction.Axis.Y),
                        $$3.cycle($$7, $$8, $$10, Direction.Axis.Z),
                        $$3.cycle($$7, $$8, $$10 + 1, Direction.Axis.X),
                        $$3.cycle($$7, $$8, $$10 + 1, Direction.Axis.Y),
                        $$3.cycle($$7, $$8, $$10 + 1, Direction.Axis.Z)
                     );
                  }
               } else if ($$9 != -1) {
                  pLineConsumer.consume(
                     $$3.cycle($$7, $$8, $$9, Direction.Axis.X),
                     $$3.cycle($$7, $$8, $$9, Direction.Axis.Y),
                     $$3.cycle($$7, $$8, $$9, Direction.Axis.Z),
                     $$3.cycle($$7, $$8, $$10, Direction.Axis.X),
                     $$3.cycle($$7, $$8, $$10, Direction.Axis.Y),
                     $$3.cycle($$7, $$8, $$10, Direction.Axis.Z)
                  );
                  $$9 = -1;
               }
            }
         }
      }
   }

   public void forAllBoxes(DiscreteVoxelShape.IntLineConsumer pConsumer, boolean pCombine) {
      BitSetDiscreteVoxelShape.forAllBoxes(this, pConsumer, pCombine);
   }

   public void forAllFaces(DiscreteVoxelShape.IntFaceConsumer pFaceConsumer) {
      this.forAllAxisFaces(pFaceConsumer, AxisCycle.NONE);
      this.forAllAxisFaces(pFaceConsumer, AxisCycle.FORWARD);
      this.forAllAxisFaces(pFaceConsumer, AxisCycle.BACKWARD);
   }

   private void forAllAxisFaces(DiscreteVoxelShape.IntFaceConsumer pFaceConsumer, AxisCycle pAxisRotation) {
      AxisCycle $$2 = pAxisRotation.inverse();
      Direction.Axis $$3 = $$2.cycle(Direction.Axis.Z);
      int $$4 = this.getSize($$2.cycle(Direction.Axis.X));
      int $$5 = this.getSize($$2.cycle(Direction.Axis.Y));
      int $$6 = this.getSize($$3);
      Direction $$7 = Direction.fromAxisAndDirection($$3, Direction.AxisDirection.NEGATIVE);
      Direction $$8 = Direction.fromAxisAndDirection($$3, Direction.AxisDirection.POSITIVE);

      for (int $$9 = 0; $$9 < $$4; $$9++) {
         for (int $$10 = 0; $$10 < $$5; $$10++) {
            boolean $$11 = false;

            for (int $$12 = 0; $$12 <= $$6; $$12++) {
               boolean $$13 = $$12 != $$6 && this.isFull($$2, $$9, $$10, $$12);
               if (!$$11 && $$13) {
                  pFaceConsumer.consume(
                     $$7,
                     $$2.cycle($$9, $$10, $$12, Direction.Axis.X),
                     $$2.cycle($$9, $$10, $$12, Direction.Axis.Y),
                     $$2.cycle($$9, $$10, $$12, Direction.Axis.Z)
                  );
               }

               if ($$11 && !$$13) {
                  pFaceConsumer.consume(
                     $$8,
                     $$2.cycle($$9, $$10, $$12 - 1, Direction.Axis.X),
                     $$2.cycle($$9, $$10, $$12 - 1, Direction.Axis.Y),
                     $$2.cycle($$9, $$10, $$12 - 1, Direction.Axis.Z)
                  );
               }

               $$11 = $$13;
            }
         }
      }
   }

   public interface IntFaceConsumer {
      void consume(Direction pDirection, int pX, int pY, int pZ);
   }

   public interface IntLineConsumer {
      void consume(int pX1, int pY1, int pZ1, int pX2, int pY2, int pZ2);
   }
}
