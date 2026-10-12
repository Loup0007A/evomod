package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class TippedArrowRecipe extends CustomRecipe {
   public TippedArrowRecipe(CraftingBookCategory pCategory) {
      super(pCategory);
   }

   public boolean matches(CraftingInput pInput, Level pLevel) {
      if (pInput.width() == 3 && pInput.height() == 3) {
         for (int $$2 = 0; $$2 < pInput.height(); $$2++) {
            for (int $$3 = 0; $$3 < pInput.width(); $$3++) {
               ItemStack $$4 = pInput.getItem($$3, $$2);
               if ($$4.isEmpty()) {
                  return false;
               }

               if ($$3 == 1 && $$2 == 1) {
                  if (!$$4.is(Items.LINGERING_POTION)) {
                     return false;
                  }
               } else if (!$$4.is(Items.ARROW)) {
                  return false;
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public ItemStack assemble(CraftingInput pInput, HolderLookup.Provider pRegistries) {
      ItemStack $$2 = pInput.getItem(1, 1);
      if (!$$2.is(Items.LINGERING_POTION)) {
         return ItemStack.EMPTY;
      } else {
         ItemStack $$3 = new ItemStack(Items.TIPPED_ARROW, 8);
         $$3.set(DataComponents.POTION_CONTENTS, $$2.get(DataComponents.POTION_CONTENTS));
         return $$3;
      }
   }

   @Override
   public boolean canCraftInDimensions(int pWidth, int pHeight) {
      return pWidth >= 3 && pHeight >= 3;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.TIPPED_ARROW;
   }
}
