package net.minecraft.util.random;

import java.util.List;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.util.RandomSource;

public class WeightedRandom {
   private WeightedRandom() {
   }

   public static int getTotalWeight(List<? extends WeightedEntry> pEntries) {
      long $$1 = 0L;

      for (WeightedEntry $$2 : pEntries) {
         $$1 += (long)$$2.getWeight().asInt();
      }

      if ($$1 > 2147483647L) {
         throw new IllegalArgumentException("Sum of weights must be <= 2147483647");
      } else {
         return (int)$$1;
      }
   }

   public static <T extends WeightedEntry> Optional<T> getRandomItem(RandomSource pRandom, List<T> pEntries, int pTotalWeight) {
      if (pTotalWeight < 0) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("Negative total weight in getRandomItem"));
      } else if (pTotalWeight == 0) {
         return Optional.empty();
      } else {
         int $$3 = pRandom.nextInt(pTotalWeight);
         return getWeightedItem(pEntries, $$3);
      }
   }

   public static <T extends WeightedEntry> Optional<T> getWeightedItem(List<T> pEntries, int pWeightedIndex) {
      for (T $$2 : pEntries) {
         pWeightedIndex -= $$2.getWeight().asInt();
         if (pWeightedIndex < 0) {
            return Optional.of($$2);
         }
      }

      return Optional.empty();
   }

   public static <T extends WeightedEntry> Optional<T> getRandomItem(RandomSource pRandom, List<T> pEntries) {
      return getRandomItem(pRandom, pEntries, getTotalWeight(pEntries));
   }
}
