package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;

@OnlyIn(Dist.CLIENT)
public class BookEditScreen extends Screen {
   private static final int TEXT_WIDTH = 114;
   private static final int TEXT_HEIGHT = 128;
   private static final int IMAGE_WIDTH = 192;
   private static final int IMAGE_HEIGHT = 192;
   private static final Component EDIT_TITLE_LABEL = Component.translatable("book.editTitle");
   private static final Component FINALIZE_WARNING_LABEL = Component.translatable("book.finalizeWarning");
   private static final FormattedCharSequence BLACK_CURSOR = FormattedCharSequence.forward("_", Style.EMPTY.withColor(ChatFormatting.BLACK));
   private static final FormattedCharSequence GRAY_CURSOR = FormattedCharSequence.forward("_", Style.EMPTY.withColor(ChatFormatting.GRAY));
   private final Player owner;
   private final ItemStack book;
   private boolean isModified;
   private boolean isSigning;
   private int frameTick;
   private int currentPage;
   private final List<String> pages = Lists.newArrayList();
   private String title = "";
   private final TextFieldHelper pageEdit = new TextFieldHelper(
      this::getCurrentPageText,
      this::setCurrentPageText,
      this::getClipboard,
      this::setClipboard,
      p_280853_ -> p_280853_.length() < 1024 && this.font.wordWrapHeight(p_280853_, 114) <= 128
   );
   private final TextFieldHelper titleEdit = new TextFieldHelper(
      () -> this.title, p_98175_ -> this.title = p_98175_, this::getClipboard, this::setClipboard, p_98170_ -> p_98170_.length() < 16
   );
   private long lastClickTime;
   private int lastIndex = -1;
   private PageButton forwardButton;
   private PageButton backButton;
   private Button doneButton;
   private Button signButton;
   private Button finalizeButton;
   private Button cancelButton;
   private final InteractionHand hand;
   @Nullable
   private BookEditScreen.DisplayCache displayCache = BookEditScreen.DisplayCache.EMPTY;
   private Component pageMsg = CommonComponents.EMPTY;
   private final Component ownerText;

   public BookEditScreen(Player pOwner, ItemStack pBook, InteractionHand pHand) {
      super(GameNarrator.NO_TITLE);
      this.owner = pOwner;
      this.book = pBook;
      this.hand = pHand;
      WritableBookContent $$3 = pBook.get(DataComponents.WRITABLE_BOOK_CONTENT);
      if ($$3 != null) {
         $$3.getPages(Minecraft.getInstance().isTextFilteringEnabled()).forEach(this.pages::add);
      }

      if (this.pages.isEmpty()) {
         this.pages.add("");
      }

      this.ownerText = Component.translatable("book.byAuthor", pOwner.getName()).withStyle(ChatFormatting.DARK_GRAY);
   }

   private void setClipboard(String p_98148_) {
      if (this.minecraft != null) {
         TextFieldHelper.setClipboardContents(this.minecraft, p_98148_);
      }
   }

   private String getClipboard() {
      return this.minecraft != null ? TextFieldHelper.getClipboardContents(this.minecraft) : "";
   }

   private int getNumPages() {
      return this.pages.size();
   }

   @Override
   public void tick() {
      super.tick();
      this.frameTick++;
   }

