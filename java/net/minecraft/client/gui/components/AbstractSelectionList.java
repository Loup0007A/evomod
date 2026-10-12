package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSelectionList<E extends AbstractSelectionList.Entry<E>> extends AbstractContainerWidget {
   protected static final int SCROLLBAR_WIDTH = 6;
   private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller");
   private static final ResourceLocation SCROLLER_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("widget/scroller_background");
   private static final ResourceLocation MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/menu_list_background.png");
   private static final ResourceLocation INWORLD_MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");
   protected final Minecraft minecraft;
   protected final int itemHeight;
   private final List<E> children = new AbstractSelectionList.TrackedList();
   protected boolean centerListVertically = true;
   private double scrollAmount;
   private boolean renderHeader;
   protected int headerHeight;
   private boolean scrolling;
   @Nullable
   private E selected;
   @Nullable
   private E hovered;

   public AbstractSelectionList(Minecraft pMinecraft, int pWidth, int pHeight, int pY, int pItemHeight) {
      super(0, pY, pWidth, pHeight, CommonComponents.EMPTY);
      this.minecraft = pMinecraft;
      this.itemHeight = pItemHeight;
   }

   protected void setRenderHeader(boolean pRenderHeader, int pHeaderHeight) {
      this.renderHeader = pRenderHeader;
      this.headerHeight = pHeaderHeight;
      if (!pRenderHeader) {
         this.headerHeight = 0;
      }
   }

   public int getRowWidth() {
      return 220;
   }

   @Nullable
   public E getSelected() {
      return this.selected;
   }

   public void setSelected(@Nullable E pSelected) {
      this.selected = pSelected;
   }

   public E getFirstElement() {
      return this.children.get(0);
   }

   @Nullable
   public E getFocused() {
      return (E)super.getFocused();
   }

   @Override
   public final List<E> children() {
      return this.children;
   }

   protected void clearEntries() {
      this.children.clear();
      this.selected = null;
   }

   protected void replaceEntries(Collection<E> pEntries) {
      this.clearEntries();
      this.children.addAll(pEntries);
   }

   protected E getEntry(int pIndex) {
      return this.children().get(pIndex);
   }

   protected int addEntry(E pEntry) {
      this.children.add(pEntry);
      return this.children.size() - 1;
   }

   protected void addEntryToTop(E pEntry) {
      double $$1 = (double)this.getMaxScroll() - this.getScrollAmount();
      this.children.add(0, pEntry);
      this.setScrollAmount((double)this.getMaxScroll() - $$1);
   }

   protected boolean removeEntryFromTop(E pEntry) {
      double $$1 = (double)this.getMaxScroll() - this.getScrollAmount();
      boolean $$2 = this.removeEntry(pEntry);
      this.setScrollAmount((double)this.getMaxScroll() - $$1);
      return $$2;
   }

   protected int getItemCount() {
      return this.children().size();
   }

   protected boolean isSelectedItem(int pIndex) {
      return Objects.equals(this.getSelected(), this.children().get(pIndex));
   }

   @Nullable
   protected final E getEntryAtPosition(double pMouseX, double pMouseY) {
      int $$2 = this.getRowWidth() / 2;
      int $$3 = this.getX() + this.width / 2;
      int $$4 = $$3 - $$2;
      int $$5 = $$3 + $$2;
      int $$6 = Mth.floor(pMouseY - (double)this.getY()) - this.headerHeight + (int)this.getScrollAmount() - 4;
      int $$7 = $$6 / this.itemHeight;
      return pMouseX >= (double)$$4 && pMouseX <= (double)$$5 && $$7 >= 0 && $$6 >= 0 && $$7 < this.getItemCount() ? this.children().get($$7) : null;
   }

   public void updateSize(int pWidth, HeaderAndFooterLayout pLayout) {
      this.updateSizeAndPosition(pWidth, pLayout.getContentHeight(), pLayout.getHeaderHeight());
   }

   public void updateSizeAndPosition(int pWidth, int pHeight, int pY) {
      this.setSize(pWidth, pHeight);
      this.setPosition(0, pY);
      this.clampScrollAmount();
   }

   protected int getMaxPosition() {
      return this.getItemCount() * this.itemHeight + this.headerHeight;
   }

   protected boolean clickedHeader(int pX, int pY) {
      return false;
   }

   protected void renderHeader(GuiGraphics pGuiGraphics, int pX, int pY) {
   }

   protected void renderDecorations(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
   }

   @Override
   public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      this.hovered = this.isMouseOver((double)pMouseX, (double)pMouseY) ? this.getEntryAtPosition((double)pMouseX, (double)pMouseY) : null;
      this.renderListBackground(pGuiGraphics);
      this.enableScissor(pGuiGraphics);
      if (this.renderHeader) {
         int $$4 = this.getRowLeft();
         int $$5 = this.getY() + 4 - (int)this.getScrollAmount();
         this.renderHeader(pGuiGraphics, $$4, $$5);
      }

      this.renderListItems(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.disableScissor();
      this.renderListSeparators(pGuiGraphics);
      if (this.scrollbarVisible()) {
         int $$6 = this.getScrollbarPosition();
         int $$7 = (int)((float)(this.height * this.height) / (float)this.getMaxPosition());
         $$7 = Mth.clamp($$7, 32, this.height - 8);
         int $$8 = (int)this.getScrollAmount() * (this.height - $$7) / this.getMaxScroll() + this.getY();
         if ($$8 < this.getY()) {
            $$8 = this.getY();
         }

         RenderSystem.enableBlend();
         pGuiGraphics.blitSprite(SCROLLER_BACKGROUND_SPRITE, $$6, this.getY(), 6, this.getHeight());
         pGuiGraphics.blitSprite(SCROLLER_SPRITE, $$6, $$8, 6, $$7);
         RenderSystem.disableBlend();
      }

      this.renderDecorations(pGuiGraphics, pMouseX, pMouseY);
      RenderSystem.disableBlend();
   }

   protected boolean scrollbarVisible() {
      return this.getMaxScroll() > 0;
   }

   protected void renderListSeparators(GuiGraphics pGuiGraphics) {
      RenderSystem.enableBlend();
      ResourceLocation $$1 = this.minecraft.level == null ? Screen.HEADER_SEPARATOR : Screen.INWORLD_HEADER_SEPARATOR;
      ResourceLocation $$2 = this.minecraft.level == null ? Screen.FOOTER_SEPARATOR : Screen.INWORLD_FOOTER_SEPARATOR;
      pGuiGraphics.blit($$1, this.getX(), this.getY() - 2, 0.0F, 0.0F, this.getWidth(), 2, 32, 2);
      pGuiGraphics.blit($$2, this.getX(), this.getBottom(), 0.0F, 0.0F, this.getWidth(), 2, 32, 2);
      RenderSystem.disableBlend();
   }

   protected void renderListBackground(GuiGraphics pGuiGraphics) {
      RenderSystem.enableBlend();
      ResourceLocation $$1 = this.minecraft.level == null ? MENU_LIST_BACKGROUND : INWORLD_MENU_LIST_BACKGROUND;
      pGuiGraphics.blit(
         $$1,
         this.getX(),
         this.getY(),
         (float)this.getRight(),
         (float)(this.getBottom() + (int)this.getScrollAmount()),
         this.getWidth(),
         this.getHeight(),
         32,
         32
      );
      RenderSystem.disableBlend();
   }

   protected void enableScissor(GuiGraphics pGuiGraphics) {
      pGuiGraphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
   }

   protected void centerScrollOn(E pEntry) {
      this.setScrollAmount((double)(this.children().indexOf(pEntry) * this.itemHeight + this.itemHeight / 2 - this.height / 2));
   }

   protected void ensureVisible(E pEntry) {
      int $$1 = this.getRowTop(this.children().indexOf(pEntry));
      int $$2 = $$1 - this.getY() - 4 - this.itemHeight;
      if ($$2 < 0) {
         this.scroll($$2);
      }

      int $$3 = this.getBottom() - $$1 - this.itemHeight - this.itemHeight;
      if ($$3 < 0) {
         this.scroll(-$$3);
      }
   }

   private void scroll(int pScroll) {
      this.setScrollAmount(this.getScrollAmount() + (double)pScroll);
   }

   public double getScrollAmount() {
      return this.scrollAmount;
   }

   public void setClampedScrollAmount(double pScroll) {
      this.scrollAmount = Mth.clamp(pScroll, 0.0, (double)this.getMaxScroll());
   }

   public void setScrollAmount(double pScroll) {
      this.setClampedScrollAmount(pScroll);
   }

   public void clampScrollAmount() {
      this.setClampedScrollAmount(this.getScrollAmount());
   }

   public int getMaxScroll() {
      return Math.max(0, this.getMaxPosition() - (this.height - 4));
   }

   protected void updateScrollingState(double pMouseX, double pMouseY, int pButton) {
      this.scrolling = pButton == 0 && pMouseX >= (double)this.getScrollbarPosition() && pMouseX < (double)(this.getScrollbarPosition() + 6);
   }

   protected int getScrollbarPosition() {
      return this.getDefaultScrollbarPosition();
   }

   protected int getDefaultScrollbarPosition() {
      return this.getRealRowRight() + this.getListOutlinePadding();
   }

   private int getListOutlinePadding() {
      return 10;
   }

   protected boolean isValidMouseClick(int pButton) {
      return pButton == 0;
   }

   @Override
   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (!this.isValidMouseClick(pButton)) {
         return false;
      } else {
         this.updateScrollingState(pMouseX, pMouseY, pButton);
         if (!this.isMouseOver(pMouseX, pMouseY)) {
            return false;
         } else {
            E $$3 = this.getEntryAtPosition(pMouseX, pMouseY);
            if ($$3 != null) {
               if ($$3.mouseClicked(pMouseX, pMouseY, pButton)) {
                  E $$4 = this.getFocused();
                  if ($$4 != $$3 && $$4 instanceof ContainerEventHandler $$5) {
                     $$5.setFocused(null);
                  }

                  this.setFocused($$3);
                  this.setDragging(true);
                  return true;
               }
            } else if (this.clickedHeader(
               (int)(pMouseX - (double)(this.getX() + this.width / 2 - this.getRowWidth() / 2)),
               (int)(pMouseY - (double)this.getY()) + (int)this.getScrollAmount() - 4
            )) {
               return true;
            }

            return this.scrolling;
         }
      }
   }

   @Override
   public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
      return this.getFocused() != null ? this.getFocused().mouseReleased(pMouseX, pMouseY, pButton) : false;
   }

   @Override
   public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
      if (super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)) {
         return true;
      } else if (pButton == 0 && this.scrolling) {
         if (pMouseY < (double)this.getY()) {
            this.setScrollAmount(0.0);
         } else if (pMouseY > (double)this.getBottom()) {
            this.setScrollAmount((double)this.getMaxScroll());
         } else {
            double $$5 = (double)Math.max(1, this.getMaxScroll());
            int $$6 = this.height;
            int $$7 = Mth.clamp((int)((float)($$6 * $$6) / (float)this.getMaxPosition()), 32, $$6 - 8);
            double $$8 = Math.max(1.0, $$5 / (double)($$6 - $$7));
            this.setScrollAmount(this.getScrollAmount() + pDragY * $$8);
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
      this.setScrollAmount(this.getScrollAmount() - pScrollY * (double)this.itemHeight / 2.0);
      return true;
   }

   @Override
   public void setFocused(@Nullable GuiEventListener pFocused) {
      super.setFocused(pFocused);
      int $$1 = this.children.indexOf(pFocused);
      if ($$1 >= 0) {
         E $$2 = this.children.get($$1);
         this.setSelected($$2);
         if (this.minecraft.getLastInputType().isKeyboard()) {
            this.ensureVisible($$2);
         }
      }
   }

   @Nullable
   protected E nextEntry(ScreenDirection pDirection) {
      return this.nextEntry(pDirection, p_93510_ -> true);
   }

   @Nullable
   protected E nextEntry(ScreenDirection pDirection, Predicate<E> pPredicate) {
      return this.nextEntry(pDirection, pPredicate, this.getSelected());
   }

   @Nullable
   protected E nextEntry(ScreenDirection pDirection, Predicate<E> pPredicate, @Nullable E pSelected) {
      int $$3 = switch (pDirection) {
         case RIGHT, LEFT -> 0;
         case UP -> -1;
         case DOWN -> 1;
      };
      if (!this.children().isEmpty() && $$3 != 0) {
         int $$4;
         if (pSelected == null) {
            $$4 = $$3 > 0 ? 0 : this.children().size() - 1;
         } else {
            $$4 = this.children().indexOf(pSelected) + $$3;
         }

         for (int $$6 = $$4; $$6 >= 0 && $$6 < this.children.size(); $$6 += $$3) {
            E $$7 = this.children().get($$6);
            if (pPredicate.test($$7)) {
               return $$7;
            }
         }
      }

      return null;
   }

   @Override
   public boolean isMouseOver(double pMouseX, double pMouseY) {
      return pMouseY >= (double)this.getY() && pMouseY <= (double)this.getBottom() && pMouseX >= (double)this.getX() && pMouseX <= (double)this.getRight();
   }

   protected void renderListItems(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      int $$4 = this.getRowLeft();
      int $$5 = this.getRowWidth();
      int $$6 = this.itemHeight - 4;
      int $$7 = this.getItemCount();

      for (int $$8 = 0; $$8 < $$7; $$8++) {
         int $$9 = this.getRowTop($$8);
         int $$10 = this.getRowBottom($$8);
         if ($$10 >= this.getY() && $$9 <= this.getBottom()) {
            this.renderItem(pGuiGraphics, pMouseX, pMouseY, pPartialTick, $$8, $$4, $$9, $$5, $$6);
         }
      }
   }

   protected void renderItem(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick, int pIndex, int pLeft, int pTop, int pWidth, int pHeight) {
      E $$9 = this.getEntry(pIndex);
      $$9.renderBack(pGuiGraphics, pIndex, pTop, pLeft, pWidth, pHeight, pMouseX, pMouseY, Objects.equals(this.hovered, $$9), pPartialTick);
      if (this.isSelectedItem(pIndex)) {
         int $$10 = this.isFocused() ? -1 : -8355712;
         this.renderSelection(pGuiGraphics, pTop, pWidth, pHeight, $$10, -16777216);
      }

      $$9.render(pGuiGraphics, pIndex, pTop, pLeft, pWidth, pHeight, pMouseX, pMouseY, Objects.equals(this.hovered, $$9), pPartialTick);
   }

   protected void renderSelection(GuiGraphics pGuiGraphics, int pTop, int pWidth, int pHeight, int pOuterColor, int pInnerColor) {
      int $$6 = this.getX() + (this.width - pWidth) / 2;
      int $$7 = this.getX() + (this.width + pWidth) / 2;
      pGuiGraphics.fill($$6, pTop - 2, $$7, pTop + pHeight + 2, pOuterColor);
      pGuiGraphics.fill($$6 + 1, pTop - 1, $$7 - 1, pTop + pHeight + 1, pInnerColor);
   }

   public int getRowLeft() {
      return this.getX() + this.width / 2 - this.getRowWidth() / 2 + 2;
   }

   private int getRealRowLeft() {
      return this.getX() + this.width / 2 - this.getRowWidth() / 2;
   }

   public int getRowRight() {
      return this.getRowLeft() + this.getRowWidth();
   }

   private int getRealRowRight() {
      return this.getRealRowLeft() + this.getRowWidth();
   }

   protected int getRowTop(int pIndex) {
      return this.getY() + 4 - (int)this.getScrollAmount() + pIndex * this.itemHeight + this.headerHeight;
   }

   protected int getRowBottom(int pIndex) {
      return this.getRowTop(pIndex) + this.itemHeight;
   }

   @Override
   public NarratableEntry.NarrationPriority narrationPriority() {
      if (this.isFocused()) {
         return NarratableEntry.NarrationPriority.FOCUSED;
      } else {
         return this.hovered != null ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
      }
   }

   @Nullable
   protected E remove(int pIndex) {
      E $$1 = this.children.get(pIndex);
      return this.removeEntry(this.children.get(pIndex)) ? $$1 : null;
   }

   protected boolean removeEntry(E pEntry) {
      boolean $$1 = this.children.remove(pEntry);
      if ($$1 && pEntry == this.getSelected()) {
         this.setSelected(null);
      }

      return $$1;
   }

   @Nullable
   protected E getHovered() {
      return this.hovered;
   }

   void bindEntryToSelf(AbstractSelectionList.Entry<E> pEntry) {
      pEntry.list = this;
   }

   protected void narrateListElementPosition(NarrationElementOutput pNarrationElementOutput, E pEntry) {
      List<E> $$2 = this.children();
      if ($$2.size() > 1) {
         int $$3 = $$2.indexOf(pEntry);
         if ($$3 != -1) {
            pNarrationElementOutput.add(NarratedElementType.POSITION, Component.translatable("narrator.position.list", $$3 + 1, $$2.size()));
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   protected abstract static class Entry<E extends AbstractSelectionList.Entry<E>> implements GuiEventListener {
      @Deprecated
      protected AbstractSelectionList<E> list;

      @Override
      public void setFocused(boolean pFocused) {
      }

      @Override
      public boolean isFocused() {
         return this.list.getFocused() == this;
      }

      public abstract void render(
         GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick
      );

      public void renderBack(
         GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick
      ) {
      }

      @Override
      public boolean isMouseOver(double pMouseX, double pMouseY) {
         return Objects.equals(this.list.getEntryAtPosition(pMouseX, pMouseY), this);
      }
   }

   @OnlyIn(Dist.CLIENT)
   class TrackedList extends AbstractList<E> {
      private final List<E> delegate = Lists.newArrayList();

      public E get(int pIndex) {
         return this.delegate.get(pIndex);
      }

      @Override
      public int size() {
         return this.delegate.size();
      }

      public E set(int pIndex, E pEntry) {
         E $$2 = this.delegate.set(pIndex, pEntry);
         AbstractSelectionList.this.bindEntryToSelf(pEntry);
         return $$2;
      }

      public void add(int pIndex, E pEntry) {
         this.delegate.add(pIndex, pEntry);
         AbstractSelectionList.this.bindEntryToSelf(pEntry);
      }

      public E remove(int pIndex) {
         return this.delegate.remove(pIndex);
      }
   }
}
