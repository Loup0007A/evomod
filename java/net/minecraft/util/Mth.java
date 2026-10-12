package net.minecraft.util;

import java.util.Locale;
import java.util.UUID;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import net.minecraft.Util;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.math.Fraction;
import org.apache.commons.lang3.math.NumberUtils;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Mth {
   private static final long UUID_VERSION = 61440L;
   private static final long UUID_VERSION_TYPE_4 = 16384L;
   private static final long UUID_VARIANT = -4611686018427387904L;
   private static final long UUID_VARIANT_2 = Long.MIN_VALUE;
   public static final float PI = (float) Math.PI;
   public static final float HALF_PI = (float) (Math.PI / 2);
   public static final float TWO_PI = (float) (Math.PI * 2);
   public static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
   public static final float RAD_TO_DEG = 180.0F / (float)Math.PI;
   public static final float EPSILON = 1.0E-5F;
   public static final float SQRT_OF_TWO = sqrt(2.0F);
   private static final float SIN_SCALE = 10430.378F;
   public static final Vector3f Y_AXIS = new Vector3f(0.0F, 1.0F, 0.0F);
   public static final Vector3f X_AXIS = new Vector3f(1.0F, 0.0F, 0.0F);
   public static final Vector3f Z_AXIS = new Vector3f(0.0F, 0.0F, 1.0F);
   private static final float[] SIN = Util.make(new float[65536], p_14077_ -> {
      for (int $$1x = 0; $$1x < p_14077_.length; $$1x++) {
         p_14077_[$$1x] = (float)Math.sin((double)$$1x * Math.PI * 2.0 / 65536.0);
      }
   });
   private static final RandomSource RANDOM = RandomSource.createThreadSafe();
   private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{
      0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9
   };
   private static final double ONE_SIXTH = 0.16666666666666666;
   private static final int FRAC_EXP = 8;
   private static final int LUT_SIZE = 257;
   private static final double FRAC_BIAS = Double.longBitsToDouble(4805340802404319232L);
   private static final double[] ASIN_TAB = new double[257];
   private static final double[] COS_TAB = new double[257];

   public static float sin(float pValue) {
      return SIN[(int)(pValue * 10430.378F) & 65535];
   }

   public static float cos(float pValue) {
      return SIN[(int)(pValue * 10430.378F + 16384.0F) & 65535];
   }

   public static float sqrt(float pValue) {
      return (float)Math.sqrt((double)pValue);
   }

   public static int floor(float pValue) {
      int $$1 = (int)pValue;
      return pValue < (float)$$1 ? $$1 - 1 : $$1;
   }

   public static int floor(double pValue) {
      int $$1 = (int)pValue;
      return pValue < (double)$$1 ? $$1 - 1 : $$1;
   }

   public static long lfloor(double pValue) {
      long $$1 = (long)pValue;
      return pValue < (double)$$1 ? $$1 - 1L : $$1;
   }

   public static float abs(float pValue) {
      return Math.abs(pValue);
   }

   public static int abs(int pValue) {
      return Math.abs(pValue);
   }

   public static int ceil(float pValue) {
      int $$1 = (int)pValue;
      return pValue > (float)$$1 ? $$1 + 1 : $$1;
   }

   public static int ceil(double pValue) {
      int $$1 = (int)pValue;
      return pValue > (double)$$1 ? $$1 + 1 : $$1;
   }

   public static int clamp(int pValue, int pMin, int pMax) {
      return Math.min(Math.max(pValue, pMin), pMax);
   }

   public static long clamp(long pValue, long pMin, long pMax) {
      return Math.min(Math.max(pValue, pMin), pMax);
   }

   public static float clamp(float pValue, float pMin, float pMax) {
      return pValue < pMin ? pMin : Math.min(pValue, pMax);
   }

   public static double clamp(double pValue, double pMin, double pMax) {
      return pValue < pMin ? pMin : Math.min(pValue, pMax);
   }

   public static double clampedLerp(double pStart, double pEnd, double pDelta) {
      if (pDelta < 0.0) {
         return pStart;
      } else {
         return pDelta > 1.0 ? pEnd : lerp(pDelta, pStart, pEnd);
      }
   }

   public static float clampedLerp(float pStart, float pEnd, float pDelta) {
      if (pDelta < 0.0F) {
         return pStart;
      } else {
         return pDelta > 1.0F ? pEnd : lerp(pDelta, pStart, pEnd);
      }
   }

   public static double absMax(double pX, double pY) {
      if (pX < 0.0) {
         pX = -pX;
      }

      if (pY < 0.0) {
         pY = -pY;
      }

      return Math.max(pX, pY);
   }

   public static int floorDiv(int pDividend, int pDivisor) {
      return Math.floorDiv(pDividend, pDivisor);
   }

   public static int nextInt(RandomSource pRandom, int pMinimum, int pMaximum) {
      return pMinimum >= pMaximum ? pMinimum : pRandom.nextInt(pMaximum - pMinimum + 1) + pMinimum;
   }

   public static float nextFloat(RandomSource pRandom, float pMinimum, float pMaximum) {
      return pMinimum >= pMaximum ? pMinimum : pRandom.nextFloat() * (pMaximum - pMinimum) + pMinimum;
   }

   public static double nextDouble(RandomSource pRandom, double pMinimum, double pMaximum) {
      return pMinimum >= pMaximum ? pMinimum : pRandom.nextDouble() * (pMaximum - pMinimum) + pMinimum;
   }

   public static boolean equal(float pX, float pY) {
      return Math.abs(pY - pX) < 1.0E-5F;
   }

   public static boolean equal(double pX, double pY) {
      return Math.abs(pY - pX) < 1.0E-5F;
   }

   public static int positiveModulo(int pX, int pY) {
      return Math.floorMod(pX, pY);
   }

   public static float positiveModulo(float pNumerator, float pDenominator) {
      return (pNumerator % pDenominator + pDenominator) % pDenominator;
   }

   public static double positiveModulo(double pNumerator, double pDenominator) {
      return (pNumerator % pDenominator + pDenominator) % pDenominator;
   }

   public static boolean isMultipleOf(int pNumber, int pMultiple) {
      return pNumber % pMultiple == 0;
   }

   public static int wrapDegrees(int pAngle) {
      int $$1 = pAngle % 360;
      if ($$1 >= 180) {
         $$1 -= 360;
      }

      if ($$1 < -180) {
         $$1 += 360;
      }

      return $$1;
   }

   public static float wrapDegrees(float pValue) {
      float $$1 = pValue % 360.0F;
      if ($$1 >= 180.0F) {
         $$1 -= 360.0F;
      }

      if ($$1 < -180.0F) {
         $$1 += 360.0F;
      }

      return $$1;
   }

   public static double wrapDegrees(double pValue) {
      double $$1 = pValue % 360.0;
      if ($$1 >= 180.0) {
         $$1 -= 360.0;
      }

      if ($$1 < -180.0) {
         $$1 += 360.0;
      }

      return $$1;
   }

   public static float degreesDifference(float pStart, float pEnd) {
      return wrapDegrees(pEnd - pStart);
   }

   public static float degreesDifferenceAbs(float pStart, float pEnd) {
      return abs(degreesDifference(pStart, pEnd));
   }

   public static float rotateIfNecessary(float pRotationToAdjust, float pActualRotation, float pMaxDifference) {
      float $$3 = degreesDifference(pRotationToAdjust, pActualRotation);
      float $$4 = clamp($$3, -pMaxDifference, pMaxDifference);
      return pActualRotation - $$4;
   }

   public static float approach(float pValue, float pLimit, float pStepSize) {
      pStepSize = abs(pStepSize);
      return pValue < pLimit ? clamp(pValue + pStepSize, pValue, pLimit) : clamp(pValue - pStepSize, pLimit, pValue);
   }

   public static float approachDegrees(float pAngle, float pLimit, float pStepSize) {
      float $$3 = degreesDifference(pAngle, pLimit);
      return approach(pAngle, pAngle + $$3, pStepSize);
   }

   public static int getInt(String pValue, int pDefaultValue) {
      return NumberUtils.toInt(pValue, pDefaultValue);
   }

   public static int smallestEncompassingPowerOfTwo(int pValue) {
      int $$1 = pValue - 1;
      $$1 |= $$1 >> 1;
      $$1 |= $$1 >> 2;
      $$1 |= $$1 >> 4;
      $$1 |= $$1 >> 8;
      $$1 |= $$1 >> 16;
      return $$1 + 1;
   }

   public static boolean isPowerOfTwo(int pValue) {
      return pValue != 0 && (pValue & pValue - 1) == 0;
   }

   public static int ceillog2(int pValue) {
      pValue = isPowerOfTwo(pValue) ? pValue : smallestEncompassingPowerOfTwo(pValue);
      return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int)((long)pValue * 125613361L >> 27) & 31];
   }

   public static int log2(int pValue) {
      return ceillog2(pValue) - (isPowerOfTwo(pValue) ? 0 : 1);
   }

   public static int color(float pR, float pG, float pB) {
      return FastColor.ARGB32.color(0, floor(pR * 255.0F), floor(pG * 255.0F), floor(pB * 255.0F));
   }

   public static float frac(float pNumber) {
      return pNumber - (float)floor(pNumber);
   }

   public static double frac(double pNumber) {
      return pNumber - (double)lfloor(pNumber);
   }

   @Deprecated
   public static long getSeed(Vec3i pPos) {
      return getSeed(pPos.getX(), pPos.getY(), pPos.getZ());
   }

   @Deprecated
   public static long getSeed(int pX, int pY, int pZ) {
      long $$3 = (long)(pX * 3129871) ^ (long)pZ * 116129781L ^ (long)pY;
      $$3 = $$3 * $$3 * 42317861L + $$3 * 11L;
      return $$3 >> 16;
   }

   public static UUID createInsecureUUID(RandomSource pRandom) {
      long $$1 = pRandom.nextLong() & -61441L | 16384L;
      long $$2 = pRandom.nextLong() & 4611686018427387903L | Long.MIN_VALUE;
      return new UUID($$1, $$2);
   }

   public static UUID createInsecureUUID() {
      return createInsecureUUID(RANDOM);
   }

   public static double inverseLerp(double pDelta, double pStart, double pEnd) {
      return (pDelta - pStart) / (pEnd - pStart);
   }

   public static float inverseLerp(float pDelta, float pStart, float pEnd) {
      return (pDelta - pStart) / (pEnd - pStart);
   }

   public static boolean rayIntersectsAABB(Vec3 pStart, Vec3 pEnd, AABB pBoundingBox) {
      double $$3 = (pBoundingBox.minX + pBoundingBox.maxX) * 0.5;
      double $$4 = (pBoundingBox.maxX - pBoundingBox.minX) * 0.5;
      double $$5 = pStart.x - $$3;
      if (Math.abs($$5) > $$4 && $$5 * pEnd.x >= 0.0) {
         return false;
      } else {
         double $$6 = (pBoundingBox.minY + pBoundingBox.maxY) * 0.5;
         double $$7 = (pBoundingBox.maxY - pBoundingBox.minY) * 0.5;
         double $$8 = pStart.y - $$6;
         if (Math.abs($$8) > $$7 && $$8 * pEnd.y >= 0.0) {
            return false;
         } else {
            double $$9 = (pBoundingBox.minZ + pBoundingBox.maxZ) * 0.5;
            double $$10 = (pBoundingBox.maxZ - pBoundingBox.minZ) * 0.5;
            double $$11 = pStart.z - $$9;
            if (Math.abs($$11) > $$10 && $$11 * pEnd.z >= 0.0) {
               return false;
            } else {
               double $$12 = Math.abs(pEnd.x);
               double $$13 = Math.abs(pEnd.y);
               double $$14 = Math.abs(pEnd.z);
               double $$15 = pEnd.y * $$11 - pEnd.z * $$8;
               if (Math.abs($$15) > $$7 * $$14 + $$10 * $$13) {
                  return false;
               } else {
                  $$15 = pEnd.z * $$5 - pEnd.x * $$11;
                  if (Math.abs($$15) > $$4 * $$14 + $$10 * $$12) {
                     return false;
                  } else {
                     $$15 = pEnd.x * $$8 - pEnd.y * $$5;
                     return Math.abs($$15) < $$4 * $$13 + $$7 * $$12;
                  }
               }
            }
         }
      }
   }

   public static double atan2(double pY, double pX) {
      double $$2 = pX * pX + pY * pY;
      if (Double.isNaN($$2)) {
         return Double.NaN;
      } else {
         boolean $$3 = pY < 0.0;
         if ($$3) {
            pY = -pY;
         }

         boolean $$4 = pX < 0.0;
         if ($$4) {
            pX = -pX;
         }

         boolean $$5 = pY > pX;
         if ($$5) {
            double $$6 = pX;
            pX = pY;
            pY = $$6;
         }

         double $$7 = fastInvSqrt($$2);
         pX *= $$7;
         pY *= $$7;
         double $$8 = FRAC_BIAS + pY;
         int $$9 = (int)Double.doubleToRawLongBits($$8);
         double $$10 = ASIN_TAB[$$9];
         double $$11 = COS_TAB[$$9];
         double $$12 = $$8 - FRAC_BIAS;
         double $$13 = pY * $$11 - pX * $$12;
         double $$14 = (6.0 + $$13 * $$13) * $$13 * 0.16666666666666666;
         double $$15 = $$10 + $$14;
         if ($$5) {
            $$15 = (Math.PI / 2) - $$15;
         }

         if ($$4) {
            $$15 = Math.PI - $$15;
         }

         if ($$3) {
            $$15 = -$$15;
         }

         return $$15;
      }
   }

   public static float invSqrt(float pNumber) {
      return org.joml.Math.invsqrt(pNumber);
   }

   public static double invSqrt(double pNumber) {
      return org.joml.Math.invsqrt(pNumber);
   }

   @Deprecated
   public static double fastInvSqrt(double pNumber) {
      double $$1 = 0.5 * pNumber;
      long $$2 = Double.doubleToRawLongBits(pNumber);
      $$2 = 6910469410427058090L - ($$2 >> 1);
      pNumber = Double.longBitsToDouble($$2);
      return pNumber * (1.5 - $$1 * pNumber * pNumber);
   }

   public static float fastInvCubeRoot(float pNumber) {
      int $$1 = Float.floatToIntBits(pNumber);
      $$1 = 1419967116 - $$1 / 3;
      float $$2 = Float.intBitsToFloat($$1);
      $$2 = 0.6666667F * $$2 + 1.0F / (3.0F * $$2 * $$2 * pNumber);
      return 0.6666667F * $$2 + 1.0F / (3.0F * $$2 * $$2 * pNumber);
   }

   public static int hsvToRgb(float pHue, float pSaturation, float pValue) {
      return hsvToArgb(pHue, pSaturation, pValue, 0);
   }

   public static int hsvToArgb(float pHue, float pSaturation, float pValue, int pAlpha) {
      int $$4 = (int)(pHue * 6.0F) % 6;
      float $$5 = pHue * 6.0F - (float)$$4;
      float $$6 = pValue * (1.0F - pSaturation);
      float $$7 = pValue * (1.0F - $$5 * pSaturation);
      float $$8 = pValue * (1.0F - (1.0F - $$5) * pSaturation);
      float $$9;
      float $$10;
      float $$11;
      switch ($$4) {
         case 0:
            $$9 = pValue;
            $$10 = $$8;
            $$11 = $$6;
            break;
         case 1:
            $$9 = $$7;
            $$10 = pValue;
            $$11 = $$6;
            break;
         case 2:
            $$9 = $$6;
            $$10 = pValue;
            $$11 = $$8;
            break;
         case 3:
            $$9 = $$6;
            $$10 = $$7;
            $$11 = pValue;
            break;
         case 4:
            $$9 = $$8;
            $$10 = $$6;
            $$11 = pValue;
            break;
         case 5:
            $$9 = pValue;
            $$10 = $$6;
            $$11 = $$7;
            break;
         default:
            throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + pHue + ", " + pSaturation + ", " + pValue);
      }

      return FastColor.ARGB32.color(pAlpha, clamp((int)($$9 * 255.0F), 0, 255), clamp((int)($$10 * 255.0F), 0, 255), clamp((int)($$11 * 255.0F), 0, 255));
   }

   public static int murmurHash3Mixer(int pInput) {
      pInput ^= pInput >>> 16;
      pInput *= -2048144789;
      pInput ^= pInput >>> 13;
      pInput *= -1028477387;
      return pInput ^ pInput >>> 16;
   }

   public static int binarySearch(int pMin, int pMax, IntPredicate pIsTargetBeforeOrAt) {
      int $$3 = pMax - pMin;

      while ($$3 > 0) {
         int $$4 = $$3 / 2;
         int $$5 = pMin + $$4;
         if (pIsTargetBeforeOrAt.test($$5)) {
            $$3 = $$4;
         } else {
            pMin = $$5 + 1;
            $$3 -= $$4 + 1;
         }
      }

      return pMin;
   }

   public static int lerpInt(float pDelta, int pStart, int pEnd) {
      return pStart + floor(pDelta * (float)(pEnd - pStart));
   }

   public static int lerpDiscrete(float pDelta, int pStart, int pEnd) {
      int $$3 = pEnd - pStart;
      return pStart + floor(pDelta * (float)($$3 - 1)) + (pDelta > 0.0F ? 1 : 0);
   }

   public static float lerp(float pDelta, float pStart, float pEnd) {
      return pStart + pDelta * (pEnd - pStart);
   }

   public static double lerp(double pDelta, double pStart, double pEnd) {
      return pStart + pDelta * (pEnd - pStart);
   }

   public static double lerp2(double pDelta1, double pDelta2, double pStart1, double pEnd1, double pStart2, double pEnd2) {
      return lerp(pDelta2, lerp(pDelta1, pStart1, pEnd1), lerp(pDelta1, pStart2, pEnd2));
   }

   public static double lerp3(
      double pDelta1,
      double pDelta2,
      double pDelta3,
      double pStart1,
      double pEnd1,
      double pStart2,
      double pEnd2,
      double pStart3,
      double pEnd3,
      double pStart4,
      double pEnd4
   ) {
      return lerp(pDelta3, lerp2(pDelta1, pDelta2, pStart1, pEnd1, pStart2, pEnd2), lerp2(pDelta1, pDelta2, pStart3, pEnd3, pStart4, pEnd4));
   }

   public static float catmullrom(float pDelta, float pControlPoint1, float pControlPoint2, float pControlPoint3, float pControlPoint4) {
      return 0.5F
         * (
            2.0F * pControlPoint2
               + (pControlPoint3 - pControlPoint1) * pDelta
               + (2.0F * pControlPoint1 - 5.0F * pControlPoint2 + 4.0F * pControlPoint3 - pControlPoint4) * pDelta * pDelta
               + (3.0F * pControlPoint2 - pControlPoint1 - 3.0F * pControlPoint3 + pControlPoint4) * pDelta * pDelta * pDelta
         );
   }

   public static double smoothstep(double pInput) {
      return pInput * pInput * pInput * (pInput * (pInput * 6.0 - 15.0) + 10.0);
   }

   public static double smoothstepDerivative(double pInput) {
      return 30.0 * pInput * pInput * (pInput - 1.0) * (pInput - 1.0);
   }

   public static int sign(double pX) {
      if (pX == 0.0) {
         return 0;
      } else {
         return pX > 0.0 ? 1 : -1;
      }
   }

   public static float rotLerp(float pDelta, float pStart, float pEnd) {
      return pStart + pDelta * wrapDegrees(pEnd - pStart);
   }

   public static double rotLerp(double pDelta, double pStart, double pEnd) {
      return pStart + pDelta * wrapDegrees(pEnd - pStart);
   }

   public static float triangleWave(float pInput, float pPeriod) {
      return (Math.abs(pInput % pPeriod - pPeriod * 0.5F) - pPeriod * 0.25F) / (pPeriod * 0.25F);
   }

   public static float square(float pValue) {
      return pValue * pValue;
   }

   public static double square(double pValue) {
      return pValue * pValue;
   }

   public static int square(int pValue) {
      return pValue * pValue;
   }

   public static long square(long pValue) {
      return pValue * pValue;
   }

   public static double clampedMap(double pInput, double pInputMin, double pInputMax, double pOuputMin, double pOutputMax) {
      return clampedLerp(pOuputMin, pOutputMax, inverseLerp(pInput, pInputMin, pInputMax));
   }

   public static float clampedMap(float pInput, float pInputMin, float pInputMax, float pOutputMin, float pOutputMax) {
      return clampedLerp(pOutputMin, pOutputMax, inverseLerp(pInput, pInputMin, pInputMax));
   }

   public static double map(double pInput, double pInputMin, double pInputMax, double pOutputMin, double pOutputMax) {
      return lerp(inverseLerp(pInput, pInputMin, pInputMax), pOutputMin, pOutputMax);
   }

   public static float map(float pInput, float pInputMin, float pInputMax, float pOutputMin, float pOutputMax) {
      return lerp(inverseLerp(pInput, pInputMin, pInputMax), pOutputMin, pOutputMax);
   }

   public static double wobble(double pInput) {
      return pInput + (2.0 * RandomSource.create((long)floor(pInput * 3000.0)).nextDouble() - 1.0) * 1.0E-7 / 2.0;
   }

   public static int roundToward(int pValue, int pFactor) {
      return positiveCeilDiv(pValue, pFactor) * pFactor;
   }

   public static int positiveCeilDiv(int pX, int pY) {
      return -Math.floorDiv(-pX, pY);
   }

   public static int randomBetweenInclusive(RandomSource pRandom, int pMinInclusive, int pMaxInclusive) {
      return pRandom.nextInt(pMaxInclusive - pMinInclusive + 1) + pMinInclusive;
   }

   public static float randomBetween(RandomSource pRandom, float pMinInclusive, float pMaxExclusive) {
      return pRandom.nextFloat() * (pMaxExclusive - pMinInclusive) + pMinInclusive;
   }

   public static float normal(RandomSource pRandom, float pMean, float pDeviation) {
      return pMean + (float)pRandom.nextGaussian() * pDeviation;
   }

   public static double lengthSquared(double pXDistance, double pYDistance) {
      return pXDistance * pXDistance + pYDistance * pYDistance;
   }

   public static double length(double pXDistance, double pYDistance) {
      return Math.sqrt(lengthSquared(pXDistance, pYDistance));
   }

   public static double lengthSquared(double pXDistance, double pYDistance, double pZDistance) {
      return pXDistance * pXDistance + pYDistance * pYDistance + pZDistance * pZDistance;
   }

   public static double length(double pXDistance, double pYDistance, double pZDistance) {
      return Math.sqrt(lengthSquared(pXDistance, pYDistance, pZDistance));
   }

   public static float lengthSquared(float pXDistance, float pYDistance, float pZDistance) {
      return pXDistance * pXDistance + pYDistance * pYDistance + pZDistance * pZDistance;
   }

   public static int quantize(double pValue, int pFactor) {
      return floor(pValue / (double)pFactor) * pFactor;
   }

   public static IntStream outFromOrigin(int pInput, int pLowerBound, int pUpperBound) {
      return outFromOrigin(pInput, pLowerBound, pUpperBound, 1);
   }

   public static IntStream outFromOrigin(int pInput, int pLowerBound, int pUpperBound, int pSteps) {
      if (pLowerBound > pUpperBound) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "upperbound %d expected to be > lowerBound %d", pUpperBound, pLowerBound));
      } else if (pSteps < 1) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "steps expected to be >= 1, was %d", pSteps));
      } else {
         return pInput >= pLowerBound && pInput <= pUpperBound ? IntStream.iterate(pInput, p_216282_ -> {
            int $$4 = Math.abs(pInput - p_216282_);
            return pInput - $$4 >= pLowerBound || pInput + $$4 <= pUpperBound;
         }, p_216260_ -> {
            boolean $$5 = p_216260_ <= pInput;
            int $$6 = Math.abs(pInput - p_216260_);
            boolean $$7 = pInput + $$6 + pSteps <= pUpperBound;
            if (!$$5 || !$$7) {
               int $$8 = pInput - $$6 - ($$5 ? pSteps : 0);
               if ($$8 >= pLowerBound) {
                  return $$8;
               }
            }

            return pInput + $$6 + pSteps;
         }) : IntStream.empty();
      }
   }

   public static Quaternionf rotationAroundAxis(Vector3f pAxis, Quaternionf pCameraOrentation, Quaternionf pOutput) {
      float $$3 = pAxis.dot(pCameraOrentation.x, pCameraOrentation.y, pCameraOrentation.z);
      return pOutput.set(pAxis.x * $$3, pAxis.y * $$3, pAxis.z * $$3, pCameraOrentation.w).normalize();
   }

   public static int mulAndTruncate(Fraction pFraction, int pFactor) {
      return pFraction.getNumerator() * pFactor / pFraction.getDenominator();
   }

   static {
      for (int $$0 = 0; $$0 < 257; $$0++) {
         double $$1 = (double)$$0 / 256.0;
         double $$2 = Math.asin($$1);
         COS_TAB[$$0] = Math.cos($$2);
         ASIN_TAB[$$0] = $$2;
      }
   }
}
