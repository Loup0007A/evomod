package net.minecraft.world.level.levelgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public class FlatLevelSource extends ChunkGenerator {
   public static final MapCodec<FlatLevelSource> CODEC = RecordCodecBuilder.mapCodec(
      p_255577_ -> p_255577_.group(FlatLevelGeneratorSettings.CODEC.fieldOf("settings").forGetter(FlatLevelSource::settings))
            .apply(p_255577_, p_255577_.stable(FlatLevelSource::new))
   );
   private final FlatLevelGeneratorSettings settings;

   public FlatLevelSource(FlatLevelGeneratorSettings p_256337_) {
      super(new FixedBiomeSource(p_256337_.getBiome()), Util.memoize(p_256337_::adjustGenerationSettings));
      this.settings = p_256337_;
   }

   @Override
   public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> pStructureSetLookup, RandomState pRandomState, long pSeed) {
      Stream<Holder<StructureSet>> $$3 = this.settings
         .structureOverrides()
         .map(HolderSet::stream)
         .orElseGet(() -> pStructureSetLookup.listElements().map(p_255579_ -> (Holder<StructureSet>)p_255579_));
      return ChunkGeneratorStructureState.createForFlat(pRandomState, pSeed, this.biomeSource, $$3);
   }

   @Override
   protected MapCodec<? extends ChunkGenerator> codec() {
      return CODEC;
   }

   public FlatLevelGeneratorSettings settings() {
      return this.settings;
   }

   @Override
   public void buildSurface(WorldGenRegion pLevel, StructureManager pStructureManager, RandomState pRandom, ChunkAccess pChunk) {
   }

   @Override
   public int getSpawnHeight(LevelHeightAccessor pLevel) {
      return pLevel.getMinBuildHeight() + Math.min(pLevel.getHeight(), this.settings.getLayers().size());
   }

   @Override
   public CompletableFuture<ChunkAccess> fillFromNoise(Blender pBlender, RandomState pRandomState, StructureManager pStructureManager, ChunkAccess pChunk) {
      List<BlockState> $$4 = this.settings.getLayers();
      BlockPos.MutableBlockPos $$5 = new BlockPos.MutableBlockPos();
      Heightmap $$6 = pChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
      Heightmap $$7 = pChunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

      for (int $$8 = 0; $$8 < Math.min(pChunk.getHeight(), $$4.size()); $$8++) {
         BlockState $$9 = $$4.get($$8);
         if ($$9 != null) {
            int $$10 = pChunk.getMinBuildHeight() + $$8;

            for (int $$11 = 0; $$11 < 16; $$11++) {
               for (int $$12 = 0; $$12 < 16; $$12++) {
                  pChunk.setBlockState($$5.set($$11, $$10, $$12), $$9, false);
                  $$6.update($$11, $$10, $$12, $$9);
                  $$7.update($$11, $$10, $$12, $$9);
               }
            }
         }
      }

      return CompletableFuture.completedFuture(pChunk);
   }

   @Override
   public int getBaseHeight(int pX, int pZ, Heightmap.Types pType, LevelHeightAccessor pLevel, RandomState pRandom) {
      List<BlockState> $$5 = this.settings.getLayers();

      for (int $$6 = Math.min($$5.size(), pLevel.getMaxBuildHeight()) - 1; $$6 >= 0; $$6--) {
         BlockState $$7 = $$5.get($$6);
         if ($$7 != null && pType.isOpaque().test($$7)) {
            return pLevel.getMinBuildHeight() + $$6 + 1;
         }
      }

      return pLevel.getMinBuildHeight();
   }

   @Override
   public NoiseColumn getBaseColumn(int pX, int pZ, LevelHeightAccessor pHeight, RandomState pRandom) {
      return new NoiseColumn(
         pHeight.getMinBuildHeight(),
         this.settings
            .getLayers()
            .stream()
            .limit((long)pHeight.getHeight())
            .map(p_204549_ -> p_204549_ == null ? Blocks.AIR.defaultBlockState() : p_204549_)
            .toArray(BlockState[]::new)
      );
   }

   @Override
   public void addDebugScreenInfo(List<String> pInfo, RandomState pRandom, BlockPos pPos) {
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
   }

   @Override
   public void spawnOriginalMobs(WorldGenRegion pLevel) {
   }

   @Override
   public int getMinY() {
      return 0;
   }

   @Override
   public int getGenDepth() {
      return 384;
   }

   @Override
   public int getSeaLevel() {
      return -63;
   }
}
