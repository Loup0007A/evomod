package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TradeWithVillager extends Behavior<Villager> {
   private Set<Item> trades = ImmutableSet.of();

   public TradeWithVillager() {
      super(
         ImmutableMap.of(
            MemoryModuleType.INTERACTION_TARGET, MemoryStatus.VALUE_PRESENT, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT
         )
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, Villager pOwner) {
      return BehaviorUtils.targetIsValid(pOwner.getBrain(), MemoryModuleType.INTERACTION_TARGET, EntityType.VILLAGER);
   }

   protected boolean canStillUse(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      return this.checkExtraStartConditions(pLevel, pEntity);
   }

   protected void start(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      Villager $$3 = (Villager)pEntity.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
      BehaviorUtils.lockGazeAndWalkToEachOther(pEntity, $$3, 0.5F, 2);
      this.trades = figureOutWhatIAmWillingToTrade(pEntity, $$3);
   }

   protected void tick(ServerLevel pLevel, Villager pOwner, long pGameTime) {
      Villager $$3 = (Villager)pOwner.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get();
      if (!(pOwner.distanceToSqr($$3) > 5.0)) {
         BehaviorUtils.lockGazeAndWalkToEachOther(pOwner, $$3, 0.5F, 2);
         pOwner.gossip(pLevel, $$3, pGameTime);
         if (pOwner.hasExcessFood() && (pOwner.getVillagerData().getProfession() == VillagerProfession.FARMER || $$3.wantsMoreFood())) {
            throwHalfStack(pOwner, Villager.FOOD_POINTS.keySet(), $$3);
         }

         if ($$3.getVillagerData().getProfession() == VillagerProfession.FARMER
            && pOwner.getInventory().countItem(Items.WHEAT) > Items.WHEAT.getDefaultMaxStackSize() / 2) {
            throwHalfStack(pOwner, ImmutableSet.of(Items.WHEAT), $$3);
         }

         if (!this.trades.isEmpty() && pOwner.getInventory().hasAnyOf(this.trades)) {
            throwHalfStack(pOwner, this.trades, $$3);
         }
      }
   }

   protected void stop(ServerLevel pLevel, Villager pEntity, long pGameTime) {
      pEntity.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
   }

   private static Set<Item> figureOutWhatIAmWillingToTrade(Villager pVillager, Villager pOther) {
      ImmutableSet<Item> $$2 = pOther.getVillagerData().getProfession().requestedItems();
      ImmutableSet<Item> $$3 = pVillager.getVillagerData().getProfession().requestedItems();
      return $$2.stream().filter(p_24431_ -> !$$3.contains(p_24431_)).collect(Collectors.toSet());
   }

   private static void throwHalfStack(Villager pVillager, Set<Item> pStack, LivingEntity pEntity) {
      SimpleContainer $$3 = pVillager.getInventory();
      ItemStack $$4 = ItemStack.EMPTY;
      int $$5 = 0;

      while ($$5 < $$3.getContainerSize()) {
         ItemStack $$6;
         Item $$7;
         int $$8;
         label28: {
            $$6 = $$3.getItem($$5);
            if (!$$6.isEmpty()) {
               $$7 = $$6.getItem();
               if (pStack.contains($$7)) {
                  if ($$6.getCount() > $$6.getMaxStackSize() / 2) {
                     $$8 = $$6.getCount() / 2;
                     break label28;
                  }

                  if ($$6.getCount() > 24) {
                     $$8 = $$6.getCount() - 24;
                     break label28;
                  }
               }
            }

            $$5++;
            continue;
         }

         $$6.shrink($$8);
         $$4 = new ItemStack($$7, $$8);
         break;
      }

      if (!$$4.isEmpty()) {
         BehaviorUtils.throwItem(pVillager, $$4, pEntity.position());
      }
   }
}
