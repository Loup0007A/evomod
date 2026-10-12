package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.joml.Vector2ic;

@OnlyIn(Dist.CLIENT)
public class MenuTooltipPositioner implements ClientTooltipPositioner {
   private static final int MARGIN = 5;
   private static final int MOUSE_OFFSET_X = 12;
   public static final int MAX_OVERLAP_WITH_WIDGET = 3;
   public static final int MAX_DISTANCE_TO_WIDGET = 5;
   private final ScreenRectangle screenRectangle;

   public MenuTooltipPositioner(ScreenRectangle pScreenRectangle) {
      this.screenRectangle = pScreenRectangle;
   }

   @Override
   public Vector2ic positionTooltip(int pScreenWidth, int pScreenHeight, int pMouseX, int pMouseY, int pTooltipWidth, int pTooltipHeight) {
      Vector2i $$6 = new Vector2i(pMouseX + 12, pMouseY);
      if ($$6.x + pTooltipWidth > pScreenWidth - 5) {
         $$6.x = Math.max(pMouseX - 12 - pTooltipWidth, 9);
      }

      $$6.y += 3;
      int $$7 = pTooltipHeight + 3 + 3;
      int $$8 = this.screenRectangle.bottom() + 3 + getOffset(0, 0, this.screenRectangle.height());
      int $$9 = pScreenHeight - 5;
      if ($$8 + $$7 <= $$9) {
         $$6.y = $$6.y + getOffset($$6.y, this.screenRectangle.top(), this.screenRectangle.height());
      } else {
         $$6.y = $$6.y - ($$7 + getOffset($$6.y, this.screenRectangle.bottom(), this.screenRectangle.height()));
      }

      return $$6;
   }

   private static int getOffset(int pMouseY, int pWidgetY, int pWidgetHeight) {
      int $$3 = Math.min(Math.abs(pMouseY - pWidgetY), pWidgetHeight);
      return Math.round(Mth.lerp((float)$$3 / (float)pWidgetHeight, (float)(pWidgetHeight - 3), 5.0F));
   }
}
