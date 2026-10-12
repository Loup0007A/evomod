package net.minecraft.util;

import net.minecraft.core.Direction;

public class SegmentedAnglePrecision {
   private final int mask;
   private final int precision;
   private final float degreeToAngle;
   private final float angleToDegree;

   public SegmentedAnglePrecision(int pPrecision) {
      if (pPrecision < 2) {
         throw new IllegalArgumentException("Precision cannot be less than 2 bits");
      } else if (pPrecision > 30) {
         throw new IllegalArgumentException("Precision cannot be greater than 30 bits");
      } else {
         int $$1 = 1 << pPrecision;
         this.mask = $$1 - 1;
         this.precision = pPrecision;
         this.degreeToAngle = (float)$$1 / 360.0F;
         this.angleToDegree = 360.0F / (float)$$1;
      }
   }

   public boolean isSameAxis(int pFirst, int pSecond) {
      int $$2 = this.getMask() >> 1;
      return (pFirst & $$2) == (pSecond & $$2);
   }

   public int fromDirection(Direction pDirection) {
      if (pDirection.getAxis().isVertical()) {
         return 0;
      } else {
         int $$1 = pDirection.get2DDataValue();
         return $$1 << this.precision - 2;
      }
   }

   public int fromDegreesWithTurns(float pDegreesWithTurns) {
      return Math.round(pDegreesWithTurns * this.degreeToAngle);
   }

   public int fromDegrees(float pDegrees) {
      return this.normalize(this.fromDegreesWithTurns(pDegrees));
   }

   public float toDegreesWithTurns(int pDegrees) {
      return (float)pDegrees * this.angleToDegree;
   }

   public float toDegrees(int pDegreesWithTurns) {
      float $$1 = this.toDegreesWithTurns(this.normalize(pDegreesWithTurns));
      return $$1 >= 180.0F ? $$1 - 360.0F : $$1;
   }

   public int normalize(int pDegrees) {
      return pDegrees & this.mask;
   }

   public int getMask() {
      return this.mask;
   }
}
