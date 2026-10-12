package net.minecraft.util;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

public class StringDecomposer {
   private static final char REPLACEMENT_CHAR = '�';
   private static final Optional<Object> STOP_ITERATION = Optional.of(Unit.INSTANCE);

   private static boolean feedChar(Style pStyle, FormattedCharSink pSink, int pPosition, char pCharacter) {
      return Character.isSurrogate(pCharacter) ? pSink.accept(pPosition, pStyle, 65533) : pSink.accept(pPosition, pStyle, pCharacter);
   }

   public static boolean iterate(String pText, Style pStyle, FormattedCharSink pSink) {
      int $$3 = pText.length();

      for (int $$4 = 0; $$4 < $$3; $$4++) {
         char $$5 = pText.charAt($$4);
         if (Character.isHighSurrogate($$5)) {
            if ($$4 + 1 >= $$3) {
               if (!pSink.accept($$4, pStyle, 65533)) {
                  return false;
               }
               break;
            }

            char $$6 = pText.charAt($$4 + 1);
            if (Character.isLowSurrogate($$6)) {
               if (!pSink.accept($$4, pStyle, Character.toCodePoint($$5, $$6))) {
                  return false;
               }

               $$4++;
            } else if (!pSink.accept($$4, pStyle, 65533)) {
               return false;
            }
         } else if (!feedChar(pStyle, pSink, $$4, $$5)) {
            return false;
         }
      }

      return true;
   }

   public static boolean iterateBackwards(String pText, Style pStyle, FormattedCharSink pSink) {
      int $$3 = pText.length();

      for (int $$4 = $$3 - 1; $$4 >= 0; $$4--) {
         char $$5 = pText.charAt($$4);
         if (Character.isLowSurrogate($$5)) {
            if ($$4 - 1 < 0) {
               if (!pSink.accept(0, pStyle, 65533)) {
                  return false;
               }
               break;
            }

            char $$6 = pText.charAt($$4 - 1);
            if (Character.isHighSurrogate($$6)) {
               if (!pSink.accept(--$$4, pStyle, Character.toCodePoint($$6, $$5))) {
                  return false;
               }
            } else if (!pSink.accept($$4, pStyle, 65533)) {
               return false;
            }
         } else if (!feedChar(pStyle, pSink, $$4, $$5)) {
            return false;
         }
      }

      return true;
   }

   public static boolean iterateFormatted(String pText, Style pStyle, FormattedCharSink pSink) {
      return iterateFormatted(pText, 0, pStyle, pSink);
   }

   public static boolean iterateFormatted(String pText, int pSkip, Style pStyle, FormattedCharSink pSink) {
      return iterateFormatted(pText, pSkip, pStyle, pStyle, pSink);
   }

   public static boolean iterateFormatted(String pText, int pSkip, Style pCurrentStyle, Style pDefaultStyle, FormattedCharSink pSink) {
      int $$5 = pText.length();
      Style $$6 = pCurrentStyle;

      for (int $$7 = pSkip; $$7 < $$5; $$7++) {
         char $$8 = pText.charAt($$7);
         if ($$8 == 167) {
            if ($$7 + 1 >= $$5) {
               break;
            }

            char $$9 = pText.charAt($$7 + 1);
            ChatFormatting $$10 = ChatFormatting.getByCode($$9);
            if ($$10 != null) {
               $$6 = $$10 == ChatFormatting.RESET ? pDefaultStyle : $$6.applyLegacyFormat($$10);
            }

            $$7++;
         } else if (Character.isHighSurrogate($$8)) {
            if ($$7 + 1 >= $$5) {
               if (!pSink.accept($$7, $$6, 65533)) {
                  return false;
               }
               break;
            }

            char $$11 = pText.charAt($$7 + 1);
            if (Character.isLowSurrogate($$11)) {
               if (!pSink.accept($$7, $$6, Character.toCodePoint($$8, $$11))) {
                  return false;
               }

               $$7++;
            } else if (!pSink.accept($$7, $$6, 65533)) {
               return false;
            }
         } else if (!feedChar($$6, pSink, $$7, $$8)) {
            return false;
         }
      }

      return true;
   }

   public static boolean iterateFormatted(FormattedText pText, Style pStyle, FormattedCharSink pSink) {
      return pText.visit((p_14302_, p_14303_) -> iterateFormatted(p_14303_, 0, p_14302_, pSink) ? Optional.empty() : STOP_ITERATION, pStyle).isEmpty();
   }

   public static String filterBrokenSurrogates(String pText) {
      StringBuilder $$1 = new StringBuilder();
      iterate(pText, Style.EMPTY, (p_14343_, p_14344_, p_14345_) -> {
         $$1.appendCodePoint(p_14345_);
         return true;
      });
      return $$1.toString();
   }

   public static String getPlainText(FormattedText pText) {
      StringBuilder $$1 = new StringBuilder();
      iterateFormatted(pText, Style.EMPTY, (p_14323_, p_14324_, p_14325_) -> {
         $$1.appendCodePoint(p_14325_);
         return true;
      });
      return $$1.toString();
   }
}
