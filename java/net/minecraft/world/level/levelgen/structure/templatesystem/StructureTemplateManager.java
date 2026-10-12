package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.FileUtil;
import net.minecraft.ResourceLocationException;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderGetter;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastBufferedInputStream;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class StructureTemplateManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final String STRUCTURE_RESOURCE_DIRECTORY_NAME = "structure";
   private static final String STRUCTURE_GENERATED_DIRECTORY_NAME = "structures";
   private static final String STRUCTURE_FILE_EXTENSION = ".nbt";
   private static final String STRUCTURE_TEXT_FILE_EXTENSION = ".snbt";
   private final Map<ResourceLocation, Optional<StructureTemplate>> structureRepository = Maps.newConcurrentMap();
   private final DataFixer fixerUpper;
   private ResourceManager resourceManager;
   private final Path generatedDir;
   private final List<StructureTemplateManager.Source> sources;
   private final HolderGetter<Block> blockLookup;
   private static final FileToIdConverter RESOURCE_LISTER = new FileToIdConverter("structure", ".nbt");

   public StructureTemplateManager(
      ResourceManager pResourceManager, LevelStorageSource.LevelStorageAccess pLevelStorageAccess, DataFixer pFixerUpper, HolderGetter<Block> pBlockLookup
   ) {
      this.resourceManager = pResourceManager;
      this.fixerUpper = pFixerUpper;
      this.generatedDir = pLevelStorageAccess.getLevelPath(LevelResource.GENERATED_DIR).normalize();
      this.blockLookup = pBlockLookup;
      Builder<StructureTemplateManager.Source> $$4 = ImmutableList.builder();
      $$4.add(new StructureTemplateManager.Source(this::loadFromGenerated, this::listGenerated));
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         $$4.add(new StructureTemplateManager.Source(this::loadFromTestStructures, this::listTestStructures));
      }

      $$4.add(new StructureTemplateManager.Source(this::loadFromResource, this::listResources));
      this.sources = $$4.build();
   }

   public StructureTemplate getOrCreate(ResourceLocation pId) {
      Optional<StructureTemplate> $$1 = this.get(pId);
      if ($$1.isPresent()) {
         return $$1.get();
      } else {
         StructureTemplate $$2 = new StructureTemplate();
         this.structureRepository.put(pId, Optional.of($$2));
         return $$2;
      }
   }

   public Optional<StructureTemplate> get(ResourceLocation pId) {
      return this.structureRepository.computeIfAbsent(pId, this::tryLoad);
   }

   public Stream<ResourceLocation> listTemplates() {
      return this.sources.stream().flatMap(p_230376_ -> p_230376_.lister().get()).distinct();
   }

   private Optional<StructureTemplate> tryLoad(ResourceLocation p_230426_) {
      for (StructureTemplateManager.Source $$1 : this.sources) {
         try {
            Optional<StructureTemplate> $$2 = $$1.loader().apply(p_230426_);
            if ($$2.isPresent()) {
               return $$2;
            }
         } catch (Exception var5) {
         }
      }

      return Optional.empty();
   }

   public void onResourceManagerReload(ResourceManager pResourceManager) {
      this.resourceManager = pResourceManager;
      this.structureRepository.clear();
   }

   private Optional<StructureTemplate> loadFromResource(ResourceLocation p_230428_) {
      ResourceLocation $$1 = RESOURCE_LISTER.idToFile(p_230428_);
      return this.load(() -> this.resourceManager.open($$1), p_230366_ -> LOGGER.error("Couldn't load structure {}", p_230428_, p_230366_));
   }

   private Stream<ResourceLocation> listResources() {
      return RESOURCE_LISTER.listMatchingResources(this.resourceManager).keySet().stream().map(RESOURCE_LISTER::fileToId);
   }

   private Optional<StructureTemplate> loadFromTestStructures(ResourceLocation p_230430_) {
      return this.loadFromSnbt(p_230430_, Paths.get(StructureUtils.testStructuresDir));
   }

   private Stream<ResourceLocation> listTestStructures() {
      Path $$0 = Paths.get(StructureUtils.testStructuresDir);
      if (!Files.isDirectory($$0)) {
         return Stream.empty();
      } else {
         List<ResourceLocation> $$1 = new ArrayList<>();
         this.listFolderContents($$0, "minecraft", ".snbt", $$1::add);
         return $$1.stream();
      }
   }

   private Optional<StructureTemplate> loadFromGenerated(ResourceLocation p_230432_) {
      if (!Files.isDirectory(this.generatedDir)) {
         return Optional.empty();
      } else {
         Path $$1 = this.createAndValidatePathToGeneratedStructure(p_230432_, ".nbt");
         return this.load(() -> new FileInputStream($$1.toFile()), p_230400_ -> LOGGER.error("Couldn't load structure from {}", $$1, p_230400_));
      }
   }

   private Stream<ResourceLocation> listGenerated() {
      if (!Files.isDirectory(this.generatedDir)) {
         return Stream.empty();
      } else {
         try {
            List<ResourceLocation> $$0 = new ArrayList<>();

            try (DirectoryStream<Path> $$1 = Files.newDirectoryStream(this.generatedDir, p_230419_ -> Files.isDirectory(p_230419_))) {
               for (Path $$2 : $$1) {
                  String $$3 = $$2.getFileName().toString();
                  Path $$4 = $$2.resolve("structures");
                  this.listFolderContents($$4, $$3, ".nbt", $$0::add);
               }
            }

            return $$0.stream();
         } catch (IOException var9) {
            return Stream.empty();
         }
      }
   }

   private void listFolderContents(Path pFolder, String pNamespace, String pExtension, Consumer<ResourceLocation> pOutput) {
      int $$4 = pExtension.length();
      Function<String, String> $$5 = p_230358_ -> p_230358_.substring(0, p_230358_.length() - $$4);

      try (Stream<Path> $$6 = Files.find(
            pFolder, Integer.MAX_VALUE, (p_341961_, p_341962_) -> p_341962_.isRegularFile() && p_341961_.toString().endsWith(pExtension)
         )) {
         $$6.forEach(p_341959_ -> {
            try {
               pOutput.accept(ResourceLocation.fromNamespaceAndPath(pNamespace, $$5.apply(this.relativize(pFolder, p_341959_))));
            } catch (ResourceLocationException var7x) {
               LOGGER.error("Invalid location while listing folder {} contents", pFolder, var7x);
            }
         });
      } catch (IOException var12) {
         LOGGER.error("Failed to list folder {} contents", pFolder, var12);
      }
   }

   private String relativize(Path pRoot, Path pPath) {
      return pRoot.relativize(pPath).toString().replace(File.separator, "/");
   }

   private Optional<StructureTemplate> loadFromSnbt(ResourceLocation pId, Path pPath) {
      if (!Files.isDirectory(pPath)) {
         return Optional.empty();
      } else {
         Path $$2 = FileUtil.createPathToResource(pPath, pId.getPath(), ".snbt");

         try {
            Optional var6;
            try (BufferedReader $$3 = Files.newBufferedReader($$2)) {
               String $$4 = IOUtils.toString($$3);
               var6 = Optional.of(this.readStructure(NbtUtils.snbtToStructure($$4)));
            }

            return var6;
         } catch (NoSuchFileException var9) {
            return Optional.empty();
         } catch (CommandSyntaxException | IOException var10) {
            LOGGER.error("Couldn't load structure from {}", $$2, var10);
            return Optional.empty();
         }
      }
   }

   private Optional<StructureTemplate> load(StructureTemplateManager.InputStreamOpener pInputStream, Consumer<Throwable> pOnError) {
      try {
         Optional var5;
         try (
            InputStream $$2 = pInputStream.open();
            InputStream $$3 = new FastBufferedInputStream($$2);
         ) {
            var5 = Optional.of(this.readStructure($$3));
         }

         return var5;
      } catch (FileNotFoundException var11) {
         return Optional.empty();
      } catch (Throwable var12) {
         pOnError.accept(var12);
         return Optional.empty();
      }
   }

   private StructureTemplate readStructure(InputStream pStream) throws IOException {
      CompoundTag $$1 = NbtIo.readCompressed(pStream, NbtAccounter.unlimitedHeap());
      return this.readStructure($$1);
   }

   public StructureTemplate readStructure(CompoundTag pNbt) {
      StructureTemplate $$1 = new StructureTemplate();
      int $$2 = NbtUtils.getDataVersion(pNbt, 500);
      $$1.load(this.blockLookup, DataFixTypes.STRUCTURE.updateToCurrentVersion(this.fixerUpper, pNbt, $$2));
      return $$1;
   }

   public boolean save(ResourceLocation pId) {
      Optional<StructureTemplate> $$1 = this.structureRepository.get(pId);
      if ($$1.isEmpty()) {
         return false;
      } else {
         StructureTemplate $$2 = $$1.get();
         Path $$3 = this.createAndValidatePathToGeneratedStructure(pId, ".nbt");
         Path $$4 = $$3.getParent();
         if ($$4 == null) {
            return false;
         } else {
            try {
               Files.createDirectories(Files.exists($$4) ? $$4.toRealPath() : $$4);
            } catch (IOException var13) {
               LOGGER.error("Failed to create parent directory: {}", $$4);
               return false;
            }

            CompoundTag $$6 = $$2.save(new CompoundTag());

            try {
               try (OutputStream $$7 = new FileOutputStream($$3.toFile())) {
                  NbtIo.writeCompressed($$6, $$7);
               }

               return true;
            } catch (Throwable var12) {
               return false;
            }
         }
      }
   }

   public Path createAndValidatePathToGeneratedStructure(ResourceLocation pLocation, String pExtension) {
      if (pLocation.getPath().contains("//")) {
         throw new ResourceLocationException("Invalid resource path: " + pLocation);
      } else {
         try {
            Path $$2 = this.generatedDir.resolve(pLocation.getNamespace());
            Path $$3 = $$2.resolve("structures");
            Path $$4 = FileUtil.createPathToResource($$3, pLocation.getPath(), pExtension);
            if ($$4.startsWith(this.generatedDir) && FileUtil.isPathNormalized($$4) && FileUtil.isPathPortable($$4)) {
               return $$4;
            } else {
               throw new ResourceLocationException("Invalid resource path: " + $$4);
            }
         } catch (InvalidPathException var6) {
            throw new ResourceLocationException("Invalid resource path: " + pLocation, var6);
         }
      }
   }

   public void remove(ResourceLocation pId) {
      this.structureRepository.remove(pId);
   }

   @FunctionalInterface
   interface InputStreamOpener {
      InputStream open() throws IOException;
   }

   static record Source(Function<ResourceLocation, Optional<StructureTemplate>> loader, Supplier<Stream<ResourceLocation>> lister) {
   }
}
