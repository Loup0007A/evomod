package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public interface Equipable {
   EquipmentSlot getEquipmentSlot();

   default Holder<SoundEvent> getEquipSound() {
      return SoundEvents.ARMOR_EQUIP_GENERIC;
   }

   default InteractionResultHolder<ItemStack> swapWithEquipmentSlot(Item pItem, Level pLevel, Player pPlayer, InteractionHand pHand) {
      ItemStack $$4 = pPlayer.getItemInHand(pHand);
      EquipmentSlot $$5 = pPlayer.getEquipmentSlotForItem($$4);
      if (!pPlayer.canUseSlot($$5)) {
         return InteractionResultHolder.pass($$4);
      } else {
         ItemStack $$6 = pPlayer.getItemBySlot($$5);
         if ((!EnchantmentHelper.has($$6, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) || pPlayer.isCreative()) && !ItemStack.matches($$4, $$6)) {
            if (!pLevel.isClientSide()) {
               pPlayer.awardStat(Stats.ITEM_USED.get(pItem));
            }

            ItemStack $$7 = $$6.isEmpty() ? $$4 : $$6.copyAndClear();
            ItemStack $$8 = pPlayer.isCreative() ? $$4.copy() : $$4.copyAndClear();
            pPlayer.setItemSlot($$5, $$8);
            return InteractionResultHolder.sidedSuccess($$7, pLevel.isClientSide());
         } else {
            return InteractionResultHolder.fail($$4);
         }
      }
   }

   @Nullable
   static Equipable get(ItemStack pStack) {
      Item $$3 = pStack.getItem();
      if ($$3 instanceof Equipable) {
         return (Equipable)$$3;
      } else {
         if (pStack.getItem() instanceof BlockItem $$2) {
            Block var6 = $$2.getBlock();
            if (var6 instanceof Equipable) {
               return (Equipable)var6;
            }
         }

         return null;
      }
   }
}
