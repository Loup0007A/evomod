package net.minecraftforge.items;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.NotNull;

public class ItemStackHandler implements IItemHandler, IItemHandlerModifiable, INBTSerializable<CompoundTag> {
   protected NonNullList<ItemStack> stacks;

   public ItemStackHandler() {
      this(1);
   }

   public ItemStackHandler(int size) {
      this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
   }

   public ItemStackHandler(NonNullList<ItemStack> stacks) {
      this.stacks = stacks;
   }

   public void setSize(int size) {
      this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
   }

   @Override
   public void setStackInSlot(int slot, @NotNull ItemStack stack) {
      this.validateSlotIndex(slot);
      this.stacks.set(slot, stack);
      this.onContentsChanged(slot);
   }

   @Override
   public int getSlots() {
      return this.stacks.size();
   }

   @NotNull
   @Override
   public ItemStack getStackInSlot(int slot) {
      this.validateSlotIndex(slot);
      return this.stacks.get(slot);
   }

   @NotNull
   @Override
   public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
      if (stack.isEmpty()) {
         return ItemStack.EMPTY;
      } else if (!this.isItemValid(slot, stack)) {
         return stack;
      } else {
         this.validateSlotIndex(slot);
         ItemStack existing = this.stacks.get(slot);
         int limit = this.getStackLimit(slot, stack);
         if (!existing.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(stack, existing)) {
               return stack;
            }

            limit -= existing.getCount();
         }

         if (limit <= 0) {
            return stack;
         } else {
            boolean reachedLimit = stack.getCount() > limit;
            if (!simulate) {
               if (existing.isEmpty()) {
                  this.stacks.set(slot, reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, limit) : stack);
               } else {
                  existing.grow(reachedLimit ? limit : stack.getCount());
               }

               this.onContentsChanged(slot);
            }

            return reachedLimit ? ItemHandlerHelper.copyStackWithSize(stack, stack.getCount() - limit) : ItemStack.EMPTY;
         }
      }
   }

   @NotNull
   @Override
   public ItemStack extractItem(int slot, int amount, boolean simulate) {
      if (amount == 0) {
         return ItemStack.EMPTY;
      } else {
         this.validateSlotIndex(slot);
         ItemStack existing = this.stacks.get(slot);
         if (existing.isEmpty()) {
            return ItemStack.EMPTY;
         } else {
            int toExtract = Math.min(amount, existing.getMaxStackSize());
            if (existing.getCount() <= toExtract) {
               if (!simulate) {
                  this.stacks.set(slot, ItemStack.EMPTY);
                  this.onContentsChanged(slot);
                  return existing;
               } else {
                  return existing.copy();
               }
            } else {
               if (!simulate) {
                  this.stacks.set(slot, ItemHandlerHelper.copyStackWithSize(existing, existing.getCount() - toExtract));
                  this.onContentsChanged(slot);
               }

               return ItemHandlerHelper.copyStackWithSize(existing, toExtract);
            }
         }
      }
   }

   @Override
   public int getSlotLimit(int slot) {
      return 64;
   }

   protected int getStackLimit(int slot, @NotNull ItemStack stack) {
      return Math.min(this.getSlotLimit(slot), stack.getMaxStackSize());
   }

   @Override
   public boolean isItemValid(int slot, @NotNull ItemStack stack) {
      return true;
   }

   public CompoundTag serializeNBT(HolderLookup.Provider lookup) {
      CompoundTag tag = new CompoundTag();
      ContainerHelper.saveAllItems(tag, this.stacks, lookup);
      return tag;
   }

   public void deserializeNBT(HolderLookup.Provider lookup, CompoundTag nbt) {
      ContainerHelper.loadAllItems(nbt, this.stacks, lookup);
      this.onLoad();
   }

   protected void validateSlotIndex(int slot) {
      if (slot < 0 || slot >= this.stacks.size()) {
         throw new RuntimeException("Slot " + slot + " not in valid range - [0," + this.stacks.size() + ")");
      }
   }

   protected void onLoad() {
   }

   protected void onContentsChanged(int slot) {
   }
}
