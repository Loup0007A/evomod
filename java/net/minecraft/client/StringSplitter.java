package net.minecraft.client;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;

@OnlyIn(Dist.CLIENT)
public class StringSplitter {
   final StringSplitter.WidthProvider widthProvider;

   public StringSplitter(StringSplitter.WidthProvider pWidthProvider) {
      this.widthProvider = pWidthProvider;
   }

   public float stringWidth(@Nullable String pContent) {
      if (pContent == null) {
         return 0.0F;
      } else {
         MutableFloat $$1 = new MutableFloat();
         StringDecomposer.iterateFormatted(pContent, Style.EMPTY, (p_92429_, p_92430_, p_92431_) -> {
            $$1.add(this.widthProvider.getWidth(p_92431_, p_92430_));
            return true;
         });
         return $$1.floatValue();
      }
   }

   public float stringWidth(FormattedText pContent) {
      MutableFloat $$1 = new MutableFloat();
      StringDecomposer.iterateFormatted(pContent, Style.EMPTY, (p_92420_, p_92421_, p_92422_) -> {
         $$1.add(this.widthProvider.getWidth(p_92422_, p_92421_));
         return true;
      });
      return $$1.floatValue();
   }

   public float stringWidth(FormattedCharSequence pContent) {
      MutableFloat $$1 = new MutableFloat();
      pContent.accept((p_92400_, p_92401_, p_92402_) -> {
         $$1.add(this.widthProvider.getWidth(p_92402_, p_92401_));
         return true;
      });
      return $$1.floatValue();
   }

   public int plainIndexAtWidth(String pContent, int pMaxWidth, Style pStyle) {
      StringSplitter.WidthLimitedCharSink $$3 = new StringSplitter.WidthLimitedCharSink((float)pMaxWidth);
      StringDecomposer.iterate(pContent, pStyle, $$3);
      return $$3.getPosition();
   }

   public String plainHeadByWidth(String pContent, int pMaxWidth, Style pStyle) {
      return pContent.substring(0, this.plainIndexAtWidth(pContent, pMaxWidth, pStyle));
   }

   public String plainTailByWidth(String pContent, int pMaxWidth, Style pStyle) {
      MutableFloat $$3 = new MutableFloat();
      MutableInt $$4 = new MutableInt(pContent.length());
      StringDecomposer.iterateBackwards(pContent, pStyle, (p_92407_, p_92408_, p_92409_) -> {
         float $$6 = $$3.addAndGet(this.widthProvider.getWidth(p_92409_, p_92408_));
         if ($$6 > (float)pMaxWidth) {
            return false;
         } else {
            $$4.setValue(p_92407_);
            return true;
         }
      });
      return pContent.substring($$4.intValue());
   }

   public int formattedIndexByWidth(String pContent, int pMaxWidth, Style pStyle) {
      StringSplitter.WidthLimitedCharSink $$3 = new StringSplitter.WidthLimitedCharSink((float)pMaxWidth);
      StringDecomposer.iterateFormatted(pContent, pStyle, $$3);
      return $$3.getPosition();
   }

   @Nullable
   public Style componentStyleAtWidth(FormattedText pContent, int pMaxWidth) {
      StringSplitter.WidthLimitedCharSink $$2 = new StringSplitter.WidthLimitedCharSink((float)pMaxWidth);
      return pContent.<Style>visit(
            (p_92343_, p_92344_) -> StringDecomposer.iterateFormatted(p_92344_, p_92343_, $$2) ? Optional.empty() : Optional.of(p_92343_), Style.EMPTY
         )
         .orElse(null);
   }

   @Nullable
   public Style componentStyleAtWidth(FormattedCharSequence pContent, int pMaxWidth) {
      StringSplitter.WidthLimitedCharSink $$2 = new StringSplitter.WidthLimitedCharSink((float)pMaxWidth);
      MutableObject<Style> $$3 = new MutableObject();
      pContent.accept((p_92348_, p_92349_, p_92350_) -> {
         if (!$$2.accept(p_92348_, p_92349_, p_92350_)) {
            $$3.setValue(p_92349_);
            return false;
         } else {
            return true;
         }
      });
      return (Style)$$3.getValue();
   }

