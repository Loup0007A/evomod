package net.minecraft.client.renderer.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.primitives.Doubles;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexSorting;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SectionRenderDispatcher {
   private static final int MAX_HIGH_PRIORITY_QUOTA = 2;
   private final PriorityBlockingQueue<SectionRenderDispatcher.RenderSection.CompileTask> toBatchHighPriority = Queues.newPriorityBlockingQueue();
   private final Queue<SectionRenderDispatcher.RenderSection.CompileTask> toBatchLowPriority = Queues.newLinkedBlockingDeque();
   private int highPriorityQuota = 2;
   private final Queue<Runnable> toUpload = Queues.newConcurrentLinkedQueue();
   final SectionBufferBuilderPack fixedBuffers;
   private final SectionBufferBuilderPool bufferPool;
   private volatile int toBatchCount;
   private volatile boolean closed;
   private final ProcessorMailbox<Runnable> mailbox;
   private final Executor executor;
   ClientLevel level;
   final LevelRenderer renderer;
   private Vec3 camera = Vec3.ZERO;
   final SectionCompiler sectionCompiler;

   public SectionRenderDispatcher(
      ClientLevel pLevel,
      LevelRenderer pRenderer,
      Executor pExecutor,
      RenderBuffers pBuffers,
      BlockRenderDispatcher pBlockRenderer,
      BlockEntityRenderDispatcher pBlockEntityRenderer
   ) {
      this.level = pLevel;
      this.renderer = pRenderer;
      this.fixedBuffers = pBuffers.fixedBufferPack();
      this.bufferPool = pBuffers.sectionBufferPool();
      this.executor = pExecutor;
      this.mailbox = ProcessorMailbox.create(pExecutor, "Section Renderer");
      this.mailbox.tell(this::runTask);
      this.sectionCompiler = new SectionCompiler(pBlockRenderer, pBlockEntityRenderer);
   }

   public void setLevel(ClientLevel pLevel) {
      this.level = pLevel;
   }

   private void runTask() {
      if (!this.closed && !this.bufferPool.isEmpty()) {
         SectionRenderDispatcher.RenderSection.CompileTask $$0 = this.pollTask();
         if ($$0 != null) {
            SectionBufferBuilderPack $$1 = Objects.requireNonNull(this.bufferPool.acquire());
            this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
            CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName($$0.name(), () -> $$0.doTask($$1)), this.executor)
               .thenCompose(p_298155_ -> (CompletionStage<SectionRenderDispatcher.SectionTaskResult>)p_298155_)
               .whenComplete((p_299295_, p_297995_) -> {
                  if (p_297995_ != null) {
                     Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_297995_, "Batching sections"));
                  } else {
                     this.mailbox.tell(() -> {
                        if (p_299295_ == SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL) {
                           $$1.clearAll();
                        } else {
                           $$1.discardAll();
                        }

                        this.bufferPool.release($$1);
                        this.runTask();
                     });
                  }
               });
         }
      }
   }

   @Nullable
   private SectionRenderDispatcher.RenderSection.CompileTask pollTask() {
      if (this.highPriorityQuota <= 0) {
         SectionRenderDispatcher.RenderSection.CompileTask $$0 = this.toBatchLowPriority.poll();
         if ($$0 != null) {
            this.highPriorityQuota = 2;
            return $$0;
         }
      }

      SectionRenderDispatcher.RenderSection.CompileTask $$1 = this.toBatchHighPriority.poll();
      if ($$1 != null) {
         this.highPriorityQuota--;
         return $$1;
      } else {
         this.highPriorityQuota = 2;
         return this.toBatchLowPriority.poll();
      }
   }

   public String getStats() {
      return String.format(Locale.ROOT, "pC: %03d, pU: %02d, aB: %02d", this.toBatchCount, this.toUpload.size(), this.bufferPool.getFreeBufferCount());
   }

   public int getToBatchCount() {
      return this.toBatchCount;
   }

   public int getToUpload() {
      return this.toUpload.size();
   }

   public int getFreeBufferCount() {
      return this.bufferPool.getFreeBufferCount();
   }

   public void setCamera(Vec3 pCamera) {
      this.camera = pCamera;
   }

   public Vec3 getCameraPosition() {
      return this.camera;
   }

   public void uploadAllPendingUploads() {
      Runnable $$0;
      while (($$0 = this.toUpload.poll()) != null) {
         $$0.run();
      }
   }

   public void rebuildSectionSync(SectionRenderDispatcher.RenderSection pSection, RenderRegionCache pRegionCache) {
      pSection.compileSync(pRegionCache);
   }

   public void blockUntilClear() {
      this.clearBatchQueue();
   }

   public void schedule(SectionRenderDispatcher.RenderSection.CompileTask pTask) {
      if (!this.closed) {
         this.mailbox.tell(() -> {
            if (!this.closed) {
               if (pTask.isHighPriority) {
                  this.toBatchHighPriority.offer(pTask);
               } else {
                  this.toBatchLowPriority.offer(pTask);
               }

               this.toBatchCount = this.toBatchHighPriority.size() + this.toBatchLowPriority.size();
               this.runTask();
            }
         });
      }
   }

   public CompletableFuture<Void> uploadSectionLayer(MeshData pMeshData, VertexBuffer pVertexBuffer) {
      return this.closed ? CompletableFuture.completedFuture(null) : CompletableFuture.runAsync(() -> {
         if (pVertexBuffer.isInvalid()) {
            pMeshData.close();
         } else {
            pVertexBuffer.bind();
            pVertexBuffer.upload(pMeshData);
            VertexBuffer.unbind();
         }
      }, this.toUpload::add);
   }

   public CompletableFuture<Void> uploadSectionIndexBuffer(ByteBufferBuilder.Result pResult, VertexBuffer pVertexBuffer) {
      return this.closed ? CompletableFuture.completedFuture(null) : CompletableFuture.runAsync(() -> {
         if (pVertexBuffer.isInvalid()) {
            pResult.close();
         } else {
            pVertexBuffer.bind();
            pVertexBuffer.uploadIndexBuffer(pResult);
            VertexBuffer.unbind();
         }
      }, this.toUpload::add);
   }

   private void clearBatchQueue() {
      while (!this.toBatchHighPriority.isEmpty()) {
         SectionRenderDispatcher.RenderSection.CompileTask $$0 = this.toBatchHighPriority.poll();
         if ($$0 != null) {
            $$0.cancel();
         }
      }

      while (!this.toBatchLowPriority.isEmpty()) {
         SectionRenderDispatcher.RenderSection.CompileTask $$1 = this.toBatchLowPriority.poll();
         if ($$1 != null) {
            $$1.cancel();
         }
      }

      this.toBatchCount = 0;
   }

   public boolean isQueueEmpty() {
      return this.toBatchCount == 0 && this.toUpload.isEmpty();
   }

   public void dispose() {
      this.closed = true;
      this.clearBatchQueue();
      this.uploadAllPendingUploads();
   }

   @OnlyIn(Dist.CLIENT)
   public static class CompiledSection {
      public static final SectionRenderDispatcher.CompiledSection UNCOMPILED = new SectionRenderDispatcher.CompiledSection() {
         @Override
         public boolean facesCanSeeEachother(Direction p_301280_, Direction p_299155_) {
            return false;
         }
      };
      public static final SectionRenderDispatcher.CompiledSection EMPTY = new SectionRenderDispatcher.CompiledSection() {
         @Override
         public boolean facesCanSeeEachother(Direction p_343413_, Direction p_342431_) {
            return true;
         }
      };
      final Set<RenderType> hasBlocks = new ObjectArraySet(RenderType.chunkBufferLayers().size());
      final List<BlockEntity> renderableBlockEntities = Lists.newArrayList();
      VisibilitySet visibilitySet = new VisibilitySet();
      @Nullable
      MeshData.SortState transparencyState;

      public boolean hasNoRenderableLayers() {
         return this.hasBlocks.isEmpty();
      }

      public boolean isEmpty(RenderType pRenderType) {
         return !this.hasBlocks.contains(pRenderType);
      }

      public List<BlockEntity> getRenderableBlockEntities() {
         return this.renderableBlockEntities;
      }

      public boolean facesCanSeeEachother(Direction pFace1, Direction pFace2) {
         return this.visibilitySet.visibilityBetween(pFace1, pFace2);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class RenderSection {
      public static final int SIZE = 16;
      public final int index;
      public final AtomicReference<SectionRenderDispatcher.CompiledSection> compiled = new AtomicReference<>(SectionRenderDispatcher.CompiledSection.UNCOMPILED);
      private final AtomicInteger initialCompilationCancelCount = new AtomicInteger(0);
      @Nullable
      private SectionRenderDispatcher.RenderSection.RebuildTask lastRebuildTask;
      @Nullable
      private SectionRenderDispatcher.RenderSection.ResortTransparencyTask lastResortTransparencyTask;
      private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
      private final Map<RenderType, VertexBuffer> buffers = RenderType.chunkBufferLayers()
         .stream()
         .collect(Collectors.toMap(p_298649_ -> (RenderType)p_298649_, p_299941_ -> new VertexBuffer(VertexBuffer.Usage.STATIC)));
      private AABB bb;
      private boolean dirty = true;
      final BlockPos.MutableBlockPos origin = new BlockPos.MutableBlockPos(-1, -1, -1);
      private final BlockPos.MutableBlockPos[] relativeOrigins = Util.make(new BlockPos.MutableBlockPos[6], p_300613_ -> {
         for (int $$1 = 0; $$1 < p_300613_.length; $$1++) {
            p_300613_[$$1] = new BlockPos.MutableBlockPos();
         }
      });
      private boolean playerChanged;

      public RenderSection(final int pIndex, final int pOriginX, final int pOriginY, final int pOriginZ) {
         this.index = pIndex;
         this.setOrigin(pOriginX, pOriginY, pOriginZ);
      }

      private boolean doesChunkExistAt(BlockPos pPos) {
         return SectionRenderDispatcher.this.level
               .getChunk(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ()), ChunkStatus.FULL, false)
            != null;
      }

      public boolean hasAllNeighbors() {
         int $$0 = 24;
         return !(this.getDistToPlayerSqr() > 576.0)
            ? true
            : this.doesChunkExistAt(this.relativeOrigins[Direction.WEST.ordinal()])
               && this.doesChunkExistAt(this.relativeOrigins[Direction.NORTH.ordinal()])
               && this.doesChunkExistAt(this.relativeOrigins[Direction.EAST.ordinal()])
               && this.doesChunkExistAt(this.relativeOrigins[Direction.SOUTH.ordinal()]);
      }

      public AABB getBoundingBox() {
         return this.bb;
      }

      public VertexBuffer getBuffer(RenderType pRenderType) {
         return this.buffers.get(pRenderType);
      }

      public void setOrigin(int pX, int pY, int pZ) {
         this.reset();
         this.origin.set(pX, pY, pZ);
         this.bb = new AABB((double)pX, (double)pY, (double)pZ, (double)(pX + 16), (double)(pY + 16), (double)(pZ + 16));

         for (Direction $$3 : Direction.values()) {
            this.relativeOrigins[$$3.ordinal()].set(this.origin).move($$3, 16);
         }
      }

      protected double getDistToPlayerSqr() {
         Camera $$0 = Minecraft.getInstance().gameRenderer.getMainCamera();
         double $$1 = this.bb.minX + 8.0 - $$0.getPosition().x;
         double $$2 = this.bb.minY + 8.0 - $$0.getPosition().y;
         double $$3 = this.bb.minZ + 8.0 - $$0.getPosition().z;
         return $$1 * $$1 + $$2 * $$2 + $$3 * $$3;
      }

      public SectionRenderDispatcher.CompiledSection getCompiled() {
         return this.compiled.get();
      }

      private void reset() {
         this.cancelTasks();
         this.compiled.set(SectionRenderDispatcher.CompiledSection.UNCOMPILED);
         this.dirty = true;
      }

      public void releaseBuffers() {
         this.reset();
         this.buffers.values().forEach(VertexBuffer::close);
      }

      public BlockPos getOrigin() {
         return this.origin;
      }

      public void setDirty(boolean pPlayerChanged) {
         boolean $$1 = this.dirty;
         this.dirty = true;
         this.playerChanged = pPlayerChanged | ($$1 && this.playerChanged);
      }

      public void setNotDirty() {
         this.dirty = false;
         this.playerChanged = false;
      }

      public boolean isDirty() {
         return this.dirty;
      }

      public boolean isDirtyFromPlayer() {
         return this.dirty && this.playerChanged;
      }

      public BlockPos getRelativeOrigin(Direction pDirection) {
         return this.relativeOrigins[pDirection.ordinal()];
      }

      public boolean resortTransparency(RenderType pRenderType, SectionRenderDispatcher pSectionRenderDispatcher) {
         SectionRenderDispatcher.CompiledSection $$2 = this.getCompiled();
         if (this.lastResortTransparencyTask != null) {
            this.lastResortTransparencyTask.cancel();
         }

         if (!$$2.hasBlocks.contains(pRenderType)) {
            return false;
         } else {
            this.lastResortTransparencyTask = new SectionRenderDispatcher.RenderSection.ResortTransparencyTask(this.getDistToPlayerSqr(), $$2);
            pSectionRenderDispatcher.schedule(this.lastResortTransparencyTask);
            return true;
         }
      }

      protected boolean cancelTasks() {
         boolean $$0 = false;
         if (this.lastRebuildTask != null) {
            this.lastRebuildTask.cancel();
            this.lastRebuildTask = null;
            $$0 = true;
         }

         if (this.lastResortTransparencyTask != null) {
            this.lastResortTransparencyTask.cancel();
            this.lastResortTransparencyTask = null;
         }

         return $$0;
      }

      public SectionRenderDispatcher.RenderSection.CompileTask createCompileTask(RenderRegionCache pRegionCache) {
         boolean $$1 = this.cancelTasks();
         RenderChunkRegion $$2 = pRegionCache.createRegion(SectionRenderDispatcher.this.level, SectionPos.of(this.origin));
         boolean $$3 = this.compiled.get() == SectionRenderDispatcher.CompiledSection.UNCOMPILED;
         if ($$3 && $$1) {
            this.initialCompilationCancelCount.incrementAndGet();
         }

         this.lastRebuildTask = new SectionRenderDispatcher.RenderSection.RebuildTask(
            this.getDistToPlayerSqr(), $$2, !$$3 || this.initialCompilationCancelCount.get() > 2
         );
         return this.lastRebuildTask;
      }

      public void rebuildSectionAsync(SectionRenderDispatcher pSectionRenderDispatcher, RenderRegionCache pRegionCache) {
         SectionRenderDispatcher.RenderSection.CompileTask $$2 = this.createCompileTask(pRegionCache);
         pSectionRenderDispatcher.schedule($$2);
      }

      void updateGlobalBlockEntities(Collection<BlockEntity> pBlockEntities) {
         Set<BlockEntity> $$1 = Sets.newHashSet(pBlockEntities);
         Set<BlockEntity> $$2;
         synchronized (this.globalBlockEntities) {
            $$2 = Sets.newHashSet(this.globalBlockEntities);
            $$1.removeAll(this.globalBlockEntities);
            $$2.removeAll(pBlockEntities);
            this.globalBlockEntities.clear();
            this.globalBlockEntities.addAll(pBlockEntities);
         }

         SectionRenderDispatcher.this.renderer.updateGlobalBlockEntities($$2, $$1);
      }

      public void compileSync(RenderRegionCache pRegionCache) {
         SectionRenderDispatcher.RenderSection.CompileTask $$1 = this.createCompileTask(pRegionCache);
         $$1.doTask(SectionRenderDispatcher.this.fixedBuffers);
      }

      public boolean isAxisAlignedWith(int pX, int pY, int pZ) {
         BlockPos $$3 = this.getOrigin();
         return pX == SectionPos.blockToSectionCoord($$3.getX())
            || pZ == SectionPos.blockToSectionCoord($$3.getZ())
            || pY == SectionPos.blockToSectionCoord($$3.getY());
      }

      void setCompiled(SectionRenderDispatcher.CompiledSection pCompiled) {
         this.compiled.set(pCompiled);
         this.initialCompilationCancelCount.set(0);
         SectionRenderDispatcher.this.renderer.addRecentlyCompiledSection(this);
      }

      VertexSorting createVertexSorting() {
         Vec3 $$0 = SectionRenderDispatcher.this.getCameraPosition();
         return VertexSorting.byDistance(
            (float)($$0.x - (double)this.origin.getX()), (float)($$0.y - (double)this.origin.getY()), (float)($$0.z - (double)this.origin.getZ())
         );
      }

      @OnlyIn(Dist.CLIENT)
      abstract class CompileTask implements Comparable<SectionRenderDispatcher.RenderSection.CompileTask> {
         protected final double distAtCreation;
         protected final AtomicBoolean isCancelled = new AtomicBoolean(false);
         protected final boolean isHighPriority;

         public CompileTask(final double pDistAtCreation, final boolean pIsHighPriority) {
            this.distAtCreation = pDistAtCreation;
            this.isHighPriority = pIsHighPriority;
         }

         public abstract CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack pSectionBufferBuilderPack);

         public abstract void cancel();

         protected abstract String name();

         public int compareTo(SectionRenderDispatcher.RenderSection.CompileTask pOther) {
            return Doubles.compare(this.distAtCreation, pOther.distAtCreation);
         }
      }

      @OnlyIn(Dist.CLIENT)
      class RebuildTask extends SectionRenderDispatcher.RenderSection.CompileTask {
         @Nullable
         protected RenderChunkRegion region;

         public RebuildTask(final double pDistAtCreation, @Nullable final RenderChunkRegion pRegion, final boolean pIsHighPriority) {
            super(pDistAtCreation, pIsHighPriority);
            this.region = pRegion;
         }

         @Override
         protected String name() {
            return "rend_chk_rebuild";
         }

         @Override
         public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack pSectionBufferBuilderPack) {
            if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else if (!RenderSection.this.hasAllNeighbors()) {
               this.cancel();
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else {
               RenderChunkRegion $$1 = this.region;
               this.region = null;
               if ($$1 == null) {
                  RenderSection.this.setCompiled(SectionRenderDispatcher.CompiledSection.EMPTY);
                  return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL);
               } else {
                  SectionPos $$2 = SectionPos.of(RenderSection.this.origin);
                  SectionCompiler.Results $$3 = SectionRenderDispatcher.this.sectionCompiler
                     .compile($$2, $$1, RenderSection.this.createVertexSorting(), pSectionBufferBuilderPack);
                  RenderSection.this.updateGlobalBlockEntities($$3.globalBlockEntities);
                  if (this.isCancelled.get()) {
                     $$3.release();
                     return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                  } else {
                     SectionRenderDispatcher.CompiledSection $$4 = new SectionRenderDispatcher.CompiledSection();
                     $$4.visibilitySet = $$3.visibilitySet;
                     $$4.renderableBlockEntities.addAll($$3.blockEntities);
                     $$4.transparencyState = $$3.transparencyState;
                     List<CompletableFuture<Void>> $$5 = new ArrayList<>($$3.renderedLayers.size());
                     $$3.renderedLayers.forEach((p_340913_, p_340914_) -> {
                        $$5.add(SectionRenderDispatcher.this.uploadSectionLayer(p_340914_, RenderSection.this.getBuffer(p_340913_)));
                        $$4.hasBlocks.add(p_340913_);
                     });
                     return Util.sequenceFailFast($$5).handle((p_340916_, p_340917_) -> {
                        if (p_340917_ != null && !(p_340917_ instanceof CancellationException) && !(p_340917_ instanceof InterruptedException)) {
                           Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_340917_, "Rendering section"));
                        }

                        if (this.isCancelled.get()) {
                           return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                        } else {
                           RenderSection.this.setCompiled($$4);
                           return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                        }
                     });
                  }
               }
            }
         }

         @Override
         public void cancel() {
            this.region = null;
            if (this.isCancelled.compareAndSet(false, true)) {
               RenderSection.this.setDirty(false);
            }
         }
      }

      @OnlyIn(Dist.CLIENT)
      class ResortTransparencyTask extends SectionRenderDispatcher.RenderSection.CompileTask {
         private final SectionRenderDispatcher.CompiledSection compiledSection;

         public ResortTransparencyTask(final double pDistAtCreation, final SectionRenderDispatcher.CompiledSection pCompiledSection) {
            super(pDistAtCreation, true);
            this.compiledSection = pCompiledSection;
         }

         @Override
         protected String name() {
            return "rend_chk_sort";
         }

         @Override
         public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack pSectionBufferBuilderPack) {
            if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else if (!RenderSection.this.hasAllNeighbors()) {
               this.isCancelled.set(true);
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else if (this.isCancelled.get()) {
               return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
            } else {
               MeshData.SortState $$1 = this.compiledSection.transparencyState;
               if ($$1 != null && !this.compiledSection.isEmpty(RenderType.translucent())) {
                  VertexSorting $$2 = RenderSection.this.createVertexSorting();
                  ByteBufferBuilder.Result $$3 = $$1.buildSortedIndexBuffer(pSectionBufferBuilderPack.buffer(RenderType.translucent()), $$2);
                  if ($$3 == null) {
                     return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                  } else if (this.isCancelled.get()) {
                     $$3.close();
                     return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                  } else {
                     CompletableFuture<SectionRenderDispatcher.SectionTaskResult> $$4 = SectionRenderDispatcher.this.uploadSectionIndexBuffer(
                           $$3, RenderSection.this.getBuffer(RenderType.translucent())
                        )
                        .thenApply(p_297230_ -> SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                     return $$4.handle(
                        (p_301037_, p_300486_) -> {
                           if (p_300486_ != null && !(p_300486_ instanceof CancellationException) && !(p_300486_ instanceof InterruptedException)) {
                              Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_300486_, "Rendering section"));
                           }

                           return this.isCancelled.get()
                              ? SectionRenderDispatcher.SectionTaskResult.CANCELLED
                              : SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                        }
                     );
                  }
               } else {
                  return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
               }
            }
         }

         @Override
         public void cancel() {
            this.isCancelled.set(true);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static enum SectionTaskResult {
      SUCCESSFUL,
      CANCELLED;
   }
}
