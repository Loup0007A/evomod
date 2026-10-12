package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;

public class ItemEnchantments implements TooltipProvider {
   public static final ItemEnchantments EMPTY = new ItemEnchantments(new Object2IntOpenHashMap(), true);
   private static final Codec<Integer> LEVEL_CODEC = Codec.intRange(0, 255);
   private static final Codec<Object2IntOpenHashMap<Holder<Enchantment>>> LEVELS_CODEC = Codec.unboundedMap(Enchantment.CODEC, LEVEL_CODEC)
      .xmap(Object2IntOpenHashMap::new, Function.identity());
   private static final Codec<ItemEnchantments> FULL_CODEC = RecordCodecBuilder.create(
      p_330315_ -> p_330315_.group(
               LEVELS_CODEC.fieldOf("levels").forGetter(p_334450_ -> p_334450_.enchantments),
               Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(p_330292_ -> p_330292_.showInTooltip)
            )
            .apply(p_330315_, ItemEnchantments::new)
   );
   public static final Codec<ItemEnchantments> CODEC = Codec.withAlternative(FULL_CODEC, LEVELS_CODEC, p_330983_ -> new ItemEnchantments(p_330983_, true));
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemEnchantments> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.map(Object2IntOpenHashMap::new, Enchantment.STREAM_CODEC, ByteBufCodecs.VAR_INT),
      p_334569_ -> p_334569_.enchantments,
      ByteBufCodecs.BOOL,
      p_328412_ -> p_328412_.showInTooltip,
      ItemEnchantments::new
   );
   final Object2IntOpenHashMap<Holder<Enchantment>> enchantments;
   final boolean showInTooltip;

   ItemEnchantments(Object2IntOpenHashMap<Holder<Enchantment>> p_329796_, boolean p_331323_) {
      this.enchantments = p_329796_;
      this.showInTooltip = p_331323_;
      ObjectIterator var3 = p_329796_.object2IntEntrySet().iterator();

      while (var3.hasNext()) {
         Entry<Holder<Enchantment>> $$2 = (Entry<Holder<Enchantment>>)var3.next();
         int $$3 = $$2.getIntValue();
         if ($$3 < 0 || $$3 > 255) {
            throw new IllegalArgumentException("Enchantment " + $$2.getKey() + " has invalid level " + $$3);
         }
      }
   }

   public int getLevel(Holder<Enchantment> pEnchantment) {
      return this.enchantments.getInt(pEnchantment);
   }

   @Override
   public void addToTooltip(Item.TooltipContext pContext, Consumer<Component> pTooltipAdder, TooltipFlag pTooltipFlag) {
      if (this.showInTooltip) {
         HolderLookup.Provider $$3 = pContext.registries();
         HolderSet<Enchantment> $$4 = getTagOrEmpty($$3, Registries.ENCHANTMENT, EnchantmentTags.TOOLTIP_ORDER);

         for (Holder<Enchantment> $$5 : $$4) {
            int $$6 = this.enchantments.getInt($$5);
            if ($$6 > 0) {
               pTooltipAdder.accept(Enchantment.getFullname($$5, $$6));
            }
         }

         ObjectIterator var9 = this.enchantments.object2IntEntrySet().iterator();

         while (var9.hasNext()) {
            Entry<Holder<Enchantment>> $$7 = (Entry<Holder<Enchantment>>)var9.next();
            Holder<Enchantment> $$8 = (Holder<Enchantment>)$$7.getKey();
            if (!$$4.contains($$8)) {
               pTooltipAdder.accept(Enchantment.getFullname((Holder<Enchantment>)$$7.getKey(), $$7.getIntValue()));
            }
         }
      }
   }

   private static <T> HolderSet<T> getTagOrEmpty(@Nullable HolderLookup.Provider pRegistries, ResourceKey<Registry<T>> pRegistryKey, TagKey<T> pKey) {
      if (pRegistries != null) {
         Optional<HolderSet.Named<T>> $$3 = pRegistries.lookupOrThrow(pRegistryKey).get(pKey);
         if ($$3.isPresent()) {
            return $$3.get();
         }
      }

      return HolderSet.direct();
   }

   public ItemEnchantments withTooltip(boolean pShowInTooltip) {
      return new ItemEnchantments(this.enchantments, pShowInTooltip);
   }

   public Set<Holder<Enchantment>> keySet() {
      return Collections.unmodifiableSet(this.enchantments.keySet());
   }

   public Set<Entry<Holder<Enchantment>>> entrySet() {
      return Collections.unmodifiableSet(this.enchantments.object2IntEntrySet());
   }

   public int size() {
      return this.enchantments.size();
   }

   public boolean isEmpty() {
      return this.enchantments.isEmpty();
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         return !(pOther instanceof ItemEnchantments $$1) ? false : this.showInTooltip == $$1.showInTooltip && this.enchantments.equals($$1.enchantments);
      }
   }

   @Override
   public int hashCode() {
      int $$0 = this.enchantments.hashCode();
      return 31 * $$0 + (this.showInTooltip ? 1 : 0);
   }

   @Override
   public String toString() {
      return "ItemEnchantments{enchantments=" + this.enchantments + ", showInTooltip=" + this.showInTooltip + "}";
   }

   public static class Mutable {
      private final Object2IntOpenHashMap<Holder<Enchantment>> enchantments = new Object2IntOpenHashMap();
      private final boolean showInTooltip;

      public Mutable(ItemEnchantments pEnchantments) {
         this.enchantments.putAll(pEnchantments.enchantments);
         this.showInTooltip = pEnchantments.showInTooltip;
      }

      public void set(Holder<Enchantment> pEnchantment, int pLevel) {
         if (pLevel <= 0) {
            this.enchantments.removeInt(pEnchantment);
         } else {
            this.enchantments.put(pEnchantment, Math.min(pLevel, 255));
         }
      }

      public void upgrade(Holder<Enchantment> pEnchantment, int pLevel) {
         if (pLevel > 0) {
            this.enchantments.merge(pEnchantment, Math.min(pLevel, 255), Integer::max);
         }
      }

      public void removeIf(Predicate<Holder<Enchantment>> pPredicate) {
         this.enchantments.keySet().removeIf(pPredicate);
      }

      public int getLevel(Holder<Enchantment> pEnchantment) {
         return this.enchantments.getOrDefault(pEnchantment, 0);
      }

      public Set<Holder<Enchantment>> keySet() {
         return this.enchantments.keySet();
      }

      public ItemEnchantments toImmutable() {
         return new ItemEnchantments(this.enchantments, this.showInTooltip);
      }
   }
}
