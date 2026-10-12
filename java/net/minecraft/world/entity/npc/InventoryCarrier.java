package net.minecraft.world.entity.npc;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public interface InventoryCarrier {
   String TAG_INVENTORY = "Inventory";

   SimpleContainer getInventory();

   static void pickUpItem(Mob pMob, InventoryCarrier pCarrier, ItemEntity pItemEntity) {
      ItemStack $$3 = pItemEntity.getItem();
      if (pMob.wantsToPickUp($$3)) {
         SimpleContainer $$4 = pCarrier.getInventory();
         boolean $$5 = $$4.canAddItem($$3);
         if (!$$5) {
            return;
         }

         pMob.onItemPickup(pItemEntity);
         int $$6 = $$3.getCount();
         ItemStack $$7 = $$4.addItem($$3);
         pMob.take(pItemEntity, $$6 - $$7.getCount());
         if ($$7.isEmpty()) {
            pItemEntity.discard();
         } else {
            $$3.setCount($$7.getCount());
         }
      }
   }

   default void readInventoryFromTag(CompoundTag pTag, HolderLookup.Provider pLevelRegistry) {
      if (pTag.contains("Inventory", 9)) {
         this.getInventory().fromTag(pTag.getList("Inventory", 10), pLevelRegistry);
      }
   }

   default void writeInventoryToTag(CompoundTag pTag, HolderLookup.Provider pLevelRegistry) {
      pTag.put("Inventory", this.getInventory().createTag(pLevelRegistry));
   }
}
