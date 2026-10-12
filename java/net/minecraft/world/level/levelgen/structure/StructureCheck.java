package net.minecraft.world.level.levelgen.structure;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2BooleanMap;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.visitors.CollectFields;
import net.minecraft.nbt.visitors.FieldSelector;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;

public class StructureCheck {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int NO_STRUCTURE = -1;
   private final ChunkScanAccess storageAccess;
   private final RegistryAccess registryAccess;
   private final StructureTemplateManager structureTemplateManager;
   private final ResourceKey<Level> dimension;
   private final ChunkGenerator chunkGenerator;
   private final RandomState randomState;
   private final LevelHeightAccessor heightAccessor;
   private final BiomeSource biomeSource;
   private final long seed;
   private final DataFixer fixerUpper;
   private final Long2ObjectMap<Object2IntMap<Structure>> loadedChunks = new Long2ObjectOpenHashMap();
   private final Map<Structure, Long2BooleanMap> featureChecks = new HashMap<>();

   public StructureCheck(
      ChunkScanAccess pStorageAccess,
      RegistryAccess pRegistryAccess,
      StructureTemplateManager pStructureTemplateManager,
      ResourceKey<Level> pDimension,
      ChunkGenerator pChunkGenerator,
      RandomState pRandomState,
      LevelHeightAccessor pHeightAccessor,
      BiomeSource pBiomeSource,
      long pSeed,
      DataFixer pFixerUpper
   ) {
      this.storageAccess = pStorageAccess;
      this.registryAccess = pRegistryAccess;
      this.structureTemplateManager = pStructureTemplateManager;
      this.dimension = pDimension;
      this.chunkGenerator = pChunkGenerator;
      this.randomState = pRandomState;
      this.heightAccessor = pHeightAccessor;
      this.biomeSource = pBiomeSource;
      this.seed = pSeed;
      this.fixerUpper = pFixerUpper;
   }

   public StructureCheckResult checkStart(ChunkPos pChunkPos, Structure pStructure, StructurePlacement pPlacement, boolean pSkipKnownStructures) {
      long $$4 = pChunkPos.toLong();
      Object2IntMap<Structure> $$5 = (Object2IntMap<Structure>)this.loadedChunks.get($$4);
      if ($$5 != null) {
         return this.checkStructureInfo($$5, pStructure, pSkipKnownStructures);
      } else {
         StructureCheckResult $$6 = this.tryLoadFromStorage(pChunkPos, pStructure, pSkipKnownStructures, $$4);
         if ($$6 != null) {
            return $$6;
         } else if (!pPlacement.applyAdditionalChunkRestrictions(pChunkPos.x, pChunkPos.z, this.seed)) {
            return StructureCheckResult.START_NOT_PRESENT;
         } else {
            boolean $$7 = this.featureChecks
               .computeIfAbsent(pStructure, p_226739_ -> new Long2BooleanOpenHashMap())
               .computeIfAbsent($$4, p_226728_ -> this.canCreateStructure(pChunkPos, pStructure));
            return !$$7 ? StructureCheckResult.START_NOT_PRESENT : StructureCheckResult.CHUNK_LOAD_NEEDED;
         }
      }
   }

   private boolean canCreateStructure(ChunkPos pChunkPos, Structure pStructure) {
      return pStructure.findValidGenerationPoint(
            new Structure.GenerationContext(
               this.registryAccess,
               this.chunkGenerator,
               this.biomeSource,
               this.randomState,
               this.structureTemplateManager,
               this.seed,
               pChunkPos,
               this.heightAccessor,
               pStructure.biomes()::contains
            )
         )
         .isPresent();
   }

