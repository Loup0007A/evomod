package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.recipebook.PlaceRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OverlayRecipeComponent implements Renderable, GuiEventListener {
   private static final ResourceLocation OVERLAY_RECIPE_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/overlay_recipe");
   static final ResourceLocation FURNACE_OVERLAY_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/furnace_overlay_highlighted");
   static final ResourceLocation FURNACE_OVERLAY_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/furnace_overlay");
   static final ResourceLocation CRAFTING_OVERLAY_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay_highlighted");
   static final ResourceLocation CRAFTING_OVERLAY_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay");
   static final ResourceLocation FURNACE_OVERLAY_DISABLED_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace(
      "recipe_book/furnace_overlay_disabled_highlighted"
   );
   static final ResourceLocation FURNACE_OVERLAY_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/furnace_overlay_disabled");
   static final ResourceLocation CRAFTING_OVERLAY_DISABLED_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace(
      "recipe_book/crafting_overlay_disabled_highlighted"
   );
   static final ResourceLocation CRAFTING_OVERLAY_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay_disabled");
   private static final int MAX_ROW = 4;
   private static final int MAX_ROW_LARGE = 5;
   private static final float ITEM_RENDER_SCALE = 0.375F;
   public static final int BUTTON_SIZE = 25;
   private final List<OverlayRecipeComponent.OverlayRecipeButton> recipeButtons = Lists.newArrayList();
   private boolean isVisible;
   private int x;
   private int y;
   private Minecraft minecraft;
   private RecipeCollection collection;
   @Nullable
   private RecipeHolder<?> lastRecipeClicked;
   float time;
   boolean isFurnaceMenu;

   public void init(Minecraft pMinecraft, RecipeCollection pCollection, int pX, int pY, int p_100199_, int p_100200_, float p_100201_) {
      this.minecraft = pMinecraft;
      this.collection = pCollection;
      if (pMinecraft.player.containerMenu instanceof AbstractFurnaceMenu) {
         this.isFurnaceMenu = true;
      }

      boolean $$7 = pMinecraft.player.getRecipeBook().isFiltering((RecipeBookMenu<?, ?>)pMinecraft.player.containerMenu);
      List<RecipeHolder<?>> $$8 = pCollection.getDisplayRecipes(true);
      List<RecipeHolder<?>> $$9 = $$7 ? Collections.emptyList() : pCollection.getDisplayRecipes(false);
      int $$10 = $$8.size();
      int $$11 = $$10 + $$9.size();
      int $$12 = $$11 <= 16 ? 4 : 5;
      int $$13 = (int)Math.ceil((double)((float)$$11 / (float)$$12));
      this.x = pX;
      this.y = pY;
      float $$14 = (float)(this.x + Math.min($$11, $$12) * 25);
      float $$15 = (float)(p_100199_ + 50);
      if ($$14 > $$15) {
         this.x = (int)((float)this.x - p_100201_ * (float)((int)(($$14 - $$15) / p_100201_)));
      }

      float $$16 = (float)(this.y + $$13 * 25);
      float $$17 = (float)(p_100200_ + 50);
      if ($$16 > $$17) {
         this.y = (int)((float)this.y - p_100201_ * (float)Mth.ceil(($$16 - $$17) / p_100201_));
      }

      float $$18 = (float)this.y;
      float $$19 = (float)(p_100200_ - 100);
      if ($$18 < $$19) {
         this.y = (int)((float)this.y - p_100201_ * (float)Mth.ceil(($$18 - $$19) / p_100201_));
      }

      this.isVisible = true;
      this.recipeButtons.clear();

      for (int $$20 = 0; $$20 < $$11; $$20++) {
         boolean $$21 = $$20 < $$10;
         RecipeHolder<?> $$22 = $$21 ? $$8.get($$20) : $$9.get($$20 - $$10);
         int $$23 = this.x + 4 + 25 * ($$20 % $$12);
         int $$24 = this.y + 5 + 25 * ($$20 / $$12);
         if (this.isFurnaceMenu) {
            this.recipeButtons.add(new OverlayRecipeComponent.OverlaySmeltingRecipeButton($$23, $$24, $$22, $$21));
         } else {
            this.recipeButtons.add(new OverlayRecipeComponent.OverlayRecipeButton($$23, $$24, $$22, $$21));
         }
      }

      this.lastRecipeClicked = null;
   }

   public RecipeCollection getRecipeCollection() {
      return this.collection;
   }

   @Nullable
   public RecipeHolder<?> getLastRecipeClicked() {
      return this.lastRecipeClicked;
   }

   @Override
   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (pButton != 0) {
         return false;
      } else {
         for (OverlayRecipeComponent.OverlayRecipeButton $$3 : this.recipeButtons) {
            if ($$3.mouseClicked(pMouseX, pMouseY, pButton)) {
               this.lastRecipeClicked = $$3.recipe;
               return true;
            }
         }

         return false;
      }
   }

   @Override
   public boolean isMouseOver(double pMouseX, double pMouseY) {
      return false;
   }

   @Override
   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      if (this.isVisible) {
         this.time += pPartialTick;
         RenderSystem.enableBlend();
         pGuiGraphics.pose().pushPose();
         pGuiGraphics.pose().translate(0.0F, 0.0F, 1000.0F);
         int $$4 = this.recipeButtons.size() <= 16 ? 4 : 5;
         int $$5 = Math.min(this.recipeButtons.size(), $$4);
         int $$6 = Mth.ceil((float)this.recipeButtons.size() / (float)$$4);
         int $$7 = 4;
         pGuiGraphics.blitSprite(OVERLAY_RECIPE_SPRITE, this.x, this.y, $$5 * 25 + 8, $$6 * 25 + 8);
         RenderSystem.disableBlend();

         for (OverlayRecipeComponent.OverlayRecipeButton $$8 : this.recipeButtons) {
            $$8.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         }

         pGuiGraphics.pose().popPose();
      }
   }

   public void setVisible(boolean pIsVisible) {
      this.isVisible = pIsVisible;
   }

   public boolean isVisible() {
      return this.isVisible;
   }

   @Override
   public void setFocused(boolean pFocused) {
   }

   @Override
   public boolean isFocused() {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   class OverlayRecipeButton extends AbstractWidget implements PlaceRecipe<Ingredient> {
      final RecipeHolder<?> recipe;
      private final boolean isCraftable;
      protected final List<OverlayRecipeComponent.OverlayRecipeButton.Pos> ingredientPos = Lists.newArrayList();

      public OverlayRecipeButton(final int pX, final int pY, final RecipeHolder<?> pRecipe, final boolean pIsCraftable) {
         super(pX, pY, 200, 20, CommonComponents.EMPTY);
         this.width = 24;
         this.height = 24;
         this.recipe = pRecipe;
         this.isCraftable = pIsCraftable;
         this.calculateIngredientsPositions(pRecipe);
      }

      protected void calculateIngredientsPositions(RecipeHolder<?> pRecipe) {
         this.placeRecipe(3, 3, -1, pRecipe, pRecipe.value().getIngredients().iterator(), 0);
      }

      @Override
      public void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
         this.defaultButtonNarrationText(pNarrationElementOutput);
      }

      public void addItemToSlot(Ingredient pItem, int pSlot, int pMaxAmount, int pX, int pY) {
         ItemStack[] $$5 = pItem.getItems();
         if ($$5.length != 0) {
            this.ingredientPos.add(new OverlayRecipeComponent.OverlayRecipeButton.Pos(3 + pX * 7, 3 + pY * 7, $$5));
         }
      }

      @Override
      public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
         ResourceLocation $$4;
         if (this.isCraftable) {
            if (OverlayRecipeComponent.this.isFurnaceMenu) {
               $$4 = this.isHoveredOrFocused() ? OverlayRecipeComponent.FURNACE_OVERLAY_HIGHLIGHTED_SPRITE : OverlayRecipeComponent.FURNACE_OVERLAY_SPRITE;
            } else {
               $$4 = this.isHoveredOrFocused() ? OverlayRecipeComponent.CRAFTING_OVERLAY_HIGHLIGHTED_SPRITE : OverlayRecipeComponent.CRAFTING_OVERLAY_SPRITE;
            }
         } else if (OverlayRecipeComponent.this.isFurnaceMenu) {
            $$4 = this.isHoveredOrFocused()
               ? OverlayRecipeComponent.FURNACE_OVERLAY_DISABLED_HIGHLIGHTED_SPRITE
               : OverlayRecipeComponent.FURNACE_OVERLAY_DISABLED_SPRITE;
         } else {
            $$4 = this.isHoveredOrFocused()
               ? OverlayRecipeComponent.CRAFTING_OVERLAY_DISABLED_HIGHLIGHTED_SPRITE
               : OverlayRecipeComponent.CRAFTING_OVERLAY_DISABLED_SPRITE;
         }

         pGuiGraphics.blitSprite($$4, this.getX(), this.getY(), this.width, this.height);
         pGuiGraphics.pose().pushPose();
         pGuiGraphics.pose().translate((double)(this.getX() + 2), (double)(this.getY() + 2), 150.0);

         for (OverlayRecipeComponent.OverlayRecipeButton.Pos $$8 : this.ingredientPos) {
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().translate((double)$$8.x, (double)$$8.y, 0.0);
            pGuiGraphics.pose().scale(0.375F, 0.375F, 1.0F);
            pGuiGraphics.pose().translate(-8.0, -8.0, 0.0);
            if ($$8.ingredients.length > 0) {
               pGuiGraphics.renderItem($$8.ingredients[Mth.floor(OverlayRecipeComponent.this.time / 30.0F) % $$8.ingredients.length], 0, 0);
            }

            pGuiGraphics.pose().popPose();
         }

         pGuiGraphics.pose().popPose();
      }

      @OnlyIn(Dist.CLIENT)
      protected class Pos {
         public final ItemStack[] ingredients;
         public final int x;
         public final int y;

         public Pos(final int pX, final int pY, final ItemStack[] pIngredients) {
            this.x = pX;
            this.y = pY;
            this.ingredients = pIngredients;
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   class OverlaySmeltingRecipeButton extends OverlayRecipeComponent.OverlayRecipeButton {
      public OverlaySmeltingRecipeButton(final int p_100262_, final int p_100263_, final RecipeHolder<?> p_299173_, final boolean p_100265_) {
         super(p_100262_, p_100263_, p_299173_, p_100265_);
      }

      @Override
      protected void calculateIngredientsPositions(RecipeHolder<?> p_301296_) {
         Ingredient $$1 = p_301296_.value().getIngredients().get(0);
         ItemStack[] $$2 = $$1.getItems();
         this.ingredientPos.add(new OverlayRecipeComponent.OverlayRecipeButton.Pos(10, 10, $$2));
      }
   }
}
