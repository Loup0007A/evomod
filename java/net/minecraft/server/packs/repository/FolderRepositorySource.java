package net.minecraft.server.packs.repository;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.linkfs.LinkFileSystem;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.slf4j.Logger;

public class FolderRepositorySource implements RepositorySource {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final PackSelectionConfig DISCOVERED_PACK_SELECTION_CONFIG = new PackSelectionConfig(false, Pack.Position.TOP, false);
   private final Path folder;
   private final PackType packType;
   private final PackSource packSource;
   private final DirectoryValidator validator;

   public FolderRepositorySource(Path pFolder, PackType pPackType, PackSource pPackSource, DirectoryValidator pValidator) {
      this.folder = pFolder;
      this.packType = pPackType;
      this.packSource = pPackSource;
      this.validator = pValidator;
   }

   private static String nameFromPath(Path pPath) {
      return pPath.getFileName().toString();
   }

   @Override
   public void loadPacks(Consumer<Pack> pOnLoad) {
      try {
         FileUtil.createDirectoriesSafe(this.folder);
         discoverPacks(this.folder, this.validator, (p_326474_, p_326475_) -> {
            PackLocationInfo $$3 = this.createDiscoveredFilePackInfo(p_326474_);
            Pack $$4 = Pack.readMetaAndCreate($$3, p_326475_, this.packType, DISCOVERED_PACK_SELECTION_CONFIG);
            if ($$4 != null) {
               pOnLoad.accept($$4);
            }
         });
      } catch (IOException var3) {
         LOGGER.warn("Failed to list packs in {}", this.folder, var3);
      }
   }

   private PackLocationInfo createDiscoveredFilePackInfo(Path pPath) {
      String $$1 = nameFromPath(pPath);
      return new PackLocationInfo("file/" + $$1, Component.literal($$1), this.packSource, Optional.empty());
   }

   public static void discoverPacks(Path pFolder, DirectoryValidator pValidator, BiConsumer<Path, Pack.ResourcesSupplier> pOutput) throws IOException {
      FolderRepositorySource.FolderPackDetector $$3 = new FolderRepositorySource.FolderPackDetector(pValidator);

      try (DirectoryStream<Path> $$4 = Files.newDirectoryStream(pFolder)) {
         for (Path $$5 : $$4) {
            try {
               List<ForbiddenSymlinkInfo> $$6 = new ArrayList<>();
               Pack.ResourcesSupplier $$7 = $$3.detectPackResources($$5, $$6);
               if (!$$6.isEmpty()) {
                  LOGGER.warn("Ignoring potential pack entry: {}", ContentValidationException.getMessage($$5, $$6));
               } else if ($$7 != null) {
                  pOutput.accept($$5, $$7);
               } else {
                  LOGGER.info("Found non-pack entry '{}', ignoring", $$5);
               }
            } catch (IOException var10) {
               LOGGER.warn("Failed to read properties of '{}', ignoring", $$5, var10);
            }
         }
      }
   }

   public static class FolderPackDetector extends PackDetector<Pack.ResourcesSupplier> {
      public FolderPackDetector(DirectoryValidator p_301187_) {
         super(p_301187_);
      }

      @Nullable
      protected Pack.ResourcesSupplier createZipPack(Path p_299114_) {
         FileSystem $$1 = p_299114_.getFileSystem();
         if ($$1 != FileSystems.getDefault() && !($$1 instanceof LinkFileSystem)) {
            FolderRepositorySource.LOGGER.info("Can't open pack archive at {}", p_299114_);
            return null;
         } else {
            return new FilePackResources.FileResourcesSupplier(p_299114_);
         }
      }

      protected Pack.ResourcesSupplier createDirectoryPack(Path p_300765_) {
         return new PathPackResources.PathResourcesSupplier(p_300765_);
      }
   }
}