   public String formattedHeadByWidth(String pContent, int pMaxWidth, Style pStyle) {
      return pContent.substring(0, this.formattedIndexByWidth(pContent, pMaxWidth, pStyle));
   }

   public FormattedText headByWidth(FormattedText pContent, int pMaxWidth, Style pStyle) {
      final StringSplitter.WidthLimitedCharSink $$3 = new StringSplitter.WidthLimitedCharSink((float)pMaxWidth);
      return pContent.visit(new FormattedText.StyledContentConsumer<FormattedText>() {
         private final ComponentCollector collector = new ComponentCollector();

         @Override
         public Optional<FormattedText> accept(Style p_92443_, String p_92444_) {
            $$3.resetPosition();
            if (!StringDecomposer.iterateFormatted(p_92444_, p_92443_, $$3)) {
               String $$2 = p_92444_.substring(0, $$3.getPosition());
               if (!$$2.isEmpty()) {
                  this.collector.append(FormattedText.of($$2, p_92443_));
               }

               return Optional.of(this.collector.getResultOrEmpty());
            } else {
               if (!p_92444_.isEmpty()) {
                  this.collector.append(FormattedText.of(p_92444_, p_92443_));
               }

               return Optional.empty();
            }
         }
      }, pStyle).orElse(pContent);
   }

   public int findLineBreak(String pContent, int pMaxWidth, Style pStyle) {
      StringSplitter.LineBreakFinder $$3 = new StringSplitter.LineBreakFinder((float)pMaxWidth);
      StringDecomposer.iterateFormatted(pContent, pStyle, $$3);
      return $$3.getSplitPosition();
   }

   public static int getWordPosition(String pContent, int pSkipCount, int pCursorPoint, boolean pIncludeWhitespace) {
      int $$4 = pCursorPoint;
      boolean $$5 = pSkipCount < 0;
      int $$6 = Math.abs(pSkipCount);

      for (int $$7 = 0; $$7 < $$6; $$7++) {
         if ($$5) {
            while (pIncludeWhitespace && $$4 > 0 && (pContent.charAt($$4 - 1) == ' ' || pContent.charAt($$4 - 1) == '\n')) {
               $$4--;
            }

            while ($$4 > 0 && pContent.charAt($$4 - 1) != ' ' && pContent.charAt($$4 - 1) != '\n') {
               $$4--;
            }
         } else {
            int $$8 = pContent.length();
            int $$9 = pContent.indexOf(32, $$4);
            int $$10 = pContent.indexOf(10, $$4);
            if ($$9 == -1 && $$10 == -1) {
               $$4 = -1;
            } else if ($$9 != -1 && $$10 != -1) {
               $$4 = Math.min($$9, $$10);
            } else if ($$9 != -1) {
               $$4 = $$9;
            } else {
               $$4 = $$10;
            }

            if ($$4 == -1) {
               $$4 = $$8;
            } else {
               while (pIncludeWhitespace && $$4 < $$8 && (pContent.charAt($$4) == ' ' || pContent.charAt($$4) == '\n')) {
                  $$4++;
               }
            }
         }
      }

      return $$4;
   }

   public void splitLines(String pContent, int pMaxWidth, Style pStyle, boolean pWithNewLines, StringSplitter.LinePosConsumer pLinePos) {
      int $$5 = 0;
      int $$6 = pContent.length();
      Style $$7 = pStyle;

      while ($$5 < $$6) {
         StringSplitter.LineBreakFinder $$8 = new StringSplitter.LineBreakFinder((float)pMaxWidth);
         boolean $$9 = StringDecomposer.iterateFormatted(pContent, $$5, $$7, pStyle, $$8);
         if ($$9) {
            pLinePos.accept($$7, $$5, $$6);
            break;
         }

         int $$10 = $$8.getSplitPosition();
         char $$11 = pContent.charAt($$10);
         int $$12 = $$11 != '\n' && $$11 != ' ' ? $$10 : $$10 + 1;
         pLinePos.accept($$7, $$5, pWithNewLines ? $$12 : $$10);
         $$5 = $$12;
         $$7 = $$8.getSplitStyle();
      }
   }

