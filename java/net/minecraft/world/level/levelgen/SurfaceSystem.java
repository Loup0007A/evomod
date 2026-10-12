package net.minecraft.world.level.levelgen;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BlockColumn;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class SurfaceSystem {
   private static final BlockState WHITE_TERRACOTTA = Blocks.WHITE_TERRACOTTA.defaultBlockState();
   private static final BlockState ORANGE_TERRACOTTA = Blocks.ORANGE_TERRACOTTA.defaultBlockState();
   private static final BlockState TERRACOTTA = Blocks.TERRACOTTA.defaultBlockState();
   private static final BlockState YELLOW_TERRACOTTA = Blocks.YELLOW_TERRACOTTA.defaultBlockState();
   private static final BlockState BROWN_TERRACOTTA = Blocks.BROWN_TERRACOTTA.defaultBlockState();
   private static final BlockState RED_TERRACOTTA = Blocks.RED_TERRACOTTA.defaultBlockState();
   private static final BlockState LIGHT_GRAY_TERRACOTTA = Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState();
   private static final BlockState PACKED_ICE = Blocks.PACKED_ICE.defaultBlockState();
   private static final BlockState SNOW_BLOCK = Blocks.SNOW_BLOCK.defaultBlockState();
   private final BlockState defaultBlock;
   private final int seaLevel;
   private final BlockState[] clayBands;
   private final NormalNoise clayBandsOffsetNoise;
   private final NormalNoise badlandsPillarNoise;
   private final NormalNoise badlandsPillarRoofNoise;
   private final NormalNoise badlandsSurfaceNoise;
   private final NormalNoise icebergPillarNoise;
   private final NormalNoise icebergPillarRoofNoise;
   private final NormalNoise icebergSurfaceNoise;
   private final PositionalRandomFactory noiseRandom;
   private final NormalNoise surfaceNoise;
   private final NormalNoise surfaceSecondaryNoise;

   public SurfaceSystem(RandomState pRandomState, BlockState pDefaultBlock, int pSeaLevel, PositionalRandomFactory pNoiseRandom) {
      this.defaultBlock = pDefaultBlock;
      this.seaLevel = pSeaLevel;
      this.noiseRandom = pNoiseRandom;
      this.clayBandsOffsetNoise = pRandomState.getOrCreateNoise(Noises.CLAY_BANDS_OFFSET);
      this.clayBands = generateBands(pNoiseRandom.fromHashOf(ResourceLocation.withDefaultNamespace("clay_bands")));
      this.surfaceNoise = pRandomState.getOrCreateNoise(Noises.SURFACE);
      this.surfaceSecondaryNoise = pRandomState.getOrCreateNoise(Noises.SURFACE_SECONDARY);
      this.badlandsPillarNoise = pRandomState.getOrCreateNoise(Noises.BADLANDS_PILLAR);
      this.badlandsPillarRoofNoise = pRandomState.getOrCreateNoise(Noises.BADLANDS_PILLAR_ROOF);
      this.badlandsSurfaceNoise = pRandomState.getOrCreateNoise(Noises.BADLANDS_SURFACE);
      this.icebergPillarNoise = pRandomState.getOrCreateNoise(Noises.ICEBERG_PILLAR);
      this.icebergPillarRoofNoise = pRandomState.getOrCreateNoise(Noises.ICEBERG_PILLAR_ROOF);
      this.icebergSurfaceNoise = pRandomState.getOrCreateNoise(Noises.ICEBERG_SURFACE);
   }

   public void buildSurface(
      RandomState pRandomState,
      BiomeManager pBiomeManager,
      Registry<Biome> pBiomes,
      boolean pUseLegacyRandomSource,
      WorldGenerationContext pContext,
      final ChunkAccess pChunk,
      NoiseChunk pNoiseChunk,
      SurfaceRules.RuleSource pRuleSource
   ) {
      final BlockPos.MutableBlockPos $$8 = new BlockPos.MutableBlockPos();
      final ChunkPos $$9 = pChunk.getPos();
      int $$10 = $$9.getMinBlockX();
      int $$11 = $$9.getMinBlockZ();
      BlockColumn $$12 = new BlockColumn() {
         @Override
         public BlockState getBlock(int p_190006_) {
            return pChunk.getBlockState($$8.setY(p_190006_));
         }

         @Override
         public void setBlock(int p_190008_, BlockState p_190009_) {
            LevelHeightAccessor $$2 = pChunk.getHeightAccessorForGeneration();
            if (p_190008_ >= $$2.getMinBuildHeight() && p_190008_ < $$2.getMaxBuildHeight()) {
               pChunk.setBlockState($$8.setY(p_190008_), p_190009_, false);
               if (!p_190009_.getFluidState().isEmpty()) {
                  pChunk.markPosForPostprocessing($$8);
               }
            }
         }

         @Override
         public String toString() {
            return "ChunkBlockColumn " + $$9;
         }
      };
      SurfaceRules.Context $$13 = new SurfaceRules.Context(this, pRandomState, pChunk, pNoiseChunk, pBiomeManager::getBiome, pBiomes, pContext);
      SurfaceRules.SurfaceRule $$14 = pRuleSource.apply($$13);
      BlockPos.MutableBlockPos $$15 = new BlockPos.MutableBlockPos();

      for (int $$16 = 0; $$16 < 16; $$16++) {
         for (int $$17 = 0; $$17 < 16; $$17++) {
            int $$18 = $$10 + $$16;
            int $$19 = $$11 + $$17;
            int $$20 = pChunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, $$16, $$17) + 1;
            $$8.setX($$18).setZ($$19);
            Holder<Biome> $$21 = pBiomeManager.getBiome($$15.set($$18, pUseLegacyRandomSource ? 0 : $$20, $$19));
            if ($$21.is(Biomes.ERODED_BADLANDS)) {
               this.erodedBadlandsExtension($$12, $$18, $$19, $$20, pChunk);
            }

            int $$22 = pChunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, $$16, $$17) + 1;
            $$13.updateXZ($$18, $$19);
            int $$23 = 0;
            int $$24 = Integer.MIN_VALUE;
            int $$25 = Integer.MAX_VALUE;
            int $$26 = pChunk.getMinBuildHeight();

            for (int $$27 = $$22; $$27 >= $$26; $$27--) {
               BlockState $$28 = $$12.getBlock($$27);
               if ($$28.isAir()) {
                  $$23 = 0;
                  $$24 = Integer.MIN_VALUE;
               } else if (!$$28.getFluidState().isEmpty()) {
                  if ($$24 == Integer.MIN_VALUE) {
                     $$24 = $$27 + 1;
                  }
               } else {
                  if ($$25 >= $$27) {
                     $$25 = DimensionType.WAY_BELOW_MIN_Y;

                     for (int $$29 = $$27 - 1; $$29 >= $$26 - 1; $$29--) {
                        BlockState $$30 = $$12.getBlock($$29);
                        if (!this.isStone($$30)) {
                           $$25 = $$29 + 1;
                           break;
                        }
                     }
                  }

                  $$23++;
                  int $$31 = $$27 - $$25 + 1;
                  $$13.updateY($$23, $$31, $$24, $$18, $$27, $$19);
                  if ($$28 == this.defaultBlock) {
                     BlockState $$32 = $$14.tryApply($$18, $$27, $$19);
                     if ($$32 != null) {
                        $$12.setBlock($$27, $$32);
                     }
                  }
               }
            }

            if ($$21.is(Biomes.FROZEN_OCEAN) || $$21.is(Biomes.DEEP_FROZEN_OCEAN)) {
               this.frozenOceanExtension($$13.getMinSurfaceLevel(), $$21.value(), $$12, $$15, $$18, $$19, $$20);
            }
         }
      }
   }

   protected int getSurfaceDepth(int pX, int pZ) {
      double $$2 = this.surfaceNoise.getValue((double)pX, 0.0, (double)pZ);
      return (int)($$2 * 2.75 + 3.0 + this.noiseRandom.at(pX, 0, pZ).nextDouble() * 0.25);
   }

   protected double getSurfaceSecondary(int pX, int pZ) {
      return this.surfaceSecondaryNoise.getValue((double)pX, 0.0, (double)pZ);
   }

   private boolean isStone(BlockState pState) {
      return !pState.isAir() && pState.getFluidState().isEmpty();
   }

   @Deprecated
   public Optional<BlockState> topMaterial(
      SurfaceRules.RuleSource pRule,
      CarvingContext pContext,
      Function<BlockPos, Holder<Biome>> pBiomeGetter,
      ChunkAccess pChunk,
      NoiseChunk pNoiseChunk,
      BlockPos pPos,
      boolean pHasFluid
   ) {
      SurfaceRules.Context $$7 = new SurfaceRules.Context(
         this, pContext.randomState(), pChunk, pNoiseChunk, pBiomeGetter, pContext.registryAccess().registryOrThrow(Registries.BIOME), pContext
      );
      SurfaceRules.SurfaceRule $$8 = pRule.apply($$7);
      int $$9 = pPos.getX();
      int $$10 = pPos.getY();
      int $$11 = pPos.getZ();
      $$7.updateXZ($$9, $$11);
      $$7.updateY(1, 1, pHasFluid ? $$10 + 1 : Integer.MIN_VALUE, $$9, $$10, $$11);
      BlockState $$12 = $$8.tryApply($$9, $$10, $$11);
      return Optional.ofNullable($$12);
   }

   private void erodedBadlandsExtension(BlockColumn pBlockColumn, int pX, int pZ, int pHeight, LevelHeightAccessor pLevel) {
      double $$5 = 0.2;
      double $$6 = Math.min(
         Math.abs(this.badlandsSurfaceNoise.getValue((double)pX, 0.0, (double)pZ) * 8.25),
         this.badlandsPillarNoise.getValue((double)pX * 0.2, 0.0, (double)pZ * 0.2) * 15.0
      );
      if (!($$6 <= 0.0)) {
         double $$7 = 0.75;
         double $$8 = 1.5;
         double $$9 = Math.abs(this.badlandsPillarRoofNoise.getValue((double)pX * 0.75, 0.0, (double)pZ * 0.75) * 1.5);
         double $$10 = 64.0 + Math.min($$6 * $$6 * 2.5, Math.ceil($$9 * 50.0) + 24.0);
         int $$11 = Mth.floor($$10);
         if (pHeight <= $$11) {
            for (int $$12 = $$11; $$12 >= pLevel.getMinBuildHeight(); $$12--) {
               BlockState $$13 = pBlockColumn.getBlock($$12);
               if ($$13.is(this.defaultBlock.getBlock())) {
                  break;
               }

               if ($$13.is(Blocks.WATER)) {
                  return;
               }
            }

            for (int $$14 = $$11; $$14 >= pLevel.getMinBuildHeight() && pBlockColumn.getBlock($$14).isAir(); $$14--) {
               pBlockColumn.setBlock($$14, this.defaultBlock);
            }
         }
      }
   }

   private void frozenOceanExtension(
      int pMinSurfaceLevel, Biome pBiome, BlockColumn pBlockColumn, BlockPos.MutableBlockPos pTopWaterPos, int pX, int pZ, int pHeight
   ) {
      double $$7 = 1.28;
      double $$8 = Math.min(
         Math.abs(this.icebergSurfaceNoise.getValue((double)pX, 0.0, (double)pZ) * 8.25),
         this.icebergPillarNoise.getValue((double)pX * 1.28, 0.0, (double)pZ * 1.28) * 15.0
      );
      if (!($$8 <= 1.8)) {
         double $$9 = 1.17;
         double $$10 = 1.5;
         double $$11 = Math.abs(this.icebergPillarRoofNoise.getValue((double)pX * 1.17, 0.0, (double)pZ * 1.17) * 1.5);
         double $$12 = Math.min($$8 * $$8 * 1.2, Math.ceil($$11 * 40.0) + 14.0);
         if (pBiome.shouldMeltFrozenOceanIcebergSlightly(pTopWaterPos.set(pX, 63, pZ))) {
            $$12 -= 2.0;
         }

         double $$13;
         if ($$12 > 2.0) {
            $$13 = (double)this.seaLevel - $$12 - 7.0;
            $$12 += (double)this.seaLevel;
         } else {
            $$12 = 0.0;
            $$13 = 0.0;
         }

         double $$15 = $$12;
         RandomSource $$16 = this.noiseRandom.at(pX, 0, pZ);
         int $$17 = 2 + $$16.nextInt(4);
         int $$18 = this.seaLevel + 18 + $$16.nextInt(10);
         int $$19 = 0;

         for (int $$20 = Math.max(pHeight, (int)$$12 + 1); $$20 >= pMinSurfaceLevel; $$20--) {
            if (pBlockColumn.getBlock($$20).isAir() && $$20 < (int)$$15 && $$16.nextDouble() > 0.01
               || pBlockColumn.getBlock($$20).is(Blocks.WATER) && $$20 > (int)$$13 && $$20 < this.seaLevel && $$13 != 0.0 && $$16.nextDouble() > 0.15) {
               if ($$19 <= $$17 && $$20 > $$18) {
                  pBlockColumn.setBlock($$20, SNOW_BLOCK);
                  $$19++;
               } else {
                  pBlockColumn.setBlock($$20, PACKED_ICE);
               }
            }
         }
      }
   }

   private static BlockState[] generateBands(RandomSource pRandom) {
      BlockState[] $$1 = new BlockState[192];
      Arrays.fill($$1, TERRACOTTA);

      for (int $$2 = 0; $$2 < $$1.length; $$2++) {
         $$2 += pRandom.nextInt(5) + 1;
         if ($$2 < $$1.length) {
            $$1[$$2] = ORANGE_TERRACOTTA;
         }
      }

      makeBands(pRandom, $$1, 1, YELLOW_TERRACOTTA);
      makeBands(pRandom, $$1, 2, BROWN_TERRACOTTA);
      makeBands(pRandom, $$1, 1, RED_TERRACOTTA);
      int $$3 = pRandom.nextIntBetweenInclusive(9, 15);
      int $$4 = 0;

      for (int $$5 = 0; $$4 < $$3 && $$5 < $$1.length; $$5 += pRandom.nextInt(16) + 4) {
         $$1[$$5] = WHITE_TERRACOTTA;
         if ($$5 - 1 > 0 && pRandom.nextBoolean()) {
            $$1[$$5 - 1] = LIGHT_GRAY_TERRACOTTA;
         }

         if ($$5 + 1 < $$1.length && pRandom.nextBoolean()) {
            $$1[$$5 + 1] = LIGHT_GRAY_TERRACOTTA;
         }

         $$4++;
      }

      return $$1;
   }

   private static void makeBands(RandomSource pRandom, BlockState[] pOutput, int pMinSize, BlockState pState) {
      int $$4 = pRandom.nextIntBetweenInclusive(6, 15);

      for (int $$5 = 0; $$5 < $$4; $$5++) {
         int $$6 = pMinSize + pRandom.nextInt(3);
         int $$7 = pRandom.nextInt(pOutput.length);

         for (int $$8 = 0; $$7 + $$8 < pOutput.length && $$8 < $$6; $$8++) {
            pOutput[$$7 + $$8] = pState;
         }
      }
   }

   protected BlockState getBand(int pX, int pY, int pZ) {
      int $$3 = (int)Math.round(this.clayBandsOffsetNoise.getValue((double)pX, 0.0, (double)pZ) * 4.0);
      return this.clayBands[(pY + $$3 + this.clayBands.length) % this.clayBands.length];
   }
}
