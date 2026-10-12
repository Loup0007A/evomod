package net.minecraft.world.ticks;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;

public class ProtoChunkTicks<T> implements SerializableTickContainer<T>, TickContainerAccess<T> {
   private final List<SavedTick<T>> ticks = Lists.newArrayList();
   private final Set<SavedTick<?>> ticksPerPosition = new ObjectOpenCustomHashSet(SavedTick.UNIQUE_TICK_HASH);

   @Override
   public void schedule(ScheduledTick<T> pTick) {
      SavedTick<T> $$1 = new SavedTick<>(pTick.type(), pTick.pos(), 0, pTick.priority());
      this.schedule($$1);
   }

   private void schedule(SavedTick<T> p_193296_) {
      if (this.ticksPerPosition.add(p_193296_)) {
         this.ticks.add(p_193296_);
      }
   }

   @Override
   public boolean hasScheduledTick(BlockPos pPos, T pType) {
      return this.ticksPerPosition.contains(SavedTick.probe(pType, pPos));
   }

   @Override
   public int count() {
      return this.ticks.size();
   }

   @Override
   public Tag save(long pGameTime, Function<T, String> pIdGetter) {
      ListTag $$2 = new ListTag();

      for (SavedTick<T> $$3 : this.ticks) {
         $$2.add($$3.save(pIdGetter));
      }

      return $$2;
   }

   public List<SavedTick<T>> scheduledTicks() {
      return List.copyOf(this.ticks);
   }

   public static <T> ProtoChunkTicks<T> load(ListTag pTag, Function<String, Optional<T>> pIdParser, ChunkPos pChunkPos) {
      ProtoChunkTicks<T> $$3 = new ProtoChunkTicks<>();
      SavedTick.loadTickList(pTag, pIdParser, pChunkPos, $$3::schedule);
      return $$3;
   }
}
