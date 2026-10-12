package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

public class CaveWorldCarver extends WorldCarver<CaveCarverConfiguration> {
   public CaveWorldCarver(Codec<CaveCarverConfiguration> pCodec) {
      super(pCodec);
   }

   public boolean isStartChunk(CaveCarverConfiguration pConfig, RandomSource pRandom) {
      return pRandom.nextFloat() <= pConfig.probability;
   }

   public boolean carve(
      CarvingContext pContext,
      CaveCarverConfiguration pConfig,
      ChunkAccess pChunk,
      Function<BlockPos, Holder<Biome>> pBiomeAccessor,
      RandomSource pRandom,
      Aquifer pAquifer,
      ChunkPos pChunkPos,
      CarvingMask pCarvingMask
   ) {
      int $$8 = SectionPos.sectionToBlockCoord(this.getRange() * 2 - 1);
      int $$9 = pRandom.nextInt(pRandom.nextInt(pRandom.nextInt(this.getCaveBound()) + 1) + 1);

      for (int $$10 = 0; $$10 < $$9; $$10++) {
         double $$11 = (double)pChunkPos.getBlockX(pRandom.nextInt(16));
         double $$12 = (double)pConfig.y.sample(pRandom, pContext);
         double $$13 = (double)pChunkPos.getBlockZ(pRandom.nextInt(16));
         double $$14 = (double)pConfig.horizontalRadiusMultiplier.sample(pRandom);
         double $$15 = (double)pConfig.verticalRadiusMultiplier.sample(pRandom);
         double $$16 = (double)pConfig.floorLevel.sample(pRandom);
         WorldCarver.CarveSkipChecker $$17 = (p_159202_, p_159203_, p_159204_, p_159205_, p_159206_) -> shouldSkip(p_159203_, p_159204_, p_159205_, $$16);
         int $$18 = 1;
         if (pRandom.nextInt(4) == 0) {
            double $$19 = (double)pConfig.yScale.sample(pRandom);
            float $$20 = 1.0F + pRandom.nextFloat() * 6.0F;
            this.createRoom(pContext, pConfig, pChunk, pBiomeAccessor, pAquifer, $$11, $$12, $$13, $$20, $$19, pCarvingMask, $$17);
            $$18 += pRandom.nextInt(4);
         }

         for (int $$21 = 0; $$21 < $$18; $$21++) {
            float $$22 = pRandom.nextFloat() * (float) (Math.PI * 2);
            float $$23 = (pRandom.nextFloat() - 0.5F) / 4.0F;
            float $$24 = this.getThickness(pRandom);
            int $$25 = $$8 - pRandom.nextInt($$8 / 4);
            int $$26 = 0;
            this.createTunnel(
               pContext,
               pConfig,
               pChunk,
               pBiomeAccessor,
               pRandom.nextLong(),
               pAquifer,
               $$11,
               $$12,
               $$13,
               $$14,
               $$15,
               $$24,
               $$22,
               $$23,
               0,
               $$25,
               this.getYScale(),
               pCarvingMask,
               $$17
            );
         }
      }

      return true;
   }

   protected int getCaveBound() {
      return 15;
   }

   protected float getThickness(RandomSource pRandom) {
      float $$1 = pRandom.nextFloat() * 2.0F + pRandom.nextFloat();
      if (pRandom.nextInt(10) == 0) {
         $$1 *= pRandom.nextFloat() * pRandom.nextFloat() * 3.0F + 1.0F;
      }

      return $$1;
   }

   protected double getYScale() {
      return 1.0;
   }

   protected void createRoom(
      CarvingContext pContext,
      CaveCarverConfiguration pConfig,
      ChunkAccess pChunk,
      Function<BlockPos, Holder<Biome>> pBiomeAccessor,
      Aquifer pAquifer,
      double pX,
      double pY,
      double pZ,
      float pRadius,
      double pHorizontalVerticalRatio,
      CarvingMask pCarvingMask,
      WorldCarver.CarveSkipChecker pSkipChecker
   ) {
      double $$12 = 1.5 + (double)(Mth.sin((float) (Math.PI / 2)) * pRadius);
      double $$13 = $$12 * pHorizontalVerticalRatio;
      this.carveEllipsoid(pContext, pConfig, pChunk, pBiomeAccessor, pAquifer, pX + 1.0, pY, pZ, $$12, $$13, pCarvingMask, pSkipChecker);
   }

