package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeBookPage {
   public static final int ITEMS_PER_PAGE = 20;
   private static final WidgetSprites PAGE_FORWARD_SPRITES = new WidgetSprites(
      ResourceLocation.withDefaultNamespace("recipe_book/page_forward"), ResourceLocation.withDefaultNamespace("recipe_book/page_forward_highlighted")
   );
   private static final WidgetSprites PAGE_BACKWARD_SPRITES = new WidgetSprites(
      ResourceLocation.withDefaultNamespace("recipe_book/page_backward"), ResourceLocation.withDefaultNamespace("recipe_book/page_backward_highlighted")
   );
   private final List<RecipeButton> buttons = Lists.newArrayListWithCapacity(20);
   @Nullable
   private RecipeButton hoveredButton;
   private final OverlayRecipeComponent overlay = new OverlayRecipeComponent();
   private Minecraft minecraft;
   private final List<RecipeShownListener> showListeners = Lists.newArrayList();
   private List<RecipeCollection> recipeCollections = ImmutableList.of();
   private StateSwitchingButton forwardButton;
   private StateSwitchingButton backButton;
   private int totalPages;
   private int currentPage;
   private RecipeBook recipeBook;
   @Nullable
   private RecipeHolder<?> lastClickedRecipe;
   @Nullable
   private RecipeCollection lastClickedRecipeCollection;

   public RecipeBookPage() {
      for (int $$0 = 0; $$0 < 20; $$0++) {
         this.buttons.add(new RecipeButton());
      }
   }

   public void init(Minecraft pMinecraft, int pX, int pY) {
      this.minecraft = pMinecraft;
      this.recipeBook = pMinecraft.player.getRecipeBook();

      for (int $$3 = 0; $$3 < this.buttons.size(); $$3++) {
         this.buttons.get($$3).setPosition(pX + 11 + 25 * ($$3 % 5), pY + 31 + 25 * ($$3 / 5));
      }

      this.forwardButton = new StateSwitchingButton(pX + 93, pY + 137, 12, 17, false);
      this.forwardButton.initTextureValues(PAGE_FORWARD_SPRITES);
      this.backButton = new StateSwitchingButton(pX + 38, pY + 137, 12, 17, true);
      this.backButton.initTextureValues(PAGE_BACKWARD_SPRITES);
   }

   public void addListener(RecipeBookComponent pListener) {
      this.showListeners.remove(pListener);
      this.showListeners.add(pListener);
   }

   public void updateCollections(List<RecipeCollection> pRecipeCollections, boolean pResetPageNumber) {
      this.recipeCollections = pRecipeCollections;
      this.totalPages = (int)Math.ceil((double)pRecipeCollections.size() / 20.0);
      if (this.totalPages <= this.currentPage || pResetPageNumber) {
         this.currentPage = 0;
      }

      this.updateButtonsForPage();
   }

   private void updateButtonsForPage() {
      int $$0 = 20 * this.currentPage;

      for (int $$1 = 0; $$1 < this.buttons.size(); $$1++) {
         RecipeButton $$2 = this.buttons.get($$1);
         if ($$0 + $$1 < this.recipeCollections.size()) {
            RecipeCollection $$3 = this.recipeCollections.get($$0 + $$1);
            $$2.init($$3, this);
            $$2.visible = true;
         } else {
            $$2.visible = false;
         }
      }

      this.updateArrowButtons();
   }

   private void updateArrowButtons() {
      this.forwardButton.visible = this.totalPages > 1 && this.currentPage < this.totalPages - 1;
      this.backButton.visible = this.totalPages > 1 && this.currentPage > 0;
   }

   public void render(GuiGraphics pGuiGraphics, int pX, int pY, int pMouseX, int pMouseY, float pPartialTick) {
      if (this.totalPages > 1) {
         Component $$6 = Component.translatable("gui.recipebook.page", this.currentPage + 1, this.totalPages);
         int $$7 = this.minecraft.font.width($$6);
         pGuiGraphics.drawString(this.minecraft.font, $$6, pX - $$7 / 2 + 73, pY + 141, -1, false);
      }

      this.hoveredButton = null;

      for (RecipeButton $$8 : this.buttons) {
         $$8.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
         if ($$8.visible && $$8.isHoveredOrFocused()) {
            this.hoveredButton = $$8;
         }
      }

      this.backButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      this.forwardButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      this.overlay.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
   }

   public void renderTooltip(GuiGraphics pGuiGraphics, int pX, int pY) {
      if (this.minecraft.screen != null && this.hoveredButton != null && !this.overlay.isVisible()) {
         pGuiGraphics.renderComponentTooltip(this.minecraft.font, this.hoveredButton.getTooltipText(), pX, pY);
      }
   }

   @Nullable
   public RecipeHolder<?> getLastClickedRecipe() {
      return this.lastClickedRecipe;
   }

   @Nullable
   public RecipeCollection getLastClickedRecipeCollection() {
      return this.lastClickedRecipeCollection;
   }

   public void setInvisible() {
      this.overlay.setVisible(false);
   }

   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton, int p_100413_, int p_100414_, int p_100415_, int p_100416_) {
      this.lastClickedRecipe = null;
      this.lastClickedRecipeCollection = null;
      if (this.overlay.isVisible()) {
         if (this.overlay.mouseClicked(pMouseX, pMouseY, pButton)) {
            this.lastClickedRecipe = this.overlay.getLastRecipeClicked();
            this.lastClickedRecipeCollection = this.overlay.getRecipeCollection();
         } else {
            this.overlay.setVisible(false);
         }

         return true;
      } else if (this.forwardButton.mouseClicked(pMouseX, pMouseY, pButton)) {
         this.currentPage++;
         this.updateButtonsForPage();
         return true;
      } else if (this.backButton.mouseClicked(pMouseX, pMouseY, pButton)) {
         this.currentPage--;
         this.updateButtonsForPage();
         return true;
      } else {
         for (RecipeButton $$7 : this.buttons) {
            if ($$7.mouseClicked(pMouseX, pMouseY, pButton)) {
               if (pButton == 0) {
                  this.lastClickedRecipe = $$7.getRecipe();
                  this.lastClickedRecipeCollection = $$7.getCollection();
               } else if (pButton == 1 && !this.overlay.isVisible() && !$$7.isOnlyOption()) {
                  this.overlay
                     .init(
                        this.minecraft,
                        $$7.getCollection(),
                        $$7.getX(),
                        $$7.getY(),
                        p_100413_ + p_100415_ / 2,
                        p_100414_ + 13 + p_100416_ / 2,
                        (float)$$7.getWidth()
                     );
               }

               return true;
            }
         }

         return false;
      }
   }

   public void recipesShown(List<RecipeHolder<?>> pRecipes) {
      for (RecipeShownListener $$1 : this.showListeners) {
         $$1.recipesShown(pRecipes);
      }
   }

   public Minecraft getMinecraft() {
      return this.minecraft;
   }

   public RecipeBook getRecipeBook() {
      return this.recipeBook;
   }

   protected void listButtons(Consumer<AbstractWidget> pConsumer) {
      pConsumer.accept(this.forwardButton);
      pConsumer.accept(this.backButton);
      this.buttons.forEach(pConsumer);
   }
}
