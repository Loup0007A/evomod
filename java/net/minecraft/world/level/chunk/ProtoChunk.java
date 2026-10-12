package net.minecraft.world.level.chunk;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import net.minecraft.world.ticks.TickContainerAccess;

public class ProtoChunk extends ChunkAccess {
   @Nullable
   private volatile LevelLightEngine lightEngine;
   private volatile ChunkStatus status = ChunkStatus.EMPTY;
   private final List<CompoundTag> entities = Lists.newArrayList();
   private final Map<GenerationStep.Carving, CarvingMask> carvingMasks = new Object2ObjectArrayMap();
   @Nullable
   private BelowZeroRetrogen belowZeroRetrogen;
   private final ProtoChunkTicks<Block> blockTicks;
   private final ProtoChunkTicks<Fluid> fluidTicks;

   public ProtoChunk(
      ChunkPos pChunkPos,
      UpgradeData pUpgradeData,
      LevelHeightAccessor pLevelHeightAccessor,
      Registry<Biome> pBiomeRegistry,
      @Nullable BlendingData pBlendingData
   ) {
      this(pChunkPos, pUpgradeData, null, new ProtoChunkTicks<>(), new ProtoChunkTicks<>(), pLevelHeightAccessor, pBiomeRegistry, pBlendingData);
   }

   public ProtoChunk(
      ChunkPos pChunkPos,
      UpgradeData pUpgradeData,
      @Nullable LevelChunkSection[] pSections,
      ProtoChunkTicks<Block> pBlockTicks,
      ProtoChunkTicks<Fluid> pLiquidTicks,
      LevelHeightAccessor pLevelHeightAccessor,
      Registry<Biome> pBiomeRegistry,
      @Nullable BlendingData pBlendingData
   ) {
      super(pChunkPos, pUpgradeData, pLevelHeightAccessor, pBiomeRegistry, 0L, pSections, pBlendingData);
      this.blockTicks = pBlockTicks;
      this.fluidTicks = pLiquidTicks;
   }

   @Override
   public TickContainerAccess<Block> getBlockTicks() {
      return this.blockTicks;
   }

   @Override
   public TickContainerAccess<Fluid> getFluidTicks() {
      return this.fluidTicks;
   }

   @Override
   public ChunkAccess.TicksToSave getTicksForSerialization() {
      return new ChunkAccess.TicksToSave(this.blockTicks, this.fluidTicks);
   }

   @Override
   public BlockState getBlockState(BlockPos pPos) {
      int $$1 = pPos.getY();
      if (this.isOutsideBuildHeight($$1)) {
         return Blocks.VOID_AIR.defaultBlockState();
      } else {
         LevelChunkSection $$2 = this.getSection(this.getSectionIndex($$1));
         return $$2.hasOnlyAir() ? Blocks.AIR.defaultBlockState() : $$2.getBlockState(pPos.getX() & 15, $$1 & 15, pPos.getZ() & 15);
      }
   }

   @Override
   public FluidState getFluidState(BlockPos pPos) {
      int $$1 = pPos.getY();
      if (this.isOutsideBuildHeight($$1)) {
         return Fluids.EMPTY.defaultFluidState();
      } else {
         LevelChunkSection $$2 = this.getSection(this.getSectionIndex($$1));
         return $$2.hasOnlyAir() ? Fluids.EMPTY.defaultFluidState() : $$2.getFluidState(pPos.getX() & 15, $$1 & 15, pPos.getZ() & 15);
      }
   }

