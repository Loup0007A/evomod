package net.minecraft.world.level.entity;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.util.AbortableIterationConsumer;
import org.slf4j.Logger;

public class EntityLookup<T extends EntityAccess> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Int2ObjectMap<T> byId = new Int2ObjectLinkedOpenHashMap();
   private final Map<UUID, T> byUuid = Maps.newHashMap();

   public <U extends T> void getEntities(EntityTypeTest<T, U> pTest, AbortableIterationConsumer<U> pConsumer) {
      ObjectIterator var3 = this.byId.values().iterator();

      while (var3.hasNext()) {
         T $$2 = (T)var3.next();
         U $$3 = (U)pTest.tryCast($$2);
         if ($$3 != null && pConsumer.accept($$3).shouldAbort()) {
            return;
         }
      }
   }

   public Iterable<T> getAllEntities() {
      return Iterables.unmodifiableIterable(this.byId.values());
   }

   public void add(T pEntity) {
      UUID $$1 = pEntity.getUUID();
      if (this.byUuid.containsKey($$1)) {
         LOGGER.warn("Duplicate entity UUID {}: {}", $$1, pEntity);
      } else {
         this.byUuid.put($$1, pEntity);
         this.byId.put(pEntity.getId(), pEntity);
      }
   }

   public void remove(T pEntity) {
      this.byUuid.remove(pEntity.getUUID());
      this.byId.remove(pEntity.getId());
   }

   @Nullable
   public T getEntity(int pId) {
      return (T)this.byId.get(pId);
   }

   @Nullable
   public T getEntity(UUID pUuid) {
      return this.byUuid.get(pUuid);
   }

   public int count() {
      return this.byUuid.size();
   }
}
