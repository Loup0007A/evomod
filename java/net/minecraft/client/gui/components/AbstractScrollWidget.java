package net.minecraft.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractScrollWidget extends AbstractWidget implements Renderable, GuiEventListener {
   private static final WidgetSprites BACKGROUND_SPRITES = new WidgetSprites(
      ResourceLocation.withDefaultNamespace("widget/text_field"), ResourceLocation.withDefaultNamespace("widget/text_field_highlighted")
   );
   private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller");
   private static final int INNER_PADDING = 4;
   private static final int SCROLL_BAR_WIDTH = 8;
   private double scrollAmount;
   private boolean scrolling;

   public AbstractScrollWidget(int pX, int pY, int pWidth, int pHeight, Component pMessage) {
      super(pX, pY, pWidth, pHeight, pMessage);
   }

   @Override
   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (!this.visible) {
         return false;
      } else {
         boolean $$3 = this.withinContentAreaPoint(pMouseX, pMouseY);
         boolean $$4 = this.scrollbarVisible()
            && pMouseX >= (double)(this.getX() + this.width)
            && pMouseX <= (double)(this.getX() + this.width + 8)
            && pMouseY >= (double)this.getY()
            && pMouseY < (double)(this.getY() + this.height);
         if ($$4 && pButton == 0) {
            this.scrolling = true;
            return true;
         } else {
            return $$3 || $$4;
         }
      }
   }

   @Override
   public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
      if (pButton == 0) {
         this.scrolling = false;
      }

      return super.mouseReleased(pMouseX, pMouseY, pButton);
   }

   @Override
   public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
      if (this.visible && this.isFocused() && this.scrolling) {
         if (pMouseY < (double)this.getY()) {
            this.setScrollAmount(0.0);
         } else if (pMouseY > (double)(this.getY() + this.height)) {
            this.setScrollAmount((double)this.getMaxScrollAmount());
         } else {
            int $$5 = this.getScrollBarHeight();
            double $$6 = (double)Math.max(1, this.getMaxScrollAmount() / (this.height - $$5));
            this.setScrollAmount(this.scrollAmount + pDragY * $$6);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
      if (!this.visible) {
         return false;
      } else {
         this.setScrollAmount(this.scrollAmount - pScrollY * this.scrollRate());
         return true;
      }
   }

   @Override
   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      boolean $$3 = pKeyCode == 265;
      boolean $$4 = pKeyCode == 264;
      if ($$3 || $$4) {
         double $$5 = this.scrollAmount;
         this.setScrollAmount(this.scrollAmount + (double)($$3 ? -1 : 1) * this.scrollRate());
         if ($$5 != this.scrollAmount) {
            return true;
         }
      }

      return super.keyPressed(pKeyCode, pScanCode, pModifiers);
   }

   @Override
   public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      if (this.visible) {
         this.renderBackground(pGuiGraphics);
         pGuiGraphics.enableScissor(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1);
         pGuiGraphics.pose().pushPose();
         pGuiGraphics.pose().translate(0.0, -this.scrollAmount, 0.0);
         this.renderContents(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         pGuiGraphics.pose().popPose();
         pGuiGraphics.disableScissor();
         this.renderDecorations(pGuiGraphics);
      }
   }

   private int getScrollBarHeight() {
      return Mth.clamp((int)((float)(this.height * this.height) / (float)this.getContentHeight()), 32, this.height);
   }

   protected void renderDecorations(GuiGraphics pGuiGraphics) {
      if (this.scrollbarVisible()) {
         this.renderScrollBar(pGuiGraphics);
      }
   }

   protected int innerPadding() {
      return 4;
   }

   protected int totalInnerPadding() {
      return this.innerPadding() * 2;
   }

   protected double scrollAmount() {
      return this.scrollAmount;
   }

   protected void setScrollAmount(double pScrollAmount) {
      this.scrollAmount = Mth.clamp(pScrollAmount, 0.0, (double)this.getMaxScrollAmount());
   }

   protected int getMaxScrollAmount() {
      return Math.max(0, this.getContentHeight() - (this.height - 4));
   }

   private int getContentHeight() {
      return this.getInnerHeight() + 4;
   }

   protected void renderBackground(GuiGraphics pGuiGraphics) {
      this.renderBorder(pGuiGraphics, this.getX(), this.getY(), this.getWidth(), this.getHeight());
   }

   protected void renderBorder(GuiGraphics pGuiGraphics, int pX, int pY, int pWidth, int pHeight) {
      ResourceLocation $$5 = BACKGROUND_SPRITES.get(this.isActive(), this.isFocused());
      pGuiGraphics.blitSprite($$5, pX, pY, pWidth, pHeight);
   }

   private void renderScrollBar(GuiGraphics pGuiGraphics) {
      int $$1 = this.getScrollBarHeight();
      int $$2 = this.getX() + this.width;
      int $$3 = Math.max(this.getY(), (int)this.scrollAmount * (this.height - $$1) / this.getMaxScrollAmount() + this.getY());
      RenderSystem.enableBlend();
      pGuiGraphics.blitSprite(SCROLLER_SPRITE, $$2, $$3, 8, $$1);
      RenderSystem.disableBlend();
   }

   protected boolean withinContentAreaTopBottom(int pTop, int pBottom) {
      return (double)pBottom - this.scrollAmount >= (double)this.getY() && (double)pTop - this.scrollAmount <= (double)(this.getY() + this.height);
   }

   protected boolean withinContentAreaPoint(double pX, double pY) {
      return pX >= (double)this.getX() && pX < (double)(this.getX() + this.width) && pY >= (double)this.getY() && pY < (double)(this.getY() + this.height);
   }

   protected boolean scrollbarVisible() {
      return this.getInnerHeight() > this.getHeight();
   }

   public int scrollbarWidth() {
      return 8;
   }

   protected abstract int getInnerHeight();

   protected abstract double scrollRate();

   protected abstract void renderContents(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick);
}
