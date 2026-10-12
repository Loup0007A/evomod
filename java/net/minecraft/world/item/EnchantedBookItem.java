package net.minecraft.world.item;

import net.minecraft.world.item.enchantment.EnchantmentInstance;

public class EnchantedBookItem extends Item {
   public EnchantedBookItem(Item.Properties pProperties) {
      super(pProperties);
   }

   @Override
   public boolean isEnchantable(ItemStack pStack) {
      return false;
   }

   public static ItemStack createForEnchantment(EnchantmentInstance pInstance) {
      ItemStack $$1 = new ItemStack(Items.ENCHANTED_BOOK);
      $$1.enchant(pInstance.enchantment, pInstance.level);
      return $$1;
   }
}
