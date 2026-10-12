package com.mojang.realmsclient.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.util.UndashedUuid;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class JsonUtils {
   public static <T> T getRequired(String pKey, JsonObject pJson, Function<JsonObject, T> pOutput) {
      JsonElement $$3 = pJson.get(pKey);
      if ($$3 == null || $$3.isJsonNull()) {
         throw new IllegalStateException("Missing required property: " + pKey);
      } else if (!$$3.isJsonObject()) {
         throw new IllegalStateException("Required property " + pKey + " was not a JsonObject as espected");
      } else {
         return pOutput.apply($$3.getAsJsonObject());
      }
   }

   @Nullable
   public static <T> T getOptional(String pKey, JsonObject pJson, Function<JsonObject, T> pOutput) {
      JsonElement $$3 = pJson.get(pKey);
      if ($$3 == null || $$3.isJsonNull()) {
         return null;
      } else if (!$$3.isJsonObject()) {
         throw new IllegalStateException("Required property " + pKey + " was not a JsonObject as espected");
      } else {
         return pOutput.apply($$3.getAsJsonObject());
      }
   }

   public static String getRequiredString(String pKey, JsonObject pJson) {
      String $$2 = getStringOr(pKey, pJson, null);
      if ($$2 == null) {
         throw new IllegalStateException("Missing required property: " + pKey);
      } else {
         return $$2;
      }
   }

   public static String getRequiredStringOr(String pKey, JsonObject pJson, String pDefaultValue) {
      JsonElement $$3 = pJson.get(pKey);
      if ($$3 != null) {
         return $$3.isJsonNull() ? pDefaultValue : $$3.getAsString();
      } else {
         return pDefaultValue;
      }
   }

   @Nullable
   public static String getStringOr(String pKey, JsonObject pJson, @Nullable String pDefaultValue) {
      JsonElement $$3 = pJson.get(pKey);
      if ($$3 != null) {
         return $$3.isJsonNull() ? pDefaultValue : $$3.getAsString();
      } else {
         return pDefaultValue;
      }
   }

   @Nullable
   public static UUID getUuidOr(String pKey, JsonObject pJson, @Nullable UUID pDefaultValue) {
      String $$3 = getStringOr(pKey, pJson, null);
      return $$3 == null ? pDefaultValue : UndashedUuid.fromStringLenient($$3);
   }

   public static int getIntOr(String pKey, JsonObject pJson, int pDefaultValue) {
      JsonElement $$3 = pJson.get(pKey);
      if ($$3 != null) {
         return $$3.isJsonNull() ? pDefaultValue : $$3.getAsInt();
      } else {
         return pDefaultValue;
      }
   }

   public static long getLongOr(String pKey, JsonObject pJson, long pDefaultValue) {
      JsonElement $$3 = pJson.get(pKey);
      if ($$3 != null) {
         return $$3.isJsonNull() ? pDefaultValue : $$3.getAsLong();
      } else {
         return pDefaultValue;
      }
   }

   public static boolean getBooleanOr(String pKey, JsonObject pJson, boolean pDefaultValue) {
      JsonElement $$3 = pJson.get(pKey);
      if ($$3 != null) {
         return $$3.isJsonNull() ? pDefaultValue : $$3.getAsBoolean();
      } else {
         return pDefaultValue;
      }
   }

   public static Date getDateOr(String pKey, JsonObject pJson) {
      JsonElement $$2 = pJson.get(pKey);
      return $$2 != null ? new Date(Long.parseLong($$2.getAsString())) : new Date();
   }
}
