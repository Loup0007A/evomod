package net.minecraft.data.worldgen;

import net.minecraft.util.CubicSpline;
import net.minecraft.util.Mth;
import net.minecraft.util.ToFloatFunction;
import net.minecraft.world.level.levelgen.NoiseRouterData;

public class TerrainProvider {
   private static final float DEEP_OCEAN_CONTINENTALNESS = -0.51F;
   private static final float OCEAN_CONTINENTALNESS = -0.4F;
   private static final float PLAINS_CONTINENTALNESS = 0.1F;
   private static final float BEACH_CONTINENTALNESS = -0.15F;
   private static final ToFloatFunction<Float> NO_TRANSFORM = ToFloatFunction.IDENTITY;
   private static final ToFloatFunction<Float> AMPLIFIED_OFFSET = ToFloatFunction.createUnlimited(p_236651_ -> p_236651_ < 0.0F ? p_236651_ : p_236651_ * 2.0F);
   private static final ToFloatFunction<Float> AMPLIFIED_FACTOR = ToFloatFunction.createUnlimited(p_236649_ -> 1.25F - 6.25F / (p_236649_ + 5.0F));
   private static final ToFloatFunction<Float> AMPLIFIED_JAGGEDNESS = ToFloatFunction.createUnlimited(p_236641_ -> p_236641_ * 2.0F);

   public static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> overworldOffset(I pContinents, I pErosion, I pRidgesFolded, boolean pAmplified) {
      ToFloatFunction<Float> $$4 = pAmplified ? AMPLIFIED_OFFSET : NO_TRANSFORM;
      CubicSpline<C, I> $$5 = buildErosionOffsetSpline(pErosion, pRidgesFolded, -0.15F, 0.0F, 0.0F, 0.1F, 0.0F, -0.03F, false, false, $$4);
      CubicSpline<C, I> $$6 = buildErosionOffsetSpline(pErosion, pRidgesFolded, -0.1F, 0.03F, 0.1F, 0.1F, 0.01F, -0.03F, false, false, $$4);
      CubicSpline<C, I> $$7 = buildErosionOffsetSpline(pErosion, pRidgesFolded, -0.1F, 0.03F, 0.1F, 0.7F, 0.01F, -0.03F, true, true, $$4);
      CubicSpline<C, I> $$8 = buildErosionOffsetSpline(pErosion, pRidgesFolded, -0.05F, 0.03F, 0.1F, 1.0F, 0.01F, 0.01F, true, true, $$4);
      return CubicSpline.<C, I>builder(pContinents, $$4)
         .addPoint(-1.1F, 0.044F)
         .addPoint(-1.02F, -0.2222F)
         .addPoint(-0.51F, -0.2222F)
         .addPoint(-0.44F, -0.12F)
         .addPoint(-0.18F, -0.12F)
         .addPoint(-0.16F, $$5)
         .addPoint(-0.15F, $$5)
         .addPoint(-0.1F, $$6)
         .addPoint(0.25F, $$7)
         .addPoint(1.0F, $$8)
         .build();
   }

   public static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> overworldFactor(I pContinents, I pErosion, I pRidges, I pRidgesFolded, boolean pAmplified) {
      ToFloatFunction<Float> $$5 = pAmplified ? AMPLIFIED_FACTOR : NO_TRANSFORM;
      return CubicSpline.<C, I>builder(pContinents, NO_TRANSFORM)
         .addPoint(-0.19F, 3.95F)
         .addPoint(-0.15F, getErosionFactor(pErosion, pRidges, pRidgesFolded, 6.25F, true, NO_TRANSFORM))
         .addPoint(-0.1F, getErosionFactor(pErosion, pRidges, pRidgesFolded, 5.47F, true, $$5))
         .addPoint(0.03F, getErosionFactor(pErosion, pRidges, pRidgesFolded, 5.08F, true, $$5))
         .addPoint(0.06F, getErosionFactor(pErosion, pRidges, pRidgesFolded, 4.69F, false, $$5))
         .build();
   }