   @Nullable
   @Override
   public BlockState setBlockState(BlockPos pPos, BlockState pState, boolean pIsMoving) {
      int $$3 = pPos.getX();
      int $$4 = pPos.getY();
      int $$5 = pPos.getZ();
      if ($$4 >= this.getMinBuildHeight() && $$4 < this.getMaxBuildHeight()) {
         int $$6 = this.getSectionIndex($$4);
         LevelChunkSection $$7 = this.getSection($$6);
         boolean $$8 = $$7.hasOnlyAir();
         if ($$8 && pState.is(Blocks.AIR)) {
            return pState;
         } else {
            int $$9 = SectionPos.sectionRelative($$3);
            int $$10 = SectionPos.sectionRelative($$4);
            int $$11 = SectionPos.sectionRelative($$5);
            BlockState $$12 = $$7.setBlockState($$9, $$10, $$11, pState);
            if (this.status.isOrAfter(ChunkStatus.INITIALIZE_LIGHT)) {
               boolean $$13 = $$7.hasOnlyAir();
               if ($$13 != $$8) {
                  this.lightEngine.updateSectionStatus(pPos, $$13);
               }

               if (LightEngine.hasDifferentLightProperties(this, pPos, $$12, pState)) {
                  this.skyLightSources.update(this, $$9, $$4, $$11);
                  this.lightEngine.checkBlock(pPos);
               }
            }

            EnumSet<Heightmap.Types> $$14 = this.getPersistedStatus().heightmapsAfter();
            EnumSet<Heightmap.Types> $$15 = null;

            for (Heightmap.Types $$16 : $$14) {
               Heightmap $$17 = this.heightmaps.get($$16);
               if ($$17 == null) {
                  if ($$15 == null) {
                     $$15 = EnumSet.noneOf(Heightmap.Types.class);
                  }

                  $$15.add($$16);
               }
            }

            if ($$15 != null) {
               Heightmap.primeHeightmaps(this, $$15);
            }

            for (Heightmap.Types $$18 : $$14) {
               this.heightmaps.get($$18).update($$9, $$4, $$11, pState);
            }

            return $$12;
         }
      } else {
         return Blocks.VOID_AIR.defaultBlockState();
      }
   }

   @Override
   public void setBlockEntity(BlockEntity pBlockEntity) {
      this.blockEntities.put(pBlockEntity.getBlockPos(), pBlockEntity);
   }

   @Nullable
   @Override
   public BlockEntity getBlockEntity(BlockPos pPos) {
      return this.blockEntities.get(pPos);
   }

   public Map<BlockPos, BlockEntity> getBlockEntities() {
      return this.blockEntities;
   }

   public void addEntity(CompoundTag pTag) {
      this.entities.add(pTag);
   }

   @Override
   public void addEntity(Entity pEntity) {
      if (!pEntity.isPassenger()) {
         CompoundTag $$1 = new CompoundTag();
         pEntity.save($$1);
         this.addEntity($$1);
      }
   }

   @Override
   public void setStartForStructure(Structure pStructure, StructureStart pStructureStart) {
      BelowZeroRetrogen $$2 = this.getBelowZeroRetrogen();
      if ($$2 != null && pStructureStart.isValid()) {
         BoundingBox $$3 = pStructureStart.getBoundingBox();
         LevelHeightAccessor $$4 = this.getHeightAccessorForGeneration();
         if ($$3.minY() < $$4.getMinBuildHeight() || $$3.maxY() >= $$4.getMaxBuildHeight()) {
            return;
         }
      }

      super.setStartForStructure(pStructure, pStructureStart);
   }

   public List<CompoundTag> getEntities() {
      return this.entities;
   }

   @Override
   public ChunkStatus getPersistedStatus() {
      return this.status;
   }

   public void setPersistedStatus(ChunkStatus pStatus) {
      this.status = pStatus;
      if (this.belowZeroRetrogen != null && pStatus.isOrAfter(this.belowZeroRetrogen.targetStatus())) {
         this.setBelowZeroRetrogen(null);
      }

      this.setUnsaved(true);
   }

   @Override
   public Holder<Biome> getNoiseBiome(int pX, int pY, int pZ) {
      if (this.getHighestGeneratedStatus().isOrAfter(ChunkStatus.BIOMES)) {
         return super.getNoiseBiome(pX, pY, pZ);
      } else {
         throw new IllegalStateException("Asking for biomes before we have biomes");
      }
   }

