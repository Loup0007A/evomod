package net.minecraft.server.packs;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;

public class CompositePackResources implements PackResources {
   private final PackResources primaryPackResources;
   private final List<PackResources> packResourcesStack;

   public CompositePackResources(PackResources pPrimaryPackResources, List<PackResources> pPackResourcesStack) {
      this.primaryPackResources = pPrimaryPackResources;
      List<PackResources> $$2 = new ArrayList<>(pPackResourcesStack.size() + 1);
      $$2.addAll(Lists.reverse(pPackResourcesStack));
      $$2.add(pPrimaryPackResources);
      this.packResourcesStack = List.copyOf($$2);
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getRootResource(String... pElements) {
      return this.primaryPackResources.getRootResource(pElements);
   }

   @Nullable
   @Override
   public IoSupplier<InputStream> getResource(PackType pPackType, ResourceLocation pLocation) {
      for (PackResources $$2 : this.packResourcesStack) {
         IoSupplier<InputStream> $$3 = $$2.getResource(pPackType, pLocation);
         if ($$3 != null) {
            return $$3;
         }
      }

      return null;
   }

   @Override
   public void listResources(PackType pPackType, String pNamespace, String pPath, PackResources.ResourceOutput pResourceOutput) {
      Map<ResourceLocation, IoSupplier<InputStream>> $$4 = new HashMap<>();

      for (PackResources $$5 : this.packResourcesStack) {
         $$5.listResources(pPackType, pNamespace, pPath, $$4::putIfAbsent);
      }

      $$4.forEach(pResourceOutput);
   }

   @Override
   public Set<String> getNamespaces(PackType pType) {
      Set<String> $$1 = new HashSet<>();

      for (PackResources $$2 : this.packResourcesStack) {
         $$1.addAll($$2.getNamespaces(pType));
      }

      return $$1;
   }

   @Nullable
   @Override
   public <T> T getMetadataSection(MetadataSectionSerializer<T> pDeserializer) throws IOException {
      return this.primaryPackResources.getMetadataSection(pDeserializer);
   }

   @Override
   public PackLocationInfo location() {
      return this.primaryPackResources.location();
   }

   @Override
   public void close() {
      this.packResourcesStack.forEach(PackResources::close);
   }
}
