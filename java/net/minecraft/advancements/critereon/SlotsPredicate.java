package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.inventory.SlotRanges;

public record SlotsPredicate(Map<SlotRange, ItemPredicate> slots) {
   public static final Codec<SlotsPredicate> CODEC = Codec.unboundedMap(SlotRanges.CODEC, ItemPredicate.CODEC).xmap(SlotsPredicate::new, SlotsPredicate::slots);

   public boolean matches(Entity pEntity) {
      for (Entry<SlotRange, ItemPredicate> $$1 : this.slots.entrySet()) {
         if (!matchSlots(pEntity, $$1.getValue(), $$1.getKey().slots())) {
            return false;
         }
      }

      return true;
   }

   private static boolean matchSlots(Entity pEntity, ItemPredicate pPredicate, IntList pSlots) {
      for (int $$3 = 0; $$3 < pSlots.size(); $$3++) {
         int $$4 = pSlots.getInt($$3);
         SlotAccess $$5 = pEntity.getSlot($$4);
         if (pPredicate.test($$5.get())) {
            return true;
         }
      }

      return false;
   }
}
