package net.minecraft.world.entity.ai.gossip;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoublePredicate;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.VisibleForDebug;
import org.slf4j.Logger;

public class GossipContainer {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int DISCARD_THRESHOLD = 2;
   private final Map<UUID, GossipContainer.EntityGossips> gossips = Maps.newHashMap();

   @VisibleForDebug
   public Map<UUID, Object2IntMap<GossipType>> getGossipEntries() {
      Map<UUID, Object2IntMap<GossipType>> $$0 = Maps.newHashMap();
      this.gossips.keySet().forEach(p_148167_ -> {
         GossipContainer.EntityGossips $$2 = this.gossips.get(p_148167_);
         $$0.put(p_148167_, $$2.entries);
      });
      return $$0;
   }

   public void decay() {
      Iterator<GossipContainer.EntityGossips> $$0 = this.gossips.values().iterator();

      while ($$0.hasNext()) {
         GossipContainer.EntityGossips $$1 = $$0.next();
         $$1.decay();
         if ($$1.isEmpty()) {
            $$0.remove();
         }
      }
   }

   private Stream<GossipContainer.GossipEntry> unpack() {
      return this.gossips.entrySet().stream().flatMap(p_26185_ -> p_26185_.getValue().unpack(p_26185_.getKey()));
   }

   private Collection<GossipContainer.GossipEntry> selectGossipsForTransfer(RandomSource pRandom, int pAmount) {
      List<GossipContainer.GossipEntry> $$2 = this.unpack().toList();
      if ($$2.isEmpty()) {
         return Collections.emptyList();
      } else {
         int[] $$3 = new int[$$2.size()];
         int $$4 = 0;

         for (int $$5 = 0; $$5 < $$2.size(); $$5++) {
            GossipContainer.GossipEntry $$6 = $$2.get($$5);
            $$4 += Math.abs($$6.weightedValue());
            $$3[$$5] = $$4 - 1;
         }

         Set<GossipContainer.GossipEntry> $$7 = Sets.newIdentityHashSet();

         for (int $$8 = 0; $$8 < pAmount; $$8++) {
            int $$9 = pRandom.nextInt($$4);
            int $$10 = Arrays.binarySearch($$3, $$9);
            $$7.add($$2.get($$10 < 0 ? -$$10 - 1 : $$10));
         }

         return $$7;
      }
   }

   private GossipContainer.EntityGossips getOrCreate(UUID pIdentifier) {
      return this.gossips.computeIfAbsent(pIdentifier, p_26202_ -> new GossipContainer.EntityGossips());
   }

   public void transferFrom(GossipContainer pContainer, RandomSource pRandomSource, int pAmount) {
      Collection<GossipContainer.GossipEntry> $$3 = pContainer.selectGossipsForTransfer(pRandomSource, pAmount);
      $$3.forEach(p_26200_ -> {
         int $$1 = p_26200_.value - p_26200_.type.decayPerTransfer;
         if ($$1 >= 2) {
            this.getOrCreate(p_26200_.target).entries.mergeInt(p_26200_.type, $$1, GossipContainer::mergeValuesForTransfer);
         }
      });
   }

   public int getReputation(UUID pIdentifier, Predicate<GossipType> pGossip) {
      GossipContainer.EntityGossips $$2 = this.gossips.get(pIdentifier);
      return $$2 != null ? $$2.weightedValue(pGossip) : 0;
   }

   public long getCountForType(GossipType pGossipType, DoublePredicate pGossipPredicate) {
      return this.gossips
         .values()
         .stream()
         .filter(p_148174_ -> pGossipPredicate.test((double)(p_148174_.entries.getOrDefault(pGossipType, 0) * pGossipType.weight)))
         .count();
   }

   public void add(UUID pIdentifier, GossipType pGossipType, int pGossipValue) {
      GossipContainer.EntityGossips $$3 = this.getOrCreate(pIdentifier);
      $$3.entries.mergeInt(pGossipType, pGossipValue, (p_186096_, p_186097_) -> this.mergeValuesForAddition(pGossipType, p_186096_, p_186097_));
      $$3.makeSureValueIsntTooLowOrTooHigh(pGossipType);
      if ($$3.isEmpty()) {
         this.gossips.remove(pIdentifier);
      }
   }

   public void remove(UUID pIdentifier, GossipType pGossipType, int pGossipValue) {
      this.add(pIdentifier, pGossipType, -pGossipValue);
   }

