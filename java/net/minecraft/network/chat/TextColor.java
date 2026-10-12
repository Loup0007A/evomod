package net.minecraft.network.chat;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;

public final class TextColor {
   private static final String CUSTOM_COLOR_PREFIX = "#";
   public static final Codec<TextColor> CODEC = Codec.STRING.comapFlatMap(TextColor::parseColor, TextColor::serialize);
   private static final Map<ChatFormatting, TextColor> LEGACY_FORMAT_TO_COLOR = Stream.of(ChatFormatting.values())
      .filter(ChatFormatting::isColor)
      .collect(ImmutableMap.toImmutableMap(Function.identity(), p_237301_ -> new TextColor(p_237301_.getColor(), p_237301_.getName())));
   private static final Map<String, TextColor> NAMED_COLORS = LEGACY_FORMAT_TO_COLOR.values()
      .stream()
      .collect(ImmutableMap.toImmutableMap(p_237297_ -> p_237297_.name, Function.identity()));
   private final int value;
   @Nullable
   private final String name;

   private TextColor(int pValue, String pName) {
      this.value = pValue & 16777215;
      this.name = pName;
   }

   private TextColor(int pValue) {
      this.value = pValue & 16777215;
      this.name = null;
   }

   public int getValue() {
      return this.value;
   }

   public String serialize() {
      return this.name != null ? this.name : this.formatValue();
   }

   private String formatValue() {
      return String.format(Locale.ROOT, "#%06X", this.value);
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else if (pOther != null && this.getClass() == pOther.getClass()) {
         TextColor $$1 = (TextColor)pOther;
         return this.value == $$1.value;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.value, this.name);
   }

   @Override
   public String toString() {
      return this.serialize();
   }

   @Nullable
   public static TextColor fromLegacyFormat(ChatFormatting pFormatting) {
      return LEGACY_FORMAT_TO_COLOR.get(pFormatting);
   }

   public static TextColor fromRgb(int pColor) {
      return new TextColor(pColor);
   }

   public static DataResult<TextColor> parseColor(String p_131269_) {
      if (p_131269_.startsWith("#")) {
         try {
            int $$1 = Integer.parseInt(p_131269_.substring(1), 16);
            return $$1 >= 0 && $$1 <= 16777215
               ? DataResult.success(fromRgb($$1), Lifecycle.stable())
               : DataResult.error(() -> "Color value out of range: " + p_131269_);
         } catch (NumberFormatException var2) {
            return DataResult.error(() -> "Invalid color value: " + p_131269_);
         }
      } else {
         TextColor $$3 = NAMED_COLORS.get(p_131269_);
         return $$3 == null ? DataResult.error(() -> "Invalid color name: " + p_131269_) : DataResult.success($$3, Lifecycle.stable());
      }
   }
}
