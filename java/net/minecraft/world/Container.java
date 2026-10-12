package net.minecraft.world;

import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface Container extends Clearable {
   float DEFAULT_DISTANCE_BUFFER = 4.0F;

   int getContainerSize();

   boolean isEmpty();

   ItemStack getItem(int pSlot);

   ItemStack removeItem(int pSlot, int pAmount);

   ItemStack removeItemNoUpdate(int pSlot);

   void setItem(int pSlot, ItemStack pStack);

   default int getMaxStackSize() {
      return 99;
   }

   default int getMaxStackSize(ItemStack pStack) {
      return Math.min(this.getMaxStackSize(), pStack.getMaxStackSize());
   }

   void setChanged();

   boolean stillValid(Player pPlayer);

   default void startOpen(Player pPlayer) {
   }

   default void stopOpen(Player pPlayer) {
   }

   default boolean canPlaceItem(int pSlot, ItemStack pStack) {
      return true;
   }

   default boolean canTakeItem(Container pTarget, int pSlot, ItemStack pStack) {
      return true;
   }

   default int countItem(Item pItem) {
      int $$1 = 0;

      for (int $$2 = 0; $$2 < this.getContainerSize(); $$2++) {
         ItemStack $$3 = this.getItem($$2);
         if ($$3.getItem().equals(pItem)) {
            $$1 += $$3.getCount();
         }
      }

      return $$1;
   }

   default boolean hasAnyOf(Set<Item> pSet) {
      return this.hasAnyMatching(p_216873_ -> !p_216873_.isEmpty() && pSet.contains(p_216873_.getItem()));
   }

   default boolean hasAnyMatching(Predicate<ItemStack> pPredicate) {
      for (int $$1 = 0; $$1 < this.getContainerSize(); $$1++) {
         ItemStack $$2 = this.getItem($$1);
         if (pPredicate.test($$2)) {
            return true;
         }
      }

      return false;
   }

   static boolean stillValidBlockEntity(BlockEntity pBlockEntity, Player pPlayer) {
      return stillValidBlockEntity(pBlockEntity, pPlayer, 4.0F);
   }

   static boolean stillValidBlockEntity(BlockEntity pBlockEntity, Player pPlayer, float pDistance) {
      Level $$3 = pBlockEntity.getLevel();
      BlockPos $$4 = pBlockEntity.getBlockPos();
      if ($$3 == null) {
         return false;
      } else {
         return $$3.getBlockEntity($$4) != pBlockEntity ? false : pPlayer.canInteractWithBlock($$4, (double)pDistance);
      }
   }
}
