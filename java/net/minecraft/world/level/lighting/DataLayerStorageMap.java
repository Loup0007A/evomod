package net.minecraft.world.level.lighting;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.world.level.chunk.DataLayer;

public abstract class DataLayerStorageMap<M extends DataLayerStorageMap<M>> {
   private static final int CACHE_SIZE = 2;
   private final long[] lastSectionKeys = new long[2];
   private final DataLayer[] lastSections = new DataLayer[2];
   private boolean cacheEnabled;
   protected final Long2ObjectOpenHashMap<DataLayer> map;

   protected DataLayerStorageMap(Long2ObjectOpenHashMap<DataLayer> pMap) {
      this.map = pMap;
      this.clearCache();
      this.cacheEnabled = true;
   }

   public abstract M copy();

   public DataLayer copyDataLayer(long pIndex) {
      DataLayer $$1 = ((DataLayer)this.map.get(pIndex)).copy();
      this.map.put(pIndex, $$1);
      this.clearCache();
      return $$1;
   }

   public boolean hasLayer(long pSectionPos) {
      return this.map.containsKey(pSectionPos);
   }

   @Nullable
   public DataLayer getLayer(long pSectionPos) {
      if (this.cacheEnabled) {
         for (int $$1 = 0; $$1 < 2; $$1++) {
            if (pSectionPos == this.lastSectionKeys[$$1]) {
               return this.lastSections[$$1];
            }
         }
      }

      DataLayer $$2 = (DataLayer)this.map.get(pSectionPos);
      if ($$2 == null) {
         return null;
      } else {
         if (this.cacheEnabled) {
            for (int $$3 = 1; $$3 > 0; $$3--) {
               this.lastSectionKeys[$$3] = this.lastSectionKeys[$$3 - 1];
               this.lastSections[$$3] = this.lastSections[$$3 - 1];
            }

            this.lastSectionKeys[0] = pSectionPos;
            this.lastSections[0] = $$2;
         }

         return $$2;
      }
   }

   @Nullable
   public DataLayer removeLayer(long pSectionPos) {
      return (DataLayer)this.map.remove(pSectionPos);
   }

   public void setLayer(long pSectionPos, DataLayer pArray) {
      this.map.put(pSectionPos, pArray);
   }

   public void clearCache() {
      for (int $$0 = 0; $$0 < 2; $$0++) {
         this.lastSectionKeys[$$0] = Long.MAX_VALUE;
         this.lastSections[$$0] = null;
      }
   }

   public void disableCache() {
      this.cacheEnabled = false;
   }
}
