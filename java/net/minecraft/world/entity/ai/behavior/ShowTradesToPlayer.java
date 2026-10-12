package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

public class ShowTradesToPlayer extends Behavior<Villager> {
   private static final int MAX_LOOK_TIME = 900;
   private static final int STARTING_LOOK_TIME = 40;
   @Nullable
   private ItemStack playerItemStack;
   private final List<ItemStack> displayItems = Lists.newArrayList();
   private int cycleCounter;
   private int displayIndex;
   private int lookTime;

   public ShowTradesToPlayer(int pMinDuration, int pMaxDuration) {
      super(ImmutableMap.of(MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_PRESENT), pMinDuration, pMaxDuration);
   }

   public boolean checkExtraStartConditions(ServerLevel pLevel, Villager pOwner) {
      Brain<?> $$2 = pOwner.getBrain();
      if ($$2.getMemory(MemoryModuleType.INTERACTION_TARGET).isEmpty()) {
         return false;
      } else {
         LivingEntity $$3 = $$2.getMemory(MemoryModuleType.INTERACTION_TARGET).get();
         return $$3.getType() == EntityType.PLAYER && pOwner.isAlive() && $$3.isAlive() && !pOwner.isBaby() && pOwner.distanceToSqr($$3) <= 17.0;
      }
   }

   public boolean canStillUse(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      return this.checkExtraStartConditions(pLevel, pEntity)
         && this.lookTime > 0
         && pEntity.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).isPresent();
   }

   public void start(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      super.start(pLevel, pEntity, pGameTime);
      this.lookAtTarget(pEntity);
      this.cycleCounter = 0;
      this.displayIndex = 0;
      this.lookTime = 40;
   }

   public void tick(ServerLevel pLevel, Villager pOwner, long pGameTime) {
      LivingEntity $$3 = this.lookAtTarget(pOwner);
      this.findItemsToDisplay($$3, pOwner);
      if (!this.displayItems.isEmpty()) {
         this.displayCyclingItems(pOwner);
      } else {
         clearHeldItem(pOwner);
         this.lookTime = Math.min(this.lookTime, 40);
      }

      this.lookTime--;
   }

   public void stop(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      super.stop(pLevel, pEntity, pGameTime);
      pEntity.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
      clearHeldItem(pEntity);
      this.playerItemStack = null;
   }

   private void findItemsToDisplay(LivingEntity pEntity, Villager pVillager) {
      boolean $$2 = false;
      ItemStack $$3 = pEntity.getMainHandItem();
      if (this.playerItemStack == null || !ItemStack.isSameItem(this.playerItemStack, $$3)) {
         this.playerItemStack = $$3;
         $$2 = true;
         this.displayItems.clear();
      }

      if ($$2 && !this.playerItemStack.isEmpty()) {
         this.updateDisplayItems(pVillager);
         if (!this.displayItems.isEmpty()) {
            this.lookTime = 900;
            this.displayFirstItem(pVillager);
         }
      }
   }

   private void displayFirstItem(Villager pVillager) {
      displayAsHeldItem(pVillager, this.displayItems.get(0));
   }

   private void updateDisplayItems(Villager pVillager) {
      for (MerchantOffer $$1 : pVillager.getOffers()) {
         if (!$$1.isOutOfStock() && this.playerItemStackMatchesCostOfOffer($$1)) {
            this.displayItems.add($$1.assemble());
         }
      }
   }

   private boolean playerItemStackMatchesCostOfOffer(MerchantOffer pOffer) {
      return ItemStack.isSameItem(this.playerItemStack, pOffer.getCostA()) || ItemStack.isSameItem(this.playerItemStack, pOffer.getCostB());
   }

   private static void clearHeldItem(Villager pVillager) {
      pVillager.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      pVillager.setDropChance(EquipmentSlot.MAINHAND, 0.085F);
   }

   private static void displayAsHeldItem(Villager pVillager, ItemStack pItem) {
      pVillager.setItemSlot(EquipmentSlot.MAINHAND, pItem);
      pVillager.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
   }

   private LivingEntity lookAtTarget(Villager pVillager) {
      Brain<?> $$1 = pVillager.getBrain();
      LivingEntity $$2 = $$1.getMemory(MemoryModuleType.INTERACTION_TARGET).get();
      $$1.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker($$2, true));
      return $$2;
   }

   private void displayCyclingItems(Villager pVillager) {
      if (this.displayItems.size() >= 2 && ++this.cycleCounter >= 40) {
         this.displayIndex++;
         this.cycleCounter = 0;
         if (this.displayIndex > this.displayItems.size() - 1) {
            this.displayIndex = 0;
         }

         displayAsHeldItem(pVillager, this.displayItems.get(this.displayIndex));
      }
   }
}
