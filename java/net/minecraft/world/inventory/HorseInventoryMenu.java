package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class HorseInventoryMenu extends AbstractContainerMenu {
   private final Container horseContainer;
   private final Container armorContainer;
   private final AbstractHorse horse;
   private static final int SLOT_BODY_ARMOR = 1;
   private static final int SLOT_HORSE_INVENTORY_START = 2;

   public HorseInventoryMenu(int pContainerId, Inventory pInventory, Container pHorseContainer, final AbstractHorse pHorse, int pColumns) {
      super(null, pContainerId);
      this.horseContainer = pHorseContainer;
      this.armorContainer = pHorse.getBodyArmorAccess();
      this.horse = pHorse;
      int $$5 = 3;
      pHorseContainer.startOpen(pInventory.player);
      int $$6 = -18;
      this.addSlot(new Slot(pHorseContainer, 0, 8, 18) {
         @Override
         public boolean mayPlace(ItemStack p_39677_) {
            return p_39677_.is(Items.SADDLE) && !this.hasItem() && pHorse.isSaddleable();
         }

         @Override
         public boolean isActive() {
            return pHorse.isSaddleable();
         }
      });
      this.addSlot(new ArmorSlot(this.armorContainer, pHorse, EquipmentSlot.BODY, 0, 8, 36, null) {
         @Override
         public boolean mayPlace(ItemStack p_39690_) {
            return pHorse.isBodyArmorItem(p_39690_);
         }

         @Override
         public boolean isActive() {
            return pHorse.canUseSlot(EquipmentSlot.BODY);
         }
      });
      if (pColumns > 0) {
         for (int $$7 = 0; $$7 < 3; $$7++) {
            for (int $$8 = 0; $$8 < pColumns; $$8++) {
               this.addSlot(new Slot(pHorseContainer, 1 + $$8 + $$7 * pColumns, 80 + $$8 * 18, 18 + $$7 * 18));
            }
         }
      }

      for (int $$9 = 0; $$9 < 3; $$9++) {
         for (int $$10 = 0; $$10 < 9; $$10++) {
            this.addSlot(new Slot(pInventory, $$10 + $$9 * 9 + 9, 8 + $$10 * 18, 102 + $$9 * 18 + -18));
         }
      }

      for (int $$11 = 0; $$11 < 9; $$11++) {
         this.addSlot(new Slot(pInventory, $$11, 8 + $$11 * 18, 142));
      }
   }

   @Override
   public boolean stillValid(Player pPlayer) {
      return !this.horse.hasInventoryChanged(this.horseContainer)
         && this.horseContainer.stillValid(pPlayer)
         && this.armorContainer.stillValid(pPlayer)
         && this.horse.isAlive()
         && pPlayer.canInteractWithEntity(this.horse, 4.0);
   }

   @Override
   public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
      ItemStack $$2 = ItemStack.EMPTY;
      Slot $$3 = this.slots.get(pIndex);
      if ($$3 != null && $$3.hasItem()) {
         ItemStack $$4 = $$3.getItem();
         $$2 = $$4.copy();
         int $$5 = this.horseContainer.getContainerSize() + 1;
         if (pIndex < $$5) {
            if (!this.moveItemStackTo($$4, $$5, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (this.getSlot(1).mayPlace($$4) && !this.getSlot(1).hasItem()) {
            if (!this.moveItemStackTo($$4, 1, 2, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.getSlot(0).mayPlace($$4)) {
            if (!this.moveItemStackTo($$4, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if ($$5 <= 1 || !this.moveItemStackTo($$4, 2, $$5, false)) {
            int $$7 = $$5 + 27;
            int $$9 = $$7 + 9;
            if (pIndex >= $$7 && pIndex < $$9) {
               if (!this.moveItemStackTo($$4, $$5, $$7, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (pIndex >= $$5 && pIndex < $$7) {
               if (!this.moveItemStackTo($$4, $$7, $$9, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo($$4, $$7, $$7, false)) {
               return ItemStack.EMPTY;
            }

            return ItemStack.EMPTY;
         }

         if ($$4.isEmpty()) {
            $$3.setByPlayer(ItemStack.EMPTY);
         } else {
            $$3.setChanged();
         }
      }

      return $$2;
   }

   @Override
   public void removed(Player pPlayer) {
      super.removed(pPlayer);
      this.horseContainer.stopOpen(pPlayer);
   }
}
