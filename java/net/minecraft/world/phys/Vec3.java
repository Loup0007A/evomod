package net.minecraft.world.phys;

import com.mojang.serialization.Codec;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

public class Vec3 implements Position {
   public static final Codec<Vec3> CODEC = Codec.DOUBLE
      .listOf()
      .comapFlatMap(
         p_327658_ -> Util.fixedSize(p_327658_, 3).map(p_231081_ -> new Vec3((Double)p_231081_.get(0), (Double)p_231081_.get(1), (Double)p_231081_.get(2))),
         p_231083_ -> List.of(p_231083_.x(), p_231083_.y(), p_231083_.z())
      );
   public static final Vec3 ZERO = new Vec3(0.0, 0.0, 0.0);
   public final double x;
   public final double y;
   public final double z;

   public static Vec3 fromRGB24(int pPacked) {
      double $$1 = (double)(pPacked >> 16 & 0xFF) / 255.0;
      double $$2 = (double)(pPacked >> 8 & 0xFF) / 255.0;
      double $$3 = (double)(pPacked & 0xFF) / 255.0;
      return new Vec3($$1, $$2, $$3);
   }

   public static Vec3 atLowerCornerOf(Vec3i pToCopy) {
      return new Vec3((double)pToCopy.getX(), (double)pToCopy.getY(), (double)pToCopy.getZ());
   }

   public static Vec3 atLowerCornerWithOffset(Vec3i pToCopy, double pOffsetX, double pOffsetY, double pOffsetZ) {
      return new Vec3((double)pToCopy.getX() + pOffsetX, (double)pToCopy.getY() + pOffsetY, (double)pToCopy.getZ() + pOffsetZ);
   }

   public static Vec3 atCenterOf(Vec3i pToCopy) {
      return atLowerCornerWithOffset(pToCopy, 0.5, 0.5, 0.5);
   }

   public static Vec3 atBottomCenterOf(Vec3i pToCopy) {
      return atLowerCornerWithOffset(pToCopy, 0.5, 0.0, 0.5);
   }

   public static Vec3 upFromBottomCenterOf(Vec3i pToCopy, double pVerticalOffset) {
      return atLowerCornerWithOffset(pToCopy, 0.5, pVerticalOffset, 0.5);
   }

   public Vec3(double pX, double pY, double pZ) {
      this.x = pX;
      this.y = pY;
      this.z = pZ;
   }

   public Vec3(Vector3f pVector) {
      this((double)pVector.x(), (double)pVector.y(), (double)pVector.z());
   }

   public Vec3 vectorTo(Vec3 pVec) {
      return new Vec3(pVec.x - this.x, pVec.y - this.y, pVec.z - this.z);
   }

   public Vec3 normalize() {
      double $$0 = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
      return $$0 < 1.0E-4 ? ZERO : new Vec3(this.x / $$0, this.y / $$0, this.z / $$0);
   }

   public double dot(Vec3 pVec) {
      return this.x * pVec.x + this.y * pVec.y + this.z * pVec.z;
   }

   public Vec3 cross(Vec3 pVec) {
      return new Vec3(this.y * pVec.z - this.z * pVec.y, this.z * pVec.x - this.x * pVec.z, this.x * pVec.y - this.y * pVec.x);
   }

   public Vec3 subtract(Vec3 pVec) {
      return this.subtract(pVec.x, pVec.y, pVec.z);
   }

   public Vec3 subtract(double pX, double pY, double pZ) {
      return this.add(-pX, -pY, -pZ);
   }

   public Vec3 add(Vec3 pVec) {
      return this.add(pVec.x, pVec.y, pVec.z);
   }

   public Vec3 add(double pX, double pY, double pZ) {
      return new Vec3(this.x + pX, this.y + pY, this.z + pZ);
   }

   public boolean closerThan(Position pPos, double pDistance) {
      return this.distanceToSqr(pPos.x(), pPos.y(), pPos.z()) < pDistance * pDistance;
   }

   public double distanceTo(Vec3 pVec) {
      double $$1 = pVec.x - this.x;
      double $$2 = pVec.y - this.y;
      double $$3 = pVec.z - this.z;
      return Math.sqrt($$1 * $$1 + $$2 * $$2 + $$3 * $$3);
   }

   public double distanceToSqr(Vec3 pVec) {
      double $$1 = pVec.x - this.x;
      double $$2 = pVec.y - this.y;
      double $$3 = pVec.z - this.z;
      return $$1 * $$1 + $$2 * $$2 + $$3 * $$3;
   }

   public double distanceToSqr(double pX, double pY, double pZ) {
      double $$3 = pX - this.x;
      double $$4 = pY - this.y;
      double $$5 = pZ - this.z;
      return $$3 * $$3 + $$4 * $$4 + $$5 * $$5;
   }

   public boolean closerThan(Vec3 pPos, double pHorizontalDistance, double pVerticalDistance) {
      double $$3 = pPos.x() - this.x;
      double $$4 = pPos.y() - this.y;
      double $$5 = pPos.z() - this.z;
      return Mth.lengthSquared($$3, $$5) < Mth.square(pHorizontalDistance) && Math.abs($$4) < pVerticalDistance;
   }

   public Vec3 scale(double pFactor) {
      return this.multiply(pFactor, pFactor, pFactor);
   }

   public Vec3 reverse() {
      return this.scale(-1.0);
   }

   public Vec3 multiply(Vec3 pVec) {
      return this.multiply(pVec.x, pVec.y, pVec.z);
   }

