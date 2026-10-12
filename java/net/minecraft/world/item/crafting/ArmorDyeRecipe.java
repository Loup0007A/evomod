package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;

public class ArmorDyeRecipe extends CustomRecipe {
   public ArmorDyeRecipe(CraftingBookCategory pCategory) {
      super(pCategory);
   }

   public boolean matches(CraftingInput pInput, Level pLevel) {
      ItemStack $$2 = ItemStack.EMPTY;
      List<ItemStack> $$3 = Lists.newArrayList();

      for (int $$4 = 0; $$4 < pInput.size(); $$4++) {
         ItemStack $$5 = pInput.getItem($$4);
         if (!$$5.isEmpty()) {
            if ($$5.is(ItemTags.DYEABLE)) {
               if (!$$2.isEmpty()) {
                  return false;
               }

               $$2 = $$5;
            } else {
               if (!($$5.getItem() instanceof DyeItem)) {
                  return false;
               }

               $$3.add($$5);
            }
         }
      }

      return !$$2.isEmpty() && !$$3.isEmpty();
   }

   public ItemStack assemble(CraftingInput pInput, HolderLookup.Provider pRegistries) {
      List<DyeItem> $$2 = Lists.newArrayList();
      ItemStack $$3 = ItemStack.EMPTY;

      for (int $$4 = 0; $$4 < pInput.size(); $$4++) {
         ItemStack $$5 = pInput.getItem($$4);
         if (!$$5.isEmpty()) {
            if ($$5.is(ItemTags.DYEABLE)) {
               if (!$$3.isEmpty()) {
                  return ItemStack.EMPTY;
               }

               $$3 = $$5.copy();
            } else {
               if (!($$5.getItem() instanceof DyeItem $$6)) {
                  return ItemStack.EMPTY;
               }

               $$2.add($$6);
            }
         }
      }

      return !$$3.isEmpty() && !$$2.isEmpty() ? DyedItemColor.applyDyes($$3, $$2) : ItemStack.EMPTY;
   }

   @Override
   public boolean canCraftInDimensions(int pWidth, int pHeight) {
      return pWidth * pHeight >= 2;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.ARMOR_DYE;
   }
}
