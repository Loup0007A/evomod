package net.minecraft.world.level.chunk.status;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.profiling.jfr.callback.ProfiledDuration;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;

public record ChunkStep(
   ChunkStatus targetStatus, ChunkDependencies directDependencies, ChunkDependencies accumulatedDependencies, int blockStateWriteRadius, ChunkStatusTask task
) {
   public int getAccumulatedRadiusOf(ChunkStatus pStatus) {
      return pStatus == this.targetStatus ? 0 : this.accumulatedDependencies.getRadiusOf(pStatus);
   }

   public CompletableFuture<ChunkAccess> apply(WorldGenContext pWorldGenContext, StaticCache2D<GenerationChunkHolder> pCache, ChunkAccess pChunk) {
      if (pChunk.getPersistedStatus().isBefore(this.targetStatus)) {
         ProfiledDuration $$3 = JvmProfiler.INSTANCE.onChunkGenerate(pChunk.getPos(), pWorldGenContext.level().dimension(), this.targetStatus.getName());
         return this.task.doWork(pWorldGenContext, this, pCache, pChunk).thenApply(p_345132_ -> this.completeChunkGeneration(p_345132_, $$3));
      } else {
         return this.task.doWork(pWorldGenContext, this, pCache, pChunk);
      }
   }

   private ChunkAccess completeChunkGeneration(ChunkAccess pChunk, @Nullable ProfiledDuration pDuration) {
      if (pChunk instanceof ProtoChunk $$2 && $$2.getPersistedStatus().isBefore(this.targetStatus)) {
         $$2.setPersistedStatus(this.targetStatus);
      }

      if (pDuration != null) {
         pDuration.finish();
      }

      return pChunk;
   }

   public static class Builder {
      private final ChunkStatus status;
      @Nullable
      private final ChunkStep parent;
      private ChunkStatus[] directDependenciesByRadius;
      private int blockStateWriteRadius = -1;
      private ChunkStatusTask task = ChunkStatusTasks::passThrough;

      protected Builder(ChunkStatus pStatus) {
         if (pStatus.getParent() != pStatus) {
            throw new IllegalArgumentException("Not starting with the first status: " + pStatus);
         } else {
            this.status = pStatus;
            this.parent = null;
            this.directDependenciesByRadius = new ChunkStatus[0];
         }
      }

      protected Builder(ChunkStatus pStatus, ChunkStep pParent) {
         if (pParent.targetStatus.getIndex() != pStatus.getIndex() - 1) {
            throw new IllegalArgumentException("Out of order status: " + pStatus);
         } else {
            this.status = pStatus;
            this.parent = pParent;
            this.directDependenciesByRadius = new ChunkStatus[]{pParent.targetStatus};
         }
      }

      public ChunkStep.Builder addRequirement(ChunkStatus pStatus, int pRadius) {
         if (pStatus.isOrAfter(this.status)) {
            throw new IllegalArgumentException("Status " + pStatus + " can not be required by " + this.status);
         } else {
            ChunkStatus[] $$2 = this.directDependenciesByRadius;
            int $$3 = pRadius + 1;
            if ($$3 > $$2.length) {
               this.directDependenciesByRadius = new ChunkStatus[$$3];
               Arrays.fill(this.directDependenciesByRadius, pStatus);
            }

            for (int $$4 = 0; $$4 < Math.min($$3, $$2.length); $$4++) {
               this.directDependenciesByRadius[$$4] = ChunkStatus.max($$2[$$4], pStatus);
            }

            return this;
         }
      }

      public ChunkStep.Builder blockStateWriteRadius(int pBlockStateWriteRadius) {
         this.blockStateWriteRadius = pBlockStateWriteRadius;
         return this;
      }

      public ChunkStep.Builder setTask(ChunkStatusTask pTask) {
         this.task = pTask;
         return this;
      }

      public ChunkStep build() {
         return new ChunkStep(
            this.status,
            new ChunkDependencies(ImmutableList.copyOf(this.directDependenciesByRadius)),
            new ChunkDependencies(ImmutableList.copyOf(this.buildAccumulatedDependencies())),
            this.blockStateWriteRadius,
            this.task
         );
      }

      private ChunkStatus[] buildAccumulatedDependencies() {
         if (this.parent == null) {
            return this.directDependenciesByRadius;
         } else {
            int $$0 = this.getRadiusOfParent(this.parent.targetStatus);
            ChunkDependencies $$1 = this.parent.accumulatedDependencies;
            ChunkStatus[] $$2 = new ChunkStatus[Math.max($$0 + $$1.size(), this.directDependenciesByRadius.length)];

            for (int $$3 = 0; $$3 < $$2.length; $$3++) {
               int $$4 = $$3 - $$0;
               if ($$4 < 0 || $$4 >= $$1.size()) {
                  $$2[$$3] = this.directDependenciesByRadius[$$3];
               } else if ($$3 >= this.directDependenciesByRadius.length) {
                  $$2[$$3] = $$1.get($$4);
               } else {
                  $$2[$$3] = ChunkStatus.max(this.directDependenciesByRadius[$$3], $$1.get($$4));
               }
            }

            return $$2;
         }
      }

      private int getRadiusOfParent(ChunkStatus pStatus) {
         for (int $$1 = this.directDependenciesByRadius.length - 1; $$1 >= 0; $$1--) {
            if (this.directDependenciesByRadius[$$1].isOrAfter(pStatus)) {
               return $$1;
            }
         }

         return 0;
      }
   }
}