   protected void createTunnel(
      CarvingContext pContext,
      CaveCarverConfiguration pConfig,
      ChunkAccess pChunk,
      Function<BlockPos, Holder<Biome>> pBiomeAccessor,
      long pSeed,
      Aquifer pAquifer,
      double pX,
      double pY,
      double pZ,
      double pHorizontalRadiusMultiplier,
      double pVerticalRadiusMultiplier,
      float pThickness,
      float pYaw,
      float pPitch,
      int pBranchIndex,
      int pBranchCount,
      double pHorizontalVerticalRatio,
      CarvingMask pCarvingMask,
      WorldCarver.CarveSkipChecker pSkipChecker
   ) {
      RandomSource $$19 = RandomSource.create(pSeed);
      int $$20 = $$19.nextInt(pBranchCount / 2) + pBranchCount / 4;
      boolean $$21 = $$19.nextInt(6) == 0;
      float $$22 = 0.0F;
      float $$23 = 0.0F;

      for (int $$24 = pBranchIndex; $$24 < pBranchCount; $$24++) {
         double $$25 = 1.5 + (double)(Mth.sin((float) Math.PI * (float)$$24 / (float)pBranchCount) * pThickness);
         double $$26 = $$25 * pHorizontalVerticalRatio;
         float $$27 = Mth.cos(pPitch);
         pX += (double)(Mth.cos(pYaw) * $$27);
         pY += (double)Mth.sin(pPitch);
         pZ += (double)(Mth.sin(pYaw) * $$27);
         pPitch *= $$21 ? 0.92F : 0.7F;
         pPitch += $$23 * 0.1F;
         pYaw += $$22 * 0.1F;
         $$23 *= 0.9F;
         $$22 *= 0.75F;
         $$23 += ($$19.nextFloat() - $$19.nextFloat()) * $$19.nextFloat() * 2.0F;
         $$22 += ($$19.nextFloat() - $$19.nextFloat()) * $$19.nextFloat() * 4.0F;
         if ($$24 == $$20 && pThickness > 1.0F) {
            this.createTunnel(
               pContext,
               pConfig,
               pChunk,
               pBiomeAccessor,
               $$19.nextLong(),
               pAquifer,
               pX,
               pY,
               pZ,
               pHorizontalRadiusMultiplier,
               pVerticalRadiusMultiplier,
               $$19.nextFloat() * 0.5F + 0.5F,
               pYaw - (float) (Math.PI / 2),
               pPitch / 3.0F,
               $$24,
               pBranchCount,
               1.0,
               pCarvingMask,
               pSkipChecker
            );
            this.createTunnel(
               pContext,
               pConfig,
               pChunk,
               pBiomeAccessor,
               $$19.nextLong(),
               pAquifer,
               pX,
               pY,
               pZ,
               pHorizontalRadiusMultiplier,
               pVerticalRadiusMultiplier,
               $$19.nextFloat() * 0.5F + 0.5F,
               pYaw + (float) (Math.PI / 2),
               pPitch / 3.0F,
               $$24,
               pBranchCount,
               1.0,
               pCarvingMask,
               pSkipChecker
            );
            return;
         }

         if ($$19.nextInt(4) != 0) {
            if (!canReach(pChunk.getPos(), pX, pZ, $$24, pBranchCount, pThickness)) {
               return;
            }

            this.carveEllipsoid(
               pContext,
               pConfig,
               pChunk,
               pBiomeAccessor,
               pAquifer,
               pX,
               pY,
               pZ,
               $$25 * pHorizontalRadiusMultiplier,
               $$26 * pVerticalRadiusMultiplier,
               pCarvingMask,
               pSkipChecker
            );
         }
      }
   }

   private static boolean shouldSkip(double pRelative, double pRelativeY, double pRelativeZ, double pMinrelativeY) {
      return pRelativeY <= pMinrelativeY ? true : pRelative * pRelative + pRelativeY * pRelativeY + pRelativeZ * pRelativeZ >= 1.0;
   }
}
