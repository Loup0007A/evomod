package net.minecraft.world.level.block.entity;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.state.BlockState;

public class CrafterBlockEntity extends RandomizableContainerBlockEntity implements CraftingContainer {
   public static final int CONTAINER_WIDTH = 3;
   public static final int CONTAINER_HEIGHT = 3;
   public static final int CONTAINER_SIZE = 9;
   public static final int SLOT_DISABLED = 1;
   public static final int SLOT_ENABLED = 0;
   public static final int DATA_TRIGGERED = 9;
   public static final int NUM_DATA = 10;
   private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
   private int craftingTicksRemaining = 0;
   protected final ContainerData containerData = new ContainerData() {
      private final int[] slotStates = new int[9];
      private int triggered = 0;

      @Override
      public int get(int p_310435_) {
         return p_310435_ == 9 ? this.triggered : this.slotStates[p_310435_];
      }

      @Override
      public void set(int p_313229_, int p_312585_) {
         if (p_313229_ == 9) {
            this.triggered = p_312585_;
         } else {
            this.slotStates[p_313229_] = p_312585_;
         }
      }

      @Override
      public int getCount() {
         return 10;
      }
   };

   public CrafterBlockEntity(BlockPos pPos, BlockState pState) {
      super(BlockEntityType.CRAFTER, pPos, pState);
   }

   @Override
   protected Component getDefaultName() {
      return Component.translatable("container.crafter");
   }

   @Override
   protected AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory) {
      return new CrafterMenu(pContainerId, pInventory, this, this.containerData);
   }

   public void setSlotState(int pSlot, boolean pState) {
      if (this.slotCanBeDisabled(pSlot)) {
         this.containerData.set(pSlot, pState ? 0 : 1);
         this.setChanged();
      }
   }

   public boolean isSlotDisabled(int pSlot) {
      return pSlot >= 0 && pSlot < 9 ? this.containerData.get(pSlot) == 1 : false;
   }

   @Override
   public boolean canPlaceItem(int pSlot, ItemStack pStack) {
      if (this.containerData.get(pSlot) == 1) {
         return false;
      } else {
         ItemStack $$2 = this.items.get(pSlot);
         int $$3 = $$2.getCount();
         if ($$3 >= $$2.getMaxStackSize()) {
            return false;
         } else {
            return $$2.isEmpty() ? true : !this.smallerStackExist($$3, $$2, pSlot);
         }
      }
   }

   private boolean smallerStackExist(int pCurrentSize, ItemStack pStack, int pSlot) {
      for (int $$3 = pSlot + 1; $$3 < 9; $$3++) {
         if (!this.isSlotDisabled($$3)) {
            ItemStack $$4 = this.getItem($$3);
            if ($$4.isEmpty() || $$4.getCount() < pCurrentSize && ItemStack.isSameItemSameComponents($$4, pStack)) {
               return true;
            }
         }
      }

      return false;
   }

   @Override
   protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.loadAdditional(pTag, pRegistries);
      this.craftingTicksRemaining = pTag.getInt("crafting_ticks_remaining");
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      if (!this.tryLoadLootTable(pTag)) {
         ContainerHelper.loadAllItems(pTag, this.items, pRegistries);
      }

      int[] $$2 = pTag.getIntArray("disabled_slots");

      for (int $$3 = 0; $$3 < 9; $$3++) {
         this.containerData.set($$3, 0);
      }

      for (int $$4 : $$2) {
         if (this.slotCanBeDisabled($$4)) {
            this.containerData.set($$4, 1);
         }
      }

      this.containerData.set(9, pTag.getInt("triggered"));
   }

   @Override
   protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.saveAdditional(pTag, pRegistries);
      pTag.putInt("crafting_ticks_remaining", this.craftingTicksRemaining);
      if (!this.trySaveLootTable(pTag)) {
         ContainerHelper.saveAllItems(pTag, this.items, pRegistries);
      }

      this.addDisabledSlots(pTag);
      this.addTriggered(pTag);
   }

   @Override
   public int getContainerSize() {
      return 9;
   }

   @Override
   public boolean isEmpty() {
      for (ItemStack $$0 : this.items) {
         if (!$$0.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   @Override
   public ItemStack getItem(int pIndex) {
      return this.items.get(pIndex);
   }

   @Override
   public void setItem(int pIndex, ItemStack pStack) {
      if (this.isSlotDisabled(pIndex)) {
         this.setSlotState(pIndex, true);
      }

      super.setItem(pIndex, pStack);
   }

   @Override
   public boolean stillValid(Player pPlayer) {
      return Container.stillValidBlockEntity(this, pPlayer);
   }

   @Override
   public NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   protected void setItems(NonNullList<ItemStack> pItems) {
      this.items = pItems;
   }

   @Override
   public int getWidth() {
      return 3;
   }

   @Override
   public int getHeight() {
      return 3;
   }

   @Override
   public void fillStackedContents(StackedContents pContents) {
      for (ItemStack $$1 : this.items) {
         pContents.accountSimpleStack($$1);
      }
   }

   private void addDisabledSlots(CompoundTag pTag) {
      IntList $$1 = new IntArrayList();

      for (int $$2 = 0; $$2 < 9; $$2++) {
         if (this.isSlotDisabled($$2)) {
            $$1.add($$2);
         }
      }

      pTag.putIntArray("disabled_slots", $$1);
   }

   private void addTriggered(CompoundTag pTag) {
      pTag.putInt("triggered", this.containerData.get(9));
   }

   public void setTriggered(boolean pTriggered) {
      this.containerData.set(9, pTriggered ? 1 : 0);
   }

   @VisibleForTesting
   public boolean isTriggered() {
      return this.containerData.get(9) == 1;
   }

   public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, CrafterBlockEntity pCrafter) {
      int $$4 = pCrafter.craftingTicksRemaining - 1;
      if ($$4 >= 0) {
         pCrafter.craftingTicksRemaining = $$4;
         if ($$4 == 0) {
            pLevel.setBlock(pPos, pState.setValue(CrafterBlock.CRAFTING, Boolean.valueOf(false)), 3);
         }
      }
   }

   public void setCraftingTicksRemaining(int pCraftingTicksRemaining) {
      this.craftingTicksRemaining = pCraftingTicksRemaining;
   }

   public int getRedstoneSignal() {
      int $$0 = 0;

      for (int $$1 = 0; $$1 < this.getContainerSize(); $$1++) {
         ItemStack $$2 = this.getItem($$1);
         if (!$$2.isEmpty() || this.isSlotDisabled($$1)) {
            $$0++;
         }
      }

      return $$0;
   }

   private boolean slotCanBeDisabled(int pSlot) {
      return pSlot > -1 && pSlot < 9 && this.items.get(pSlot).isEmpty();
   }
}
