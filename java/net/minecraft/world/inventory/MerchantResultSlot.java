package net.minecraft.world.inventory;

import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

public class MerchantResultSlot extends Slot {
   private final MerchantContainer slots;
   private final Player player;
   private int removeCount;
   private final Merchant merchant;

   public MerchantResultSlot(Player pPlayer, Merchant pMerchant, MerchantContainer pSlots, int pSlot, int pXPosition, int pYPosition) {
      super(pSlots, pSlot, pXPosition, pYPosition);
      this.player = pPlayer;
      this.merchant = pMerchant;
      this.slots = pSlots;
   }

   @Override
   public boolean mayPlace(ItemStack pStack) {
      return false;
   }

   @Override
   public ItemStack remove(int pAmount) {
      if (this.hasItem()) {
         this.removeCount = this.removeCount + Math.min(pAmount, this.getItem().getCount());
      }

      return super.remove(pAmount);
   }

   @Override
   protected void onQuickCraft(ItemStack pStack, int pAmount) {
      this.removeCount += pAmount;
      this.checkTakeAchievements(pStack);
   }

   @Override
   protected void checkTakeAchievements(ItemStack pStack) {
      pStack.onCraftedBy(this.player.level(), this.player, this.removeCount);
      this.removeCount = 0;
   }

   @Override
   public void onTake(Player pPlayer, ItemStack pStack) {
      this.checkTakeAchievements(pStack);
      MerchantOffer $$2 = this.slots.getActiveOffer();
      if ($$2 != null) {
         ItemStack $$3 = this.slots.getItem(0);
         ItemStack $$4 = this.slots.getItem(1);
         if ($$2.take($$3, $$4) || $$2.take($$4, $$3)) {
            this.merchant.notifyTrade($$2);
            pPlayer.awardStat(Stats.TRADED_WITH_VILLAGER);
            this.slots.setItem(0, $$3);
            this.slots.setItem(1, $$4);
         }

         this.merchant.overrideXp(this.merchant.getVillagerXp() + $$2.getXp());
      }
   }
}
