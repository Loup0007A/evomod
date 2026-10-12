package net.minecraft.client.gui.screens;

import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CreateFlatWorldScreen extends Screen {
   static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");
   private static final int SLOT_BG_SIZE = 18;
   private static final int SLOT_STAT_HEIGHT = 20;
   private static final int SLOT_BG_X = 1;
   private static final int SLOT_BG_Y = 1;
   private static final int SLOT_FG_X = 2;
   private static final int SLOT_FG_Y = 2;
   protected final CreateWorldScreen parent;
   private final Consumer<FlatLevelGeneratorSettings> applySettings;
   FlatLevelGeneratorSettings generator;
   private Component columnType;
   private Component columnHeight;
   private CreateFlatWorldScreen.DetailsList list;
   private Button deleteLayerButton;

   public CreateFlatWorldScreen(CreateWorldScreen pParent, Consumer<FlatLevelGeneratorSettings> pApplySettings, FlatLevelGeneratorSettings pGenerator) {
      super(Component.translatable("createWorld.customize.flat.title"));
      this.parent = pParent;
      this.applySettings = pApplySettings;
      this.generator = pGenerator;
   }

   public FlatLevelGeneratorSettings settings() {
      return this.generator;
   }

   public void setConfig(FlatLevelGeneratorSettings pGenerator) {
      this.generator = pGenerator;
   }

   @Override
   protected void init() {
      this.columnType = Component.translatable("createWorld.customize.flat.tile");
      this.columnHeight = Component.translatable("createWorld.customize.flat.height");
      this.list = this.addRenderableWidget(new CreateFlatWorldScreen.DetailsList());
      this.deleteLayerButton = this.addRenderableWidget(Button.builder(Component.translatable("createWorld.customize.flat.removeLayer"), p_95845_ -> {
         if (this.hasValidSelection()) {
            List<FlatLayerInfo> $$1 = this.generator.getLayersInfo();
            int $$2 = this.list.children().indexOf(this.list.getSelected());
            int $$3 = $$1.size() - $$2 - 1;
            $$1.remove($$3);
            this.list.setSelected($$1.isEmpty() ? null : this.list.children().get(Math.min($$2, $$1.size() - 1)));
            this.generator.updateLayers();
            this.list.resetRows();
            this.updateButtonValidity();
         }
      }).bounds(this.width / 2 - 155, this.height - 52, 150, 20).build());
      this.addRenderableWidget(Button.builder(Component.translatable("createWorld.customize.presets"), p_280790_ -> {
         this.minecraft.setScreen(new PresetFlatWorldScreen(this));
         this.generator.updateLayers();
         this.updateButtonValidity();
      }).bounds(this.width / 2 + 5, this.height - 52, 150, 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, p_280791_ -> {
         this.applySettings.accept(this.generator);
         this.minecraft.setScreen(this.parent);
         this.generator.updateLayers();
      }).bounds(this.width / 2 - 155, this.height - 28, 150, 20).build());
      this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, p_280792_ -> {
         this.minecraft.setScreen(this.parent);
         this.generator.updateLayers();
      }).bounds(this.width / 2 + 5, this.height - 28, 150, 20).build());
      this.generator.updateLayers();
      this.updateButtonValidity();
   }

   void updateButtonValidity() {
      this.deleteLayerButton.active = this.hasValidSelection();
   }

   private boolean hasValidSelection() {
      return this.list.getSelected() != null;
   }

   @Override
   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   @Override
   public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
      pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 16777215);
      int $$4 = this.width / 2 - 92 - 16;
      pGuiGraphics.drawString(this.font, this.columnType, $$4, 32, 16777215);
      pGuiGraphics.drawString(this.font, this.columnHeight, $$4 + 2 + 213 - this.font.width(this.columnHeight), 32, 16777215);
   }

   @OnlyIn(Dist.CLIENT)
   class DetailsList extends ObjectSelectionList<CreateFlatWorldScreen.DetailsList.Entry> {
      public DetailsList() {
         super(CreateFlatWorldScreen.this.minecraft, CreateFlatWorldScreen.this.width, CreateFlatWorldScreen.this.height - 103, 43, 24);

         for (int $$0 = 0; $$0 < CreateFlatWorldScreen.this.generator.getLayersInfo().size(); $$0++) {
            this.addEntry(new CreateFlatWorldScreen.DetailsList.Entry());
         }
      }

      public void setSelected(@Nullable CreateFlatWorldScreen.DetailsList.Entry pEntry) {
         super.setSelected(pEntry);
         CreateFlatWorldScreen.this.updateButtonValidity();
      }

      public void resetRows() {
         int $$0 = this.children().indexOf(this.getSelected());
         this.clearEntries();

         for (int $$1 = 0; $$1 < CreateFlatWorldScreen.this.generator.getLayersInfo().size(); $$1++) {
            this.addEntry(new CreateFlatWorldScreen.DetailsList.Entry());
         }

         List<CreateFlatWorldScreen.DetailsList.Entry> $$2 = this.children();
         if ($$0 >= 0 && $$0 < $$2.size()) {
            this.setSelected($$2.get($$0));
         }
      }

      @OnlyIn(Dist.CLIENT)
      class Entry extends ObjectSelectionList.Entry<CreateFlatWorldScreen.DetailsList.Entry> {
         @Override
         public void render(
            GuiGraphics pGuiGraphics, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pHovering, float pPartialTick
         ) {
            FlatLayerInfo $$10 = CreateFlatWorldScreen.this.generator
               .getLayersInfo()
               .get(CreateFlatWorldScreen.this.generator.getLayersInfo().size() - pIndex - 1);
            BlockState $$11 = $$10.getBlockState();
            ItemStack $$12 = this.getDisplayItem($$11);
            this.blitSlot(pGuiGraphics, pLeft, pTop, $$12);
            pGuiGraphics.drawString(CreateFlatWorldScreen.this.font, $$12.getHoverName(), pLeft + 18 + 5, pTop + 3, 16777215, false);
            Component $$13;
            if (pIndex == 0) {
               $$13 = Component.translatable("createWorld.customize.flat.layer.top", $$10.getHeight());
            } else if (pIndex == CreateFlatWorldScreen.this.generator.getLayersInfo().size() - 1) {
               $$13 = Component.translatable("createWorld.customize.flat.layer.bottom", $$10.getHeight());
            } else {
               $$13 = Component.translatable("createWorld.customize.flat.layer", $$10.getHeight());
            }

            pGuiGraphics.drawString(
               CreateFlatWorldScreen.this.font, $$13, pLeft + 2 + 213 - CreateFlatWorldScreen.this.font.width($$13), pTop + 3, 16777215, false
            );
         }

         private ItemStack getDisplayItem(BlockState pState) {
            Item $$1 = pState.getBlock().asItem();
            if ($$1 == Items.AIR) {
               if (pState.is(Blocks.WATER)) {
                  $$1 = Items.WATER_BUCKET;
               } else if (pState.is(Blocks.LAVA)) {
                  $$1 = Items.LAVA_BUCKET;
               }
            }

            return new ItemStack($$1);
         }

         @Override
         public Component getNarration() {
            FlatLayerInfo $$0 = CreateFlatWorldScreen.this.generator
               .getLayersInfo()
               .get(CreateFlatWorldScreen.this.generator.getLayersInfo().size() - DetailsList.this.children().indexOf(this) - 1);
            ItemStack $$1 = this.getDisplayItem($$0.getBlockState());
            return (Component)(!$$1.isEmpty() ? Component.translatable("narrator.select", $$1.getHoverName()) : CommonComponents.EMPTY);
         }

         @Override
         public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            DetailsList.this.setSelected(this);
            return super.mouseClicked(pMouseX, pMouseY, pButton);
         }

         private void blitSlot(GuiGraphics pGuiGraphics, int pX, int pY, ItemStack pStack) {
            this.blitSlotBg(pGuiGraphics, pX + 1, pY + 1);
            if (!pStack.isEmpty()) {
               pGuiGraphics.renderFakeItem(pStack, pX + 2, pY + 2);
            }
         }

         private void blitSlotBg(GuiGraphics pGuiGraphics, int pX, int pY) {
            pGuiGraphics.blitSprite(CreateFlatWorldScreen.SLOT_SPRITE, pX, pY, 0, 18, 18);
         }
      }
   }
}
