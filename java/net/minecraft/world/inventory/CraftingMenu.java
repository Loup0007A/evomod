package net.minecraft.world.inventory;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class CraftingMenu extends RecipeBookMenu<CraftingInput, CraftingRecipe> {
   public static final int RESULT_SLOT = 0;
   private static final int CRAFT_SLOT_START = 1;
   private static final int CRAFT_SLOT_END = 10;
   private static final int INV_SLOT_START = 10;
   private static final int INV_SLOT_END = 37;
   private static final int USE_ROW_SLOT_START = 37;
   private static final int USE_ROW_SLOT_END = 46;
   private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
   private final ResultContainer resultSlots = new ResultContainer();
   private final ContainerLevelAccess access;
   private final Player player;
   private boolean placingRecipe;

   public CraftingMenu(int pContainerId, Inventory pPlayerInventory) {
      this(pContainerId, pPlayerInventory, ContainerLevelAccess.NULL);
   }

   public CraftingMenu(int pContainerId, Inventory pPlayerInventory, ContainerLevelAccess pAccess) {
      super(MenuType.CRAFTING, pContainerId);
      this.access = pAccess;
      this.player = pPlayerInventory.player;
      this.addSlot(new ResultSlot(pPlayerInventory.player, this.craftSlots, this.resultSlots, 0, 124, 35));

      for (int $$3 = 0; $$3 < 3; $$3++) {
         for (int $$4 = 0; $$4 < 3; $$4++) {
            this.addSlot(new Slot(this.craftSlots, $$4 + $$3 * 3, 30 + $$4 * 18, 17 + $$3 * 18));
         }
      }

      for (int $$5 = 0; $$5 < 3; $$5++) {
         for (int $$6 = 0; $$6 < 9; $$6++) {
            this.addSlot(new Slot(pPlayerInventory, $$6 + $$5 * 9 + 9, 8 + $$6 * 18, 84 + $$5 * 18));
         }
      }

      for (int $$7 = 0; $$7 < 9; $$7++) {
         this.addSlot(new Slot(pPlayerInventory, $$7, 8 + $$7 * 18, 142));
      }
   }

   protected static void slotChangedCraftingGrid(
      AbstractContainerMenu pMenu,
      Level pLevel,
      Player pPlayer,
      CraftingContainer pCraftSlots,
      ResultContainer pResultSlots,
      @Nullable RecipeHolder<CraftingRecipe> pRecipe
   ) {
      if (!pLevel.isClientSide) {
         CraftingInput $$6 = pCraftSlots.asCraftInput();
         ServerPlayer $$7 = (ServerPlayer)pPlayer;
         ItemStack $$8 = ItemStack.EMPTY;
         Optional<RecipeHolder<CraftingRecipe>> $$9 = pLevel.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, $$6, pLevel, pRecipe);
         if ($$9.isPresent()) {
            RecipeHolder<CraftingRecipe> $$10 = $$9.get();
            CraftingRecipe $$11 = $$10.value();
            if (pResultSlots.setRecipeUsed(pLevel, $$7, $$10)) {
               ItemStack $$12 = $$11.assemble($$6, pLevel.registryAccess());
               if ($$12.isItemEnabled(pLevel.enabledFeatures())) {
                  $$8 = $$12;
               }
            }
         }

         pResultSlots.setItem(0, $$8);
         pMenu.setRemoteSlot(0, $$8);
         $$7.connection.send(new ClientboundContainerSetSlotPacket(pMenu.containerId, pMenu.incrementStateId(), 0, $$8));
      }
   }

   @Override
   public void slotsChanged(Container pInventory) {
      if (!this.placingRecipe) {
         this.access.execute((p_341505_, p_341506_) -> slotChangedCraftingGrid(this, p_341505_, this.player, this.craftSlots, this.resultSlots, null));
      }
   }

   @Override
   public void beginPlacingRecipe() {
      this.placingRecipe = true;
   }

   @Override
   public void finishPlacingRecipe(RecipeHolder<CraftingRecipe> pRecipe) {
      this.placingRecipe = false;
      this.access.execute((p_341503_, p_341504_) -> slotChangedCraftingGrid(this, p_341503_, this.player, this.craftSlots, this.resultSlots, pRecipe));
   }

   @Override
   public void fillCraftSlotsStackedContents(StackedContents pItemHelper) {
      this.craftSlots.fillStackedContents(pItemHelper);
   }

   @Override
   public void clearCraftingContent() {
      this.craftSlots.clearContent();
      this.resultSlots.clearContent();
   }

   @Override
   public boolean recipeMatches(RecipeHolder<CraftingRecipe> pRecipe) {
      return pRecipe.value().matches(this.craftSlots.asCraftInput(), this.player.level());
   }

   @Override
   public void removed(Player pPlayer) {
      super.removed(pPlayer);
      this.access.execute((p_39371_, p_39372_) -> this.clearContainer(pPlayer, this.craftSlots));
   }

   @Override
   public boolean stillValid(Player pPlayer) {
      return stillValid(this.access, pPlayer, Blocks.CRAFTING_TABLE);
   }

   @Override
   public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
      ItemStack $$2 = ItemStack.EMPTY;
      Slot $$3 = this.slots.get(pIndex);
      if ($$3 != null && $$3.hasItem()) {
         ItemStack $$4 = $$3.getItem();
         $$2 = $$4.copy();
         if (pIndex == 0) {
            this.access.execute((p_39378_, p_39379_) -> $$4.getItem().onCraftedBy($$4, p_39378_, pPlayer));
            if (!this.moveItemStackTo($$4, 10, 46, true)) {
               return ItemStack.EMPTY;
            }

            $$3.onQuickCraft($$4, $$2);
         } else if (pIndex >= 10 && pIndex < 46) {
            if (!this.moveItemStackTo($$4, 1, 10, false)) {
               if (pIndex < 37) {
                  if (!this.moveItemStackTo($$4, 37, 46, false)) {
                     return ItemStack.EMPTY;
                  }
               } else if (!this.moveItemStackTo($$4, 10, 37, false)) {
                  return ItemStack.EMPTY;
               }
            }
         } else if (!this.moveItemStackTo($$4, 10, 46, false)) {
            return ItemStack.EMPTY;
         }

         if ($$4.isEmpty()) {
            $$3.setByPlayer(ItemStack.EMPTY);
         } else {
            $$3.setChanged();
         }

         if ($$4.getCount() == $$2.getCount()) {
            return ItemStack.EMPTY;
         }

         $$3.onTake(pPlayer, $$4);
         if (pIndex == 0) {
            pPlayer.drop($$4, false);
         }
      }

      return $$2;
   }

   @Override
   public boolean canTakeItemForPickAll(ItemStack pStack, Slot pSlot) {
      return pSlot.container != this.resultSlots && super.canTakeItemForPickAll(pStack, pSlot);
   }

   @Override
   public int getResultSlotIndex() {
      return 0;
   }

   @Override
   public int getGridWidth() {
      return this.craftSlots.getWidth();
   }

   @Override
   public int getGridHeight() {
      return this.craftSlots.getHeight();
   }

   @Override
   public int getSize() {
      return 10;
   }

   @Override
   public RecipeBookType getRecipeBookType() {
      return RecipeBookType.CRAFTING;
   }

   @Override
   public boolean shouldMoveToInventory(int pSlotIndex) {
      return pSlotIndex != this.getResultSlotIndex();
   }
}
