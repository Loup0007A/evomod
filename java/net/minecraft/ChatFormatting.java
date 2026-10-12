package net.minecraft;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Contract;

public enum ChatFormatting implements StringRepresentable {
   BLACK("BLACK", '0', 0, 0),
   DARK_BLUE("DARK_BLUE", '1', 1, 170),
   DARK_GREEN("DARK_GREEN", '2', 2, 43520),
   DARK_AQUA("DARK_AQUA", '3', 3, 43690),
   DARK_RED("DARK_RED", '4', 4, 11141120),
   DARK_PURPLE("DARK_PURPLE", '5', 5, 11141290),
   GOLD("GOLD", '6', 6, 16755200),
   GRAY("GRAY", '7', 7, 11184810),
   DARK_GRAY("DARK_GRAY", '8', 8, 5592405),
   BLUE("BLUE", '9', 9, 5592575),
   GREEN("GREEN", 'a', 10, 5635925),
   AQUA("AQUA", 'b', 11, 5636095),
   RED("RED", 'c', 12, 16733525),
   LIGHT_PURPLE("LIGHT_PURPLE", 'd', 13, 16733695),
   YELLOW("YELLOW", 'e', 14, 16777045),
   WHITE("WHITE", 'f', 15, 16777215),
   OBFUSCATED("OBFUSCATED", 'k', true),
   BOLD("BOLD", 'l', true),
   STRIKETHROUGH("STRIKETHROUGH", 'm', true),
   UNDERLINE("UNDERLINE", 'n', true),
   ITALIC("ITALIC", 'o', true),
   RESET("RESET", 'r', -1, null);

   public static final Codec<ChatFormatting> CODEC = StringRepresentable.fromEnum(ChatFormatting::values);
   public static final char PREFIX_CODE = '§';
   private static final Map<String, ChatFormatting> FORMATTING_BY_NAME = Arrays.stream(values())
      .collect(Collectors.toMap(p_126660_ -> cleanName(p_126660_.name), p_126652_ -> (ChatFormatting)p_126652_));
   private static final Pattern STRIP_FORMATTING_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");
   private final String name;
   private final char code;
   private final boolean isFormat;
   private final String toString;
   private final int id;
   @Nullable
   private final Integer color;

   private static String cleanName(String pString) {
      return pString.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
   }

   private ChatFormatting(final String pName, final char pCode, final int pId, @Nullable final Integer pColor) {
      this(pName, pCode, false, pId, pColor);
   }

   private ChatFormatting(final String pName, final char pCode, final boolean pIsFormat) {
      this(pName, pCode, pIsFormat, -1, null);
   }

   private ChatFormatting(final String pName, final char pCode, final boolean pIsFormat, final int pId, @Nullable final Integer pColor) {
      this.name = pName;
      this.code = pCode;
      this.isFormat = pIsFormat;
      this.id = pId;
      this.color = pColor;
      this.toString = "§" + pCode;
   }

   public char getChar() {
      return this.code;
   }

   public int getId() {
      return this.id;
   }

   public boolean isFormat() {
      return this.isFormat;
   }

   public boolean isColor() {
      return !this.isFormat && this != RESET;
   }

   @Nullable
   public Integer getColor() {
      return this.color;
   }

   public String getName() {
      return this.name().toLowerCase(Locale.ROOT);
   }

   @Override
   public String toString() {
      return this.toString;
   }

   @Nullable
   @Contract("!null->!null;_->_")
   public static String stripFormatting(@Nullable String pText) {
      return pText == null ? null : STRIP_FORMATTING_PATTERN.matcher(pText).replaceAll("");
   }

   @Nullable
   public static ChatFormatting getByName(@Nullable String pFriendlyName) {
      return pFriendlyName == null ? null : FORMATTING_BY_NAME.get(cleanName(pFriendlyName));
   }

   @Nullable
   public static ChatFormatting getById(int pIndex) {
      if (pIndex < 0) {
         return RESET;
      } else {
         for (ChatFormatting $$1 : values()) {
            if ($$1.getId() == pIndex) {
               return $$1;
            }
         }

         return null;
      }
   }

   @Nullable
   public static ChatFormatting getByCode(char pFormattingCode) {
      char $$1 = Character.toLowerCase(pFormattingCode);

      for (ChatFormatting $$2 : values()) {
         if ($$2.code == $$1) {
            return $$2;
         }
      }

      return null;
   }

   public static Collection<String> getNames(boolean pGetColor, boolean pGetFancyStyling) {
      List<String> $$2 = Lists.newArrayList();

      for (ChatFormatting $$3 : values()) {
         if ((!$$3.isColor() || pGetColor) && (!$$3.isFormat() || pGetFancyStyling)) {
            $$2.add($$3.getName());
         }
      }

      return $$2;
   }

   @Override
   public String getSerializedName() {
      return this.getName();
   }
}
