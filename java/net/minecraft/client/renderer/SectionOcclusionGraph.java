package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.util.Mth;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SectionOcclusionGraph {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Direction[] DIRECTIONS = Direction.values();
   private static final int MINIMUM_ADVANCED_CULLING_DISTANCE = 60;
   private static final double CEILED_SECTION_DIAGONAL = Math.ceil(Math.sqrt(3.0) * 16.0);
   private boolean needsFullUpdate = true;
   @Nullable
   private Future<?> fullUpdateTask;
   @Nullable
   private ViewArea viewArea;
   private final AtomicReference<SectionOcclusionGraph.GraphState> currentGraph = new AtomicReference<>();
   private final AtomicReference<SectionOcclusionGraph.GraphEvents> nextGraphEvents = new AtomicReference<>();
   private final AtomicBoolean needsFrustumUpdate = new AtomicBoolean(false);

   public void waitAndReset(@Nullable ViewArea pViewArea) {
      if (this.fullUpdateTask != null) {
         try {
            this.fullUpdateTask.get();
            this.fullUpdateTask = null;
         } catch (Exception var3) {
            LOGGER.warn("Full update failed", var3);
         }
      }

      this.viewArea = pViewArea;
      if (pViewArea != null) {
         this.currentGraph.set(new SectionOcclusionGraph.GraphState(pViewArea.sections.length));
         this.invalidate();
      } else {
         this.currentGraph.set(null);
      }
   }

   public void invalidate() {
      this.needsFullUpdate = true;
   }

   public void addSectionsInFrustum(Frustum pFrustum, List<SectionRenderDispatcher.RenderSection> pSections) {
      for (SectionOcclusionGraph.Node $$2 : this.currentGraph.get().storage().renderSections) {
         if (pFrustum.isVisible($$2.section.getBoundingBox())) {
            pSections.add($$2.section);
         }
      }
   }

   public boolean consumeFrustumUpdate() {
      return this.needsFrustumUpdate.compareAndSet(true, false);
   }

   public void onChunkLoaded(ChunkPos pChunkPos) {
      SectionOcclusionGraph.GraphEvents $$1 = this.nextGraphEvents.get();
      if ($$1 != null) {
         this.addNeighbors($$1, pChunkPos);
      }

      SectionOcclusionGraph.GraphEvents $$2 = this.currentGraph.get().events;
      if ($$2 != $$1) {
         this.addNeighbors($$2, pChunkPos);
      }
   }

   public void onSectionCompiled(SectionRenderDispatcher.RenderSection pSection) {
      SectionOcclusionGraph.GraphEvents $$1 = this.nextGraphEvents.get();
      if ($$1 != null) {
         $$1.sectionsToPropagateFrom.add(pSection);
      }

      SectionOcclusionGraph.GraphEvents $$2 = this.currentGraph.get().events;
      if ($$2 != $$1) {
         $$2.sectionsToPropagateFrom.add(pSection);
      }
   }

   public void update(boolean pSmartCull, Camera pCamera, Frustum pFrustum, List<SectionRenderDispatcher.RenderSection> pSections) {
      Vec3 $$4 = pCamera.getPosition();
      if (this.needsFullUpdate && (this.fullUpdateTask == null || this.fullUpdateTask.isDone())) {
         this.scheduleFullUpdate(pSmartCull, pCamera, $$4);
      }

      this.runPartialUpdate(pSmartCull, pFrustum, pSections, $$4);
   }

   private void scheduleFullUpdate(boolean pSmartCull, Camera pCamera, Vec3 pCameraPosition) {
      this.needsFullUpdate = false;
      this.fullUpdateTask = Util.backgroundExecutor().submit(() -> {
         SectionOcclusionGraph.GraphState $$3 = new SectionOcclusionGraph.GraphState(this.viewArea.sections.length);
         this.nextGraphEvents.set($$3.events);
         Queue<SectionOcclusionGraph.Node> $$4 = Queues.newArrayDeque();
         this.initializeQueueForFullUpdate(pCamera, $$4);
         $$4.forEach(p_299757_ -> $$3.storage.sectionToNodeMap.put(p_299757_.section, p_299757_));
         this.runUpdates($$3.storage, pCameraPosition, $$4, pSmartCull, p_299279_ -> {
         });
         this.currentGraph.set($$3);
         this.nextGraphEvents.set(null);
         this.needsFrustumUpdate.set(true);
      });
   }

   private void runPartialUpdate(boolean pSmartCull, Frustum pFrustum, List<SectionRenderDispatcher.RenderSection> pSections, Vec3 pCameraPosition) {
      SectionOcclusionGraph.GraphState $$4 = this.currentGraph.get();
      this.queueSectionsWithNewNeighbors($$4);
      if (!$$4.events.sectionsToPropagateFrom.isEmpty()) {
         Queue<SectionOcclusionGraph.Node> $$5 = Queues.newArrayDeque();

         while (!$$4.events.sectionsToPropagateFrom.isEmpty()) {
            SectionRenderDispatcher.RenderSection $$6 = $$4.events.sectionsToPropagateFrom.poll();
            SectionOcclusionGraph.Node $$7 = $$4.storage.sectionToNodeMap.get($$6);
            if ($$7 != null && $$7.section == $$6) {
               $$5.add($$7);
            }
         }

         Frustum $$8 = LevelRenderer.offsetFrustum(pFrustum);
         Consumer<SectionRenderDispatcher.RenderSection> $$9 = p_298102_ -> {
            if ($$8.isVisible(p_298102_.getBoundingBox())) {
               pSections.add(p_298102_);
            }
         };
         this.runUpdates($$4.storage, pCameraPosition, $$5, pSmartCull, $$9);
      }
   }

   private void queueSectionsWithNewNeighbors(SectionOcclusionGraph.GraphState pGraphState) {
      LongIterator $$1 = pGraphState.events.chunksWhichReceivedNeighbors.iterator();

      while ($$1.hasNext()) {
         long $$2 = $$1.nextLong();
         List<SectionRenderDispatcher.RenderSection> $$3 = (List<SectionRenderDispatcher.RenderSection>)pGraphState.storage.chunksWaitingForNeighbors.get($$2);
         if ($$3 != null && $$3.get(0).hasAllNeighbors()) {
            pGraphState.events.sectionsToPropagateFrom.addAll($$3);
            pGraphState.storage.chunksWaitingForNeighbors.remove($$2);
         }
      }

      pGraphState.events.chunksWhichReceivedNeighbors.clear();
   }

   private void addNeighbors(SectionOcclusionGraph.GraphEvents pGraphEvents, ChunkPos pChunkPos) {
      pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x - 1, pChunkPos.z));
      pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x, pChunkPos.z - 1));
      pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x + 1, pChunkPos.z));
      pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x, pChunkPos.z + 1));
   }

   private void initializeQueueForFullUpdate(Camera pCamera, Queue<SectionOcclusionGraph.Node> pNodeQueue) {
      int $$2 = 16;
      Vec3 $$3 = pCamera.getPosition();
      BlockPos $$4 = pCamera.getBlockPosition();
      SectionRenderDispatcher.RenderSection $$5 = this.viewArea.getRenderSectionAt($$4);
      if ($$5 == null) {
         LevelHeightAccessor $$6 = this.viewArea.getLevelHeightAccessor();
         boolean $$7 = $$4.getY() > $$6.getMinBuildHeight();
         int $$8 = $$7 ? $$6.getMaxBuildHeight() - 8 : $$6.getMinBuildHeight() + 8;
         int $$9 = Mth.floor($$3.x / 16.0) * 16;
         int $$10 = Mth.floor($$3.z / 16.0) * 16;
         int $$11 = this.viewArea.getViewDistance();
         List<SectionOcclusionGraph.Node> $$12 = Lists.newArrayList();

         for (int $$13 = -$$11; $$13 <= $$11; $$13++) {
            for (int $$14 = -$$11; $$14 <= $$11; $$14++) {
               SectionRenderDispatcher.RenderSection $$15 = this.viewArea
                  .getRenderSectionAt(new BlockPos($$9 + SectionPos.sectionToBlockCoord($$13, 8), $$8, $$10 + SectionPos.sectionToBlockCoord($$14, 8)));
               if ($$15 != null && this.isInViewDistance($$4, $$15.getOrigin())) {
                  Direction $$16 = $$7 ? Direction.DOWN : Direction.UP;
                  SectionOcclusionGraph.Node $$17 = new SectionOcclusionGraph.Node($$15, $$16, 0);
                  $$17.setDirections($$17.directions, $$16);
                  if ($$13 > 0) {
                     $$17.setDirections($$17.directions, Direction.EAST);
                  } else if ($$13 < 0) {
                     $$17.setDirections($$17.directions, Direction.WEST);
                  }

                  if ($$14 > 0) {
                     $$17.setDirections($$17.directions, Direction.SOUTH);
                  } else if ($$14 < 0) {
                     $$17.setDirections($$17.directions, Direction.NORTH);
                  }

                  $$12.add($$17);
               }
            }
         }

         $$12.sort(Comparator.comparingDouble(p_299847_ -> $$4.distSqr(p_299847_.section.getOrigin().offset(8, 8, 8))));
         pNodeQueue.addAll($$12);
      } else {
         pNodeQueue.add(new SectionOcclusionGraph.Node($$5, null, 0));
      }
   }

   private void runUpdates(
      SectionOcclusionGraph.GraphStorage pGraphStorage,
      Vec3 pCameraPosition,
      Queue<SectionOcclusionGraph.Node> pNodeQueue,
      boolean pSmartCull,
      Consumer<SectionRenderDispatcher.RenderSection> pSections
   ) {
      int $$5 = 16;
      BlockPos $$6 = new BlockPos(Mth.floor(pCameraPosition.x / 16.0) * 16, Mth.floor(pCameraPosition.y / 16.0) * 16, Mth.floor(pCameraPosition.z / 16.0) * 16);
      BlockPos $$7 = $$6.offset(8, 8, 8);

      while (!pNodeQueue.isEmpty()) {
         SectionOcclusionGraph.Node $$8 = pNodeQueue.poll();
         SectionRenderDispatcher.RenderSection $$9 = $$8.section;
         if (pGraphStorage.renderSections.add($$8)) {
            pSections.accept($$8.section);
         }

         boolean $$10 = Math.abs($$9.getOrigin().getX() - $$6.getX()) > 60
            || Math.abs($$9.getOrigin().getY() - $$6.getY()) > 60
            || Math.abs($$9.getOrigin().getZ() - $$6.getZ()) > 60;

         for (Direction $$11 : DIRECTIONS) {
            SectionRenderDispatcher.RenderSection $$12 = this.getRelativeFrom($$6, $$9, $$11);
            if ($$12 != null && (!pSmartCull || !$$8.hasDirection($$11.getOpposite()))) {
               if (pSmartCull && $$8.hasSourceDirections()) {
                  SectionRenderDispatcher.CompiledSection $$13 = $$9.getCompiled();
                  boolean $$14 = false;

                  for (int $$15 = 0; $$15 < DIRECTIONS.length; $$15++) {
                     if ($$8.hasSourceDirection($$15) && $$13.facesCanSeeEachother(DIRECTIONS[$$15].getOpposite(), $$11)) {
                        $$14 = true;
                        break;
                     }
                  }

                  if (!$$14) {
                     continue;
                  }
               }

               if (pSmartCull && $$10) {
                  BlockPos $$16 = $$12.getOrigin();
                  BlockPos $$17 = $$16.offset(
                     ($$11.getAxis() == Direction.Axis.X ? $$7.getX() <= $$16.getX() : $$7.getX() >= $$16.getX()) ? 0 : 16,
                     ($$11.getAxis() == Direction.Axis.Y ? $$7.getY() <= $$16.getY() : $$7.getY() >= $$16.getY()) ? 0 : 16,
                     ($$11.getAxis() == Direction.Axis.Z ? $$7.getZ() <= $$16.getZ() : $$7.getZ() >= $$16.getZ()) ? 0 : 16
                  );
                  Vec3 $$18 = new Vec3((double)$$17.getX(), (double)$$17.getY(), (double)$$17.getZ());
                  Vec3 $$19 = pCameraPosition.subtract($$18).normalize().scale(CEILED_SECTION_DIAGONAL);
                  boolean $$20 = true;

                  while (pCameraPosition.subtract($$18).lengthSqr() > 3600.0) {
                     $$18 = $$18.add($$19);
                     LevelHeightAccessor $$21 = this.viewArea.getLevelHeightAccessor();
                     if ($$18.y > (double)$$21.getMaxBuildHeight() || $$18.y < (double)$$21.getMinBuildHeight()) {
                        break;
                     }

                     SectionRenderDispatcher.RenderSection $$22 = this.viewArea.getRenderSectionAt(BlockPos.containing($$18.x, $$18.y, $$18.z));
                     if ($$22 == null || pGraphStorage.sectionToNodeMap.get($$22) == null) {
                        $$20 = false;
                        break;
                     }
                  }

                  if (!$$20) {
                     continue;
                  }
               }

               SectionOcclusionGraph.Node $$23 = pGraphStorage.sectionToNodeMap.get($$12);
               if ($$23 != null) {
                  $$23.addSourceDirection($$11);
               } else {
                  SectionOcclusionGraph.Node $$24 = new SectionOcclusionGraph.Node($$12, $$11, $$8.step + 1);
                  $$24.setDirections($$8.directions, $$11);
                  if ($$12.hasAllNeighbors()) {
                     pNodeQueue.add($$24);
                     pGraphStorage.sectionToNodeMap.put($$12, $$24);
                  } else if (this.isInViewDistance($$6, $$12.getOrigin())) {
                     pGraphStorage.sectionToNodeMap.put($$12, $$24);
                     ((List)pGraphStorage.chunksWaitingForNeighbors.computeIfAbsent(ChunkPos.asLong($$12.getOrigin()), p_298371_ -> new ArrayList())).add($$12);
                  }
               }
            }
         }
      }
   }

   private boolean isInViewDistance(BlockPos pPos, BlockPos pOrigin) {
      int $$2 = SectionPos.blockToSectionCoord(pPos.getX());
      int $$3 = SectionPos.blockToSectionCoord(pPos.getZ());
      int $$4 = SectionPos.blockToSectionCoord(pOrigin.getX());
      int $$5 = SectionPos.blockToSectionCoord(pOrigin.getZ());
      return ChunkTrackingView.isInViewDistance($$2, $$3, this.viewArea.getViewDistance(), $$4, $$5);
   }

   @Nullable
   private SectionRenderDispatcher.RenderSection getRelativeFrom(BlockPos pPos, SectionRenderDispatcher.RenderSection pSection, Direction pDirection) {
      BlockPos $$3 = pSection.getRelativeOrigin(pDirection);
      if (!this.isInViewDistance(pPos, $$3)) {
         return null;
      } else {
         return Mth.abs(pPos.getY() - $$3.getY()) > this.viewArea.getViewDistance() * 16 ? null : this.viewArea.getRenderSectionAt($$3);
      }
   }

   @Nullable
   @VisibleForDebug
   protected SectionOcclusionGraph.Node getNode(SectionRenderDispatcher.RenderSection pSection) {
      return this.currentGraph.get().storage.sectionToNodeMap.get(pSection);
   }

   @OnlyIn(Dist.CLIENT)
   static record GraphEvents(LongSet chunksWhichReceivedNeighbors, BlockingQueue<SectionRenderDispatcher.RenderSection> sectionsToPropagateFrom) {
      public GraphEvents() {
         this(new LongOpenHashSet(), new LinkedBlockingQueue<>());
      }
   }

   @OnlyIn(Dist.CLIENT)
   static record GraphState(SectionOcclusionGraph.GraphStorage storage, SectionOcclusionGraph.GraphEvents events) {
      public GraphState(int pSize) {
         this(new SectionOcclusionGraph.GraphStorage(pSize), new SectionOcclusionGraph.GraphEvents());
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class GraphStorage {
      public final SectionOcclusionGraph.SectionToNodeMap sectionToNodeMap;
      public final LinkedHashSet<SectionOcclusionGraph.Node> renderSections;
      public final Long2ObjectMap<List<SectionRenderDispatcher.RenderSection>> chunksWaitingForNeighbors;

      public GraphStorage(int pSize) {
         this.sectionToNodeMap = new SectionOcclusionGraph.SectionToNodeMap(pSize);
         this.renderSections = new LinkedHashSet<>(pSize);
         this.chunksWaitingForNeighbors = new Long2ObjectOpenHashMap();
      }
   }

   @OnlyIn(Dist.CLIENT)
   @VisibleForDebug
   protected static class Node {
      @VisibleForDebug
      protected final SectionRenderDispatcher.RenderSection section;
      private byte sourceDirections;
      byte directions;
      @VisibleForDebug
      protected final int step;

      Node(SectionRenderDispatcher.RenderSection pSection, @Nullable Direction pSourceDirection, int pStep) {
         this.section = pSection;
         if (pSourceDirection != null) {
            this.addSourceDirection(pSourceDirection);
         }

         this.step = pStep;
      }

      void setDirections(byte pCurrentValue, Direction pDirection) {
         this.directions = (byte)(this.directions | pCurrentValue | 1 << pDirection.ordinal());
      }

      boolean hasDirection(Direction pDirection) {
         return (this.directions & 1 << pDirection.ordinal()) > 0;
      }

      void addSourceDirection(Direction pSourceDirection) {
         this.sourceDirections = (byte)(this.sourceDirections | this.sourceDirections | 1 << pSourceDirection.ordinal());
      }

      @VisibleForDebug
      protected boolean hasSourceDirection(int pDirection) {
         return (this.sourceDirections & 1 << pDirection) > 0;
      }

      boolean hasSourceDirections() {
         return this.sourceDirections != 0;
      }

      @Override
      public int hashCode() {
         return this.section.getOrigin().hashCode();
      }

      @Override
      public boolean equals(Object pOther) {
         return !(pOther instanceof SectionOcclusionGraph.Node $$1) ? false : this.section.getOrigin().equals($$1.section.getOrigin());
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class SectionToNodeMap {
      private final SectionOcclusionGraph.Node[] nodes;

      SectionToNodeMap(int pSize) {
         this.nodes = new SectionOcclusionGraph.Node[pSize];
      }

      public void put(SectionRenderDispatcher.RenderSection pSection, SectionOcclusionGraph.Node pNode) {
         this.nodes[pSection.index] = pNode;
      }

      @Nullable
      public SectionOcclusionGraph.Node get(SectionRenderDispatcher.RenderSection pSection) {
         int $$1 = pSection.index;
         return $$1 >= 0 && $$1 < this.nodes.length ? this.nodes[$$1] : null;
      }
   }
}
