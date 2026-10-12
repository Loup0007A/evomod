package net.minecraft.world.item;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.util.Mth;

public class ItemCooldowns {
   private final Map<Item, ItemCooldowns.CooldownInstance> cooldowns = Maps.newHashMap();
   private int tickCount;

   public boolean isOnCooldown(Item pItem) {
      return this.getCooldownPercent(pItem, 0.0F) > 0.0F;
   }

   public float getCooldownPercent(Item pItem, float pPartialTicks) {
      ItemCooldowns.CooldownInstance $$2 = this.cooldowns.get(pItem);
      if ($$2 != null) {
         float $$3 = (float)($$2.endTime - $$2.startTime);
         float $$4 = (float)$$2.endTime - ((float)this.tickCount + pPartialTicks);
         return Mth.clamp($$4 / $$3, 0.0F, 1.0F);
      } else {
         return 0.0F;
      }
   }

   public void tick() {
      this.tickCount++;
      if (!this.cooldowns.isEmpty()) {
         Iterator<Entry<Item, ItemCooldowns.CooldownInstance>> $$0 = this.cooldowns.entrySet().iterator();

         while ($$0.hasNext()) {
            Entry<Item, ItemCooldowns.CooldownInstance> $$1 = $$0.next();
            if ($$1.getValue().endTime <= this.tickCount) {
               $$0.remove();
               this.onCooldownEnded($$1.getKey());
            }
         }
      }
   }

   public void addCooldown(Item pItem, int pTicks) {
      this.cooldowns.put(pItem, new ItemCooldowns.CooldownInstance(this.tickCount, this.tickCount + pTicks));
      this.onCooldownStarted(pItem, pTicks);
   }

   public void removeCooldown(Item pItem) {
      this.cooldowns.remove(pItem);
      this.onCooldownEnded(pItem);
   }

   protected void onCooldownStarted(Item pItem, int pTicks) {
   }

   protected void onCooldownEnded(Item pItem) {
   }

   static class CooldownInstance {
      final int startTime;
      final int endTime;

      CooldownInstance(int pStartTime, int pEndTime) {
         this.startTime = pStartTime;
         this.endTime = pEndTime;
      }
   }
}
