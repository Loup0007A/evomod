package net.minecraft.world.level.timers;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.primitives.UnsignedLong;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.slf4j.Logger;

public class TimerQueue<T> {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String CALLBACK_DATA_TAG = "Callback";
   private static final String TIMER_NAME_TAG = "Name";
   private static final String TIMER_TRIGGER_TIME_TAG = "TriggerTime";
   private final TimerCallbacks<T> callbacksRegistry;
   private final Queue<TimerQueue.Event<T>> queue = new PriorityQueue<>(createComparator());
   private UnsignedLong sequentialId = UnsignedLong.ZERO;
   private final Table<String, Long, TimerQueue.Event<T>> events = HashBasedTable.create();

   private static <T> Comparator<TimerQueue.Event<T>> createComparator() {
      return Comparator.<TimerQueue.Event<T>>comparingLong(p_82272_ -> p_82272_.triggerTime).thenComparing(p_82269_ -> p_82269_.sequentialId);
   }

   public TimerQueue(TimerCallbacks<T> pCallbacksRegistry, Stream<? extends Dynamic<?>> pScheduledEventsDynamic) {
      this(pCallbacksRegistry);
      this.queue.clear();
      this.events.clear();
      this.sequentialId = UnsignedLong.ZERO;
      pScheduledEventsDynamic.forEach(p_265027_ -> {
         Tag $$1 = (Tag)p_265027_.convert(NbtOps.INSTANCE).getValue();
         if ($$1 instanceof CompoundTag $$2) {
            this.loadEvent($$2);
         } else {
            LOGGER.warn("Invalid format of events: {}", $$1);
         }
      });
   }

   public TimerQueue(TimerCallbacks<T> pCallbacksRegistry) {
      this.callbacksRegistry = pCallbacksRegistry;
   }

   public void tick(T pObj, long pGameTime) {
      while (true) {
         TimerQueue.Event<T> $$2 = this.queue.peek();
         if ($$2 == null || $$2.triggerTime > pGameTime) {
            return;
         }

         this.queue.remove();
         this.events.remove($$2.id, pGameTime);
         $$2.callback.handle(pObj, this, pGameTime);
      }
   }

   public void schedule(String pId, long pTriggerTime, TimerCallback<T> pCallback) {
      if (!this.events.contains(pId, pTriggerTime)) {
         this.sequentialId = this.sequentialId.plus(UnsignedLong.ONE);
         TimerQueue.Event<T> $$3 = new TimerQueue.Event<>(pTriggerTime, this.sequentialId, pId, pCallback);
         this.events.put(pId, pTriggerTime, $$3);
         this.queue.add($$3);
      }
   }

   public int remove(String pEventId) {
      Collection<TimerQueue.Event<T>> $$1 = this.events.row(pEventId).values();
      $$1.forEach(this.queue::remove);
      int $$2 = $$1.size();
      $$1.clear();
      return $$2;
   }

   public Set<String> getEventsIds() {
      return Collections.unmodifiableSet(this.events.rowKeySet());
   }

   private void loadEvent(CompoundTag pTag) {
      CompoundTag $$1 = pTag.getCompound("Callback");
      TimerCallback<T> $$2 = this.callbacksRegistry.deserialize($$1);
      if ($$2 != null) {
         String $$3 = pTag.getString("Name");
         long $$4 = pTag.getLong("TriggerTime");
         this.schedule($$3, $$4, $$2);
      }
   }

   private CompoundTag storeEvent(TimerQueue.Event<T> p_82255_) {
      CompoundTag $$1 = new CompoundTag();
      $$1.putString("Name", p_82255_.id);
      $$1.putLong("TriggerTime", p_82255_.triggerTime);
      $$1.put("Callback", this.callbacksRegistry.serialize(p_82255_.callback));
      return $$1;
   }

   public ListTag store() {
      ListTag $$0 = new ListTag();
      this.queue.stream().sorted(createComparator()).map(this::storeEvent).forEach($$0::add);
      return $$0;
   }

   public static class Event<T> {
      public final long triggerTime;
      public final UnsignedLong sequentialId;
      public final String id;
      public final TimerCallback<T> callback;

      Event(long pTriggerTime, UnsignedLong pSequentialId, String pId, TimerCallback<T> pCallback) {
         this.triggerTime = pTriggerTime;
         this.sequentialId = pSequentialId;
         this.id = pId;
         this.callback = pCallback;
      }
   }
}
