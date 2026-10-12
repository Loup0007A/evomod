package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBook;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeButton extends AbstractWidget {
   private static final ResourceLocation SLOT_MANY_CRAFTABLE_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/slot_many_craftable");
   private static final ResourceLocation SLOT_CRAFTABLE_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/slot_craftable");
   private static final ResourceLocation SLOT_MANY_UNCRAFTABLE_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/slot_many_uncraftable");
   private static final ResourceLocation SLOT_UNCRAFTABLE_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/slot_uncraftable");
   private static final float ANIMATION_TIME = 15.0F;
   private static final int BACKGROUND_SIZE = 25;
   public static final int TICKS_TO_SWAP = 30;
   private static final Component MORE_RECIPES_TOOLTIP = Component.translatable("gui.recipebook.moreRecipes");
   private RecipeBookMenu<?, ?> menu;
   private RecipeBook book;
   private RecipeCollection collection;
   private float time;
   private float animationTime;
   private int currentIndex;

   public RecipeButton() {
      super(0, 0, 25, 25, CommonComponents.EMPTY);
   }

   public void init(RecipeCollection pCollection, RecipeBookPage pRecipeBookPage) {
      this.collection = pCollection;
      this.menu = (RecipeBookMenu<?, ?>)pRecipeBookPage.getMinecraft().player.containerMenu;
      this.book = pRecipeBookPage.getRecipeBook();
      List<RecipeHolder<?>> $$2 = pCollection.getRecipes(this.book.isFiltering(this.menu));

      for (RecipeHolder<?> $$3 : $$2) {
         if (this.book.willHighlight($$3)) {
            pRecipeBookPage.recipesShown($$2);
            this.animationTime = 15.0F;
            break;
         }
      }
   }

   public RecipeCollection getCollection() {
      return this.collection;
   }

   @Override
   public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      if (!Screen.hasControlDown()) {
         this.time += pPartialTick;
      }

      ResourceLocation $$4;
      if (this.collection.hasCraftable()) {
         if (this.collection.getRecipes(this.book.isFiltering(this.menu)).size() > 1) {
            $$4 = SLOT_MANY_CRAFTABLE_SPRITE;
         } else {
            $$4 = SLOT_CRAFTABLE_SPRITE;
         }
      } else if (this.collection.getRecipes(this.book.isFiltering(this.menu)).size() > 1) {
         $$4 = SLOT_MANY_UNCRAFTABLE_SPRITE;
      } else {
         $$4 = SLOT_UNCRAFTABLE_SPRITE;
      }

      boolean $$8 = this.animationTime > 0.0F;
      if ($$8) {
         float $$9 = 1.0F + 0.1F * (float)Math.sin((double)(this.animationTime / 15.0F * (float) Math.PI));
         pGuiGraphics.pose().pushPose();
         pGuiGraphics.pose().translate((float)(this.getX() + 8), (float)(this.getY() + 12), 0.0F);
         pGuiGraphics.pose().scale($$9, $$9, 1.0F);
         pGuiGraphics.pose().translate((float)(-(this.getX() + 8)), (float)(-(this.getY() + 12)), 0.0F);
         this.animationTime -= pPartialTick;
      }

      pGuiGraphics.blitSprite($$4, this.getX(), this.getY(), this.width, this.height);
      List<RecipeHolder<?>> $$10 = this.getOrderedRecipes();
      this.currentIndex = Mth.floor(this.time / 30.0F) % $$10.size();
      ItemStack $$11 = $$10.get(this.currentIndex).value().getResultItem(this.collection.registryAccess());
      int $$12 = 4;
      if (this.collection.hasSingleResultItem() && this.getOrderedRecipes().size() > 1) {
         pGuiGraphics.renderItem($$11, this.getX() + $$12 + 1, this.getY() + $$12 + 1, 0, 10);
         $$12--;
      }

      pGuiGraphics.renderFakeItem($$11, this.getX() + $$12, this.getY() + $$12);
      if ($$8) {
         pGuiGraphics.pose().popPose();
      }
   }

   private List<RecipeHolder<?>> getOrderedRecipes() {
      List<RecipeHolder<?>> $$0 = this.collection.getDisplayRecipes(true);
      if (!this.book.isFiltering(this.menu)) {
         $$0.addAll(this.collection.getDisplayRecipes(false));
      }

      return $$0;
   }

   public boolean isOnlyOption() {
      return this.getOrderedRecipes().size() == 1;
   }

   public RecipeHolder<?> getRecipe() {
      List<RecipeHolder<?>> $$0 = this.getOrderedRecipes();
      return $$0.get(this.currentIndex);
   }

   public List<Component> getTooltipText() {
      ItemStack $$0 = this.getOrderedRecipes().get(this.currentIndex).value().getResultItem(this.collection.registryAccess());
      List<Component> $$1 = Lists.newArrayList(Screen.getTooltipFromItem(Minecraft.getInstance(), $$0));
      if (this.collection.getRecipes(this.book.isFiltering(this.menu)).size() > 1) {
         $$1.add(MORE_RECIPES_TOOLTIP);
      }

      return $$1;
   }

   @Override
   public void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
      ItemStack $$1 = this.getOrderedRecipes().get(this.currentIndex).value().getResultItem(this.collection.registryAccess());
      pNarrationElementOutput.add(NarratedElementType.TITLE, Component.translatable("narration.recipe", $$1.getHoverName()));
      if (this.collection.getRecipes(this.book.isFiltering(this.menu)).size() > 1) {
         pNarrationElementOutput.add(
            NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"), Component.translatable("narration.recipe.usage.more")
         );
      } else {
         pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
      }
   }

   @Override
   public int getWidth() {
      return 25;
   }

   @Override
   protected boolean isValidClickButton(int pButton) {
      return pButton == 0 || pButton == 1;
   }
}
