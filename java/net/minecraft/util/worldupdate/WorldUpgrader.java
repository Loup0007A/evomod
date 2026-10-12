package net.minecraft.util.worldupdate;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Reference2FloatMap;
import it.unimi.dsi.fastutil.objects.Reference2FloatMaps;
import it.unimi.dsi.fastutil.objects.Reference2FloatOpenHashMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.RecreatingChunkStorage;
import net.minecraft.world.level.chunk.storage.RecreatingSimpleRegionStorage;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;

public class WorldUpgrader {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder().setDaemon(true).build();
   private static final String NEW_DIRECTORY_PREFIX = "new_";
   static final MutableComponent STATUS_UPGRADING_POI = Component.translatable("optimizeWorld.stage.upgrading.poi");
   static final MutableComponent STATUS_FINISHED_POI = Component.translatable("optimizeWorld.stage.finished.poi");
   static final MutableComponent STATUS_UPGRADING_ENTITIES = Component.translatable("optimizeWorld.stage.upgrading.entities");
   static final MutableComponent STATUS_FINISHED_ENTITIES = Component.translatable("optimizeWorld.stage.finished.entities");
   static final MutableComponent STATUS_UPGRADING_CHUNKS = Component.translatable("optimizeWorld.stage.upgrading.chunks");
   static final MutableComponent STATUS_FINISHED_CHUNKS = Component.translatable("optimizeWorld.stage.finished.chunks");
   final Registry<LevelStem> dimensions;
   final Set<ResourceKey<Level>> levels;
   final boolean eraseCache;
   final boolean recreateRegionFiles;
   final LevelStorageSource.LevelStorageAccess levelStorage;
   private final Thread thread;
   final DataFixer dataFixer;
   volatile boolean running = true;
   private volatile boolean finished;
   volatile float progress;
   volatile int totalChunks;
   volatile int totalFiles;
   volatile int converted;
   volatile int skipped;
   final Reference2FloatMap<ResourceKey<Level>> progressMap = Reference2FloatMaps.synchronize(new Reference2FloatOpenHashMap());
   volatile Component status = Component.translatable("optimizeWorld.stage.counting");
   static final Pattern REGEX = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca$");
   final DimensionDataStorage overworldDataStorage;

   public WorldUpgrader(
      LevelStorageSource.LevelStorageAccess pLevelStorage,
      DataFixer pDataFixer,
      RegistryAccess pRegistryAccess,
      boolean pEraseCache,
      boolean pRecreateRegionFiles
   ) {
      this.dimensions = pRegistryAccess.registryOrThrow(Registries.LEVEL_STEM);
      this.levels = this.dimensions.registryKeySet().stream().map(Registries::levelStemToLevel).collect(Collectors.toUnmodifiableSet());
      this.eraseCache = pEraseCache;
      this.dataFixer = pDataFixer;
      this.levelStorage = pLevelStorage;
      this.overworldDataStorage = new DimensionDataStorage(
         this.levelStorage.getDimensionPath(Level.OVERWORLD).resolve("data").toFile(), pDataFixer, pRegistryAccess
      );
      this.recreateRegionFiles = pRecreateRegionFiles;
      this.thread = THREAD_FACTORY.newThread(this::work);
      this.thread.setUncaughtExceptionHandler((p_18825_, p_18826_) -> {
         LOGGER.error("Error upgrading world", p_18826_);
         this.status = Component.translatable("optimizeWorld.stage.failed");
         this.finished = true;
      });
      this.thread.start();
   }

   public void cancel() {
      this.running = false;

      try {
         this.thread.join();
      } catch (InterruptedException var2) {
      }
   }

