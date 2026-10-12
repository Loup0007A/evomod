package net.minecraft.world.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CrafterBlock;

public class CrafterMenu extends AbstractContainerMenu implements ContainerListener {
   protected static final int SLOT_COUNT = 9;
   private static final int INV_SLOT_START = 9;
   private static final int INV_SLOT_END = 36;
   private static final int USE_ROW_SLOT_START = 36;
   private static final int USE_ROW_SLOT_END = 45;
   private final ResultContainer resultContainer = new ResultContainer();
   private final ContainerData containerData;
   private final Player player;
   private final CraftingContainer container;

   public CrafterMenu(int pContainerId, Inventory pPlayerInventory) {
      super(MenuType.CRAFTER_3x3, pContainerId);
      this.player = pPlayerInventory.player;
      this.containerData = new SimpleContainerData(10);
      this.container = new TransientCraftingContainer(this, 3, 3);
      this.addSlots(pPlayerInventory);
   }

   public CrafterMenu(int pContainerId, Inventory pPlayerInventory, CraftingContainer pContainer, ContainerData pContainerData) {
      super(MenuType.CRAFTER_3x3, pContainerId);
      this.player = pPlayerInventory.player;
      this.containerData = pContainerData;
      this.container = pContainer;
      checkContainerSize(pContainer, 9);
      pContainer.startOpen(pPlayerInventory.player);
      this.addSlots(pPlayerInventory);
      this.addSlotListener(this);
   }

   private void addSlots(Inventory pPlayerInventory) {
      for (int $$1 = 0; $$1 < 3; $$1++) {
         for (int $$2 = 0; $$2 < 3; $$2++) {
            int $$3 = $$2 + $$1 * 3;
            this.addSlot(new CrafterSlot(this.container, $$3, 26 + $$2 * 18, 17 + $$1 * 18, this));
         }
      }

      for (int $$4 = 0; $$4 < 3; $$4++) {
         for (int $$5 = 0; $$5 < 9; $$5++) {
            this.addSlot(new Slot(pPlayerInventory, $$5 + $$4 * 9 + 9, 8 + $$5 * 18, 84 + $$4 * 18));
         }
      }

      for (int $$6 = 0; $$6 < 9; $$6++) {
         this.addSlot(new Slot(pPlayerInventory, $$6, 8 + $$6 * 18, 142));
      }

      this.addSlot(new NonInteractiveResultSlot(this.resultContainer, 0, 134, 35));
      this.addDataSlots(this.containerData);
      this.refreshRecipeResult();
   }

   public void setSlotState(int pSlot, boolean pEnabled) {
      CrafterSlot $$2 = (CrafterSlot)this.getSlot(pSlot);
      this.containerData.set($$2.index, pEnabled ? 0 : 1);
      this.broadcastChanges();
   }

   public boolean isSlotDisabled(int pSlot) {
      return pSlot > -1 && pSlot < 9 ? this.containerData.get(pSlot) == 1 : false;
   }

   public boolean isPowered() {
      return this.containerData.get(9) == 1;
   }

   @Override
   public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
      ItemStack $$2 = ItemStack.EMPTY;
      Slot $$3 = this.slots.get(pIndex);
      if ($$3 != null && $$3.hasItem()) {
         ItemStack $$4 = $$3.getItem();
         $$2 = $$4.copy();
         if (pIndex < 9) {
            if (!this.moveItemStackTo($$4, 9, 45, true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo($$4, 0, 9, false)) {
            return ItemStack.EMPTY;
         }

         if ($$4.isEmpty()) {
            $$3.set(ItemStack.EMPTY);
         } else {
            $$3.setChanged();
         }

         if ($$4.getCount() == $$2.getCount()) {
            return ItemStack.EMPTY;
         }

         $$3.onTake(pPlayer, $$4);
      }

      return $$2;
   }

   @Override
   public boolean stillValid(Player pPlayer) {
      return this.container.stillValid(pPlayer);
   }

   private void refreshRecipeResult() {
      if (this.player instanceof ServerPlayer $$0) {
         Level $$1 = $$0.level();
         CraftingInput $$2 = this.container.asCraftInput();
         ItemStack $$3 = CrafterBlock.getPotentialResults($$1, $$2)
            .map(p_341501_ -> p_341501_.value().assemble($$2, $$1.registryAccess()))
            .orElse(ItemStack.EMPTY);
         this.resultContainer.setItem(0, $$3);
      }
   }

   public Container getContainer() {
      return this.container;
   }

   @Override
   public void slotChanged(AbstractContainerMenu pContainerToSend, int pDataSlotIndex, ItemStack pStack) {
      this.refreshRecipeResult();
   }

   @Override
   public void dataChanged(AbstractContainerMenu pContainerMenu, int pDataSlotIndex, int pValue) {
   }
}
