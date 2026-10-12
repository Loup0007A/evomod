package net.minecraft.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;

public class GsonHelper {
   private static final Gson GSON = new GsonBuilder().create();

   public static boolean isStringValue(JsonObject pJson, String pMemberName) {
      return !isValidPrimitive(pJson, pMemberName) ? false : pJson.getAsJsonPrimitive(pMemberName).isString();
   }

   public static boolean isStringValue(JsonElement pJson) {
      return !pJson.isJsonPrimitive() ? false : pJson.getAsJsonPrimitive().isString();
   }

   public static boolean isNumberValue(JsonObject pJson, String pMemberName) {
      return !isValidPrimitive(pJson, pMemberName) ? false : pJson.getAsJsonPrimitive(pMemberName).isNumber();
   }

   public static boolean isNumberValue(JsonElement pJson) {
      return !pJson.isJsonPrimitive() ? false : pJson.getAsJsonPrimitive().isNumber();
   }

   public static boolean isBooleanValue(JsonObject pJson, String pMemberName) {
      return !isValidPrimitive(pJson, pMemberName) ? false : pJson.getAsJsonPrimitive(pMemberName).isBoolean();
   }

   public static boolean isBooleanValue(JsonElement pJson) {
      return !pJson.isJsonPrimitive() ? false : pJson.getAsJsonPrimitive().isBoolean();
   }

   public static boolean isArrayNode(JsonObject pJson, String pMemberName) {
      return !isValidNode(pJson, pMemberName) ? false : pJson.get(pMemberName).isJsonArray();
   }

   public static boolean isObjectNode(JsonObject pJson, String pMemberName) {
      return !isValidNode(pJson, pMemberName) ? false : pJson.get(pMemberName).isJsonObject();
   }

   public static boolean isValidPrimitive(JsonObject pJson, String pMemberName) {
      return !isValidNode(pJson, pMemberName) ? false : pJson.get(pMemberName).isJsonPrimitive();
   }

   public static boolean isValidNode(@Nullable JsonObject pJson, String pMemberName) {
      return pJson == null ? false : pJson.get(pMemberName) != null;
   }

   public static JsonElement getNonNull(JsonObject pJson, String pMemberName) {
      JsonElement $$2 = pJson.get(pMemberName);
      if ($$2 != null && !$$2.isJsonNull()) {
         return $$2;
      } else {
         throw new JsonSyntaxException("Missing field " + pMemberName);
      }
   }

   public static String convertToString(JsonElement pJson, String pMemberName) {
      if (pJson.isJsonPrimitive()) {
         return pJson.getAsString();
      } else {
         throw new JsonSyntaxException("Expected " + pMemberName + " to be a string, was " + getType(pJson));
      }
   }

   public static String getAsString(JsonObject pJson, String pMemberName) {
      if (pJson.has(pMemberName)) {
         return convertToString(pJson.get(pMemberName), pMemberName);
      } else {
         throw new JsonSyntaxException("Missing " + pMemberName + ", expected to find a string");
      }
   }

   @Nullable
   @Contract("_,_,!null->!null;_,_,null->_")
   public static String getAsString(JsonObject pJson, String pMemberName, @Nullable String pFallback) {
      return pJson.has(pMemberName) ? convertToString(pJson.get(pMemberName), pMemberName) : pFallback;
   }

   public static Holder<Item> convertToItem(JsonElement pJson, String pMemberName) {
      if (pJson.isJsonPrimitive()) {
         String $$2 = pJson.getAsString();
         return BuiltInRegistries.ITEM
            .getHolder(ResourceLocation.parse($$2))
            .orElseThrow(() -> new JsonSyntaxException("Expected " + pMemberName + " to be an item, was unknown string '" + $$2 + "'"));
      } else {
         throw new JsonSyntaxException("Expected " + pMemberName + " to be an item, was " + getType(pJson));
      }
   }

   public static Holder<Item> getAsItem(JsonObject pJson, String pMemberName) {
      if (pJson.has(pMemberName)) {
         return convertToItem(pJson.get(pMemberName), pMemberName);
      } else {
         throw new JsonSyntaxException("Missing " + pMemberName + ", expected to find an item");
      }
   }

   @Nullable
   @Contract("_,_,!null->!null;_,_,null->_")
   public static Holder<Item> getAsItem(JsonObject pJson, String pMemberName, @Nullable Holder<Item> pFallback) {
      return pJson.has(pMemberName) ? convertToItem(pJson.get(pMemberName), pMemberName) : pFallback;
   }

