package net.minecraft.world.item.crafting;

import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public class MapExtendingRecipe extends ShapedRecipe {
   public MapExtendingRecipe(CraftingBookCategory pCategory) {
      super(
         "",
         pCategory,
         ShapedRecipePattern.of(Map.of('#', Ingredient.of(Items.PAPER), 'x', Ingredient.of(Items.FILLED_MAP)), "###", "#x#", "###"),
         new ItemStack(Items.MAP)
      );
   }

   @Override
   public boolean matches(CraftingInput pInput, Level pLevel) {
      if (!super.matches(pInput, pLevel)) {
         return false;
      } else {
         ItemStack $$2 = findFilledMap(pInput);
         if ($$2.isEmpty()) {
            return false;
         } else {
            MapItemSavedData $$3 = MapItem.getSavedData($$2, pLevel);
            if ($$3 == null) {
               return false;
            } else {
               return $$3.isExplorationMap() ? false : $$3.scale < 4;
            }
         }
      }
   }

   @Override
   public ItemStack assemble(CraftingInput pInput, HolderLookup.Provider pRegistries) {
      ItemStack $$2 = findFilledMap(pInput).copyWithCount(1);
      $$2.set(DataComponents.MAP_POST_PROCESSING, MapPostProcessing.SCALE);
      return $$2;
   }

   private static ItemStack findFilledMap(CraftingInput pInput) {
      for (int $$1 = 0; $$1 < pInput.size(); $$1++) {
         ItemStack $$2 = pInput.getItem($$1);
         if ($$2.is(Items.FILLED_MAP)) {
            return $$2;
         }
      }

      return ItemStack.EMPTY;
   }

   @Override
   public boolean isSpecial() {
      return true;
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.MAP_EXTENDING;
   }
}
