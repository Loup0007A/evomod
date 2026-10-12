package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import org.slf4j.Logger;

public class SectionStorage<R> implements AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String SECTIONS_TAG = "Sections";
   private final SimpleRegionStorage simpleRegionStorage;
   private final Long2ObjectMap<Optional<R>> storage = new Long2ObjectOpenHashMap();
   private final LongLinkedOpenHashSet dirty = new LongLinkedOpenHashSet();
   private final Function<Runnable, Codec<R>> codec;
   private final Function<Runnable, R> factory;
   private final RegistryAccess registryAccess;
   private final ChunkIOErrorReporter errorReporter;
   protected final LevelHeightAccessor levelHeightAccessor;

   public SectionStorage(
      SimpleRegionStorage pSimpleRegionStorage,
      Function<Runnable, Codec<R>> pCodec,
      Function<Runnable, R> pFactory,
      RegistryAccess pRegistryAccess,
      ChunkIOErrorReporter pErrorReporter,
      LevelHeightAccessor pLevelHeightAccessor
   ) {
      this.simpleRegionStorage = pSimpleRegionStorage;
      this.codec = pCodec;
      this.factory = pFactory;
      this.registryAccess = pRegistryAccess;
      this.errorReporter = pErrorReporter;
      this.levelHeightAccessor = pLevelHeightAccessor;
   }

   protected void tick(BooleanSupplier pAheadOfTime) {
      while (this.hasWork() && pAheadOfTime.getAsBoolean()) {
         ChunkPos $$1 = SectionPos.of(this.dirty.firstLong()).chunk();
         this.writeColumn($$1);
      }
   }

   public boolean hasWork() {
      return !this.dirty.isEmpty();
   }

   @Nullable
   protected Optional<R> get(long pSectionKey) {
      return (Optional<R>)this.storage.get(pSectionKey);
   }

   protected Optional<R> getOrLoad(long pSectionKey) {
      if (this.outsideStoredRange(pSectionKey)) {
         return Optional.empty();
      } else {
         Optional<R> $$1 = this.get(pSectionKey);
         if ($$1 != null) {
            return $$1;
         } else {
            this.readColumn(SectionPos.of(pSectionKey).chunk());
            $$1 = this.get(pSectionKey);
            if ($$1 == null) {
               throw (IllegalStateException)Util.pauseInIde(new IllegalStateException());
            } else {
               return $$1;
            }
         }
      }
   }

   protected boolean outsideStoredRange(long pSectionKey) {
      int $$1 = SectionPos.sectionToBlockCoord(SectionPos.y(pSectionKey));
      return this.levelHeightAccessor.isOutsideBuildHeight($$1);
   }

   protected R getOrCreate(long pSectionKey) {
      if (this.outsideStoredRange(pSectionKey)) {
         throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("sectionPos out of bounds"));
      } else {
         Optional<R> $$1 = this.getOrLoad(pSectionKey);
         if ($$1.isPresent()) {
            return $$1.get();
         } else {
            R $$2 = this.factory.apply(() -> this.setDirty(pSectionKey));
            this.storage.put(pSectionKey, Optional.of($$2));
            return $$2;
         }
      }
   }

   private void readColumn(ChunkPos pChunkPos) {
      Optional<CompoundTag> $$1 = this.tryRead(pChunkPos).join();
      RegistryOps<Tag> $$2 = this.registryAccess.createSerializationContext(NbtOps.INSTANCE);
      this.readColumn(pChunkPos, $$2, $$1.orElse(null));
   }

   private CompletableFuture<Optional<CompoundTag>> tryRead(ChunkPos pChunkPos) {
      return this.simpleRegionStorage.read(pChunkPos).exceptionally(p_341893_ -> {
         if (p_341893_ instanceof IOException $$2) {
            LOGGER.error("Error reading chunk {} data from disk", pChunkPos, $$2);
            this.errorReporter.reportChunkLoadFailure($$2, this.simpleRegionStorage.storageInfo(), pChunkPos);
            return Optional.empty();
         } else {
            throw new CompletionException(p_341893_);
         }
      });
   }

   private void readColumn(ChunkPos pChunkPos, RegistryOps<Tag> pOps, @Nullable CompoundTag pTag) {
      if (pTag == null) {
         for (int $$3 = this.levelHeightAccessor.getMinSection(); $$3 < this.levelHeightAccessor.getMaxSection(); $$3++) {
            this.storage.put(getKey(pChunkPos, $$3), Optional.empty());
         }
      } else {
         Dynamic<Tag> $$4 = new Dynamic(pOps, pTag);
         int $$5 = getVersion($$4);
         int $$6 = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
         boolean $$7 = $$5 != $$6;
         Dynamic<Tag> $$8 = this.simpleRegionStorage.upgradeChunkTag($$4, $$5);
         OptionalDynamic<Tag> $$9 = $$8.get("Sections");

         for (int $$10 = this.levelHeightAccessor.getMinSection(); $$10 < this.levelHeightAccessor.getMaxSection(); $$10++) {
            long $$11 = getKey(pChunkPos, $$10);
            Optional<R> $$12 = $$9.get(Integer.toString($$10))
               .result()
               .flatMap(p_327426_ -> this.codec.apply(() -> this.setDirty($$11)).parse(p_327426_).resultOrPartial(LOGGER::error));
            this.storage.put($$11, $$12);
            $$12.ifPresent(p_223523_ -> {
               this.onSectionLoad($$11);
               if ($$7) {
                  this.setDirty($$11);
               }
            });
         }
      }
   }

   private void writeColumn(ChunkPos pChunkPos) {
      RegistryOps<Tag> $$1 = this.registryAccess.createSerializationContext(NbtOps.INSTANCE);
      Dynamic<Tag> $$2 = this.writeColumn(pChunkPos, $$1);
      Tag $$3 = (Tag)$$2.getValue();
      if ($$3 instanceof CompoundTag) {
         this.simpleRegionStorage.write(pChunkPos, (CompoundTag)$$3).exceptionally(p_341891_ -> {
            this.errorReporter.reportChunkSaveFailure(p_341891_, this.simpleRegionStorage.storageInfo(), pChunkPos);
            return null;
         });
      } else {
         LOGGER.error("Expected compound tag, got {}", $$3);
      }
   }

   private <T> Dynamic<T> writeColumn(ChunkPos pChunkPos, DynamicOps<T> pOps) {
      Map<T, T> $$2 = Maps.newHashMap();

      for (int $$3 = this.levelHeightAccessor.getMinSection(); $$3 < this.levelHeightAccessor.getMaxSection(); $$3++) {
         long $$4 = getKey(pChunkPos, $$3);
         this.dirty.remove($$4);
         Optional<R> $$5 = (Optional<R>)this.storage.get($$4);
         if ($$5 != null && !$$5.isEmpty()) {
            DataResult<T> $$6 = this.codec.apply(() -> this.setDirty($$4)).encodeStart(pOps, $$5.get());
            String $$7 = Integer.toString($$3);
            $$6.resultOrPartial(LOGGER::error).ifPresent(p_223531_ -> $$2.put((T)pOps.createString($$7), (T)p_223531_));
         }
      }

      return new Dynamic(
         pOps,
         pOps.createMap(
            ImmutableMap.of(
               pOps.createString("Sections"),
               pOps.createMap($$2),
               pOps.createString("DataVersion"),
               pOps.createInt(SharedConstants.getCurrentVersion().getDataVersion().getVersion())
            )
         )
      );
   }

   private static long getKey(ChunkPos pChunkPos, int pSectionY) {
      return SectionPos.asLong(pChunkPos.x, pSectionY, pChunkPos.z);
   }

   protected void onSectionLoad(long pSectionKey) {
   }

   protected void setDirty(long pSectionPos) {
      Optional<R> $$1 = (Optional<R>)this.storage.get(pSectionPos);
      if ($$1 != null && !$$1.isEmpty()) {
         this.dirty.add(pSectionPos);
      } else {
         LOGGER.warn("No data for position: {}", SectionPos.of(pSectionPos));
      }
   }

   private static int getVersion(Dynamic<?> pColumnData) {
      return pColumnData.get("DataVersion").asInt(1945);
   }

   public void flush(ChunkPos pChunkPos) {
      if (this.hasWork()) {
         for (int $$1 = this.levelHeightAccessor.getMinSection(); $$1 < this.levelHeightAccessor.getMaxSection(); $$1++) {
            long $$2 = getKey(pChunkPos, $$1);
            if (this.dirty.contains($$2)) {
               this.writeColumn(pChunkPos);
               return;
            }
         }
      }
   }

   @Override
   public void close() throws IOException {
      this.simpleRegionStorage.close();
   }
}
