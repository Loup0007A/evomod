package net.minecraftforge.common.brewing;

import java.util.function.BiFunction;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;

public class VanillaBrewingRecipe implements IBrewingRecipe {
   private final PotionBrewing vanilla;
   private final BiFunction<ItemStack, ItemStack, ItemStack> vanillaMix;

   public VanillaBrewingRecipe(PotionBrewing vanilla, BiFunction<ItemStack, ItemStack, ItemStack> vanillaMix) {
      this.vanilla = vanilla;
      this.vanillaMix = vanillaMix;
   }

   @Override
   public boolean isInput(ItemStack stack) {
      return BrewingStandMenu.PotionSlot.mayPlaceItem(stack);
   }

   @Override
   public boolean isIngredient(ItemStack stack) {
      return this.vanilla.isContainerIngredient(stack) || this.vanilla.isPotionIngredient(stack);
   }

   @Override
   public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
      if (!input.isEmpty() && !ingredient.isEmpty() && this.isIngredient(ingredient)) {
         ItemStack result = this.vanillaMix.apply(ingredient, input);
         return result != input ? result : ItemStack.EMPTY;
      } else {
         return ItemStack.EMPTY;
      }
   }
}
