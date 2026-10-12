package net.minecraftforge.common.crafting;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;

public interface IShapedRecipe<T extends RecipeInput> extends Recipe<T> {
   int getRecipeWidth();

   int getRecipeHeight();
}
