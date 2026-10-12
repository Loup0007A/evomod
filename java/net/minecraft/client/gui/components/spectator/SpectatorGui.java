package net.minecraft.client.gui.components.spectator;

import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.client.gui.spectator.SpectatorMenuItem;
import net.minecraft.client.gui.spectator.SpectatorMenuListener;
import net.minecraft.client.gui.spectator.categories.SpectatorPage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpectatorGui implements SpectatorMenuListener {
   private static final ResourceLocation HOTBAR_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar");
   private static final ResourceLocation HOTBAR_SELECTION_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar_selection");
   private static final long FADE_OUT_DELAY = 5000L;
   private static final long FADE_OUT_TIME = 2000L;
   private final Minecraft minecraft;
   private long lastSelectionTime;
   @Nullable
   private SpectatorMenu menu;

   public SpectatorGui(Minecraft pMinecraft) {
      this.minecraft = pMinecraft;
   }

   public void onHotbarSelected(int pSlot) {
      this.lastSelectionTime = Util.getMillis();
      if (this.menu != null) {
         this.menu.selectSlot(pSlot);
      } else {
         this.menu = new SpectatorMenu(this);
      }
   }

   private float getHotbarAlpha() {
      long $$0 = this.lastSelectionTime - Util.getMillis() + 5000L;
      return Mth.clamp((float)$$0 / 2000.0F, 0.0F, 1.0F);
   }

   public void renderHotbar(GuiGraphics pGuiGraphics) {
      if (this.menu != null) {
         float $$1 = this.getHotbarAlpha();
         if ($$1 <= 0.0F) {
            this.menu.exit();
         } else {
            int $$2 = pGuiGraphics.guiWidth() / 2;
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().translate(0.0F, 0.0F, -90.0F);
            int $$3 = Mth.floor((float)pGuiGraphics.guiHeight() - 22.0F * $$1);
            SpectatorPage $$4 = this.menu.getCurrentPage();
            this.renderPage(pGuiGraphics, $$1, $$2, $$3, $$4);
            pGuiGraphics.pose().popPose();
         }
      }
   }

   protected void renderPage(GuiGraphics pGuiGraphics, float pAlpha, int pX, int pY, SpectatorPage pSpectatorPage) {
      RenderSystem.enableBlend();
      pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, pAlpha);
      pGuiGraphics.blitSprite(HOTBAR_SPRITE, pX - 91, pY, 182, 22);
      if (pSpectatorPage.getSelectedSlot() >= 0) {
         pGuiGraphics.blitSprite(HOTBAR_SELECTION_SPRITE, pX - 91 - 1 + pSpectatorPage.getSelectedSlot() * 20, pY - 1, 24, 23);
      }

      pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

      for (int $$5 = 0; $$5 < 9; $$5++) {
         this.renderSlot(pGuiGraphics, $$5, pGuiGraphics.guiWidth() / 2 - 90 + $$5 * 20 + 2, (float)(pY + 3), pAlpha, pSpectatorPage.getItem($$5));
      }

      RenderSystem.disableBlend();
   }

   private void renderSlot(GuiGraphics pGuiGraphics, int pSlot, int pX, float pY, float pAlpha, SpectatorMenuItem pSpectatorMenuItem) {
      if (pSpectatorMenuItem != SpectatorMenu.EMPTY_SLOT) {
         int $$6 = (int)(pAlpha * 255.0F);
         pGuiGraphics.pose().pushPose();
         pGuiGraphics.pose().translate((float)pX, pY, 0.0F);
         float $$7 = pSpectatorMenuItem.isEnabled() ? 1.0F : 0.25F;
         pGuiGraphics.setColor($$7, $$7, $$7, pAlpha);
         pSpectatorMenuItem.renderIcon(pGuiGraphics, $$7, $$6);
         pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
         pGuiGraphics.pose().popPose();
         if ($$6 > 3 && pSpectatorMenuItem.isEnabled()) {
            Component $$8 = this.minecraft.options.keyHotbarSlots[pSlot].getTranslatedKeyMessage();
            pGuiGraphics.drawString(this.minecraft.font, $$8, pX + 19 - 2 - this.minecraft.font.width($$8), (int)pY + 6 + 3, 16777215 + ($$6 << 24));
         }
      }
   }

   public void renderTooltip(GuiGraphics pGuiGraphics) {
      int $$1 = (int)(this.getHotbarAlpha() * 255.0F);
      if ($$1 > 3 && this.menu != null) {
         SpectatorMenuItem $$2 = this.menu.getSelectedItem();
         Component $$3 = $$2 == SpectatorMenu.EMPTY_SLOT ? this.menu.getSelectedCategory().getPrompt() : $$2.getName();
         if ($$3 != null) {
            int $$4 = this.minecraft.font.width($$3);
            int $$5 = (pGuiGraphics.guiWidth() - $$4) / 2;
            int $$6 = pGuiGraphics.guiHeight() - 35;
            pGuiGraphics.drawStringWithBackdrop(this.minecraft.font, $$3, $$5, $$6, $$4, FastColor.ARGB32.color($$1, -1));
         }
      }
   }

   @Override
   public void onSpectatorMenuClosed(SpectatorMenu pMenu) {
      this.menu = null;
      this.lastSelectionTime = 0L;
   }

   public boolean isMenuActive() {
      return this.menu != null;
   }

   public void onMouseScrolled(int pAmount) {
      int $$1 = this.menu.getSelectedSlot() + pAmount;

      while ($$1 >= 0 && $$1 <= 8 && (this.menu.getItem($$1) == SpectatorMenu.EMPTY_SLOT || !this.menu.getItem($$1).isEnabled())) {
         $$1 += pAmount;
      }

      if ($$1 >= 0 && $$1 <= 8) {
         this.menu.selectSlot($$1);
         this.lastSelectionTime = Util.getMillis();
      }
   }

   public void onMouseMiddleClick() {
      this.lastSelectionTime = Util.getMillis();
      if (this.isMenuActive()) {
         int $$0 = this.menu.getSelectedSlot();
         if ($$0 != -1) {
            this.menu.selectSlot($$0);
         }
      } else {
         this.menu = new SpectatorMenu(this);
      }
   }
}
