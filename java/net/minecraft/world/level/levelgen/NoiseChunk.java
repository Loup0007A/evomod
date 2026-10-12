package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ColumnPos;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.material.MaterialRuleList;

public class NoiseChunk implements DensityFunction.ContextProvider, DensityFunction.FunctionContext {
   private final NoiseSettings noiseSettings;
   final int cellCountXZ;
   final int cellCountY;
   final int cellNoiseMinY;
   private final int firstCellX;
   private final int firstCellZ;
   final int firstNoiseX;
   final int firstNoiseZ;
   final List<NoiseChunk.NoiseInterpolator> interpolators;
   final List<NoiseChunk.CacheAllInCell> cellCaches;
   private final Map<DensityFunction, DensityFunction> wrapped = new HashMap<>();
   private final Long2IntMap preliminarySurfaceLevel = new Long2IntOpenHashMap();
   private final Aquifer aquifer;
   private final DensityFunction initialDensityNoJaggedness;
   private final NoiseChunk.BlockStateFiller blockStateRule;
   private final Blender blender;
   private final NoiseChunk.FlatCache blendAlpha;
   private final NoiseChunk.FlatCache blendOffset;
   private final DensityFunctions.BeardifierOrMarker beardifier;
   private long lastBlendingDataPos = ChunkPos.INVALID_CHUNK_POS;
   private Blender.BlendingOutput lastBlendingOutput = new Blender.BlendingOutput(1.0, 0.0);
   final int noiseSizeXZ;
   final int cellWidth;
   final int cellHeight;
   boolean interpolating;
   boolean fillingCell;
   private int cellStartBlockX;
   int cellStartBlockY;
   private int cellStartBlockZ;
   int inCellX;
   int inCellY;
   int inCellZ;
   long interpolationCounter;
   long arrayInterpolationCounter;
   int arrayIndex;
   private final DensityFunction.ContextProvider sliceFillingContextProvider = new DensityFunction.ContextProvider() {
      @Override
      public DensityFunction.FunctionContext forIndex(int p_209253_) {
         NoiseChunk.this.cellStartBlockY = (p_209253_ + NoiseChunk.this.cellNoiseMinY) * NoiseChunk.this.cellHeight;
         NoiseChunk.this.interpolationCounter++;
         NoiseChunk.this.inCellY = 0;
         NoiseChunk.this.arrayIndex = p_209253_;
         return NoiseChunk.this;
      }

      @Override
      public void fillAllDirectly(double[] p_209255_, DensityFunction p_209256_) {
         for (int $$2 = 0; $$2 < NoiseChunk.this.cellCountY + 1; $$2++) {
            NoiseChunk.this.cellStartBlockY = ($$2 + NoiseChunk.this.cellNoiseMinY) * NoiseChunk.this.cellHeight;
            NoiseChunk.this.interpolationCounter++;
            NoiseChunk.this.inCellY = 0;
            NoiseChunk.this.arrayIndex = $$2;
            p_209255_[$$2] = p_209256_.compute(NoiseChunk.this);
         }
      }
   };

   public static NoiseChunk forChunk(
      ChunkAccess pChunk,
      RandomState pState,
      DensityFunctions.BeardifierOrMarker pBeardifierOrMarker,
      NoiseGeneratorSettings pNoiseGeneratorSettings,
      Aquifer.FluidPicker pFluidPicke,
      Blender pBlender
   ) {
      NoiseSettings $$6 = pNoiseGeneratorSettings.noiseSettings().clampToHeightAccessor(pChunk);
      ChunkPos $$7 = pChunk.getPos();
      int $$8 = 16 / $$6.getCellWidth();
      return new NoiseChunk($$8, pState, $$7.getMinBlockX(), $$7.getMinBlockZ(), $$6, pBeardifierOrMarker, pNoiseGeneratorSettings, pFluidPicke, pBlender);
   }

