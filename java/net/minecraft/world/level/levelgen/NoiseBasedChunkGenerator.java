package net.minecraft.world.level.levelgen;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.text.DecimalFormat;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import org.apache.commons.lang3.mutable.MutableObject;

public class NoiseBasedChunkGenerator extends ChunkGenerator {
   public static final MapCodec<NoiseBasedChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
      p_255585_ -> p_255585_.group(
               BiomeSource.CODEC.fieldOf("biome_source").forGetter(p_255584_ -> p_255584_.biomeSource),
               NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(p_224278_ -> p_224278_.settings)
            )
            .apply(p_255585_, p_255585_.stable(NoiseBasedChunkGenerator::new))
   );
   private static final BlockState AIR = Blocks.AIR.defaultBlockState();
   private final Holder<NoiseGeneratorSettings> settings;
   private final Supplier<Aquifer.FluidPicker> globalFluidPicker;

   public NoiseBasedChunkGenerator(BiomeSource p_256415_, Holder<NoiseGeneratorSettings> p_256182_) {
      super(p_256415_);
      this.settings = p_256182_;
      this.globalFluidPicker = Suppliers.memoize(() -> createFluidPicker(p_256182_.value()));
   }

   private static Aquifer.FluidPicker createFluidPicker(NoiseGeneratorSettings pSettings) {
      Aquifer.FluidStatus $$1 = new Aquifer.FluidStatus(-54, Blocks.LAVA.defaultBlockState());
      int $$2 = pSettings.seaLevel();
      Aquifer.FluidStatus $$3 = new Aquifer.FluidStatus($$2, pSettings.defaultFluid());
      Aquifer.FluidStatus $$4 = new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());
      return (p_224274_, p_224275_, p_224276_) -> p_224275_ < Math.min(-54, $$2) ? $$1 : $$3;
   }

   @Override
   public CompletableFuture<ChunkAccess> createBiomes(RandomState pRandomState, Blender pBlender, StructureManager pStructureManager, ChunkAccess pChunk) {
      return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("init_biomes", () -> {
         this.doCreateBiomes(pBlender, pRandomState, pStructureManager, pChunk);
         return pChunk;
      }), Util.backgroundExecutor());
   }

   private void doCreateBiomes(Blender pBlender, RandomState pRandom, StructureManager pStructureManager, ChunkAccess pChunk) {
      NoiseChunk $$4 = pChunk.getOrCreateNoiseChunk(p_224340_ -> this.createNoiseChunk(p_224340_, pStructureManager, pBlender, pRandom));
      BiomeResolver $$5 = BelowZeroRetrogen.getBiomeResolver(pBlender.getBiomeResolver(this.biomeSource), pChunk);
      pChunk.fillBiomesFromNoise($$5, $$4.cachedClimateSampler(pRandom.router(), this.settings.value().spawnTarget()));
   }

   private NoiseChunk createNoiseChunk(ChunkAccess pChunk, StructureManager pStructureManager, Blender pBlender, RandomState pRandom) {
      return NoiseChunk.forChunk(
         pChunk, pRandom, Beardifier.forStructuresInChunk(pStructureManager, pChunk.getPos()), this.settings.value(), this.globalFluidPicker.get(), pBlender
      );
   }

   @Override
   protected MapCodec<? extends ChunkGenerator> codec() {
      return CODEC;
   }

   public Holder<NoiseGeneratorSettings> generatorSettings() {
      return this.settings;
   }

   public boolean stable(ResourceKey<NoiseGeneratorSettings> pSettings) {
      return this.settings.is(pSettings);
   }

   @Override
   public int getBaseHeight(int pX, int pZ, Heightmap.Types pType, LevelHeightAccessor pLevel, RandomState pRandom) {
      return this.iterateNoiseColumn(pLevel, pRandom, pX, pZ, null, pType.isOpaque()).orElse(pLevel.getMinBuildHeight());
   }

   @Override
   public NoiseColumn getBaseColumn(int pX, int pZ, LevelHeightAccessor pHeight, RandomState pRandom) {
      MutableObject<NoiseColumn> $$4 = new MutableObject();
      this.iterateNoiseColumn(pHeight, pRandom, pX, pZ, $$4, null);
      return (NoiseColumn)$$4.getValue();
   }

   @Override
   public void addDebugScreenInfo(List<String> pInfo, RandomState pRandom, BlockPos pPos) {
      DecimalFormat $$3 = new DecimalFormat("0.000");
      NoiseRouter $$4 = pRandom.router();
      DensityFunction.SinglePointContext $$5 = new DensityFunction.SinglePointContext(pPos.getX(), pPos.getY(), pPos.getZ());
      double $$6 = $$4.ridges().compute($$5);
      pInfo.add(
         "NoiseRouter T: "
            + $$3.format($$4.temperature().compute($$5))
            + " V: "
            + $$3.format($$4.vegetation().compute($$5))
            + " C: "
            + $$3.format($$4.continents().compute($$5))
            + " E: "
            + $$3.format($$4.erosion().compute($$5))
            + " D: "
            + $$3.format($$4.depth().compute($$5))
            + " W: "
            + $$3.format($$6)
            + " PV: "
            + $$3.format((double)NoiseRouterData.peaksAndValleys((float)$$6))
            + " AS: "
            + $$3.format($$4.initialDensityWithoutJaggedness().compute($$5))
            + " N: "
            + $$3.format($$4.finalDensity().compute($$5))
      );
   }

   protected OptionalInt iterateNoiseColumn(
      LevelHeightAccessor pLevel,
      RandomState pRandom,
      int pX,
      int pZ,
      @Nullable MutableObject<NoiseColumn> pColumn,
      @Nullable Predicate<BlockState> pStoppingState
   ) {
      NoiseSettings $$6 = this.settings.value().noiseSettings().clampToHeightAccessor(pLevel);
      int $$7 = $$6.getCellHeight();
      int $$8 = $$6.minY();
      int $$9 = Mth.floorDiv($$8, $$7);
      int $$10 = Mth.floorDiv($$6.height(), $$7);
      if ($$10 <= 0) {
         return OptionalInt.empty();
      } else {
         BlockState[] $$11;
         if (pColumn == null) {
            $$11 = null;
         } else {
            $$11 = new BlockState[$$6.height()];
            pColumn.setValue(new NoiseColumn($$8, $$11));
         }

         int $$13 = $$6.getCellWidth();
         int $$14 = Math.floorDiv(pX, $$13);
         int $$15 = Math.floorDiv(pZ, $$13);
         int $$16 = Math.floorMod(pX, $$13);
         int $$17 = Math.floorMod(pZ, $$13);
         int $$18 = $$14 * $$13;
         int $$19 = $$15 * $$13;
         double $$20 = (double)$$16 / (double)$$13;
         double $$21 = (double)$$17 / (double)$$13;
         NoiseChunk $$22 = new NoiseChunk(
            1, pRandom, $$18, $$19, $$6, DensityFunctions.BeardifierMarker.INSTANCE, this.settings.value(), this.globalFluidPicker.get(), Blender.empty()
         );
         $$22.initializeForFirstCellX();
         $$22.advanceCellX(0);

         for (int $$23 = $$10 - 1; $$23 >= 0; $$23--) {
            $$22.selectCellYZ($$23, 0);

            for (int $$24 = $$7 - 1; $$24 >= 0; $$24--) {
               int $$25 = ($$9 + $$23) * $$7 + $$24;
               double $$26 = (double)$$24 / (double)$$7;
               $$22.updateForY($$25, $$26);
               $$22.updateForX(pX, $$20);
               $$22.updateForZ(pZ, $$21);
               BlockState $$27 = $$22.getInterpolatedState();
               BlockState $$28 = $$27 == null ? this.settings.value().defaultBlock() : $$27;
               if ($$11 != null) {
                  int $$29 = $$23 * $$7 + $$24;
                  $$11[$$29] = $$28;
               }

               if (pStoppingState != null && pStoppingState.test($$28)) {
                  $$22.stopInterpolation();
                  return OptionalInt.of($$25 + 1);
               }
            }
         }

         $$22.stopInterpolation();
         return OptionalInt.empty();
      }
   }

   @Override
   public void buildSurface(WorldGenRegion pLevel, StructureManager pStructureManager, RandomState pRandom, ChunkAccess pChunk) {
      if (!SharedConstants.debugVoidTerrain(pChunk.getPos())) {
         WorldGenerationContext $$4 = new WorldGenerationContext(this, pLevel);
         this.buildSurface(
            pChunk, $$4, pRandom, pStructureManager, pLevel.getBiomeManager(), pLevel.registryAccess().registryOrThrow(Registries.BIOME), Blender.of(pLevel)
         );
      }
   }

   @VisibleForTesting
   public void buildSurface(
      ChunkAccess pChunk,
      WorldGenerationContext pContext,
      RandomState pRandom,
      StructureManager pStructureManager,
      BiomeManager pBiomeManager,
      Registry<Biome> pBiomes,
      Blender pBlender
   ) {
      NoiseChunk $$7 = pChunk.getOrCreateNoiseChunk(p_224321_ -> this.createNoiseChunk(p_224321_, pStructureManager, pBlender, pRandom));
      NoiseGeneratorSettings $$8 = this.settings.value();
      pRandom.surfaceSystem().buildSurface(pRandom, pBiomeManager, pBiomes, $$8.useLegacyRandomSource(), pContext, pChunk, $$7, $$8.surfaceRule());
   }

   @Override
   public void applyCarvers(
      WorldGenRegion pLevel,
      long pSeed,
      RandomState pRandom,
      BiomeManager pBiomeManager,
      StructureManager pStructureManager,
      ChunkAccess pChunk,
      GenerationStep.Carving pStep
   ) {
      BiomeManager $$7 = pBiomeManager.withDifferentSource(
         (p_255581_, p_255582_, p_255583_) -> this.biomeSource.getNoiseBiome(p_255581_, p_255582_, p_255583_, pRandom.sampler())
      );
      WorldgenRandom $$8 = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
      int $$9 = 8;
      ChunkPos $$10 = pChunk.getPos();
      NoiseChunk $$11 = pChunk.getOrCreateNoiseChunk(p_224250_ -> this.createNoiseChunk(p_224250_, pStructureManager, Blender.of(pLevel), pRandom));
      Aquifer $$12 = $$11.aquifer();
      CarvingContext $$13 = new CarvingContext(
         this, pLevel.registryAccess(), pChunk.getHeightAccessorForGeneration(), $$11, pRandom, this.settings.value().surfaceRule()
      );
      CarvingMask $$14 = ((ProtoChunk)pChunk).getOrCreateCarvingMask(pStep);

      for (int $$15 = -8; $$15 <= 8; $$15++) {
         for (int $$16 = -8; $$16 <= 8; $$16++) {
            ChunkPos $$17 = new ChunkPos($$10.x + $$15, $$10.z + $$16);
            ChunkAccess $$18 = pLevel.getChunk($$17.x, $$17.z);
            BiomeGenerationSettings $$19 = $$18.carverBiome(
               () -> this.getBiomeGenerationSettings(
                     this.biomeSource.getNoiseBiome(QuartPos.fromBlock($$17.getMinBlockX()), 0, QuartPos.fromBlock($$17.getMinBlockZ()), pRandom.sampler())
                  )
            );
            Iterable<Holder<ConfiguredWorldCarver<?>>> $$20 = $$19.getCarvers(pStep);
            int $$21 = 0;

            for (Holder<ConfiguredWorldCarver<?>> $$22 : $$20) {
               ConfiguredWorldCarver<?> $$23 = $$22.value();
               $$8.setLargeFeatureSeed(pSeed + (long)$$21, $$17.x, $$17.z);
               if ($$23.isStartChunk($$8)) {
                  $$23.carve($$13, pChunk, $$7::getBiome, $$8, $$12, $$17, $$14);
               }

               $$21++;
            }
         }
      }
   }

   @Override
   public CompletableFuture<ChunkAccess> fillFromNoise(Blender pBlender, RandomState pRandomState, StructureManager pStructureManager, ChunkAccess pChunk) {
      NoiseSettings $$4 = this.settings.value().noiseSettings().clampToHeightAccessor(pChunk.getHeightAccessorForGeneration());
      int $$5 = $$4.minY();
      int $$6 = Mth.floorDiv($$5, $$4.getCellHeight());
      int $$7 = Mth.floorDiv($$4.height(), $$4.getCellHeight());
      return $$7 <= 0 ? CompletableFuture.completedFuture(pChunk) : CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("wgen_fill_noise", () -> {
         int $$8 = pChunk.getSectionIndex($$7 * $$4.getCellHeight() - 1 + $$5);
         int $$9 = pChunk.getSectionIndex($$5);
         Set<LevelChunkSection> $$10 = Sets.newHashSet();

         for (int $$11 = $$8; $$11 >= $$9; $$11--) {
            LevelChunkSection $$12 = pChunk.getSection($$11);
            $$12.acquire();
            $$10.add($$12);
         }

         ChunkAccess var20;
         try {
            var20 = this.doFill(pBlender, pStructureManager, pRandomState, pChunk, $$6, $$7);
         } finally {
            for (LevelChunkSection $$14 : $$10) {
               $$14.release();
            }
         }

         return var20;
      }), Util.backgroundExecutor());
   }

   private ChunkAccess doFill(Blender pBlender, StructureManager pStructureManager, RandomState pRandom, ChunkAccess pChunk, int pMinCellY, int pCellCountY) {
      NoiseChunk $$6 = pChunk.getOrCreateNoiseChunk(p_224255_ -> this.createNoiseChunk(p_224255_, pStructureManager, pBlender, pRandom));
      Heightmap $$7 = pChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
      Heightmap $$8 = pChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
      ChunkPos $$9 = pChunk.getPos();
      int $$10 = $$9.getMinBlockX();
      int $$11 = $$9.getMinBlockZ();
      Aquifer $$12 = $$6.aquifer();
      $$6.initializeForFirstCellX();
      BlockPos.MutableBlockPos $$13 = new BlockPos.MutableBlockPos();
      int $$14 = $$6.cellWidth();
      int $$15 = $$6.cellHeight();
      int $$16 = 16 / $$14;
      int $$17 = 16 / $$14;

      for (int $$18 = 0; $$18 < $$16; $$18++) {
         $$6.advanceCellX($$18);

         for (int $$19 = 0; $$19 < $$17; $$19++) {
            int $$20 = pChunk.getSectionsCount() - 1;
            LevelChunkSection $$21 = pChunk.getSection($$20);

            for (int $$22 = pCellCountY - 1; $$22 >= 0; $$22--) {
               $$6.selectCellYZ($$22, $$19);

               for (int $$23 = $$15 - 1; $$23 >= 0; $$23--) {
                  int $$24 = (pMinCellY + $$22) * $$15 + $$23;
                  int $$25 = $$24 & 15;
                  int $$26 = pChunk.getSectionIndex($$24);
                  if ($$20 != $$26) {
                     $$20 = $$26;
                     $$21 = pChunk.getSection($$26);
                  }

                  double $$27 = (double)$$23 / (double)$$15;
                  $$6.updateForY($$24, $$27);

                  for (int $$28 = 0; $$28 < $$14; $$28++) {
                     int $$29 = $$10 + $$18 * $$14 + $$28;
                     int $$30 = $$29 & 15;
                     double $$31 = (double)$$28 / (double)$$14;
                     $$6.updateForX($$29, $$31);

                     for (int $$32 = 0; $$32 < $$14; $$32++) {
                        int $$33 = $$11 + $$19 * $$14 + $$32;
                        int $$34 = $$33 & 15;
                        double $$35 = (double)$$32 / (double)$$14;
                        $$6.updateForZ($$33, $$35);
                        BlockState $$36 = $$6.getInterpolatedState();
                        if ($$36 == null) {
                           $$36 = this.settings.value().defaultBlock();
                        }

                        $$36 = this.debugPreliminarySurfaceLevel($$6, $$29, $$24, $$33, $$36);
                        if ($$36 != AIR && !SharedConstants.debugVoidTerrain(pChunk.getPos())) {
                           $$21.setBlockState($$30, $$25, $$34, $$36, false);
                           $$7.update($$30, $$24, $$34, $$36);
                           $$8.update($$30, $$24, $$34, $$36);
                           if ($$12.shouldScheduleFluidUpdate() && !$$36.getFluidState().isEmpty()) {
                              $$13.set($$29, $$24, $$33);
                              pChunk.markPosForPostprocessing($$13);
                           }
                        }
                     }
                  }
               }
            }
         }

         $$6.swapSlices();
      }

      $$6.stopInterpolation();
      return pChunk;
   }

   private BlockState debugPreliminarySurfaceLevel(NoiseChunk pChunk, int pX, int pY, int pZ, BlockState pState) {
      return pState;
   }

   @Override
   public int getGenDepth() {
      return this.settings.value().noiseSettings().height();
   }

   @Override
   public int getSeaLevel() {
      return this.settings.value().seaLevel();
   }

   @Override
   public int getMinY() {
      return this.settings.value().noiseSettings().minY();
   }

   @Override
   public void spawnOriginalMobs(WorldGenRegion pLevel) {
      if (!this.settings.value().disableMobGeneration()) {
         ChunkPos $$1 = pLevel.getCenter();
         Holder<Biome> $$2 = pLevel.getBiome($$1.getWorldPosition().atY(pLevel.getMaxBuildHeight() - 1));
         WorldgenRandom $$3 = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
         $$3.setDecorationSeed(pLevel.getSeed(), $$1.getMinBlockX(), $$1.getMinBlockZ());
         NaturalSpawner.spawnMobsForChunkGeneration(pLevel, $$2, $$1, $$3);
      }
   }
}