   private void work() {
      long $$0 = Util.getMillis();
      LOGGER.info("Upgrading entities");
      new WorldUpgrader.EntityUpgrader().upgrade();
      LOGGER.info("Upgrading POIs");
      new WorldUpgrader.PoiUpgrader().upgrade();
      LOGGER.info("Upgrading blocks");
      new WorldUpgrader.ChunkUpgrader().upgrade();
      this.overworldDataStorage.save();
      $$0 = Util.getMillis() - $$0;
      LOGGER.info("World optimizaton finished after {} seconds", $$0 / 1000L);
      this.finished = true;
   }

   public boolean isFinished() {
      return this.finished;
   }

   public Set<ResourceKey<Level>> levels() {
      return this.levels;
   }

   public float dimensionProgress(ResourceKey<Level> pLevel) {
      return this.progressMap.getFloat(pLevel);
   }

   public float getProgress() {
      return this.progress;
   }

   public int getTotalChunks() {
      return this.totalChunks;
   }

   public int getConverted() {
      return this.converted;
   }

   public int getSkipped() {
      return this.skipped;
   }

   public Component getStatus() {
      return this.status;
   }

   static Path resolveRecreateDirectory(Path pPath) {
      return pPath.resolveSibling("new_" + pPath.getFileName().toString());
   }

   abstract class AbstractUpgrader<T extends AutoCloseable> {
      private final MutableComponent upgradingStatus;
      private final MutableComponent finishedStatus;
      private final String type;
      private final String folderName;
      @Nullable
      protected CompletableFuture<Void> previousWriteFuture;
      protected final DataFixTypes dataFixType;

      AbstractUpgrader(
         final DataFixTypes pDataFixType,
         final String pType,
         final String pFolderName,
         final MutableComponent pUpgradingStatus,
         final MutableComponent pFinishedStatus
      ) {
         this.dataFixType = pDataFixType;
         this.type = pType;
         this.folderName = pFolderName;
         this.upgradingStatus = pUpgradingStatus;
         this.finishedStatus = pFinishedStatus;
      }

      public void upgrade() {
         WorldUpgrader.this.totalFiles = 0;
         WorldUpgrader.this.totalChunks = 0;
         WorldUpgrader.this.converted = 0;
         WorldUpgrader.this.skipped = 0;
         List<WorldUpgrader.DimensionToUpgrade<T>> $$0 = this.getDimensionsToUpgrade();
         if (WorldUpgrader.this.totalChunks != 0) {
            float $$1 = (float)WorldUpgrader.this.totalFiles;
            WorldUpgrader.this.status = this.upgradingStatus;

            while (WorldUpgrader.this.running) {
               boolean $$2 = false;
               float $$3 = 0.0F;

               for (WorldUpgrader.DimensionToUpgrade<T> $$4 : $$0) {
                  ResourceKey<Level> $$5 = $$4.dimensionKey;
                  ListIterator<WorldUpgrader.FileToUpgrade> $$6 = $$4.files;
                  T $$7 = $$4.storage;
                  if ($$6.hasNext()) {
                     WorldUpgrader.FileToUpgrade $$8 = $$6.next();
                     boolean $$9 = true;

                     for (ChunkPos $$10 : $$8.chunksToUpgrade) {
                        $$9 = $$9 && this.processOnePosition($$5, $$7, $$10);
                        $$2 = true;
                     }

                     if (WorldUpgrader.this.recreateRegionFiles) {
                        if ($$9) {
                           this.onFileFinished($$8.file);
                        } else {
                           WorldUpgrader.LOGGER.error("Failed to convert region file {}", $$8.file.getPath());
                        }
                     }
                  }

                  float $$11 = (float)$$6.nextIndex() / $$1;
                  WorldUpgrader.this.progressMap.put($$5, $$11);
                  $$3 += $$11;
               }

               WorldUpgrader.this.progress = $$3;
               if (!$$2) {
                  break;
               }
            }

            WorldUpgrader.this.status = this.finishedStatus;

            for (WorldUpgrader.DimensionToUpgrade<T> $$12 : $$0) {
               try {
                  $$12.storage.close();
               } catch (Exception var14) {
                  WorldUpgrader.LOGGER.error("Error upgrading chunk", var14);
               }
            }
         }
      }

