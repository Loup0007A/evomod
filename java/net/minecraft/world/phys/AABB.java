package net.minecraft.world.phys;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.joml.Vector3f;

public class AABB {
   private static final double EPSILON = 1.0E-7;
   public final double minX;
   public final double minY;
   public final double minZ;
   public final double maxX;
   public final double maxY;
   public final double maxZ;

   public AABB(double pX1, double pY1, double pZ1, double pX2, double pY2, double pZ2) {
      this.minX = Math.min(pX1, pX2);
      this.minY = Math.min(pY1, pY2);
      this.minZ = Math.min(pZ1, pZ2);
      this.maxX = Math.max(pX1, pX2);
      this.maxY = Math.max(pY1, pY2);
      this.maxZ = Math.max(pZ1, pZ2);
   }

   public AABB(BlockPos pPos) {
      this((double)pPos.getX(), (double)pPos.getY(), (double)pPos.getZ(), (double)(pPos.getX() + 1), (double)(pPos.getY() + 1), (double)(pPos.getZ() + 1));
   }

   public AABB(Vec3 pStart, Vec3 pEnd) {
      this(pStart.x, pStart.y, pStart.z, pEnd.x, pEnd.y, pEnd.z);
   }

   public static AABB of(BoundingBox pMutableBox) {
      return new AABB(
         (double)pMutableBox.minX(),
         (double)pMutableBox.minY(),
         (double)pMutableBox.minZ(),
         (double)(pMutableBox.maxX() + 1),
         (double)(pMutableBox.maxY() + 1),
         (double)(pMutableBox.maxZ() + 1)
      );
   }

   public static AABB unitCubeFromLowerCorner(Vec3 pVector) {
      return new AABB(pVector.x, pVector.y, pVector.z, pVector.x + 1.0, pVector.y + 1.0, pVector.z + 1.0);
   }

   public static AABB encapsulatingFullBlocks(BlockPos pStartPos, BlockPos pEndPos) {
      return new AABB(
         (double)Math.min(pStartPos.getX(), pEndPos.getX()),
         (double)Math.min(pStartPos.getY(), pEndPos.getY()),
         (double)Math.min(pStartPos.getZ(), pEndPos.getZ()),
         (double)(Math.max(pStartPos.getX(), pEndPos.getX()) + 1),
         (double)(Math.max(pStartPos.getY(), pEndPos.getY()) + 1),
         (double)(Math.max(pStartPos.getZ(), pEndPos.getZ()) + 1)
      );
   }

   public AABB setMinX(double pMinX) {
      return new AABB(pMinX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ);
   }

   public AABB setMinY(double pMinY) {
      return new AABB(this.minX, pMinY, this.minZ, this.maxX, this.maxY, this.maxZ);
   }

   public AABB setMinZ(double pMinZ) {
      return new AABB(this.minX, this.minY, pMinZ, this.maxX, this.maxY, this.maxZ);
   }

   public AABB setMaxX(double pMaxX) {
      return new AABB(this.minX, this.minY, this.minZ, pMaxX, this.maxY, this.maxZ);
   }

   public AABB setMaxY(double pMaxY) {
      return new AABB(this.minX, this.minY, this.minZ, this.maxX, pMaxY, this.maxZ);
   }