   public static short packOffsetCoordinates(BlockPos pPos) {
      int $$1 = pPos.getX();
      int $$2 = pPos.getY();
      int $$3 = pPos.getZ();
      int $$4 = $$1 & 15;
      int $$5 = $$2 & 15;
      int $$6 = $$3 & 15;
      return (short)($$4 | $$5 << 4 | $$6 << 8);
   }

   public static BlockPos unpackOffsetCoordinates(short pPackedPos, int pYOffset, ChunkPos pChunkPos) {
      int $$3 = SectionPos.sectionToBlockCoord(pChunkPos.x, pPackedPos & 15);
      int $$4 = SectionPos.sectionToBlockCoord(pYOffset, pPackedPos >>> 4 & 15);
      int $$5 = SectionPos.sectionToBlockCoord(pChunkPos.z, pPackedPos >>> 8 & 15);
      return new BlockPos($$3, $$4, $$5);
   }

   @Override
   public void markPosForPostprocessing(BlockPos pPos) {
      if (!this.isOutsideBuildHeight(pPos)) {
         ChunkAccess.getOrCreateOffsetList(this.postProcessing, this.getSectionIndex(pPos.getY())).add(packOffsetCoordinates(pPos));
      }
   }

   @Override
   public void addPackedPostProcess(short pPackedPosition, int pIndex) {
      ChunkAccess.getOrCreateOffsetList(this.postProcessing, pIndex).add(pPackedPosition);
   }

   public Map<BlockPos, CompoundTag> getBlockEntityNbts() {
      return Collections.unmodifiableMap(this.pendingBlockEntities);
   }

   @Nullable
   @Override
   public CompoundTag getBlockEntityNbtForSaving(BlockPos pPos, HolderLookup.Provider pRegistries) {
      BlockEntity $$2 = this.getBlockEntity(pPos);
      return $$2 != null ? $$2.saveWithFullMetadata(pRegistries) : this.pendingBlockEntities.get(pPos);
   }

   @Override
   public void removeBlockEntity(BlockPos pPos) {
      this.blockEntities.remove(pPos);
      this.pendingBlockEntities.remove(pPos);
   }

   @Nullable
   public CarvingMask getCarvingMask(GenerationStep.Carving pStep) {
      return this.carvingMasks.get(pStep);
   }

   public CarvingMask getOrCreateCarvingMask(GenerationStep.Carving pStep) {
      return this.carvingMasks.computeIfAbsent(pStep, p_327418_ -> new CarvingMask(this.getHeight(), this.getMinBuildHeight()));
   }

   public void setCarvingMask(GenerationStep.Carving pStep, CarvingMask pCarvingMask) {
      this.carvingMasks.put(pStep, pCarvingMask);
   }

   public void setLightEngine(LevelLightEngine pLightEngine) {
      this.lightEngine = pLightEngine;
   }

   public void setBelowZeroRetrogen(@Nullable BelowZeroRetrogen pBelowZeroRetrogen) {
      this.belowZeroRetrogen = pBelowZeroRetrogen;
   }

   @Nullable
   @Override
   public BelowZeroRetrogen getBelowZeroRetrogen() {
      return this.belowZeroRetrogen;
   }

   private static <T> LevelChunkTicks<T> unpackTicks(ProtoChunkTicks<T> pTicks) {
      return new LevelChunkTicks<>(pTicks.scheduledTicks());
   }

   public LevelChunkTicks<Block> unpackBlockTicks() {
      return unpackTicks(this.blockTicks);
   }

   public LevelChunkTicks<Fluid> unpackFluidTicks() {
      return unpackTicks(this.fluidTicks);
   }

   @Override
   public LevelHeightAccessor getHeightAccessorForGeneration() {
      return (LevelHeightAccessor)(this.isUpgrading() ? BelowZeroRetrogen.UPGRADE_HEIGHT_ACCESSOR : this);
   }
}
