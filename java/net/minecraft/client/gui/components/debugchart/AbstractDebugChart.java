package net.minecraft.client.gui.components.debugchart;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.debugchart.SampleStorage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractDebugChart {
   protected static final int COLOR_GREY = 14737632;
   protected static final int CHART_HEIGHT = 60;
   protected static final int LINE_WIDTH = 1;
   protected final Font font;
   protected final SampleStorage sampleStorage;

   protected AbstractDebugChart(Font pFont, SampleStorage pSampleStorage) {
      this.font = pFont;
      this.sampleStorage = pSampleStorage;
   }

   public int getWidth(int pMaxWidth) {
      return Math.min(this.sampleStorage.capacity() + 2, pMaxWidth);
   }

   public void drawChart(GuiGraphics pGuiGraphics, int pX, int pWidth) {
      int $$3 = pGuiGraphics.guiHeight();
      pGuiGraphics.fill(RenderType.guiOverlay(), pX, $$3 - 60, pX + pWidth, $$3, -1873784752);
      long $$4 = 0L;
      long $$5 = 2147483647L;
      long $$6 = -2147483648L;
      int $$7 = Math.max(0, this.sampleStorage.capacity() - (pWidth - 2));
      int $$8 = this.sampleStorage.size() - $$7;

      for (int $$9 = 0; $$9 < $$8; $$9++) {
         int $$10 = pX + $$9 + 1;
         int $$11 = $$7 + $$9;
         long $$12 = this.getValueForAggregation($$11);
         $$5 = Math.min($$5, $$12);
         $$6 = Math.max($$6, $$12);
         $$4 += $$12;
         this.drawDimensions(pGuiGraphics, $$3, $$10, $$11);
      }

      pGuiGraphics.hLine(RenderType.guiOverlay(), pX, pX + pWidth - 1, $$3 - 60, -1);
      pGuiGraphics.hLine(RenderType.guiOverlay(), pX, pX + pWidth - 1, $$3 - 1, -1);
      pGuiGraphics.vLine(RenderType.guiOverlay(), pX, $$3 - 60, $$3, -1);
      pGuiGraphics.vLine(RenderType.guiOverlay(), pX + pWidth - 1, $$3 - 60, $$3, -1);
      if ($$8 > 0) {
         String $$13 = this.toDisplayString((double)$$5) + " min";
         String $$14 = this.toDisplayString((double)$$4 / (double)$$8) + " avg";
         String $$15 = this.toDisplayString((double)$$6) + " max";
         pGuiGraphics.drawString(this.font, $$13, pX + 2, $$3 - 60 - 9, 14737632);
         pGuiGraphics.drawCenteredString(this.font, $$14, pX + pWidth / 2, $$3 - 60 - 9, 14737632);
         pGuiGraphics.drawString(this.font, $$15, pX + pWidth - this.font.width($$15) - 2, $$3 - 60 - 9, 14737632);
      }

      this.renderAdditionalLinesAndLabels(pGuiGraphics, pX, pWidth, $$3);
   }

   protected void drawDimensions(GuiGraphics pGuiGraphics, int pHeight, int pX, int pIndex) {
      this.drawMainDimension(pGuiGraphics, pHeight, pX, pIndex);
      this.drawAdditionalDimensions(pGuiGraphics, pHeight, pX, pIndex);
   }

   protected void drawMainDimension(GuiGraphics pGuiGraphics, int pHeight, int pX, int pIndex) {
      long $$4 = this.sampleStorage.get(pIndex);
      int $$5 = this.getSampleHeight((double)$$4);
      int $$6 = this.getSampleColor($$4);
      pGuiGraphics.fill(RenderType.guiOverlay(), pX, pHeight - $$5, pX + 1, pHeight, $$6);
   }

   protected void drawAdditionalDimensions(GuiGraphics pGuiGraphics, int pHeight, int pX, int pIndex) {
   }

   protected long getValueForAggregation(int pIndex) {
      return this.sampleStorage.get(pIndex);
   }

   protected void renderAdditionalLinesAndLabels(GuiGraphics pGuiGraphics, int pX, int pWidth, int pHeight) {
   }

   protected void drawStringWithShade(GuiGraphics pGuiGraphics, String pText, int pX, int pY) {
      pGuiGraphics.fill(RenderType.guiOverlay(), pX, pY, pX + this.font.width(pText) + 1, pY + 9, -1873784752);
      pGuiGraphics.drawString(this.font, pText, pX + 1, pY + 1, 14737632, false);
   }

   protected abstract String toDisplayString(double pValue);

   protected abstract int getSampleHeight(double pValue);

   protected abstract int getSampleColor(long pValue);

   protected int getSampleColor(double pValue, double pMinPosition, int pMinColor, double pMidPosition, int pMidColor, double pMaxPosition, int pGuiGraphics) {
      pValue = Mth.clamp(pValue, pMinPosition, pMaxPosition);
      return pValue < pMidPosition
         ? FastColor.ARGB32.lerp((float)((pValue - pMinPosition) / (pMidPosition - pMinPosition)), pMinColor, pMidColor)
         : FastColor.ARGB32.lerp((float)((pValue - pMidPosition) / (pMaxPosition - pMidPosition)), pMidColor, pGuiGraphics);
   }
}
