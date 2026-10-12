package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;

public abstract class LayerLightSectionStorage<M extends DataLayerStorageMap<M>> {
   private final LightLayer layer;
   protected final LightChunkGetter chunkSource;
   protected final Long2ByteMap sectionStates = new Long2ByteOpenHashMap();
   private final LongSet columnsWithSources = new LongOpenHashSet();
   protected volatile M visibleSectionData;
   protected final M updatingSectionData;
   protected final LongSet changedSections = new LongOpenHashSet();
   protected final LongSet sectionsAffectedByLightUpdates = new LongOpenHashSet();
   protected final Long2ObjectMap<DataLayer> queuedSections = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap());
   private final LongSet columnsToRetainQueuedDataFor = new LongOpenHashSet();
   private final LongSet toRemove = new LongOpenHashSet();
   protected volatile boolean hasInconsistencies;

   protected LayerLightSectionStorage(LightLayer pLayer, LightChunkGetter pChunkSource, M pUpdatingSectionData) {
      this.layer = pLayer;
      this.chunkSource = pChunkSource;
      this.updatingSectionData = pUpdatingSectionData;
      this.visibleSectionData = pUpdatingSectionData.copy();
      this.visibleSectionData.disableCache();
      this.sectionStates.defaultReturnValue((byte)0);
   }

   protected boolean storingLightForSection(long pSectionPos) {
      return this.getDataLayer(pSectionPos, true) != null;
   }

   @Nullable
   protected DataLayer getDataLayer(long pSectionPos, boolean pCached) {
      return this.getDataLayer(pCached ? this.updatingSectionData : this.visibleSectionData, pSectionPos);
   }

   @Nullable
   protected DataLayer getDataLayer(M pMap, long pSectionPos) {
      return pMap.getLayer(pSectionPos);
   }

   @Nullable
   protected DataLayer getDataLayerToWrite(long pSectionPos) {
      DataLayer $$1 = this.updatingSectionData.getLayer(pSectionPos);
      if ($$1 == null) {
         return null;
      } else {
         if (this.changedSections.add(pSectionPos)) {
            $$1 = $$1.copy();
            this.updatingSectionData.setLayer(pSectionPos, $$1);
            this.updatingSectionData.clearCache();
         }

         return $$1;
      }
   }

   @Nullable
   public DataLayer getDataLayerData(long pSectionPos) {
      DataLayer $$1 = (DataLayer)this.queuedSections.get(pSectionPos);
      return $$1 != null ? $$1 : this.getDataLayer(pSectionPos, false);
   }

   protected abstract int getLightValue(long pLevelPos);

   protected int getStoredLevel(long pLevelPos) {
      long $$1 = SectionPos.blockToSection(pLevelPos);
      DataLayer $$2 = this.getDataLayer($$1, true);
      return $$2.get(
         SectionPos.sectionRelative(BlockPos.getX(pLevelPos)),
         SectionPos.sectionRelative(BlockPos.getY(pLevelPos)),
         SectionPos.sectionRelative(BlockPos.getZ(pLevelPos))
      );
   }

   protected void setStoredLevel(long pLevelPos, int pLightLevel) {
      long $$2 = SectionPos.blockToSection(pLevelPos);
      DataLayer $$3;
      if (this.changedSections.add($$2)) {
         $$3 = this.updatingSectionData.copyDataLayer($$2);
      } else {
         $$3 = this.getDataLayer($$2, true);
      }

      $$3.set(
         SectionPos.sectionRelative(BlockPos.getX(pLevelPos)),
         SectionPos.sectionRelative(BlockPos.getY(pLevelPos)),
         SectionPos.sectionRelative(BlockPos.getZ(pLevelPos)),
         pLightLevel
      );
      SectionPos.aroundAndAtBlockPos(pLevelPos, this.sectionsAffectedByLightUpdates::add);
   }

   protected void markSectionAndNeighborsAsAffected(long pSectionPos) {
      int $$1 = SectionPos.x(pSectionPos);
      int $$2 = SectionPos.y(pSectionPos);
      int $$3 = SectionPos.z(pSectionPos);

      for (int $$4 = -1; $$4 <= 1; $$4++) {
         for (int $$5 = -1; $$5 <= 1; $$5++) {
            for (int $$6 = -1; $$6 <= 1; $$6++) {
               this.sectionsAffectedByLightUpdates.add(SectionPos.asLong($$1 + $$5, $$2 + $$6, $$3 + $$4));
            }
         }
      }
   }

   protected DataLayer createDataLayer(long pSectionPos) {
      DataLayer $$1 = (DataLayer)this.queuedSections.get(pSectionPos);
      return $$1 != null ? $$1 : new DataLayer();
   }

   protected boolean hasInconsistencies() {
      return this.hasInconsistencies;
   }

   protected void markNewInconsistencies(LightEngine<M, ?> pLightEngine) {
      if (this.hasInconsistencies) {
         this.hasInconsistencies = false;
         LongIterator $$5 = this.toRemove.iterator();

         while ($$5.hasNext()) {
            long $$1 = (Long)$$5.next();
            DataLayer $$2 = (DataLayer)this.queuedSections.remove($$1);
            DataLayer $$3 = this.updatingSectionData.removeLayer($$1);
            if (this.columnsToRetainQueuedDataFor.contains(SectionPos.getZeroNode($$1))) {
               if ($$2 != null) {
                  this.queuedSections.put($$1, $$2);
               } else if ($$3 != null) {
                  this.queuedSections.put($$1, $$3);
               }
            }
         }

         this.updatingSectionData.clearCache();
         $$5 = this.toRemove.iterator();

         while ($$5.hasNext()) {
            long $$4 = (Long)$$5.next();
            this.onNodeRemoved($$4);
            this.changedSections.add($$4);
         }

         this.toRemove.clear();
         ObjectIterator<Entry<DataLayer>> $$5x = Long2ObjectMaps.fastIterator(this.queuedSections);

         while ($$5x.hasNext()) {
            Entry<DataLayer> $$6 = (Entry<DataLayer>)$$5x.next();
            long $$7 = $$6.getLongKey();
            if (this.storingLightForSection($$7)) {
               DataLayer $$8 = (DataLayer)$$6.getValue();
               if (this.updatingSectionData.getLayer($$7) != $$8) {
                  this.updatingSectionData.setLayer($$7, $$8);
                  this.changedSections.add($$7);
               }

               $$5x.remove();
            }
         }

         this.updatingSectionData.clearCache();
      }
   }

   protected void onNodeAdded(long pSectionPos) {
   }

   protected void onNodeRemoved(long pSectionPos) {
   }

   protected void setLightEnabled(long pSectionPos, boolean pLightEnabled) {
      if (pLightEnabled) {
         this.columnsWithSources.add(pSectionPos);
      } else {
         this.columnsWithSources.remove(pSectionPos);
      }
   }

   protected boolean lightOnInSection(long pSectionPos) {
      long $$1 = SectionPos.getZeroNode(pSectionPos);
      return this.columnsWithSources.contains($$1);
   }

   public void retainData(long pSectionColumnPos, boolean pRetain) {
      if (pRetain) {
         this.columnsToRetainQueuedDataFor.add(pSectionColumnPos);
      } else {
         this.columnsToRetainQueuedDataFor.remove(pSectionColumnPos);
      }
   }

   protected void queueSectionData(long pSectionPos, @Nullable DataLayer pData) {
      if (pData != null) {
         this.queuedSections.put(pSectionPos, pData);
         this.hasInconsistencies = true;
      } else {
         this.queuedSections.remove(pSectionPos);
      }
   }

   protected void updateSectionStatus(long pSectionPos, boolean pIsEmpty) {
      byte $$2 = this.sectionStates.get(pSectionPos);
      byte $$3 = LayerLightSectionStorage.SectionState.hasData($$2, !pIsEmpty);
      if ($$2 != $$3) {
         this.putSectionState(pSectionPos, $$3);
         int $$4 = pIsEmpty ? -1 : 1;

         for (int $$5 = -1; $$5 <= 1; $$5++) {
            for (int $$6 = -1; $$6 <= 1; $$6++) {
               for (int $$7 = -1; $$7 <= 1; $$7++) {
                  if ($$5 != 0 || $$6 != 0 || $$7 != 0) {
                     long $$8 = SectionPos.offset(pSectionPos, $$5, $$6, $$7);
                     byte $$9 = this.sectionStates.get($$8);
                     this.putSectionState(
                        $$8, LayerLightSectionStorage.SectionState.neighborCount($$9, LayerLightSectionStorage.SectionState.neighborCount($$9) + $$4)
                     );
                  }
               }
            }
         }
      }
   }

   protected void putSectionState(long pSectionPos, byte pSectionState) {
      if (pSectionState != 0) {
         if (this.sectionStates.put(pSectionPos, pSectionState) == 0) {
            this.initializeSection(pSectionPos);
         }
      } else if (this.sectionStates.remove(pSectionPos) != 0) {
         this.removeSection(pSectionPos);
      }
   }

   private void initializeSection(long pSectionPos) {
      if (!this.toRemove.remove(pSectionPos)) {
         this.updatingSectionData.setLayer(pSectionPos, this.createDataLayer(pSectionPos));
         this.changedSections.add(pSectionPos);
         this.onNodeAdded(pSectionPos);
         this.markSectionAndNeighborsAsAffected(pSectionPos);
         this.hasInconsistencies = true;
      }
   }

   private void removeSection(long pSectionPos) {
      this.toRemove.add(pSectionPos);
      this.hasInconsistencies = true;
   }

   protected void swapSectionMap() {
      if (!this.changedSections.isEmpty()) {
         M $$0 = this.updatingSectionData.copy();
         $$0.disableCache();
         this.visibleSectionData = $$0;
         this.changedSections.clear();
      }

      if (!this.sectionsAffectedByLightUpdates.isEmpty()) {
         LongIterator $$1 = this.sectionsAffectedByLightUpdates.iterator();

         while ($$1.hasNext()) {
            long $$2 = $$1.nextLong();
            this.chunkSource.onLightUpdate(this.layer, SectionPos.of($$2));
         }

         this.sectionsAffectedByLightUpdates.clear();
      }
   }

   public LayerLightSectionStorage.SectionType getDebugSectionType(long pSectionPos) {
      return LayerLightSectionStorage.SectionState.type(this.sectionStates.get(pSectionPos));
   }

   protected static class SectionState {
      public static final byte EMPTY = 0;
      private static final int MIN_NEIGHBORS = 0;
      private static final int MAX_NEIGHBORS = 26;
      private static final byte HAS_DATA_BIT = 32;
      private static final byte NEIGHBOR_COUNT_BITS = 31;

      public static byte hasData(byte pSectionState, boolean pHasData) {
         return (byte)(pHasData ? pSectionState | 32 : pSectionState & -33);
      }

      public static byte neighborCount(byte pSectionState, int pNeighborCount) {
         if (pNeighborCount >= 0 && pNeighborCount <= 26) {
            return (byte)(pSectionState & -32 | pNeighborCount & 31);
         } else {
            throw new IllegalArgumentException("Neighbor count was not within range [0; 26]");
         }
      }

      public static boolean hasData(byte pSectionState) {
         return (pSectionState & 32) != 0;
      }

      public static int neighborCount(byte pSectionState) {
         return pSectionState & 31;
      }

      public static LayerLightSectionStorage.SectionType type(byte pSectionState) {
         if (pSectionState == 0) {
            return LayerLightSectionStorage.SectionType.EMPTY;
         } else {
            return hasData(pSectionState) ? LayerLightSectionStorage.SectionType.LIGHT_AND_DATA : LayerLightSectionStorage.SectionType.LIGHT_ONLY;
         }
      }
   }

   public static enum SectionType {
      EMPTY("2"),
      LIGHT_ONLY("1"),
      LIGHT_AND_DATA("0");

      private final String display;

      private SectionType(final String pDisplay) {
         this.display = pDisplay;
      }

      public String display() {
         return this.display;
      }
   }
}