      private List<WorldUpgrader.DimensionToUpgrade<T>> getDimensionsToUpgrade() {
         List<WorldUpgrader.DimensionToUpgrade<T>> $$0 = Lists.newArrayList();

         for (ResourceKey<Level> $$1 : WorldUpgrader.this.levels) {
            RegionStorageInfo $$2 = new RegionStorageInfo(WorldUpgrader.this.levelStorage.getLevelId(), $$1, this.type);
            Path $$3 = WorldUpgrader.this.levelStorage.getDimensionPath($$1).resolve(this.folderName);
            T $$4 = this.createStorage($$2, $$3);
            ListIterator<WorldUpgrader.FileToUpgrade> $$5 = this.getFilesToProcess($$2, $$3);
            $$0.add(new WorldUpgrader.DimensionToUpgrade<>($$1, $$4, $$5));
         }

         return $$0;
      }

      protected abstract T createStorage(RegionStorageInfo pRegionStorageInfo, Path pPath);

      private ListIterator<WorldUpgrader.FileToUpgrade> getFilesToProcess(RegionStorageInfo pRegionStorageInfo, Path pPath) {
         List<WorldUpgrader.FileToUpgrade> $$2 = getAllChunkPositions(pRegionStorageInfo, pPath);
         WorldUpgrader.this.totalFiles = WorldUpgrader.this.totalFiles + $$2.size();
         WorldUpgrader.this.totalChunks = WorldUpgrader.this.totalChunks + $$2.stream().mapToInt(p_328536_ -> p_328536_.chunksToUpgrade.size()).sum();
         return $$2.listIterator();
      }

      private static List<WorldUpgrader.FileToUpgrade> getAllChunkPositions(RegionStorageInfo pRegionStorageInfo, Path pPath) {
         File[] $$2 = pPath.toFile().listFiles((p_336334_, p_329184_) -> p_329184_.endsWith(".mca"));
         if ($$2 == null) {
            return List.of();
         } else {
            List<WorldUpgrader.FileToUpgrade> $$3 = Lists.newArrayList();

            for (File $$4 : $$2) {
               Matcher $$5 = WorldUpgrader.REGEX.matcher($$4.getName());
               if ($$5.matches()) {
                  int $$6 = Integer.parseInt($$5.group(1)) << 5;
                  int $$7 = Integer.parseInt($$5.group(2)) << 5;
                  List<ChunkPos> $$8 = Lists.newArrayList();

                  try (RegionFile $$9 = new RegionFile(pRegionStorageInfo, $$4.toPath(), pPath, true)) {
                     for (int $$10 = 0; $$10 < 32; $$10++) {
                        for (int $$11 = 0; $$11 < 32; $$11++) {
                           ChunkPos $$12 = new ChunkPos($$10 + $$6, $$11 + $$7);
                           if ($$9.doesChunkExist($$12)) {
                              $$8.add($$12);
                           }
                        }
                     }

                     if (!$$8.isEmpty()) {
                        $$3.add(new WorldUpgrader.FileToUpgrade($$9, $$8));
                     }
                  } catch (Throwable var18) {
                     WorldUpgrader.LOGGER.error("Failed to read chunks from region file {}", $$4.toPath(), var18);
                  }
               }
            }

            return $$3;
         }
      }

      private boolean processOnePosition(ResourceKey<Level> pDimesion, T pStorage, ChunkPos pChunkPos) {
         boolean $$3 = false;

         try {
            $$3 = this.tryProcessOnePosition(pStorage, pChunkPos, pDimesion);
         } catch (CompletionException | ReportedException var7) {
            Throwable $$5 = var7.getCause();
            if (!($$5 instanceof IOException)) {
               throw var7;
            }

            WorldUpgrader.LOGGER.error("Error upgrading chunk {}", pChunkPos, $$5);
         }

         if ($$3) {
            WorldUpgrader.this.converted++;
         } else {
            WorldUpgrader.this.skipped++;
         }

         return $$3;
      }