   public static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> overworldJaggedness(
      I pContinents, I pErosion, I pRidges, I pRidgesFolded, boolean pAmplified
   ) {
      ToFloatFunction<Float> $$5 = pAmplified ? AMPLIFIED_JAGGEDNESS : NO_TRANSFORM;
      float $$6 = 0.65F;
      return CubicSpline.<C, I>builder(pContinents, $$5)
         .addPoint(-0.11F, 0.0F)
         .addPoint(0.03F, buildErosionJaggednessSpline(pErosion, pRidges, pRidgesFolded, 1.0F, 0.5F, 0.0F, 0.0F, $$5))
         .addPoint(0.65F, buildErosionJaggednessSpline(pErosion, pRidges, pRidgesFolded, 1.0F, 1.0F, 1.0F, 0.0F, $$5))
         .build();
   }

   private static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> buildErosionJaggednessSpline(
      I pErosion, I pRidges, I pRidgesFolded, float p_236617_, float p_236618_, float p_236619_, float p_236620_, ToFloatFunction<Float> pTransform
   ) {
      float $$8 = -0.5775F;
      CubicSpline<C, I> $$9 = buildRidgeJaggednessSpline(pRidges, pRidgesFolded, p_236617_, p_236619_, pTransform);
      CubicSpline<C, I> $$10 = buildRidgeJaggednessSpline(pRidges, pRidgesFolded, p_236618_, p_236620_, pTransform);
      return CubicSpline.<C, I>builder(pErosion, pTransform)
         .addPoint(-1.0F, $$9)
         .addPoint(-0.78F, $$10)
         .addPoint(-0.5775F, $$10)
         .addPoint(-0.375F, 0.0F)
         .build();
   }

   private static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> buildRidgeJaggednessSpline(
      I pRidges, I pRidgesFolded, float p_236610_, float p_236611_, ToFloatFunction<Float> pTransform
   ) {
      float $$5 = NoiseRouterData.peaksAndValleys(0.4F);
      float $$6 = NoiseRouterData.peaksAndValleys(0.56666666F);
      float $$7 = ($$5 + $$6) / 2.0F;
      CubicSpline.Builder<C, I> $$8 = CubicSpline.builder(pRidgesFolded, pTransform);
      $$8.addPoint($$5, 0.0F);
      if (p_236611_ > 0.0F) {
         $$8.addPoint($$7, buildWeirdnessJaggednessSpline(pRidges, p_236611_, pTransform));
      } else {
         $$8.addPoint($$7, 0.0F);
      }

      if (p_236610_ > 0.0F) {
         $$8.addPoint(1.0F, buildWeirdnessJaggednessSpline(pRidges, p_236610_, pTransform));
      } else {
         $$8.addPoint(1.0F, 0.0F);
      }

      return $$8.build();
   }

   private static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> buildWeirdnessJaggednessSpline(
      I pRidges, float p_236588_, ToFloatFunction<Float> pTransform
   ) {
      float $$3 = 0.63F * p_236588_;
      float $$4 = 0.3F * p_236588_;
      return CubicSpline.<C, I>builder(pRidges, pTransform).addPoint(-0.01F, $$3).addPoint(0.01F, $$4).build();
   }

   private static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> getErosionFactor(
      I pErosion, I pRidges, I pRidgesFolded, float p_236626_, boolean p_236627_, ToFloatFunction<Float> pTransform
   ) {
      CubicSpline<C, I> $$6 = CubicSpline.<C, I>builder(pRidges, pTransform).addPoint(-0.2F, 6.3F).addPoint(0.2F, p_236626_).build();
      CubicSpline.Builder<C, I> $$7 = CubicSpline.<C, I>builder(pErosion, pTransform)
         .addPoint(-0.6F, $$6)
         .addPoint(-0.5F, CubicSpline.<C, I>builder(pRidges, pTransform).addPoint(-0.05F, 6.3F).addPoint(0.05F, 2.67F).build())
         .addPoint(-0.35F, $$6)
         .addPoint(-0.25F, $$6)
         .addPoint(-0.1F, CubicSpline.<C, I>builder(pRidges, pTransform).addPoint(-0.05F, 2.67F).addPoint(0.05F, 6.3F).build())
         .addPoint(0.03F, $$6);
      if (p_236627_) {
         CubicSpline<C, I> $$8 = CubicSpline.<C, I>builder(pRidges, pTransform).addPoint(0.0F, p_236626_).addPoint(0.1F, 0.625F).build();
         CubicSpline<C, I> $$9 = CubicSpline.<C, I>builder(pRidgesFolded, pTransform).addPoint(-0.9F, p_236626_).addPoint(-0.69F, $$8).build();
         $$7.addPoint(0.35F, p_236626_).addPoint(0.45F, $$9).addPoint(0.55F, $$9).addPoint(0.62F, p_236626_);
      } else {
         CubicSpline<C, I> $$10 = CubicSpline.<C, I>builder(pRidgesFolded, pTransform).addPoint(-0.7F, $$6).addPoint(-0.15F, 1.37F).build();
         CubicSpline<C, I> $$11 = CubicSpline.<C, I>builder(pRidgesFolded, pTransform).addPoint(0.45F, $$6).addPoint(0.7F, 1.56F).build();
         $$7.addPoint(0.05F, $$11).addPoint(0.4F, $$11).addPoint(0.45F, $$10).addPoint(0.55F, $$10).addPoint(0.58F, p_236626_);
      }

      return $$7.build();
   }

