package net.minecraft.data.recipes;

import javax.annotation.Nullable;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.level.ItemLike;

public interface RecipeBuilder {
   ResourceLocation ROOT_RECIPE_ADVANCEMENT = ResourceLocation.withDefaultNamespace("recipes/root");

   RecipeBuilder unlockedBy(String pName, Criterion<?> pCriterion);

   RecipeBuilder group(@Nullable String pGroupName);

   Item getResult();

   void save(RecipeOutput pRecipeOutput, ResourceLocation pId);

   default void save(RecipeOutput pRecipeOutput) {
      this.save(pRecipeOutput, getDefaultRecipeId(this.getResult()));
   }

   default void save(RecipeOutput pRecipeOutput, String pId) {
      ResourceLocation $$2 = getDefaultRecipeId(this.getResult());
      ResourceLocation $$3 = ResourceLocation.parse(pId);
      if ($$3.equals($$2)) {
         throw new IllegalStateException("Recipe " + pId + " should remove its 'save' argument as it is equal to default one");
      } else {
         this.save(pRecipeOutput, $$3);
      }
   }

   static ResourceLocation getDefaultRecipeId(ItemLike pItemLike) {
      return BuiltInRegistries.ITEM.getKey(pItemLike.asItem());
   }

   static CraftingBookCategory determineBookCategory(RecipeCategory pCategory) {
      return switch (pCategory) {
         case BUILDING_BLOCKS -> CraftingBookCategory.BUILDING;
         case TOOLS, COMBAT -> CraftingBookCategory.EQUIPMENT;
         case REDSTONE -> CraftingBookCategory.REDSTONE;
         default -> CraftingBookCategory.MISC;
      };
   }
}
