package net.minecraft.server.packs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public class FilePackResources extends AbstractPackResources {
   static final Logger LOGGER = LogUtils.getLogger();
   private final FilePackResources.SharedZipFileAccess zipFileAccess;
   private final String prefix;

   FilePackResources(PackLocationInfo pLocation, FilePackResources.SharedZipFileAccess pZipFileAccess, String pPrefix) {
      super(pLocation);
      this.zipFileAccess = pZipFileAccess;
      this.prefix = pPrefix;
   }

   private static String getPathFromLocation(PackType pPackType, ResourceLocation pLocation) {
      return String.format(Locale.ROOT, "%s/%s/%s", pPackType.getDirectory(), pLocation.getNamespace(), pLocation.getPath());
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getRootResource(String... pElements) {
      return this.getResource(String.join("/", pElements));
   }

   @Override
   public IoSupplier<InputStream> getResource(PackType pPackType, ResourceLocation pLocation) {
      return this.getResource(getPathFromLocation(pPackType, pLocation));
   }

   private String addPrefix(String pResourcePath) {
      return this.prefix.isEmpty() ? pResourcePath : this.prefix + "/" + pResourcePath;
   }

   @Nullable
   private IoSupplier<InputStream> getResource(String pResourcePath) {
      ZipFile $$1 = this.zipFileAccess.getOrCreateZipFile();
      if ($$1 == null) {
         return null;
      } else {
         ZipEntry $$2 = $$1.getEntry(this.addPrefix(pResourcePath));
         return $$2 == null ? null : IoSupplier.create($$1, $$2);
      }
   }

   @Override
   public Set<String> getNamespaces(PackType pType) {
      ZipFile $$1 = this.zipFileAccess.getOrCreateZipFile();
      if ($$1 == null) {
         return Set.of();
      } else {
         Enumeration<? extends ZipEntry> $$2 = $$1.entries();
         Set<String> $$3 = Sets.newHashSet();
         String $$4 = this.addPrefix(pType.getDirectory() + "/");

         while ($$2.hasMoreElements()) {
            ZipEntry $$5 = $$2.nextElement();
            String $$6 = $$5.getName();
            String $$7 = extractNamespace($$4, $$6);
            if (!$$7.isEmpty()) {
               if (ResourceLocation.isValidNamespace($$7)) {
                  $$3.add($$7);
               } else {
                  LOGGER.warn("Non [a-z0-9_.-] character in namespace {} in pack {}, ignoring", $$7, this.zipFileAccess.file);
               }
            }
         }

         return $$3;
      }
   }

   @VisibleForTesting
   public static String extractNamespace(String pDirectory, String pName) {
      if (!pName.startsWith(pDirectory)) {
         return "";
      } else {
         int $$2 = pDirectory.length();
         int $$3 = pName.indexOf(47, $$2);
         return $$3 == -1 ? pName.substring($$2) : pName.substring($$2, $$3);
      }
   }

   @Override
   public void close() {
      this.zipFileAccess.close();
   }

   @Override
   public void listResources(PackType pPackType, String pNamespace, String pPath, PackResources.ResourceOutput pResourceOutput) {
      ZipFile $$4 = this.zipFileAccess.getOrCreateZipFile();
      if ($$4 != null) {
         Enumeration<? extends ZipEntry> $$5 = $$4.entries();
         String $$6 = this.addPrefix(pPackType.getDirectory() + "/" + pNamespace + "/");
         String $$7 = $$6 + pPath + "/";

         while ($$5.hasMoreElements()) {
            ZipEntry $$8 = $$5.nextElement();
            if (!$$8.isDirectory()) {
               String $$9 = $$8.getName();
               if ($$9.startsWith($$7)) {
                  String $$10 = $$9.substring($$6.length());
                  ResourceLocation $$11 = ResourceLocation.tryBuild(pNamespace, $$10);
                  if ($$11 != null) {
                     pResourceOutput.accept($$11, IoSupplier.create($$4, $$8));
                  } else {
                     LOGGER.warn("Invalid path in datapack: {}:{}, ignoring", pNamespace, $$10);
                  }
               }
            }
         }
      }
   }

   public static class FileResourcesSupplier implements Pack.ResourcesSupplier {
      private final File content;

      public FileResourcesSupplier(Path pContent) {
         this(pContent.toFile());
      }

      public FileResourcesSupplier(File pContent) {
         this.content = pContent;
      }

      @Override
      public PackResources openPrimary(PackLocationInfo pLocation) {
         FilePackResources.SharedZipFileAccess $$1 = new FilePackResources.SharedZipFileAccess(this.content);
         return new FilePackResources(pLocation, $$1, "");
      }

      @Override
      public PackResources openFull(PackLocationInfo pLocation, Pack.Metadata pMetadata) {
         FilePackResources.SharedZipFileAccess $$2 = new FilePackResources.SharedZipFileAccess(this.content);
         PackResources $$3 = new FilePackResources(pLocation, $$2, "");
         List<String> $$4 = pMetadata.overlays();
         if ($$4.isEmpty()) {
            return $$3;
         } else {
            List<PackResources> $$5 = new ArrayList<>($$4.size());

            for (String $$6 : $$4) {
               $$5.add(new FilePackResources(pLocation, $$2, $$6));
            }

            return new CompositePackResources($$3, $$5);
         }
      }
   }

   static class SharedZipFileAccess implements AutoCloseable {
      final File file;
      @Nullable
      private ZipFile zipFile;
      private boolean failedToLoad;

      SharedZipFileAccess(File pFile) {
         this.file = pFile;
      }

      @Nullable
      ZipFile getOrCreateZipFile() {
         if (this.failedToLoad) {
            return null;
         } else {
            if (this.zipFile == null) {
               try {
                  this.zipFile = new ZipFile(this.file);
               } catch (IOException var2) {
                  FilePackResources.LOGGER.error("Failed to open pack {}", this.file, var2);
                  this.failedToLoad = true;
                  return null;
               }
            }

            return this.zipFile;
         }
      }

      @Override
      public void close() {
         if (this.zipFile != null) {
            IOUtils.closeQuietly(this.zipFile);
            this.zipFile = null;
         }
      }

      @Override
      protected void finalize() throws Throwable {
         this.close();
         super.finalize();
      }
   }
}
