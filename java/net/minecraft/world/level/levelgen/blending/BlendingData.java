package net.minecraft.world.level.levelgen.blending;

import com.google.common.primitives.Doubles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction8;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

public class BlendingData {
   private static final double BLENDING_DENSITY_FACTOR = 0.1;
   protected static final int CELL_WIDTH = 4;
   protected static final int CELL_HEIGHT = 8;
   protected static final int CELL_RATIO = 2;
   private static final double SOLID_DENSITY = 1.0;
   private static final double AIR_DENSITY = -1.0;
   private static final int CELLS_PER_SECTION_Y = 2;
   private static final int QUARTS_PER_SECTION = QuartPos.fromBlock(16);
   private static final int CELL_HORIZONTAL_MAX_INDEX_INSIDE = QUARTS_PER_SECTION - 1;
   private static final int CELL_HORIZONTAL_MAX_INDEX_OUTSIDE = QUARTS_PER_SECTION;
   private static final int CELL_COLUMN_INSIDE_COUNT = 2 * CELL_HORIZONTAL_MAX_INDEX_INSIDE + 1;
   private static final int CELL_COLUMN_OUTSIDE_COUNT = 2 * CELL_HORIZONTAL_MAX_INDEX_OUTSIDE + 1;
   private static final int CELL_COLUMN_COUNT = CELL_COLUMN_INSIDE_COUNT + CELL_COLUMN_OUTSIDE_COUNT;
   private final LevelHeightAccessor areaWithOldGeneration;
   private static final List<Block> SURFACE_BLOCKS = List.of(
      Blocks.PODZOL,
      Blocks.GRAVEL,
      Blocks.GRASS_BLOCK,
      Blocks.STONE,
      Blocks.COARSE_DIRT,
      Blocks.SAND,
      Blocks.RED_SAND,
      Blocks.MYCELIUM,
      Blocks.SNOW_BLOCK,
      Blocks.TERRACOTTA,
      Blocks.DIRT
   );
   protected static final double NO_VALUE = Double.MAX_VALUE;
   private boolean hasCalculatedData;
   private final double[] heights;
   private final List<List<Holder<Biome>>> biomes;
   private final transient double[][] densities;
   private static final Codec<double[]> DOUBLE_ARRAY_CODEC = Codec.DOUBLE.listOf().xmap(Doubles::toArray, Doubles::asList);
   public static final Codec<BlendingData> CODEC = RecordCodecBuilder.create(
         p_327462_ -> p_327462_.group(
                  Codec.INT.fieldOf("min_section").forGetter(p_224767_ -> p_224767_.areaWithOldGeneration.getMinSection()),
                  Codec.INT.fieldOf("max_section").forGetter(p_224765_ -> p_224765_.areaWithOldGeneration.getMaxSection()),
                  DOUBLE_ARRAY_CODEC.lenientOptionalFieldOf("heights")
                     .forGetter(
                        p_224762_ -> DoubleStream.of(p_224762_.heights).anyMatch(p_224745_ -> p_224745_ != Double.MAX_VALUE)
                              ? Optional.of(p_224762_.heights)
                              : Optional.empty()
                     )
               )
               .apply(p_327462_, BlendingData::new)
      )
      .comapFlatMap(BlendingData::validateArraySize, Function.identity());

   private static DataResult<BlendingData> validateArraySize(BlendingData p_190321_) {
      return p_190321_.heights.length != CELL_COLUMN_COUNT
         ? DataResult.error(() -> "heights has to be of length " + CELL_COLUMN_COUNT)
         : DataResult.success(p_190321_);
   }

   private BlendingData(int p_224740_, int p_224741_, Optional<double[]> p_224742_) {
      this.heights = p_224742_.orElse(Util.make(new double[CELL_COLUMN_COUNT], p_224756_ -> Arrays.fill(p_224756_, Double.MAX_VALUE)));
      this.densities = new double[CELL_COLUMN_COUNT][];
      ObjectArrayList<List<Holder<Biome>>> $$3 = new ObjectArrayList(CELL_COLUMN_COUNT);
      $$3.size(CELL_COLUMN_COUNT);
      this.biomes = $$3;
      int $$4 = SectionPos.sectionToBlockCoord(p_224740_);
      int $$5 = SectionPos.sectionToBlockCoord(p_224741_) - $$4;
      this.areaWithOldGeneration = LevelHeightAccessor.create($$4, $$5);
   }

