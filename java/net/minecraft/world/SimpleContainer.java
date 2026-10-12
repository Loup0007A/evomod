package net.minecraft.world;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SimpleContainer implements Container, StackedContentsCompatible {
   private final int size;
   private final NonNullList<ItemStack> items;
   @Nullable
   private List<ContainerListener> listeners;

   public SimpleContainer(int pSize) {
      this.size = pSize;
      this.items = NonNullList.withSize(pSize, ItemStack.EMPTY);
   }

   public SimpleContainer(ItemStack... pItems) {
      this.size = pItems.length;
      this.items = NonNullList.of(ItemStack.EMPTY, pItems);
   }

   public void addListener(ContainerListener pListener) {
      if (this.listeners == null) {
         this.listeners = Lists.newArrayList();
      }

      this.listeners.add(pListener);
   }

   public void removeListener(ContainerListener pListener) {
      if (this.listeners != null) {
         this.listeners.remove(pListener);
      }
   }

   @Override
   public ItemStack getItem(int pIndex) {
      return pIndex >= 0 && pIndex < this.items.size() ? this.items.get(pIndex) : ItemStack.EMPTY;
   }

   public List<ItemStack> removeAllItems() {
      List<ItemStack> $$0 = this.items.stream().filter(p_19197_ -> !p_19197_.isEmpty()).collect(Collectors.toList());
      this.clearContent();
      return $$0;
   }

   @Override
   public ItemStack removeItem(int pIndex, int pCount) {
      ItemStack $$2 = ContainerHelper.removeItem(this.items, pIndex, pCount);
      if (!$$2.isEmpty()) {
         this.setChanged();
      }

      return $$2;
   }

   public ItemStack removeItemType(Item pItem, int pAmount) {
      ItemStack $$2 = new ItemStack(pItem, 0);

      for (int $$3 = this.size - 1; $$3 >= 0; $$3--) {
         ItemStack $$4 = this.getItem($$3);
         if ($$4.getItem().equals(pItem)) {
            int $$5 = pAmount - $$2.getCount();
            ItemStack $$6 = $$4.split($$5);
            $$2.grow($$6.getCount());
            if ($$2.getCount() == pAmount) {
               break;
            }
         }
      }

      if (!$$2.isEmpty()) {
         this.setChanged();
      }

      return $$2;
   }

   public ItemStack addItem(ItemStack p_19174_) {
      if (p_19174_.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack $$1 = p_19174_.copy();
         this.moveItemToOccupiedSlotsWithSameType($$1);
         if ($$1.isEmpty()) {
            return ItemStack.EMPTY;
         } else {
            this.moveItemToEmptySlots($$1);
            return $$1.isEmpty() ? ItemStack.EMPTY : $$1;
         }
      }
   }

   public boolean canAddItem(ItemStack pStack) {
      boolean $$1 = false;

      for (ItemStack $$2 : this.items) {
         if ($$2.isEmpty() || ItemStack.isSameItemSameComponents($$2, pStack) && $$2.getCount() < $$2.getMaxStackSize()) {
            $$1 = true;
            break;
         }
      }

      return $$1;
   }

   @Override
   public ItemStack removeItemNoUpdate(int pIndex) {
      ItemStack $$1 = this.items.get(pIndex);
      if ($$1.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         this.items.set(pIndex, ItemStack.EMPTY);
         return $$1;
      }
   }

   @Override
   public void setItem(int pIndex, ItemStack pStack) {
      this.items.set(pIndex, pStack);
      pStack.limitSize(this.getMaxStackSize(pStack));
      this.setChanged();
   }

   @Override
   public int getContainerSize() {
      return this.size;
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
   public void setChanged() {
      if (this.listeners != null) {
         for (ContainerListener $$0 : this.listeners) {
            $$0.containerChanged(this);
         }
      }
   }

   @Override
   public boolean stillValid(Player pPlayer) {
      return true;
   }

   @Override
   public void clearContent() {
      this.items.clear();
      this.setChanged();
   }

   @Override
   public void fillStackedContents(StackedContents pHelper) {
      for (ItemStack $$1 : this.items) {
         pHelper.accountStack($$1);
      }
   }

   @Override
   public String toString() {
      return this.items.stream().filter(p_19194_ -> !p_19194_.isEmpty()).collect(Collectors.toList()).toString();
   }

   private void moveItemToEmptySlots(ItemStack pStack) {
      for (int $$1 = 0; $$1 < this.size; $$1++) {
         ItemStack $$2 = this.getItem($$1);
         if ($$2.isEmpty()) {
            this.setItem($$1, pStack.copyAndClear());
            return;
         }
      }
   }

   private void moveItemToOccupiedSlotsWithSameType(ItemStack pStack) {
      for (int $$1 = 0; $$1 < this.size; $$1++) {
         ItemStack $$2 = this.getItem($$1);
         if (ItemStack.isSameItemSameComponents($$2, pStack)) {
            this.moveItemsBetweenStacks(pStack, $$2);
            if (pStack.isEmpty()) {
               return;
            }
         }
      }
   }

   private void moveItemsBetweenStacks(ItemStack pStack, ItemStack pOther) {
      int $$2 = this.getMaxStackSize(pOther);
      int $$3 = Math.min(pStack.getCount(), $$2 - pOther.getCount());
      if ($$3 > 0) {
         pOther.grow($$3);
         pStack.shrink($$3);
         this.setChanged();
      }
   }

   public void fromTag(ListTag pTag, HolderLookup.Provider pLevelRegistry) {
      this.clearContent();

      for (int $$2 = 0; $$2 < pTag.size(); $$2++) {
         ItemStack.parse(pLevelRegistry, pTag.getCompound($$2)).ifPresent(this::addItem);
      }
   }

   public ListTag createTag(HolderLookup.Provider pLevelRegistry) {
      ListTag $$1 = new ListTag();

      for (int $$2 = 0; $$2 < this.getContainerSize(); $$2++) {
         ItemStack $$3 = this.getItem($$2);
         if (!$$3.isEmpty()) {
            $$1.add($$3.save(pLevelRegistry));
         }
      }

      return $$1;
   }

   public NonNullList<ItemStack> getItems() {
      return this.items;
   }
}