      protected abstract boolean tryProcessOnePosition(T pChunkStorage, ChunkPos pChunkPos, ResourceKey<Level> pDimension);

      private void onFileFinished(RegionFile pRegionFile) {
         if (WorldUpgrader.this.recreateRegionFiles) {
            if (this.previousWriteFuture != null) {
               this.previousWriteFuture.join();
            }

            Path $$1 = pRegionFile.getPath();
            Path $$2 = $$1.getParent();
            Path $$3 = WorldUpgrader.resolveRecreateDirectory($$2).resolve($$1.getFileName().toString());

            try {
               if ($$3.toFile().exists()) {
                  Files.delete($$1);
                  Files.move($$3, $$1);
               } else {
                  WorldUpgrader.LOGGER.error("Failed to replace an old region file. New file {} does not exist.", $$3);
               }
            } catch (IOException var6) {
               WorldUpgrader.LOGGER.error("Failed to replace an old region file", var6);
            }
         }
      }
   }

   class ChunkUpgrader extends WorldUpgrader.AbstractUpgrader<ChunkStorage> {
      ChunkUpgrader() {
         super(DataFixTypes.CHUNK, "chunk", "region", WorldUpgrader.STATUS_UPGRADING_CHUNKS, WorldUpgrader.STATUS_FINISHED_CHUNKS);
      }

      protected boolean tryProcessOnePosition(ChunkStorage p_330540_, ChunkPos p_331086_, ResourceKey<Level> p_327850_) {
         CompoundTag $$3 = p_330540_.read(p_331086_).join().orElse(null);
         if ($$3 != null) {
            int $$4 = ChunkStorage.getVersion($$3);
            ChunkGenerator $$5 = WorldUpgrader.this.dimensions.getOrThrow(Registries.levelToLevelStem(p_327850_)).generator();
            CompoundTag $$6 = p_330540_.upgradeChunkTag(p_327850_, () -> WorldUpgrader.this.overworldDataStorage, $$3, $$5.getTypeNameForDataFixer());
            ChunkPos $$7 = new ChunkPos($$6.getInt("xPos"), $$6.getInt("zPos"));
            if (!$$7.equals(p_331086_)) {
               WorldUpgrader.LOGGER.warn("Chunk {} has invalid position {}", p_331086_, $$7);
            }

            boolean $$8 = $$4 < SharedConstants.getCurrentVersion().getDataVersion().getVersion();
            if (WorldUpgrader.this.eraseCache) {
               $$8 = $$8 || $$6.contains("Heightmaps");
               $$6.remove("Heightmaps");
               $$8 = $$8 || $$6.contains("isLightOn");
               $$6.remove("isLightOn");
               ListTag $$9 = $$6.getList("sections", 10);

               for (int $$10 = 0; $$10 < $$9.size(); $$10++) {
                  CompoundTag $$11 = $$9.getCompound($$10);
                  $$8 = $$8 || $$11.contains("BlockLight");
                  $$11.remove("BlockLight");
                  $$8 = $$8 || $$11.contains("SkyLight");
                  $$11.remove("SkyLight");
               }
            }

            if ($$8 || WorldUpgrader.this.recreateRegionFiles) {
               if (this.previousWriteFuture != null) {
                  this.previousWriteFuture.join();
               }

               this.previousWriteFuture = p_330540_.write(p_331086_, $$6);
               return true;
            }
         }

         return false;
      }

