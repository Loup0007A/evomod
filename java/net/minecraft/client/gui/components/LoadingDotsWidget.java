package net.minecraft.client.gui.components;

import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LoadingDotsWidget extends AbstractWidget {
   private final Font font;

   public LoadingDotsWidget(Font pFont, Component pMessage) {
      super(0, 0, pFont.width(pMessage), 9 * 3, pMessage);
      this.font = pFont;
   }

   @Override
   protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      int $$4 = this.getX() + this.getWidth() / 2;
      int $$5 = this.getY() + this.getHeight() / 2;
      Component $$6 = this.getMessage();
      pGuiGraphics.drawString(this.font, $$6, $$4 - this.font.width($$6) / 2, $$5 - 9, -1, false);
      String $$7 = LoadingDotsText.get(Util.getMillis());
      pGuiGraphics.drawString(this.font, $$7, $$4 - this.font.width($$7) / 2, $$5 + 9, -8355712, false);
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
   }

   @Override
   public void playDownSound(SoundManager pHandler) {
   }

   @Override
   public boolean isActive() {
      return false;
   }

   @Nullable
   @Override
   public ComponentPath nextFocusPath(FocusNavigationEvent pEvent) {
      return null;
   }
}
