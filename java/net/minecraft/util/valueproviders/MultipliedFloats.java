package net.minecraft.util.valueproviders;

import java.util.Arrays;
import net.minecraft.util.RandomSource;

public class MultipliedFloats implements SampledFloat {
   private final SampledFloat[] values;

   public MultipliedFloats(SampledFloat... pValues) {
      this.values = pValues;
   }

   @Override
   public float sample(RandomSource pRandom) {
      float $$1 = 1.0F;

      for (SampledFloat $$2 : this.values) {
         $$1 *= $$2.sample(pRandom);
      }

      return $$1;
   }

   @Override
   public String toString() {
      return "MultipliedFloats" + Arrays.toString((Object[])this.values);
   }
}
