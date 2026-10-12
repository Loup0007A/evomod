package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;

public class SkyLightSectionStorage extends LayerLightSectionStorage<SkyLightSectionStorage.SkyDataLayerStorageMap> {
   protected SkyLightSectionStorage(LightChunkGetter pChunkSource) {
      super(
         LightLayer.SKY,
         pChunkSource,
         new SkyLightSectionStorage.SkyDataLayerStorageMap(new Long2ObjectOpenHashMap(), new Long2IntOpenHashMap(), Integer.MAX_VALUE)
      );
   }

   @Override
   protected int getLightValue(long pLevelPos) {
      return this.getLightValue(pLevelPos, false);
   }

   protected int getLightValue(long pPackedPos, boolean pUpdateAll) {
      long $$2 = SectionPos.blockToSection(pPackedPos);
      int $$3 = SectionPos.y($$2);
      SkyLightSectionStorage.SkyDataLayerStorageMap $$4 = pUpdateAll ? this.updatingSectionData : this.visibleSectionData;
      int $$5 = $$4.topSections.get(SectionPos.getZeroNode($$2));
      if ($$5 != $$4.currentLowestY && $$3 < $$5) {
         DataLayer $$6 = this.getDataLayer($$4, $$2);
         if ($$6 == null) {
            for (pPackedPos = BlockPos.getFlatIndex(pPackedPos); $$6 == null; $$6 = this.getDataLayer($$4, $$2)) {
               if (++$$3 >= $$5) {
                  return 15;
               }

               $$2 = SectionPos.offset($$2, Direction.UP);
            }
         }

         return $$6.get(
            SectionPos.sectionRelative(BlockPos.getX(pPackedPos)),
            SectionPos.sectionRelative(BlockPos.getY(pPackedPos)),
            SectionPos.sectionRelative(BlockPos.getZ(pPackedPos))
         );
      } else {
         return pUpdateAll && !this.lightOnInSection($$2) ? 0 : 15;
      }
   }

   @Override
   protected void onNodeAdded(long pSectionPos) {
      int $$1 = SectionPos.y(pSectionPos);
      if (this.updatingSectionData.currentLowestY > $$1) {
         this.updatingSectionData.currentLowestY = $$1;
         this.updatingSectionData.topSections.defaultReturnValue(this.updatingSectionData.currentLowestY);
      }

      long $$2 = SectionPos.getZeroNode(pSectionPos);
      int $$3 = this.updatingSectionData.topSections.get($$2);
      if ($$3 < $$1 + 1) {
         this.updatingSectionData.topSections.put($$2, $$1 + 1);
      }
   }

   @Override
   protected void onNodeRemoved(long pSectionPos) {
      long $$1 = SectionPos.getZeroNode(pSectionPos);
      int $$2 = SectionPos.y(pSectionPos);
      if (this.updatingSectionData.topSections.get($$1) == $$2 + 1) {
         long $$3;
         for ($$3 = pSectionPos; !this.storingLightForSection($$3) && this.hasLightDataAtOrBelow($$2); $$3 = SectionPos.offset($$3, Direction.DOWN)) {
            $$2--;
         }

         if (this.storingLightForSection($$3)) {
            this.updatingSectionData.topSections.put($$1, $$2 + 1);
         } else {
            this.updatingSectionData.topSections.remove($$1);
         }
      }
   }

   @Override
   protected DataLayer createDataLayer(long pSectionPos) {
      DataLayer $$1 = (DataLayer)this.queuedSections.get(pSectionPos);
      if ($$1 != null) {
         return $$1;
      } else {
         int $$2 = this.updatingSectionData.topSections.get(SectionPos.getZeroNode(pSectionPos));
         if ($$2 != this.updatingSectionData.currentLowestY && SectionPos.y(pSectionPos) < $$2) {
            long $$3 = SectionPos.offset(pSectionPos, Direction.UP);

            DataLayer $$4;
            while (($$4 = this.getDataLayer($$3, true)) == null) {
               $$3 = SectionPos.offset($$3, Direction.UP);
            }

            return repeatFirstLayer($$4);
         } else {
            return this.lightOnInSection(pSectionPos) ? new DataLayer(15) : new DataLayer();
         }
      }
   }

   private static DataLayer repeatFirstLayer(DataLayer pDataLayer) {
      if (pDataLayer.isDefinitelyHomogenous()) {
         return pDataLayer.copy();
      } else {
         byte[] $$1 = pDataLayer.getData();
         byte[] $$2 = new byte[2048];

         for (int $$3 = 0; $$3 < 16; $$3++) {
            System.arraycopy($$1, 0, $$2, $$3 * 128, 128);
         }

         return new DataLayer($$2);
      }
   }

   protected boolean hasLightDataAtOrBelow(int pY) {
      return pY >= this.updatingSectionData.currentLowestY;
   }

   protected boolean isAboveData(long pSectionPos) {
      long $$1 = SectionPos.getZeroNode(pSectionPos);
      int $$2 = this.updatingSectionData.topSections.get($$1);
      return $$2 == this.updatingSectionData.currentLowestY || SectionPos.y(pSectionPos) >= $$2;
   }

   protected int getTopSectionY(long pSectionPos) {
      return this.updatingSectionData.topSections.get(pSectionPos);
   }

   protected int getBottomSectionY() {
      return this.updatingSectionData.currentLowestY;
   }

   protected static final class SkyDataLayerStorageMap extends DataLayerStorageMap<SkyLightSectionStorage.SkyDataLayerStorageMap> {
      int currentLowestY;
      final Long2IntOpenHashMap topSections;

      public SkyDataLayerStorageMap(Long2ObjectOpenHashMap<DataLayer> pMap, Long2IntOpenHashMap pTopSections, int pCurrentLowestY) {
         super(pMap);
         this.topSections = pTopSections;
         pTopSections.defaultReturnValue(pCurrentLowestY);
         this.currentLowestY = pCurrentLowestY;
      }

      public SkyLightSectionStorage.SkyDataLayerStorageMap copy() {
         return new SkyLightSectionStorage.SkyDataLayerStorageMap(this.map.clone(), this.topSections.clone(), this.currentLowestY);
      }
   }
}
