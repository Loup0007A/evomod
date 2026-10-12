package net.minecraft.client.gui.screens.inventory;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BookViewScreen extends Screen {
   public static final int PAGE_INDICATOR_TEXT_Y_OFFSET = 16;
   public static final int PAGE_TEXT_X_OFFSET = 36;
   public static final int PAGE_TEXT_Y_OFFSET = 30;
   public static final BookViewScreen.BookAccess EMPTY_ACCESS = new BookViewScreen.BookAccess(List.of());
   public static final ResourceLocation BOOK_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/book.png");
   protected static final int TEXT_WIDTH = 114;
   protected static final int TEXT_HEIGHT = 128;
   protected static final int IMAGE_WIDTH = 192;
   protected static final int IMAGE_HEIGHT = 192;
   private BookViewScreen.BookAccess bookAccess;
   private int currentPage;
   private List<FormattedCharSequence> cachedPageComponents = Collections.emptyList();
   private int cachedPage = -1;
   private Component pageMsg = CommonComponents.EMPTY;
   private PageButton forwardButton;
   private PageButton backButton;
   private final boolean playTurnSound;

   public BookViewScreen(BookViewScreen.BookAccess pBookAccess) {
      this(pBookAccess, true);
   }

   public BookViewScreen() {
      this(EMPTY_ACCESS, false);
   }

   private BookViewScreen(BookViewScreen.BookAccess pBookAccess, boolean pPlayTurnSound) {
      super(GameNarrator.NO_TITLE);
      this.bookAccess = pBookAccess;
      this.playTurnSound = pPlayTurnSound;
   }

   public void setBookAccess(BookViewScreen.BookAccess pBookAccess) {
      this.bookAccess = pBookAccess;
      this.currentPage = Mth.clamp(this.currentPage, 0, pBookAccess.getPageCount());
      this.updateButtonVisibility();
      this.cachedPage = -1;
   }

   public boolean setPage(int pPageNum) {
      int $$1 = Mth.clamp(pPageNum, 0, this.bookAccess.getPageCount() - 1);
      if ($$1 != this.currentPage) {
         this.currentPage = $$1;
         this.updateButtonVisibility();
         this.cachedPage = -1;
         return true;
      } else {
         return false;
      }
   }

   protected boolean forcePage(int pPageNum) {
      return this.setPage(pPageNum);
   }

   @Override
   protected void init() {
      this.createMenuControls();
      this.createPageControlButtons();
   }

   protected void createMenuControls() {
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, p_325379_ -> this.onClose()).bounds(this.width / 2 - 100, 196, 200, 20).build());
   }

   protected void createPageControlButtons() {
      int $$0 = (this.width - 192) / 2;
      int $$1 = 2;
      this.forwardButton = this.addRenderableWidget(new PageButton($$0 + 116, 159, true, p_98297_ -> this.pageForward(), this.playTurnSound));
      this.backButton = this.addRenderableWidget(new PageButton($$0 + 43, 159, false, p_98287_ -> this.pageBack(), this.playTurnSound));
      this.updateButtonVisibility();
   }

   private int getNumPages() {
      return this.bookAccess.getPageCount();
   }

   protected void pageBack() {
      if (this.currentPage > 0) {
         this.currentPage--;
      }

      this.updateButtonVisibility();
   }

   protected void pageForward() {
      if (this.currentPage < this.getNumPages() - 1) {
         this.currentPage++;
      }

      this.updateButtonVisibility();
   }

   private void updateButtonVisibility() {
      this.forwardButton.visible = this.currentPage < this.getNumPages() - 1;
      this.backButton.visible = this.currentPage > 0;
   }

   @Override
   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
         return true;
      } else {
         switch (pKeyCode) {
            case 266:
               this.backButton.onPress();
               return true;
            case 267:
               this.forwardButton.onPress();
               return true;
            default:
               return false;
         }
      }
   }

   @Override
   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      int $$4 = (this.width - 192) / 2;
      int $$5 = 2;
      if (this.cachedPage != this.currentPage) {
         FormattedText $$6 = this.bookAccess.getPage(this.currentPage);
         this.cachedPageComponents = this.font.split($$6, 114);
         this.pageMsg = Component.translatable("book.pageIndicator", this.currentPage + 1, Math.max(this.getNumPages(), 1));
      }

      this.cachedPage = this.currentPage;
      int $$7 = this.font.width(this.pageMsg);
      pGuiGraphics.drawString(this.font, this.pageMsg, $$4 - $$7 + 192 - 44, 18, 0, false);
      int $$8 = Math.min(128 / 9, this.cachedPageComponents.size());

      for (int $$9 = 0; $$9 < $$8; $$9++) {
         FormattedCharSequence $$10 = this.cachedPageComponents.get($$9);
         pGuiGraphics.drawString(this.font, $$10, $$4 + 36, 32 + $$9 * 9, 0, false);
      }

      Style $$11 = this.getClickedComponentStyleAt((double)pMouseX, (double)pMouseY);
      if ($$11 != null) {
         pGuiGraphics.renderComponentHoverEffect(this.font, $$11, pMouseX, pMouseY);
      }
   }

   @Override
   public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderTransparentBackground(pGuiGraphics);
      pGuiGraphics.blit(BOOK_LOCATION, (this.width - 192) / 2, 2, 0, 0, 192, 192);
   }

   @Override
   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (pButton == 0) {
         Style $$3 = this.getClickedComponentStyleAt(pMouseX, pMouseY);
         if ($$3 != null && this.handleComponentClicked($$3)) {
            return true;
         }
      }

      return super.mouseClicked(pMouseX, pMouseY, pButton);
   }

   @Override
   public boolean handleComponentClicked(Style pStyle) {
      ClickEvent $$1 = pStyle.getClickEvent();
      if ($$1 == null) {
         return false;
      } else if ($$1.getAction() == ClickEvent.Action.CHANGE_PAGE) {
         String $$2 = $$1.getValue();

         try {
            int $$3 = Integer.parseInt($$2) - 1;
            return this.forcePage($$3);
         } catch (Exception var5) {
            return false;
         }
      } else {
         boolean $$4 = super.handleComponentClicked(pStyle);
         if ($$4 && $$1.getAction() == ClickEvent.Action.RUN_COMMAND) {
            this.closeScreen();
         }

         return $$4;
      }
   }

   protected void closeScreen() {
      this.minecraft.setScreen(null);
   }

   @Nullable
   public Style getClickedComponentStyleAt(double pMouseX, double pMouseY) {
      if (this.cachedPageComponents.isEmpty()) {
         return null;
      } else {
         int $$2 = Mth.floor(pMouseX - (double)((this.width - 192) / 2) - 36.0);
         int $$3 = Mth.floor(pMouseY - 2.0 - 30.0);
         if ($$2 >= 0 && $$3 >= 0) {
            int $$4 = Math.min(128 / 9, this.cachedPageComponents.size());
            if ($$2 <= 114 && $$3 < 9 * $$4 + $$4) {
               int $$5 = $$3 / 9;
               if ($$5 >= 0 && $$5 < this.cachedPageComponents.size()) {
                  FormattedCharSequence $$6 = this.cachedPageComponents.get($$5);
                  return this.minecraft.font.getSplitter().componentStyleAtWidth($$6, $$2);
               } else {
                  return null;
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static record BookAccess(List<Component> pages) {
      public int getPageCount() {
         return this.pages.size();
      }

      public FormattedText getPage(int pPage) {
         return pPage >= 0 && pPage < this.getPageCount() ? this.pages.get(pPage) : FormattedText.EMPTY;
      }

      @Nullable
      public static BookViewScreen.BookAccess fromItem(ItemStack pStack) {
         boolean $$1 = Minecraft.getInstance().isTextFilteringEnabled();
         WrittenBookContent $$2 = pStack.get(DataComponents.WRITTEN_BOOK_CONTENT);
         if ($$2 != null) {
            return new BookViewScreen.BookAccess($$2.getPages($$1));
         } else {
            WritableBookContent $$3 = pStack.get(DataComponents.WRITABLE_BOOK_CONTENT);
            return $$3 != null ? new BookViewScreen.BookAccess($$3.getPages($$1).map(Component::literal).toList()) : null;
         }
      }
   }
}
