package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.recipebook.AbstractFurnaceRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractFurnaceScreen<T extends AbstractFurnaceMenu> extends AbstractContainerScreen<T> implements RecipeUpdateListener {
   public final AbstractFurnaceRecipeBookComponent recipeBookComponent;
   private boolean widthTooNarrow;
   private final ResourceLocation texture;
   private final ResourceLocation litProgressSprite;
   private final ResourceLocation burnProgressSprite;

   public AbstractFurnaceScreen(
      T pMenu,
      AbstractFurnaceRecipeBookComponent pRecipeBookComponent,
      Inventory pPlayerInventory,
      Component pTitle,
      ResourceLocation pTexture,
      ResourceLocation pListProgressSprite,
      ResourceLocation pBurnProgressSprite
   ) {
      super(pMenu, pPlayerInventory, pTitle);
      this.recipeBookComponent = pRecipeBookComponent;
      this.texture = pTexture;
      this.litProgressSprite = pListProgressSprite;
      this.burnProgressSprite = pBurnProgressSprite;
   }

   @Override
   public void init() {
      super.init();
      this.widthTooNarrow = this.width < 379;
      this.recipeBookComponent.init(this.width, this.height, this.minecraft, this.widthTooNarrow, this.menu);
      this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
      this.addRenderableWidget(new ImageButton(this.leftPos + 20, this.height / 2 - 49, 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES, p_308201_ -> {
         this.recipeBookComponent.toggleVisibility();
         this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
         p_308201_.setPosition(this.leftPos + 20, this.height / 2 - 49);
      }));
      this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
   }

   @Override
   public void containerTick() {
      super.containerTick();
      this.recipeBookComponent.tick();
   }

   @Override
   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      if (this.recipeBookComponent.isVisible() && this.widthTooNarrow) {
         this.renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         this.recipeBookComponent.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      } else {
         super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         this.recipeBookComponent.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         this.recipeBookComponent.renderGhostRecipe(pGuiGraphics, this.leftPos, this.topPos, true, pPartialTick);
      }

      this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
      this.recipeBookComponent.renderTooltip(pGuiGraphics, this.leftPos, this.topPos, pMouseX, pMouseY);
   }

   @Override
   protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
      int $$4 = this.leftPos;
      int $$5 = this.topPos;
      pGuiGraphics.blit(this.texture, $$4, $$5, 0, 0, this.imageWidth, this.imageHeight);
      if (this.menu.isLit()) {
         int $$6 = 14;
         int $$7 = Mth.ceil(this.menu.getLitProgress() * 13.0F) + 1;
         pGuiGraphics.blitSprite(this.litProgressSprite, 14, 14, 0, 14 - $$7, $$4 + 56, $$5 + 36 + 14 - $$7, 14, $$7);
      }

      int $$8 = 24;
      int $$9 = Mth.ceil(this.menu.getBurnProgress() * 24.0F);
      pGuiGraphics.blitSprite(this.burnProgressSprite, 24, 16, 0, 0, $$4 + 79, $$5 + 34, $$9, 16);
   }

   @Override
   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (this.recipeBookComponent.mouseClicked(pMouseX, pMouseY, pButton)) {
         return true;
      } else {
         return this.widthTooNarrow && this.recipeBookComponent.isVisible() ? true : super.mouseClicked(pMouseX, pMouseY, pButton);
      }
   }

   @Override
   protected void slotClicked(Slot pSlot, int pSlotId, int pMouseButton, ClickType pType) {
      super.slotClicked(pSlot, pSlotId, pMouseButton, pType);
      this.recipeBookComponent.slotClicked(pSlot);
   }

   @Override
   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      return this.recipeBookComponent.keyPressed(pKeyCode, pScanCode, pModifiers) ? true : super.keyPressed(pKeyCode, pScanCode, pModifiers);
   }

   @Override
   protected boolean hasClickedOutside(double pMouseX, double pMouseY, int pGuiLeft, int pGuiTop, int pMouseButton) {
      boolean $$5 = pMouseX < (double)pGuiLeft
         || pMouseY < (double)pGuiTop
         || pMouseX >= (double)(pGuiLeft + this.imageWidth)
         || pMouseY >= (double)(pGuiTop + this.imageHeight);
      return this.recipeBookComponent.hasClickedOutside(pMouseX, pMouseY, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, pMouseButton) && $$5;
   }

   @Override
   public boolean charTyped(char pCodePoint, int pModifiers) {
      return this.recipeBookComponent.charTyped(pCodePoint, pModifiers) ? true : super.charTyped(pCodePoint, pModifiers);
   }

   @Override
   public void recipesUpdated() {
      this.recipeBookComponent.recipesUpdated();
   }

   @Override
   public RecipeBookComponent getRecipeBookComponent() {
      return this.recipeBookComponent;
   }
}
