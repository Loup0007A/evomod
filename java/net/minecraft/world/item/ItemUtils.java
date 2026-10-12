package net.minecraft.world.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ItemUtils {
   public static InteractionResultHolder<ItemStack> startUsingInstantly(Level pLevel, Player pPlayer, InteractionHand pHand) {
      pPlayer.startUsingItem(pHand);
      return InteractionResultHolder.consume(pPlayer.getItemInHand(pHand));
   }

   public static ItemStack createFilledResult(ItemStack pEmptyStack, Player pPlayer, ItemStack pFilledStack, boolean pPreventDuplicates) {
      boolean $$4 = pPlayer.hasInfiniteMaterials();
      if (pPreventDuplicates && $$4) {
         if (!pPlayer.getInventory().contains(pFilledStack)) {
            pPlayer.getInventory().add(pFilledStack);
         }

         return pEmptyStack;
      } else {
         pEmptyStack.consume(1, pPlayer);
         if (pEmptyStack.isEmpty()) {
            return pFilledStack;
         } else {
            if (!pPlayer.getInventory().add(pFilledStack)) {
               pPlayer.drop(pFilledStack, false);
            }

            return pEmptyStack;
         }
      }
   }

   public static ItemStack createFilledResult(ItemStack pEmptyStack, Player pPlayer, ItemStack pFilledStack) {
      return createFilledResult(pEmptyStack, pPlayer, pFilledStack, true);
   }

   public static void onContainerDestroyed(ItemEntity pContainer, Iterable<ItemStack> pContents) {
      Level $$2 = pContainer.level();
      if (!$$2.isClientSide) {
         pContents.forEach(p_341566_ -> $$2.addFreshEntity(new ItemEntity($$2, pContainer.getX(), pContainer.getY(), pContainer.getZ(), p_341566_)));
      }
   }
}