   @Override
   protected void init() {
      this.clearDisplayCache();
      this.signButton = this.addRenderableWidget(Button.builder(Component.translatable("book.signButton"), p_98177_ -> {
         this.isSigning = true;
         this.updateButtonVisibility();
      }).bounds(this.width / 2 - 100, 196, 98, 20).build());
      this.doneButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, p_280851_ -> {
         this.minecraft.setScreen(null);
         this.saveChanges(false);
      }).bounds(this.width / 2 + 2, 196, 98, 20).build());
      this.finalizeButton = this.addRenderableWidget(Button.builder(Component.translatable("book.finalizeButton"), p_280852_ -> {
         if (this.isSigning) {
            this.saveChanges(true);
            this.minecraft.setScreen(null);
         }
      }).bounds(this.width / 2 - 100, 196, 98, 20).build());
      this.cancelButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, p_98157_ -> {
         if (this.isSigning) {
            this.isSigning = false;
         }

         this.updateButtonVisibility();
      }).bounds(this.width / 2 + 2, 196, 98, 20).build());
      int $$0 = (this.width - 192) / 2;
      int $$1 = 2;
      this.forwardButton = this.addRenderableWidget(new PageButton($$0 + 116, 159, true, p_98144_ -> this.pageForward(), true));
      this.backButton = this.addRenderableWidget(new PageButton($$0 + 43, 159, false, p_98113_ -> this.pageBack(), true));
      this.updateButtonVisibility();
   }

   private void pageBack() {
      if (this.currentPage > 0) {
         this.currentPage--;
      }

      this.updateButtonVisibility();
      this.clearDisplayCacheAfterPageChange();
   }

   private void pageForward() {
      if (this.currentPage < this.getNumPages() - 1) {
         this.currentPage++;
      } else {
         this.appendPageToBook();
         if (this.currentPage < this.getNumPages() - 1) {
            this.currentPage++;
         }
      }

      this.updateButtonVisibility();
      this.clearDisplayCacheAfterPageChange();
   }

   private void updateButtonVisibility() {
      this.backButton.visible = !this.isSigning && this.currentPage > 0;
      this.forwardButton.visible = !this.isSigning;
      this.doneButton.visible = !this.isSigning;
      this.signButton.visible = !this.isSigning;
      this.cancelButton.visible = this.isSigning;
      this.finalizeButton.visible = this.isSigning;
      this.finalizeButton.active = !StringUtil.isBlank(this.title);
   }

   private void eraseEmptyTrailingPages() {
      ListIterator<String> $$0 = this.pages.listIterator(this.pages.size());

      while ($$0.hasPrevious() && $$0.previous().isEmpty()) {
         $$0.remove();
      }
   }

   private void saveChanges(boolean pPublish) {
      if (this.isModified) {
         this.eraseEmptyTrailingPages();
         this.updateLocalCopy();
         int $$1 = this.hand == InteractionHand.MAIN_HAND ? this.owner.getInventory().selected : 40;
         this.minecraft.getConnection().send(new ServerboundEditBookPacket($$1, this.pages, pPublish ? Optional.of(this.title.trim()) : Optional.empty()));
      }
   }

   private void updateLocalCopy() {
      this.book.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(this.pages.stream().map(Filterable::passThrough).toList()));
   }

   private void appendPageToBook() {
      if (this.getNumPages() < 100) {
         this.pages.add("");
         this.isModified = true;
      }
   }

   @Override
   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (super.keyPressed(pKeyCode, pScanCode, pModifiers)) {
         return true;
      } else if (this.isSigning) {
         return this.titleKeyPressed(pKeyCode, pScanCode, pModifiers);
      } else {
         boolean $$3 = this.bookKeyPressed(pKeyCode, pScanCode, pModifiers);
         if ($$3) {
            this.clearDisplayCache();
            return true;
         } else {
            return false;
         }
      }
   }

   @Override
   public boolean charTyped(char pCodePoint, int pModifiers) {
      if (super.charTyped(pCodePoint, pModifiers)) {
         return true;
      } else if (this.isSigning) {
         boolean $$2 = this.titleEdit.charTyped(pCodePoint);
         if ($$2) {
            this.updateButtonVisibility();
            this.isModified = true;
            return true;
         } else {
            return false;
         }
      } else if (StringUtil.isAllowedChatCharacter(pCodePoint)) {
         this.pageEdit.insertText(Character.toString(pCodePoint));
         this.clearDisplayCache();
         return true;
      } else {
         return false;
      }
   }

   private boolean bookKeyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (Screen.isSelectAll(pKeyCode)) {
         this.pageEdit.selectAll();
         return true;
      } else if (Screen.isCopy(pKeyCode)) {
         this.pageEdit.copy();
         return true;
      } else if (Screen.isPaste(pKeyCode)) {
         this.pageEdit.paste();
         return true;
      } else if (Screen.isCut(pKeyCode)) {
         this.pageEdit.cut();
         return true;
      } else {
         TextFieldHelper.CursorStep $$3 = Screen.hasControlDown() ? TextFieldHelper.CursorStep.WORD : TextFieldHelper.CursorStep.CHARACTER;
         switch (pKeyCode) {
            case 257:
            case 335:
               this.pageEdit.insertText("\n");
               return true;
            case 259:
               this.pageEdit.removeFromCursor(-1, $$3);
               return true;
            case 261:
               this.pageEdit.removeFromCursor(1, $$3);
               return true;
            case 262:
               this.pageEdit.moveBy(1, Screen.hasShiftDown(), $$3);
               return true;
            case 263:
               this.pageEdit.moveBy(-1, Screen.hasShiftDown(), $$3);
               return true;
            case 264:
               this.keyDown();
               return true;
            case 265:
               this.keyUp();
               return true;
            case 266:
               this.backButton.onPress();
               return true;
            case 267:
               this.forwardButton.onPress();
               return true;
            case 268:
               this.keyHome();
               return true;
            case 269:
               this.keyEnd();
               return true;
            default:
               return false;
         }
      }
   }

   private void keyUp() {
      this.changeLine(-1);
   }

   private void keyDown() {
      this.changeLine(1);
   }

   private void changeLine(int pYChange) {
      int $$1 = this.pageEdit.getCursorPos();
      int $$2 = this.getDisplayCache().changeLine($$1, pYChange);
      this.pageEdit.setCursorPos($$2, Screen.hasShiftDown());
   }

   private void keyHome() {
      if (Screen.hasControlDown()) {
         this.pageEdit.setCursorToStart(Screen.hasShiftDown());
      } else {
         int $$0 = this.pageEdit.getCursorPos();
         int $$1 = this.getDisplayCache().findLineStart($$0);
         this.pageEdit.setCursorPos($$1, Screen.hasShiftDown());
      }
   }

   private void keyEnd() {
      if (Screen.hasControlDown()) {
         this.pageEdit.setCursorToEnd(Screen.hasShiftDown());
      } else {
         BookEditScreen.DisplayCache $$0 = this.getDisplayCache();
         int $$1 = this.pageEdit.getCursorPos();
         int $$2 = $$0.findLineEnd($$1);
         this.pageEdit.setCursorPos($$2, Screen.hasShiftDown());
      }
   }

   private boolean titleKeyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      switch (pKeyCode) {
         case 257:
         case 335:
            if (!this.title.isEmpty()) {
               this.saveChanges(true);
               this.minecraft.setScreen(null);
            }

            return true;
         case 259:
            this.titleEdit.removeCharsFromCursor(-1);
            this.updateButtonVisibility();
            this.isModified = true;
            return true;
         default:
            return false;
      }
   }

   private String getCurrentPageText() {
      return this.currentPage >= 0 && this.currentPage < this.pages.size() ? this.pages.get(this.currentPage) : "";
   }

   private void setCurrentPageText(String p_98159_) {
      if (this.currentPage >= 0 && this.currentPage < this.pages.size()) {
         this.pages.set(this.currentPage, p_98159_);
         this.isModified = true;
         this.clearDisplayCache();
      }
   }

   @Override
   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      this.setFocused(null);
      int $$4 = (this.width - 192) / 2;
      int $$5 = 2;
      if (this.isSigning) {
         boolean $$6 = this.frameTick / 6 % 2 == 0;
         FormattedCharSequence $$7 = FormattedCharSequence.composite(FormattedCharSequence.forward(this.title, Style.EMPTY), $$6 ? BLACK_CURSOR : GRAY_CURSOR);
         int $$8 = this.font.width(EDIT_TITLE_LABEL);
         pGuiGraphics.drawString(this.font, EDIT_TITLE_LABEL, $$4 + 36 + (114 - $$8) / 2, 34, 0, false);
         int $$9 = this.font.width($$7);
         pGuiGraphics.drawString(this.font, $$7, $$4 + 36 + (114 - $$9) / 2, 50, 0, false);
         int $$10 = this.font.width(this.ownerText);
         pGuiGraphics.drawString(this.font, this.ownerText, $$4 + 36 + (114 - $$10) / 2, 60, 0, false);
         pGuiGraphics.drawWordWrap(this.font, FINALIZE_WARNING_LABEL, $$4 + 36, 82, 114, 0);
      } else {
         int $$11 = this.font.width(this.pageMsg);
         pGuiGraphics.drawString(this.font, this.pageMsg, $$4 - $$11 + 192 - 44, 18, 0, false);
         BookEditScreen.DisplayCache $$12 = this.getDisplayCache();

         for (BookEditScreen.LineInfo $$13 : $$12.lines) {
            pGuiGraphics.drawString(this.font, $$13.asComponent, $$13.x, $$13.y, -16777216, false);
         }

         this.renderHighlight(pGuiGraphics, $$12.selection);
         this.renderCursor(pGuiGraphics, $$12.cursor, $$12.cursorAtEnd);
      }
   }

   @Override
   public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderTransparentBackground(pGuiGraphics);
      pGuiGraphics.blit(BookViewScreen.BOOK_LOCATION, (this.width - 192) / 2, 2, 0, 0, 192, 192);
   }

   private void renderCursor(GuiGraphics pGuiGraphics, BookEditScreen.Pos2i pCursorPos, boolean pIsEndOfText) {
      if (this.frameTick / 6 % 2 == 0) {
         pCursorPos = this.convertLocalToScreen(pCursorPos);
         if (!pIsEndOfText) {
            pGuiGraphics.fill(pCursorPos.x, pCursorPos.y - 1, pCursorPos.x + 1, pCursorPos.y + 9, -16777216);
         } else {
            pGuiGraphics.drawString(this.font, "_", pCursorPos.x, pCursorPos.y, 0, false);
         }
      }
   }

   private void renderHighlight(GuiGraphics pGuiGraphics, Rect2i[] pHighlightAreas) {
      for (Rect2i $$2 : pHighlightAreas) {
         int $$3 = $$2.getX();
         int $$4 = $$2.getY();
         int $$5 = $$3 + $$2.getWidth();
         int $$6 = $$4 + $$2.getHeight();
         pGuiGraphics.fill(RenderType.guiTextHighlight(), $$3, $$4, $$5, $$6, -16776961);
      }
   }

   private BookEditScreen.Pos2i convertScreenToLocal(BookEditScreen.Pos2i pScreenPos) {
      return new BookEditScreen.Pos2i(pScreenPos.x - (this.width - 192) / 2 - 36, pScreenPos.y - 32);
   }

   private BookEditScreen.Pos2i convertLocalToScreen(BookEditScreen.Pos2i pLocalScreenPos) {
      return new BookEditScreen.Pos2i(pLocalScreenPos.x + (this.width - 192) / 2 + 36, pLocalScreenPos.y + 32);
   }

   @Override
   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (super.mouseClicked(pMouseX, pMouseY, pButton)) {
         return true;
      } else {
         if (pButton == 0) {
            long $$3 = Util.getMillis();
            BookEditScreen.DisplayCache $$4 = this.getDisplayCache();
            int $$5 = $$4.getIndexAtPosition(this.font, this.convertScreenToLocal(new BookEditScreen.Pos2i((int)pMouseX, (int)pMouseY)));
            if ($$5 >= 0) {
               if ($$5 != this.lastIndex || $$3 - this.lastClickTime >= 250L) {
                  this.pageEdit.setCursorPos($$5, Screen.hasShiftDown());
               } else if (!this.pageEdit.isSelecting()) {
                  this.selectWord($$5);
               } else {
                  this.pageEdit.selectAll();
               }

               this.clearDisplayCache();
            }

            this.lastIndex = $$5;
            this.lastClickTime = $$3;
         }

         return true;
      }
   }

   private void selectWord(int pIndex) {
      String $$1 = this.getCurrentPageText();
      this.pageEdit.setSelectionRange(StringSplitter.getWordPosition($$1, -1, pIndex, false), StringSplitter.getWordPosition($$1, 1, pIndex, false));
   }

   @Override
   public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
      if (super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)) {
         return true;
      } else {
         if (pButton == 0) {
            BookEditScreen.DisplayCache $$5 = this.getDisplayCache();
            int $$6 = $$5.getIndexAtPosition(this.font, this.convertScreenToLocal(new BookEditScreen.Pos2i((int)pMouseX, (int)pMouseY)));
            this.pageEdit.setCursorPos($$6, true);
            this.clearDisplayCache();
         }

         return true;
      }
   }

   private BookEditScreen.DisplayCache getDisplayCache() {
      if (this.displayCache == null) {
         this.displayCache = this.rebuildDisplayCache();
         this.pageMsg = Component.translatable("book.pageIndicator", this.currentPage + 1, this.getNumPages());
      }

      return this.displayCache;
   }

   private void clearDisplayCache() {
      this.displayCache = null;
   }

   private void clearDisplayCacheAfterPageChange() {
      this.pageEdit.setCursorToEnd();
      this.clearDisplayCache();
   }

   private BookEditScreen.DisplayCache rebuildDisplayCache() {
      String $$0 = this.getCurrentPageText();
      if ($$0.isEmpty()) {
         return BookEditScreen.DisplayCache.EMPTY;
      } else {
         int $$1 = this.pageEdit.getCursorPos();
         int $$2 = this.pageEdit.getSelectionPos();
         IntList $$3 = new IntArrayList();
         List<BookEditScreen.LineInfo> $$4 = Lists.newArrayList();
         MutableInt $$5 = new MutableInt();
         MutableBoolean $$6 = new MutableBoolean();
         StringSplitter $$7 = this.font.getSplitter();
         $$7.splitLines($$0, 114, Style.EMPTY, true, (p_98132_, p_98133_, p_98134_) -> {
            int $$8x = $$5.getAndIncrement();
            String $$9x = $$0.substring(p_98133_, p_98134_);
            $$6.setValue($$9x.endsWith("\n"));
            String $$10 = StringUtils.stripEnd($$9x, " \n");
            int $$11 = $$8x * 9;
            BookEditScreen.Pos2i $$12 = this.convertLocalToScreen(new BookEditScreen.Pos2i(0, $$11));
            $$3.add(p_98133_);
            $$4.add(new BookEditScreen.LineInfo(p_98132_, $$10, $$12.x, $$12.y));
         });
         int[] $$8 = $$3.toIntArray();
         boolean $$9 = $$1 == $$0.length();
         BookEditScreen.Pos2i $$10;
         if ($$9 && $$6.isTrue()) {
            $$10 = new BookEditScreen.Pos2i(0, $$4.size() * 9);
         } else {
            int $$11 = findLineFromPos($$8, $$1);
            int $$12 = this.font.width($$0.substring($$8[$$11], $$1));
            $$10 = new BookEditScreen.Pos2i($$12, $$11 * 9);
         }

         List<Rect2i> $$14 = Lists.newArrayList();
         if ($$1 != $$2) {
            int $$15 = Math.min($$1, $$2);
            int $$16 = Math.max($$1, $$2);
            int $$17 = findLineFromPos($$8, $$15);
            int $$18 = findLineFromPos($$8, $$16);
            if ($$17 == $$18) {
               int $$19 = $$17 * 9;
               int $$20 = $$8[$$17];
               $$14.add(this.createPartialLineSelection($$0, $$7, $$15, $$16, $$19, $$20));
            } else {
               int $$21 = $$17 + 1 > $$8.length ? $$0.length() : $$8[$$17 + 1];
               $$14.add(this.createPartialLineSelection($$0, $$7, $$15, $$21, $$17 * 9, $$8[$$17]));

               for (int $$22 = $$17 + 1; $$22 < $$18; $$22++) {
                  int $$23 = $$22 * 9;
                  String $$24 = $$0.substring($$8[$$22], $$8[$$22 + 1]);
                  int $$25 = (int)$$7.stringWidth($$24);
                  $$14.add(this.createSelection(new BookEditScreen.Pos2i(0, $$23), new BookEditScreen.Pos2i($$25, $$23 + 9)));
               }

               $$14.add(this.createPartialLineSelection($$0, $$7, $$8[$$18], $$16, $$18 * 9, $$8[$$18]));
            }
         }

         return new BookEditScreen.DisplayCache($$0, $$10, $$9, $$8, $$4.toArray(new BookEditScreen.LineInfo[0]), $$14.toArray(new Rect2i[0]));
      }
   }

   static int findLineFromPos(int[] pLineStarts, int pFind) {
      int $$2 = Arrays.binarySearch(pLineStarts, pFind);
      return $$2 < 0 ? -($$2 + 2) : $$2;
   }

   private Rect2i createPartialLineSelection(String pInput, StringSplitter pSplitter, int pStartPos, int pEndPos, int pY, int pLineStart) {
      String $$6 = pInput.substring(pLineStart, pStartPos);
      String $$7 = pInput.substring(pLineStart, pEndPos);
      BookEditScreen.Pos2i $$8 = new BookEditScreen.Pos2i((int)pSplitter.stringWidth($$6), pY);
      BookEditScreen.Pos2i $$9 = new BookEditScreen.Pos2i((int)pSplitter.stringWidth($$7), pY + 9);
      return this.createSelection($$8, $$9);
   }

   private Rect2i createSelection(BookEditScreen.Pos2i pCorner1, BookEditScreen.Pos2i pCorner2) {
      BookEditScreen.Pos2i $$2 = this.convertLocalToScreen(pCorner1);
      BookEditScreen.Pos2i $$3 = this.convertLocalToScreen(pCorner2);
      int $$4 = Math.min($$2.x, $$3.x);
      int $$5 = Math.max($$2.x, $$3.x);
      int $$6 = Math.min($$2.y, $$3.y);
      int $$7 = Math.max($$2.y, $$3.y);
      return new Rect2i($$4, $$6, $$5 - $$4, $$7 - $$6);
   }

   @OnlyIn(Dist.CLIENT)
   static class DisplayCache {
      static final BookEditScreen.DisplayCache EMPTY = new BookEditScreen.DisplayCache(
         "",
         new BookEditScreen.Pos2i(0, 0),
         true,
         new int[]{0},
         new BookEditScreen.LineInfo[]{new BookEditScreen.LineInfo(Style.EMPTY, "", 0, 0)},
         new Rect2i[0]
      );
      private final String fullText;
      final BookEditScreen.Pos2i cursor;
      final boolean cursorAtEnd;
      private final int[] lineStarts;
      final BookEditScreen.LineInfo[] lines;
      final Rect2i[] selection;

      public DisplayCache(
         String pFullText, BookEditScreen.Pos2i pCursor, boolean pCursorAtEnd, int[] pLineStarts, BookEditScreen.LineInfo[] pLines, Rect2i[] pSelection
      ) {
         this.fullText = pFullText;
         this.cursor = pCursor;
         this.cursorAtEnd = pCursorAtEnd;
         this.lineStarts = pLineStarts;
         this.lines = pLines;
         this.selection = pSelection;
      }

      public int getIndexAtPosition(Font pFont, BookEditScreen.Pos2i pCursorPosition) {
         int $$2 = pCursorPosition.y / 9;
         if ($$2 < 0) {
            return 0;
         } else if ($$2 >= this.lines.length) {
            return this.fullText.length();
         } else {
            BookEditScreen.LineInfo $$3 = this.lines[$$2];
            return this.lineStarts[$$2] + pFont.getSplitter().plainIndexAtWidth($$3.contents, pCursorPosition.x, $$3.style);
         }
      }

      public int changeLine(int pXChange, int pYChange) {
         int $$2 = BookEditScreen.findLineFromPos(this.lineStarts, pXChange);
         int $$3 = $$2 + pYChange;
         int $$6;
         if (0 <= $$3 && $$3 < this.lineStarts.length) {
            int $$4 = pXChange - this.lineStarts[$$2];
            int $$5 = this.lines[$$3].contents.length();
            $$6 = this.lineStarts[$$3] + Math.min($$4, $$5);
         } else {
            $$6 = pXChange;
         }

         return $$6;
      }

      public int findLineStart(int pLine) {
         int $$1 = BookEditScreen.findLineFromPos(this.lineStarts, pLine);
         return this.lineStarts[$$1];
      }

      public int findLineEnd(int pLine) {
         int $$1 = BookEditScreen.findLineFromPos(this.lineStarts, pLine);
         return this.lineStarts[$$1] + this.lines[$$1].contents.length();
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class LineInfo {
      final Style style;
      final String contents;
      final Component asComponent;
      final int x;
      final int y;

      public LineInfo(Style pStyle, String pContents, int pX, int pY) {
         this.style = pStyle;
         this.contents = pContents;
         this.x = pX;
         this.y = pY;
         this.asComponent = Component.literal(pContents).setStyle(pStyle);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class Pos2i {
      public final int x;
      public final int y;

      Pos2i(int pX, int pY) {
         this.x = pX;
         this.y = pY;
      }
   }
}
