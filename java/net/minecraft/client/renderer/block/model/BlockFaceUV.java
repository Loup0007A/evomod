package net.minecraft.client.renderer.block.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import javax.annotation.Nullable;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockFaceUV {
   public float[] uvs;
   public final int rotation;

   public BlockFaceUV(@Nullable float[] pUvs, int pRotation) {
      this.uvs = pUvs;
      this.rotation = pRotation;
   }

   public float getU(int pIndex) {
      if (this.uvs == null) {
         throw new NullPointerException("uvs");
      } else {
         int $$1 = this.getShiftedIndex(pIndex);
         return this.uvs[$$1 != 0 && $$1 != 1 ? 2 : 0];
      }
   }

   public float getV(int pIndex) {
      if (this.uvs == null) {
         throw new NullPointerException("uvs");
      } else {
         int $$1 = this.getShiftedIndex(pIndex);
         return this.uvs[$$1 != 0 && $$1 != 3 ? 3 : 1];
      }
   }

   private int getShiftedIndex(int pIndex) {
      return (pIndex + this.rotation / 90) % 4;
   }

   public int getReverseIndex(int pIndex) {
      return (pIndex + 4 - this.rotation / 90) % 4;
   }

   public void setMissingUv(float[] pUvs) {
      if (this.uvs == null) {
         this.uvs = pUvs;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Deserializer implements JsonDeserializer<BlockFaceUV> {
      private static final int DEFAULT_ROTATION = 0;

      public BlockFaceUV deserialize(JsonElement pJson, Type pType, JsonDeserializationContext pContext) throws JsonParseException {
         JsonObject $$3 = pJson.getAsJsonObject();
         float[] $$4 = this.getUVs($$3);
         int $$5 = this.getRotation($$3);
         return new BlockFaceUV($$4, $$5);
      }

      protected int getRotation(JsonObject pJson) {
         int $$1 = GsonHelper.getAsInt(pJson, "rotation", 0);
         if ($$1 >= 0 && $$1 % 90 == 0 && $$1 / 90 <= 3) {
            return $$1;
         } else {
            throw new JsonParseException("Invalid rotation " + $$1 + " found, only 0/90/180/270 allowed");
         }
      }

      @Nullable
      private float[] getUVs(JsonObject pJson) {
         if (!pJson.has("uv")) {
            return null;
         } else {
            JsonArray $$1 = GsonHelper.getAsJsonArray(pJson, "uv");
            if ($$1.size() != 4) {
               throw new JsonParseException("Expected 4 uv values, found: " + $$1.size());
            } else {
               float[] $$2 = new float[4];

               for (int $$3 = 0; $$3 < $$2.length; $$3++) {
                  $$2[$$3] = GsonHelper.convertToFloat($$1.get($$3), "uv[" + $$3 + "]");
               }

               return $$2;
            }
         }
      }
   }
}
