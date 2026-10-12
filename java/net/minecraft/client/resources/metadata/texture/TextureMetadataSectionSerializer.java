package net.minecraft.client.resources.metadata.texture;

import com.google.gson.JsonObject;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TextureMetadataSectionSerializer implements MetadataSectionSerializer<TextureMetadataSection> {
   public TextureMetadataSection fromJson(JsonObject pJson) {
      boolean $$1 = GsonHelper.getAsBoolean(pJson, "blur", false);
      boolean $$2 = GsonHelper.getAsBoolean(pJson, "clamp", false);
      return new TextureMetadataSection($$1, $$2);
   }

   @Override
   public String getMetadataSectionName() {
      return "texture";
   }
}
