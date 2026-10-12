package net.minecraft.world.item.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;

public class CraftingInput implements RecipeInput {
   public static final CraftingInput EMPTY = new CraftingInput(0, 0, List.of());
   private final int width;
   private final int height;
   private final List<ItemStack> items;
   private final StackedContents stackedContents = new StackedContents();
   private final int ingredientCount;

   private CraftingInput(int pWidth, int pHeight, List<ItemStack> pItem) {
      this.width = pWidth;
      this.height = pHeight;
      this.items = pItem;
      int $$3 = 0;

      for (ItemStack $$4 : pItem) {
         if (!$$4.isEmpty()) {
            $$3++;
            this.stackedContents.accountStack($$4, 1);
         }
      }

      this.ingredientCount = $$3;
   }

   public static CraftingInput of(int pWidth, int pHeight, List<ItemStack> pItems) {
      return ofPositioned(pWidth, pHeight, pItems).input();
   }

   public static CraftingInput.Positioned ofPositioned(int pWidth, int pHeight, List<ItemStack> pItems) {
      if (pWidth != 0 && pHeight != 0) {
         int $$3 = pWidth - 1;
         int $$4 = 0;
         int $$5 = pHeight - 1;
         int $$6 = 0;

         for (int $$7 = 0; $$7 < pHeight; $$7++) {
            boolean $$8 = true;

            for (int $$9 = 0; $$9 < pWidth; $$9++) {
               ItemStack $$10 = pItems.get($$9 + $$7 * pWidth);
               if (!$$10.isEmpty()) {
                  $$3 = Math.min($$3, $$9);
                  $$4 = Math.max($$4, $$9);
                  $$8 = false;
               }
            }

            if (!$$8) {
               $$5 = Math.min($$5, $$7);
               $$6 = Math.max($$6, $$7);
            }
         }

         int $$11 = $$4 - $$3 + 1;
         int $$12 = $$6 - $$5 + 1;
         if ($$11 <= 0 || $$12 <= 0) {
            return CraftingInput.Positioned.EMPTY;
         } else if ($$11 == pWidth && $$12 == pHeight) {
            return new CraftingInput.Positioned(new CraftingInput(pWidth, pHeight, pItems), $$3, $$5);
         } else {
            List<ItemStack> $$13 = new ArrayList<>($$11 * $$12);

            for (int $$14 = 0; $$14 < $$12; $$14++) {
               for (int $$15 = 0; $$15 < $$11; $$15++) {
                  int $$16 = $$15 + $$3 + ($$14 + $$5) * pWidth;
                  $$13.add(pItems.get($$16));
               }
            }

            return new CraftingInput.Positioned(new CraftingInput($$11, $$12, $$13), $$3, $$5);
         }
      } else {
         return CraftingInput.Positioned.EMPTY;
      }
   }

   @Override
   public ItemStack getItem(int pIndex) {
      return this.items.get(pIndex);
   }

   public ItemStack getItem(int pRow, int pColumn) {
      return this.items.get(pRow + pColumn * this.width);
   }

   @Override
   public int size() {
      return this.items.size();
   }

   @Override
   public boolean isEmpty() {
      return this.ingredientCount == 0;
   }

   public StackedContents stackedContents() {
      return this.stackedContents;
   }

   public List<ItemStack> items() {
      return this.items;
   }

   public int ingredientCount() {
      return this.ingredientCount;
   }

   public int width() {
      return this.width;
   }

   public int height() {
      return this.height;
   }

   @Override
   public boolean equals(Object pOther) {
      if (pOther == this) {
         return true;
      } else {
         return !(pOther instanceof CraftingInput $$1)
            ? false
            : this.width == $$1.width
               && this.height == $$1.height
               && this.ingredientCount == $$1.ingredientCount
               && ItemStack.listMatches(this.items, $$1.items);
      }
   }

   @Override
   public int hashCode() {
      int $$0 = ItemStack.hashStackList(this.items);
      $$0 = 31 * $$0 + this.width;
      return 31 * $$0 + this.height;
   }

   public static record Positioned(CraftingInput input, int left, int top) {
      public static final CraftingInput.Positioned EMPTY = new CraftingInput.Positioned(CraftingInput.EMPTY, 0, 0);
   }
}
