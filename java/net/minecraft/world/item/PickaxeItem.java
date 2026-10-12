package net.minecraft.world.item;

import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

public class PickaxeItem extends DiggerItem {
   public PickaxeItem(Tier p_42961_, Item.Properties p_42964_) {
      super(p_42961_, BlockTags.MINEABLE_WITH_PICKAXE, p_42964_);
   }

   @Override
   public boolean canPerformAction(ItemStack stack, ToolAction toolAction) {
      return ToolActions.DEFAULT_PICKAXE_ACTIONS.contains(toolAction);
   }
}
