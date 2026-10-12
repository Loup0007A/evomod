package net.minecraft.world.item.component;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.apache.commons.lang3.math.Fraction;

public final class BundleContents implements TooltipComponent {
   public static final BundleContents EMPTY = new BundleContents(List.of());
   public static final Codec<BundleContents> CODEC = ItemStack.CODEC.listOf().xmap(BundleContents::new, p_332949_ -> p_332949_.items);
   public static final StreamCodec<RegistryFriendlyByteBuf, BundleContents> STREAM_CODEC = ItemStack.STREAM_CODEC
      .apply(ByteBufCodecs.list())
      .map(BundleContents::new, p_328832_ -> p_328832_.items);
   private static final Fraction BUNDLE_IN_BUNDLE_WEIGHT = Fraction.getFraction(1, 16);
   private static final int NO_STACK_INDEX = -1;
   final List<ItemStack> items;
   final Fraction weight;

   BundleContents(List<ItemStack> pItems, Fraction pWeight) {
      this.items = pItems;
      this.weight = pWeight;
   }

   public BundleContents(List<ItemStack> p_334686_) {
      this(p_334686_, computeContentWeight(p_334686_));
   }

   private static Fraction computeContentWeight(List<ItemStack> pContent) {
      Fraction $$1 = Fraction.ZERO;

      for (ItemStack $$2 : pContent) {
         $$1 = $$1.add(getWeight($$2).multiplyBy(Fraction.getFraction($$2.getCount(), 1)));
      }

      return $$1;
   }

   static Fraction getWeight(ItemStack pStack) {
      BundleContents $$1 = pStack.get(DataComponents.BUNDLE_CONTENTS);
      if ($$1 != null) {
         return BUNDLE_IN_BUNDLE_WEIGHT.add($$1.weight());
      } else {
         List<BeehiveBlockEntity.Occupant> $$2 = pStack.getOrDefault(DataComponents.BEES, List.of());
         return !$$2.isEmpty() ? Fraction.ONE : Fraction.getFraction(1, pStack.getMaxStackSize());
      }
   }

   public ItemStack getItemUnsafe(int pIndex) {
      return this.items.get(pIndex);
   }

   public Stream<ItemStack> itemCopyStream() {
      return this.items.stream().map(ItemStack::copy);
   }

   public Iterable<ItemStack> items() {
      return this.items;
   }

   public Iterable<ItemStack> itemsCopy() {
      return Lists.transform(this.items, ItemStack::copy);
   }

   public int size() {
      return this.items.size();
   }

   public Fraction weight() {
      return this.weight;
   }

   public boolean isEmpty() {
      return this.items.isEmpty();
   }

   @Override
   public boolean equals(Object pOther) {
      if (this == pOther) {
         return true;
      } else {
         return !(pOther instanceof BundleContents $$1) ? false : this.weight.equals($$1.weight) && ItemStack.listMatches(this.items, $$1.items);
      }
   }

   @Override
   public int hashCode() {
      return ItemStack.hashStackList(this.items);
   }

   @Override
   public String toString() {
      return "BundleContents" + this.items;
   }

   public static class Mutable {
      private final List<ItemStack> items;
      private Fraction weight;

      public Mutable(BundleContents pContents) {
         this.items = new ArrayList<>(pContents.items);
         this.weight = pContents.weight;
      }

      public BundleContents.Mutable clearItems() {
         this.items.clear();
         this.weight = Fraction.ZERO;
         return this;
      }

      private int findStackIndex(ItemStack pStack) {
         if (!pStack.isStackable()) {
            return -1;
         } else {
            for (int $$1 = 0; $$1 < this.items.size(); $$1++) {
               if (ItemStack.isSameItemSameComponents(this.items.get($$1), pStack)) {
                  return $$1;
               }
            }

            return -1;
         }
      }

      private int getMaxAmountToAdd(ItemStack pStack) {
         Fraction $$1 = Fraction.ONE.subtract(this.weight);
         return Math.max($$1.divideBy(BundleContents.getWeight(pStack)).intValue(), 0);
      }

      public int tryInsert(ItemStack pStack) {
         if (!pStack.isEmpty() && pStack.getItem().canFitInsideContainerItems()) {
            int $$1 = Math.min(pStack.getCount(), this.getMaxAmountToAdd(pStack));
            if ($$1 == 0) {
               return 0;
            } else {
               this.weight = this.weight.add(BundleContents.getWeight(pStack).multiplyBy(Fraction.getFraction($$1, 1)));
               int $$2 = this.findStackIndex(pStack);
               if ($$2 != -1) {
                  ItemStack $$3 = this.items.remove($$2);
                  ItemStack $$4 = $$3.copyWithCount($$3.getCount() + $$1);
                  pStack.shrink($$1);
                  this.items.add(0, $$4);
               } else {
                  this.items.add(0, pStack.split($$1));
               }

               return $$1;
            }
         } else {
            return 0;
         }
      }

      public int tryTransfer(Slot pSlot, Player pPlayer) {
         ItemStack $$2 = pSlot.getItem();
         int $$3 = this.getMaxAmountToAdd($$2);
         return this.tryInsert(pSlot.safeTake($$2.getCount(), $$3, pPlayer));
      }

      @Nullable
      public ItemStack removeOne() {
         if (this.items.isEmpty()) {
            return null;
         } else {
            ItemStack $$0 = this.items.remove(0).copy();
            this.weight = this.weight.subtract(BundleContents.getWeight($$0).multiplyBy(Fraction.getFraction($$0.getCount(), 1)));
            return $$0;
         }
      }

      public Fraction weight() {
         return this.weight;
      }

      public BundleContents toImmutable() {
         return new BundleContents(List.copyOf(this.items), this.weight);
      }
   }
}
