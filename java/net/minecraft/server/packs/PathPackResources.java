package net.minecraft.server.packs;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;

public class PathPackResources extends AbstractPackResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final Joiner PATH_JOINER = Joiner.on("/");
   private final Path root;

   public PathPackResources(PackLocationInfo pLocation, Path pRoot) {
      super(pLocation);
      this.root = pRoot;
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getRootResource(String... pElements) {
      FileUtil.validatePath(pElements);
      Path $$1 = FileUtil.resolvePath(this.root, List.of(pElements));
      return Files.exists($$1) ? IoSupplier.create($$1) : null;
   }

   public static boolean validatePath(Path pPath) {
      return true;
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getResource(PackType pPackType, ResourceLocation pLocation) {
      Path $$2 = this.root.resolve(pPackType.getDirectory()).resolve(pLocation.getNamespace());
      return getResource(pLocation, $$2);
   }

   @Nullable
   public static IoSupplier<InputStream> getResource(ResourceLocation pLocation, Path pPath) {
      return (IoSupplier<InputStream>)FileUtil.decomposePath(pLocation.getPath()).mapOrElse(p_251647_ -> {
         Path $$2 = FileUtil.resolvePath(pPath, p_251647_);
         return returnFileIfExists($$2);
      }, p_326463_ -> {
         LOGGER.error("Invalid path {}: {}", pLocation, p_326463_.message());
         return null;
      });
   }

   @Nullable
   private static IoSupplier<InputStream> returnFileIfExists(Path pPath) {
      return Files.exists(pPath) && validatePath(pPath) ? IoSupplier.create(pPath) : null;
   }

   @Override
   public void listResources(PackType pPackType, String pNamespace, String pPath, PackResources.ResourceOutput pResourceOutput) {
      FileUtil.decomposePath(pPath).ifSuccess(p_250225_ -> {
         Path $$4 = this.root.resolve(pPackType.getDirectory()).resolve(pNamespace);
         listPath(pNamespace, $$4, p_250225_, pResourceOutput);
      }).ifError(p_326465_ -> LOGGER.error("Invalid path {}: {}", pPath, p_326465_.message()));
   }

   public static void listPath(String pNamespace, Path pNamespacePath, List<String> pDecomposedPath, PackResources.ResourceOutput pResourceOutput) {
      Path $$4 = FileUtil.resolvePath(pNamespacePath, pDecomposedPath);

      try (Stream<Path> $$5 = Files.find($$4, Integer.MAX_VALUE, (p_250060_, p_250796_) -> p_250796_.isRegularFile())) {
         $$5.forEach(p_249092_ -> {
            String $$4x = PATH_JOINER.join(pNamespacePath.relativize(p_249092_));
            ResourceLocation $$5x = ResourceLocation.tryBuild(pNamespace, $$4x);
            if ($$5x == null) {
               Util.logAndPauseIfInIde(String.format(Locale.ROOT, "Invalid path in pack: %s:%s, ignoring", pNamespace, $$4x));
            } else {
               pResourceOutput.accept($$5x, IoSupplier.create(p_249092_));
            }
         });
      } catch (NotDirectoryException | NoSuchFileException var10) {
      } catch (IOException var11) {
         LOGGER.error("Failed to list path {}", $$4, var11);
      }
   }

   @Override
   public Set<String> getNamespaces(PackType pType) {
      Set<String> $$1 = Sets.newHashSet();
      Path $$2 = this.root.resolve(pType.getDirectory());

      try (DirectoryStream<Path> $$3 = Files.newDirectoryStream($$2)) {
         for (Path $$4 : $$3) {
            String $$5 = $$4.getFileName().toString();
            if (ResourceLocation.isValidNamespace($$5)) {
               $$1.add($$5);
            } else {
               LOGGER.warn("Non [a-z0-9_.-] character in namespace {} in pack {}, ignoring", $$5, this.root);
            }
         }
      } catch (NotDirectoryException | NoSuchFileException var10) {
      } catch (IOException var11) {
         LOGGER.error("Failed to list path {}", $$2, var11);
      }

      return $$1;
   }

   @Override
   public void close() {
   }

   public static class PathResourcesSupplier implements Pack.ResourcesSupplier {
      private final Path content;

      public PathResourcesSupplier(Path pContent) {
         this.content = pContent;
      }

      @Override
      public PackResources openPrimary(PackLocationInfo pLocation) {
         return new PathPackResources(pLocation, this.content);
      }

      @Override
      public PackResources openFull(PackLocationInfo pLocation, Pack.Metadata pMetadata) {
         PackResources $$2 = this.openPrimary(pLocation);
         List<String> $$3 = pMetadata.overlays();
         if ($$3.isEmpty()) {
            return $$2;
         } else {
            List<PackResources> $$4 = new ArrayList<>($$3.size());

            for (String $$5 : $$3) {
               Path $$6 = this.content.resolve($$5);
               $$4.add(new PathPackResources(pLocation, $$6));
            }

            return new CompositePackResources($$2, $$4);
         }
      }
   }
}