      protected ChunkStorage createStorage(RegionStorageInfo p_333791_, Path p_332463_) {
         return (ChunkStorage)(WorldUpgrader.this.recreateRegionFiles
            ? new RecreatingChunkStorage(
               p_333791_.withTypeSuffix("source"),
               p_332463_,
               p_333791_.withTypeSuffix("target"),
               WorldUpgrader.resolveRecreateDirectory(p_332463_),
               WorldUpgrader.this.dataFixer,
               true
            )
            : new ChunkStorage(p_333791_, p_332463_, WorldUpgrader.this.dataFixer, true));
      }
   }

   static record DimensionToUpgrade<T>(ResourceKey<Level> dimensionKey, T storage, ListIterator<WorldUpgrader.FileToUpgrade> files) {
   }

   class EntityUpgrader extends WorldUpgrader.SimpleRegionStorageUpgrader {
      EntityUpgrader() {
         super(DataFixTypes.ENTITY_CHUNK, "entities", WorldUpgrader.STATUS_UPGRADING_ENTITIES, WorldUpgrader.STATUS_FINISHED_ENTITIES);
      }

      @Override
      protected CompoundTag upgradeTag(SimpleRegionStorage p_334286_, CompoundTag p_335346_) {
         return p_334286_.upgradeChunkTag(p_335346_, -1);
      }
   }

   static record FileToUpgrade(RegionFile file, List<ChunkPos> chunksToUpgrade) {
   }

   class PoiUpgrader extends WorldUpgrader.SimpleRegionStorageUpgrader {
      PoiUpgrader() {
         super(DataFixTypes.POI_CHUNK, "poi", WorldUpgrader.STATUS_UPGRADING_POI, WorldUpgrader.STATUS_FINISHED_POI);
      }

      @Override
      protected CompoundTag upgradeTag(SimpleRegionStorage p_329642_, CompoundTag p_336180_) {
         return p_329642_.upgradeChunkTag(p_336180_, 1945);
      }
   }

   abstract class SimpleRegionStorageUpgrader extends WorldUpgrader.AbstractUpgrader<SimpleRegionStorage> {
      SimpleRegionStorageUpgrader(
         final DataFixTypes pDataFixType, final String pType, final MutableComponent pUpgradingStatus, final MutableComponent pFinishedStatus
      ) {
         super(pDataFixType, pType, pType, pUpgradingStatus, pFinishedStatus);
      }

      protected SimpleRegionStorage createStorage(RegionStorageInfo pRegionStorageInfo, Path pPath) {
         return (SimpleRegionStorage)(WorldUpgrader.this.recreateRegionFiles
            ? new RecreatingSimpleRegionStorage(
               pRegionStorageInfo.withTypeSuffix("source"),
               pPath,
               pRegionStorageInfo.withTypeSuffix("target"),
               WorldUpgrader.resolveRecreateDirectory(pPath),
               WorldUpgrader.this.dataFixer,
               true,
               this.dataFixType
            )
            : new SimpleRegionStorage(pRegionStorageInfo, pPath, WorldUpgrader.this.dataFixer, true, this.dataFixType));
      }

      protected boolean tryProcessOnePosition(SimpleRegionStorage pChunkStorage, ChunkPos pChunkPos, ResourceKey<Level> pDimension) {
         CompoundTag $$3 = pChunkStorage.read(pChunkPos).join().orElse(null);
         if ($$3 != null) {
            int $$4 = ChunkStorage.getVersion($$3);
            CompoundTag $$5 = this.upgradeTag(pChunkStorage, $$3);
            boolean $$6 = $$4 < SharedConstants.getCurrentVersion().getDataVersion().getVersion();
            if ($$6 || WorldUpgrader.this.recreateRegionFiles) {
               if (this.previousWriteFuture != null) {
                  this.previousWriteFuture.join();
               }

               this.previousWriteFuture = pChunkStorage.write(pChunkPos, $$5);
               return true;
            }
         }

         return false;
      }

      protected abstract CompoundTag upgradeTag(SimpleRegionStorage pRegionStorage, CompoundTag pChunkTag);
   }
}
