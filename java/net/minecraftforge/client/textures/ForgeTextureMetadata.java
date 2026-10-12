package net.minecraftforge.client.textures;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ForgeTextureMetadata(@Nullable ITextureAtlasSpriteLoader loader) {
   public static final ForgeTextureMetadata EMPTY = new ForgeTextureMetadata(null);
   public static final MetadataSectionSerializer<ForgeTextureMetadata> SERIALIZER = new ForgeTextureMetadata.Serializer();

   public static ForgeTextureMetadata forResource(Resource resource) throws IOException {
      Optional<ForgeTextureMetadata> metadata = resource.metadata().getSection(SERIALIZER);
      return metadata.orElse(EMPTY);
   }

   @Nullable
   public ITextureAtlasSpriteLoader getLoader() {
      return this.loader;
   }

   private static final class Serializer implements MetadataSectionSerializer<ForgeTextureMetadata> {
      @NotNull
      @Override
      public String getMetadataSectionName() {
         return "forge";
      }

      @NotNull
      public ForgeTextureMetadata fromJson(JsonObject json) {
         ITextureAtlasSpriteLoader loader;
         if (json.has("loader")) {
            ResourceLocation loaderName = ResourceLocation.parse(GsonHelper.getAsString(json, "loader"));
            loader = TextureAtlasSpriteLoaderManager.get(loaderName);
            if (loader == null) {
               throw new JsonSyntaxException("Unknown TextureAtlasSpriteLoader " + loaderName);
            }
         } else {
            loader = null;
         }

         return new ForgeTextureMetadata(loader);
      }
   }
}
