package net.minecraft.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.StringReader;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.lang.reflect.Field;
import net.minecraft.CharPredicate;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;

public class ParserUtils {
   private static final Field JSON_READER_POS = Util.make(() -> {
      try {
         Field $$0 = JsonReader.class.getDeclaredField("pos");
         $$0.setAccessible(true);
         return $$0;
      } catch (NoSuchFieldException var1) {
         throw new IllegalStateException("Couldn't get field 'pos' for JsonReader", var1);
      }
   });
   private static final Field JSON_READER_LINESTART = Util.make(() -> {
      try {
         Field $$0 = JsonReader.class.getDeclaredField("lineStart");
         $$0.setAccessible(true);
         return $$0;
      } catch (NoSuchFieldException var1) {
         throw new IllegalStateException("Couldn't get field 'lineStart' for JsonReader", var1);
      }
   });

   private static int getPos(JsonReader pReader) {
      try {
         return JSON_READER_POS.getInt(pReader) - JSON_READER_LINESTART.getInt(pReader);
      } catch (IllegalAccessException var2) {
         throw new IllegalStateException("Couldn't read position of JsonReader", var2);
      }
   }

   public static <T> T parseJson(HolderLookup.Provider pRegistries, StringReader pReader, Codec<T> pCodec) {
      JsonReader $$3 = new JsonReader(new java.io.StringReader(pReader.getRemaining()));
      $$3.setLenient(false);

      Object var5;
      try {
         JsonElement $$4 = Streams.parse($$3);
         var5 = pCodec.parse(pRegistries.createSerializationContext(JsonOps.INSTANCE), $$4).getOrThrow(JsonParseException::new);
      } catch (StackOverflowError var9) {
         throw new JsonParseException(var9);
      } finally {
         pReader.setCursor(pReader.getCursor() + getPos($$3));
      }

      return (T)var5;
   }

   public static String readWhile(StringReader pReader, CharPredicate pPredicate) {
      int $$2 = pReader.getCursor();

      while (pReader.canRead() && pPredicate.test(pReader.peek())) {
         pReader.skip();
      }

      return pReader.getString().substring($$2, pReader.getCursor());
   }
}
