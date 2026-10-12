package net.minecraft.world.level.chunk.storage;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.MapCodec;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.LegacyStructureDataHandler;
import net.minecraft.world.level.storage.DimensionDataStorage;

public class ChunkStorage implements AutoCloseable {
   public static final int LAST_MONOLYTH_STRUCTURE_DATA_VERSION = 1493;
   private final IOWorker worker;
   protected final DataFixer fixerUpper;
   @Nullable
   private volatile LegacyStructureDataHandler legacyStructureHandler;

   public ChunkStorage(RegionStorageInfo pInfo, Path pFolder, DataFixer pFixerUpper, boolean pSync) {
      this.fixerUpper = pFixerUpper;
      this.worker = new IOWorker(pInfo, pFolder, pSync);
   }

   public boolean isOldChunkAround(ChunkPos pPos, int pRadius) {
      return this.worker.isOldChunkAround(pPos, pRadius);
   }

   public CompoundTag upgradeChunkTag(
      ResourceKey<Level> pLevelKey,
      Supplier<DimensionDataStorage> pStorage,
      CompoundTag pChunkData,
      Optional<ResourceKey<MapCodec<? extends ChunkGenerator>>> pChunkGeneratorKey
   ) {
      int $$4 = getVersion(pChunkData);
      if ($$4 == SharedConstants.getCurrentVersion().getDataVersion().getVersion()) {
         return pChunkData;
      } else {
         try {
            if ($$4 < 1493) {
               pChunkData = DataFixTypes.CHUNK.update(this.fixerUpper, pChunkData, $$4, 1493);
               if (pChunkData.getCompound("Level").getBoolean("hasLegacyStructureData")) {
                  LegacyStructureDataHandler $$5 = this.getLegacyStructureHandler(pLevelKey, pStorage);
                  pChunkData = $$5.updateFromLegacy(pChunkData);
               }
            }

            injectDatafixingContext(pChunkData, pLevelKey, pChunkGeneratorKey);
            pChunkData = DataFixTypes.CHUNK.updateToCurrentVersion(this.fixerUpper, pChunkData, Math.max(1493, $$4));
            removeDatafixingContext(pChunkData);
            NbtUtils.addCurrentDataVersion(pChunkData);
            return pChunkData;
         } catch (Exception var9) {
            CrashReport $$7 = CrashReport.forThrowable(var9, "Updated chunk");
            CrashReportCategory $$8 = $$7.addCategory("Updated chunk details");
            $$8.setDetail("Data version", $$4);
            throw new ReportedException($$7);
         }
      }
   }

   private LegacyStructureDataHandler getLegacyStructureHandler(ResourceKey<Level> pLevel, Supplier<DimensionDataStorage> pStorage) {
      LegacyStructureDataHandler $$2 = this.legacyStructureHandler;
      if ($$2 == null) {
         synchronized (this) {
            $$2 = this.legacyStructureHandler;
            if ($$2 == null) {
               this.legacyStructureHandler = $$2 = LegacyStructureDataHandler.getLegacyStructureHandler(pLevel, pStorage.get());
            }
         }
      }

      return $$2;
   }

   public static void injectDatafixingContext(
      CompoundTag pChunkData, ResourceKey<Level> pLevelKey, Optional<ResourceKey<MapCodec<? extends ChunkGenerator>>> pChunkGeneratorKey
   ) {
      CompoundTag $$3 = new CompoundTag();
      $$3.putString("dimension", pLevelKey.location().toString());
      pChunkGeneratorKey.ifPresent(p_196917_ -> $$3.putString("generator", p_196917_.location().toString()));
      pChunkData.put("__context", $$3);
   }

   private static void removeDatafixingContext(CompoundTag pTag) {
      pTag.remove("__context");
   }

   public static int getVersion(CompoundTag pChunkData) {
      return NbtUtils.getDataVersion(pChunkData, -1);
   }

   public CompletableFuture<Optional<CompoundTag>> read(ChunkPos pChunkPos) {
      return this.worker.loadAsync(pChunkPos);
   }

   public CompletableFuture<Void> write(ChunkPos pChunkPos, CompoundTag pData) {
      this.handleLegacyStructureIndex(pChunkPos);
      return this.worker.store(pChunkPos, pData);
   }

   protected void handleLegacyStructureIndex(ChunkPos pChunkPos) {
      if (this.legacyStructureHandler != null) {
         this.legacyStructureHandler.removeIndex(pChunkPos.toLong());
      }
   }

   public void flushWorker() {
      this.worker.synchronize(true).join();
   }

   @Override
   public void close() throws IOException {
      this.worker.close();
   }

   public ChunkScanAccess chunkScanner() {
      return this.worker;
   }

   protected RegionStorageInfo storageInfo() {
      return this.worker.storageInfo();
   }
}
