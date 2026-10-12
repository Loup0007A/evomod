package net.minecraft.world.level.levelgen.synth;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

public class PerlinNoise {
   private static final int ROUND_OFF = 33554432;
   private final ImprovedNoise[] noiseLevels;
   private final int firstOctave;
   private final DoubleList amplitudes;
   private final double lowestFreqValueFactor;
   private final double lowestFreqInputFactor;
   private final double maxValue;

   @Deprecated
   public static PerlinNoise createLegacyForBlendedNoise(RandomSource pRandom, IntStream pOctaves) {
      return new PerlinNoise(pRandom, makeAmplitudes(new IntRBTreeSet(pOctaves.boxed().collect(ImmutableList.toImmutableList()))), false);
   }

   @Deprecated
   public static PerlinNoise createLegacyForLegacyNetherBiome(RandomSource pRandom, int pFirstOctave, DoubleList pAmplitudes) {
      return new PerlinNoise(pRandom, Pair.of(pFirstOctave, pAmplitudes), false);
   }

   public static PerlinNoise create(RandomSource pRandom, IntStream pOctaves) {
      return create(pRandom, pOctaves.boxed().collect(ImmutableList.toImmutableList()));
   }

   public static PerlinNoise create(RandomSource pRandom, List<Integer> pOctaves) {
      return new PerlinNoise(pRandom, makeAmplitudes(new IntRBTreeSet(pOctaves)), true);
   }

   public static PerlinNoise create(RandomSource pRandom, int pFirstOctave, double pFirstAmplitude, double... pAmplitudes) {
      DoubleArrayList $$4 = new DoubleArrayList(pAmplitudes);
      $$4.add(0, pFirstAmplitude);
      return new PerlinNoise(pRandom, Pair.of(pFirstOctave, $$4), true);
   }

   public static PerlinNoise create(RandomSource pRandom, int pFirstOctave, DoubleList pAmplitudes) {
      return new PerlinNoise(pRandom, Pair.of(pFirstOctave, pAmplitudes), true);
   }

   private static Pair<Integer, DoubleList> makeAmplitudes(IntSortedSet pOctaves) {
      if (pOctaves.isEmpty()) {
         throw new IllegalArgumentException("Need some octaves!");
      } else {
         int $$1 = -pOctaves.firstInt();
         int $$2 = pOctaves.lastInt();
         int $$3 = $$1 + $$2 + 1;
         if ($$3 < 1) {
            throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
         } else {
            DoubleList $$4 = new DoubleArrayList(new double[$$3]);
            IntBidirectionalIterator $$5 = pOctaves.iterator();

            while ($$5.hasNext()) {
               int $$6 = $$5.nextInt();
               $$4.set($$6 + $$1, 1.0);
            }

            return Pair.of(-$$1, $$4);
         }
      }
   }

   protected PerlinNoise(RandomSource pRandom, Pair<Integer, DoubleList> pOctavesAndAmplitudes, boolean pUseNewFactory) {
      this.firstOctave = (Integer)pOctavesAndAmplitudes.getFirst();
      this.amplitudes = (DoubleList)pOctavesAndAmplitudes.getSecond();
      int $$3 = this.amplitudes.size();
      int $$4 = -this.firstOctave;
      this.noiseLevels = new ImprovedNoise[$$3];
      if (pUseNewFactory) {
         PositionalRandomFactory $$5 = pRandom.forkPositional();

         for (int $$6 = 0; $$6 < $$3; $$6++) {
            if (this.amplitudes.getDouble($$6) != 0.0) {
               int $$7 = this.firstOctave + $$6;
               this.noiseLevels[$$6] = new ImprovedNoise($$5.fromHashOf("octave_" + $$7));
            }
         }
      } else {
         ImprovedNoise $$8 = new ImprovedNoise(pRandom);
         if ($$4 >= 0 && $$4 < $$3) {
            double $$9 = this.amplitudes.getDouble($$4);
            if ($$9 != 0.0) {
               this.noiseLevels[$$4] = $$8;
            }
         }

         for (int $$10 = $$4 - 1; $$10 >= 0; $$10--) {
            if ($$10 < $$3) {
               double $$11 = this.amplitudes.getDouble($$10);
               if ($$11 != 0.0) {
                  this.noiseLevels[$$10] = new ImprovedNoise(pRandom);
               } else {
                  skipOctave(pRandom);
               }
            } else {
               skipOctave(pRandom);
            }
         }

         if (Arrays.stream(this.noiseLevels).filter(Objects::nonNull).count() != this.amplitudes.stream().filter(p_192897_ -> p_192897_ != 0.0).count()) {
            throw new IllegalStateException("Failed to create correct number of noise levels for given non-zero amplitudes");
         }

         if ($$4 < $$3 - 1) {
            throw new IllegalArgumentException("Positive octaves are temporarily disabled");
         }
      }

      this.lowestFreqInputFactor = Math.pow(2.0, (double)(-$$4));
      this.lowestFreqValueFactor = Math.pow(2.0, (double)($$3 - 1)) / (Math.pow(2.0, (double)$$3) - 1.0);
      this.maxValue = this.edgeValue(2.0);
   }