   public static boolean convertToBoolean(JsonElement pJson, String pMemberName) {
      if (pJson.isJsonPrimitive()) {
         return pJson.getAsBoolean();
      } else {
         throw new JsonSyntaxException("Expected " + pMemberName + " to be a Boolean, was " + getType(pJson));
      }
   }

   public static boolean getAsBoolean(JsonObject pJson, String pMemberName) {
      if (pJson.has(pMemberName)) {
         return convertToBoolean(pJson.get(pMemberName), pMemberName);
      } else {
         throw new JsonSyntaxException("Missing " + pMemberName + ", expected to find a Boolean");
      }
   }

   public static boolean getAsBoolean(JsonObject pJson, String pMemberName, boolean pFallback) {
      return pJson.has(pMemberName) ? convertToBoolean(pJson.get(pMemberName), pMemberName) : pFallback;
   }

   public static double convertToDouble(JsonElement pJson, String pMemberName) {
      if (pJson.isJsonPrimitive() && pJson.getAsJsonPrimitive().isNumber()) {
         return pJson.getAsDouble();
      } else {
         throw new JsonSyntaxException("Expected " + pMemberName + " to be a Double, was " + getType(pJson));
      }
   }

   public static double getAsDouble(JsonObject pJson, String pMemberName) {
      if (pJson.has(pMemberName)) {
         return convertToDouble(pJson.get(pMemberName), pMemberName);
      } else {
         throw new JsonSyntaxException("Missing " + pMemberName + ", expected to find a Double");
      }
   }

   public static double getAsDouble(JsonObject pJson, String pMemberName, double pFallback) {
      return pJson.has(pMemberName) ? convertToDouble(pJson.get(pMemberName), pMemberName) : pFallback;
   }

   public static float convertToFloat(JsonElement pJson, String pMemberName) {
      if (pJson.isJsonPrimitive() && pJson.getAsJsonPrimitive().isNumber()) {
         return pJson.getAsFloat();
      } else {
         throw new JsonSyntaxException("Expected " + pMemberName + " to be a Float, was " + getType(pJson));
      }
   }

   public static float getAsFloat(JsonObject pJson, String pMemberName) {
      if (pJson.has(pMemberName)) {
         return convertToFloat(pJson.get(pMemberName), pMemberName);
      } else {
         throw new JsonSyntaxException("Missing " + pMemberName + ", expected to find a Float");
      }
   }

   public static float getAsFloat(JsonObject pJson, String pMemberName, float pFallback) {
      return pJson.has(pMemberName) ? convertToFloat(pJson.get(pMemberName), pMemberName) : pFallback;
   }

   public static long convertToLong(JsonElement pJson, String pMemberName) {
      if (pJson.isJsonPrimitive() && pJson.getAsJsonPrimitive().isNumber()) {
         return pJson.getAsLong();
      } else {
         throw new JsonSyntaxException("Expected " + pMemberName + " to be a Long, was " + getType(pJson));
      }
   }

   public static long getAsLong(JsonObject pJson, String pMemberName) {
      if (pJson.has(pMemberName)) {
         return convertToLong(pJson.get(pMemberName), pMemberName);
      } else {
         throw new JsonSyntaxException("Missing " + pMemberName + ", expected to find a Long");
      }
   }

   public static long getAsLong(JsonObject pJson, String pMemberName, long pFallback) {
      return pJson.has(pMemberName) ? convertToLong(pJson.get(pMemberName), pMemberName) : pFallback;
   }

   public static int convertToInt(JsonElement pJson, String pMemberName) {
      if (pJson.isJsonPrimitive() && pJson.getAsJsonPrimitive().isNumber()) {
         return pJson.getAsInt();
      } else {
         throw new JsonSyntaxException("Expected " + pMemberName + " to be a Int, was " + getType(pJson));
      }
   }

   public static int getAsInt(JsonObject pJson, String pMemberName) {
      if (pJson.has(pMemberName)) {
         return convertToInt(pJson.get(pMemberName), pMemberName);
      } else {
         throw new JsonSyntaxException("Missing " + pMemberName + ", expected to find a Int");
      }
   }

   public static int getAsInt(JsonObject pJson, String pMemberName, int pFallback) {
      return pJson.has(pMemberName) ? convertToInt(pJson.get(pMemberName), pMemberName) : pFallback;
   }

   public static byte convertToByte(JsonElement pJson, String pMemberName) {
      if (pJson.isJsonPrimitive() && pJson.getAsJsonPrimitive().isNumber()) {
         return pJson.getAsByte();
      } else {
         throw new JsonSyntaxException("Expected " + pMemberName + " to be a Byte, was " + getType(pJson));
      }
   }

