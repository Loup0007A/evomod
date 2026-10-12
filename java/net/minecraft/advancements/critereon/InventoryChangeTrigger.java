package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class InventoryChangeTrigger extends SimpleCriterionTrigger<InventoryChangeTrigger.TriggerInstance> {
   @Override
   public Codec<InventoryChangeTrigger.TriggerInstance> codec() {
      return InventoryChangeTrigger.TriggerInstance.CODEC;
   }

   public void trigger(ServerPlayer pPlayer, Inventory pInventory, ItemStack pStack) {
      int $$3 = 0;
      int $$4 = 0;
      int $$5 = 0;

      for (int $$6 = 0; $$6 < pInventory.getContainerSize(); $$6++) {
         ItemStack $$7 = pInventory.getItem($$6);
         if ($$7.isEmpty()) {
            $$4++;
         } else {
            $$5++;
            if ($$7.getCount() >= $$7.getMaxStackSize()) {
               $$3++;
            }
         }
      }

      this.trigger(pPlayer, pInventory, pStack, $$3, $$4, $$5);
   }

   private void trigger(ServerPlayer pPlayer, Inventory pInventory, ItemStack pStack, int pFull, int pEmpty, int pOccupied) {
      this.trigger(pPlayer, p_43166_ -> p_43166_.matches(pInventory, pStack, pFull, pEmpty, pOccupied));
   }

   public static record TriggerInstance(Optional<ContextAwarePredicate> player, InventoryChangeTrigger.TriggerInstance.Slots slots, List<ItemPredicate> items)
      implements SimpleCriterionTrigger.SimpleInstance {
      public static final Codec<InventoryChangeTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
         p_325218_ -> p_325218_.group(
                  EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(InventoryChangeTrigger.TriggerInstance::player),
                  InventoryChangeTrigger.TriggerInstance.Slots.CODEC
                     .optionalFieldOf("slots", InventoryChangeTrigger.TriggerInstance.Slots.ANY)
                     .forGetter(InventoryChangeTrigger.TriggerInstance::slots),
                  ItemPredicate.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(InventoryChangeTrigger.TriggerInstance::items)
               )
               .apply(p_325218_, InventoryChangeTrigger.TriggerInstance::new)
      );

      public static Criterion<InventoryChangeTrigger.TriggerInstance> hasItems(ItemPredicate.Builder... pItems) {
         return hasItems(Stream.of(pItems).map(ItemPredicate.Builder::build).toArray(ItemPredicate[]::new));
      }

      public static Criterion<InventoryChangeTrigger.TriggerInstance> hasItems(ItemPredicate... pItems) {
         return CriteriaTriggers.INVENTORY_CHANGED
            .createCriterion(new InventoryChangeTrigger.TriggerInstance(Optional.empty(), InventoryChangeTrigger.TriggerInstance.Slots.ANY, List.of(pItems)));
      }

      public static Criterion<InventoryChangeTrigger.TriggerInstance> hasItems(ItemLike... pItems) {
         ItemPredicate[] $$1 = new ItemPredicate[pItems.length];

         for (int $$2 = 0; $$2 < pItems.length; $$2++) {
            $$1[$$2] = new ItemPredicate(
               Optional.of(HolderSet.direct(pItems[$$2].asItem().builtInRegistryHolder())), MinMaxBounds.Ints.ANY, DataComponentPredicate.EMPTY, Map.of()
            );
         }

         return hasItems($$1);
      }

      public boolean matches(Inventory pInventory, ItemStack pStack, int pFull, int pEmpty, int pOccupied) {
         if (!this.slots.matches(pFull, pEmpty, pOccupied)) {
            return false;
         } else if (this.items.isEmpty()) {
            return true;
         } else if (this.items.size() != 1) {
            List<ItemPredicate> $$5 = new ObjectArrayList(this.items);
            int $$6 = pInventory.getContainerSize();

            for (int $$7 = 0; $$7 < $$6; $$7++) {
               if ($$5.isEmpty()) {
                  return true;
               }

               ItemStack $$8 = pInventory.getItem($$7);
               if (!$$8.isEmpty()) {
                  $$5.removeIf(p_325217_ -> p_325217_.test($$8));
               }
            }

            return $$5.isEmpty();
         } else {
            return !pStack.isEmpty() && this.items.get(0).test(pStack);
         }
      }

      public static record Slots(MinMaxBounds.Ints occupied, MinMaxBounds.Ints full, MinMaxBounds.Ints empty) {
         public static final Codec<InventoryChangeTrigger.TriggerInstance.Slots> CODEC = RecordCodecBuilder.create(
            p_325219_ -> p_325219_.group(
                     MinMaxBounds.Ints.CODEC
                        .optionalFieldOf("occupied", MinMaxBounds.Ints.ANY)
                        .forGetter(InventoryChangeTrigger.TriggerInstance.Slots::occupied),
                     MinMaxBounds.Ints.CODEC.optionalFieldOf("full", MinMaxBounds.Ints.ANY).forGetter(InventoryChangeTrigger.TriggerInstance.Slots::full),
                     MinMaxBounds.Ints.CODEC.optionalFieldOf("empty", MinMaxBounds.Ints.ANY).forGetter(InventoryChangeTrigger.TriggerInstance.Slots::empty)
                  )
                  .apply(p_325219_, InventoryChangeTrigger.TriggerInstance.Slots::new)
         );
         public static final InventoryChangeTrigger.TriggerInstance.Slots ANY = new InventoryChangeTrigger.TriggerInstance.Slots(
            MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY
         );

         public boolean matches(int pFull, int pEmpty, int pOccupied) {
            if (!this.full.matches(pFull)) {
               return false;
            } else {
               return !this.empty.matches(pEmpty) ? false : this.occupied.matches(pOccupied);
            }
         }
      }
   }
}
