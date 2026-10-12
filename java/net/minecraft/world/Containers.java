package net.minecraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class Containers {
   public static void dropContents(Level pLevel, BlockPos pPos, Container pInventory) {
      dropContents(pLevel, (double)pPos.getX(), (double)pPos.getY(), (double)pPos.getZ(), pInventory);
   }

   public static void dropContents(Level pLevel, Entity pEntityAt, Container pInventory) {
      dropContents(pLevel, pEntityAt.getX(), pEntityAt.getY(), pEntityAt.getZ(), pInventory);
   }

   private static void dropContents(Level pLevel, double pX, double pY, double pZ, Container pInventory) {
      for (int $$5 = 0; $$5 < pInventory.getContainerSize(); $$5++) {
         dropItemStack(pLevel, pX, pY, pZ, pInventory.getItem($$5));
      }
   }

   public static void dropContents(Level pLevel, BlockPos pPos, NonNullList<ItemStack> pStackList) {
      pStackList.forEach(p_19009_ -> dropItemStack(pLevel, (double)pPos.getX(), (double)pPos.getY(), (double)pPos.getZ(), p_19009_));
   }

   public static void dropItemStack(Level pLevel, double pX, double pY, double pZ, ItemStack pStack) {
      double $$5 = (double)EntityType.ITEM.getWidth();
      double $$6 = 1.0 - $$5;
      double $$7 = $$5 / 2.0;
      double $$8 = Math.floor(pX) + pLevel.random.nextDouble() * $$6 + $$7;
      double $$9 = Math.floor(pY) + pLevel.random.nextDouble() * $$6;
      double $$10 = Math.floor(pZ) + pLevel.random.nextDouble() * $$6 + $$7;

      while (!pStack.isEmpty()) {
         ItemEntity $$11 = new ItemEntity(pLevel, $$8, $$9, $$10, pStack.split(pLevel.random.nextInt(21) + 10));
         float $$12 = 0.05F;
         $$11.setDeltaMovement(
            pLevel.random.triangle(0.0, 0.11485000171139836),
            pLevel.random.triangle(0.2, 0.11485000171139836),
            pLevel.random.triangle(0.0, 0.11485000171139836)
         );
         pLevel.addFreshEntity($$11);
      }
   }

   public static void dropContentsOnDestroy(BlockState pState, BlockState pNewState, Level pLevel, BlockPos pPos) {
      if (!pState.is(pNewState.getBlock())) {
         if (pLevel.getBlockEntity(pPos) instanceof Container $$5) {
            dropContents(pLevel, pPos, $$5);
            pLevel.updateNeighbourForOutputSignal(pPos, pState.getBlock());
         }
      }
   }
}
