package net.minecraft.server.packs;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.slf4j.Logger;

public class VanillaPackResources implements PackResources {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final PackLocationInfo location;
   private final BuiltInMetadata metadata;
   private final Set<String> namespaces;
   private final List<Path> rootPaths;
   private final Map<PackType, List<Path>> pathsForType;

   VanillaPackResources(
      PackLocationInfo pLocation, BuiltInMetadata pMetadata, Set<String> pNamespaces, List<Path> pRootPaths, Map<PackType, List<Path>> pPathsForType
   ) {
      this.location = pLocation;
      this.metadata = pMetadata;
      this.namespaces = pNamespaces;
      this.rootPaths = pRootPaths;
      this.pathsForType = pPathsForType;
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getRootResource(String... pElements) {
      FileUtil.validatePath(pElements);
      List<String> $$1 = List.of(pElements);

      for (Path $$2 : this.rootPaths) {
         Path $$3 = FileUtil.resolvePath($$2, $$1);
         if (Files.exists($$3) && PathPackResources.validatePath($$3)) {
            return IoSupplier.create($$3);
         }
      }

      return null;
   }

   public void listRawPaths(PackType pPackType, ResourceLocation pPackLocation, Consumer<Path> pOutput) {
      FileUtil.decomposePath(pPackLocation.getPath()).ifSuccess(p_248238_ -> {
         String $$4 = pPackLocation.getNamespace();

         for (Path $$5 : this.pathsForType.get(pPackType)) {
            Path $$6 = $$5.resolve($$4);
            pOutput.accept(FileUtil.resolvePath($$6, p_248238_));
         }
      }).ifError(p_326467_ -> LOGGER.error("Invalid path {}: {}", pPackLocation, p_326467_.message()));
   }

   @Override
   public void listResources(PackType pPackType, String pNamespace, String pPath, PackResources.ResourceOutput pResourceOutput) {
      FileUtil.decomposePath(pPath).ifSuccess(p_248228_ -> {
         List<Path> $$4 = this.pathsForType.get(pPackType);
         int $$5 = $$4.size();
         if ($$5 == 1) {
            getResources(pResourceOutput, pNamespace, $$4.get(0), p_248228_);
         } else if ($$5 > 1) {
            Map<ResourceLocation, IoSupplier<InputStream>> $$6 = new HashMap<>();

            for (int $$7 = 0; $$7 < $$5 - 1; $$7++) {
               getResources($$6::putIfAbsent, pNamespace, $$4.get($$7), p_248228_);
            }

            Path $$8 = $$4.get($$5 - 1);
            if ($$6.isEmpty()) {
               getResources(pResourceOutput, pNamespace, $$8, p_248228_);
            } else {
               getResources($$6::putIfAbsent, pNamespace, $$8, p_248228_);
               $$6.forEach(pResourceOutput);
            }
         }
      }).ifError(p_326469_ -> LOGGER.error("Invalid path {}: {}", pPath, p_326469_.message()));
   }

   private static void getResources(PackResources.ResourceOutput pResourceOutput, String pNamespace, Path pRoot, List<String> pPaths) {
      Path $$4 = pRoot.resolve(pNamespace);
      PathPackResources.listPath(pNamespace, $$4, pPaths, pResourceOutput);
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getResource(PackType pPackType, ResourceLocation pLocation) {
      return (IoSupplier<InputStream>)FileUtil.decomposePath(pLocation.getPath()).mapOrElse(p_248224_ -> {
         String $$3 = pLocation.getNamespace();

         for (Path $$4 : this.pathsForType.get(pPackType)) {
            Path $$5 = FileUtil.resolvePath($$4.resolve($$3), p_248224_);
            if (Files.exists($$5) && PathPackResources.validatePath($$5)) {
               return IoSupplier.create($$5);
            }
         }

         return null;
      }, p_326471_ -> {
         LOGGER.error("Invalid path {}: {}", pLocation, p_326471_.message());
         return null;
      });
   }

   @Override
   public Set<String> getNamespaces(PackType pType) {
      return this.namespaces;
   }

   @Nullable
   @Override
   public <T> T getMetadataSection(MetadataSectionSerializer<T> pDeserializer) {
      IoSupplier<InputStream> $$1 = this.getRootResource("pack.mcmeta");
      if ($$1 != null) {
         try (InputStream $$2 = $$1.get()) {
            T $$3 = AbstractPackResources.getMetadataFromStream(pDeserializer, $$2);
            if ($$3 != null) {
               return $$3;
            }

            return this.metadata.get(pDeserializer);
         } catch (IOException var8) {
         }
      }

      return this.metadata.get(pDeserializer);
   }

   @Override
   public PackLocationInfo location() {
      return this.location;
   }

   @Override
   public void close() {
   }

   public ResourceProvider asProvider() {
      return p_248239_ -> Optional.ofNullable(this.getResource(PackType.CLIENT_RESOURCES, p_248239_))
            .map(p_248221_ -> new Resource(this, (IoSupplier<InputStream>)p_248221_));
   }
}
