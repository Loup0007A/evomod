package net.minecraft.client.gui.components;

import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiLineEditBox extends AbstractScrollWidget {
   private static final int CURSOR_INSERT_WIDTH = 1;
   private static final int CURSOR_INSERT_COLOR = -3092272;
   private static final String CURSOR_APPEND_CHARACTER = "_";
   private static final int TEXT_COLOR = -2039584;
   private static final int PLACEHOLDER_TEXT_COLOR = -857677600;
   private static final int CURSOR_BLINK_INTERVAL_MS = 300;
   private final Font font;
   private final Component placeholder;
   private final MultilineTextField textField;
   private long focusedTime = Util.getMillis();

   public MultiLineEditBox(Font pFont, int pX, int pY, int pWidth, int pHeight, Component pPlaceholder, Component pMessage) {
      super(pX, pY, pWidth, pHeight, pMessage);
      this.font = pFont;
      this.placeholder = pPlaceholder;
      this.textField = new MultilineTextField(pFont, pWidth - this.totalInnerPadding());
      this.textField.setCursorListener(this::scrollToCursor);
   }

   public void setCharacterLimit(int pCharacterLimit) {
      this.textField.setCharacterLimit(pCharacterLimit);
   }

   public void setValueListener(Consumer<String> pValueListener) {
      this.textField.setValueListener(pValueListener);
   }

   public void setValue(String pFullText) {
      this.textField.setValue(pFullText);
   }

   public String getValue() {
      return this.textField.value();
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
      pNarrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.editBox", this.getMessage(), this.getValue()));
   }

   @Override
   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (this.withinContentAreaPoint(pMouseX, pMouseY) && pButton == 0) {
         this.textField.setSelecting(Screen.hasShiftDown());
         this.seekCursorScreen(pMouseX, pMouseY);
         return true;
      } else {
         return super.mouseClicked(pMouseX, pMouseY, pButton);
      }
   }

   @Override
   public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
      if (super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY)) {
         return true;
      } else if (this.withinContentAreaPoint(pMouseX, pMouseY) && pButton == 0) {
         this.textField.setSelecting(true);
         this.seekCursorScreen(pMouseX, pMouseY);
         this.textField.setSelecting(Screen.hasShiftDown());
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      return this.textField.keyPressed(pKeyCode);
   }

   @Override
   public boolean charTyped(char pCodePoint, int pModifiers) {
      if (this.visible && this.isFocused() && StringUtil.isAllowedChatCharacter(pCodePoint)) {
         this.textField.insertText(Character.toString(pCodePoint));
         return true;
      } else {
         return false;
      }
   }

   @Override
   protected void renderContents(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      String $$4 = this.textField.value();
      if ($$4.isEmpty() && !this.isFocused()) {
         pGuiGraphics.drawWordWrap(
            this.font,
            this.placeholder,
            this.getX() + this.innerPadding(),
            this.getY() + this.innerPadding(),
            this.width - this.totalInnerPadding(),
            -857677600
         );
      } else {
         int $$5 = this.textField.cursor();
         boolean $$6 = this.isFocused() && (Util.getMillis() - this.focusedTime) / 300L % 2L == 0L;
         boolean $$7 = $$5 < $$4.length();
         int $$8 = 0;
         int $$9 = 0;
         int $$10 = this.getY() + this.innerPadding();

         for (MultilineTextField.StringView $$11 : this.textField.iterateLines()) {
            boolean $$12 = this.withinContentAreaTopBottom($$10, $$10 + 9);
            if ($$6 && $$7 && $$5 >= $$11.beginIndex() && $$5 <= $$11.endIndex()) {
               if ($$12) {
                  $$8 = pGuiGraphics.drawString(this.font, $$4.substring($$11.beginIndex(), $$5), this.getX() + this.innerPadding(), $$10, -2039584) - 1;
                  pGuiGraphics.fill($$8, $$10 - 1, $$8 + 1, $$10 + 1 + 9, -3092272);
                  pGuiGraphics.drawString(this.font, $$4.substring($$5, $$11.endIndex()), $$8, $$10, -2039584);
               }
            } else {
               if ($$12) {
                  $$8 = pGuiGraphics.drawString(this.font, $$4.substring($$11.beginIndex(), $$11.endIndex()), this.getX() + this.innerPadding(), $$10, -2039584)
                     - 1;
               }

               $$9 = $$10;
            }

            $$10 += 9;
         }

         if ($$6 && !$$7 && this.withinContentAreaTopBottom($$9, $$9 + 9)) {
            pGuiGraphics.drawString(this.font, "_", $$8, $$9, -3092272);
         }

         if (this.textField.hasSelection()) {
            MultilineTextField.StringView $$13 = this.textField.getSelected();
            int $$14 = this.getX() + this.innerPadding();
            $$10 = this.getY() + this.innerPadding();

            for (MultilineTextField.StringView $$15 : this.textField.iterateLines()) {
               if ($$13.beginIndex() > $$15.endIndex()) {
                  $$10 += 9;
               } else {
                  if ($$15.beginIndex() > $$13.endIndex()) {
                     break;
                  }

                  if (this.withinContentAreaTopBottom($$10, $$10 + 9)) {
                     int $$16 = this.font.width($$4.substring($$15.beginIndex(), Math.max($$13.beginIndex(), $$15.beginIndex())));
                     int $$17;
                     if ($$13.endIndex() > $$15.endIndex()) {
                        $$17 = this.width - this.innerPadding();
                     } else {
                        $$17 = this.font.width($$4.substring($$15.beginIndex(), $$13.endIndex()));
                     }

                     this.renderHighlight(pGuiGraphics, $$14 + $$16, $$10, $$14 + $$17, $$10 + 9);
                  }

                  $$10 += 9;
               }
            }
         }
      }
   }

   @Override
   protected void renderDecorations(GuiGraphics pGuiGraphics) {
      super.renderDecorations(pGuiGraphics);
      if (this.textField.hasCharacterLimit()) {
         int $$1 = this.textField.characterLimit();
         Component $$2 = Component.translatable("gui.multiLineEditBox.character_limit", this.textField.value().length(), $$1);
         pGuiGraphics.drawString(this.font, $$2, this.getX() + this.width - this.font.width($$2), this.getY() + this.height + 4, 10526880);
      }
   }

   @Override
   public int getInnerHeight() {
      return 9 * this.textField.getLineCount();
   }

   @Override
   protected boolean scrollbarVisible() {
      return (double)this.textField.getLineCount() > this.getDisplayableLineCount();
   }

   @Override
   protected double scrollRate() {
      return 9.0 / 2.0;
   }

   private void renderHighlight(GuiGraphics pGuiGraphics, int pMinX, int pMinY, int pMaxX, int pMaxY) {
      pGuiGraphics.fill(RenderType.guiTextHighlight(), pMinX, pMinY, pMaxX, pMaxY, -16776961);
   }

   private void scrollToCursor() {
      double $$0 = this.scrollAmount();
      MultilineTextField.StringView $$1 = this.textField.getLineView((int)($$0 / 9.0));
      if (this.textField.cursor() <= $$1.beginIndex()) {
         $$0 = (double)(this.textField.getLineAtCursor() * 9);
      } else {
         MultilineTextField.StringView $$2 = this.textField.getLineView((int)(($$0 + (double)this.height) / 9.0) - 1);
         if (this.textField.cursor() > $$2.endIndex()) {
            $$0 = (double)(this.textField.getLineAtCursor() * 9 - this.height + 9 + this.totalInnerPadding());
         }
      }

      this.setScrollAmount($$0);
   }

   private double getDisplayableLineCount() {
      return (double)(this.height - this.totalInnerPadding()) / 9.0;
   }

   private void seekCursorScreen(double pMouseX, double pMouseY) {
      double $$2 = pMouseX - (double)this.getX() - (double)this.innerPadding();
      double $$3 = pMouseY - (double)this.getY() - (double)this.innerPadding() + this.scrollAmount();
      this.textField.seekCursorToPoint($$2, $$3);
   }

   @Override
   public void setFocused(boolean pFocused) {
      super.setFocused(pFocused);
      if (pFocused) {
         this.focusedTime = Util.getMillis();
      }
   }
}