   public NoiseChunk(
      int pCellCountXZ,
      RandomState pRandom,
      int pFirstNoiseX,
      int pFirstNoiseZ,
      NoiseSettings pNoiseSettings,
      DensityFunctions.BeardifierOrMarker pBeardifier,
      NoiseGeneratorSettings pNoiseGeneratorSettings,
      Aquifer.FluidPicker pFluidPicker,
      Blender pBlendifier
   ) {
      this.noiseSettings = pNoiseSettings;
      this.cellWidth = pNoiseSettings.getCellWidth();
      this.cellHeight = pNoiseSettings.getCellHeight();
      this.cellCountXZ = pCellCountXZ;
      this.cellCountY = Mth.floorDiv(pNoiseSettings.height(), this.cellHeight);
      this.cellNoiseMinY = Mth.floorDiv(pNoiseSettings.minY(), this.cellHeight);
      this.firstCellX = Math.floorDiv(pFirstNoiseX, this.cellWidth);
      this.firstCellZ = Math.floorDiv(pFirstNoiseZ, this.cellWidth);
      this.interpolators = Lists.newArrayList();
      this.cellCaches = Lists.newArrayList();
      this.firstNoiseX = QuartPos.fromBlock(pFirstNoiseX);
      this.firstNoiseZ = QuartPos.fromBlock(pFirstNoiseZ);
      this.noiseSizeXZ = QuartPos.fromBlock(pCellCountXZ * this.cellWidth);
      this.blender = pBlendifier;
      this.beardifier = pBeardifier;
      this.blendAlpha = new NoiseChunk.FlatCache(new NoiseChunk.BlendAlpha(), false);
      this.blendOffset = new NoiseChunk.FlatCache(new NoiseChunk.BlendOffset(), false);

      for (int $$9 = 0; $$9 <= this.noiseSizeXZ; $$9++) {
         int $$10 = this.firstNoiseX + $$9;
         int $$11 = QuartPos.toBlock($$10);

         for (int $$12 = 0; $$12 <= this.noiseSizeXZ; $$12++) {
            int $$13 = this.firstNoiseZ + $$12;
            int $$14 = QuartPos.toBlock($$13);
            Blender.BlendingOutput $$15 = pBlendifier.blendOffsetAndFactor($$11, $$14);
            this.blendAlpha.values[$$9][$$12] = $$15.alpha();
            this.blendOffset.values[$$9][$$12] = $$15.blendingOffset();
         }
      }

      NoiseRouter $$16 = pRandom.router();
      NoiseRouter $$17 = $$16.mapAll(this::wrap);
      if (!pNoiseGeneratorSettings.isAquifersEnabled()) {
         this.aquifer = Aquifer.createDisabled(pFluidPicker);
      } else {
         int $$18 = SectionPos.blockToSectionCoord(pFirstNoiseX);
         int $$19 = SectionPos.blockToSectionCoord(pFirstNoiseZ);
         this.aquifer = Aquifer.create(
            this, new ChunkPos($$18, $$19), $$17, pRandom.aquiferRandom(), pNoiseSettings.minY(), pNoiseSettings.height(), pFluidPicker
         );
      }

      Builder<NoiseChunk.BlockStateFiller> $$20 = ImmutableList.builder();
      DensityFunction $$21 = DensityFunctions.cacheAllInCell(DensityFunctions.add($$17.finalDensity(), DensityFunctions.BeardifierMarker.INSTANCE))
         .mapAll(this::wrap);
      $$20.add((NoiseChunk.BlockStateFiller)p_209217_ -> this.aquifer.computeSubstance(p_209217_, $$21.compute(p_209217_)));
      if (pNoiseGeneratorSettings.oreVeinsEnabled()) {
         $$20.add(OreVeinifier.create($$17.veinToggle(), $$17.veinRidged(), $$17.veinGap(), pRandom.oreRandom()));
      }

      this.blockStateRule = new MaterialRuleList($$20.build());
      this.initialDensityNoJaggedness = $$17.initialDensityWithoutJaggedness();
   }

   protected Climate.Sampler cachedClimateSampler(NoiseRouter pNoiseRouter, List<Climate.ParameterPoint> pPoints) {
      return new Climate.Sampler(
         pNoiseRouter.temperature().mapAll(this::wrap),
         pNoiseRouter.vegetation().mapAll(this::wrap),
         pNoiseRouter.continents().mapAll(this::wrap),
         pNoiseRouter.erosion().mapAll(this::wrap),
         pNoiseRouter.depth().mapAll(this::wrap),
         pNoiseRouter.ridges().mapAll(this::wrap),
         pPoints
      );
   }

