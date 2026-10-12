package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ChestMenu extends AbstractContainerMenu {
   private static final int SLOTS_PER_ROW = 9;
   private final Container container;
   private final int containerRows;

   private ChestMenu(MenuType<?> pType, int pContainerId, Inventory pPlayerInventory, int pRows) {
      this(pType, pContainerId, pPlayerInventory, new SimpleContainer(9 * pRows), pRows);
   }

   public static ChestMenu oneRow(int pContainerId, Inventory pPlayerInventory) {
      return new ChestMenu(MenuType.GENERIC_9x1, pContainerId, pPlayerInventory, 1);
   }

   public static ChestMenu twoRows(int pContainerId, Inventory pPlayerInventory) {
      return new ChestMenu(MenuType.GENERIC_9x2, pContainerId, pPlayerInventory, 2);
   }

   public static ChestMenu threeRows(int pContainerId, Inventory pPlayerInventory) {
      return new ChestMenu(MenuType.GENERIC_9x3, pContainerId, pPlayerInventory, 3);
   }

   public static ChestMenu fourRows(int pContainerId, Inventory pPlayerInventory) {
      return new ChestMenu(MenuType.GENERIC_9x4, pContainerId, pPlayerInventory, 4);
   }

   public static ChestMenu fiveRows(int pContainerId, Inventory pPlayerInventory) {
      return new ChestMenu(MenuType.GENERIC_9x5, pContainerId, pPlayerInventory, 5);
   }

   public static ChestMenu sixRows(int pContainerId, Inventory pPlayerInventory) {
      return new ChestMenu(MenuType.GENERIC_9x6, pContainerId, pPlayerInventory, 6);
   }

   public static ChestMenu threeRows(int pContainerId, Inventory pPlayerInventory, Container pContainer) {
      return new ChestMenu(MenuType.GENERIC_9x3, pContainerId, pPlayerInventory, pContainer, 3);
   }

   public static ChestMenu sixRows(int pContainerId, Inventory pPlayerInventory, Container pContainer) {
      return new ChestMenu(MenuType.GENERIC_9x6, pContainerId, pPlayerInventory, pContainer, 6);
   }

   public ChestMenu(MenuType<?> pType, int pContainerId, Inventory pPlayerInventory, Container pContainer, int pRows) {
      super(pType, pContainerId);
      checkContainerSize(pContainer, pRows * 9);
      this.container = pContainer;
      this.containerRows = pRows;
      pContainer.startOpen(pPlayerInventory.player);
      int $$5 = (this.containerRows - 4) * 18;

      for (int $$6 = 0; $$6 < this.containerRows; $$6++) {
         for (int $$7 = 0; $$7 < 9; $$7++) {
            this.addSlot(new Slot(pContainer, $$7 + $$6 * 9, 8 + $$7 * 18, 18 + $$6 * 18));
         }
      }

      for (int $$8 = 0; $$8 < 3; $$8++) {
         for (int $$9 = 0; $$9 < 9; $$9++) {
            this.addSlot(new Slot(pPlayerInventory, $$9 + $$8 * 9 + 9, 8 + $$9 * 18, 103 + $$8 * 18 + $$5));
         }
      }

      for (int $$10 = 0; $$10 < 9; $$10++) {
         this.addSlot(new Slot(pPlayerInventory, $$10, 8 + $$10 * 18, 161 + $$5));
      }
   }

   @Override
   public boolean stillValid(Player pPlayer) {
      return this.container.stillValid(pPlayer);
   }

   @Override
   public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
      ItemStack $$2 = ItemStack.EMPTY;
      Slot $$3 = this.slots.get(pIndex);
      if ($$3 != null && $$3.hasItem()) {
         ItemStack $$4 = $$3.getItem();
         $$2 = $$4.copy();
         if (pIndex < this.containerRows * 9) {
            if (!this.moveItemStackTo($$4, this.containerRows * 9, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo($$4, 0, this.containerRows * 9, false)) {
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
      this.container.stopOpen(pPlayer);
   }

   public Container getContainer() {
      return this.container;
   }

   public int getRowCount() {
      return this.containerRows;
   }
}
