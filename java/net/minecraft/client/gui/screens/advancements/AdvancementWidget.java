package net.minecraft.client.gui.screens.advancements;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AdvancementWidget {
   private static final ResourceLocation TITLE_BOX_SPRITE = ResourceLocation.withDefaultNamespace("advancements/title_box");
   private static final int HEIGHT = 26;
   private static final int BOX_X = 0;
   private static final int BOX_WIDTH = 200;
   private static final int FRAME_WIDTH = 26;
   private static final int ICON_X = 8;
   private static final int ICON_Y = 5;
   private static final int ICON_WIDTH = 26;
   private static final int TITLE_PADDING_LEFT = 3;
   private static final int TITLE_PADDING_RIGHT = 5;
   private static final int TITLE_X = 32;
   private static final int TITLE_Y = 9;
   private static final int TITLE_MAX_WIDTH = 163;
   private static final int[] TEST_SPLIT_OFFSETS = new int[]{0, 10, -10, 25, -25};
   private final AdvancementTab tab;
   private final AdvancementNode advancementNode;
   private final DisplayInfo display;
   private final FormattedCharSequence title;
   private final int width;
   private final List<FormattedCharSequence> description;
   private final Minecraft minecraft;
   @Nullable
   private AdvancementWidget parent;
   private final List<AdvancementWidget> children = Lists.newArrayList();
   @Nullable
   private AdvancementProgress progress;
   private final int x;
   private final int y;

   public AdvancementWidget(AdvancementTab pTab, Minecraft pMinecraft, AdvancementNode pAdvancementNode, DisplayInfo pDisplay) {
      this.tab = pTab;
      this.advancementNode = pAdvancementNode;
      this.display = pDisplay;
      this.minecraft = pMinecraft;
      this.title = Language.getInstance().getVisualOrder(pMinecraft.font.substrByWidth(pDisplay.getTitle(), 163));
      this.x = Mth.floor(pDisplay.getX() * 28.0F);
      this.y = Mth.floor(pDisplay.getY() * 27.0F);
      int $$4 = this.getMaxProgressWidth();
      int $$5 = 29 + pMinecraft.font.width(this.title) + $$4;
      this.description = Language.getInstance()
         .getVisualOrder(
            this.findOptimalLines(ComponentUtils.mergeStyles(pDisplay.getDescription().copy(), Style.EMPTY.withColor(pDisplay.getType().getChatColor())), $$5)
         );

      for (FormattedCharSequence $$6 : this.description) {
         $$5 = Math.max($$5, pMinecraft.font.width($$6));
      }

      this.width = $$5 + 3 + 5;
   }

   private int getMaxProgressWidth() {
      int $$0 = this.advancementNode.advancement().requirements().size();
      if ($$0 <= 1) {
         return 0;
      } else {
         int $$1 = 8;
         Component $$2 = Component.translatable("advancements.progress", $$0, $$0);
         return this.minecraft.font.width($$2) + 8;
      }
   }

   private static float getMaxWidth(StringSplitter pManager, List<FormattedText> pText) {
      return (float)pText.stream().mapToDouble(pManager::stringWidth).max().orElse(0.0);
   }

   private List<FormattedText> findOptimalLines(Component pComponent, int pMaxWidth) {
      StringSplitter $$2 = this.minecraft.font.getSplitter();
      List<FormattedText> $$3 = null;
      float $$4 = Float.MAX_VALUE;

      for (int $$5 : TEST_SPLIT_OFFSETS) {
         List<FormattedText> $$6 = $$2.splitLines(pComponent, pMaxWidth - $$5, Style.EMPTY);
         float $$7 = Math.abs(getMaxWidth($$2, $$6) - (float)pMaxWidth);
         if ($$7 <= 10.0F) {
            return $$6;
         }

         if ($$7 < $$4) {
            $$4 = $$7;
            $$3 = $$6;
         }
      }

      return $$3;
   }

   @Nullable
   private AdvancementWidget getFirstVisibleParent(AdvancementNode pAdvancement) {
      do {
         pAdvancement = pAdvancement.parent();
      } while (pAdvancement != null && pAdvancement.advancement().display().isEmpty());

      return pAdvancement != null && !pAdvancement.advancement().display().isEmpty() ? this.tab.getWidget(pAdvancement.holder()) : null;
   }

   public void drawConnectivity(GuiGraphics pGuiGraphics, int pX, int pY, boolean pDropShadow) {
      if (this.parent != null) {
         int $$4 = pX + this.parent.x + 13;
         int $$5 = pX + this.parent.x + 26 + 4;
         int $$6 = pY + this.parent.y + 13;
         int $$7 = pX + this.x + 13;
         int $$8 = pY + this.y + 13;
         int $$9 = pDropShadow ? -16777216 : -1;
         if (pDropShadow) {
            pGuiGraphics.hLine($$5, $$4, $$6 - 1, $$9);
            pGuiGraphics.hLine($$5 + 1, $$4, $$6, $$9);
            pGuiGraphics.hLine($$5, $$4, $$6 + 1, $$9);
            pGuiGraphics.hLine($$7, $$5 - 1, $$8 - 1, $$9);
            pGuiGraphics.hLine($$7, $$5 - 1, $$8, $$9);
            pGuiGraphics.hLine($$7, $$5 - 1, $$8 + 1, $$9);
            pGuiGraphics.vLine($$5 - 1, $$8, $$6, $$9);
            pGuiGraphics.vLine($$5 + 1, $$8, $$6, $$9);
         } else {
            pGuiGraphics.hLine($$5, $$4, $$6, $$9);
            pGuiGraphics.hLine($$7, $$5, $$8, $$9);
            pGuiGraphics.vLine($$5, $$8, $$6, $$9);
         }
      }

      for (AdvancementWidget $$10 : this.children) {
         $$10.drawConnectivity(pGuiGraphics, pX, pY, pDropShadow);
      }
   }

   public void draw(GuiGraphics pGuiGraphics, int pX, int pY) {
      if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
         float $$3 = this.progress == null ? 0.0F : this.progress.getPercent();
         AdvancementWidgetType $$4;
         if ($$3 >= 1.0F) {
            $$4 = AdvancementWidgetType.OBTAINED;
         } else {
            $$4 = AdvancementWidgetType.UNOBTAINED;
         }

         pGuiGraphics.blitSprite($$4.frameSprite(this.display.getType()), pX + this.x + 3, pY + this.y, 26, 26);
         pGuiGraphics.renderFakeItem(this.display.getIcon(), pX + this.x + 8, pY + this.y + 5);
      }

      for (AdvancementWidget $$6 : this.children) {
         $$6.draw(pGuiGraphics, pX, pY);
      }
   }

   public int getWidth() {
      return this.width;
   }

   public void setProgress(AdvancementProgress pProgress) {
      this.progress = pProgress;
   }

   public void addChild(AdvancementWidget pAdvancementWidget) {
      this.children.add(pAdvancementWidget);
   }

   public void drawHover(GuiGraphics pGuiGraphics, int pX, int pY, float pFade, int pWidth, int pHeight) {
      boolean $$6 = pWidth + pX + this.x + this.width + 26 >= this.tab.getScreen().width;
      Component $$7 = this.progress == null ? null : this.progress.getProgressText();
      int $$8 = $$7 == null ? 0 : this.minecraft.font.width($$7);
      boolean $$9 = 113 - pY - this.y - 26 <= 6 + this.description.size() * 9;
      float $$10 = this.progress == null ? 0.0F : this.progress.getPercent();
      int $$11 = Mth.floor($$10 * (float)this.width);
      AdvancementWidgetType $$12;
      AdvancementWidgetType $$13;
      AdvancementWidgetType $$14;
      if ($$10 >= 1.0F) {
         $$11 = this.width / 2;
         $$12 = AdvancementWidgetType.OBTAINED;
         $$13 = AdvancementWidgetType.OBTAINED;
         $$14 = AdvancementWidgetType.OBTAINED;
      } else if ($$11 < 2) {
         $$11 = this.width / 2;
         $$12 = AdvancementWidgetType.UNOBTAINED;
         $$13 = AdvancementWidgetType.UNOBTAINED;
         $$14 = AdvancementWidgetType.UNOBTAINED;
      } else if ($$11 > this.width - 2) {
         $$11 = this.width / 2;
         $$12 = AdvancementWidgetType.OBTAINED;
         $$13 = AdvancementWidgetType.OBTAINED;
         $$14 = AdvancementWidgetType.UNOBTAINED;
      } else {
         $$12 = AdvancementWidgetType.OBTAINED;
         $$13 = AdvancementWidgetType.UNOBTAINED;
         $$14 = AdvancementWidgetType.UNOBTAINED;
      }

      int $$24 = this.width - $$11;
      RenderSystem.enableBlend();
      int $$25 = pY + this.y;
      int $$26;
      if ($$6) {
         $$26 = pX + this.x - this.width + 26 + 6;
      } else {
         $$26 = pX + this.x;
      }

      int $$28 = 32 + this.description.size() * 9;
      if (!this.description.isEmpty()) {
         if ($$9) {
            pGuiGraphics.blitSprite(TITLE_BOX_SPRITE, $$26, $$25 + 26 - $$28, this.width, $$28);
         } else {
            pGuiGraphics.blitSprite(TITLE_BOX_SPRITE, $$26, $$25, this.width, $$28);
         }
      }

      pGuiGraphics.blitSprite($$12.boxSprite(), 200, 26, 0, 0, $$26, $$25, $$11, 26);
      pGuiGraphics.blitSprite($$13.boxSprite(), 200, 26, 200 - $$24, 0, $$26 + $$11, $$25, $$24, 26);
      pGuiGraphics.blitSprite($$14.frameSprite(this.display.getType()), pX + this.x + 3, pY + this.y, 26, 26);
      if ($$6) {
         pGuiGraphics.drawString(this.minecraft.font, this.title, $$26 + 5, pY + this.y + 9, -1);
         if ($$7 != null) {
            pGuiGraphics.drawString(this.minecraft.font, $$7, pX + this.x - $$8, pY + this.y + 9, -1);
         }
      } else {
         pGuiGraphics.drawString(this.minecraft.font, this.title, pX + this.x + 32, pY + this.y + 9, -1);
         if ($$7 != null) {
            pGuiGraphics.drawString(this.minecraft.font, $$7, pX + this.x + this.width - $$8 - 5, pY + this.y + 9, -1);
         }
      }

      if ($$9) {
         for (int $$29 = 0; $$29 < this.description.size(); $$29++) {
            pGuiGraphics.drawString(this.minecraft.font, this.description.get($$29), $$26 + 5, $$25 + 26 - $$28 + 7 + $$29 * 9, -5592406, false);
         }
      } else {
         for (int $$30 = 0; $$30 < this.description.size(); $$30++) {
            pGuiGraphics.drawString(this.minecraft.font, this.description.get($$30), $$26 + 5, pY + this.y + 9 + 17 + $$30 * 9, -5592406, false);
         }
      }

      pGuiGraphics.renderFakeItem(this.display.getIcon(), pX + this.x + 8, pY + this.y + 5);
   }

   public boolean isMouseOver(int pX, int pY, int pMouseX, int pMouseY) {
      if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
         int $$4 = pX + this.x;
         int $$5 = $$4 + 26;
         int $$6 = pY + this.y;
         int $$7 = $$6 + 26;
         return pMouseX >= $$4 && pMouseX <= $$5 && pMouseY >= $$6 && pMouseY <= $$7;
      } else {
         return false;
      }
   }

   public void attachToParent() {
      if (this.parent == null && this.advancementNode.parent() != null) {
         this.parent = this.getFirstVisibleParent(this.advancementNode);
         if (this.parent != null) {
            this.parent.addChild(this);
         }
      }
   }

   public int getY() {
      return this.y;
   }

   public int getX() {
      return this.x;
   }
}
