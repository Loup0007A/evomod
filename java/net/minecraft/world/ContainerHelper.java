package net.minecraft.world;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public class ContainerHelper {
   public static final String TAG_ITEMS = "Items";

   public static ItemStack removeItem(List<ItemStack> pStacks, int pIndex, int pAmount) {
      return pIndex >= 0 && pIndex < pStacks.size() && !pStacks.get(pIndex).isEmpty() && pAmount > 0 ? pStacks.get(pIndex).split(pAmount) : ItemStack.EMPTY;
   }

   public static ItemStack takeItem(List<ItemStack> pStacks, int pIndex) {
      return pIndex >= 0 && pIndex < pStacks.size() ? pStacks.set(pIndex, ItemStack.EMPTY) : ItemStack.EMPTY;
   }

   public static CompoundTag saveAllItems(CompoundTag pTag, NonNullList<ItemStack> pItems, HolderLookup.Provider pLevelRegistry) {
      return saveAllItems(pTag, pItems, true, pLevelRegistry);
   }

   public static CompoundTag saveAllItems(CompoundTag pTag, NonNullList<ItemStack> pItems, boolean pAlwaysPutTag, HolderLookup.Provider pLevelRegistry) {
      ListTag $$4 = new ListTag();

      for (int $$5 = 0; $$5 < pItems.size(); $$5++) {
         ItemStack $$6 = pItems.get($$5);
         if (!$$6.isEmpty()) {
            CompoundTag $$7 = new CompoundTag();
            $$7.putByte("Slot", (byte)$$5);
            $$4.add($$6.save(pLevelRegistry, $$7));
         }
      }

      if (!$$4.isEmpty() || pAlwaysPutTag) {
         pTag.put("Items", $$4);
      }

      return pTag;
   }

   public static void loadAllItems(CompoundTag pTag, NonNullList<ItemStack> pItems, HolderLookup.Provider pLevelRegistry) {
      ListTag $$3 = pTag.getList("Items", 10);

      for (int $$4 = 0; $$4 < $$3.size(); $$4++) {
         CompoundTag $$5 = $$3.getCompound($$4);
         int $$6 = $$5.getByte("Slot") & 255;
         if ($$6 >= 0 && $$6 < pItems.size()) {
            pItems.set($$6, ItemStack.parse(pLevelRegistry, $$5).orElse(ItemStack.EMPTY));
         }
      }
   }

   public static int clearOrCountMatchingItems(Container pContainer, Predicate<ItemStack> pItemPredicate, int pMaxItems, boolean pSimulate) {
      int $$4 = 0;

      for (int $$5 = 0; $$5 < pContainer.getContainerSize(); $$5++) {
         ItemStack $$6 = pContainer.getItem($$5);
         int $$7 = clearOrCountMatchingItems($$6, pItemPredicate, pMaxItems - $$4, pSimulate);
         if ($$7 > 0 && !pSimulate && $$6.isEmpty()) {
            pContainer.setItem($$5, ItemStack.EMPTY);
         }

         $$4 += $$7;
      }

      return $$4;
   }

   public static int clearOrCountMatchingItems(ItemStack pStack, Predicate<ItemStack> pItemPredicate, int pMaxItems, boolean pSimulate) {
      if (pStack.isEmpty() || !pItemPredicate.test(pStack)) {
         return 0;
      } else if (pSimulate) {
         return pStack.getCount();
      } else {
         int $$4 = pMaxItems < 0 ? pStack.getCount() : Math.min(pMaxItems, pStack.getCount());
         pStack.shrink($$4);
         return $$4;
      }
   }
}
