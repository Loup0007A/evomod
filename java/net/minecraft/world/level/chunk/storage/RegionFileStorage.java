package net.minecraft.world.level.chunk.storage;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.util.ExceptionCollector;
import net.minecraft.world.level.ChunkPos;

public final class RegionFileStorage implements AutoCloseable {
   public static final String ANVIL_EXTENSION = ".mca";
   private static final int MAX_CACHE_SIZE = 256;
   private final Long2ObjectLinkedOpenHashMap<RegionFile> regionCache = new Long2ObjectLinkedOpenHashMap();
   private final RegionStorageInfo info;
   private final Path folder;
   private final boolean sync;

   RegionFileStorage(RegionStorageInfo pInfo, Path pFolder, boolean pSync) {
      this.folder = pFolder;
      this.sync = pSync;
      this.info = pInfo;
   }

   private RegionFile getRegionFile(ChunkPos pChunkPos) throws IOException {
      long $$1 = ChunkPos.asLong(pChunkPos.getRegionX(), pChunkPos.getRegionZ());
      RegionFile $$2 = (RegionFile)this.regionCache.getAndMoveToFirst($$1);
      if ($$2 != null) {
         return $$2;
      } else {
         if (this.regionCache.size() >= 256) {
            ((RegionFile)this.regionCache.removeLast()).close();
         }

         FileUtil.createDirectoriesSafe(this.folder);
         Path $$3 = this.folder.resolve("r." + pChunkPos.getRegionX() + "." + pChunkPos.getRegionZ() + ".mca");
         RegionFile $$4 = new RegionFile(this.info, $$3, this.folder, this.sync);
         this.regionCache.putAndMoveToFirst($$1, $$4);
         return $$4;
      }
   }

   @Nullable
   public CompoundTag read(ChunkPos pChunkPos) throws IOException {
      RegionFile $$1 = this.getRegionFile(pChunkPos);

      CompoundTag var4;
      try (DataInputStream $$2 = $$1.getChunkDataInputStream(pChunkPos)) {
         if ($$2 == null) {
            return null;
         }

         var4 = NbtIo.read($$2);
      }

      return var4;
   }

   public void scanChunk(ChunkPos pChunkPos, StreamTagVisitor pVisitor) throws IOException {
      RegionFile $$2 = this.getRegionFile(pChunkPos);

      try (DataInputStream $$3 = $$2.getChunkDataInputStream(pChunkPos)) {
         if ($$3 != null) {
            NbtIo.parse($$3, pVisitor, NbtAccounter.unlimitedHeap());
         }
      }
   }

   protected void write(ChunkPos pChunkPos, @Nullable CompoundTag pChunkData) throws IOException {
      RegionFile $$2 = this.getRegionFile(pChunkPos);
      if (pChunkData == null) {
         $$2.clear(pChunkPos);
      } else {
         try (DataOutputStream $$3 = $$2.getChunkDataOutputStream(pChunkPos)) {
            NbtIo.write(pChunkData, $$3);
         }
      }
   }

   @Override
   public void close() throws IOException {
      ExceptionCollector<IOException> $$0 = new ExceptionCollector<>();
      ObjectIterator var2 = this.regionCache.values().iterator();

      while (var2.hasNext()) {
         RegionFile $$1 = (RegionFile)var2.next();

         try {
            $$1.close();
         } catch (IOException var5) {
            $$0.add(var5);
         }
      }

      $$0.throwIfPresent();
   }

   public void flush() throws IOException {
      ObjectIterator var1 = this.regionCache.values().iterator();

      while (var1.hasNext()) {
         RegionFile $$0 = (RegionFile)var1.next();
         $$0.flush();
      }
   }

   public RegionStorageInfo info() {
      return this.info;
   }
}
