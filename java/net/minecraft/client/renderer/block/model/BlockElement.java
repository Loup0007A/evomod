package net.minecraft.client.renderer.block.model;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class BlockElement {
   private static final boolean DEFAULT_RESCALE = false;
   private static final float MIN_EXTENT = -16.0F;
   private static final float MAX_EXTENT = 32.0F;
   public final Vector3f from;
   public final Vector3f to;
   public final Map<Direction, BlockElementFace> faces;
   public final BlockElementRotation rotation;
   public final boolean shade;

   public BlockElement(Vector3f pFrom, Vector3f pTo, Map<Direction, BlockElementFace> pFaces, @Nullable BlockElementRotation pRotation, boolean pShade) {
      this.from = pFrom;
      this.to = pTo;
      this.faces = pFaces;
      this.rotation = pRotation;
      this.shade = pShade;
      this.fillUvs();
   }

   private void fillUvs() {
      for (Entry<Direction, BlockElementFace> $$0 : this.faces.entrySet()) {
         float[] $$1 = this.uvsByFace($$0.getKey());
         $$0.getValue().uv().setMissingUv($$1);
      }
   }

   public float[] uvsByFace(Direction pFace) {
      switch (pFace) {
         case DOWN:
            return new float[]{this.from.x(), 16.0F - this.to.z(), this.to.x(), 16.0F - this.from.z()};
         case UP:
            return new float[]{this.from.x(), this.from.z(), this.to.x(), this.to.z()};
         case NORTH:
         default:
            return new float[]{16.0F - this.to.x(), 16.0F - this.to.y(), 16.0F - this.from.x(), 16.0F - this.from.y()};
         case SOUTH:
            return new float[]{this.from.x(), 16.0F - this.to.y(), this.to.x(), 16.0F - this.from.y()};
         case WEST:
            return new float[]{this.from.z(), 16.0F - this.to.y(), this.to.z(), 16.0F - this.from.y()};
         case EAST:
            return new float[]{16.0F - this.to.z(), 16.0F - this.to.y(), 16.0F - this.from.z(), 16.0F - this.from.y()};
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Deserializer implements JsonDeserializer<BlockElement> {
      private static final boolean DEFAULT_SHADE = true;

      public BlockElement deserialize(JsonElement pJson, Type pType, JsonDeserializationContext pContext) throws JsonParseException {
         JsonObject $$3 = pJson.getAsJsonObject();
         Vector3f $$4 = this.getFrom($$3);
         Vector3f $$5 = this.getTo($$3);
         BlockElementRotation $$6 = this.getRotation($$3);
         Map<Direction, BlockElementFace> $$7 = this.getFaces(pContext, $$3);
         if ($$3.has("shade") && !GsonHelper.isBooleanValue($$3, "shade")) {
            throw new JsonParseException("Expected shade to be a Boolean");
         } else {
            boolean $$8 = GsonHelper.getAsBoolean($$3, "shade", true);
            return new BlockElement($$4, $$5, $$7, $$6, $$8);
         }
      }

      @Nullable
      private BlockElementRotation getRotation(JsonObject pJson) {
         BlockElementRotation $$1 = null;
         if (pJson.has("rotation")) {
            JsonObject $$2 = GsonHelper.getAsJsonObject(pJson, "rotation");
            Vector3f $$3 = this.getVector3f($$2, "origin");
            $$3.mul(0.0625F);
            Direction.Axis $$4 = this.getAxis($$2);
            float $$5 = this.getAngle($$2);
            boolean $$6 = GsonHelper.getAsBoolean($$2, "rescale", false);
            $$1 = new BlockElementRotation($$3, $$4, $$5, $$6);
         }

         return $$1;
      }

      private float getAngle(JsonObject pJson) {
         float $$1 = GsonHelper.getAsFloat(pJson, "angle");
         if ($$1 != 0.0F && Mth.abs($$1) != 22.5F && Mth.abs($$1) != 45.0F) {
            throw new JsonParseException("Invalid rotation " + $$1 + " found, only -45/-22.5/0/22.5/45 allowed");
         } else {
            return $$1;
         }
      }

      private Direction.Axis getAxis(JsonObject pJson) {
         String $$1 = GsonHelper.getAsString(pJson, "axis");
         Direction.Axis $$2 = Direction.Axis.byName($$1.toLowerCase(Locale.ROOT));
         if ($$2 == null) {
            throw new JsonParseException("Invalid rotation axis: " + $$1);
         } else {
            return $$2;
         }
      }

      private Map<Direction, BlockElementFace> getFaces(JsonDeserializationContext pContext, JsonObject pJson) {
         Map<Direction, BlockElementFace> $$2 = this.filterNullFromFaces(pContext, pJson);
         if ($$2.isEmpty()) {
            throw new JsonParseException("Expected between 1 and 6 unique faces, got 0");
         } else {
            return $$2;
         }
      }

      private Map<Direction, BlockElementFace> filterNullFromFaces(JsonDeserializationContext pContext, JsonObject pJson) {
         Map<Direction, BlockElementFace> $$2 = Maps.newEnumMap(Direction.class);
         JsonObject $$3 = GsonHelper.getAsJsonObject(pJson, "faces");

         for (Entry<String, JsonElement> $$4 : $$3.entrySet()) {
            Direction $$5 = this.getFacing($$4.getKey());
            $$2.put($$5, (BlockElementFace)pContext.deserialize($$4.getValue(), BlockElementFace.class));
         }

         return $$2;
      }

      private Direction getFacing(String pName) {
         Direction $$1 = Direction.byName(pName);
         if ($$1 == null) {
            throw new JsonParseException("Unknown facing: " + pName);
         } else {
            return $$1;
         }
      }

      private Vector3f getTo(JsonObject pJson) {
         Vector3f $$1 = this.getVector3f(pJson, "to");
         if (!($$1.x() < -16.0F) && !($$1.y() < -16.0F) && !($$1.z() < -16.0F) && !($$1.x() > 32.0F) && !($$1.y() > 32.0F) && !($$1.z() > 32.0F)) {
            return $$1;
         } else {
            throw new JsonParseException("'to' specifier exceeds the allowed boundaries: " + $$1);
         }
      }

      private Vector3f getFrom(JsonObject pJson) {
         Vector3f $$1 = this.getVector3f(pJson, "from");
         if (!($$1.x() < -16.0F) && !($$1.y() < -16.0F) && !($$1.z() < -16.0F) && !($$1.x() > 32.0F) && !($$1.y() > 32.0F) && !($$1.z() > 32.0F)) {
            return $$1;
         } else {
            throw new JsonParseException("'from' specifier exceeds the allowed boundaries: " + $$1);
         }
      }

      private Vector3f getVector3f(JsonObject pJson, String pMemberName) {
         JsonArray $$2 = GsonHelper.getAsJsonArray(pJson, pMemberName);
         if ($$2.size() != 3) {
            throw new JsonParseException("Expected 3 " + pMemberName + " values, found: " + $$2.size());
         } else {
            float[] $$3 = new float[3];

            for (int $$4 = 0; $$4 < $$3.length; $$4++) {
               $$3[$$4] = GsonHelper.convertToFloat($$2.get($$4), pMemberName + "[" + $$4 + "]");
            }

            return new Vector3f($$3[0], $$3[1], $$3[2]);
         }
      }
   }
}
