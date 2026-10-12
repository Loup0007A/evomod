package com.mojang.realmsclient.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class RowButton {
   public final int width;
   public final int height;
   public final int xOffset;
   public final int yOffset;

   public RowButton(int pWidth, int pHeight, int pXOffset, int pYOffset) {
      this.width = pWidth;
      this.height = pHeight;
      this.xOffset = pXOffset;
      this.yOffset = pYOffset;
   }

   public void drawForRowAt(GuiGraphics pGuiGraphics, int pX, int pY, int pMouseX, int pMouseY) {
      int $$5 = pX + this.xOffset;
      int $$6 = pY + this.yOffset;
      boolean $$7 = pMouseX >= $$5 && pMouseX <= $$5 + this.width && pMouseY >= $$6 && pMouseY <= $$6 + this.height;
      this.draw(pGuiGraphics, $$5, $$6, $$7);
   }

   protected abstract void draw(GuiGraphics pGuiGraphics, int pX, int pY, boolean pShowTooltip);

   public int getRight() {
      return this.xOffset + this.width;
   }

   public int getBottom() {
      return this.yOffset + this.height;
   }

   public abstract void onClick(int pIndex);

   public static void drawButtonsInRow(
      GuiGraphics pGuiGraphics, List<RowButton> pButtons, RealmsObjectSelectionList<?> pPendingInvitations, int pX, int pY, int pMouseX, int pMouseY
   ) {
      for (RowButton $$7 : pButtons) {
         if (pPendingInvitations.getRowWidth() > $$7.getRight()) {
            $$7.drawForRowAt(pGuiGraphics, pX, pY, pMouseX, pMouseY);
         }
      }
   }

   public static void rowButtonMouseClicked(
      RealmsObjectSelectionList<?> pList, ObjectSelectionList.Entry<?> pEntry, List<RowButton> pButtons, int pButton, double pMouseX, double pMouseY
   ) {
      int $$6 = pList.children().indexOf(pEntry);
      if ($$6 > -1) {
         pList.selectItem($$6);
         int $$7 = pList.getRowLeft();
         int $$8 = pList.getRowTop($$6);
         int $$9 = (int)(pMouseX - (double)$$7);
         int $$10 = (int)(pMouseY - (double)$$8);

         for (RowButton $$11 : pButtons) {
            if ($$9 >= $$11.xOffset && $$9 <= $$11.getRight() && $$10 >= $$11.yOffset && $$10 <= $$11.getBottom()) {
               $$11.onClick($$6);
            }
         }
      }
   }
}
