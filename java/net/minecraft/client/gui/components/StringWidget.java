package net.minecraft.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StringWidget extends AbstractStringWidget {
   private float alignX = 0.5F;

   public StringWidget(Component pMessage, Font pFont) {
      this(0, 0, pFont.width(pMessage.getVisualOrderText()), 9, pMessage, pFont);
   }

   public StringWidget(int pWidth, int pHeight, Component pMessage, Font pFont) {
      this(0, 0, pWidth, pHeight, pMessage, pFont);
   }

   public StringWidget(int pX, int pY, int pWidth, int pHeight, Component pMessage, Font pFont) {
      super(pX, pY, pWidth, pHeight, pMessage, pFont);
      this.active = false;
   }

   public StringWidget setColor(int pColor) {
      super.setColor(pColor);
      return this;
   }

   private StringWidget horizontalAlignment(float pHorizontalAlignment) {
      this.alignX = pHorizontalAlignment;
      return this;
   }

   public StringWidget alignLeft() {
      return this.horizontalAlignment(0.0F);
   }

   public StringWidget alignCenter() {
      return this.horizontalAlignment(0.5F);
   }

   public StringWidget alignRight() {
      return this.horizontalAlignment(1.0F);
   }

   @Override
   public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      Component $$4 = this.getMessage();
      Font $$5 = this.getFont();
      int $$6 = this.getWidth();
      int $$7 = $$5.width($$4);
      int $$8 = this.getX() + Math.round(this.alignX * (float)($$6 - $$7));
      int $$9 = this.getY() + (this.getHeight() - 9) / 2;
      FormattedCharSequence $$10 = $$7 > $$6 ? this.clipText($$4, $$6) : $$4.getVisualOrderText();
      pGuiGraphics.drawString($$5, $$10, $$8, $$9, this.getColor());
   }

   private FormattedCharSequence clipText(Component pMessage, int pWidth) {
      Font $$2 = this.getFont();
      FormattedText $$3 = $$2.substrByWidth(pMessage, pWidth - $$2.width(CommonComponents.ELLIPSIS));
      return Language.getInstance().getVisualOrder(FormattedText.composite($$3, CommonComponents.ELLIPSIS));
   }
}