   public AABB setMaxZ(double pMaxZ) {
      return new AABB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, pMaxZ);
   }

   public double min(Direction.Axis pAxis) {
      return pAxis.choose(this.minX, this.minY, this.minZ);
   }

   public double max(Direction.Axis pAxis) {
      return pAxis.choose(this.maxX, this.maxY, this.maxZ);
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (!(pOther instanceof AABB $$1)) {
         return false;
      } else if (Double.compare($$1.minX, this.minX) != 0) {
         return false;
      } else if (Double.compare($$1.minY, this.minY) != 0) {
         return false;
      } else if (Double.compare($$1.minZ, this.minZ) != 0) {
         return false;
      } else if (Double.compare($$1.maxX, this.maxX) != 0) {
         return false;
      } else {
         return Double.compare($$1.maxY, this.maxY) != 0 ? false : Double.compare($$1.maxZ, this.maxZ) == 0;
      }
   }

   @Override
   public int hashCode() {
      long $$0 = Double.doubleToLongBits(this.minX);
      int $$1 = (int)($$0 ^ $$0 >>> 32);
      $$0 = Double.doubleToLongBits(this.minY);
      $$1 = 31 * $$1 + (int)($$0 ^ $$0 >>> 32);
      $$0 = Double.doubleToLongBits(this.minZ);
      $$1 = 31 * $$1 + (int)($$0 ^ $$0 >>> 32);
      $$0 = Double.doubleToLongBits(this.maxX);
      $$1 = 31 * $$1 + (int)($$0 ^ $$0 >>> 32);
      $$0 = Double.doubleToLongBits(this.maxY);
      $$1 = 31 * $$1 + (int)($$0 ^ $$0 >>> 32);
      $$0 = Double.doubleToLongBits(this.maxZ);
      return 31 * $$1 + (int)($$0 ^ $$0 >>> 32);
   }

   public AABB contract(double pX, double pY, double pZ) {
      double $$3 = this.minX;
      double $$4 = this.minY;
      double $$5 = this.minZ;
      double $$6 = this.maxX;
      double $$7 = this.maxY;
      double $$8 = this.maxZ;
      if (pX < 0.0) {
         $$3 -= pX;
      } else if (pX > 0.0) {
         $$6 -= pX;
      }

      if (pY < 0.0) {
         $$4 -= pY;
      } else if (pY > 0.0) {
         $$7 -= pY;
      }

      if (pZ < 0.0) {
         $$5 -= pZ;
      } else if (pZ > 0.0) {
         $$8 -= pZ;
      }

      return new AABB($$3, $$4, $$5, $$6, $$7, $$8);
   }

   public AABB expandTowards(Vec3 pVector) {
      return this.expandTowards(pVector.x, pVector.y, pVector.z);
   }

   public AABB expandTowards(double pX, double pY, double pZ) {
      double $$3 = this.minX;
      double $$4 = this.minY;
      double $$5 = this.minZ;
      double $$6 = this.maxX;
      double $$7 = this.maxY;
      double $$8 = this.maxZ;
      if (pX < 0.0) {
         $$3 += pX;
      } else if (pX > 0.0) {
         $$6 += pX;
      }

      if (pY < 0.0) {
         $$4 += pY;
      } else if (pY > 0.0) {
         $$7 += pY;
      }

      if (pZ < 0.0) {
         $$5 += pZ;
      } else if (pZ > 0.0) {
         $$8 += pZ;
      }

      return new AABB($$3, $$4, $$5, $$6, $$7, $$8);
   }

   public AABB inflate(double pX, double pY, double pZ) {
      double $$3 = this.minX - pX;
      double $$4 = this.minY - pY;
      double $$5 = this.minZ - pZ;
      double $$6 = this.maxX + pX;
      double $$7 = this.maxY + pY;
      double $$8 = this.maxZ + pZ;
      return new AABB($$3, $$4, $$5, $$6, $$7, $$8);
   }

   public AABB inflate(double pValue) {
      return this.inflate(pValue, pValue, pValue);
   }

   public AABB intersect(AABB pOther) {
      double $$1 = Math.max(this.minX, pOther.minX);
      double $$2 = Math.max(this.minY, pOther.minY);
      double $$3 = Math.max(this.minZ, pOther.minZ);
      double $$4 = Math.min(this.maxX, pOther.maxX);
      double $$5 = Math.min(this.maxY, pOther.maxY);
      double $$6 = Math.min(this.maxZ, pOther.maxZ);
      return new AABB($$1, $$2, $$3, $$4, $$5, $$6);
   }

   public AABB minmax(AABB pOther) {
      double $$1 = Math.min(this.minX, pOther.minX);
      double $$2 = Math.min(this.minY, pOther.minY);
      double $$3 = Math.min(this.minZ, pOther.minZ);
      double $$4 = Math.max(this.maxX, pOther.maxX);
      double $$5 = Math.max(this.maxY, pOther.maxY);
      double $$6 = Math.max(this.maxZ, pOther.maxZ);
      return new AABB($$1, $$2, $$3, $$4, $$5, $$6);
   }

   public AABB move(double pX, double pY, double pZ) {
      return new AABB(this.minX + pX, this.minY + pY, this.minZ + pZ, this.maxX + pX, this.maxY + pY, this.maxZ + pZ);
   }

   public AABB move(BlockPos pPos) {
      return new AABB(
         this.minX + (double)pPos.getX(),
         this.minY + (double)pPos.getY(),
         this.minZ + (double)pPos.getZ(),
         this.maxX + (double)pPos.getX(),
         this.maxY + (double)pPos.getY(),
         this.maxZ + (double)pPos.getZ()
      );
   }

   public AABB move(Vec3 pVec) {
      return this.move(pVec.x, pVec.y, pVec.z);
   }

   public AABB move(Vector3f pVec) {
      return this.move((double)pVec.x, (double)pVec.y, (double)pVec.z);
   }

   public boolean intersects(AABB pOther) {
      return this.intersects(pOther.minX, pOther.minY, pOther.minZ, pOther.maxX, pOther.maxY, pOther.maxZ);
   }

   public boolean intersects(double pX1, double pY1, double pZ1, double pX2, double pY2, double pZ2) {
      return this.minX < pX2 && this.maxX > pX1 && this.minY < pY2 && this.maxY > pY1 && this.minZ < pZ2 && this.maxZ > pZ1;
   }

   public boolean intersects(Vec3 pMin, Vec3 pMax) {
      return this.intersects(
         Math.min(pMin.x, pMax.x),
         Math.min(pMin.y, pMax.y),
         Math.min(pMin.z, pMax.z),
         Math.max(pMin.x, pMax.x),
         Math.max(pMin.y, pMax.y),
         Math.max(pMin.z, pMax.z)
      );
   }

   public boolean contains(Vec3 pVec) {
      return this.contains(pVec.x, pVec.y, pVec.z);
   }

   public boolean contains(double pX, double pY, double pZ) {
      return pX >= this.minX && pX < this.maxX && pY >= this.minY && pY < this.maxY && pZ >= this.minZ && pZ < this.maxZ;
   }

   public double getSize() {
      double $$0 = this.getXsize();
      double $$1 = this.getYsize();
      double $$2 = this.getZsize();
      return ($$0 + $$1 + $$2) / 3.0;
   }

   public double getXsize() {
      return this.maxX - this.minX;
   }

   public double getYsize() {
      return this.maxY - this.minY;
   }

   public double getZsize() {
      return this.maxZ - this.minZ;
   }

   public AABB deflate(double pX, double pY, double pZ) {
      return this.inflate(-pX, -pY, -pZ);
   }

   public AABB deflate(double pValue) {
      return this.inflate(-pValue);
   }

   public Optional<Vec3> clip(Vec3 pFrom, Vec3 pTo) {
      double[] $$2 = new double[]{1.0};
      double $$3 = pTo.x - pFrom.x;
      double $$4 = pTo.y - pFrom.y;
      double $$5 = pTo.z - pFrom.z;
      Direction $$6 = getDirection(this, pFrom, $$2, null, $$3, $$4, $$5);
      if ($$6 == null) {
         return Optional.empty();
      } else {
         double $$7 = $$2[0];
         return Optional.of(pFrom.add($$7 * $$3, $$7 * $$4, $$7 * $$5));
      }
   }

   @Nullable
   public static BlockHitResult clip(Iterable<AABB> pBoxes, Vec3 pStart, Vec3 pEnd, BlockPos pPos) {
      double[] $$4 = new double[]{1.0};
      Direction $$5 = null;
      double $$6 = pEnd.x - pStart.x;
      double $$7 = pEnd.y - pStart.y;
      double $$8 = pEnd.z - pStart.z;

      for (AABB $$9 : pBoxes) {
         $$5 = getDirection($$9.move(pPos), pStart, $$4, $$5, $$6, $$7, $$8);
      }

      if ($$5 == null) {
         return null;
      } else {
         double $$10 = $$4[0];
         return new BlockHitResult(pStart.add($$10 * $$6, $$10 * $$7, $$10 * $$8), $$5, pPos, false);
      }
   }

   @Nullable
   private static Direction getDirection(
      AABB pAabb, Vec3 pStart, double[] pMinDistance, @Nullable Direction pFacing, double pDeltaX, double pDeltaY, double pDeltaZ
   ) {
      if (pDeltaX > 1.0E-7) {
         pFacing = clipPoint(
            pMinDistance,
            pFacing,
            pDeltaX,
            pDeltaY,
            pDeltaZ,
            pAabb.minX,
            pAabb.minY,
            pAabb.maxY,
            pAabb.minZ,
            pAabb.maxZ,
            Direction.WEST,
            pStart.x,
            pStart.y,
            pStart.z
         );
      } else if (pDeltaX < -1.0E-7) {
         pFacing = clipPoint(
            pMinDistance,
            pFacing,
            pDeltaX,
            pDeltaY,
            pDeltaZ,
            pAabb.maxX,
            pAabb.minY,
            pAabb.maxY,
            pAabb.minZ,
            pAabb.maxZ,
            Direction.EAST,
            pStart.x,
            pStart.y,
            pStart.z
         );
      }

      if (pDeltaY > 1.0E-7) {
         pFacing = clipPoint(
            pMinDistance,
            pFacing,
            pDeltaY,
            pDeltaZ,
            pDeltaX,
            pAabb.minY,
            pAabb.minZ,
            pAabb.maxZ,
            pAabb.minX,
            pAabb.maxX,
            Direction.DOWN,
            pStart.y,
            pStart.z,
            pStart.x
         );
      } else if (pDeltaY < -1.0E-7) {
         pFacing = clipPoint(
            pMinDistance,
            pFacing,
            pDeltaY,
            pDeltaZ,
            pDeltaX,
            pAabb.maxY,
            pAabb.minZ,
            pAabb.maxZ,
            pAabb.minX,
            pAabb.maxX,
            Direction.UP,
            pStart.y,
            pStart.z,
            pStart.x
         );
      }

      if (pDeltaZ > 1.0E-7) {
         pFacing = clipPoint(
            pMinDistance,
            pFacing,
            pDeltaZ,
            pDeltaX,
            pDeltaY,
            pAabb.minZ,
            pAabb.minX,
            pAabb.maxX,
            pAabb.minY,
            pAabb.maxY,
            Direction.NORTH,
            pStart.z,
            pStart.x,
            pStart.y
         );
      } else if (pDeltaZ < -1.0E-7) {
         pFacing = clipPoint(
            pMinDistance,
            pFacing,
            pDeltaZ,
            pDeltaX,
            pDeltaY,
            pAabb.maxZ,
            pAabb.minX,
            pAabb.maxX,
            pAabb.minY,
            pAabb.maxY,
            Direction.SOUTH,
            pStart.z,
            pStart.x,
            pStart.y
         );
      }

      return pFacing;
   }

   @Nullable
   private static Direction clipPoint(
      double[] pMinDistance,
      @Nullable Direction pPrevDirection,
      double pDistanceSide,
      double pDistanceOtherA,
      double pDistanceOtherB,
      double pMinSide,
      double pMinOtherA,
      double pMaxOtherA,
      double pMinOtherB,
      double pMaxOtherB,
      Direction pHitSide,
      double pStartSide,
      double pStartOtherA,
      double pStartOtherB
   ) {
      double $$14 = (pMinSide - pStartSide) / pDistanceSide;
      double $$15 = pStartOtherA + $$14 * pDistanceOtherA;
      double $$16 = pStartOtherB + $$14 * pDistanceOtherB;
      if (0.0 < $$14
         && $$14 < pMinDistance[0]
         && pMinOtherA - 1.0E-7 < $$15
         && $$15 < pMaxOtherA + 1.0E-7
         && pMinOtherB - 1.0E-7 < $$16
         && $$16 < pMaxOtherB + 1.0E-7) {
         pMinDistance[0] = $$14;
         return pHitSide;
      } else {
         return pPrevDirection;
      }
   }

   public double distanceToSqr(Vec3 pVec) {
      double $$1 = Math.max(Math.max(this.minX - pVec.x, pVec.x - this.maxX), 0.0);
      double $$2 = Math.max(Math.max(this.minY - pVec.y, pVec.y - this.maxY), 0.0);
      double $$3 = Math.max(Math.max(this.minZ - pVec.z, pVec.z - this.maxZ), 0.0);
      return Mth.lengthSquared($$1, $$2, $$3);
   }

   @Override
   public String toString() {
      return "AABB[" + this.minX + ", " + this.minY + ", " + this.minZ + "] -> [" + this.maxX + ", " + this.maxY + ", " + this.maxZ + "]";
   }

   public boolean hasNaN() {
      return Double.isNaN(this.minX)
         || Double.isNaN(this.minY)
         || Double.isNaN(this.minZ)
         || Double.isNaN(this.maxX)
         || Double.isNaN(this.maxY)
         || Double.isNaN(this.maxZ);
   }

   public Vec3 getCenter() {
      return new Vec3(Mth.lerp(0.5, this.minX, this.maxX), Mth.lerp(0.5, this.minY, this.maxY), Mth.lerp(0.5, this.minZ, this.maxZ));
   }

   public Vec3 getBottomCenter() {
      return new Vec3(Mth.lerp(0.5, this.minX, this.maxX), this.minY, Mth.lerp(0.5, this.minZ, this.maxZ));
   }

   public Vec3 getMinPosition() {
      return new Vec3(this.minX, this.minY, this.minZ);
   }

   public Vec3 getMaxPosition() {
      return new Vec3(this.maxX, this.maxY, this.maxZ);
   }

   public static AABB ofSize(Vec3 pCenter, double pXSize, double pYSize, double pZSize) {
      return new AABB(
         pCenter.x - pXSize / 2.0,
         pCenter.y - pYSize / 2.0,
         pCenter.z - pZSize / 2.0,
         pCenter.x + pXSize / 2.0,
         pCenter.y + pYSize / 2.0,
         pCenter.z + pZSize / 2.0
      );
   }
}
