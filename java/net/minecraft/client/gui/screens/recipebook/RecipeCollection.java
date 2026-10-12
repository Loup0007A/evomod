package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import net.minecraft.core.RegistryAccess;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeCollection {
   private final RegistryAccess registryAccess;
   private final List<RecipeHolder<?>> recipes;
   private final boolean singleResultItem;
   private final Set<RecipeHolder<?>> craftable = Sets.newHashSet();
   private final Set<RecipeHolder<?>> fitsDimensions = Sets.newHashSet();
   private final Set<RecipeHolder<?>> known = Sets.newHashSet();

   public RecipeCollection(RegistryAccess pRegistryAccess, List<RecipeHolder<?>> pRecipes) {
      this.registryAccess = pRegistryAccess;
      this.recipes = ImmutableList.copyOf(pRecipes);
      if (pRecipes.size() <= 1) {
         this.singleResultItem = true;
      } else {
         this.singleResultItem = allRecipesHaveSameResult(pRegistryAccess, pRecipes);
      }
   }

   private static boolean allRecipesHaveSameResult(RegistryAccess pRegistryAccess, List<RecipeHolder<?>> pRecipes) {
      int $$2 = pRecipes.size();
      ItemStack $$3 = pRecipes.get(0).value().getResultItem(pRegistryAccess);

      for (int $$4 = 1; $$4 < $$2; $$4++) {
         ItemStack $$5 = pRecipes.get($$4).value().getResultItem(pRegistryAccess);
         if (!ItemStack.isSameItemSameComponents($$3, $$5)) {
            return false;
         }
      }

      return true;
   }

   public RegistryAccess registryAccess() {
      return this.registryAccess;
   }

   public boolean hasKnownRecipes() {
      return !this.known.isEmpty();
   }

   public void updateKnownRecipes(RecipeBook pBook) {
      for (RecipeHolder<?> $$1 : this.recipes) {
         if (pBook.contains($$1)) {
            this.known.add($$1);
         }
      }
   }

   public void canCraft(StackedContents pHandler, int pWidth, int pHeight, RecipeBook pBook) {
      for (RecipeHolder<?> $$4 : this.recipes) {
         boolean $$5 = $$4.value().canCraftInDimensions(pWidth, pHeight) && pBook.contains($$4);
         if ($$5) {
            this.fitsDimensions.add($$4);
         } else {
            this.fitsDimensions.remove($$4);
         }

         if ($$5 && pHandler.canCraft($$4.value(), null)) {
            this.craftable.add($$4);
         } else {
            this.craftable.remove($$4);
         }
      }
   }

   public boolean isCraftable(RecipeHolder<?> pRecipe) {
      return this.craftable.contains(pRecipe);
   }

   public boolean hasCraftable() {
      return !this.craftable.isEmpty();
   }

   public boolean hasFitting() {
      return !this.fitsDimensions.isEmpty();
   }

   public List<RecipeHolder<?>> getRecipes() {
      return this.recipes;
   }

   public List<RecipeHolder<?>> getRecipes(boolean pOnlyCraftable) {
      List<RecipeHolder<?>> $$1 = Lists.newArrayList();
      Set<RecipeHolder<?>> $$2 = pOnlyCraftable ? this.craftable : this.fitsDimensions;

      for (RecipeHolder<?> $$3 : this.recipes) {
         if ($$2.contains($$3)) {
            $$1.add($$3);
         }
      }

      return $$1;
   }

   public List<RecipeHolder<?>> getDisplayRecipes(boolean pCraftable) {
      List<RecipeHolder<?>> $$1 = Lists.newArrayList();

      for (RecipeHolder<?> $$2 : this.recipes) {
         if (this.fitsDimensions.contains($$2) && this.craftable.contains($$2) == pCraftable) {
            $$1.add($$2);
         }
      }

      return $$1;
   }

   public boolean hasSingleResultItem() {
      return this.singleResultItem;
   }
}
