package net.minecraft.world.item.component;

import com.google.common.collect.Iterables;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Stream;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public final class ItemContainerContents {
   private static final int NO_SLOT = -1;
   private static final int MAX_SIZE = 256;
   public static final ItemContainerContents EMPTY = new ItemContainerContents(NonNullList.create());
   public static final Codec<ItemContainerContents> CODEC = ItemContainerContents.Slot.CODEC
      .sizeLimitedListOf(256)
      .xmap(ItemContainerContents::fromSlots, ItemContainerContents::asSlots);
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemContainerContents> STREAM_CODEC = ItemStack.OPTIONAL_STREAM_CODEC
      .apply(ByteBufCodecs.list(256))
      .map(ItemContainerContents::new, p_333580_ -> p_333580_.items);
   private final NonNullList<ItemStack> items;
   private final int hashCode;

   private ItemContainerContents(NonNullList<ItemStack> pItems) {
      if (pItems.size() > 256) {
         throw new IllegalArgumentException("Got " + pItems.size() + " items, but maximum is 256");
      } else {
         this.items = pItems;
         this.hashCode = ItemStack.hashStackList(pItems);
      }
   }

   private ItemContainerContents(int pSize) {
      this(NonNullList.withSize(pSize, ItemStack.EMPTY));
   }

   private ItemContainerContents(List<ItemStack> p_332487_) {
      this(p_332487_.size());

      for (int $$1 = 0; $$1 < p_332487_.size(); $$1++) {
         this.items.set($$1, p_332487_.get($$1));
      }
   }

   private static ItemContainerContents fromSlots(List<ItemContainerContents.Slot> p_334537_) {
      OptionalInt $$1 = p_334537_.stream().mapToInt(ItemContainerContents.Slot::index).max();
      if ($$1.isEmpty()) {
         return EMPTY;
      } else {
         ItemContainerContents $$2 = new ItemContainerContents($$1.getAsInt() + 1);

         for (ItemContainerContents.Slot $$3 : p_334537_) {
            $$2.items.set($$3.index(), $$3.item());
         }

         return $$2;
      }
   }

   public static ItemContainerContents fromItems(List<ItemStack> pItems) {
      int $$1 = findLastNonEmptySlot(pItems);
      if ($$1 == -1) {
         return EMPTY;
      } else {
         ItemContainerContents $$2 = new ItemContainerContents($$1 + 1);

         for (int $$3 = 0; $$3 <= $$1; $$3++) {
            $$2.items.set($$3, pItems.get($$3).copy());
         }

         return $$2;
      }
   }

   private static int findLastNonEmptySlot(List<ItemStack> pItems) {
      for (int $$1 = pItems.size() - 1; $$1 >= 0; $$1--) {
         if (!pItems.get($$1).isEmpty()) {
            return $$1;
         }
      }

      return -1;
   }

   private List<ItemContainerContents.Slot> asSlots() {
      List<ItemContainerContents.Slot> $$0 = new ArrayList<>();

      for (int $$1 = 0; $$1 < this.items.size(); $$1++) {
         ItemStack $$2 = this.items.get($$1);
         if (!$$2.isEmpty()) {
            $$0.add(new ItemContainerContents.Slot($$1, $$2));
         }
      }

      return $$0;
   }

   public void copyInto(NonNullList<ItemStack> pList) {
      for (int $$1 = 0; $$1 < pList.size(); $$1++) {
         ItemStack $$2 = $$1 < this.items.size() ? this.items.get($$1) : ItemStack.EMPTY;
         pList.set($$1, $$2.copy());
      }
   }

   public ItemStack copyOne() {
      return this.items.isEmpty() ? ItemStack.EMPTY : this.items.get(0).copy();
   }

   public Stream<ItemStack> stream() {
      return this.items.stream().map(ItemStack::copy);
   }

   public Stream<ItemStack> nonEmptyStream() {
      return this.items.stream().filter(p_332163_ -> !p_332163_.isEmpty()).map(ItemStack::copy);
   }

   public Iterable<ItemStack> nonEmptyItems() {
      return Iterables.filter(this.items, p_330818_ -> !p_330818_.isEmpty());
   }

   public Iterable<ItemStack> nonEmptyItemsCopy() {
      return Iterables.transform(this.nonEmptyItems(), ItemStack::copy);
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         if (pOther instanceof ItemContainerContents $$1 && ItemStack.listMatches(this.items, $$1.items)) {
            return true;
         }

         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.hashCode;
   }

   static record Slot(int index, ItemStack item) {
      public static final Codec<ItemContainerContents.Slot> CODEC = RecordCodecBuilder.create(
         p_327964_ -> p_327964_.group(
                  Codec.intRange(0, 255).fieldOf("slot").forGetter(ItemContainerContents.Slot::index),
                  ItemStack.CODEC.fieldOf("item").forGetter(ItemContainerContents.Slot::item)
               )
               .apply(p_327964_, ItemContainerContents.Slot::new)
      );
   }
}
