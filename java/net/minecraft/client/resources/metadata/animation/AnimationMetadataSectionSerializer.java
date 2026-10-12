package net.minecraft.client.resources.metadata.animation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import javax.annotation.Nullable;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.Validate;

@OnlyIn(Dist.CLIENT)
public class AnimationMetadataSectionSerializer implements MetadataSectionSerializer<AnimationMetadataSection> {
   public AnimationMetadataSection fromJson(JsonObject pJson) {
      Builder<AnimationFrame> $$1 = ImmutableList.builder();
      int $$2 = GsonHelper.getAsInt(pJson, "frametime", 1);
      if ($$2 != 1) {
         Validate.inclusiveBetween(1L, 2147483647L, (long)$$2, "Invalid default frame time");
      }

      if (pJson.has("frames")) {
         try {
            JsonArray $$3 = GsonHelper.getAsJsonArray(pJson, "frames");

            for (int $$4 = 0; $$4 < $$3.size(); $$4++) {
               JsonElement $$5 = $$3.get($$4);
               AnimationFrame $$6 = this.getFrame($$4, $$5);
               if ($$6 != null) {
                  $$1.add($$6);
               }
            }
         } catch (ClassCastException var8) {
            throw new JsonParseException("Invalid animation->frames: expected array, was " + pJson.get("frames"), var8);
         }
      }

      int $$8 = GsonHelper.getAsInt(pJson, "width", -1);
      int $$9 = GsonHelper.getAsInt(pJson, "height", -1);
      if ($$8 != -1) {
         Validate.inclusiveBetween(1L, 2147483647L, (long)$$8, "Invalid width");
      }

      if ($$9 != -1) {
         Validate.inclusiveBetween(1L, 2147483647L, (long)$$9, "Invalid height");
      }

      boolean $$10 = GsonHelper.getAsBoolean(pJson, "interpolate", false);
      return new AnimationMetadataSection($$1.build(), $$8, $$9, $$2, $$10);
   }

   @Nullable
   private AnimationFrame getFrame(int pFrame, JsonElement pElement) {
      if (pElement.isJsonPrimitive()) {
         return new AnimationFrame(GsonHelper.convertToInt(pElement, "frames[" + pFrame + "]"));
      } else if (pElement.isJsonObject()) {
         JsonObject $$2 = GsonHelper.convertToJsonObject(pElement, "frames[" + pFrame + "]");
         int $$3 = GsonHelper.getAsInt($$2, "time", -1);
         if ($$2.has("time")) {
            Validate.inclusiveBetween(1L, 2147483647L, (long)$$3, "Invalid frame time");
         }

         int $$4 = GsonHelper.getAsInt($$2, "index");
         Validate.inclusiveBetween(0L, 2147483647L, (long)$$4, "Invalid frame index");
         return new AnimationFrame($$4, $$3);
      } else {
         return null;
      }
   }

   @Override
   public String getMetadataSectionName() {
      return "animation";
   }
}
