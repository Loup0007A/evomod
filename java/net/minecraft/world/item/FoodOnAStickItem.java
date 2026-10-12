package net.minecraft.world.item;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class FoodOnAStickItem<T extends Entity & ItemSteerable> extends Item {
   private final EntityType<T> canInteractWith;
   private final int consumeItemDamage;

   public FoodOnAStickItem(Item.Properties pProperties, EntityType<T> pCanInteractWith, int pConsumeItemDamage) {
      super(pProperties);
      this.canInteractWith = pCanInteractWith;
      this.consumeItemDamage = pConsumeItemDamage;
   }

   @Override
   public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
      ItemStack $$3 = pPlayer.getItemInHand(pHand);
      if (pLevel.isClientSide) {
         return InteractionResultHolder.pass($$3);
      } else {
         Entity $$4 = pPlayer.getControlledVehicle();
         if (pPlayer.isPassenger() && $$4 instanceof ItemSteerable $$5 && $$4.getType() == this.canInteractWith && $$5.boost()) {
            EquipmentSlot $$6 = LivingEntity.getSlotForHand(pHand);
            ItemStack $$7 = $$3.hurtAndConvertOnBreak(this.consumeItemDamage, Items.FISHING_ROD, pPlayer, $$6);
            return InteractionResultHolder.success($$7);
         }

         pPlayer.awardStat(Stats.ITEM_USED.get(this));
         return InteractionResultHolder.pass($$3);
      }
   }
}
