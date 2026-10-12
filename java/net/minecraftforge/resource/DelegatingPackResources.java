package net.minecraftforge.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class DelegatingPackResources extends AbstractPackResources {
   private final PackMetadataSection packMeta;
   private final List<PackResources> delegates;
   private final Map<String, List<PackResources>> namespacesAssets;
   private final Map<String, List<PackResources>> namespacesData;

   public DelegatingPackResources(PackLocationInfo info, PackMetadataSection packMeta, List<? extends PackResources> packs) {
      super(info);
      this.packMeta = packMeta;
      this.delegates = ImmutableList.copyOf(packs);
      this.namespacesAssets = this.buildNamespaceMap(PackType.CLIENT_RESOURCES, this.delegates);
      this.namespacesData = this.buildNamespaceMap(PackType.SERVER_DATA, this.delegates);
   }

   private Map<String, List<PackResources>> buildNamespaceMap(PackType type, List<PackResources> packList) {
      Map<String, List<PackResources>> map = new HashMap<>();

      for (PackResources pack : packList) {
         for (String namespace : pack.getNamespaces(type)) {
            map.computeIfAbsent(namespace, k -> new ArrayList<>()).add(pack);
         }
      }

      map.replaceAll((k, list) -> ImmutableList.copyOf(list));
      return ImmutableMap.copyOf(map);
   }

   @Nullable
   @Override
   public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) throws IOException {
      return (T)(deserializer.getMetadataSectionName().equals("pack") ? this.packMeta : null);
   }

   @Override
   public void listResources(PackType type, String resourceNamespace, String paths, PackResources.ResourceOutput resourceOutput) {
      for (PackResources delegate : this.delegates) {
         delegate.listResources(type, resourceNamespace, paths, resourceOutput);
      }
   }

   @Override
   public Set<String> getNamespaces(PackType type) {
      return type == PackType.CLIENT_RESOURCES ? this.namespacesAssets.keySet() : this.namespacesData.keySet();
   }

   @Override
   public void close() {
      for (PackResources pack : this.delegates) {
         pack.close();
      }
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getRootResource(String... paths) {
      return null;
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
      for (PackResources pack : this.getCandidatePacks(type, location)) {
         IoSupplier<InputStream> ioSupplier = pack.getResource(type, location);
         if (ioSupplier != null) {
            return ioSupplier;
         }
      }

      return null;
   }

   @Nullable
   @Override
   public Collection<PackResources> getChildren() {
      return this.delegates;
   }

   private List<PackResources> getCandidatePacks(PackType type, ResourceLocation location) {
      Map<String, List<PackResources>> map = type == PackType.CLIENT_RESOURCES ? this.namespacesAssets : this.namespacesData;
      List<PackResources> packsWithNamespace = map.get(location.getNamespace());
      return packsWithNamespace == null ? Collections.emptyList() : packsWithNamespace;
   }

   public Pack.ResourcesSupplier supplier() {
      return new DelegatingPackResources.Supplier();
   }

   private class Supplier implements Pack.ResourcesSupplier {
      @Override
      public PackResources openPrimary(PackLocationInfo p_332103_) {
         return DelegatingPackResources.this;
      }

      @Override
      public PackResources openFull(PackLocationInfo p_330351_, Pack.Metadata p_333429_) {
         return DelegatingPackResources.this;
      }
   }
}
