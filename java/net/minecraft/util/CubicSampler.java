package net.minecraft.util;

import net.minecraft.world.phys.Vec3;

public class CubicSampler {
   private static final int GAUSSIAN_SAMPLE_RADIUS = 2;
   private static final int GAUSSIAN_SAMPLE_BREADTH = 6;
   private static final double[] GAUSSIAN_SAMPLE_KERNEL = new double[]{0.0, 1.0, 4.0, 6.0, 4.0, 1.0, 0.0};

   private CubicSampler() {
   }

   public static Vec3 gaussianSampleVec3(Vec3 pVec, CubicSampler.Vec3Fetcher pFetcher) {
      int $$2 = Mth.floor(pVec.x());
      int $$3 = Mth.floor(pVec.y());
      int $$4 = Mth.floor(pVec.z());
      double $$5 = pVec.x() - (double)$$2;
      double $$6 = pVec.y() - (double)$$3;
      double $$7 = pVec.z() - (double)$$4;
      double $$8 = 0.0;
      Vec3 $$9 = Vec3.ZERO;

      for (int $$10 = 0; $$10 < 6; $$10++) {
         double $$11 = Mth.lerp($$5, GAUSSIAN_SAMPLE_KERNEL[$$10 + 1], GAUSSIAN_SAMPLE_KERNEL[$$10]);
         int $$12 = $$2 - 2 + $$10;

         for (int $$13 = 0; $$13 < 6; $$13++) {
            double $$14 = Mth.lerp($$6, GAUSSIAN_SAMPLE_KERNEL[$$13 + 1], GAUSSIAN_SAMPLE_KERNEL[$$13]);
            int $$15 = $$3 - 2 + $$13;

            for (int $$16 = 0; $$16 < 6; $$16++) {
               double $$17 = Mth.lerp($$7, GAUSSIAN_SAMPLE_KERNEL[$$16 + 1], GAUSSIAN_SAMPLE_KERNEL[$$16]);
               int $$18 = $$4 - 2 + $$16;
               double $$19 = $$11 * $$14 * $$17;
               $$8 += $$19;
               $$9 = $$9.add(pFetcher.fetch($$12, $$15, $$18).scale($$19));
            }
         }
      }

      return $$9.scale(1.0 / $$8);
   }

   @FunctionalInterface
   public interface Vec3Fetcher {
      Vec3 fetch(int pX, int pY, int pZ);
   }
}
