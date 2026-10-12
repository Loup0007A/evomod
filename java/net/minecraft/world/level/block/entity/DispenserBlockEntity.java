package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class DispenserBlockEntity extends RandomizableContainerBlockEntity {
   public static final int CONTAINER_SIZE = 9;
   private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

   protected DispenserBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
      super(pType, pPos, pBlockState);
   }

   public DispenserBlockEntity(BlockPos pPos, BlockState pBlockState) {
      this(BlockEntityType.DISPENSER, pPos, pBlockState);
   }

   @Override
   public int getContainerSize() {
      return 9;
   }

   public int getRandomSlot(RandomSource pRandom) {
      this.unpackLootTable(null);
      int $$1 = -1;
      int $$2 = 1;

      for (int $$3 = 0; $$3 < this.items.size(); $$3++) {
         if (!this.items.get($$3).isEmpty() && pRandom.nextInt($$2++) == 0) {
            $$1 = $$3;
         }
      }

      return $$1;
   }

   public ItemStack insertItem(ItemStack pStack) {
      int $$1 = this.getMaxStackSize(pStack);

      for (int $$2 = 0; $$2 < this.items.size(); $$2++) {
         ItemStack $$3 = this.items.get($$2);
         if ($$3.isEmpty() || ItemStack.isSameItemSameComponents(pStack, $$3)) {
            int $$4 = Math.min(pStack.getCount(), $$1 - $$3.getCount());
            if ($$4 > 0) {
               if ($$3.isEmpty()) {
                  this.setItem($$2, pStack.split($$4));
               } else {
                  pStack.shrink($$4);
                  $$3.grow($$4);
               }
            }

            if (pStack.isEmpty()) {
               break;
            }
         }
      }

      return pStack;
   }

   @Override
   protected Component getDefaultName() {
      return Component.translatable("container.dispenser");
   }

   @Override
   protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.loadAdditional(pTag, pRegistries);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      if (!this.tryLoadLootTable(pTag)) {
         ContainerHelper.loadAllItems(pTag, this.items, pRegistries);
      }
   }

   @Override
   protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
      super.saveAdditional(pTag, pRegistries);
      if (!this.trySaveLootTable(pTag)) {
         ContainerHelper.saveAllItems(pTag, this.items, pRegistries);
      }
   }

   @Override
   protected NonNullList<ItemStack> getItems() {
      return this.items;
   }

   @Override
   protected void setItems(NonNullList<ItemStack> pItems) {
      this.items = pItems;
   }

   @Override
   protected AbstractContainerMenu createMenu(int pId, Inventory pPlayer) {
      return new DispenserMenu(pId, pPlayer, this);
   }
}