   public Vec3 multiply(double pFactorX, double pFactorY, double pFactorZ) {
      return new Vec3(this.x * pFactorX, this.y * pFactorY, this.z * pFactorZ);
   }

   public Vec3 offsetRandom(RandomSource pRandom, float pFactor) {
      return this.add(
         (double)((pRandom.nextFloat() - 0.5F) * pFactor), (double)((pRandom.nextFloat() - 0.5F) * pFactor), (double)((pRandom.nextFloat() - 0.5F) * pFactor)
      );
   }

   public double length() {
      return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
   }

   public double lengthSqr() {
      return this.x * this.x + this.y * this.y + this.z * this.z;
   }

   public double horizontalDistance() {
      return Math.sqrt(this.x * this.x + this.z * this.z);
   }

   public double horizontalDistanceSqr() {
      return this.x * this.x + this.z * this.z;
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (!(pOther instanceof Vec3 $$1)) {
         return false;
      } else if (Double.compare($$1.x, this.x) != 0) {
         return false;
      } else {
         return Double.compare($$1.y, this.y) != 0 ? false : Double.compare($$1.z, this.z) == 0;
      }
   }

   @Override
   public int hashCode() {
      long $$0 = Double.doubleToLongBits(this.x);
      int $$1 = (int)($$0 ^ $$0 >>> 32);
      $$0 = Double.doubleToLongBits(this.y);
      $$1 = 31 * $$1 + (int)($$0 ^ $$0 >>> 32);
      $$0 = Double.doubleToLongBits(this.z);
      return 31 * $$1 + (int)($$0 ^ $$0 >>> 32);
   }

   @Override
   public String toString() {
      return "(" + this.x + ", " + this.y + ", " + this.z + ")";
   }

   public Vec3 lerp(Vec3 pTo, double pDelta) {
      return new Vec3(Mth.lerp(pDelta, this.x, pTo.x), Mth.lerp(pDelta, this.y, pTo.y), Mth.lerp(pDelta, this.z, pTo.z));
   }

   public Vec3 xRot(float pPitch) {
      float $$1 = Mth.cos(pPitch);
      float $$2 = Mth.sin(pPitch);
      double $$3 = this.x;
      double $$4 = this.y * (double)$$1 + this.z * (double)$$2;
      double $$5 = this.z * (double)$$1 - this.y * (double)$$2;
      return new Vec3($$3, $$4, $$5);
   }

   public Vec3 yRot(float pYaw) {
      float $$1 = Mth.cos(pYaw);
      float $$2 = Mth.sin(pYaw);
      double $$3 = this.x * (double)$$1 + this.z * (double)$$2;
      double $$4 = this.y;
      double $$5 = this.z * (double)$$1 - this.x * (double)$$2;
      return new Vec3($$3, $$4, $$5);
   }

   public Vec3 zRot(float pRoll) {
      float $$1 = Mth.cos(pRoll);
      float $$2 = Mth.sin(pRoll);
      double $$3 = this.x * (double)$$1 + this.y * (double)$$2;
      double $$4 = this.y * (double)$$1 - this.x * (double)$$2;
      double $$5 = this.z;
      return new Vec3($$3, $$4, $$5);
   }

   public static Vec3 directionFromRotation(Vec2 pVec) {
      return directionFromRotation(pVec.x, pVec.y);
   }

   public static Vec3 directionFromRotation(float pPitch, float pYaw) {
      float $$2 = Mth.cos(-pYaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float $$3 = Mth.sin(-pYaw * (float) (Math.PI / 180.0) - (float) Math.PI);
      float $$4 = -Mth.cos(-pPitch * (float) (Math.PI / 180.0));
      float $$5 = Mth.sin(-pPitch * (float) (Math.PI / 180.0));
      return new Vec3((double)($$3 * $$4), (double)$$5, (double)($$2 * $$4));
   }

   public Vec3 align(EnumSet<Direction.Axis> pAxes) {
      double $$1 = pAxes.contains(Direction.Axis.X) ? (double)Mth.floor(this.x) : this.x;
      double $$2 = pAxes.contains(Direction.Axis.Y) ? (double)Mth.floor(this.y) : this.y;
      double $$3 = pAxes.contains(Direction.Axis.Z) ? (double)Mth.floor(this.z) : this.z;
      return new Vec3($$1, $$2, $$3);
   }

   public double get(Direction.Axis pAxis) {
      return pAxis.choose(this.x, this.y, this.z);
   }

   public Vec3 with(Direction.Axis pAxis, double pLength) {
      double $$2 = pAxis == Direction.Axis.X ? pLength : this.x;
      double $$3 = pAxis == Direction.Axis.Y ? pLength : this.y;
      double $$4 = pAxis == Direction.Axis.Z ? pLength : this.z;
      return new Vec3($$2, $$3, $$4);
   }

   public Vec3 relative(Direction pDirection, double pLength) {
      Vec3i $$2 = pDirection.getNormal();
      return new Vec3(this.x + pLength * (double)$$2.getX(), this.y + pLength * (double)$$2.getY(), this.z + pLength * (double)$$2.getZ());
   }

   @Override
   public final double x() {
      return this.x;
   }

   @Override
   public final double y() {
      return this.y;
   }

   @Override
   public final double z() {
      return this.z;
   }

   public Vector3f toVector3f() {
      return new Vector3f((float)this.x, (float)this.y, (float)this.z);
   }
}
