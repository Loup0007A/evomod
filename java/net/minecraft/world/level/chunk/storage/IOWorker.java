package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StreamTagVisitor;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.util.Unit;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class IOWorker implements ChunkScanAccess, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final AtomicBoolean shutdownRequested = new AtomicBoolean();
   private final ProcessorMailbox<StrictQueue.IntRunnable> mailbox;
   private final RegionFileStorage storage;
   private final Map<ChunkPos, IOWorker.PendingStore> pendingWrites = Maps.newLinkedHashMap();
   private final Long2ObjectLinkedOpenHashMap<CompletableFuture<BitSet>> regionCacheForBlender = new Long2ObjectLinkedOpenHashMap();
   private static final int REGION_CACHE_SIZE = 1024;

   protected IOWorker(RegionStorageInfo pInfo, Path pFolder, boolean pSync) {
      this.storage = new RegionFileStorage(pInfo, pFolder, pSync);
      this.mailbox = new ProcessorMailbox<>(new StrictQueue.FixedPriorityQueue(IOWorker.Priority.values().length), Util.ioPool(), "IOWorker-" + pInfo.type());
   }

   public boolean isOldChunkAround(ChunkPos pChunkPos, int pRadius) {
      ChunkPos $$2 = new ChunkPos(pChunkPos.x - pRadius, pChunkPos.z - pRadius);
      ChunkPos $$3 = new ChunkPos(pChunkPos.x + pRadius, pChunkPos.z + pRadius);

      for (int $$4 = $$2.getRegionX(); $$4 <= $$3.getRegionX(); $$4++) {
         for (int $$5 = $$2.getRegionZ(); $$5 <= $$3.getRegionZ(); $$5++) {
            BitSet $$6 = this.getOrCreateOldDataForRegion($$4, $$5).join();
            if (!$$6.isEmpty()) {
               ChunkPos $$7 = ChunkPos.minFromRegion($$4, $$5);
               int $$8 = Math.max($$2.x - $$7.x, 0);
               int $$9 = Math.max($$2.z - $$7.z, 0);
               int $$10 = Math.min($$3.x - $$7.x, 31);
               int $$11 = Math.min($$3.z - $$7.z, 31);

               for (int $$12 = $$8; $$12 <= $$10; $$12++) {
                  for (int $$13 = $$9; $$13 <= $$11; $$13++) {
                     int $$14 = $$13 * 32 + $$12;
                     if ($$6.get($$14)) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private CompletableFuture<BitSet> getOrCreateOldDataForRegion(int pChunkX, int pChunkZ) {
      long $$2 = ChunkPos.asLong(pChunkX, pChunkZ);
      synchronized (this.regionCacheForBlender) {
         CompletableFuture<BitSet> $$3 = (CompletableFuture<BitSet>)this.regionCacheForBlender.getAndMoveToFirst($$2);
         if ($$3 == null) {
            $$3 = this.createOldDataForRegion(pChunkX, pChunkZ);
            this.regionCacheForBlender.putAndMoveToFirst($$2, $$3);
            if (this.regionCacheForBlender.size() > 1024) {
               this.regionCacheForBlender.removeLast();
            }
         }

         return $$3;
      }
   }

   private CompletableFuture<BitSet> createOldDataForRegion(int pChunkX, int pChunkZ) {
      return CompletableFuture.supplyAsync(() -> {
         ChunkPos $$2 = ChunkPos.minFromRegion(pChunkX, pChunkZ);
         ChunkPos $$3 = ChunkPos.maxFromRegion(pChunkX, pChunkZ);
         BitSet $$4 = new BitSet();
         ChunkPos.rangeClosed($$2, $$3).forEach(p_223480_ -> {
            CollectFields $$2x = new CollectFields(new FieldSelector(IntTag.TYPE, "DataVersion"), new FieldSelector(CompoundTag.TYPE, "blending_data"));

            try {
               this.scanChunk(p_223480_, $$2x).join();
            } catch (Exception var7) {
               LOGGER.warn("Failed to scan chunk {}", p_223480_, var7);
               return;
            }

            if ($$2x.getResult() instanceof CompoundTag $$5 && this.isOldChunk($$5)) {
               int $$6 = p_223480_.getRegionLocalZ() * 32 + p_223480_.getRegionLocalX();
               $$4.set($$6);
            }
         });
         return $$4;
      }, Util.backgroundExecutor());
   }

   private boolean isOldChunk(CompoundTag pChunkData) {
      return pChunkData.contains("DataVersion", 99) && pChunkData.getInt("DataVersion") >= 3441 ? pChunkData.contains("blending_data", 10) : true;
   }

   public CompletableFuture<Void> store(ChunkPos pChunkPos, @Nullable CompoundTag pChunkData) {
      return this.submitTask(() -> {
         IOWorker.PendingStore $$2 = this.pendingWrites.computeIfAbsent(pChunkPos, p_223488_ -> new IOWorker.PendingStore(pChunkData));
         $$2.data = pChunkData;
         return Either.left($$2.result);
      }).thenCompose(Function.identity());
   }

   public CompletableFuture<Optional<CompoundTag>> loadAsync(ChunkPos pChunkPos) {
      return this.submitTask(() -> {
         IOWorker.PendingStore $$1 = this.pendingWrites.get(pChunkPos);
         if ($$1 != null) {
            return Either.left(Optional.ofNullable($$1.copyData()));
         } else {
            try {
               CompoundTag $$2 = this.storage.read(pChunkPos);
               return Either.left(Optional.ofNullable($$2));
            } catch (Exception var4) {
               LOGGER.warn("Failed to read chunk {}", pChunkPos, var4);
               return Either.right(var4);
            }
         }
      });
   }

   public CompletableFuture<Void> synchronize(boolean pFlushStorage) {
      CompletableFuture<Void> $$1 = this.submitTask(
            () -> Either.left(
                  CompletableFuture.allOf(this.pendingWrites.values().stream().map(p_223475_ -> p_223475_.result).toArray(CompletableFuture[]::new))
               )
         )
         .thenCompose(Function.identity());
      return pFlushStorage ? $$1.thenCompose(p_182494_ -> this.submitTask(() -> {
            try {
               this.storage.flush();
               return Either.left(null);
            } catch (Exception var2x) {
               LOGGER.warn("Failed to synchronize chunks", var2x);
               return Either.right(var2x);
            }
         })) : $$1.thenCompose(p_223477_ -> this.submitTask(() -> Either.left(null)));
   }

   @Override
   public CompletableFuture<Void> scanChunk(ChunkPos pChunkPos, StreamTagVisitor pVisitor) {
      return this.submitTask(() -> {
         try {
            IOWorker.PendingStore $$2 = this.pendingWrites.get(pChunkPos);
            if ($$2 != null) {
               if ($$2.data != null) {
                  $$2.data.acceptAsRoot(pVisitor);
               }
            } else {
               this.storage.scanChunk(pChunkPos, pVisitor);
            }

            return Either.left(null);
         } catch (Exception var4) {
            LOGGER.warn("Failed to bulk scan chunk {}", pChunkPos, var4);
            return Either.right(var4);
         }
      });
   }

   private <T> CompletableFuture<T> submitTask(Supplier<Either<T, Exception>> pTask) {
      return this.mailbox.askEither(p_223483_ -> new StrictQueue.IntRunnable(IOWorker.Priority.FOREGROUND.ordinal(), () -> {
            if (!this.shutdownRequested.get()) {
               p_223483_.tell(pTask.get());
            }

            this.tellStorePending();
         }));
   }

   private void storePendingChunk() {
      if (!this.pendingWrites.isEmpty()) {
         Iterator<Entry<ChunkPos, IOWorker.PendingStore>> $$0 = this.pendingWrites.entrySet().iterator();
         Entry<ChunkPos, IOWorker.PendingStore> $$1 = $$0.next();
         $$0.remove();
         this.runStore($$1.getKey(), $$1.getValue());
         this.tellStorePending();
      }
   }

   private void tellStorePending() {
      this.mailbox.tell(new StrictQueue.IntRunnable(IOWorker.Priority.BACKGROUND.ordinal(), this::storePendingChunk));
   }

   private void runStore(ChunkPos pChunkPos, IOWorker.PendingStore pPendingStore) {
      try {
         this.storage.write(pChunkPos, pPendingStore.data);
         pPendingStore.result.complete(null);
      } catch (Exception var4) {
         LOGGER.error("Failed to store chunk {}", pChunkPos, var4);
         pPendingStore.result.completeExceptionally(var4);
      }
   }

   @Override
   public void close() throws IOException {
      if (this.shutdownRequested.compareAndSet(false, true)) {
         this.mailbox.ask(p_223467_ -> new StrictQueue.IntRunnable(IOWorker.Priority.SHUTDOWN.ordinal(), () -> p_223467_.tell(Unit.INSTANCE))).join();
         this.mailbox.close();

         try {
            this.storage.close();
         } catch (Exception var2) {
            LOGGER.error("Failed to close storage", var2);
         }
      }
   }

   public RegionStorageInfo storageInfo() {
      return this.storage.info();
   }

   static class PendingStore {
      @Nullable
      CompoundTag data;
      final CompletableFuture<Void> result = new CompletableFuture<>();

      public PendingStore(@Nullable CompoundTag pData) {
         this.data = pData;
      }

      @Nullable
      CompoundTag copyData() {
         CompoundTag $$0 = this.data;
         return $$0 == null ? null : $$0.copy();
      }
   }

   static enum Priority {
      FOREGROUND,
      BACKGROUND,
      SHUTDOWN;
   }
}