   private static float calculateSlope(float pY1, float pY2, float pX1, float pX2) {
      return (pY2 - pY1) / (pX2 - pX1);
   }

   private static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> buildMountainRidgeSplineWithPoints(
      I pRidgesFolded, float p_236592_, boolean p_236593_, ToFloatFunction<Float> pTransform
   ) {
      CubicSpline.Builder<C, I> $$4 = CubicSpline.builder(pRidgesFolded, pTransform);
      float $$5 = -0.7F;
      float $$6 = -1.0F;
      float $$7 = mountainContinentalness(-1.0F, p_236592_, -0.7F);
      float $$8 = 1.0F;
      float $$9 = mountainContinentalness(1.0F, p_236592_, -0.7F);
      float $$10 = calculateMountainRidgeZeroContinentalnessPoint(p_236592_);
      float $$11 = -0.65F;
      if (-0.65F < $$10 && $$10 < 1.0F) {
         float $$12 = mountainContinentalness(-0.65F, p_236592_, -0.7F);
         float $$13 = -0.75F;
         float $$14 = mountainContinentalness(-0.75F, p_236592_, -0.7F);
         float $$15 = calculateSlope($$7, $$14, -1.0F, -0.75F);
         $$4.addPoint(-1.0F, $$7, $$15);
         $$4.addPoint(-0.75F, $$14);
         $$4.addPoint(-0.65F, $$12);
         float $$16 = mountainContinentalness($$10, p_236592_, -0.7F);
         float $$17 = calculateSlope($$16, $$9, $$10, 1.0F);
         float $$18 = 0.01F;
         $$4.addPoint($$10 - 0.01F, $$16);
         $$4.addPoint($$10, $$16, $$17);
         $$4.addPoint(1.0F, $$9, $$17);
      } else {
         float $$19 = calculateSlope($$7, $$9, -1.0F, 1.0F);
         if (p_236593_) {
            $$4.addPoint(-1.0F, Math.max(0.2F, $$7));
            $$4.addPoint(0.0F, Mth.lerp(0.5F, $$7, $$9), $$19);
         } else {
            $$4.addPoint(-1.0F, $$7, $$19);
         }

         $$4.addPoint(1.0F, $$9, $$19);
      }

      return $$4.build();
   }

   private static float mountainContinentalness(float p_236569_, float p_236570_, float p_236571_) {
      float $$3 = 1.17F;
      float $$4 = 0.46082947F;
      float $$5 = 1.0F - (1.0F - p_236570_) * 0.5F;
      float $$6 = 0.5F * (1.0F - p_236570_);
      float $$7 = (p_236569_ + 1.17F) * 0.46082947F;
      float $$8 = $$7 * $$5 - $$6;
      return p_236569_ < p_236571_ ? Math.max($$8, -0.2222F) : Math.max($$8, 0.0F);
   }

   private static float calculateMountainRidgeZeroContinentalnessPoint(float p_236567_) {
      float $$1 = 1.17F;
      float $$2 = 0.46082947F;
      float $$3 = 1.0F - (1.0F - p_236567_) * 0.5F;
      float $$4 = 0.5F * (1.0F - p_236567_);
      return $$4 / (0.46082947F * $$3) - 1.17F;
   }

