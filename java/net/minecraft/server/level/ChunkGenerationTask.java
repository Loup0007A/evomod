package net.minecraft.server.level;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkDependencies;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class ChunkGenerationTask {
   private final GeneratingChunkMap chunkMap;
   private final ChunkPos pos;
   @Nullable
   private ChunkStatus scheduledStatus = null;
   public final ChunkStatus targetStatus;
   private volatile boolean markedForCancellation;
   private final List<CompletableFuture<ChunkResult<ChunkAccess>>> scheduledLayer = new ArrayList<>();
   private final StaticCache2D<GenerationChunkHolder> cache;
   private boolean needsGeneration;

   private ChunkGenerationTask(GeneratingChunkMap pChunkMap, ChunkStatus pTargetStatus, ChunkPos pPos, StaticCache2D<GenerationChunkHolder> pCache) {
      this.chunkMap = pChunkMap;
      this.targetStatus = pTargetStatus;
      this.pos = pPos;
      this.cache = pCache;
   }

   public static ChunkGenerationTask create(GeneratingChunkMap pChunkMap, ChunkStatus pTargetStatus, ChunkPos pPos) {
      int $$3 = ChunkPyramid.GENERATION_PYRAMID.getStepTo(pTargetStatus).getAccumulatedRadiusOf(ChunkStatus.EMPTY);
      StaticCache2D<GenerationChunkHolder> $$4 = StaticCache2D.create(
         pPos.x, pPos.z, $$3, (p_342548_, p_344508_) -> pChunkMap.acquireGeneration(ChunkPos.asLong(p_342548_, p_344508_))
      );
      return new ChunkGenerationTask(pChunkMap, pTargetStatus, pPos, $$4);
   }

   @Nullable
   public CompletableFuture<?> runUntilWait() {
      while (true) {
         CompletableFuture<?> $$0 = this.waitForScheduledLayer();
         if ($$0 != null) {
            return $$0;
         }

         if (this.markedForCancellation || this.scheduledStatus == this.targetStatus) {
            this.releaseClaim();
            return null;
         }

         this.scheduleNextLayer();
      }
   }

   private void scheduleNextLayer() {
      ChunkStatus $$0;
      if (this.scheduledStatus == null) {
         $$0 = ChunkStatus.EMPTY;
      } else if (!this.needsGeneration && this.scheduledStatus == ChunkStatus.EMPTY && !this.canLoadWithoutGeneration()) {
         this.needsGeneration = true;
         $$0 = ChunkStatus.EMPTY;
      } else {
         $$0 = ChunkStatus.getStatusList().get(this.scheduledStatus.getIndex() + 1);
      }

      this.scheduleLayer($$0, this.needsGeneration);
      this.scheduledStatus = $$0;
   }

   public void markForCancellation() {
      this.markedForCancellation = true;
   }

   private void releaseClaim() {
      GenerationChunkHolder $$0 = this.cache.get(this.pos.x, this.pos.z);
      $$0.removeTask(this);
      this.cache.forEach(this.chunkMap::releaseGeneration);
   }

   private boolean canLoadWithoutGeneration() {
      if (this.targetStatus == ChunkStatus.EMPTY) {
         return true;
      } else {
         ChunkStatus $$0 = this.cache.get(this.pos.x, this.pos.z).getPersistedStatus();
         if ($$0 != null && !$$0.isBefore(this.targetStatus)) {
            ChunkDependencies $$1 = ChunkPyramid.LOADING_PYRAMID.getStepTo(this.targetStatus).accumulatedDependencies();
            int $$2 = $$1.getRadius();

            for (int $$3 = this.pos.x - $$2; $$3 <= this.pos.x + $$2; $$3++) {
               for (int $$4 = this.pos.z - $$2; $$4 <= this.pos.z + $$2; $$4++) {
                  int $$5 = this.pos.getChessboardDistance($$3, $$4);
                  ChunkStatus $$6 = $$1.get($$5);
                  ChunkStatus $$7 = this.cache.get($$3, $$4).getPersistedStatus();
                  if ($$7 == null || $$7.isBefore($$6)) {
                     return false;
                  }
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }

   public GenerationChunkHolder getCenter() {
      return this.cache.get(this.pos.x, this.pos.z);
   }

   private void scheduleLayer(ChunkStatus pStatus, boolean pNeedsGeneration) {
      int $$2 = this.getRadiusForLayer(pStatus, pNeedsGeneration);

      for (int $$3 = this.pos.x - $$2; $$3 <= this.pos.x + $$2; $$3++) {
         for (int $$4 = this.pos.z - $$2; $$4 <= this.pos.z + $$2; $$4++) {
            GenerationChunkHolder $$5 = this.cache.get($$3, $$4);
            if (this.markedForCancellation || !this.scheduleChunkInLayer(pStatus, pNeedsGeneration, $$5)) {
               return;
            }
         }
      }
   }

   private int getRadiusForLayer(ChunkStatus pStatus, boolean pNeedsGeneration) {
      ChunkPyramid $$2 = pNeedsGeneration ? ChunkPyramid.GENERATION_PYRAMID : ChunkPyramid.LOADING_PYRAMID;
      return $$2.getStepTo(this.targetStatus).getAccumulatedRadiusOf(pStatus);
   }

   private boolean scheduleChunkInLayer(ChunkStatus pStatus, boolean pNeedsGeneration, GenerationChunkHolder pChunk) {
      ChunkStatus $$3 = pChunk.getPersistedStatus();
      boolean $$4 = $$3 != null && pStatus.isAfter($$3);
      ChunkPyramid $$5 = $$4 ? ChunkPyramid.GENERATION_PYRAMID : ChunkPyramid.LOADING_PYRAMID;
      if ($$4 && !pNeedsGeneration) {
         throw new IllegalStateException("Can't load chunk, but didn't expect to need to generate");
      } else {
         CompletableFuture<ChunkResult<ChunkAccess>> $$6 = pChunk.applyStep($$5.getStepTo(pStatus), this.chunkMap, this.cache);
         ChunkResult<ChunkAccess> $$7 = $$6.getNow(null);
         if ($$7 == null) {
            this.scheduledLayer.add($$6);
            return true;
         } else if ($$7.isSuccess()) {
            return true;
         } else {
            this.markForCancellation();
            return false;
         }
      }
   }

   @Nullable
   private CompletableFuture<?> waitForScheduledLayer() {
      while (!this.scheduledLayer.isEmpty()) {
         CompletableFuture<ChunkResult<ChunkAccess>> $$0 = this.scheduledLayer.getLast();
         ChunkResult<ChunkAccess> $$1 = $$0.getNow(null);
         if ($$1 == null) {
            return $$0;
         }

         this.scheduledLayer.removeLast();
         if (!$$1.isSuccess()) {
            this.markForCancellation();
         }
      }

      return null;
   }
}
