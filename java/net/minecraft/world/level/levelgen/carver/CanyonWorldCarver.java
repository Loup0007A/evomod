package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

public class CanyonWorldCarver extends WorldCarver<CanyonCarverConfiguration> {
   public CanyonWorldCarver(Codec<CanyonCarverConfiguration> pCodec) {
      super(pCodec);
   }

   public boolean isStartChunk(CanyonCarverConfiguration pConfig, RandomSource pRandom) {
      return pRandom.nextFloat() <= pConfig.probability;
   }

   public boolean carve(
      CarvingContext pContext,
      CanyonCarverConfiguration pConfig,
      ChunkAccess pChunk,
      Function<BlockPos, Holder<Biome>> pBiomeAccessor,
      RandomSource pRandom,
      Aquifer pAquifer,
      ChunkPos pChunkPos,
      CarvingMask pCarvingMask
   ) {
      int $$8 = (this.getRange() * 2 - 1) * 16;
      double $$9 = (double)pChunkPos.getBlockX(pRandom.nextInt(16));
      int $$10 = pConfig.y.sample(pRandom, pContext);
      double $$11 = (double)pChunkPos.getBlockZ(pRandom.nextInt(16));
      float $$12 = pRandom.nextFloat() * (float) (Math.PI * 2);
      float $$13 = pConfig.verticalRotation.sample(pRandom);
      double $$14 = (double)pConfig.yScale.sample(pRandom);
      float $$15 = pConfig.shape.thickness.sample(pRandom);
      int $$16 = (int)((float)$$8 * pConfig.shape.distanceFactor.sample(pRandom));
      int $$17 = 0;
      this.doCarve(
         pContext, pConfig, pChunk, pBiomeAccessor, pRandom.nextLong(), pAquifer, $$9, (double)$$10, $$11, $$15, $$12, $$13, 0, $$16, $$14, pCarvingMask
      );
      return true;
   }

   private void doCarve(
      CarvingContext pContext,
      CanyonCarverConfiguration pConfig,
      ChunkAccess pChunk,
      Function<BlockPos, Holder<Biome>> pBiomeAccessor,
      long pSeed,
      Aquifer pAquifer,
      double pX,
      double pY,
      double pZ,
      float pThickness,
      float pYaw,
      float pPitch,
      int pBranchIndex,
      int pBranchCount,
      double pHorizontalVerticalRatio,
      CarvingMask pCarvingMask
   ) {
      RandomSource $$16 = RandomSource.create(pSeed);
      float[] $$17 = this.initWidthFactors(pContext, pConfig, $$16);
      float $$18 = 0.0F;
      float $$19 = 0.0F;

      for (int $$20 = pBranchIndex; $$20 < pBranchCount; $$20++) {
         double $$21 = 1.5 + (double)(Mth.sin((float)$$20 * (float) Math.PI / (float)pBranchCount) * pThickness);
         double $$22 = $$21 * pHorizontalVerticalRatio;
         $$21 *= (double)pConfig.shape.horizontalRadiusFactor.sample($$16);
         $$22 = this.updateVerticalRadius(pConfig, $$16, $$22, (float)pBranchCount, (float)$$20);
         float $$23 = Mth.cos(pPitch);
         float $$24 = Mth.sin(pPitch);
         pX += (double)(Mth.cos(pYaw) * $$23);
         pY += (double)$$24;
         pZ += (double)(Mth.sin(pYaw) * $$23);
         pPitch *= 0.7F;
         pPitch += $$19 * 0.05F;
         pYaw += $$18 * 0.05F;
         $$19 *= 0.8F;
         $$18 *= 0.5F;
         $$19 += ($$16.nextFloat() - $$16.nextFloat()) * $$16.nextFloat() * 2.0F;
         $$18 += ($$16.nextFloat() - $$16.nextFloat()) * $$16.nextFloat() * 4.0F;
         if ($$16.nextInt(4) != 0) {
            if (!canReach(pChunk.getPos(), pX, pZ, $$20, pBranchCount, pThickness)) {
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
               $$21,
               $$22,
               pCarvingMask,
               (p_159082_, p_159083_, p_159084_, p_159085_, p_159086_) -> this.shouldSkip(p_159082_, $$17, p_159083_, p_159084_, p_159085_, p_159086_)
            );
         }
      }
   }

   private float[] initWidthFactors(CarvingContext pContext, CanyonCarverConfiguration pConfig, RandomSource pRandom) {
      int $$3 = pContext.getGenDepth();
      float[] $$4 = new float[$$3];
      float $$5 = 1.0F;

      for (int $$6 = 0; $$6 < $$3; $$6++) {
         if ($$6 == 0 || pRandom.nextInt(pConfig.shape.widthSmoothness) == 0) {
            $$5 = 1.0F + pRandom.nextFloat() * pRandom.nextFloat();
         }

         $$4[$$6] = $$5 * $$5;
      }

      return $$4;
   }

   private double updateVerticalRadius(
      CanyonCarverConfiguration pConfig, RandomSource pRandom, double pVerticalRadius, float pBranchCount, float pCurrentBranch
   ) {
      float $$5 = 1.0F - Mth.abs(0.5F - pCurrentBranch / pBranchCount) * 2.0F;
      float $$6 = pConfig.shape.verticalRadiusDefaultFactor + pConfig.shape.verticalRadiusCenterFactor * $$5;
      return (double)$$6 * pVerticalRadius * (double)Mth.randomBetween(pRandom, 0.75F, 1.0F);
   }

   private boolean shouldSkip(CarvingContext pContext, float[] pWidthFactors, double pRelativeX, double pRelativeY, double pRelativeZ, int pY) {
      int $$6 = pY - pContext.getMinGenY();
      return (pRelativeX * pRelativeX + pRelativeZ * pRelativeZ) * (double)pWidthFactors[$$6 - 1] + pRelativeY * pRelativeY / 6.0 >= 1.0;
   }
}
