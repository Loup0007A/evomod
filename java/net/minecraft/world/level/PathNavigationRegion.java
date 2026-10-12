package net.minecraft.world.level;

import com.google.common.base.Suppliers;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PathNavigationRegion implements BlockGetter, CollisionGetter {
   protected final int centerX;
   protected final int centerZ;
   protected final ChunkAccess[][] chunks;
   protected boolean allEmpty;
   protected final Level level;
   private final Supplier<Holder<Biome>> plains;

   public PathNavigationRegion(Level pLevel, BlockPos pCenterPos, BlockPos pOffsetPos) {
      this.level = pLevel;
      this.plains = Suppliers.memoize(() -> pLevel.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS));
      this.centerX = SectionPos.blockToSectionCoord(pCenterPos.getX());
      this.centerZ = SectionPos.blockToSectionCoord(pCenterPos.getZ());
      int $$3 = SectionPos.blockToSectionCoord(pOffsetPos.getX());
      int $$4 = SectionPos.blockToSectionCoord(pOffsetPos.getZ());
      this.chunks = new ChunkAccess[$$3 - this.centerX + 1][$$4 - this.centerZ + 1];
      ChunkSource $$5 = pLevel.getChunkSource();
      this.allEmpty = true;

      for (int $$6 = this.centerX; $$6 <= $$3; $$6++) {
         for (int $$7 = this.centerZ; $$7 <= $$4; $$7++) {
            this.chunks[$$6 - this.centerX][$$7 - this.centerZ] = $$5.getChunkNow($$6, $$7);
         }
      }

      for (int $$8 = SectionPos.blockToSectionCoord(pCenterPos.getX()); $$8 <= SectionPos.blockToSectionCoord(pOffsetPos.getX()); $$8++) {
         for (int $$9 = SectionPos.blockToSectionCoord(pCenterPos.getZ()); $$9 <= SectionPos.blockToSectionCoord(pOffsetPos.getZ()); $$9++) {
            ChunkAccess $$10 = this.chunks[$$8 - this.centerX][$$9 - this.centerZ];
            if ($$10 != null && !$$10.isYSpaceEmpty(pCenterPos.getY(), pOffsetPos.getY())) {
               this.allEmpty = false;
               return;
            }
         }
      }
   }

   private ChunkAccess getChunk(BlockPos pPos) {
      return this.getChunk(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ()));
   }

   private ChunkAccess getChunk(int pX, int pZ) {
      int $$2 = pX - this.centerX;
      int $$3 = pZ - this.centerZ;
      if ($$2 >= 0 && $$2 < this.chunks.length && $$3 >= 0 && $$3 < this.chunks[$$2].length) {
         ChunkAccess $$4 = this.chunks[$$2][$$3];
         return (ChunkAccess)($$4 != null ? $$4 : new EmptyLevelChunk(this.level, new ChunkPos(pX, pZ), this.plains.get()));
      } else {
         return new EmptyLevelChunk(this.level, new ChunkPos(pX, pZ), this.plains.get());
      }
   }

   @Override
   public WorldBorder getWorldBorder() {
      return this.level.getWorldBorder();
   }

   @Override
   public BlockGetter getChunkForCollisions(int pChunkX, int pChunkZ) {
      return this.getChunk(pChunkX, pChunkZ);
   }

   @Override
   public List<VoxelShape> getEntityCollisions(@Nullable Entity pEntity, AABB pCollisionBox) {
      return List.of();
   }

   @Nullable
   @Override
   public BlockEntity getBlockEntity(BlockPos pPos) {
      ChunkAccess $$1 = this.getChunk(pPos);
      return $$1.getBlockEntity(pPos);
   }

   @Override
   public BlockState getBlockState(BlockPos pPos) {
      if (this.isOutsideBuildHeight(pPos)) {
         return Blocks.AIR.defaultBlockState();
      } else {
         ChunkAccess $$1 = this.getChunk(pPos);
         return $$1.getBlockState(pPos);
      }
   }

   @Override
   public FluidState getFluidState(BlockPos pPos) {
      if (this.isOutsideBuildHeight(pPos)) {
         return Fluids.EMPTY.defaultFluidState();
      } else {
         ChunkAccess $$1 = this.getChunk(pPos);
         return $$1.getFluidState(pPos);
      }
   }

   @Override
   public int getMinBuildHeight() {
      return this.level.getMinBuildHeight();
   }

   @Override
   public int getHeight() {
      return this.level.getHeight();
   }

   public ProfilerFiller getProfiler() {
      return this.level.getProfiler();
   }
}
