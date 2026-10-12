package net.minecraft.server.packs.resources;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;

public interface ResourceMetadata {
   ResourceMetadata EMPTY = new ResourceMetadata() {
      @Override
      public <T> Optional<T> getSection(MetadataSectionSerializer<T> p_215584_) {
         return Optional.empty();
      }
   };
   IoSupplier<ResourceMetadata> EMPTY_SUPPLIER = () -> EMPTY;

   static ResourceMetadata fromJsonStream(InputStream pStream) throws IOException {
      ResourceMetadata var3;
      try (BufferedReader $$1 = new BufferedReader(new InputStreamReader(pStream, StandardCharsets.UTF_8))) {
         final JsonObject $$2 = GsonHelper.parse($$1);
         var3 = new ResourceMetadata() {
            @Override
            public <T> Optional<T> getSection(MetadataSectionSerializer<T> p_215589_) {
               String $$1 = p_215589_.getMetadataSectionName();
               return $$2.has($$1) ? Optional.of(p_215589_.fromJson(GsonHelper.getAsJsonObject($$2, $$1))) : Optional.empty();
            }
         };
      }

      return var3;
   }

   <T> Optional<T> getSection(MetadataSectionSerializer<T> pSerializer);

   default ResourceMetadata copySections(Collection<MetadataSectionSerializer<?>> pSerializers) {
      ResourceMetadata.Builder $$1 = new ResourceMetadata.Builder();

      for (MetadataSectionSerializer<?> $$2 : pSerializers) {
         this.copySection($$1, $$2);
      }

      return $$1.build();
   }

   private <T> void copySection(ResourceMetadata.Builder pBuilder, MetadataSectionSerializer<T> pSerializer) {
      this.getSection(pSerializer).ifPresent(p_296603_ -> pBuilder.put(pSerializer, (T)p_296603_));
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableMap.Builder<MetadataSectionSerializer<?>, Object> map = ImmutableMap.builder();

      public <T> ResourceMetadata.Builder put(MetadataSectionSerializer<T> pKey, T pValue) {
         this.map.put(pKey, pValue);
         return this;
      }

      public ResourceMetadata build() {
         final ImmutableMap<MetadataSectionSerializer<?>, Object> $$0 = this.map.build();
         return $$0.isEmpty() ? ResourceMetadata.EMPTY : new ResourceMetadata() {
            @Override
            public <T> Optional<T> getSection(MetadataSectionSerializer<T> p_299920_) {
               return Optional.ofNullable((T)$$0.get(p_299920_));
            }
         };
      }
   }
}