   public static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> buildErosionOffsetSpline(
      I pErosion,
      I pRidgesFolded,
      float p_236598_,
      float p_236599_,
      float p_236600_,
      float p_236601_,
      float p_236602_,
      float p_236603_,
      boolean p_236604_,
      boolean p_236605_,
      ToFloatFunction<Float> pTransform
   ) {
      float $$11 = 0.6F;
      float $$12 = 0.5F;
      float $$13 = 0.5F;
      CubicSpline<C, I> $$14 = buildMountainRidgeSplineWithPoints(pRidgesFolded, Mth.lerp(p_236601_, 0.6F, 1.5F), p_236605_, pTransform);
      CubicSpline<C, I> $$15 = buildMountainRidgeSplineWithPoints(pRidgesFolded, Mth.lerp(p_236601_, 0.6F, 1.0F), p_236605_, pTransform);
      CubicSpline<C, I> $$16 = buildMountainRidgeSplineWithPoints(pRidgesFolded, p_236601_, p_236605_, pTransform);
      CubicSpline<C, I> $$17 = ridgeSpline(
         pRidgesFolded, p_236598_ - 0.15F, 0.5F * p_236601_, Mth.lerp(0.5F, 0.5F, 0.5F) * p_236601_, 0.5F * p_236601_, 0.6F * p_236601_, 0.5F, pTransform
      );
      CubicSpline<C, I> $$18 = ridgeSpline(
         pRidgesFolded, p_236598_, p_236602_ * p_236601_, p_236599_ * p_236601_, 0.5F * p_236601_, 0.6F * p_236601_, 0.5F, pTransform
      );
      CubicSpline<C, I> $$19 = ridgeSpline(pRidgesFolded, p_236598_, p_236602_, p_236602_, p_236599_, p_236600_, 0.5F, pTransform);
      CubicSpline<C, I> $$20 = ridgeSpline(pRidgesFolded, p_236598_, p_236602_, p_236602_, p_236599_, p_236600_, 0.5F, pTransform);
      CubicSpline<C, I> $$21 = CubicSpline.<C, I>builder(pRidgesFolded, pTransform)
         .addPoint(-1.0F, p_236598_)
         .addPoint(-0.4F, $$19)
         .addPoint(0.0F, p_236600_ + 0.07F)
         .build();
      CubicSpline<C, I> $$22 = ridgeSpline(pRidgesFolded, -0.02F, p_236603_, p_236603_, p_236599_, p_236600_, 0.0F, pTransform);
      CubicSpline.Builder<C, I> $$23 = CubicSpline.<C, I>builder(pErosion, pTransform)
         .addPoint(-0.85F, $$14)
         .addPoint(-0.7F, $$15)
         .addPoint(-0.4F, $$16)
         .addPoint(-0.35F, $$17)
         .addPoint(-0.1F, $$18)
         .addPoint(0.2F, $$19);
      if (p_236604_) {
         $$23.addPoint(0.4F, $$20).addPoint(0.45F, $$21).addPoint(0.55F, $$21).addPoint(0.58F, $$20);
      }

      $$23.addPoint(0.7F, $$22);
      return $$23.build();
   }

   private static <C, I extends ToFloatFunction<C>> CubicSpline<C, I> ridgeSpline(
      I pRidgesFolded, float pY1, float pY2, float pY3, float pY4, float pY5, float pMinSmoothing, ToFloatFunction<Float> pTransform
   ) {
      float $$8 = Math.max(0.5F * (pY2 - pY1), pMinSmoothing);
      float $$9 = 5.0F * (pY3 - pY2);
      return CubicSpline.<C, I>builder(pRidgesFolded, pTransform)
         .addPoint(-1.0F, pY1, $$8)
         .addPoint(-0.4F, pY2, Math.min($$8, $$9))
         .addPoint(0.0F, pY3, $$9)
         .addPoint(0.4F, pY4, 2.0F * (pY4 - pY3))
         .addPoint(1.0F, pY5, 0.7F * (pY5 - pY4))
         .build();
   }
}