   @Nullable
   private StructureCheckResult tryLoadFromStorage(ChunkPos pChunkPos, Structure pStructure, boolean pSkipKnownStructures, long pPackedChunkPos) {
      CollectFields $$4 = new CollectFields(
         new FieldSelector(IntTag.TYPE, "DataVersion"),
         new FieldSelector("Level", "Structures", CompoundTag.TYPE, "Starts"),
         new FieldSelector("structures", CompoundTag.TYPE, "starts")
      );

      try {
         this.storageAccess.scanChunk(pChunkPos, $$4).join();
      } catch (Exception var13) {
         LOGGER.warn("Failed to read chunk {}", pChunkPos, var13);
         return StructureCheckResult.CHUNK_LOAD_NEEDED;
      }

      if (!($$4.getResult() instanceof CompoundTag $$7)) {
         return null;
      } else {
         int $$8 = ChunkStorage.getVersion($$7);
         if ($$8 <= 1493) {
            return StructureCheckResult.CHUNK_LOAD_NEEDED;
         } else {
            ChunkStorage.injectDatafixingContext($$7, this.dimension, this.chunkGenerator.getTypeNameForDataFixer());

            CompoundTag $$9;
            try {
               $$9 = DataFixTypes.CHUNK.updateToCurrentVersion(this.fixerUpper, $$7, $$8);
            } catch (Exception var12) {
               LOGGER.warn("Failed to partially datafix chunk {}", pChunkPos, var12);
               return StructureCheckResult.CHUNK_LOAD_NEEDED;
            }

            Object2IntMap<Structure> $$12 = this.loadStructures($$9);
            if ($$12 == null) {
               return null;
            } else {
               this.storeFullResults(pPackedChunkPos, $$12);
               return this.checkStructureInfo($$12, pStructure, pSkipKnownStructures);
            }
         }
      }
   }

   @Nullable
   private Object2IntMap<Structure> loadStructures(CompoundTag pTag) {
      if (!pTag.contains("structures", 10)) {
         return null;
      } else {
         CompoundTag $$1 = pTag.getCompound("structures");
         if (!$$1.contains("starts", 10)) {
            return null;
         } else {
            CompoundTag $$2 = $$1.getCompound("starts");
            if ($$2.isEmpty()) {
               return Object2IntMaps.emptyMap();
            } else {
               Object2IntMap<Structure> $$3 = new Object2IntOpenHashMap();
               Registry<Structure> $$4 = this.registryAccess.registryOrThrow(Registries.STRUCTURE);

               for (String $$5 : $$2.getAllKeys()) {
                  ResourceLocation $$6 = ResourceLocation.tryParse($$5);
                  if ($$6 != null) {
                     Structure $$7 = $$4.get($$6);
                     if ($$7 != null) {
                        CompoundTag $$8 = $$2.getCompound($$5);
                        if (!$$8.isEmpty()) {
                           String $$9 = $$8.getString("id");
                           if (!"INVALID".equals($$9)) {
                              int $$10 = $$8.getInt("references");
                              $$3.put($$7, $$10);
                           }
                        }
                     }
                  }
               }

               return $$3;
            }
         }
      }
   }

   private static Object2IntMap<Structure> deduplicateEmptyMap(Object2IntMap<Structure> pMap) {
      return pMap.isEmpty() ? Object2IntMaps.emptyMap() : pMap;
   }

   private StructureCheckResult checkStructureInfo(Object2IntMap<Structure> pStructureChunks, Structure pStructure, boolean pSkipKnownStructures) {
      int $$3 = pStructureChunks.getOrDefault(pStructure, -1);
      return $$3 == -1 || pSkipKnownStructures && $$3 != 0 ? StructureCheckResult.START_NOT_PRESENT : StructureCheckResult.START_PRESENT;
   }

   public void onStructureLoad(ChunkPos pChunkPos, Map<Structure, StructureStart> pChunkStarts) {
      long $$2 = pChunkPos.toLong();
      Object2IntMap<Structure> $$3 = new Object2IntOpenHashMap();
      pChunkStarts.forEach((p_226749_, p_226750_) -> {
         if (p_226750_.isValid()) {
            $$3.put(p_226749_, p_226750_.getReferences());
         }
      });
      this.storeFullResults($$2, $$3);
   }

   private void storeFullResults(long pChunkPos, Object2IntMap<Structure> pStructureChunks) {
      this.loadedChunks.put(pChunkPos, deduplicateEmptyMap(pStructureChunks));
      this.featureChecks.values().forEach(p_209956_ -> p_209956_.remove(pChunkPos));
   }

   public void incrementReference(ChunkPos pPos, Structure pStructure) {
      this.loadedChunks.compute(pPos.toLong(), (p_226745_, p_226746_) -> {
         if (p_226746_ == null || p_226746_.isEmpty()) {
            p_226746_ = new Object2IntOpenHashMap();
         }

         p_226746_.computeInt(pStructure, (p_226741_, p_226742_) -> p_226742_ == null ? 1 : p_226742_ + 1);
         return p_226746_;
      });
   }
}
