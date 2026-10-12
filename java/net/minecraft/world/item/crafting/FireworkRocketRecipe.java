package net.minecraft.world.item.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;

public class FireworkRocketRecipe extends CustomRecipe {
   private static final Ingredient PAPER_INGREDIENT = Ingredient.of(Items.PAPER);
   private static final Ingredient GUNPOWDER_INGREDIENT = Ingredient.of(Items.GUNPOWDER);
   private static final Ingredient STAR_INGREDIENT = Ingredient.of(Items.FIREWORK_STAR);

   public FireworkRocketRecipe(CraftingBookCategory pCategory) {
      super(pCategory);
   }

   public boolean matches(CraftingInput pInput, Level pLevel) {
      boolean $$2 = false;
      int $$3 = 0;

      for (int $$4 = 0; $$4 < pInput.size(); $$4++) {
         ItemStack $$5 = pInput.getItem($$4);
         if (!$$5.isEmpty()) {
            if (PAPER_INGREDIENT.test($$5)) {
               if ($$2) {
                  return false;
               }

               $$2 = true;
            } else if (GUNPOWDER_INGREDIENT.test($$5)) {
               if (++$$3 > 3) {
                  return false;
               }
            } else if (!STAR_INGREDIENT.test($$5)) {
               return false;
            }
         }
      }

      return $$2 && $$3 >= 1;
   }

   public ItemStack assemble(CraftingInput pInput, HolderLookup.Provider pRegistries) {
      List<FireworkExplosion> $$2 = new ArrayList<>();
      int $$3 = 0;

      for (int $$4 = 0; $$4 < pInput.size(); $$4++) {
         ItemStack $$5 = pInput.getItem($$4);
         if (!$$5.isEmpty()) {
            if (GUNPOWDER_INGREDIENT.test($$5)) {
               $$3++;
            } else if (STAR_INGREDIENT.test($$5)) {
               FireworkExplosion $$6 = $$5.get(DataComponents.FIREWORK_EXPLOSION);
               if ($$6 != null) {
                  $$2.add($$6);
               }
            }
         }
      }

      ItemStack $$7 = new ItemStack(Items.FIREWORK_ROCKET, 3);
      $$7.set(DataComponents.FIREWORKS, new Fireworks($$3, $$2));
      return $$7;
   }

   @Override
   public boolean canCraftInDimensions(int pWidth, int pHeight) {
      return pWidth * pHeight >= 2;
   }

   @Override
   public ItemStack getResultItem(HolderLookup.Provider pRegistries) {
      return new ItemStack(Items.FIREWORK_ROCKET);
   }

   @Override
   public RecipeSerializer<?> getSerializer() {
      return RecipeSerializer.FIREWORK_ROCKET;
   }
}
