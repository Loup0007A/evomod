package net.minecraft.world.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;

public interface EquipmentUser {
   void setItemSlot(EquipmentSlot pSlot, ItemStack pStack);

   ItemStack getItemBySlot(EquipmentSlot pSlot);

   void setDropChance(EquipmentSlot pSlot, float pDropChance);

   default void equip(EquipmentTable pEquipmentTable, LootParams pParams) {
      this.equip(pEquipmentTable.lootTable(), pParams, pEquipmentTable.slotDropChances());
   }

   default void equip(ResourceKey<LootTable> pEquipmentLootTable, LootParams pParams, Map<EquipmentSlot, Float> pSlotDropChances) {
      this.equip(pEquipmentLootTable, pParams, 0L, pSlotDropChances);
   }

   default void equip(ResourceKey<LootTable> pEquipmentLootTable, LootParams pParams, long pSeed, Map<EquipmentSlot, Float> pSlotDropChances) {
      if (!pEquipmentLootTable.equals(BuiltInLootTables.EMPTY)) {
         LootTable $$4 = pParams.getLevel().getServer().reloadableRegistries().getLootTable(pEquipmentLootTable);
         if ($$4 != LootTable.EMPTY) {
            List<ItemStack> $$5 = $$4.getRandomItems(pParams, pSeed);
            List<EquipmentSlot> $$6 = new ArrayList<>();

            for (ItemStack $$7 : $$5) {
               EquipmentSlot $$8 = this.resolveSlot($$7, $$6);
               if ($$8 != null) {
                  ItemStack $$9 = $$8.limit($$7);
                  this.setItemSlot($$8, $$9);
                  Float $$10 = pSlotDropChances.get($$8);
                  if ($$10 != null) {
                     this.setDropChance($$8, $$10);
                  }

                  $$6.add($$8);
               }
            }
         }
      }
   }

   @Nullable
   default EquipmentSlot resolveSlot(ItemStack pStack, List<EquipmentSlot> pExcludedSlots) {
      if (pStack.isEmpty()) {
         return null;
      } else {
         Equipable $$2 = Equipable.get(pStack);
         if ($$2 != null) {
            EquipmentSlot $$3 = $$2.getEquipmentSlot();
            if (!pExcludedSlots.contains($$3)) {
               return $$3;
            }
         } else if (!pExcludedSlots.contains(EquipmentSlot.MAINHAND)) {
            return EquipmentSlot.MAINHAND;
         }

         return null;
      }
   }
}
