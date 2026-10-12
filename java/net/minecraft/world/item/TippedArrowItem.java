package net.minecraft.world.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public class TippedArrowItem extends ArrowItem {
   public TippedArrowItem(Item.Properties pProperties) {
      super(pProperties);
   }

   @Override
   public ItemStack getDefaultInstance() {
      ItemStack $$0 = super.getDefaultInstance();
      $$0.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.POISON));
      return $$0;
   }

   @Override
   public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
      PotionContents $$4 = pStack.get(DataComponents.POTION_CONTENTS);
      if ($$4 != null) {
         $$4.addPotionTooltip(pTooltipComponents::add, 0.125F, pContext.tickRate());
      }
   }

   @Override
   public String getDescriptionId(ItemStack pStack) {
      return Potion.getName(pStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion(), this.getDescriptionId() + ".effect.");
   }
}