   @Nullable
   public static BlendingData getOrUpdateBlendingData(WorldGenRegion pRegion, int pChunkX, int pChunkZ) {
      ChunkAccess $$3 = pRegion.getChunk(pChunkX, pChunkZ);
      BlendingData $$4 = $$3.getBlendingData();
      if ($$4 != null && !$$3.getHighestGeneratedStatus().isBefore(ChunkStatus.BIOMES)) {
         $$4.calculateData($$3, sideByGenerationAge(pRegion, pChunkX, pChunkZ, false));
         return $$4;
      } else {
         return null;
      }
   }

   public static Set<Direction8> sideByGenerationAge(WorldGenLevel pLevel, int pChunkX, int pChunkZ, boolean pOldNoiseGeneration) {
      Set<Direction8> $$4 = EnumSet.noneOf(Direction8.class);

      for (Direction8 $$5 : Direction8.values()) {
         int $$6 = pChunkX + $$5.getStepX();
         int $$7 = pChunkZ + $$5.getStepZ();
         if (pLevel.getChunk($$6, $$7).isOldNoiseGeneration() == pOldNoiseGeneration) {
            $$4.add($$5);
         }
      }

      return $$4;
   }

   private void calculateData(ChunkAccess pChunk, Set<Direction8> pDirections) {
      if (!this.hasCalculatedData) {
         if (pDirections.contains(Direction8.NORTH) || pDirections.contains(Direction8.WEST) || pDirections.contains(Direction8.NORTH_WEST)) {
            this.addValuesForColumn(getInsideIndex(0, 0), pChunk, 0, 0);
         }

         if (pDirections.contains(Direction8.NORTH)) {
            for (int $$2 = 1; $$2 < QUARTS_PER_SECTION; $$2++) {
               this.addValuesForColumn(getInsideIndex($$2, 0), pChunk, 4 * $$2, 0);
            }
         }

         if (pDirections.contains(Direction8.WEST)) {
            for (int $$3 = 1; $$3 < QUARTS_PER_SECTION; $$3++) {
               this.addValuesForColumn(getInsideIndex(0, $$3), pChunk, 0, 4 * $$3);
            }
         }

         if (pDirections.contains(Direction8.EAST)) {
            for (int $$4 = 1; $$4 < QUARTS_PER_SECTION; $$4++) {
               this.addValuesForColumn(getOutsideIndex(CELL_HORIZONTAL_MAX_INDEX_OUTSIDE, $$4), pChunk, 15, 4 * $$4);
            }
         }

         if (pDirections.contains(Direction8.SOUTH)) {
            for (int $$5 = 0; $$5 < QUARTS_PER_SECTION; $$5++) {
               this.addValuesForColumn(getOutsideIndex($$5, CELL_HORIZONTAL_MAX_INDEX_OUTSIDE), pChunk, 4 * $$5, 15);
            }
         }

         if (pDirections.contains(Direction8.EAST) && pDirections.contains(Direction8.NORTH_EAST)) {
            this.addValuesForColumn(getOutsideIndex(CELL_HORIZONTAL_MAX_INDEX_OUTSIDE, 0), pChunk, 15, 0);
         }

         if (pDirections.contains(Direction8.EAST) && pDirections.contains(Direction8.SOUTH) && pDirections.contains(Direction8.SOUTH_EAST)) {
            this.addValuesForColumn(getOutsideIndex(CELL_HORIZONTAL_MAX_INDEX_OUTSIDE, CELL_HORIZONTAL_MAX_INDEX_OUTSIDE), pChunk, 15, 15);
         }

         this.hasCalculatedData = true;
      }
   }

   private void addValuesForColumn(int pIndex, ChunkAccess pChunk, int pX, int pZ) {
      if (this.heights[pIndex] == Double.MAX_VALUE) {
         this.heights[pIndex] = (double)this.getHeightAtXZ(pChunk, pX, pZ);
      }

      this.densities[pIndex] = this.getDensityColumn(pChunk, pX, pZ, Mth.floor(this.heights[pIndex]));
      this.biomes.set(pIndex, this.getBiomeColumn(pChunk, pX, pZ));
   }

   private int getHeightAtXZ(ChunkAccess pChunk, int pX, int pZ) {
      int $$3;
      if (pChunk.hasPrimedHeightmap(Heightmap.Types.WORLD_SURFACE_WG)) {
         $$3 = Math.min(pChunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, pX, pZ) + 1, this.areaWithOldGeneration.getMaxBuildHeight());
      } else {
         $$3 = this.areaWithOldGeneration.getMaxBuildHeight();
      }

      int $$5 = this.areaWithOldGeneration.getMinBuildHeight();
      BlockPos.MutableBlockPos $$6 = new BlockPos.MutableBlockPos(pX, $$3, pZ);

