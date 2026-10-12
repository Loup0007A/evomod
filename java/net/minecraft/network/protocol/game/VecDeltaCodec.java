package net.minecraft.network.protocol.game;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.VisibleForTesting;

public class VecDeltaCodec {
   private static final double TRUNCATION_STEPS = 4096.0;
   private Vec3 base = Vec3.ZERO;

   @VisibleForTesting
   static long encode(double pValue) {
      return Math.round(pValue * 4096.0);
   }

   @VisibleForTesting
   static double decode(long pValue) {
      return (double)pValue / 4096.0;
   }

   public Vec3 decode(long pX, long pY, long pZ) {
      if (pX == 0L && pY == 0L && pZ == 0L) {
         return this.base;
      } else {
         double $$3 = pX == 0L ? this.base.x : decode(encode(this.base.x) + pX);
         double $$4 = pY == 0L ? this.base.y : decode(encode(this.base.y) + pY);
         double $$5 = pZ == 0L ? this.base.z : decode(encode(this.base.z) + pZ);
         return new Vec3($$3, $$4, $$5);
      }
   }

   public long encodeX(Vec3 pValue) {
      return encode(pValue.x) - encode(this.base.x);
   }

   public long encodeY(Vec3 pValue) {
      return encode(pValue.y) - encode(this.base.y);
   }

   public long encodeZ(Vec3 pValue) {
      return encode(pValue.z) - encode(this.base.z);
   }

   public Vec3 delta(Vec3 pValue) {
      return pValue.subtract(this.base);
   }

   public void setBase(Vec3 pBase) {
      this.base = pBase;
   }

   public Vec3 getBase() {
      return this.base;
   }
}
