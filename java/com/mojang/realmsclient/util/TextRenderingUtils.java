package com.mojang.realmsclient.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TextRenderingUtils {
   private TextRenderingUtils() {
   }

   @VisibleForTesting
   protected static List<String> lineBreak(String pText) {
      return Arrays.asList(pText.split("\\n"));
   }

   public static List<TextRenderingUtils.Line> decompose(String pText, TextRenderingUtils.LineSegment... pSegments) {
      return decompose(pText, Arrays.asList(pSegments));
   }

   private static List<TextRenderingUtils.Line> decompose(String pText, List<TextRenderingUtils.LineSegment> pSegments) {
      List<String> $$2 = lineBreak(pText);
      return insertLinks($$2, pSegments);
   }

   private static List<TextRenderingUtils.Line> insertLinks(List<String> pLines, List<TextRenderingUtils.LineSegment> pSegments) {
      int $$2 = 0;
      List<TextRenderingUtils.Line> $$3 = Lists.newArrayList();

      for (String $$4 : pLines) {
         List<TextRenderingUtils.LineSegment> $$5 = Lists.newArrayList();

         for (String $$7 : split($$4, "%link")) {
            if ("%link".equals($$7)) {
               $$5.add(pSegments.get($$2++));
            } else {
               $$5.add(TextRenderingUtils.LineSegment.text($$7));
            }
         }

         $$3.add(new TextRenderingUtils.Line($$5));
      }

      return $$3;
   }

   public static List<String> split(String pToSplit, String pDelimiter) {
      if (pDelimiter.isEmpty()) {
         throw new IllegalArgumentException("Delimiter cannot be the empty string");
      } else {
         List<String> $$2 = Lists.newArrayList();
         int $$3 = 0;

         int $$4;
         while (($$4 = pToSplit.indexOf(pDelimiter, $$3)) != -1) {
            if ($$4 > $$3) {
               $$2.add(pToSplit.substring($$3, $$4));
            }

            $$2.add(pDelimiter);
            $$3 = $$4 + pDelimiter.length();
         }

         if ($$3 < pToSplit.length()) {
            $$2.add(pToSplit.substring($$3));
         }

         return $$2;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Line {
      public final List<TextRenderingUtils.LineSegment> segments;

      Line(TextRenderingUtils.LineSegment... pSegments) {
         this(Arrays.asList(pSegments));
      }

      Line(List<TextRenderingUtils.LineSegment> pSegments) {
         this.segments = pSegments;
      }

      @Override
      public String toString() {
         return "Line{segments=" + this.segments + "}";
      }

      @Override
      public boolean equals(Object pOther) {
         if (this == pOther) {
            return true;
         } else if (pOther != null && this.getClass() == pOther.getClass()) {
            TextRenderingUtils.Line $$1 = (TextRenderingUtils.Line)pOther;
            return Objects.equals(this.segments, $$1.segments);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.segments);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class LineSegment {
      private final String fullText;
      @Nullable
      private final String linkTitle;
      @Nullable
      private final String linkUrl;

      private LineSegment(String pFullText) {
         this.fullText = pFullText;
         this.linkTitle = null;
         this.linkUrl = null;
      }

      private LineSegment(String pFullText, @Nullable String pLinkTitle, @Nullable String pLinkUrl) {
         this.fullText = pFullText;
         this.linkTitle = pLinkTitle;
         this.linkUrl = pLinkUrl;
      }

      @Override
      public boolean equals(Object pOther) {
         if (this == pOther) {
            return true;
         } else if (pOther != null && this.getClass() == pOther.getClass()) {
            TextRenderingUtils.LineSegment $$1 = (TextRenderingUtils.LineSegment)pOther;
            return Objects.equals(this.fullText, $$1.fullText) && Objects.equals(this.linkTitle, $$1.linkTitle) && Objects.equals(this.linkUrl, $$1.linkUrl);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         return Objects.hash(this.fullText, this.linkTitle, this.linkUrl);
      }

      @Override
      public String toString() {
         return "Segment{fullText='" + this.fullText + "', linkTitle='" + this.linkTitle + "', linkUrl='" + this.linkUrl + "'}";
      }

      public String renderedText() {
         return this.isLink() ? this.linkTitle : this.fullText;
      }

      public boolean isLink() {
         return this.linkTitle != null;
      }

      public String getLinkUrl() {
         if (!this.isLink()) {
            throw new IllegalStateException("Not a link: " + this);
         } else {
            return this.linkUrl;
         }
      }

      public static TextRenderingUtils.LineSegment link(String pLinkTitle, String pLinkUrl) {
         return new TextRenderingUtils.LineSegment(null, pLinkTitle, pLinkUrl);
      }

      @VisibleForTesting
      protected static TextRenderingUtils.LineSegment text(String pFullText) {
         return new TextRenderingUtils.LineSegment(pFullText);
      }
   }
}