   @Nullable
   protected BlockState getInterpolatedState() {
      return this.blockStateRule.calculate(this);
   }

   @Override
   public int blockX() {
      return this.cellStartBlockX + this.inCellX;
   }

   @Override
   public int blockY() {
      return this.cellStartBlockY + this.inCellY;
   }

   @Override
   public int blockZ() {
      return this.cellStartBlockZ + this.inCellZ;
   }

   public int preliminarySurfaceLevel(int pX, int pZ) {
      int $$2 = QuartPos.toBlock(QuartPos.fromBlock(pX));
      int $$3 = QuartPos.toBlock(QuartPos.fromBlock(pZ));
      return this.preliminarySurfaceLevel.computeIfAbsent(ColumnPos.asLong($$2, $$3), this::computePreliminarySurfaceLevel);
   }

   private int computePreliminarySurfaceLevel(long p_198250_) {
      int $$1 = ColumnPos.getX(p_198250_);
      int $$2 = ColumnPos.getZ(p_198250_);
      int $$3 = this.noiseSettings.minY();

      for (int $$4 = $$3 + this.noiseSettings.height(); $$4 >= $$3; $$4 -= this.cellHeight) {
         if (this.initialDensityNoJaggedness.compute(new DensityFunction.SinglePointContext($$1, $$4, $$2)) > 0.390625) {
            return $$4;
         }
      }

      return Integer.MAX_VALUE;
   }

   @Override
   public Blender getBlender() {
      return this.blender;
   }

   private void fillSlice(boolean pIsSlice0, int pStart) {
      this.cellStartBlockX = pStart * this.cellWidth;
      this.inCellX = 0;

      for (int $$2 = 0; $$2 < this.cellCountXZ + 1; $$2++) {
         int $$3 = this.firstCellZ + $$2;
         this.cellStartBlockZ = $$3 * this.cellWidth;
         this.inCellZ = 0;
         this.arrayInterpolationCounter++;

         for (NoiseChunk.NoiseInterpolator $$4 : this.interpolators) {
            double[] $$5 = (pIsSlice0 ? $$4.slice0 : $$4.slice1)[$$2];
            $$4.fillArray($$5, this.sliceFillingContextProvider);
         }
      }

      this.arrayInterpolationCounter++;
   }

   public void initializeForFirstCellX() {
      if (this.interpolating) {
         throw new IllegalStateException("Staring interpolation twice");
      } else {
         this.interpolating = true;
         this.interpolationCounter = 0L;
         this.fillSlice(true, this.firstCellX);
      }
   }

   public void advanceCellX(int pIncrement) {
      this.fillSlice(false, this.firstCellX + pIncrement + 1);
      this.cellStartBlockX = (this.firstCellX + pIncrement) * this.cellWidth;
   }

   public NoiseChunk forIndex(int pArrayIndex) {
      int $$1 = Math.floorMod(pArrayIndex, this.cellWidth);
      int $$2 = Math.floorDiv(pArrayIndex, this.cellWidth);
      int $$3 = Math.floorMod($$2, this.cellWidth);
      int $$4 = this.cellHeight - 1 - Math.floorDiv($$2, this.cellWidth);
      this.inCellX = $$3;
      this.inCellY = $$4;
      this.inCellZ = $$1;
      this.arrayIndex = pArrayIndex;
      return this;
   }

   @Override
   public void fillAllDirectly(double[] pValues, DensityFunction pFunction) {
      this.arrayIndex = 0;

      for (int $$2 = this.cellHeight - 1; $$2 >= 0; $$2--) {
         this.inCellY = $$2;

         for (int $$3 = 0; $$3 < this.cellWidth; $$3++) {
            this.inCellX = $$3;

            for (int $$4 = 0; $$4 < this.cellWidth; $$4++) {
               this.inCellZ = $$4;
               pValues[this.arrayIndex++] = pFunction.compute(this);
            }
         }
      }
   }

