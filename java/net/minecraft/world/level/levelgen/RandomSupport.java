package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.primitives.Longs;
import java.util.concurrent.atomic.AtomicLong;

public final class RandomSupport {
   public static final long GOLDEN_RATIO_64 = -7046029254386353131L;
   public static final long SILVER_RATIO_64 = 7640891576956012809L;
   private static final HashFunction MD5_128 = Hashing.md5();
   private static final AtomicLong SEED_UNIQUIFIER = new AtomicLong(8682522807148012L);

   @VisibleForTesting
   public static long mixStafford13(long pSeed) {
      pSeed = (pSeed ^ pSeed >>> 30) * -4658895280553007687L;
      pSeed = (pSeed ^ pSeed >>> 27) * -7723592293110705685L;
      return pSeed ^ pSeed >>> 31;
   }

   public static RandomSupport.Seed128bit upgradeSeedTo128bitUnmixed(long pSeed) {
      long $$1 = pSeed ^ 7640891576956012809L;
      long $$2 = $$1 + -7046029254386353131L;
      return new RandomSupport.Seed128bit($$1, $$2);
   }

   public static RandomSupport.Seed128bit upgradeSeedTo128bit(long pSeed) {
      return upgradeSeedTo128bitUnmixed(pSeed).mixed();
   }

   public static RandomSupport.Seed128bit seedFromHashOf(String pString) {
      byte[] $$1 = MD5_128.hashString(pString, Charsets.UTF_8).asBytes();
      long $$2 = Longs.fromBytes($$1[0], $$1[1], $$1[2], $$1[3], $$1[4], $$1[5], $$1[6], $$1[7]);
      long $$3 = Longs.fromBytes($$1[8], $$1[9], $$1[10], $$1[11], $$1[12], $$1[13], $$1[14], $$1[15]);
      return new RandomSupport.Seed128bit($$2, $$3);
   }

   public static long generateUniqueSeed() {
      return SEED_UNIQUIFIER.updateAndGet(p_224601_ -> p_224601_ * 1181783497276652981L) ^ System.nanoTime();
   }

   public static record Seed128bit(long seedLo, long seedHi) {
      public RandomSupport.Seed128bit xor(long pSeedLo, long pSeedHi) {
         return new RandomSupport.Seed128bit(this.seedLo ^ pSeedLo, this.seedHi ^ pSeedHi);
      }

      public RandomSupport.Seed128bit xor(RandomSupport.Seed128bit pSeed) {
         return this.xor(pSeed.seedLo, pSeed.seedHi);
      }

      public RandomSupport.Seed128bit mixed() {
         return new RandomSupport.Seed128bit(RandomSupport.mixStafford13(this.seedLo), RandomSupport.mixStafford13(this.seedHi));
      }
   }
}
