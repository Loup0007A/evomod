package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GhostRecipe {
   @Nullable
   private RecipeHolder<?> recipe;
   private final List<GhostRecipe.GhostIngredient> ingredients = Lists.newArrayList();
   float time;

   public void clear() {
      this.recipe = null;
      this.ingredients.clear();
      this.time = 0.0F;
   }

   public void addIngredient(Ingredient pIngredient, int pX, int pY) {
      this.ingredients.add(new GhostRecipe.GhostIngredient(pIngredient, pX, pY));
   }

   public GhostRecipe.GhostIngredient get(int pIndex) {
      return this.ingredients.get(pIndex);
   }

   public int size() {
      return this.ingredients.size();
   }

   @Nullable
   public RecipeHolder<?> getRecipe() {
      return this.recipe;
   }

   public void setRecipe(RecipeHolder<?> pRecipe) {
      this.recipe = pRecipe;
   }

   public void render(GuiGraphics pGuiGraphics, Minecraft pMinecraft, int pLeftPos, int pTopPos, boolean pOffset, float pPartialTick) {
      if (!Screen.hasControlDown()) {
         this.time += pPartialTick;
      }

      for (int $$6 = 0; $$6 < this.ingredients.size(); $$6++) {
         GhostRecipe.GhostIngredient $$7 = this.ingredients.get($$6);
         int $$8 = $$7.getX() + pLeftPos;
         int $$9 = $$7.getY() + pTopPos;
         if ($$6 == 0 && pOffset) {
            pGuiGraphics.fill($$8 - 4, $$9 - 4, $$8 + 20, $$9 + 20, 822018048);
         } else {
            pGuiGraphics.fill($$8, $$9, $$8 + 16, $$9 + 16, 822018048);
         }

         ItemStack $$10 = $$7.getItem();
         pGuiGraphics.renderFakeItem($$10, $$8, $$9);
         pGuiGraphics.fill(RenderType.guiGhostRecipeOverlay(), $$8, $$9, $$8 + 16, $$9 + 16, 822083583);
         if ($$6 == 0) {
            pGuiGraphics.renderItemDecorations(pMinecraft.font, $$10, $$8, $$9);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class GhostIngredient {
      private final Ingredient ingredient;
      private final int x;
      private final int y;

      public GhostIngredient(final Ingredient pIngredient, final int pX, final int pY) {
         this.ingredient = pIngredient;
         this.x = pX;
         this.y = pY;
      }

      public int getX() {
         return this.x;
      }

      public int getY() {
         return this.y;
      }

      public ItemStack getItem() {
         ItemStack[] $$0 = this.ingredient.getItems();
         return $$0.length == 0 ? ItemStack.EMPTY : $$0[Mth.floor(GhostRecipe.this.time / 30.0F) % $$0.length];
      }
   }
}
