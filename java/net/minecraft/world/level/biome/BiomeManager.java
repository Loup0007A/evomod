package net.minecraft.world.level.biome;

import com.google.common.hash.Hashing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.util.Mth;

public class BiomeManager {
   public static final int CHUNK_CENTER_QUART = QuartPos.fromBlock(8);
   private static final int ZOOM_BITS = 2;
   private static final int ZOOM = 4;
   private static final int ZOOM_MASK = 3;
   private final BiomeManager.NoiseBiomeSource noiseBiomeSource;
   private final long biomeZoomSeed;

   public BiomeManager(BiomeManager.NoiseBiomeSource pNoiseBiomeSource, long pBiomeZoomSeed) {
      this.noiseBiomeSource = pNoiseBiomeSource;
      this.biomeZoomSeed = pBiomeZoomSeed;
   }

   public static long obfuscateSeed(long pSeed) {
      return Hashing.sha256().hashLong(pSeed).asLong();
   }

   public BiomeManager withDifferentSource(BiomeManager.NoiseBiomeSource pNewSource) {
      return new BiomeManager(pNewSource, this.biomeZoomSeed);
   }

   public Holder<Biome> getBiome(BlockPos pPos) {
      int $$1 = pPos.getX() - 2;
      int $$2 = pPos.getY() - 2;
      int $$3 = pPos.getZ() - 2;
      int $$4 = $$1 >> 2;
      int $$5 = $$2 >> 2;
      int $$6 = $$3 >> 2;
      double $$7 = (double)($$1 & 3) / 4.0;
      double $$8 = (double)($$2 & 3) / 4.0;
      double $$9 = (double)($$3 & 3) / 4.0;
      int $$10 = 0;
      double $$11 = Double.POSITIVE_INFINITY;

      for (int $$12 = 0; $$12 < 8; $$12++) {
         boolean $$13 = ($$12 & 4) == 0;
         boolean $$14 = ($$12 & 2) == 0;
         boolean $$15 = ($$12 & 1) == 0;
         int $$16 = $$13 ? $$4 : $$4 + 1;
         int $$17 = $$14 ? $$5 : $$5 + 1;
         int $$18 = $$15 ? $$6 : $$6 + 1;
         double $$19 = $$13 ? $$7 : $$7 - 1.0;
         double $$20 = $$14 ? $$8 : $$8 - 1.0;
         double $$21 = $$15 ? $$9 : $$9 - 1.0;
         double $$22 = getFiddledDistance(this.biomeZoomSeed, $$16, $$17, $$18, $$19, $$20, $$21);
         if ($$11 > $$22) {
            $$10 = $$12;
            $$11 = $$22;
         }
      }

      int $$23 = ($$10 & 4) == 0 ? $$4 : $$4 + 1;
      int $$24 = ($$10 & 2) == 0 ? $$5 : $$5 + 1;
      int $$25 = ($$10 & 1) == 0 ? $$6 : $$6 + 1;
      return this.noiseBiomeSource.getNoiseBiome($$23, $$24, $$25);
   }

   public Holder<Biome> getNoiseBiomeAtPosition(double pX, double pY, double pZ) {
      int $$3 = QuartPos.fromBlock(Mth.floor(pX));
      int $$4 = QuartPos.fromBlock(Mth.floor(pY));
      int $$5 = QuartPos.fromBlock(Mth.floor(pZ));
      return this.getNoiseBiomeAtQuart($$3, $$4, $$5);
   }

   public Holder<Biome> getNoiseBiomeAtPosition(BlockPos pPos) {
      int $$1 = QuartPos.fromBlock(pPos.getX());
      int $$2 = QuartPos.fromBlock(pPos.getY());
      int $$3 = QuartPos.fromBlock(pPos.getZ());
      return this.getNoiseBiomeAtQuart($$1, $$2, $$3);
   }

   public Holder<Biome> getNoiseBiomeAtQuart(int pX, int pY, int pZ) {
      return this.noiseBiomeSource.getNoiseBiome(pX, pY, pZ);
   }

   private static double getFiddledDistance(long pSeed, int pX, int pY, int pZ, double pXNoise, double pYNoise, double pZNoise) {
      long $$7 = LinearCongruentialGenerator.next(pSeed, (long)pX);
      $$7 = LinearCongruentialGenerator.next($$7, (long)pY);
      $$7 = LinearCongruentialGenerator.next($$7, (long)pZ);
      $$7 = LinearCongruentialGenerator.next($$7, (long)pX);
      $$7 = LinearCongruentialGenerator.next($$7, (long)pY);
      $$7 = LinearCongruentialGenerator.next($$7, (long)pZ);
      double $$8 = getFiddle($$7);
      $$7 = LinearCongruentialGenerator.next($$7, pSeed);
      double $$9 = getFiddle($$7);
      $$7 = LinearCongruentialGenerator.next($$7, pSeed);
      double $$10 = getFiddle($$7);
      return Mth.square(pZNoise + $$10) + Mth.square(pYNoise + $$9) + Mth.square(pXNoise + $$8);
   }

   private static double getFiddle(long pSeed) {
      double $$1 = (double)Math.floorMod(pSeed >> 24, 1024) / 1024.0;
      return ($$1 - 0.5) * 0.9;
   }

   public interface NoiseBiomeSource {
      Holder<Biome> getNoiseBiome(int pX, int pY, int pZ);
   }
}
