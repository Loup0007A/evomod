package net.minecraft.client.gui.screens.inventory;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StonecutterScreen extends AbstractContainerScreen<StonecutterMenu> {
   private static final ResourceLocation SCROLLER_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/scroller");
   private static final ResourceLocation SCROLLER_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/scroller_disabled");
   private static final ResourceLocation RECIPE_SELECTED_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe_selected");
   private static final ResourceLocation RECIPE_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe_highlighted");
   private static final ResourceLocation RECIPE_SPRITE = ResourceLocation.withDefaultNamespace("container/stonecutter/recipe");
   private static final ResourceLocation BG_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/stonecutter.png");
   private static final int SCROLLER_WIDTH = 12;
   private static final int SCROLLER_HEIGHT = 15;
   private static final int RECIPES_COLUMNS = 4;
   private static final int RECIPES_ROWS = 3;
   private static final int RECIPES_IMAGE_SIZE_WIDTH = 16;
   private static final int RECIPES_IMAGE_SIZE_HEIGHT = 18;
   private static final int SCROLLER_FULL_HEIGHT = 54;
   private static final int RECIPES_X = 52;
   private static final int RECIPES_Y = 14;
   private float scrollOffs;
   private boolean scrolling;
   private int startIndex;
   private boolean displayRecipes;

   public StonecutterScreen(StonecutterMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
      super(pMenu, pPlayerInventory, pTitle);
      pMenu.registerUpdateListener(this::containerChanged);
      this.titleLabelY--;
   }

   @Override
   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
   }

   @Override
   protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
      int $$4 = this.leftPos;
      int $$5 = this.topPos;
      pGuiGraphics.blit(BG_LOCATION, $$4, $$5, 0, 0, this.imageWidth, this.imageHeight);
      int $$6 = (int)(41.0F * this.scrollOffs);
      ResourceLocation $$7 = this.isScrollBarActive() ? SCROLLER_SPRITE : SCROLLER_DISABLED_SPRITE;
      pGuiGraphics.blitSprite($$7, $$4 + 119, $$5 + 15 + $$6, 12, 15);
      int $$8 = this.leftPos + 52;
      int $$9 = this.topPos + 14;
      int $$10 = this.startIndex + 12;
      this.renderButtons(pGuiGraphics, pMouseX, pMouseY, $$8, $$9, $$10);
      this.renderRecipes(pGuiGraphics, $$8, $$9, $$10);
   }

   @Override
   protected void renderTooltip(GuiGraphics pGuiGraphics, int pX, int pY) {
      super.renderTooltip(pGuiGraphics, pX, pY);
      if (this.displayRecipes) {
         int $$3 = this.leftPos + 52;
         int $$4 = this.topPos + 14;
         int $$5 = this.startIndex + 12;
         List<RecipeHolder<StonecutterRecipe>> $$6 = this.menu.getRecipes();

         for (int $$7 = this.startIndex; $$7 < $$5 && $$7 < this.menu.getNumRecipes(); $$7++) {
            int $$8 = $$7 - this.startIndex;
            int $$9 = $$3 + $$8 % 4 * 16;
            int $$10 = $$4 + $$8 / 4 * 18 + 2;
            if (pX >= $$9 && pX < $$9 + 16 && pY >= $$10 && pY < $$10 + 18) {
               pGuiGraphics.renderTooltip(this.font, $$6.get($$7).value().getResultItem(this.minecraft.level.registryAccess()), pX, pY);
            }
         }
      }
   }

   private void renderButtons(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, int pX, int pY, int pLastVisibleElementIndex) {
      for (int $$6 = this.startIndex; $$6 < pLastVisibleElementIndex && $$6 < this.menu.getNumRecipes(); $$6++) {
         int $$7 = $$6 - this.startIndex;
         int $$8 = pX + $$7 % 4 * 16;
         int $$9 = $$7 / 4;
         int $$10 = pY + $$9 * 18 + 2;
         ResourceLocation $$11;
         if ($$6 == this.menu.getSelectedRecipeIndex()) {
            $$11 = RECIPE_SELECTED_SPRITE;
         } else if (pMouseX >= $$8 && pMouseY >= $$10 && pMouseX < $$8 + 16 && pMouseY < $$10 + 18) {
            $$11 = RECIPE_HIGHLIGHTED_SPRITE;
         } else {
            $$11 = RECIPE_SPRITE;
         }

         pGuiGraphics.blitSprite($$11, $$8, $$10 - 1, 16, 18);
      }
   }

   private void renderRecipes(GuiGraphics pGuiGraphics, int pX, int pY, int pStartIndex) {
      List<RecipeHolder<StonecutterRecipe>> $$4 = this.menu.getRecipes();

      for (int $$5 = this.startIndex; $$5 < pStartIndex && $$5 < this.menu.getNumRecipes(); $$5++) {
         int $$6 = $$5 - this.startIndex;
         int $$7 = pX + $$6 % 4 * 16;
         int $$8 = $$6 / 4;
         int $$9 = pY + $$8 * 18 + 2;
         pGuiGraphics.renderItem($$4.get($$5).value().getResultItem(this.minecraft.level.registryAccess()), $$7, $$9);
      }
   }

   @Override
   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      this.scrolling = false;
      if (this.displayRecipes) {
         int $$3 = this.leftPos + 52;
         int $$4 = this.topPos + 14;
         int $$5 = this.startIndex + 12;

         for (int $$6 = this.startIndex; $$6 < $$5; $$6++) {
            int $$7 = $$6 - this.startIndex;
            double $$8 = pMouseX - (double)($$3 + $$7 % 4 * 16);
            double $$9 = pMouseY - (double)($$4 + $$7 / 4 * 18);
            if ($$8 >= 0.0 && $$9 >= 0.0 && $$8 < 16.0 && $$9 < 18.0 && this.menu.clickMenuButton(this.minecraft.player, $$6)) {
               Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
               this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, $$6);
               return true;
            }
         }

         $$3 = this.leftPos + 119;
         $$4 = this.topPos + 9;
         if (pMouseX >= (double)$$3 && pMouseX < (double)($$3 + 12) && pMouseY >= (double)$$4 && pMouseY < (double)($$4 + 54)) {
            this.scrolling = true;
         }
      }

      return super.mouseClicked(pMouseX, pMouseY, pButton);
   }

   @Override
   public boolean mouseDragged(double pMouseX, double pMouseY, int pButton, double pDragX, double pDragY) {
      if (this.scrolling && this.isScrollBarActive()) {
         int $$5 = this.topPos + 14;
         int $$6 = $$5 + 54;
         this.scrollOffs = ((float)pMouseY - (float)$$5 - 7.5F) / ((float)($$6 - $$5) - 15.0F);
         this.scrollOffs = Mth.clamp(this.scrollOffs, 0.0F, 1.0F);
         this.startIndex = (int)((double)(this.scrollOffs * (float)this.getOffscreenRows()) + 0.5) * 4;
         return true;
      } else {
         return super.mouseDragged(pMouseX, pMouseY, pButton, pDragX, pDragY);
      }
   }

   @Override
   public boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
      if (this.isScrollBarActive()) {
         int $$4 = this.getOffscreenRows();
         float $$5 = (float)pScrollY / (float)$$4;
         this.scrollOffs = Mth.clamp(this.scrollOffs - $$5, 0.0F, 1.0F);
         this.startIndex = (int)((double)(this.scrollOffs * (float)$$4) + 0.5) * 4;
      }

      return true;
   }

   private boolean isScrollBarActive() {
      return this.displayRecipes && this.menu.getNumRecipes() > 12;
   }

   protected int getOffscreenRows() {
      return (this.menu.getNumRecipes() + 4 - 1) / 4 - 3;
   }

   private void containerChanged() {
      this.displayRecipes = this.menu.hasInputItem();
      if (!this.displayRecipes) {
         this.scrollOffs = 0.0F;
         this.startIndex = 0;
      }
   }
}