   protected double maxValue() {
      return this.maxValue;
   }

   private static void skipOctave(RandomSource pRandom) {
      pRandom.consumeCount(262);
   }

   public double getValue(double pX, double pY, double pZ) {
      return this.getValue(pX, pY, pZ, 0.0, 0.0, false);
   }

   @Deprecated
   public double getValue(double pX, double pY, double pZ, double pYScale, double pYMax, boolean pUseFixedY) {
      double $$6 = 0.0;
      double $$7 = this.lowestFreqInputFactor;
      double $$8 = this.lowestFreqValueFactor;

      for (int $$9 = 0; $$9 < this.noiseLevels.length; $$9++) {
         ImprovedNoise $$10 = this.noiseLevels[$$9];
         if ($$10 != null) {
            double $$11 = $$10.noise(wrap(pX * $$7), pUseFixedY ? -$$10.yo : wrap(pY * $$7), wrap(pZ * $$7), pYScale * $$7, pYMax * $$7);
            $$6 += this.amplitudes.getDouble($$9) * $$11 * $$8;
         }

         $$7 *= 2.0;
         $$8 /= 2.0;
      }

      return $$6;
   }

   public double maxBrokenValue(double pYMultiplier) {
      return this.edgeValue(pYMultiplier + 2.0);
   }

   private double edgeValue(double pMultiplier) {
      double $$1 = 0.0;
      double $$2 = this.lowestFreqValueFactor;

      for (int $$3 = 0; $$3 < this.noiseLevels.length; $$3++) {
         ImprovedNoise $$4 = this.noiseLevels[$$3];
         if ($$4 != null) {
            $$1 += this.amplitudes.getDouble($$3) * pMultiplier * $$2;
         }

         $$2 /= 2.0;
      }

      return $$1;
   }

   @Nullable
   public ImprovedNoise getOctaveNoise(int pOctave) {
      return this.noiseLevels[this.noiseLevels.length - 1 - pOctave];
   }

   public static double wrap(double pValue) {
      return pValue - (double)Mth.lfloor(pValue / 3.3554432E7 + 0.5) * 3.3554432E7;
   }

   protected int firstOctave() {
      return this.firstOctave;
   }

   protected DoubleList amplitudes() {
      return this.amplitudes;
   }

   @VisibleForTesting
   public void parityConfigString(StringBuilder pBuilder) {
      pBuilder.append("PerlinNoise{");
      List<String> $$1 = this.amplitudes.stream().map(p_192889_ -> String.format(Locale.ROOT, "%.2f", p_192889_)).toList();
      pBuilder.append("first octave: ").append(this.firstOctave).append(", amplitudes: ").append($$1).append(", noise levels: [");

      for (int $$2 = 0; $$2 < this.noiseLevels.length; $$2++) {
         pBuilder.append($$2).append(": ");
         ImprovedNoise $$3 = this.noiseLevels[$$2];
         if ($$3 == null) {
            pBuilder.append("null");
         } else {
            $$3.parityConfigString(pBuilder);
         }

         pBuilder.append(", ");
      }

      pBuilder.append("]");
      pBuilder.append("}");
   }
}
