package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class ImprovedNoise {
   private static final float SHIFT_UP_EPSILON = 1.0E-7F;
   private final byte[] p;
   public final double xo;
   public final double yo;
   public final double zo;

   public ImprovedNoise(RandomSource pRandom) {
      this.xo = pRandom.nextDouble() * 256.0;
      this.yo = pRandom.nextDouble() * 256.0;
      this.zo = pRandom.nextDouble() * 256.0;
      this.p = new byte[256];

      for (int $$1 = 0; $$1 < 256; $$1++) {
         this.p[$$1] = (byte)$$1;
      }

      for (int $$2 = 0; $$2 < 256; $$2++) {
         int $$3 = pRandom.nextInt(256 - $$2);
         byte $$4 = this.p[$$2];
         this.p[$$2] = this.p[$$2 + $$3];
         this.p[$$2 + $$3] = $$4;
      }
   }

   public double noise(double pX, double pY, double pZ) {
      return this.noise(pX, pY, pZ, 0.0, 0.0);
   }

   @Deprecated
   public double noise(double pX, double pY, double pZ, double pYScale, double pYMax) {
      double $$5 = pX + this.xo;
      double $$6 = pY + this.yo;
      double $$7 = pZ + this.zo;
      int $$8 = Mth.floor($$5);
      int $$9 = Mth.floor($$6);
      int $$10 = Mth.floor($$7);
      double $$11 = $$5 - (double)$$8;
      double $$12 = $$6 - (double)$$9;
      double $$13 = $$7 - (double)$$10;
      double $$16;
      if (pYScale != 0.0) {
         double $$14;
         if (pYMax >= 0.0 && pYMax < $$12) {
            $$14 = pYMax;
         } else {
            $$14 = $$12;
         }

         $$16 = (double)Mth.floor($$14 / pYScale + 1.0E-7F) * pYScale;
      } else {
         $$16 = 0.0;
      }

      return this.sampleAndLerp($$8, $$9, $$10, $$11, $$12 - $$16, $$13, $$12);
   }

   public double noiseWithDerivative(double pX, double pY, double pZ, double[] pValues) {
      double $$4 = pX + this.xo;
      double $$5 = pY + this.yo;
      double $$6 = pZ + this.zo;
      int $$7 = Mth.floor($$4);
      int $$8 = Mth.floor($$5);
      int $$9 = Mth.floor($$6);
      double $$10 = $$4 - (double)$$7;
      double $$11 = $$5 - (double)$$8;
      double $$12 = $$6 - (double)$$9;
      return this.sampleWithDerivative($$7, $$8, $$9, $$10, $$11, $$12, pValues);
   }

   private static double gradDot(int pGradIndex, double pXFactor, double pYFactor, double pZFactor) {
      return SimplexNoise.dot(SimplexNoise.GRADIENT[pGradIndex & 15], pXFactor, pYFactor, pZFactor);
   }

   private int p(int pIndex) {
      return this.p[pIndex & 0xFF] & 0xFF;
   }

   private double sampleAndLerp(int pGridX, int pGridY, int pGridZ, double pDeltaX, double pWeirdDeltaY, double pDeltaZ, double pDeltaY) {
      int $$7 = this.p(pGridX);
      int $$8 = this.p(pGridX + 1);
      int $$9 = this.p($$7 + pGridY);
      int $$10 = this.p($$7 + pGridY + 1);
      int $$11 = this.p($$8 + pGridY);
      int $$12 = this.p($$8 + pGridY + 1);
      double $$13 = gradDot(this.p($$9 + pGridZ), pDeltaX, pWeirdDeltaY, pDeltaZ);
      double $$14 = gradDot(this.p($$11 + pGridZ), pDeltaX - 1.0, pWeirdDeltaY, pDeltaZ);
      double $$15 = gradDot(this.p($$10 + pGridZ), pDeltaX, pWeirdDeltaY - 1.0, pDeltaZ);
      double $$16 = gradDot(this.p($$12 + pGridZ), pDeltaX - 1.0, pWeirdDeltaY - 1.0, pDeltaZ);
      double $$17 = gradDot(this.p($$9 + pGridZ + 1), pDeltaX, pWeirdDeltaY, pDeltaZ - 1.0);
      double $$18 = gradDot(this.p($$11 + pGridZ + 1), pDeltaX - 1.0, pWeirdDeltaY, pDeltaZ - 1.0);
      double $$19 = gradDot(this.p($$10 + pGridZ + 1), pDeltaX, pWeirdDeltaY - 1.0, pDeltaZ - 1.0);
      double $$20 = gradDot(this.p($$12 + pGridZ + 1), pDeltaX - 1.0, pWeirdDeltaY - 1.0, pDeltaZ - 1.0);
      double $$21 = Mth.smoothstep(pDeltaX);
      double $$22 = Mth.smoothstep(pDeltaY);
      double $$23 = Mth.smoothstep(pDeltaZ);
      return Mth.lerp3($$21, $$22, $$23, $$13, $$14, $$15, $$16, $$17, $$18, $$19, $$20);
   }

   private double sampleWithDerivative(int pGridX, int pGridY, int pGridZ, double pDeltaX, double pDeltaY, double pDeltaZ, double[] pNoiseValues) {
      int $$7 = this.p(pGridX);
      int $$8 = this.p(pGridX + 1);
      int $$9 = this.p($$7 + pGridY);
      int $$10 = this.p($$7 + pGridY + 1);
      int $$11 = this.p($$8 + pGridY);
      int $$12 = this.p($$8 + pGridY + 1);
      int $$13 = this.p($$9 + pGridZ);
      int $$14 = this.p($$11 + pGridZ);
      int $$15 = this.p($$10 + pGridZ);
      int $$16 = this.p($$12 + pGridZ);
      int $$17 = this.p($$9 + pGridZ + 1);
      int $$18 = this.p($$11 + pGridZ + 1);
      int $$19 = this.p($$10 + pGridZ + 1);
      int $$20 = this.p($$12 + pGridZ + 1);
      int[] $$21 = SimplexNoise.GRADIENT[$$13 & 15];
      int[] $$22 = SimplexNoise.GRADIENT[$$14 & 15];
      int[] $$23 = SimplexNoise.GRADIENT[$$15 & 15];
      int[] $$24 = SimplexNoise.GRADIENT[$$16 & 15];
      int[] $$25 = SimplexNoise.GRADIENT[$$17 & 15];
      int[] $$26 = SimplexNoise.GRADIENT[$$18 & 15];
      int[] $$27 = SimplexNoise.GRADIENT[$$19 & 15];
      int[] $$28 = SimplexNoise.GRADIENT[$$20 & 15];
      double $$29 = SimplexNoise.dot($$21, pDeltaX, pDeltaY, pDeltaZ);
      double $$30 = SimplexNoise.dot($$22, pDeltaX - 1.0, pDeltaY, pDeltaZ);
      double $$31 = SimplexNoise.dot($$23, pDeltaX, pDeltaY - 1.0, pDeltaZ);
      double $$32 = SimplexNoise.dot($$24, pDeltaX - 1.0, pDeltaY - 1.0, pDeltaZ);
      double $$33 = SimplexNoise.dot($$25, pDeltaX, pDeltaY, pDeltaZ - 1.0);
      double $$34 = SimplexNoise.dot($$26, pDeltaX - 1.0, pDeltaY, pDeltaZ - 1.0);
      double $$35 = SimplexNoise.dot($$27, pDeltaX, pDeltaY - 1.0, pDeltaZ - 1.0);
      double $$36 = SimplexNoise.dot($$28, pDeltaX - 1.0, pDeltaY - 1.0, pDeltaZ - 1.0);
      double $$37 = Mth.smoothstep(pDeltaX);
      double $$38 = Mth.smoothstep(pDeltaY);
      double $$39 = Mth.smoothstep(pDeltaZ);
      double $$40 = Mth.lerp3(
         $$37,
         $$38,
         $$39,
         (double)$$21[0],
         (double)$$22[0],
         (double)$$23[0],
         (double)$$24[0],
         (double)$$25[0],
         (double)$$26[0],
         (double)$$27[0],
         (double)$$28[0]
      );
      double $$41 = Mth.lerp3(
         $$37,
         $$38,
         $$39,
         (double)$$21[1],
         (double)$$22[1],
         (double)$$23[1],
         (double)$$24[1],
         (double)$$25[1],
         (double)$$26[1],
         (double)$$27[1],
         (double)$$28[1]
      );
      double $$42 = Mth.lerp3(
         $$37,
         $$38,
         $$39,
         (double)$$21[2],
         (double)$$22[2],
         (double)$$23[2],
         (double)$$24[2],
         (double)$$25[2],
         (double)$$26[2],
         (double)$$27[2],
         (double)$$28[2]
      );
      double $$43 = Mth.lerp2($$38, $$39, $$30 - $$29, $$32 - $$31, $$34 - $$33, $$36 - $$35);
      double $$44 = Mth.lerp2($$39, $$37, $$31 - $$29, $$35 - $$33, $$32 - $$30, $$36 - $$34);
      double $$45 = Mth.lerp2($$37, $$38, $$33 - $$29, $$34 - $$30, $$35 - $$31, $$36 - $$32);
      double $$46 = Mth.smoothstepDerivative(pDeltaX);
      double $$47 = Mth.smoothstepDerivative(pDeltaY);
      double $$48 = Mth.smoothstepDerivative(pDeltaZ);
      double $$49 = $$40 + $$46 * $$43;
      double $$50 = $$41 + $$47 * $$44;
      double $$51 = $$42 + $$48 * $$45;
      pNoiseValues[0] += $$49;
      pNoiseValues[1] += $$50;
      pNoiseValues[2] += $$51;
      return Mth.lerp3($$37, $$38, $$39, $$29, $$30, $$31, $$32, $$33, $$34, $$35, $$36);
   }

   @VisibleForTesting
   public void parityConfigString(StringBuilder pBuilder) {
      NoiseUtils.parityNoiseOctaveConfigString(pBuilder, this.xo, this.yo, this.zo, this.p);
   }
}
