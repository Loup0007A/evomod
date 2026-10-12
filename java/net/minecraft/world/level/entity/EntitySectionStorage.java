package net.minecraft.world.level.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import java.util.Objects;
import java.util.Spliterators;
import java.util.PrimitiveIterator.OfLong;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

public class EntitySectionStorage<T extends EntityAccess> {
   private final Class<T> entityClass;
   private final Long2ObjectFunction<Visibility> intialSectionVisibility;
   private final Long2ObjectMap<EntitySection<T>> sections = new Long2ObjectOpenHashMap();
   private final LongSortedSet sectionIds = new LongAVLTreeSet();

   public EntitySectionStorage(Class<T> pEntityClass, Long2ObjectFunction<Visibility> pInitialSectionVisibility) {
      this.entityClass = pEntityClass;
      this.intialSectionVisibility = pInitialSectionVisibility;
   }

   public void forEachAccessibleNonEmptySection(AABB pBoundingBox, AbortableIterationConsumer<EntitySection<T>> pConsumer) {
      int $$2 = 2;
      int $$3 = SectionPos.posToSectionCoord(pBoundingBox.minX - 2.0);
      int $$4 = SectionPos.posToSectionCoord(pBoundingBox.minY - 4.0);
      int $$5 = SectionPos.posToSectionCoord(pBoundingBox.minZ - 2.0);
      int $$6 = SectionPos.posToSectionCoord(pBoundingBox.maxX + 2.0);
      int $$7 = SectionPos.posToSectionCoord(pBoundingBox.maxY + 0.0);
      int $$8 = SectionPos.posToSectionCoord(pBoundingBox.maxZ + 2.0);

      for (int $$9 = $$3; $$9 <= $$6; $$9++) {
         long $$10 = SectionPos.asLong($$9, 0, 0);
         long $$11 = SectionPos.asLong($$9, -1, -1);
         LongIterator $$12 = this.sectionIds.subSet($$10, $$11 + 1L).iterator();

         while ($$12.hasNext()) {
            long $$13 = $$12.nextLong();
            int $$14 = SectionPos.y($$13);
            int $$15 = SectionPos.z($$13);
            if ($$14 >= $$4 && $$14 <= $$7 && $$15 >= $$5 && $$15 <= $$8) {
               EntitySection<T> $$16 = (EntitySection<T>)this.sections.get($$13);
               if ($$16 != null && !$$16.isEmpty() && $$16.getStatus().isAccessible() && pConsumer.accept($$16).shouldAbort()) {
                  return;
               }
            }
         }
      }
   }

   public LongStream getExistingSectionPositionsInChunk(long pPos) {
      int $$1 = ChunkPos.getX(pPos);
      int $$2 = ChunkPos.getZ(pPos);
      LongSortedSet $$3 = this.getChunkSections($$1, $$2);
      if ($$3.isEmpty()) {
         return LongStream.empty();
      } else {
         OfLong $$4 = $$3.iterator();
         return StreamSupport.longStream(Spliterators.spliteratorUnknownSize($$4, 1301), false);
      }
   }

   private LongSortedSet getChunkSections(int pX, int pZ) {
      long $$2 = SectionPos.asLong(pX, 0, pZ);
      long $$3 = SectionPos.asLong(pX, -1, pZ);
      return this.sectionIds.subSet($$2, $$3 + 1L);
   }

   public Stream<EntitySection<T>> getExistingSectionsInChunk(long pPos) {
      return this.getExistingSectionPositionsInChunk(pPos).<EntitySection<T>>mapToObj(this.sections::get).filter(Objects::nonNull);
   }

   private static long getChunkKeyFromSectionKey(long pPos) {
      return ChunkPos.asLong(SectionPos.x(pPos), SectionPos.z(pPos));
   }

   public EntitySection<T> getOrCreateSection(long pSectionPos) {
      return (EntitySection<T>)this.sections.computeIfAbsent(pSectionPos, this::createSection);
   }

   @Nullable
   public EntitySection<T> getSection(long pSectionPos) {
      return (EntitySection<T>)this.sections.get(pSectionPos);
   }

   private EntitySection<T> createSection(long p_156902_) {
      long $$1 = getChunkKeyFromSectionKey(p_156902_);
      Visibility $$2 = (Visibility)this.intialSectionVisibility.get($$1);
      this.sectionIds.add(p_156902_);
      return new EntitySection<>(this.entityClass, $$2);
   }

   public LongSet getAllChunksWithExistingSections() {
      LongSet $$0 = new LongOpenHashSet();
      this.sections.keySet().forEach(p_156886_ -> $$0.add(getChunkKeyFromSectionKey(p_156886_)));
      return $$0;
   }

   public void getEntities(AABB pBounds, AbortableIterationConsumer<T> pConsumer) {
      this.forEachAccessibleNonEmptySection(pBounds, p_261459_ -> p_261459_.getEntities(pBounds, pConsumer));
   }

   public <U extends T> void getEntities(EntityTypeTest<T, U> pTest, AABB pBounds, AbortableIterationConsumer<U> pConsumer) {
      this.forEachAccessibleNonEmptySection(pBounds, p_261463_ -> p_261463_.getEntities(pTest, pBounds, pConsumer));
   }

   public void remove(long pSectionId) {
      this.sections.remove(pSectionId);
      this.sectionIds.remove(pSectionId);
   }

   @VisibleForDebug
   public int count() {
      return this.sectionIds.size();
   }
}