      while ($$6.getY() > $$5) {
         $$6.move(Direction.DOWN);
         if (SURFACE_BLOCKS.contains(pChunk.getBlockState($$6).getBlock())) {
            return $$6.getY();
         }
      }

      return $$5;
   }

   private static double read1(ChunkAccess pChunk, BlockPos.MutableBlockPos pPos) {
      return isGround(pChunk, pPos.move(Direction.DOWN)) ? 1.0 : -1.0;
   }

   private static double read7(ChunkAccess pChunk, BlockPos.MutableBlockPos pPos) {
      double $$2 = 0.0;

      for (int $$3 = 0; $$3 < 7; $$3++) {
         $$2 += read1(pChunk, pPos);
      }

      return $$2;
   }

   private double[] getDensityColumn(ChunkAccess pChunk, int pX, int pZ, int pHeight) {
      double[] $$4 = new double[this.cellCountPerColumn()];
      Arrays.fill($$4, -1.0);
      BlockPos.MutableBlockPos $$5 = new BlockPos.MutableBlockPos(pX, this.areaWithOldGeneration.getMaxBuildHeight(), pZ);
      double $$6 = read7(pChunk, $$5);

      for (int $$7 = $$4.length - 2; $$7 >= 0; $$7--) {
         double $$8 = read1(pChunk, $$5);
         double $$9 = read7(pChunk, $$5);
         $$4[$$7] = ($$6 + $$8 + $$9) / 15.0;
         $$6 = $$9;
      }

      int $$10 = this.getCellYIndex(Mth.floorDiv(pHeight, 8));
      if ($$10 >= 0 && $$10 < $$4.length - 1) {
         double $$11 = ((double)pHeight + 0.5) % 8.0 / 8.0;
         double $$12 = (1.0 - $$11) / $$11;
         double $$13 = Math.max($$12, 1.0) * 0.25;
         $$4[$$10 + 1] = -$$12 / $$13;
         $$4[$$10] = 1.0 / $$13;
      }

      return $$4;
   }

   private List<Holder<Biome>> getBiomeColumn(ChunkAccess pChunk, int pX, int pZ) {
      ObjectArrayList<Holder<Biome>> $$3 = new ObjectArrayList(this.quartCountPerColumn());
      $$3.size(this.quartCountPerColumn());

      for (int $$4 = 0; $$4 < $$3.size(); $$4++) {
         int $$5 = $$4 + QuartPos.fromBlock(this.areaWithOldGeneration.getMinBuildHeight());
         $$3.set($$4, pChunk.getNoiseBiome(QuartPos.fromBlock(pX), $$5, QuartPos.fromBlock(pZ)));
      }

      return $$3;
   }

   private static boolean isGround(ChunkAccess pChunk, BlockPos pPos) {
      BlockState $$2 = pChunk.getBlockState(pPos);
      if ($$2.isAir()) {
         return false;
      } else if ($$2.is(BlockTags.LEAVES)) {
         return false;
      } else if ($$2.is(BlockTags.LOGS)) {
         return false;
      } else {
         return $$2.is(Blocks.BROWN_MUSHROOM_BLOCK) || $$2.is(Blocks.RED_MUSHROOM_BLOCK) ? false : !$$2.getCollisionShape(pChunk, pPos).isEmpty();
      }
   }

   protected double getHeight(int pX, int pY, int pZ) {
      if (pX == CELL_HORIZONTAL_MAX_INDEX_OUTSIDE || pZ == CELL_HORIZONTAL_MAX_INDEX_OUTSIDE) {
         return this.heights[getOutsideIndex(pX, pZ)];
      } else {
         return pX != 0 && pZ != 0 ? Double.MAX_VALUE : this.heights[getInsideIndex(pX, pZ)];
      }
   }

   private double getDensity(@Nullable double[] pHeights, int pY) {
      if (pHeights == null) {
         return Double.MAX_VALUE;
      } else {
         int $$2 = this.getCellYIndex(pY);
         return $$2 >= 0 && $$2 < pHeights.length ? pHeights[$$2] * 0.1 : Double.MAX_VALUE;
      }
   }

   protected double getDensity(int pX, int pY, int pZ) {
      if (pY == this.getMinY()) {
         return 0.1;
      } else if (pX == CELL_HORIZONTAL_MAX_INDEX_OUTSIDE || pZ == CELL_HORIZONTAL_MAX_INDEX_OUTSIDE) {
         return this.getDensity(this.densities[getOutsideIndex(pX, pZ)], pY);
      } else {
         return pX != 0 && pZ != 0 ? Double.MAX_VALUE : this.getDensity(this.densities[getInsideIndex(pX, pZ)], pY);
      }
   }

   protected void iterateBiomes(int pX, int pY, int pZ, BlendingData.BiomeConsumer pConsumer) {
      if (pY >= QuartPos.fromBlock(this.areaWithOldGeneration.getMinBuildHeight()) && pY < QuartPos.fromBlock(this.areaWithOldGeneration.getMaxBuildHeight())) {
         int $$4 = pY - QuartPos.fromBlock(this.areaWithOldGeneration.getMinBuildHeight());

         for (int $$5 = 0; $$5 < this.biomes.size(); $$5++) {
            if (this.biomes.get($$5) != null) {
               Holder<Biome> $$6 = this.biomes.get($$5).get($$4);
               if ($$6 != null) {
                  pConsumer.consume(pX + getX($$5), pZ + getZ($$5), $$6);
               }
            }
         }
      }
   }

   protected void iterateHeights(int pX, int pZ, BlendingData.HeightConsumer pConsumer) {
      for (int $$3 = 0; $$3 < this.heights.length; $$3++) {
         double $$4 = this.heights[$$3];
         if ($$4 != Double.MAX_VALUE) {
            pConsumer.consume(pX + getX($$3), pZ + getZ($$3), $$4);
         }
      }
   }

   protected void iterateDensities(int pX, int pZ, int pMinY, int pMaxY, BlendingData.DensityConsumer pConsumer) {
      int $$5 = this.getColumnMinY();
      int $$6 = Math.max(0, pMinY - $$5);
      int $$7 = Math.min(this.cellCountPerColumn(), pMaxY - $$5);

      for (int $$8 = 0; $$8 < this.densities.length; $$8++) {
         double[] $$9 = this.densities[$$8];
         if ($$9 != null) {
            int $$10 = pX + getX($$8);
            int $$11 = pZ + getZ($$8);

            for (int $$12 = $$6; $$12 < $$7; $$12++) {
               pConsumer.consume($$10, $$12 + $$5, $$11, $$9[$$12] * 0.1);
            }
         }
      }
   }

   private int cellCountPerColumn() {
      return this.areaWithOldGeneration.getSectionsCount() * 2;
   }

   private int quartCountPerColumn() {
      return QuartPos.fromSection(this.areaWithOldGeneration.getSectionsCount());
   }

   private int getColumnMinY() {
      return this.getMinY() + 1;
   }

   private int getMinY() {
      return this.areaWithOldGeneration.getMinSection() * 2;
   }

   private int getCellYIndex(int pY) {
      return pY - this.getColumnMinY();
   }

   private static int getInsideIndex(int pX, int pZ) {
      return CELL_HORIZONTAL_MAX_INDEX_INSIDE - pX + pZ;
   }

   private static int getOutsideIndex(int pX, int pZ) {
      return CELL_COLUMN_INSIDE_COUNT + pX + CELL_HORIZONTAL_MAX_INDEX_OUTSIDE - pZ;
   }

   private static int getX(int pIndex) {
      if (pIndex < CELL_COLUMN_INSIDE_COUNT) {
         return zeroIfNegative(CELL_HORIZONTAL_MAX_INDEX_INSIDE - pIndex);
      } else {
         int $$1 = pIndex - CELL_COLUMN_INSIDE_COUNT;
         return CELL_HORIZONTAL_MAX_INDEX_OUTSIDE - zeroIfNegative(CELL_HORIZONTAL_MAX_INDEX_OUTSIDE - $$1);
      }
   }

   private static int getZ(int pIndex) {
      if (pIndex < CELL_COLUMN_INSIDE_COUNT) {
         return zeroIfNegative(pIndex - CELL_HORIZONTAL_MAX_INDEX_INSIDE);
      } else {
         int $$1 = pIndex - CELL_COLUMN_INSIDE_COUNT;
         return CELL_HORIZONTAL_MAX_INDEX_OUTSIDE - zeroIfNegative($$1 - CELL_HORIZONTAL_MAX_INDEX_OUTSIDE);
      }
   }

   private static int zeroIfNegative(int pValue) {
      return pValue & ~(pValue >> 31);
   }

   public LevelHeightAccessor getAreaWithOldGeneration() {
      return this.areaWithOldGeneration;
   }

   protected interface BiomeConsumer {
      void consume(int pX, int pZ, Holder<Biome> pBiome);
   }

   protected interface DensityConsumer {
      void consume(int pX, int pY, int pZ, double pDensity);
   }

   protected interface HeightConsumer {
      void consume(int pX, int pZ, double pHeight);
   }
}