   public void selectCellYZ(int pY, int pZ) {
      this.interpolators.forEach(p_209205_ -> p_209205_.selectCellYZ(pY, pZ));
      this.fillingCell = true;
      this.cellStartBlockY = (pY + this.cellNoiseMinY) * this.cellHeight;
      this.cellStartBlockZ = (this.firstCellZ + pZ) * this.cellWidth;
      this.arrayInterpolationCounter++;

      for (NoiseChunk.CacheAllInCell $$2 : this.cellCaches) {
         $$2.noiseFiller.fillArray($$2.values, this);
      }

      this.arrayInterpolationCounter++;
      this.fillingCell = false;
   }

   public void updateForY(int pCellEndBlockY, double pY) {
      this.inCellY = pCellEndBlockY - this.cellStartBlockY;
      this.interpolators.forEach(p_209238_ -> p_209238_.updateForY(pY));
   }

   public void updateForX(int pCellEndBlockX, double pX) {
      this.inCellX = pCellEndBlockX - this.cellStartBlockX;
      this.interpolators.forEach(p_209229_ -> p_209229_.updateForX(pX));
   }

   public void updateForZ(int pCellEndBlockZ, double pZ) {
      this.inCellZ = pCellEndBlockZ - this.cellStartBlockZ;
      this.interpolationCounter++;
      this.interpolators.forEach(p_209188_ -> p_209188_.updateForZ(pZ));
   }

   public void stopInterpolation() {
      if (!this.interpolating) {
         throw new IllegalStateException("Staring interpolation twice");
      } else {
         this.interpolating = false;
      }
   }

   public void swapSlices() {
      this.interpolators.forEach(NoiseChunk.NoiseInterpolator::swapSlices);
   }

   public Aquifer aquifer() {
      return this.aquifer;
   }

   protected int cellWidth() {
      return this.cellWidth;
   }

   protected int cellHeight() {
      return this.cellHeight;
   }

   Blender.BlendingOutput getOrComputeBlendingOutput(int pChunkX, int pChunkZ) {
      long $$2 = ChunkPos.asLong(pChunkX, pChunkZ);
      if (this.lastBlendingDataPos == $$2) {
         return this.lastBlendingOutput;
      } else {
         this.lastBlendingDataPos = $$2;
         Blender.BlendingOutput $$3 = this.blender.blendOffsetAndFactor(pChunkX, pChunkZ);
         this.lastBlendingOutput = $$3;
         return $$3;
      }
   }

   protected DensityFunction wrap(DensityFunction p_209214_) {
      return this.wrapped.computeIfAbsent(p_209214_, this::wrapNew);
   }

   private DensityFunction wrapNew(DensityFunction p_209234_) {
      if (p_209234_ instanceof DensityFunctions.Marker $$1) {
         return (DensityFunction)(switch ($$1.type()) {
            case Interpolated -> new NoiseChunk.NoiseInterpolator($$1.wrapped());
            case FlatCache -> new NoiseChunk.FlatCache($$1.wrapped(), true);
            case Cache2D -> new NoiseChunk.Cache2D($$1.wrapped());
            case CacheOnce -> new NoiseChunk.CacheOnce($$1.wrapped());
            case CacheAllInCell -> new NoiseChunk.CacheAllInCell($$1.wrapped());
         });
      } else {
         if (this.blender != Blender.empty()) {
            if (p_209234_ == DensityFunctions.BlendAlpha.INSTANCE) {
               return this.blendAlpha;
            }

            if (p_209234_ == DensityFunctions.BlendOffset.INSTANCE) {
               return this.blendOffset;
            }
         }

         if (p_209234_ == DensityFunctions.BeardifierMarker.INSTANCE) {
            return this.beardifier;
         } else {
            return p_209234_ instanceof DensityFunctions.HolderHolder $$2 ? $$2.function().value() : p_209234_;
         }
      }
   }

   class BlendAlpha implements NoiseChunk.NoiseChunkDensityFunction {
      @Override
      public DensityFunction wrapped() {
         return DensityFunctions.BlendAlpha.INSTANCE;
      }

      @Override
      public DensityFunction mapAll(DensityFunction.Visitor p_224365_) {
         return this.wrapped().mapAll(p_224365_);
      }

      @Override
      public double compute(DensityFunction.FunctionContext p_209264_) {
         return NoiseChunk.this.getOrComputeBlendingOutput(p_209264_.blockX(), p_209264_.blockZ()).alpha();
      }

