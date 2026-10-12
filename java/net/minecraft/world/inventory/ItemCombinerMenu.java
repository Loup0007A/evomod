package net.minecraft.world.inventory;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ItemCombinerMenu extends AbstractContainerMenu {
   private static final int INVENTORY_SLOTS_PER_ROW = 9;
   private static final int INVENTORY_SLOTS_PER_COLUMN = 3;
   protected final ContainerLevelAccess access;
   protected final Player player;
   protected final Container inputSlots;
   private final List<Integer> inputSlotIndexes;
   protected final ResultContainer resultSlots = new ResultContainer();
   private final int resultSlotIndex;

   protected abstract boolean mayPickup(Player pPlayer, boolean pHasStack);

   protected abstract void onTake(Player pPlayer, ItemStack pStack);

   protected abstract boolean isValidBlock(BlockState pState);

   public ItemCombinerMenu(@Nullable MenuType<?> pType, int pContainerId, Inventory pPlayerInventory, ContainerLevelAccess pAccess) {
      super(pType, pContainerId);
      this.access = pAccess;
      this.player = pPlayerInventory.player;
      ItemCombinerMenuSlotDefinition $$4 = this.createInputSlotDefinitions();
      this.inputSlots = this.createContainer($$4.getNumOfInputSlots());
      this.inputSlotIndexes = $$4.getInputSlotIndexes();
      this.resultSlotIndex = $$4.getResultSlotIndex();
      this.createInputSlots($$4);
      this.createResultSlot($$4);
      this.createInventorySlots(pPlayerInventory);
   }

   private void createInputSlots(ItemCombinerMenuSlotDefinition pSlotDefinition) {
      for (final ItemCombinerMenuSlotDefinition.SlotDefinition $$1 : pSlotDefinition.getSlots()) {
         this.addSlot(new Slot(this.inputSlots, $$1.slotIndex(), $$1.x(), $$1.y()) {
            @Override
            public boolean mayPlace(ItemStack p_267156_) {
               return $$1.mayPlace().test(p_267156_);
            }
         });
      }
   }

   private void createResultSlot(ItemCombinerMenuSlotDefinition pSlotDefinition) {
      this.addSlot(
         new Slot(this.resultSlots, pSlotDefinition.getResultSlot().slotIndex(), pSlotDefinition.getResultSlot().x(), pSlotDefinition.getResultSlot().y()) {
            @Override
            public boolean mayPlace(ItemStack p_39818_) {
               return false;
            }

            @Override
            public boolean mayPickup(Player p_39813_) {
               return ItemCombinerMenu.this.mayPickup(p_39813_, this.hasItem());
            }

            @Override
            public void onTake(Player p_150604_, ItemStack p_150605_) {
               ItemCombinerMenu.this.onTake(p_150604_, p_150605_);
            }
         }
      );
   }

   private void createInventorySlots(Inventory pInventory) {
      for (int $$1 = 0; $$1 < 3; $$1++) {
         for (int $$2 = 0; $$2 < 9; $$2++) {
            this.addSlot(new Slot(pInventory, $$2 + $$1 * 9 + 9, 8 + $$2 * 18, 84 + $$1 * 18));
         }
      }

      for (int $$3 = 0; $$3 < 9; $$3++) {
         this.addSlot(new Slot(pInventory, $$3, 8 + $$3 * 18, 142));
      }
   }

   public abstract void createResult();

   protected abstract ItemCombinerMenuSlotDefinition createInputSlotDefinitions();

   private SimpleContainer createContainer(int pSize) {
      return new SimpleContainer(pSize) {
         @Override
         public void setChanged() {
            super.setChanged();
            ItemCombinerMenu.this.slotsChanged(this);
         }
      };
   }

   @Override
   public void slotsChanged(Container pInventory) {
      super.slotsChanged(pInventory);
      if (pInventory == this.inputSlots) {
         this.createResult();
      }
   }

   @Override
   public void removed(Player pPlayer) {
      super.removed(pPlayer);
      this.access.execute((p_39796_, p_39797_) -> this.clearContainer(pPlayer, this.inputSlots));
   }

   @Override
   public boolean stillValid(Player pPlayer) {
      return this.access
         .evaluate(
            (p_327088_, p_327089_) -> !this.isValidBlock(p_327088_.getBlockState(p_327089_)) ? false : pPlayer.canInteractWithBlock(p_327089_, 4.0), true
         );
   }

   @Override
   public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
      ItemStack $$2 = ItemStack.EMPTY;
      Slot $$3 = this.slots.get(pIndex);
      if ($$3 != null && $$3.hasItem()) {
         ItemStack $$4 = $$3.getItem();
         $$2 = $$4.copy();
         int $$5 = this.getInventorySlotStart();
         int $$6 = this.getUseRowEnd();
         if (pIndex == this.getResultSlot()) {
            if (!this.moveItemStackTo($$4, $$5, $$6, true)) {
               return ItemStack.EMPTY;
            }

            $$3.onQuickCraft($$4, $$2);
         } else if (this.inputSlotIndexes.contains(pIndex)) {
            if (!this.moveItemStackTo($$4, $$5, $$6, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.canMoveIntoInputSlots($$4) && pIndex >= this.getInventorySlotStart() && pIndex < this.getUseRowEnd()) {
            int $$7 = this.getSlotToQuickMoveTo($$2);
            if (!this.moveItemStackTo($$4, $$7, this.getResultSlot(), false)) {
               return ItemStack.EMPTY;
            }
         } else if (pIndex >= this.getInventorySlotStart() && pIndex < this.getInventorySlotEnd()) {
            if (!this.moveItemStackTo($$4, this.getUseRowStart(), this.getUseRowEnd(), false)) {
               return ItemStack.EMPTY;
            }
         } else if (pIndex >= this.getUseRowStart()
            && pIndex < this.getUseRowEnd()
            && !this.moveItemStackTo($$4, this.getInventorySlotStart(), this.getInventorySlotEnd(), false)) {
            return ItemStack.EMPTY;
         }

         if ($$4.isEmpty()) {
            $$3.setByPlayer(ItemStack.EMPTY);
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

   protected boolean canMoveIntoInputSlots(ItemStack pStack) {
      return true;
   }

   public int getSlotToQuickMoveTo(ItemStack pStack) {
      return this.inputSlots.isEmpty() ? 0 : this.inputSlotIndexes.get(0);
   }

   public int getResultSlot() {
      return this.resultSlotIndex;
   }

   private int getInventorySlotStart() {
      return this.getResultSlot() + 1;
   }

   private int getInventorySlotEnd() {
      return this.getInventorySlotStart() + 27;
   }

   private int getUseRowStart() {
      return this.getInventorySlotEnd();
   }

   private int getUseRowEnd() {
      return this.getUseRowStart() + 9;
   }
}
