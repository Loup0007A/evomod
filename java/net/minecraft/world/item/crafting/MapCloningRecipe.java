package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class MapCloningRecipe extends CustomRecipe {
   public MapCloningRecipe(CraftingBookCategory pCategory) {
      super(pCategory);
   }

   public boolean matches(CraftingInput pInput, Level pLevel) {
      int $$2 = 0;
      ItemStack $$3 = ItemStack.EMPTY;

      for (int $$4 = 0; $$4 < pInput.size(); $$4++) {
         ItemStack $$5 = pInput.getItem($$4);
         if (!$$5.isEmpty()) {
            if ($$5.is(Items.FILLED_MAP)) {
               if (!$$3.isEmpty()) {
                  return false;
               }

               $$3 = $$5;
            } else {
               if (!$$5.is(Items.MAP)) {
                  return false;
               }

               $$2++;
            }
         }
      }

      return !$$3.isEmpty() && $$2 > 0;
   }

   public ItemStack assemble(CraftingInput pInput, HolderLookup.Provider pRegistries) {
      int $$2 = 0;
      ItemStack $$3 = ItemStack.EMPTY;

      for (int $$4 = 0; $$4 < pInput.size(); $$4++) {
         ItemStack $$5 = pInput.getItem($$4);
         if (!$$5.isEmpty()) {
            if ($$5.is(Items.FILLED_MAP)) {
               if (!$$3.isEmpty()) {
                  return ItemStack.EMPTY;
               }

               $$3 = $$5;
            } else {
               if (!$$5.is(Items.MAP)) {
                  return ItemStack.EMPTY;
               }

               $$2++;
            }
         }
      }

      return !$$3.isEmpty() && $$2 >= 1 ? $$3.copyWithCount($$2 + 1) : ItemStack.EMPTY;
   }

   @Override
   public boolean canCraftInDimensions(int pWidth, int pHeight) {
      return pWidth >= 3 && pHeight >= 3;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.MAP_CLONING;
   }
}