   public void remove(UUID pIdentifier, GossipType pGossipType) {
      GossipContainer.EntityGossips $$2 = this.gossips.get(pIdentifier);
      if ($$2 != null) {
         $$2.remove(pGossipType);
         if ($$2.isEmpty()) {
            this.gossips.remove(pIdentifier);
         }
      }
   }

   public void remove(GossipType pGossipType) {
      Iterator<GossipContainer.EntityGossips> $$1 = this.gossips.values().iterator();

      while ($$1.hasNext()) {
         GossipContainer.EntityGossips $$2 = $$1.next();
         $$2.remove(pGossipType);
         if ($$2.isEmpty()) {
            $$1.remove();
         }
      }
   }

   public <T> T store(DynamicOps<T> pOps) {
      return (T)GossipContainer.GossipEntry.LIST_CODEC
         .encodeStart(pOps, this.unpack().toList())
         .resultOrPartial(p_262900_ -> LOGGER.warn("Failed to serialize gossips: {}", p_262900_))
         .orElseGet(pOps::emptyList);
   }

   public void update(Dynamic<?> pDynamic) {
      GossipContainer.GossipEntry.LIST_CODEC
         .decode(pDynamic)
         .resultOrPartial(p_262901_ -> LOGGER.warn("Failed to deserialize gossips: {}", p_262901_))
         .stream()
         .flatMap(p_262899_ -> ((List)p_262899_.getFirst()).stream())
         .forEach(p_26162_ -> this.getOrCreate(p_26162_.target).entries.put(p_26162_.type, p_26162_.value));
   }

   private static int mergeValuesForTransfer(int p_26159_, int p_26160_) {
      return Math.max(p_26159_, p_26160_);
   }

   private int mergeValuesForAddition(GossipType pGossipType, int pExisting, int pAdditive) {
      int $$3 = pExisting + pAdditive;
      return $$3 > pGossipType.max ? Math.max(pGossipType.max, pExisting) : $$3;
   }

   static class EntityGossips {
      final Object2IntMap<GossipType> entries = new Object2IntOpenHashMap();

      public int weightedValue(Predicate<GossipType> pGossipType) {
         return this.entries
            .object2IntEntrySet()
            .stream()
            .filter(p_26224_ -> pGossipType.test((GossipType)p_26224_.getKey()))
            .mapToInt(p_26214_ -> p_26214_.getIntValue() * ((GossipType)p_26214_.getKey()).weight)
            .sum();
      }

      public Stream<GossipContainer.GossipEntry> unpack(UUID pIdentifier) {
         return this.entries
            .object2IntEntrySet()
            .stream()
            .map(p_26219_ -> new GossipContainer.GossipEntry(pIdentifier, (GossipType)p_26219_.getKey(), p_26219_.getIntValue()));
      }

      public void decay() {
         ObjectIterator<Entry<GossipType>> $$0 = this.entries.object2IntEntrySet().iterator();

         while ($$0.hasNext()) {
            Entry<GossipType> $$1 = (Entry<GossipType>)$$0.next();
            int $$2 = $$1.getIntValue() - ((GossipType)$$1.getKey()).decayPerDay;
            if ($$2 < 2) {
               $$0.remove();
            } else {
               $$1.setValue($$2);
            }
         }
      }

      public boolean isEmpty() {
         return this.entries.isEmpty();
      }

      public void makeSureValueIsntTooLowOrTooHigh(GossipType pGossipType) {
         int $$1 = this.entries.getInt(pGossipType);
         if ($$1 > pGossipType.max) {
            this.entries.put(pGossipType, pGossipType.max);
         }

         if ($$1 < 2) {
            this.remove(pGossipType);
         }
      }

      public void remove(GossipType pGossipType) {
         this.entries.removeInt(pGossipType);
      }
   }

   static record GossipEntry(UUID target, GossipType type, int value) {
      public static final Codec<GossipContainer.GossipEntry> CODEC = RecordCodecBuilder.create(
         p_263007_ -> p_263007_.group(
                  UUIDUtil.CODEC.fieldOf("Target").forGetter(GossipContainer.GossipEntry::target),
                  GossipType.CODEC.fieldOf("Type").forGetter(GossipContainer.GossipEntry::type),
                  ExtraCodecs.POSITIVE_INT.fieldOf("Value").forGetter(GossipContainer.GossipEntry::value)
               )
               .apply(p_263007_, GossipContainer.GossipEntry::new)
      );
      public static final Codec<List<GossipContainer.GossipEntry>> LIST_CODEC = CODEC.listOf();

      public int weightedValue() {
         return this.value * this.type.weight;
      }
   }
}
