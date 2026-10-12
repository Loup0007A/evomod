package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.joml.Vector2ic;

@OnlyIn(Dist.CLIENT)
public class DefaultTooltipPositioner implements ClientTooltipPositioner {
   public static final ClientTooltipPositioner INSTANCE = new DefaultTooltipPositioner();

   private DefaultTooltipPositioner() {
   }

   @Override
   public Vector2ic positionTooltip(int pScreenWidth, int pScreenHeight, int pMouseX, int pMouseY, int pTooltipWidth, int pTooltipHeight) {
      Vector2i $$6 = new Vector2i(pMouseX, pMouseY).add(12, -12);
      this.positionTooltip(pScreenWidth, pScreenHeight, $$6, pTooltipWidth, pTooltipHeight);
      return $$6;
   }

   private void positionTooltip(int pScreenWidth, int pScreenHeight, Vector2i pTooltipPos, int pTooltipWidth, int pTooltipHeight) {
      if (pTooltipPos.x + pTooltipWidth > pScreenWidth) {
         pTooltipPos.x = Math.max(pTooltipPos.x - 24 - pTooltipWidth, 4);
      }

      int $$5 = pTooltipHeight + 3;
      if (pTooltipPos.y + $$5 > pScreenHeight) {
         pTooltipPos.y = pScreenHeight - $$5;
      }
   }
}
