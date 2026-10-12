package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleListIterator;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.RandomSource;

public class NormalNoise {
   private static final double INPUT_FACTOR = 1.0181268882175227;
   private static final double TARGET_DEVIATION = 0.3333333333333333;
   private final double valueFactor;
   private final PerlinNoise first;
   private final PerlinNoise second;
   private final double maxValue;
   private final NormalNoise.NoiseParameters parameters;

   @Deprecated
   public static NormalNoise createLegacyNetherBiome(RandomSource pRandom, NormalNoise.NoiseParameters pParameters) {
      return new NormalNoise(pRandom, pParameters, false);
   }

   public static NormalNoise create(RandomSource pRandom, int pFirstOctave, double... pAmplitudes) {
      return create(pRandom, new NormalNoise.NoiseParameters(pFirstOctave, new DoubleArrayList(pAmplitudes)));
   }

   public static NormalNoise create(RandomSource pRandom, NormalNoise.NoiseParameters pParameters) {
      return new NormalNoise(pRandom, pParameters, true);
   }

   private NormalNoise(RandomSource pRandom, NormalNoise.NoiseParameters pParameters, boolean pUseLegacyNetherBiome) {
      int $$3 = pParameters.firstOctave;
      DoubleList $$4 = pParameters.amplitudes;
      this.parameters = pParameters;
      if (pUseLegacyNetherBiome) {
         this.first = PerlinNoise.create(pRandom, $$3, $$4);
         this.second = PerlinNoise.create(pRandom, $$3, $$4);
      } else {
         this.first = PerlinNoise.createLegacyForLegacyNetherBiome(pRandom, $$3, $$4);
         this.second = PerlinNoise.createLegacyForLegacyNetherBiome(pRandom, $$3, $$4);
      }

      int $$5 = Integer.MAX_VALUE;
      int $$6 = Integer.MIN_VALUE;
      DoubleListIterator $$7 = $$4.iterator();

      while ($$7.hasNext()) {
         int $$8 = $$7.nextIndex();
         double $$9 = $$7.nextDouble();
         if ($$9 != 0.0) {
            $$5 = Math.min($$5, $$8);
            $$6 = Math.max($$6, $$8);
         }
      }

      this.valueFactor = 0.16666666666666666 / expectedDeviation($$6 - $$5);
      this.maxValue = (this.first.maxValue() + this.second.maxValue()) * this.valueFactor;
   }

   public double maxValue() {
      return this.maxValue;
   }

   private static double expectedDeviation(int pOctaves) {
      return 0.1 * (1.0 + 1.0 / (double)(pOctaves + 1));
   }

   public double getValue(double pX, double pY, double pZ) {
      double $$3 = pX * 1.0181268882175227;
      double $$4 = pY * 1.0181268882175227;
      double $$5 = pZ * 1.0181268882175227;
      return (this.first.getValue(pX, pY, pZ) + this.second.getValue($$3, $$4, $$5)) * this.valueFactor;
   }

   public NormalNoise.NoiseParameters parameters() {
      return this.parameters;
   }

   @VisibleForTesting
   public void parityConfigString(StringBuilder pBuilder) {
      pBuilder.append("NormalNoise {");
      pBuilder.append("first: ");
      this.first.parityConfigString(pBuilder);
      pBuilder.append(", second: ");
      this.second.parityConfigString(pBuilder);
      pBuilder.append("}");
   }

   public static record NoiseParameters(int firstOctave, DoubleList amplitudes) {
      public static final Codec<NormalNoise.NoiseParameters> DIRECT_CODEC = RecordCodecBuilder.create(
         p_192865_ -> p_192865_.group(
                  Codec.INT.fieldOf("firstOctave").forGetter(NormalNoise.NoiseParameters::firstOctave),
                  Codec.DOUBLE.listOf().fieldOf("amplitudes").forGetter(NormalNoise.NoiseParameters::amplitudes)
               )
               .apply(p_192865_, NormalNoise.NoiseParameters::new)
      );
      public static final Codec<Holder<NormalNoise.NoiseParameters>> CODEC = RegistryFileCodec.create(Registries.NOISE, DIRECT_CODEC);

      public NoiseParameters(int p_192861_, List<Double> p_192862_) {
         this(p_192861_, new DoubleArrayList(p_192862_));
      }

      public NoiseParameters(int pFirstOctave, double pAmplitude, double... pOtherAmplitudes) {
         this(pFirstOctave, Util.make(new DoubleArrayList(pOtherAmplitudes), p_210636_ -> p_210636_.add(0, pAmplitude)));
      }
   }
}
