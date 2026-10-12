package net.minecraft.client.gui.components;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EditBox extends AbstractWidget implements Renderable {
   private static final WidgetSprites SPRITES = new WidgetSprites(
      ResourceLocation.withDefaultNamespace("widget/text_field"), ResourceLocation.withDefaultNamespace("widget/text_field_highlighted")
   );
   public static final int BACKWARDS = -1;
   public static final int FORWARDS = 1;
   private static final int CURSOR_INSERT_WIDTH = 1;
   private static final int CURSOR_INSERT_COLOR = -3092272;
   private static final String CURSOR_APPEND_CHARACTER = "_";
   public static final int DEFAULT_TEXT_COLOR = 14737632;
   private static final int CURSOR_BLINK_INTERVAL_MS = 300;
   private final Font font;
   private String value = "";
   private int maxLength = 32;
   private boolean bordered = true;
   private boolean canLoseFocus = true;
   private boolean isEditable = true;
   private int displayPos;
   private int cursorPos;
   private int highlightPos;
   private int textColor = 14737632;
   private int textColorUneditable = 7368816;
   @Nullable
   private String suggestion;
   @Nullable
   private Consumer<String> responder;
   private Predicate<String> filter = Objects::nonNull;
   private BiFunction<String, Integer, FormattedCharSequence> formatter = (p_94147_, p_94148_) -> FormattedCharSequence.forward(p_94147_, Style.EMPTY);
   @Nullable
   private Component hint;
   private long focusedTime = Util.getMillis();

   public EditBox(Font pFont, int pWidth, int pHeight, Component pMessage) {
      this(pFont, 0, 0, pWidth, pHeight, pMessage);
   }

   public EditBox(Font pFont, int pX, int pY, int pWidth, int pHeight, Component pMessage) {
      this(pFont, pX, pY, pWidth, pHeight, null, pMessage);
   }

   public EditBox(Font pFont, int pX, int pY, int pWidth, int pHeight, @Nullable EditBox pEditBox, Component pMessage) {
      super(pX, pY, pWidth, pHeight, pMessage);
      this.font = pFont;
      if (pEditBox != null) {
         this.setValue(pEditBox.getValue());
      }
   }

   public void setResponder(Consumer<String> pResponder) {
      this.responder = pResponder;
   }

   public void setFormatter(BiFunction<String, Integer, FormattedCharSequence> pTextFormatter) {
      this.formatter = pTextFormatter;
   }

   @Override
   protected MutableComponent createNarrationMessage() {
      Component $$0 = this.getMessage();
      return Component.translatable("gui.narrate.editBox", $$0, this.value);
   }

   public void setValue(String pText) {
      if (this.filter.test(pText)) {
         if (pText.length() > this.maxLength) {
            this.value = pText.substring(0, this.maxLength);
         } else {
            this.value = pText;
         }

         this.moveCursorToEnd(false);
         this.setHighlightPos(this.cursorPos);
         this.onValueChange(pText);
      }
   }

   public String getValue() {
      return this.value;
   }

   public String getHighlighted() {
      int $$0 = Math.min(this.cursorPos, this.highlightPos);
      int $$1 = Math.max(this.cursorPos, this.highlightPos);
      return this.value.substring($$0, $$1);
   }

   public void setFilter(Predicate<String> pValidator) {
      this.filter = pValidator;
   }

   public void insertText(String pTextToWrite) {
      int $$1 = Math.min(this.cursorPos, this.highlightPos);
      int $$2 = Math.max(this.cursorPos, this.highlightPos);
      int $$3 = this.maxLength - this.value.length() - ($$1 - $$2);
      if ($$3 > 0) {
         String $$4 = StringUtil.filterText(pTextToWrite);
         int $$5 = $$4.length();
         if ($$3 < $$5) {
            if (Character.isHighSurrogate($$4.charAt($$3 - 1))) {
               $$3--;
            }

            $$4 = $$4.substring(0, $$3);
            $$5 = $$3;
         }

         String $$6 = new StringBuilder(this.value).replace($$1, $$2, $$4).toString();
         if (this.filter.test($$6)) {
            this.value = $$6;
            this.setCursorPosition($$1 + $$5);
            this.setHighlightPos(this.cursorPos);
            this.onValueChange(this.value);
         }
      }
   }

   private void onValueChange(String pNewText) {
      if (this.responder != null) {
         this.responder.accept(pNewText);
      }
   }

   private void deleteText(int pCount) {
      if (Screen.hasControlDown()) {
         this.deleteWords(pCount);
      } else {
         this.deleteChars(pCount);
      }
   }

   public void deleteWords(int pNum) {
      if (!this.value.isEmpty()) {
         if (this.highlightPos != this.cursorPos) {
            this.insertText("");
         } else {
            this.deleteCharsToPos(this.getWordPosition(pNum));
         }
      }
   }

   public void deleteChars(int pNum) {
      this.deleteCharsToPos(this.getCursorPos(pNum));
   }

   public void deleteCharsToPos(int pNum) {
      if (!this.value.isEmpty()) {
         if (this.highlightPos != this.cursorPos) {
            this.insertText("");
         } else {
            int $$1 = Math.min(pNum, this.cursorPos);
            int $$2 = Math.max(pNum, this.cursorPos);
            if ($$1 != $$2) {
               String $$3 = new StringBuilder(this.value).delete($$1, $$2).toString();
               if (this.filter.test($$3)) {
                  this.value = $$3;
                  this.moveCursorTo($$1, false);
               }
            }
         }
      }
   }

   public int getWordPosition(int pNumWords) {
      return this.getWordPosition(pNumWords, this.getCursorPosition());
   }

   private int getWordPosition(int pNumWords, int pPos) {
      return this.getWordPosition(pNumWords, pPos, true);
   }

   private int getWordPosition(int pNumWords, int pPos, boolean pSkipConsecutiveSpaces) {
      int $$3 = pPos;
      boolean $$4 = pNumWords < 0;
      int $$5 = Math.abs(pNumWords);

      for (int $$6 = 0; $$6 < $$5; $$6++) {
         if (!$$4) {
            int $$7 = this.value.length();
            $$3 = this.value.indexOf(32, $$3);
            if ($$3 == -1) {
               $$3 = $$7;
            } else {
               while (pSkipConsecutiveSpaces && $$3 < $$7 && this.value.charAt($$3) == ' ') {
                  $$3++;
               }
            }
         } else {
            while (pSkipConsecutiveSpaces && $$3 > 0 && this.value.charAt($$3 - 1) == ' ') {
               $$3--;
            }

            while ($$3 > 0 && this.value.charAt($$3 - 1) != ' ') {
               $$3--;
            }
         }
      }

      return $$3;
   }

   public void moveCursor(int pDelta, boolean pSelect) {
      this.moveCursorTo(this.getCursorPos(pDelta), pSelect);
   }

   private int getCursorPos(int pDelta) {
      return Util.offsetByCodepoints(this.value, this.cursorPos, pDelta);
   }

   public void moveCursorTo(int pDelta, boolean pSelect) {
      this.setCursorPosition(pDelta);
      if (!pSelect) {
         this.setHighlightPos(this.cursorPos);
      }

      this.onValueChange(this.value);
   }

   public void setCursorPosition(int pPos) {
      this.cursorPos = Mth.clamp(pPos, 0, this.value.length());
      this.scrollTo(this.cursorPos);
   }

   public void moveCursorToStart(boolean pSelect) {
      this.moveCursorTo(0, pSelect);
   }

   public void moveCursorToEnd(boolean pSelect) {
      this.moveCursorTo(this.value.length(), pSelect);
   }

   @Override
   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (this.isActive() && this.isFocused()) {
         switch (pKeyCode) {
            case 259:
               if (this.isEditable) {
                  this.deleteText(-1);
               }

               return true;
            case 260:
            case 264:
            case 265:
            case 266:
            case 267:
            default:
               if (Screen.isSelectAll(pKeyCode)) {
                  this.moveCursorToEnd(false);
                  this.setHighlightPos(0);
                  return true;
               } else if (Screen.isCopy(pKeyCode)) {
                  Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
                  return true;
               } else if (Screen.isPaste(pKeyCode)) {
                  if (this.isEditable()) {
                     this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
                  }

                  return true;
               } else {
                  if (Screen.isCut(pKeyCode)) {
                     Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
                     if (this.isEditable()) {
                        this.insertText("");
                     }

                     return true;
                  }

                  return false;
               }
            case 261:
               if (this.isEditable) {
                  this.deleteText(1);
               }

               return true;
            case 262:
               if (Screen.hasControlDown()) {
                  this.moveCursorTo(this.getWordPosition(1), Screen.hasShiftDown());
               } else {
                  this.moveCursor(1, Screen.hasShiftDown());
               }

               return true;
            case 263:
               if (Screen.hasControlDown()) {
                  this.moveCursorTo(this.getWordPosition(-1), Screen.hasShiftDown());
               } else {
                  this.moveCursor(-1, Screen.hasShiftDown());
               }

               return true;
            case 268:
               this.moveCursorToStart(Screen.hasShiftDown());
               return true;
            case 269:
               this.moveCursorToEnd(Screen.hasShiftDown());
               return true;
         }
      } else {
         return false;
      }
   }

   public boolean canConsumeInput() {
      return this.isActive() && this.isFocused() && this.isEditable();
   }

   @Override
   public boolean charTyped(char pCodePoint, int pModifiers) {
      if (!this.canConsumeInput()) {
         return false;
      } else if (StringUtil.isAllowedChatCharacter(pCodePoint)) {
         if (this.isEditable) {
            this.insertText(Character.toString(pCodePoint));
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public void onClick(double pMouseX, double pMouseY) {
      int $$2 = Mth.floor(pMouseX) - this.getX();
      if (this.bordered) {
         $$2 -= 4;
      }

      String $$3 = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
      this.moveCursorTo(this.font.plainSubstrByWidth($$3, $$2).length() + this.displayPos, Screen.hasShiftDown());
   }

   @Override
   public void playDownSound(SoundManager pHandler) {
   }

   @Override
   public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      if (this.isVisible()) {
         if (this.isBordered()) {
            ResourceLocation $$4 = SPRITES.get(this.isActive(), this.isFocused());
            pGuiGraphics.blitSprite($$4, this.getX(), this.getY(), this.getWidth(), this.getHeight());
         }

         int $$5 = this.isEditable ? this.textColor : this.textColorUneditable;
         int $$6 = this.cursorPos - this.displayPos;
         String $$7 = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
         boolean $$8 = $$6 >= 0 && $$6 <= $$7.length();
         boolean $$9 = this.isFocused() && (Util.getMillis() - this.focusedTime) / 300L % 2L == 0L && $$8;
         int $$10 = this.bordered ? this.getX() + 4 : this.getX();
         int $$11 = this.bordered ? this.getY() + (this.height - 8) / 2 : this.getY();
         int $$12 = $$10;
         int $$13 = Mth.clamp(this.highlightPos - this.displayPos, 0, $$7.length());
         if (!$$7.isEmpty()) {
            String $$14 = $$8 ? $$7.substring(0, $$6) : $$7;
            $$12 = pGuiGraphics.drawString(this.font, this.formatter.apply($$14, this.displayPos), $$10, $$11, $$5);
         }

         boolean $$15 = this.cursorPos < this.value.length() || this.value.length() >= this.getMaxLength();
         int $$16 = $$12;
         if (!$$8) {
            $$16 = $$6 > 0 ? $$10 + this.width : $$10;
         } else if ($$15) {
            $$16 = $$12 - 1;
            $$12--;
         }

         if (!$$7.isEmpty() && $$8 && $$6 < $$7.length()) {
            pGuiGraphics.drawString(this.font, this.formatter.apply($$7.substring($$6), this.cursorPos), $$12, $$11, $$5);
         }

         if (this.hint != null && $$7.isEmpty() && !this.isFocused()) {
            pGuiGraphics.drawString(this.font, this.hint, $$12, $$11, $$5);
         }

         if (!$$15 && this.suggestion != null) {
            pGuiGraphics.drawString(this.font, this.suggestion, $$16 - 1, $$11, -8355712);
         }

         if ($$9) {
            if ($$15) {
               pGuiGraphics.fill(RenderType.guiOverlay(), $$16, $$11 - 1, $$16 + 1, $$11 + 1 + 9, -3092272);
            } else {
               pGuiGraphics.drawString(this.font, "_", $$16, $$11, $$5);
            }
         }

         if ($$13 != $$6) {
            int $$17 = $$10 + this.font.width($$7.substring(0, $$13));
            this.renderHighlight(pGuiGraphics, $$16, $$11 - 1, $$17 - 1, $$11 + 1 + 9);
         }
      }
   }

   private void renderHighlight(GuiGraphics pGuiGraphics, int pMinX, int pMinY, int pMaxX, int pMaxY) {
      if (pMinX < pMaxX) {
         int $$5 = pMinX;
         pMinX = pMaxX;
         pMaxX = $$5;
      }

      if (pMinY < pMaxY) {
         int $$6 = pMinY;
         pMinY = pMaxY;
         pMaxY = $$6;
      }

      if (pMaxX > this.getX() + this.width) {
         pMaxX = this.getX() + this.width;
      }

      if (pMinX > this.getX() + this.width) {
         pMinX = this.getX() + this.width;
      }

      pGuiGraphics.fill(RenderType.guiTextHighlight(), pMinX, pMinY, pMaxX, pMaxY, -16776961);
   }

   public void setMaxLength(int pLength) {
      this.maxLength = pLength;
      if (this.value.length() > pLength) {
         this.value = this.value.substring(0, pLength);
         this.onValueChange(this.value);
      }
   }

   private int getMaxLength() {
      return this.maxLength;
   }

   public int getCursorPosition() {
      return this.cursorPos;
   }

   public boolean isBordered() {
      return this.bordered;
   }

   public void setBordered(boolean pEnableBackgroundDrawing) {
      this.bordered = pEnableBackgroundDrawing;
   }

   public void setTextColor(int pColor) {
      this.textColor = pColor;
   }

   public void setTextColorUneditable(int pColor) {
      this.textColorUneditable = pColor;
   }

   @Override
   public void setFocused(boolean pFocused) {
      if (this.canLoseFocus || pFocused) {
         super.setFocused(pFocused);
         if (pFocused) {
            this.focusedTime = Util.getMillis();
         }
      }
   }

   private boolean isEditable() {
      return this.isEditable;
   }

   public void setEditable(boolean pEnabled) {
      this.isEditable = pEnabled;
   }

   public int getInnerWidth() {
      return this.isBordered() ? this.width - 8 : this.width;
   }

   public void setHighlightPos(int pPosition) {
      this.highlightPos = Mth.clamp(pPosition, 0, this.value.length());
      this.scrollTo(this.highlightPos);
   }

   private void scrollTo(int pPosition) {
      if (this.font != null) {
         this.displayPos = Math.min(this.displayPos, this.value.length());
         int $$1 = this.getInnerWidth();
         String $$2 = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), $$1);
         int $$3 = $$2.length() + this.displayPos;
         if (pPosition == this.displayPos) {
            this.displayPos = this.displayPos - this.font.plainSubstrByWidth(this.value, $$1, true).length();
         }

         if (pPosition > $$3) {
            this.displayPos += pPosition - $$3;
         } else if (pPosition <= this.displayPos) {
            this.displayPos = this.displayPos - (this.displayPos - pPosition);
         }

         this.displayPos = Mth.clamp(this.displayPos, 0, this.value.length());
      }
   }

   public void setCanLoseFocus(boolean pCanLoseFocus) {
      this.canLoseFocus = pCanLoseFocus;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public void setVisible(boolean pIsVisible) {
      this.visible = pIsVisible;
   }

   public void setSuggestion(@Nullable String pSuggestion) {
      this.suggestion = pSuggestion;
   }

   public int getScreenX(int pCharNum) {
      return pCharNum > this.value.length() ? this.getX() : this.getX() + this.font.width(this.value.substring(0, pCharNum));
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
      pNarrationElementOutput.add(NarratedElementType.TITLE, this.createNarrationMessage());
   }

   public void setHint(Component pHint) {
      this.hint = pHint;
   }
}