      @Override
      public void fillArray(double[] p_209266_, DensityFunction.ContextProvider p_209267_) {
         p_209267_.fillAllDirectly(p_209266_, this);
      }

      @Override
      public double minValue() {
         return 0.0;
      }

      @Override
      public double maxValue() {
         return 1.0;
      }

      @Override
      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return DensityFunctions.BlendAlpha.CODEC;
      }
   }

   class BlendOffset implements NoiseChunk.NoiseChunkDensityFunction {
      @Override
      public DensityFunction wrapped() {
         return DensityFunctions.BlendOffset.INSTANCE;
      }

      @Override
      public DensityFunction mapAll(DensityFunction.Visitor p_224368_) {
         return this.wrapped().mapAll(p_224368_);
      }

      @Override
      public double compute(DensityFunction.FunctionContext p_209276_) {
         return NoiseChunk.this.getOrComputeBlendingOutput(p_209276_.blockX(), p_209276_.blockZ()).blendingOffset();
      }

      @Override
      public void fillArray(double[] p_209278_, DensityFunction.ContextProvider p_209279_) {
         p_209279_.fillAllDirectly(p_209278_, this);
      }

      @Override
      public double minValue() {
         return Double.NEGATIVE_INFINITY;
      }

      @Override
      public double maxValue() {
         return Double.POSITIVE_INFINITY;
      }

      @Override
      public KeyDispatchDataCodec<? extends DensityFunction> codec() {
         return DensityFunctions.BlendOffset.CODEC;
      }
   }

   @FunctionalInterface
   public interface BlockStateFiller {
      @Nullable
      BlockState calculate(DensityFunction.FunctionContext pContext);
   }

   static class Cache2D implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      private final DensityFunction function;
      private long lastPos2D = ChunkPos.INVALID_CHUNK_POS;
      private double lastValue;

      Cache2D(DensityFunction pFunction) {
         this.function = pFunction;
      }

      @Override
      public double compute(DensityFunction.FunctionContext pContext) {
         int $$1 = pContext.blockX();
         int $$2 = pContext.blockZ();
         long $$3 = ChunkPos.asLong($$1, $$2);
         if (this.lastPos2D == $$3) {
            return this.lastValue;
         } else {
            this.lastPos2D = $$3;
            double $$4 = this.function.compute(pContext);
            this.lastValue = $$4;
            return $$4;
         }
      }

      @Override
      public void fillArray(double[] pArray, DensityFunction.ContextProvider pContextProvider) {
         this.function.fillArray(pArray, pContextProvider);
      }

      @Override
      public DensityFunction wrapped() {
         return this.function;
      }

      @Override
      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.Cache2D;
      }
   }

   class CacheAllInCell implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      final DensityFunction noiseFiller;
      final double[] values;

      CacheAllInCell(final DensityFunction pNoiseFilter) {
         this.noiseFiller = pNoiseFilter;
         this.values = new double[NoiseChunk.this.cellWidth * NoiseChunk.this.cellWidth * NoiseChunk.this.cellHeight];
         NoiseChunk.this.cellCaches.add(this);
      }

      @Override
      public double compute(DensityFunction.FunctionContext pContext) {
         if (pContext != NoiseChunk.this) {
            return this.noiseFiller.compute(pContext);
         } else if (!NoiseChunk.this.interpolating) {
            throw new IllegalStateException("Trying to sample interpolator outside the interpolation loop");
         } else {
            int $$1 = NoiseChunk.this.inCellX;
            int $$2 = NoiseChunk.this.inCellY;
            int $$3 = NoiseChunk.this.inCellZ;
            return $$1 >= 0 && $$2 >= 0 && $$3 >= 0 && $$1 < NoiseChunk.this.cellWidth && $$2 < NoiseChunk.this.cellHeight && $$3 < NoiseChunk.this.cellWidth
               ? this.values[((NoiseChunk.this.cellHeight - 1 - $$2) * NoiseChunk.this.cellWidth + $$1) * NoiseChunk.this.cellWidth + $$3]
               : this.noiseFiller.compute(pContext);
         }
      }

      @Override
      public void fillArray(double[] pArray, DensityFunction.ContextProvider pContextProvider) {
         pContextProvider.fillAllDirectly(pArray, this);
      }

      @Override
      public DensityFunction wrapped() {
         return this.noiseFiller;
      }

      @Override
      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.CacheAllInCell;
      }
   }

   class CacheOnce implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      private final DensityFunction function;
      private long lastCounter;
      private long lastArrayCounter;
      private double lastValue;
      @Nullable
      private double[] lastArray;

      CacheOnce(final DensityFunction pFunction) {
         this.function = pFunction;
      }

      @Override
      public double compute(DensityFunction.FunctionContext pContext) {
         if (pContext != NoiseChunk.this) {
            return this.function.compute(pContext);
         } else if (this.lastArray != null && this.lastArrayCounter == NoiseChunk.this.arrayInterpolationCounter) {
            return this.lastArray[NoiseChunk.this.arrayIndex];
         } else if (this.lastCounter == NoiseChunk.this.interpolationCounter) {
            return this.lastValue;
         } else {
            this.lastCounter = NoiseChunk.this.interpolationCounter;
            double $$1 = this.function.compute(pContext);
            this.lastValue = $$1;
            return $$1;
         }
      }

      @Override
      public void fillArray(double[] pArray, DensityFunction.ContextProvider pContextProvider) {
         if (this.lastArray != null && this.lastArrayCounter == NoiseChunk.this.arrayInterpolationCounter) {
            System.arraycopy(this.lastArray, 0, pArray, 0, pArray.length);
         } else {
            this.wrapped().fillArray(pArray, pContextProvider);
            if (this.lastArray != null && this.lastArray.length == pArray.length) {
               System.arraycopy(pArray, 0, this.lastArray, 0, pArray.length);
            } else {
               this.lastArray = (double[])pArray.clone();
            }

            this.lastArrayCounter = NoiseChunk.this.arrayInterpolationCounter;
         }
      }

      @Override
      public DensityFunction wrapped() {
         return this.function;
      }

      @Override
      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.CacheOnce;
      }
   }

   class FlatCache implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      private final DensityFunction noiseFiller;
      final double[][] values;

      FlatCache(final DensityFunction pNoiseFiller, final boolean pComputeValues) {
         this.noiseFiller = pNoiseFiller;
         this.values = new double[NoiseChunk.this.noiseSizeXZ + 1][NoiseChunk.this.noiseSizeXZ + 1];
         if (pComputeValues) {
            for (int $$2 = 0; $$2 <= NoiseChunk.this.noiseSizeXZ; $$2++) {
               int $$3 = NoiseChunk.this.firstNoiseX + $$2;
               int $$4 = QuartPos.toBlock($$3);

               for (int $$5 = 0; $$5 <= NoiseChunk.this.noiseSizeXZ; $$5++) {
                  int $$6 = NoiseChunk.this.firstNoiseZ + $$5;
                  int $$7 = QuartPos.toBlock($$6);
                  this.values[$$2][$$5] = pNoiseFiller.compute(new DensityFunction.SinglePointContext($$4, 0, $$7));
               }
            }
         }
      }

      @Override
      public double compute(DensityFunction.FunctionContext pContext) {
         int $$1 = QuartPos.fromBlock(pContext.blockX());
         int $$2 = QuartPos.fromBlock(pContext.blockZ());
         int $$3 = $$1 - NoiseChunk.this.firstNoiseX;
         int $$4 = $$2 - NoiseChunk.this.firstNoiseZ;
         int $$5 = this.values.length;
         return $$3 >= 0 && $$4 >= 0 && $$3 < $$5 && $$4 < $$5 ? this.values[$$3][$$4] : this.noiseFiller.compute(pContext);
      }

      @Override
      public void fillArray(double[] pArray, DensityFunction.ContextProvider pContextProvider) {
         pContextProvider.fillAllDirectly(pArray, this);
      }

      @Override
      public DensityFunction wrapped() {
         return this.noiseFiller;
      }

      @Override
      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.FlatCache;
      }
   }

   interface NoiseChunkDensityFunction extends DensityFunction {
      DensityFunction wrapped();

      @Override
      default double minValue() {
         return this.wrapped().minValue();
      }

      @Override
      default double maxValue() {
         return this.wrapped().maxValue();
      }
   }

   public class NoiseInterpolator implements DensityFunctions.MarkerOrMarked, NoiseChunk.NoiseChunkDensityFunction {
      double[][] slice0;
      double[][] slice1;
      private final DensityFunction noiseFiller;
      private double noise000;
      private double noise001;
      private double noise100;
      private double noise101;
      private double noise010;
      private double noise011;
      private double noise110;
      private double noise111;
      private double valueXZ00;
      private double valueXZ10;
      private double valueXZ01;
      private double valueXZ11;
      private double valueZ0;
      private double valueZ1;
      private double value;

      NoiseInterpolator(final DensityFunction pNoiseFilter) {
         this.noiseFiller = pNoiseFilter;
         this.slice0 = this.allocateSlice(NoiseChunk.this.cellCountY, NoiseChunk.this.cellCountXZ);
         this.slice1 = this.allocateSlice(NoiseChunk.this.cellCountY, NoiseChunk.this.cellCountXZ);
         NoiseChunk.this.interpolators.add(this);
      }

      private double[][] allocateSlice(int pCellCountY, int pCellCountXZ) {
         int $$2 = pCellCountXZ + 1;
         int $$3 = pCellCountY + 1;
         double[][] $$4 = new double[$$2][$$3];

         for (int $$5 = 0; $$5 < $$2; $$5++) {
            $$4[$$5] = new double[$$3];
         }

         return $$4;
      }

      void selectCellYZ(int pY, int pZ) {
         this.noise000 = this.slice0[pZ][pY];
         this.noise001 = this.slice0[pZ + 1][pY];
         this.noise100 = this.slice1[pZ][pY];
         this.noise101 = this.slice1[pZ + 1][pY];
         this.noise010 = this.slice0[pZ][pY + 1];
         this.noise011 = this.slice0[pZ + 1][pY + 1];
         this.noise110 = this.slice1[pZ][pY + 1];
         this.noise111 = this.slice1[pZ + 1][pY + 1];
      }

      void updateForY(double pY) {
         this.valueXZ00 = Mth.lerp(pY, this.noise000, this.noise010);
         this.valueXZ10 = Mth.lerp(pY, this.noise100, this.noise110);
         this.valueXZ01 = Mth.lerp(pY, this.noise001, this.noise011);
         this.valueXZ11 = Mth.lerp(pY, this.noise101, this.noise111);
      }

      void updateForX(double pX) {
         this.valueZ0 = Mth.lerp(pX, this.valueXZ00, this.valueXZ10);
         this.valueZ1 = Mth.lerp(pX, this.valueXZ01, this.valueXZ11);
      }

      void updateForZ(double pZ) {
         this.value = Mth.lerp(pZ, this.valueZ0, this.valueZ1);
      }

      @Override
      public double compute(DensityFunction.FunctionContext pContext) {
         if (pContext != NoiseChunk.this) {
            return this.noiseFiller.compute(pContext);
         } else if (!NoiseChunk.this.interpolating) {
            throw new IllegalStateException("Trying to sample interpolator outside the interpolation loop");
         } else {
            return NoiseChunk.this.fillingCell
               ? Mth.lerp3(
                  (double)NoiseChunk.this.inCellX / (double)NoiseChunk.this.cellWidth,
                  (double)NoiseChunk.this.inCellY / (double)NoiseChunk.this.cellHeight,
                  (double)NoiseChunk.this.inCellZ / (double)NoiseChunk.this.cellWidth,
                  this.noise000,
                  this.noise100,
                  this.noise010,
                  this.noise110,
                  this.noise001,
                  this.noise101,
                  this.noise011,
                  this.noise111
               )
               : this.value;
         }
      }

      @Override
      public void fillArray(double[] pArray, DensityFunction.ContextProvider pContextProvider) {
         if (NoiseChunk.this.fillingCell) {
            pContextProvider.fillAllDirectly(pArray, this);
         } else {
            this.wrapped().fillArray(pArray, pContextProvider);
         }
      }

      @Override
      public DensityFunction wrapped() {
         return this.noiseFiller;
      }

      private void swapSlices() {
         double[][] $$0 = this.slice0;
         this.slice0 = this.slice1;
         this.slice1 = $$0;
      }

      @Override
      public DensityFunctions.Marker.Type type() {
         return DensityFunctions.Marker.Type.Interpolated;
      }
   }
}
