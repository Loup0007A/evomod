package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AnvilScreen extends ItemCombinerScreen<AnvilMenu> {
   private static final ResourceLocation TEXT_FIELD_SPRITE = ResourceLocation.withDefaultNamespace("container/anvil/text_field");
   private static final ResourceLocation TEXT_FIELD_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("container/anvil/text_field_disabled");
   private static final ResourceLocation ERROR_SPRITE = ResourceLocation.withDefaultNamespace("container/anvil/error");
   private static final ResourceLocation ANVIL_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/anvil.png");
   private static final Component TOO_EXPENSIVE_TEXT = Component.translatable("container.repair.expensive");
   private EditBox name;
   private final Player player;

   public AnvilScreen(AnvilMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
      super(pMenu, pPlayerInventory, pTitle, ANVIL_LOCATION);
      this.player = pPlayerInventory.player;
      this.titleLabelX = 60;
   }

   @Override
   protected void subInit() {
      int $$0 = (this.width - this.imageWidth) / 2;
      int $$1 = (this.height - this.imageHeight) / 2;
      this.name = new EditBox(this.font, $$0 + 62, $$1 + 24, 103, 12, Component.translatable("container.repair"));
      this.name.setCanLoseFocus(false);
      this.name.setTextColor(-1);
      this.name.setTextColorUneditable(-1);
      this.name.setBordered(false);
      this.name.setMaxLength(50);
      this.name.setResponder(this::onNameChanged);
      this.name.setValue("");
      this.addWidget(this.name);
      this.name.setEditable(this.menu.getSlot(0).hasItem());
   }

   @Override
   protected void setInitialFocus() {
      this.setInitialFocus(this.name);
   }

   @Override
   public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
      String $$3 = this.name.getValue();
      this.init(pMinecraft, pWidth, pHeight);
      this.name.setValue($$3);
   }

   @Override
   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.minecraft.player.closeContainer();
      }

      return !this.name.keyPressed(pKeyCode, pScanCode, pModifiers) && !this.name.canConsumeInput() ? super.keyPressed(pKeyCode, pScanCode, pModifiers) : true;
   }

   private void onNameChanged(String p_97899_) {
      Slot $$1 = this.menu.getSlot(0);
      if ($$1.hasItem()) {
         String $$2 = p_97899_;
         if (!$$1.getItem().has(DataComponents.CUSTOM_NAME) && p_97899_.equals($$1.getItem().getHoverName().getString())) {
            $$2 = "";
         }

         if (this.menu.setItemName($$2)) {
            this.minecraft.player.connection.send(new ServerboundRenameItemPacket($$2));
         }
      }
   }

   @Override
   protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
      super.renderLabels(pGuiGraphics, pMouseX, pMouseY);
      int $$3 = this.menu.getCost();
      if ($$3 > 0) {
         int $$4 = 8453920;
         Component $$5;
         if ($$3 >= 40 && !this.minecraft.player.getAbilities().instabuild) {
            $$5 = TOO_EXPENSIVE_TEXT;
            $$4 = 16736352;
         } else if (!this.menu.getSlot(2).hasItem()) {
            $$5 = null;
         } else {
            $$5 = Component.translatable("container.repair.cost", $$3);
            if (!this.menu.getSlot(2).mayPickup(this.player)) {
               $$4 = 16736352;
            }
         }

         if ($$5 != null) {
            int $$8 = this.imageWidth - 8 - this.font.width($$5) - 2;
            int $$9 = 69;
            pGuiGraphics.fill($$8 - 2, 67, this.imageWidth - 8, 79, 1325400064);
            pGuiGraphics.drawString(this.font, $$5, $$8, 69, $$4);
         }
      }
   }

   @Override
   protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
      super.renderBg(pGuiGraphics, pPartialTick, pMouseX, pMouseY);
      pGuiGraphics.blitSprite(this.menu.getSlot(0).hasItem() ? TEXT_FIELD_SPRITE : TEXT_FIELD_DISABLED_SPRITE, this.leftPos + 59, this.topPos + 20, 110, 16);
   }

   @Override
   public void renderFg(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
      this.name.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
   }

   @Override
   protected void renderErrorIcon(GuiGraphics pGuiGraphics, int pX, int pY) {
      if ((this.menu.getSlot(0).hasItem() || this.menu.getSlot(1).hasItem()) && !this.menu.getSlot(this.menu.getResultSlot()).hasItem()) {
         pGuiGraphics.blitSprite(ERROR_SPRITE, pX + 99, pY + 45, 28, 21);
      }
   }

   @Override
   public void slotChanged(AbstractContainerMenu pContainerToSend, int pSlotInd, ItemStack pStack) {
      if (pSlotInd == 0) {
         this.name.setValue(pStack.isEmpty() ? "" : pStack.getHoverName().getString());
         this.name.setEditable(!pStack.isEmpty());
         this.setFocused(this.name);
      }
   }
}
