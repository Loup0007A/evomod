package net.minecraft.world.level.storage.loot;

import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

public interface ContainerComponentManipulator<T> {
   DataComponentType<T> type();

   T empty();

   T setContents(T pContents, Stream<ItemStack> pItems);

   Stream<ItemStack> getContents(T pContents);

   default void setContents(ItemStack pStack, T pContents, Stream<ItemStack> pItems) {
      T $$3 = pStack.getOrDefault(this.type(), pContents);
      T $$4 = this.setContents($$3, pItems);
      pStack.set(this.type(), $$4);
   }

   default void setContents(ItemStack pStack, Stream<ItemStack> pItems) {
      this.setContents(pStack, this.empty(), pItems);
   }

   default void modifyItems(ItemStack pStack, UnaryOperator<ItemStack> pModifier) {
      T $$2 = pStack.get(this.type());
      if ($$2 != null) {
         UnaryOperator<ItemStack> $$3 = p_341972_ -> {
            if (p_341972_.isEmpty()) {
               return p_341972_;
            } else {
               ItemStack $$2x = pModifier.apply(p_341972_);
               $$2x.limitSize($$2x.getMaxStackSize());
               return $$2x;
            }
         };
         this.setContents(pStack, this.getContents($$2).map($$3));
      }
   }
}
