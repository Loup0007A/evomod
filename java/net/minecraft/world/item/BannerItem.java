package net.minecraft.world.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.apache.commons.lang3.Validate;

public class BannerItem extends StandingAndWallBlockItem {
   public BannerItem(Block pBlock, Block pWallBlock, Item.Properties pProperties) {
      super(pBlock, pWallBlock, pProperties, Direction.DOWN);
      Validate.isInstanceOf(AbstractBannerBlock.class, pBlock);
      Validate.isInstanceOf(AbstractBannerBlock.class, pWallBlock);
   }

   public static void appendHoverTextFromBannerBlockEntityTag(ItemStack pStack, List<Component> pTooltipComponents) {
      BannerPatternLayers $$2 = pStack.get(DataComponents.BANNER_PATTERNS);
      if ($$2 != null) {
         for (int $$3 = 0; $$3 < Math.min($$2.layers().size(), 6); $$3++) {
            BannerPatternLayers.Layer $$4 = $$2.layers().get($$3);
            pTooltipComponents.add($$4.description().withStyle(ChatFormatting.GRAY));
         }
      }
   }

   public DyeColor getColor() {
      return ((AbstractBannerBlock)this.getBlock()).getColor();
   }

   @Override
   public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
      appendHoverTextFromBannerBlockEntityTag(pStack, pTooltipComponents);
   }
}
