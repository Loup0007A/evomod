package net.minecraft.advancements.critereon;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

public interface SingleComponentItemPredicate<T> extends ItemSubPredicate {
   @Override
   default boolean matches(ItemStack pStack) {
      T $$1 = pStack.get(this.componentType());
      return $$1 != null && this.matches(pStack, $$1);
   }

   DataComponentType<T> componentType();

   boolean matches(ItemStack pStack, T pValue);
}
