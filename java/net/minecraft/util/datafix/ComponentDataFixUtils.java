package net.minecraft.util.datafix;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.util.GsonHelper;

public class ComponentDataFixUtils {
   private static final String EMPTY_CONTENTS = createTextComponentJson("");

   public static <T> Dynamic<T> createPlainTextComponent(DynamicOps<T> pOps, String pText) {
      String $$2 = createTextComponentJson(pText);
      return new Dynamic(pOps, pOps.createString($$2));
   }

   public static <T> Dynamic<T> createEmptyComponent(DynamicOps<T> pOps) {
      return new Dynamic(pOps, pOps.createString(EMPTY_CONTENTS));
   }

   private static String createTextComponentJson(String pText) {
      JsonObject $$1 = new JsonObject();
      $$1.addProperty("text", pText);
      return GsonHelper.toStableString($$1);
   }

   public static <T> Dynamic<T> createTranslatableComponent(DynamicOps<T> pOps, String pTranslationKey) {
      JsonObject $$2 = new JsonObject();
      $$2.addProperty("translate", pTranslationKey);
      return new Dynamic(pOps, pOps.createString(GsonHelper.toStableString($$2)));
   }

   public static <T> Dynamic<T> wrapLiteralStringAsComponent(Dynamic<T> pDynamic) {
      return (Dynamic<T>)DataFixUtils.orElse(pDynamic.asString().map(p_312090_ -> createPlainTextComponent(pDynamic.getOps(), p_312090_)).result(), pDynamic);
   }

   public static Dynamic<?> rewriteFromLenient(Dynamic<?> pDynamic) {
      Optional<String> $$1 = pDynamic.asString().result();
      if ($$1.isEmpty()) {
         return pDynamic;
      } else {
         String $$2 = $$1.get();
         if (!$$2.isEmpty() && !$$2.equals("null")) {
            char $$3 = $$2.charAt(0);
            char $$4 = $$2.charAt($$2.length() - 1);
            if ($$3 == '"' && $$4 == '"' || $$3 == '{' && $$4 == '}' || $$3 == '[' && $$4 == ']') {
               try {
                  JsonElement $$5 = JsonParser.parseString($$2);
                  if ($$5.isJsonPrimitive()) {
                     return createPlainTextComponent(pDynamic.getOps(), $$5.getAsString());
                  }

                  return pDynamic.createString(GsonHelper.toStableString($$5));
               } catch (JsonParseException var6) {
               }
            }

            return createPlainTextComponent(pDynamic.getOps(), $$2);
         } else {
            return createEmptyComponent(pDynamic.getOps());
         }
      }
   }

   public static Optional<String> extractTranslationString(String pData) {
      try {
         JsonElement $$1 = JsonParser.parseString(pData);
         if ($$1.isJsonObject()) {
            JsonObject $$2 = $$1.getAsJsonObject();
            JsonElement $$3 = $$2.get("translate");
            if ($$3 != null && $$3.isJsonPrimitive()) {
               return Optional.of($$3.getAsString());
            }
         }
      } catch (JsonParseException var4) {
      }

      return Optional.empty();
   }
}
