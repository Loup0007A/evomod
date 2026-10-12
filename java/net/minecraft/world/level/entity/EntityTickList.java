package net.minecraft.world.level.entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;

public class EntityTickList {
   private Int2ObjectMap<Entity> active = new Int2ObjectLinkedOpenHashMap();
   private Int2ObjectMap<Entity> passive = new Int2ObjectLinkedOpenHashMap();
   @Nullable
   private Int2ObjectMap<Entity> iterated;

   private void ensureActiveIsNotIterated() {
      if (this.iterated == this.active) {
         this.passive.clear();
         ObjectIterator $$1 = Int2ObjectMaps.fastIterable(this.active).iterator();

         while ($$1.hasNext()) {
            Entry<Entity> $$0 = (Entry<Entity>)$$1.next();
            this.passive.put($$0.getIntKey(), (Entity)$$0.getValue());
         }

         Int2ObjectMap<Entity> $$1x = this.active;
         this.active = this.passive;
         this.passive = $$1x;
      }
   }

   public void add(Entity pEntity) {
      this.ensureActiveIsNotIterated();
      this.active.put(pEntity.getId(), pEntity);
   }

   public void remove(Entity pEntity) {
      this.ensureActiveIsNotIterated();
      this.active.remove(pEntity.getId());
   }

   public boolean contains(Entity pEntity) {
      return this.active.containsKey(pEntity.getId());
   }

   public void forEach(Consumer<Entity> pEntity) {
      if (this.iterated != null) {
         throw new UnsupportedOperationException("Only one concurrent iteration supported");
      } else {
         this.iterated = this.active;

         try {
            ObjectIterator var2 = this.active.values().iterator();

            while (var2.hasNext()) {
               Entity $$1 = (Entity)var2.next();
               pEntity.accept($$1);
            }
         } finally {
            this.iterated = null;
         }
      }
   }
}