   public static byte getAsByte(JsonObject pJson, String pMemberName) {
      if (pJson.has(pMemberName)) {
         return convertToByte(pJson.get(pMemberName), pMemberName);
      } else {
         throw new JsonSyntaxException("Missing " + pMemberName + ", expected to find a Byte");
      }
   }

   public static byte getAsByte(JsonObject pJson, String pMemberName, byte pFallback) {
      return pJson.has(pMemberName) ? convertToByte(pJson.get(pMemberName), pMemberName) : pFallback;
   }

   public static char convertToCharacter(JsonElement pJson, String pMemberName) {
      if (pJson.isJsonPrimitive() && pJson.getAsJsonPrimitive().isNumber()) {
         return pJson.getAsCharacter();
      } else {
         throw new JsonSyntaxException("Expected " + pMemberName + " to be a Character, was " + getType(pJson));
      }
   }

   public static char getAsCharacter(JsonObject pJson, String pMemberName) {
      if (pJson.has(pMemberName)) {
         return convertToCharacter(pJson.get(pMemberName), pMemberName);
      } else {
         throw new JsonSyntaxException("Missing " + pMemberName + ", expected to find a Character");
      }
   }

   public static char getAsCharacter(JsonObject pJson, String pMemberName, char pFallback) {
      return pJson.has(pMemberName) ? convertToCharacter(pJson.get(pMemberName), pMemberName) : pFallback;
   }

   public static BigDecimal convertToBigDecimal(JsonElement pJson, String pMemberName) {
      if (pJson.isJsonPrimitive() && pJson.getAsJsonPrimitive().isNumber()) {
         return pJson.getAsBigDecimal();
      } else {
         throw new JsonSyntaxException("Expected " + pMemberName + " to be a BigDecimal, was " + getType(pJson));
      }
   }

   public static BigDecimal getAsBigDecimal(JsonObject pJson, String pMemberName) {
      if (pJson.has(pMemberName)) {
         return convertToBigDecimal(pJson.get(pMemberName), pMemberName);
      } else {
         throw new JsonSyntaxException("Missing " + pMemberName + ", expected to find a BigDecimal");
      }
   }

   public static BigDecimal getAsBigDecimal(JsonObject pJson, String pMemberName, BigDecimal pFallback) {
      return pJson.has(pMemberName) ? convertToBigDecimal(pJson.get(pMemberName), pMemberName) : pFallback;
   }

   public static BigInteger convertToBigInteger(JsonElement pJson, String pMemberName) {
      if (pJson.isJsonPrimitive() && pJson.getAsJsonPrimitive().isNumber()) {
         return pJson.getAsBigInteger();
      } else {
         throw new JsonSyntaxException("Expected " + pMemberName + " to be a BigInteger, was " + getType(pJson));
      }
   }

   public static BigInteger getAsBigInteger(JsonObject pJson, String pMemberName) {
      if (pJson.has(pMemberName)) {
         return convertToBigInteger(pJson.get(pMemberName), pMemberName);
      } else {
         throw new JsonSyntaxException("Missing " + pMemberName + ", expected to find a BigInteger");
      }
   }

   public static BigInteger getAsBigInteger(JsonObject pJson, String pMemberName, BigInteger pFallback) {
      return pJson.has(pMemberName) ? convertToBigInteger(pJson.get(pMemberName), pMemberName) : pFallback;
   }

   public static short convertToShort(JsonElement pJson, String pMemberName) {
      if (pJson.isJsonPrimitive() && pJson.getAsJsonPrimitive().isNumber()) {
         return pJson.getAsShort();
      } else {
         throw new JsonSyntaxException("Expected " + pMemberName + " to be a Short, was " + getType(pJson));
      }
   }

   public static short getAsShort(JsonObject pJson, String pMemberName) {
      if (pJson.has(pMemberName)) {
         return convertToShort(pJson.get(pMemberName), pMemberName);
      } else {
         throw new JsonSyntaxException("Missing " + pMemberName + ", expected to find a Short");
      }
   }

   public static short getAsShort(JsonObject pJson, String pMemberName, short pFallback) {
      return pJson.has(pMemberName) ? convertToShort(pJson.get(pMemberName), pMemberName) : pFallback;
   }

   public static JsonObject convertToJsonObject(JsonElement pJson, String pMemberName) {
      if (pJson.isJsonObject()) {
         return pJson.getAsJsonObject();
      } else {
         throw new JsonSyntaxException("Expected " + pMemberName + " to be a JsonObject, was " + getType(pJson));
      }
   }

