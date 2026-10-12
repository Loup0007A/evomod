package net.minecraft.client.gui.screens.inventory;

import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CartographyTableScreen extends AbstractContainerScreen<CartographyTableMenu> {
   private static final ResourceLocation ERROR_SPRITE = ResourceLocation.withDefaultNamespace("container/cartography_table/error");
   private static final ResourceLocation SCALED_MAP_SPRITE = ResourceLocation.withDefaultNamespace("container/cartography_table/scaled_map");
   private static final ResourceLocation DUPLICATED_MAP_SPRITE = ResourceLocation.withDefaultNamespace("container/cartography_table/duplicated_map");
   private static final ResourceLocation MAP_SPRITE = ResourceLocation.withDefaultNamespace("container/cartography_table/map");
   private static final ResourceLocation LOCKED_SPRITE = ResourceLocation.withDefaultNamespace("container/cartography_table/locked");
   private static final ResourceLocation BG_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/cartography_table.png");

   public CartographyTableScreen(CartographyTableMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
      super(pMenu, pPlayerInventory, pTitle);
      this.titleLabelY -= 2;
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
      ItemStack $$6 = this.menu.getSlot(1).getItem();
      boolean $$7 = $$6.is(Items.MAP);
      boolean $$8 = $$6.is(Items.PAPER);
      boolean $$9 = $$6.is(Items.GLASS_PANE);
      ItemStack $$10 = this.menu.getSlot(0).getItem();
      MapId $$11 = $$10.get(DataComponents.MAP_ID);
      boolean $$12 = false;
      MapItemSavedData $$13;
      if ($$11 != null) {
         $$13 = MapItem.getSavedData($$11, this.minecraft.level);
         if ($$13 != null) {
            if ($$13.locked) {
               $$12 = true;
               if ($$8 || $$9) {
                  pGuiGraphics.blitSprite(ERROR_SPRITE, $$4 + 35, $$5 + 31, 28, 21);
               }
            }

            if ($$8 && $$13.scale >= 4) {
               $$12 = true;
               pGuiGraphics.blitSprite(ERROR_SPRITE, $$4 + 35, $$5 + 31, 28, 21);
            }
         }
      } else {
         $$13 = null;
      }

      this.renderResultingMap(pGuiGraphics, $$11, $$13, $$7, $$8, $$9, $$12);
   }

   private void renderResultingMap(
      GuiGraphics pGuiGraphics,
      @Nullable MapId pMapId,
      @Nullable MapItemSavedData pMapData,
      boolean pHasMap,
      boolean pHasPaper,
      boolean pHasGlassPane,
      boolean pIsMaxSize
   ) {
      int $$7 = this.leftPos;
      int $$8 = this.topPos;
      if (pHasPaper && !pIsMaxSize) {
         pGuiGraphics.blitSprite(SCALED_MAP_SPRITE, $$7 + 67, $$8 + 13, 66, 66);
         this.renderMap(pGuiGraphics, pMapId, pMapData, $$7 + 85, $$8 + 31, 0.226F);
      } else if (pHasMap) {
         pGuiGraphics.blitSprite(DUPLICATED_MAP_SPRITE, $$7 + 67 + 16, $$8 + 13, 50, 66);
         this.renderMap(pGuiGraphics, pMapId, pMapData, $$7 + 86, $$8 + 16, 0.34F);
         pGuiGraphics.pose().pushPose();
         pGuiGraphics.pose().translate(0.0F, 0.0F, 1.0F);
         pGuiGraphics.blitSprite(DUPLICATED_MAP_SPRITE, $$7 + 67, $$8 + 13 + 16, 50, 66);
         this.renderMap(pGuiGraphics, pMapId, pMapData, $$7 + 70, $$8 + 32, 0.34F);
         pGuiGraphics.pose().popPose();
      } else if (pHasGlassPane) {
         pGuiGraphics.blitSprite(MAP_SPRITE, $$7 + 67, $$8 + 13, 66, 66);
         this.renderMap(pGuiGraphics, pMapId, pMapData, $$7 + 71, $$8 + 17, 0.45F);
         pGuiGraphics.pose().pushPose();
         pGuiGraphics.pose().translate(0.0F, 0.0F, 1.0F);
         pGuiGraphics.blitSprite(LOCKED_SPRITE, $$7 + 118, $$8 + 60, 10, 14);
         pGuiGraphics.pose().popPose();
      } else {
         pGuiGraphics.blitSprite(MAP_SPRITE, $$7 + 67, $$8 + 13, 66, 66);
         this.renderMap(pGuiGraphics, pMapId, pMapData, $$7 + 71, $$8 + 17, 0.45F);
      }
   }

   private void renderMap(GuiGraphics pGuiGraphics, @Nullable MapId pMapId, @Nullable MapItemSavedData pMapData, int pX, int pY, float pScale) {
      if (pMapId != null && pMapData != null) {
         pGuiGraphics.pose().pushPose();
         pGuiGraphics.pose().translate((float)pX, (float)pY, 1.0F);
         pGuiGraphics.pose().scale(pScale, pScale, 1.0F);
         this.minecraft.gameRenderer.getMapRenderer().render(pGuiGraphics.pose(), pGuiGraphics.bufferSource(), pMapId, pMapData, true, 15728880);
         pGuiGraphics.flush();
         pGuiGraphics.pose().popPose();
      }
   }
}
