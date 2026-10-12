package net.minecraft.world.item.crafting;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RecipeCache {
   private final RecipeCache.Entry[] entries;
   private WeakReference<RecipeManager> cachedRecipeManager = new WeakReference<>(null);

   public RecipeCache(int pSize) {
      this.entries = new RecipeCache.Entry[pSize];
   }

   public Optional<RecipeHolder<CraftingRecipe>> get(Level pLevel, CraftingInput pInput) {
      if (pInput.isEmpty()) {
         return Optional.empty();
      } else {
         this.validateRecipeManager(pLevel);

         for (int $$2 = 0; $$2 < this.entries.length; $$2++) {
            RecipeCache.Entry $$3 = this.entries[$$2];
            if ($$3 != null && $$3.matches(pInput)) {
               this.moveEntryToFront($$2);
               return Optional.ofNullable($$3.value());
            }
         }

         return this.compute(pInput, pLevel);
      }
   }

   private void validateRecipeManager(Level pLevel) {
      RecipeManager $$1 = pLevel.getRecipeManager();
      if ($$1 != this.cachedRecipeManager.get()) {
         this.cachedRecipeManager = new WeakReference<>($$1);
         Arrays.fill(this.entries, null);
      }
   }

   private Optional<RecipeHolder<CraftingRecipe>> compute(CraftingInput pInput, Level pLevel) {
      Optional<RecipeHolder<CraftingRecipe>> $$2 = pLevel.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, pInput, pLevel);
      this.insert(pInput, $$2.orElse(null));
      return $$2;
   }

   private void moveEntryToFront(int pIndex) {
      if (pIndex > 0) {
         RecipeCache.Entry $$1 = this.entries[pIndex];
         System.arraycopy(this.entries, 0, this.entries, 1, pIndex);
         this.entries[0] = $$1;
      }
   }

   private void insert(CraftingInput pInput, @Nullable RecipeHolder<CraftingRecipe> pRecipe) {
      NonNullList<ItemStack> $$2 = NonNullList.withSize(pInput.size(), ItemStack.EMPTY);

      for (int $$3 = 0; $$3 < pInput.size(); $$3++) {
         $$2.set($$3, pInput.getItem($$3).copyWithCount(1));
      }

      System.arraycopy(this.entries, 0, this.entries, 1, this.entries.length - 1);
      this.entries[0] = new RecipeCache.Entry($$2, pInput.width(), pInput.height(), pRecipe);
   }

   static record Entry(NonNullList<ItemStack> key, int width, int height, @Nullable RecipeHolder<CraftingRecipe> value) {
      public boolean matches(CraftingInput pInput) {
         if (this.width == pInput.width() && this.height == pInput.height()) {
            for (int $$1 = 0; $$1 < this.key.size(); $$1++) {
               if (!ItemStack.isSameItemSameComponents(this.key.get($$1), pInput.getItem($$1))) {
                  return false;
               }
            }

            return true;
         } else {
            return false;
         }
      }
   }
}