   public static JsonObject getAsJsonObject(JsonObject pJson, String pMemberName) {
      if (pJson.has(pMemberName)) {
         return convertToJsonObject(pJson.get(pMemberName), pMemberName);
      } else {
         throw new JsonSyntaxException("Missing " + pMemberName + ", expected to find a JsonObject");
      }
   }

   @Nullable
   @Contract("_,_,!null->!null;_,_,null->_")
   public static JsonObject getAsJsonObject(JsonObject pJson, String pMemberName, @Nullable JsonObject pFallback) {
      return pJson.has(pMemberName) ? convertToJsonObject(pJson.get(pMemberName), pMemberName) : pFallback;
   }

   public static JsonArray convertToJsonArray(JsonElement pJson, String pMemberName) {
      if (pJson.isJsonArray()) {
         return pJson.getAsJsonArray();
      } else {
         throw new JsonSyntaxException("Expected " + pMemberName + " to be a JsonArray, was " + getType(pJson));
      }
   }

   public static JsonArray getAsJsonArray(JsonObject pJson, String pMemberName) {
      if (pJson.has(pMemberName)) {
         return convertToJsonArray(pJson.get(pMemberName), pMemberName);
      } else {
         throw new JsonSyntaxException("Missing " + pMemberName + ", expected to find a JsonArray");
      }
   }

   @Nullable
   @Contract("_,_,!null->!null;_,_,null->_")
   public static JsonArray getAsJsonArray(JsonObject pJson, String pMemberName, @Nullable JsonArray pFallback) {
      return pJson.has(pMemberName) ? convertToJsonArray(pJson.get(pMemberName), pMemberName) : pFallback;
   }

   public static <T> T convertToObject(@Nullable JsonElement pJson, String pMemberName, JsonDeserializationContext pContext, Class<? extends T> pAdapter) {
      if (pJson != null) {
         return (T)pContext.deserialize(pJson, pAdapter);
      } else {
         throw new JsonSyntaxException("Missing " + pMemberName);
      }
   }

   public static <T> T getAsObject(JsonObject pJson, String pMemberName, JsonDeserializationContext pContext, Class<? extends T> pAdapter) {
      if (pJson.has(pMemberName)) {
         return convertToObject(pJson.get(pMemberName), pMemberName, pContext, pAdapter);
      } else {
         throw new JsonSyntaxException("Missing " + pMemberName);
      }
   }

   @Nullable
   @Contract("_,_,!null,_,_->!null;_,_,null,_,_->_")
   public static <T> T getAsObject(
      JsonObject pJson, String pMemberName, @Nullable T pFallback, JsonDeserializationContext pContext, Class<? extends T> pAdapter
   ) {
      return pJson.has(pMemberName) ? convertToObject(pJson.get(pMemberName), pMemberName, pContext, pAdapter) : pFallback;
   }

   public static String getType(@Nullable JsonElement pJson) {
      String $$1 = StringUtils.abbreviateMiddle(String.valueOf(pJson), "...", 10);
      if (pJson == null) {
         return "null (missing)";
      } else if (pJson.isJsonNull()) {
         return "null (json)";
      } else if (pJson.isJsonArray()) {
         return "an array (" + $$1 + ")";
      } else if (pJson.isJsonObject()) {
         return "an object (" + $$1 + ")";
      } else {
         if (pJson.isJsonPrimitive()) {
            JsonPrimitive $$2 = pJson.getAsJsonPrimitive();
            if ($$2.isNumber()) {
               return "a number (" + $$1 + ")";
            }

            if ($$2.isBoolean()) {
               return "a boolean (" + $$1 + ")";
            }
         }

         return $$1;
      }
   }

   @Nullable
   public static <T> T fromNullableJson(Gson pGson, Reader pReader, Class<T> pAdapter, boolean pLenient) {
      try {
         JsonReader $$4 = new JsonReader(pReader);
         $$4.setLenient(pLenient);
         return (T)pGson.getAdapter(pAdapter).read($$4);
      } catch (IOException var5) {
         throw new JsonParseException(var5);
      }
   }

   public static <T> T fromJson(Gson pGson, Reader pReader, Class<T> pAdapter, boolean pLenient) {
      T $$4 = fromNullableJson(pGson, pReader, pAdapter, pLenient);
      if ($$4 == null) {
         throw new JsonParseException("JSON data was null or empty");
      } else {
         return $$4;
      }
   }

   @Nullable
   public static <T> T fromNullableJson(Gson pGson, Reader pReader, TypeToken<T> pType, boolean pLenient) {
      try {
         JsonReader $$4 = new JsonReader(pReader);
         $$4.setLenient(pLenient);
         return (T)pGson.getAdapter(pType).read($$4);
      } catch (IOException var5) {
         throw new JsonParseException(var5);
      }
   }

