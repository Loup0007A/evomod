package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;

public record WrappedMinMaxBounds(@Nullable Float min, @Nullable Float max) {
   public static final WrappedMinMaxBounds ANY = new WrappedMinMaxBounds(null, null);
   public static final SimpleCommandExceptionType ERROR_INTS_ONLY = new SimpleCommandExceptionType(Component.translatable("argument.range.ints"));

   public static WrappedMinMaxBounds exactly(float pValue) {
      return new WrappedMinMaxBounds(pValue, pValue);
   }

   public static WrappedMinMaxBounds between(float pMin, float pMax) {
      return new WrappedMinMaxBounds(pMin, pMax);
   }

   public static WrappedMinMaxBounds atLeast(float pMin) {
      return new WrappedMinMaxBounds(pMin, null);
   }

   public static WrappedMinMaxBounds atMost(float pMax) {
      return new WrappedMinMaxBounds(null, pMax);
   }

   public boolean matches(float pValue) {
      if (this.min != null && this.max != null && this.min > this.max && this.min > pValue && this.max < pValue) {
         return false;
      } else {
         return this.min != null && this.min > pValue ? false : this.max == null || !(this.max < pValue);
      }
   }

   public boolean matchesSqr(double pValue) {
      if (this.min != null && this.max != null && this.min > this.max && (double)(this.min * this.min) > pValue && (double)(this.max * this.max) < pValue) {
         return false;
      } else {
         return this.min != null && (double)(this.min * this.min) > pValue ? false : this.max == null || !((double)(this.max * this.max) < pValue);
      }
   }

   public JsonElement serializeToJson() {
      if (this == ANY) {
         return JsonNull.INSTANCE;
      } else if (this.min != null && this.max != null && this.min.equals(this.max)) {
         return new JsonPrimitive(this.min);
      } else {
         JsonObject $$0 = new JsonObject();
         if (this.min != null) {
            $$0.addProperty("min", this.min);
         }

         if (this.max != null) {
            $$0.addProperty("max", this.min);
         }

         return $$0;
      }
   }

   public static WrappedMinMaxBounds fromJson(@Nullable JsonElement pJson) {
      if (pJson == null || pJson.isJsonNull()) {
         return ANY;
      } else if (GsonHelper.isNumberValue(pJson)) {
         float $$1 = GsonHelper.convertToFloat(pJson, "value");
         return new WrappedMinMaxBounds($$1, $$1);
      } else {
         JsonObject $$2 = GsonHelper.convertToJsonObject(pJson, "value");
         Float $$3 = $$2.has("min") ? GsonHelper.getAsFloat($$2, "min") : null;
         Float $$4 = $$2.has("max") ? GsonHelper.getAsFloat($$2, "max") : null;
         return new WrappedMinMaxBounds($$3, $$4);
      }
   }

   public static WrappedMinMaxBounds fromReader(StringReader pReader, boolean pIsFloatingPoint) throws CommandSyntaxException {
      return fromReader(pReader, pIsFloatingPoint, p_164413_ -> p_164413_);
   }

   public static WrappedMinMaxBounds fromReader(StringReader pReader, boolean pIsFloatingPoint, Function<Float, Float> pValueFactory) throws CommandSyntaxException {
      if (!pReader.canRead()) {
         throw MinMaxBounds.ERROR_EMPTY.createWithContext(pReader);
      } else {
         int $$3 = pReader.getCursor();
         Float $$4 = optionallyFormat(readNumber(pReader, pIsFloatingPoint), pValueFactory);
         Float $$5;
         if (pReader.canRead(2) && pReader.peek() == '.' && pReader.peek(1) == '.') {
            pReader.skip();
            pReader.skip();
            $$5 = optionallyFormat(readNumber(pReader, pIsFloatingPoint), pValueFactory);
            if ($$4 == null && $$5 == null) {
               pReader.setCursor($$3);
               throw MinMaxBounds.ERROR_EMPTY.createWithContext(pReader);
            }
         } else {
            if (!pIsFloatingPoint && pReader.canRead() && pReader.peek() == '.') {
               pReader.setCursor($$3);
               throw ERROR_INTS_ONLY.createWithContext(pReader);
            }

            $$5 = $$4;
         }

         if ($$4 == null && $$5 == null) {
            pReader.setCursor($$3);
            throw MinMaxBounds.ERROR_EMPTY.createWithContext(pReader);
         } else {
            return new WrappedMinMaxBounds($$4, $$5);
         }
      }
   }

   @Nullable
   private static Float readNumber(StringReader pReader, boolean pIsFloatingPoint) throws CommandSyntaxException {
      int $$2 = pReader.getCursor();

      while (pReader.canRead() && isAllowedNumber(pReader, pIsFloatingPoint)) {
         pReader.skip();
      }

      String $$3 = pReader.getString().substring($$2, pReader.getCursor());
      if ($$3.isEmpty()) {
         return null;
      } else {
         try {
            return Float.parseFloat($$3);
         } catch (NumberFormatException var5) {
            if (pIsFloatingPoint) {
               throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidDouble().createWithContext(pReader, $$3);
            } else {
               throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().createWithContext(pReader, $$3);
            }
         }
      }
   }

   private static boolean isAllowedNumber(StringReader pReader, boolean pIsFloatingPoint) {
      char $$2 = pReader.peek();
      if (($$2 < '0' || $$2 > '9') && $$2 != '-') {
         return pIsFloatingPoint && $$2 == '.' ? !pReader.canRead(2) || pReader.peek(1) != '.' : false;
      } else {
         return true;
      }
   }

   @Nullable
   private static Float optionallyFormat(@Nullable Float pValue, Function<Float, Float> pValueFactory) {
      return pValue == null ? null : pValueFactory.apply(pValue);
   }
}