   public List<FormattedText> splitLines(String pContent, int pMaxWidth, Style pStyle) {
      List<FormattedText> $$3 = Lists.newArrayList();
      this.splitLines(
         pContent, pMaxWidth, pStyle, false, (p_92373_, p_92374_, p_92375_) -> $$3.add(FormattedText.of(pContent.substring(p_92374_, p_92375_), p_92373_))
      );
      return $$3;
   }

   public List<FormattedText> splitLines(FormattedText pContent, int pMaxWidth, Style pStyle) {
      List<FormattedText> $$3 = Lists.newArrayList();
      this.splitLines(pContent, pMaxWidth, pStyle, (p_92378_, p_92379_) -> $$3.add(p_92378_));
      return $$3;
   }

   public List<FormattedText> splitLines(FormattedText pContent, int pMaxWidth, Style pStyle, FormattedText pPrefix) {
      List<FormattedText> $$4 = Lists.newArrayList();
      this.splitLines(pContent, pMaxWidth, pStyle, (p_168619_, p_168620_) -> $$4.add(p_168620_ ? FormattedText.composite(pPrefix, p_168619_) : p_168619_));
      return $$4;
   }

   public void splitLines(FormattedText pContent, int pMaxWidth, Style pStyle, BiConsumer<FormattedText, Boolean> pSplitifier) {
      List<StringSplitter.LineComponent> $$4 = Lists.newArrayList();
      pContent.visit((p_92382_, p_92383_) -> {
         if (!p_92383_.isEmpty()) {
            $$4.add(new StringSplitter.LineComponent(p_92383_, p_92382_));
         }

         return Optional.empty();
      }, pStyle);
      StringSplitter.FlatComponents $$5 = new StringSplitter.FlatComponents($$4);
      boolean $$6 = true;
      boolean $$7 = false;
      boolean $$8 = false;

      while ($$6) {
         $$6 = false;
         StringSplitter.LineBreakFinder $$9 = new StringSplitter.LineBreakFinder((float)pMaxWidth);

         for (StringSplitter.LineComponent $$10 : $$5.parts) {
            boolean $$11 = StringDecomposer.iterateFormatted($$10.contents, 0, $$10.style, pStyle, $$9);
            if (!$$11) {
               int $$12 = $$9.getSplitPosition();
               Style $$13 = $$9.getSplitStyle();
               char $$14 = $$5.charAt($$12);
               boolean $$15 = $$14 == '\n';
               boolean $$16 = $$15 || $$14 == ' ';
               $$7 = $$15;
               FormattedText $$17 = $$5.splitAt($$12, $$16 ? 1 : 0, $$13);
               pSplitifier.accept($$17, $$8);
               $$8 = !$$15;
               $$6 = true;
               break;
            }

            $$9.addToOffset($$10.contents.length());
         }
      }

      FormattedText $$18 = $$5.getRemainder();
      if ($$18 != null) {
         pSplitifier.accept($$18, $$8);
      } else if ($$7) {
         pSplitifier.accept(FormattedText.EMPTY, false);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class FlatComponents {
      final List<StringSplitter.LineComponent> parts;
      private String flatParts;

      public FlatComponents(List<StringSplitter.LineComponent> pParts) {
         this.parts = pParts;
         this.flatParts = pParts.stream().map(p_92459_ -> p_92459_.contents).collect(Collectors.joining());
      }

      public char charAt(int pCodePoint) {
         return this.flatParts.charAt(pCodePoint);
      }

      public FormattedText splitAt(int pBegin, int pEnd, Style pStyle) {
         ComponentCollector $$3 = new ComponentCollector();
         ListIterator<StringSplitter.LineComponent> $$4 = this.parts.listIterator();
         int $$5 = pBegin;
         boolean $$6 = false;

         while ($$4.hasNext()) {
            StringSplitter.LineComponent $$7 = $$4.next();
            String $$8 = $$7.contents;
            int $$9 = $$8.length();
            if (!$$6) {
               if ($$5 > $$9) {
                  $$3.append($$7);
                  $$4.remove();
                  $$5 -= $$9;
               } else {
                  String $$10 = $$8.substring(0, $$5);
                  if (!$$10.isEmpty()) {
                     $$3.append(FormattedText.of($$10, $$7.style));
                  }

                  $$5 += pEnd;
                  $$6 = true;
               }
            }

            if ($$6) {
               if ($$5 <= $$9) {
                  String $$11 = $$8.substring($$5);
                  if ($$11.isEmpty()) {
                     $$4.remove();
                  } else {
                     $$4.set(new StringSplitter.LineComponent($$11, pStyle));
                  }
                  break;
               }

               $$4.remove();
               $$5 -= $$9;
            }
         }

         this.flatParts = this.flatParts.substring(pBegin + pEnd);
         return $$3.getResultOrEmpty();
      }

      @Nullable
      public FormattedText getRemainder() {
         ComponentCollector $$0 = new ComponentCollector();
         this.parts.forEach($$0::append);
         this.parts.clear();
         return $$0.getResult();
      }
   }

   @OnlyIn(Dist.CLIENT)
   class LineBreakFinder implements FormattedCharSink {
      private final float maxWidth;
      private int lineBreak = -1;
      private Style lineBreakStyle = Style.EMPTY;
      private boolean hadNonZeroWidthChar;
      private float width;
      private int lastSpace = -1;
      private Style lastSpaceStyle = Style.EMPTY;
      private int nextChar;
      private int offset;

      public LineBreakFinder(final float pMaxWidth) {
         this.maxWidth = Math.max(pMaxWidth, 1.0F);
      }

      @Override
      public boolean accept(int pPositionInCurrentSequence, Style pStyle, int pCodePoint) {
         int $$3 = pPositionInCurrentSequence + this.offset;
         switch (pCodePoint) {
            case 10:
               return this.finishIteration($$3, pStyle);
            case 32:
               this.lastSpace = $$3;
               this.lastSpaceStyle = pStyle;
            default:
               float $$4 = StringSplitter.this.widthProvider.getWidth(pCodePoint, pStyle);
               this.width += $$4;
               if (!this.hadNonZeroWidthChar || !(this.width > this.maxWidth)) {
                  this.hadNonZeroWidthChar |= $$4 != 0.0F;
                  this.nextChar = $$3 + Character.charCount(pCodePoint);
                  return true;
               } else {
                  return this.lastSpace != -1 ? this.finishIteration(this.lastSpace, this.lastSpaceStyle) : this.finishIteration($$3, pStyle);
               }
         }
      }

      private boolean finishIteration(int pLineBreak, Style pLineBreakStyle) {
         this.lineBreak = pLineBreak;
         this.lineBreakStyle = pLineBreakStyle;
         return false;
      }

      private boolean lineBreakFound() {
         return this.lineBreak != -1;
      }

      public int getSplitPosition() {
         return this.lineBreakFound() ? this.lineBreak : this.nextChar;
      }

      public Style getSplitStyle() {
         return this.lineBreakStyle;
      }

      public void addToOffset(int pOffset) {
         this.offset += pOffset;
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class LineComponent implements FormattedText {
      final String contents;
      final Style style;

      public LineComponent(String pContents, Style pStyle) {
         this.contents = pContents;
         this.style = pStyle;
      }

      @Override
      public <T> Optional<T> visit(FormattedText.ContentConsumer<T> pAcceptor) {
         return pAcceptor.accept(this.contents);
      }

      @Override
      public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> pAcceptor, Style pStyle) {
         return pAcceptor.accept(this.style.applyTo(pStyle), this.contents);
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface LinePosConsumer {
      void accept(Style pStyle, int pCurrentPos, int pContentWidth);
   }

   @OnlyIn(Dist.CLIENT)
   class WidthLimitedCharSink implements FormattedCharSink {
      private float maxWidth;
      private int position;

      public WidthLimitedCharSink(final float pMaxWidth) {
         this.maxWidth = pMaxWidth;
      }

      @Override
      public boolean accept(int pPositionInCurrentSequence, Style pStyle, int pCodePoint) {
         this.maxWidth = this.maxWidth - StringSplitter.this.widthProvider.getWidth(pCodePoint, pStyle);
         if (this.maxWidth >= 0.0F) {
            this.position = pPositionInCurrentSequence + Character.charCount(pCodePoint);
            return true;
         } else {
            return false;
         }
      }

      public int getPosition() {
         return this.position;
      }

      public void resetPosition() {
         this.position = 0;
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface WidthProvider {
      float getWidth(int pCodePoint, Style pStyle);
   }
}
