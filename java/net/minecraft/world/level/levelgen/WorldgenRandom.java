package net.minecraft.world.level.levelgen;

import java.util.function.LongFunction;
import net.minecraft.util.RandomSource;

public class WorldgenRandom extends LegacyRandomSource {
   private final RandomSource randomSource;
   private int count;

   public WorldgenRandom(RandomSource pRandomSource) {
      super(0L);
      this.randomSource = pRandomSource;
   }

   public int getCount() {
      return this.count;
   }

   @Override
   public RandomSource fork() {
      return this.randomSource.fork();
   }

   @Override
   public PositionalRandomFactory forkPositional() {
      return this.randomSource.forkPositional();
   }

   @Override
   public int next(int pBits) {
      this.count++;
      return this.randomSource instanceof LegacyRandomSource $$1 ? $$1.next(pBits) : (int)(this.randomSource.nextLong() >>> 64 - pBits);
   }

   @Override
   public synchronized void setSeed(long pSeed) {
      if (this.randomSource != null) {
         this.randomSource.setSeed(pSeed);
      }
   }

   public long setDecorationSeed(long pLevelSeed, int pMinChunkBlockX, int pMinChunkBlockZ) {
      this.setSeed(pLevelSeed);
      long $$3 = this.nextLong() | 1L;
      long $$4 = this.nextLong() | 1L;
      long $$5 = (long)pMinChunkBlockX * $$3 + (long)pMinChunkBlockZ * $$4 ^ pLevelSeed;
      this.setSeed($$5);
      return $$5;
   }

   public void setFeatureSeed(long pDecorationSeed, int pIndex, int pDecorationStep) {
      long $$3 = pDecorationSeed + (long)pIndex + (long)(10000 * pDecorationStep);
      this.setSeed($$3);
   }

   public void setLargeFeatureSeed(long pBaseSeed, int pChunkX, int pChunkZ) {
      this.setSeed(pBaseSeed);
      long $$3 = this.nextLong();
      long $$4 = this.nextLong();
      long $$5 = (long)pChunkX * $$3 ^ (long)pChunkZ * $$4 ^ pBaseSeed;
      this.setSeed($$5);
   }

   public void setLargeFeatureWithSalt(long pLevelSeed, int pRegionX, int pRegionZ, int pSalt) {
      long $$4 = (long)pRegionX * 341873128712L + (long)pRegionZ * 132897987541L + pLevelSeed + (long)pSalt;
      this.setSeed($$4);
   }

   public static RandomSource seedSlimeChunk(int pChunkX, int pChunkZ, long pLevelSeed, long pSalt) {
      return RandomSource.create(
         pLevelSeed + (long)(pChunkX * pChunkX * 4987142) + (long)(pChunkX * 5947611) + (long)(pChunkZ * pChunkZ) * 4392871L + (long)(pChunkZ * 389711) ^ pSalt
      );
   }

   public static enum Algorithm {
      LEGACY(LegacyRandomSource::new),
      XOROSHIRO(XoroshiroRandomSource::new);

      private final LongFunction<RandomSource> constructor;

      private Algorithm(final LongFunction<RandomSource> pConstructor) {
         this.constructor = pConstructor;
      }

      public RandomSource newInstance(long pSeed) {
         return this.constructor.apply(pSeed);
      }
   }
}
