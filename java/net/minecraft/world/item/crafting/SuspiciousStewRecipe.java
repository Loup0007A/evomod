package net.minecraft.world.item.crafting;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SuspiciousEffectHolder;

public class SuspiciousStewRecipe extends CustomRecipe {
   public SuspiciousStewRecipe(CraftingBookCategory pCategory) {
      super(pCategory);
   }

   public boolean matches(CraftingInput pInput, Level pLevel) {
      boolean $$2 = false;
      boolean $$3 = false;
      boolean $$4 = false;
      boolean $$5 = false;

      for (int $$6 = 0; $$6 < pInput.size(); $$6++) {
         ItemStack $$7 = pInput.getItem($$6);
         if (!$$7.isEmpty()) {
            if ($$7.is(Blocks.BROWN_MUSHROOM.asItem()) && !$$4) {
               $$4 = true;
            } else if ($$7.is(Blocks.RED_MUSHROOM.asItem()) && !$$3) {
               $$3 = true;
            } else if ($$7.is(ItemTags.SMALL_FLOWERS) && !$$2) {
               $$2 = true;
            } else {
               if (!$$7.is(Items.BOWL) || $$5) {
                  return false;
               }

               $$5 = true;
            }
         }
      }

      return $$2 && $$4 && $$3 && $$5;
   }

   public ItemStack assemble(CraftingInput pInput, HolderLookup.Provider pRegistries) {
      ItemStack $$2 = new ItemStack(Items.SUSPICIOUS_STEW, 1);

      for (int $$3 = 0; $$3 < pInput.size(); $$3++) {
         ItemStack $$4 = pInput.getItem($$3);
         if (!$$4.isEmpty()) {
            SuspiciousEffectHolder $$5 = SuspiciousEffectHolder.tryGet($$4.getItem());
            if ($$5 != null) {
               $$2.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, $$5.getSuspiciousEffects());
               break;
            }
         }
      }

      return $$2;
   }

   @Override
   public boolean canCraftInDimensions(int pWidth, int pHeight) {
      return pWidth >= 2 && pHeight >= 2;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.SUSPICIOUS_STEW;
   }
}