   public static <T> T fromJson(Gson pGson, Reader pReader, TypeToken<T> pType, boolean pLenient) {
      T $$4 = fromNullableJson(pGson, pReader, pType, pLenient);
      if ($$4 == null) {
         throw new JsonParseException("JSON data was null or empty");
      } else {
         return $$4;
      }
   }

   @Nullable
   public static <T> T fromNullableJson(Gson pGson, String pJson, TypeToken<T> pType, boolean pLenient) {
      return fromNullableJson(pGson, new StringReader(pJson), pType, pLenient);
   }

   public static <T> T fromJson(Gson pGson, String pJson, Class<T> pAdapter, boolean pLenient) {
      return fromJson(pGson, new StringReader(pJson), pAdapter, pLenient);
   }

   @Nullable
   public static <T> T fromNullableJson(Gson pGson, String pJson, Class<T> pAdapter, boolean pLenient) {
      return fromNullableJson(pGson, new StringReader(pJson), pAdapter, pLenient);
   }

   public static <T> T fromJson(Gson pGson, Reader pReader, TypeToken<T> pType) {
      return fromJson(pGson, pReader, pType, false);
   }

   @Nullable
   public static <T> T fromNullableJson(Gson pGson, String pJson, TypeToken<T> pType) {
      return fromNullableJson(pGson, pJson, pType, false);
   }

   public static <T> T fromJson(Gson pGson, Reader pReader, Class<T> pJsonClass) {
      return fromJson(pGson, pReader, pJsonClass, false);
   }

   public static <T> T fromJson(Gson pGson, String pJson, Class<T> pAdapter) {
      return fromJson(pGson, pJson, pAdapter, false);
   }

   public static JsonObject parse(String pJson, boolean pLenient) {
      return parse(new StringReader(pJson), pLenient);
   }

   public static JsonObject parse(Reader pReader, boolean pLenient) {
      return fromJson(GSON, pReader, JsonObject.class, pLenient);
   }

   public static JsonObject parse(String pJson) {
      return parse(pJson, false);
   }

   public static JsonObject parse(Reader pReader) {
      return parse(pReader, false);
   }

   public static JsonArray parseArray(String pString) {
      return parseArray(new StringReader(pString));
   }

   public static JsonArray parseArray(Reader pReader) {
      return fromJson(GSON, pReader, JsonArray.class, false);
   }

   public static String toStableString(JsonElement pJson) {
      StringWriter $$1 = new StringWriter();
      JsonWriter $$2 = new JsonWriter($$1);

      try {
         writeValue($$2, pJson, Comparator.naturalOrder());
      } catch (IOException var4) {
         throw new AssertionError(var4);
      }

      return $$1.toString();
   }

   public static void writeValue(JsonWriter pWriter, @Nullable JsonElement pJsonElement, @Nullable Comparator<String> pSorter) throws IOException {
      if (pJsonElement == null || pJsonElement.isJsonNull()) {
         pWriter.nullValue();
      } else if (pJsonElement.isJsonPrimitive()) {
         JsonPrimitive $$3 = pJsonElement.getAsJsonPrimitive();
         if ($$3.isNumber()) {
            pWriter.value($$3.getAsNumber());
         } else if ($$3.isBoolean()) {
            pWriter.value($$3.getAsBoolean());
         } else {
            pWriter.value($$3.getAsString());
         }
      } else if (pJsonElement.isJsonArray()) {
         pWriter.beginArray();

         for (JsonElement $$4 : pJsonElement.getAsJsonArray()) {
            writeValue(pWriter, $$4, pSorter);
         }

         pWriter.endArray();
      } else {
         if (!pJsonElement.isJsonObject()) {
            throw new IllegalArgumentException("Couldn't write " + pJsonElement.getClass());
         }

         pWriter.beginObject();

         for (Entry<String, JsonElement> $$5 : sortByKeyIfNeeded(pJsonElement.getAsJsonObject().entrySet(), pSorter)) {
            pWriter.name($$5.getKey());
            writeValue(pWriter, $$5.getValue(), pSorter);
         }

         pWriter.endObject();
      }
   }

   private static Collection<Entry<String, JsonElement>> sortByKeyIfNeeded(
      Collection<Entry<String, JsonElement>> pEntries, @Nullable Comparator<String> pSorter
   ) {
      if (pSorter == null) {
         return pEntries;
      } else {
         List<Entry<String, JsonElement>> $$2 = new ArrayList<>(pEntries);
         $$2.sort(Entry.comparingByKey(pSorter));
         return $$2;
      }
   }
}
