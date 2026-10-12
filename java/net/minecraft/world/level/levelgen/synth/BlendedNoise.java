package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.stream.IntStream;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

public class BlendedNoise implements DensityFunction.SimpleFunction {
   private static final Codec<Double> SCALE_RANGE = Codec.doubleRange(0.001, 1000.0);
   private static final MapCodec<BlendedNoise> DATA_CODEC = RecordCodecBuilder.mapCodec(
      p_230486_ -> p_230486_.group(
               SCALE_RANGE.fieldOf("xz_scale").forGetter(p_230497_ -> p_230497_.xzScale),
               SCALE_RANGE.fieldOf("y_scale").forGetter(p_230495_ -> p_230495_.yScale),
               SCALE_RANGE.fieldOf("xz_factor").forGetter(p_230493_ -> p_230493_.xzFactor),
               SCALE_RANGE.fieldOf("y_factor").forGetter(p_230490_ -> p_230490_.yFactor),
               Codec.doubleRange(1.0, 8.0).fieldOf("smear_scale_multiplier").forGetter(p_230488_ -> p_230488_.smearScaleMultiplier)
            )
            .apply(p_230486_, BlendedNoise::createUnseeded)
   );
   public static final KeyDispatchDataCodec<BlendedNoise> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);
   private final PerlinNoise minLimitNoise;
   private final PerlinNoise maxLimitNoise;
   private final PerlinNoise mainNoise;
   private final double xzMultiplier;
   private final double yMultiplier;
   private final double xzFactor;
   private final double yFactor;
   private final double smearScaleMultiplier;
   private final double maxValue;
   private final double xzScale;
   private final double yScale;

   public static BlendedNoise createUnseeded(double p_230478_, double p_230479_, double p_230480_, double p_230481_, double p_230482_) {
      return new BlendedNoise(new XoroshiroRandomSource(0L), p_230478_, p_230479_, p_230480_, p_230481_, p_230482_);
   }

   private BlendedNoise(
      PerlinNoise pMinLimitNoise,
      PerlinNoise pMaxLimitNoise,
      PerlinNoise pMainNoise,
      double pXzScale,
      double pYScale,
      double pXzFactor,
      double pYFactor,
      double pSmearScaleMultiplier
   ) {
      this.minLimitNoise = pMinLimitNoise;
      this.maxLimitNoise = pMaxLimitNoise;
      this.mainNoise = pMainNoise;
      this.xzScale = pXzScale;
      this.yScale = pYScale;
      this.xzFactor = pXzFactor;
      this.yFactor = pYFactor;
      this.smearScaleMultiplier = pSmearScaleMultiplier;
      this.xzMultiplier = 684.412 * this.xzScale;
      this.yMultiplier = 684.412 * this.yScale;
      this.maxValue = pMinLimitNoise.maxBrokenValue(this.yMultiplier);
   }

   @VisibleForTesting
   public BlendedNoise(RandomSource pRandom, double pXzScale, double pYScale, double pXzFactor, double pYFactor, double pSmearScaleMultiplier) {
      this(
         PerlinNoise.createLegacyForBlendedNoise(pRandom, IntStream.rangeClosed(-15, 0)),
         PerlinNoise.createLegacyForBlendedNoise(pRandom, IntStream.rangeClosed(-15, 0)),
         PerlinNoise.createLegacyForBlendedNoise(pRandom, IntStream.rangeClosed(-7, 0)),
         pXzScale,
         pYScale,
         pXzFactor,
         pYFactor,
         pSmearScaleMultiplier
      );
   }

   public BlendedNoise withNewRandom(RandomSource pRandom) {
      return new BlendedNoise(pRandom, this.xzScale, this.yScale, this.xzFactor, this.yFactor, this.smearScaleMultiplier);
   }

   @Override
   public double compute(DensityFunction.FunctionContext pContext) {
      double $$1 = (double)pContext.blockX() * this.xzMultiplier;
      double $$2 = (double)pContext.blockY() * this.yMultiplier;
      double $$3 = (double)pContext.blockZ() * this.xzMultiplier;
      double $$4 = $$1 / this.xzFactor;
      double $$5 = $$2 / this.yFactor;
      double $$6 = $$3 / this.xzFactor;
      double $$7 = this.yMultiplier * this.smearScaleMultiplier;
      double $$8 = $$7 / this.yFactor;
      double $$9 = 0.0;
      double $$10 = 0.0;
      double $$11 = 0.0;
      boolean $$12 = true;
      double $$13 = 1.0;

      for (int $$14 = 0; $$14 < 8; $$14++) {
         ImprovedNoise $$15 = this.mainNoise.getOctaveNoise($$14);
         if ($$15 != null) {
            $$11 += $$15.noise(PerlinNoise.wrap($$4 * $$13), PerlinNoise.wrap($$5 * $$13), PerlinNoise.wrap($$6 * $$13), $$8 * $$13, $$5 * $$13) / $$13;
         }

         $$13 /= 2.0;
      }

      double $$16 = ($$11 / 10.0 + 1.0) / 2.0;
      boolean $$17 = $$16 >= 1.0;
      boolean $$18 = $$16 <= 0.0;
      $$13 = 1.0;

      for (int $$19 = 0; $$19 < 16; $$19++) {
         double $$20 = PerlinNoise.wrap($$1 * $$13);
         double $$21 = PerlinNoise.wrap($$2 * $$13);
         double $$22 = PerlinNoise.wrap($$3 * $$13);
         double $$23 = $$7 * $$13;
         if (!$$17) {
            ImprovedNoise $$24 = this.minLimitNoise.getOctaveNoise($$19);
            if ($$24 != null) {
               $$9 += $$24.noise($$20, $$21, $$22, $$23, $$2 * $$13) / $$13;
            }
         }

         if (!$$18) {
            ImprovedNoise $$25 = this.maxLimitNoise.getOctaveNoise($$19);
            if ($$25 != null) {
               $$10 += $$25.noise($$20, $$21, $$22, $$23, $$2 * $$13) / $$13;
            }
         }

         $$13 /= 2.0;
      }

      return Mth.clampedLerp($$9 / 512.0, $$10 / 512.0, $$16) / 128.0;
   }

   @Override
   public double minValue() {
      return -this.maxValue();
   }

   @Override
   public double maxValue() {
      return this.maxValue;
   }

   @VisibleForTesting
   public void parityConfigString(StringBuilder pBuilder) {
      pBuilder.append("BlendedNoise{minLimitNoise=");
      this.minLimitNoise.parityConfigString(pBuilder);
      pBuilder.append(", maxLimitNoise=");
      this.maxLimitNoise.parityConfigString(pBuilder);
      pBuilder.append(", mainNoise=");
      this.mainNoise.parityConfigString(pBuilder);
      pBuilder.append(
            String.format(
               Locale.ROOT,
               ", xzScale=%.3f, yScale=%.3f, xzMainScale=%.3f, yMainScale=%.3f, cellWidth=4, cellHeight=8",
               684.412,
               684.412,
               8.555150000000001,
               4.277575000000001
            )
         )
         .append('}');
   }

   @Override
   public KeyDispatchDataCodec<? extends DensityFunction> codec() {
      return CODEC;
   }
}
