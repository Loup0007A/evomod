package net.minecraft.world.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public class MerchantMenu extends AbstractContainerMenu {
   protected static final int PAYMENT1_SLOT = 0;
   protected static final int PAYMENT2_SLOT = 1;
   protected static final int RESULT_SLOT = 2;
   private static final int INV_SLOT_START = 3;
   private static final int INV_SLOT_END = 30;
   private static final int USE_ROW_SLOT_START = 30;
   private static final int USE_ROW_SLOT_END = 39;
   private static final int SELLSLOT1_X = 136;
   private static final int SELLSLOT2_X = 162;
   private static final int BUYSLOT_X = 220;
   private static final int ROW_Y = 37;
   private final Merchant trader;
   private final MerchantContainer tradeContainer;
   private int merchantLevel;
   private boolean showProgressBar;
   private boolean canRestock;

   public MerchantMenu(int pContainerId, Inventory pPlayerInventory) {
      this(pContainerId, pPlayerInventory, new ClientSideMerchant(pPlayerInventory.player));
   }

   public MerchantMenu(int pContainerId, Inventory pPlayerInventory, Merchant pTrader) {
      super(MenuType.MERCHANT, pContainerId);
      this.trader = pTrader;
      this.tradeContainer = new MerchantContainer(pTrader);
      this.addSlot(new Slot(this.tradeContainer, 0, 136, 37));
      this.addSlot(new Slot(this.tradeContainer, 1, 162, 37));
      this.addSlot(new MerchantResultSlot(pPlayerInventory.player, pTrader, this.tradeContainer, 2, 220, 37));

      for (int $$3 = 0; $$3 < 3; $$3++) {
         for (int $$4 = 0; $$4 < 9; $$4++) {
            this.addSlot(new Slot(pPlayerInventory, $$4 + $$3 * 9 + 9, 108 + $$4 * 18, 84 + $$3 * 18));
         }
      }

      for (int $$5 = 0; $$5 < 9; $$5++) {
         this.addSlot(new Slot(pPlayerInventory, $$5, 108 + $$5 * 18, 142));
      }
   }

   public void setShowProgressBar(boolean pShowProgressBar) {
      this.showProgressBar = pShowProgressBar;
   }

   @Override
   public void slotsChanged(Container pInventory) {
      this.tradeContainer.updateSellItem();
      super.slotsChanged(pInventory);
   }

   public void setSelectionHint(int pCurrentRecipeIndex) {
      this.tradeContainer.setSelectionHint(pCurrentRecipeIndex);
   }

   @Override
   public boolean stillValid(Player pPlayer) {
      return this.trader.getTradingPlayer() == pPlayer;
   }

   public int getTraderXp() {
      return this.trader.getVillagerXp();
   }

   public int getFutureTraderXp() {
      return this.tradeContainer.getFutureXp();
   }

   public void setXp(int pXp) {
      this.trader.overrideXp(pXp);
   }

   public int getTraderLevel() {
      return this.merchantLevel;
   }

   public void setMerchantLevel(int pLevel) {
      this.merchantLevel = pLevel;
   }

   public void setCanRestock(boolean pCanRestock) {
      this.canRestock = pCanRestock;
   }

   public boolean canRestock() {
      return this.canRestock;
   }

   @Override
   public boolean canTakeItemForPickAll(ItemStack pStack, Slot pSlot) {
      return false;
   }

   @Override
   public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
      ItemStack $$2 = ItemStack.EMPTY;
      Slot $$3 = this.slots.get(pIndex);
      if ($$3 != null && $$3.hasItem()) {
         ItemStack $$4 = $$3.getItem();
         $$2 = $$4.copy();
         if (pIndex == 2) {
            if (!this.moveItemStackTo($$4, 3, 39, true)) {
               return ItemStack.EMPTY;
            }

            $$3.onQuickCraft($$4, $$2);
            this.playTradeSound();
         } else if (pIndex != 0 && pIndex != 1) {
            if (pIndex >= 3 && pIndex < 30) {
               if (!this.moveItemStackTo($$4, 30, 39, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (pIndex >= 30 && pIndex < 39 && !this.moveItemStackTo($$4, 3, 30, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo($$4, 3, 39, false)) {
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
      }

      return $$2;
   }

   private void playTradeSound() {
      if (!this.trader.isClientSide()) {
         Entity $$0 = (Entity)this.trader;
         $$0.level().playLocalSound($$0.getX(), $$0.getY(), $$0.getZ(), this.trader.getNotifyTradeSound(), SoundSource.NEUTRAL, 1.0F, 1.0F, false);
      }
   }

   @Override
   public void removed(Player pPlayer) {
      super.removed(pPlayer);
      this.trader.setTradingPlayer(null);
      if (!this.trader.isClientSide()) {
         if (!pPlayer.isAlive() || pPlayer instanceof ServerPlayer && ((ServerPlayer)pPlayer).hasDisconnected()) {
            ItemStack $$1 = this.tradeContainer.removeItemNoUpdate(0);
            if (!$$1.isEmpty()) {
               pPlayer.drop($$1, false);
            }

            $$1 = this.tradeContainer.removeItemNoUpdate(1);
            if (!$$1.isEmpty()) {
               pPlayer.drop($$1, false);
            }
         } else if (pPlayer instanceof ServerPlayer) {
            pPlayer.getInventory().placeItemBackInInventory(this.tradeContainer.removeItemNoUpdate(0));
            pPlayer.getInventory().placeItemBackInInventory(this.tradeContainer.removeItemNoUpdate(1));
         }
      }
   }

   public void tryMoveItems(int pSelectedMerchantRecipe) {
      if (pSelectedMerchantRecipe >= 0 && this.getOffers().size() > pSelectedMerchantRecipe) {
         ItemStack $$1 = this.tradeContainer.getItem(0);
         if (!$$1.isEmpty()) {
            if (!this.moveItemStackTo($$1, 3, 39, true)) {
               return;
            }

            this.tradeContainer.setItem(0, $$1);
         }

         ItemStack $$2 = this.tradeContainer.getItem(1);
         if (!$$2.isEmpty()) {
            if (!this.moveItemStackTo($$2, 3, 39, true)) {
               return;
            }

            this.tradeContainer.setItem(1, $$2);
         }

         if (this.tradeContainer.getItem(0).isEmpty() && this.tradeContainer.getItem(1).isEmpty()) {
            MerchantOffer $$3 = this.getOffers().get(pSelectedMerchantRecipe);
            this.moveFromInventoryToPaymentSlot(0, $$3.getItemCostA());
            $$3.getItemCostB().ifPresent(p_332192_ -> this.moveFromInventoryToPaymentSlot(1, p_332192_));
         }
      }
   }

   private void moveFromInventoryToPaymentSlot(int pPaymentSlotIndex, ItemCost pPayment) {
      for (int $$2 = 3; $$2 < 39; $$2++) {
         ItemStack $$3 = this.slots.get($$2).getItem();
         if (!$$3.isEmpty() && pPayment.test($$3)) {
            ItemStack $$4 = this.tradeContainer.getItem(pPaymentSlotIndex);
            if ($$4.isEmpty() || ItemStack.isSameItemSameComponents($$3, $$4)) {
               int $$5 = $$3.getMaxStackSize();
               int $$6 = Math.min($$5 - $$4.getCount(), $$3.getCount());
               ItemStack $$7 = $$3.copyWithCount($$4.getCount() + $$6);
               $$3.shrink($$6);
               this.tradeContainer.setItem(pPaymentSlotIndex, $$7);
               if ($$7.getCount() >= $$5) {
                  break;
               }
            }
         }
      }
   }

   public void setOffers(MerchantOffers pOffers) {
      this.trader.overrideOffers(pOffers);
   }

   public MerchantOffers getOffers() {
      return this.trader.getOffers();
   }

   public boolean showProgressBar() {
      return this.showProgressBar;
   }
}
