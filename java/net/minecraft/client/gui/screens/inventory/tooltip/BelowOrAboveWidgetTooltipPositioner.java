package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.joml.Vector2ic;

@OnlyIn(Dist.CLIENT)
public class BelowOrAboveWidgetTooltipPositioner implements ClientTooltipPositioner {
   private final ScreenRectangle screenRectangle;

   public BelowOrAboveWidgetTooltipPositioner(ScreenRectangle pScreenRectangle) {
      this.screenRectangle = pScreenRectangle;
   }

   @Override
   public Vector2ic positionTooltip(int pScreenWidth, int pScreenHeight, int pMouseX, int pMouseY, int pTooltipWidth, int pTooltipHeight) {
      Vector2i $$6 = new Vector2i();
      $$6.x = this.screenRectangle.left() + 3;
      $$6.y = this.screenRectangle.bottom() + 3 + 1;
      if ($$6.y + pTooltipHeight + 3 > pScreenHeight) {
         $$6.y = this.screenRectangle.top() - pTooltipHeight - 3 - 1;
      }

      if ($$6.x + pTooltipWidth > pScreenWidth) {
         $$6.x = Math.max(this.screenRectangle.right() - pTooltipWidth - 3, 4);
      }

      return $$6;
   }
}
