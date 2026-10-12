package net.minecraft.core.dispenser;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class DefaultDispenseItemBehavior implements DispenseItemBehavior {
   private static final int DEFAULT_ACCURACY = 6;

   @Override
   public final ItemStack dispense(BlockSource pBlockSource, ItemStack pItem) {
      ItemStack $$2 = this.execute(pBlockSource, pItem);
      this.playSound(pBlockSource);
      this.playAnimation(pBlockSource, pBlockSource.state().getValue(DispenserBlock.FACING));
      return $$2;
   }

   protected ItemStack execute(BlockSource pBlockSource, ItemStack pItem) {
      Direction $$2 = pBlockSource.state().getValue(DispenserBlock.FACING);
      Position $$3 = DispenserBlock.getDispensePosition(pBlockSource);
      ItemStack $$4 = pItem.split(1);
      spawnItem(pBlockSource.level(), $$4, 6, $$2, $$3);
      return pItem;
   }

   public static void spawnItem(Level pLevel, ItemStack pStack, int pSpeed, Direction pFacing, Position pPosition) {
      double $$5 = pPosition.x();
      double $$6 = pPosition.y();
      double $$7 = pPosition.z();
      if (pFacing.getAxis() == Direction.Axis.Y) {
         $$6 -= 0.125;
      } else {
         $$6 -= 0.15625;
      }

      ItemEntity $$8 = new ItemEntity(pLevel, $$5, $$6, $$7, pStack);
      double $$9 = pLevel.random.nextDouble() * 0.1 + 0.2;
      $$8.setDeltaMovement(
         pLevel.random.triangle((double)pFacing.getStepX() * $$9, 0.0172275 * (double)pSpeed),
         pLevel.random.triangle(0.2, 0.0172275 * (double)pSpeed),
         pLevel.random.triangle((double)pFacing.getStepZ() * $$9, 0.0172275 * (double)pSpeed)
      );
      pLevel.addFreshEntity($$8);
   }

   protected void playSound(BlockSource pBlockSource) {
      playDefaultSound(pBlockSource);
   }

   protected void playAnimation(BlockSource pBlockSource, Direction pDirection) {
      playDefaultAnimation(pBlockSource, pDirection);
   }

   private static void playDefaultSound(BlockSource pBlockSource) {
      pBlockSource.level().levelEvent(1000, pBlockSource.pos(), 0);
   }

   private static void playDefaultAnimation(BlockSource pBlockSource, Direction pDirection) {
      pBlockSource.level().levelEvent(2000, pBlockSource.pos(), pDirection.get3DDataValue());
   }

   protected ItemStack consumeWithRemainder(BlockSource pBlockSource, ItemStack pStack, ItemStack pRemainder) {
      pStack.shrink(1);
      if (pStack.isEmpty()) {
         return pRemainder;
      } else {
         this.addToInventoryOrDispense(pBlockSource, pRemainder);
         return pStack;
      }
   }

   private void addToInventoryOrDispense(BlockSource pBlockSource, ItemStack pRemainder) {
      ItemStack $$2 = pBlockSource.blockEntity().insertItem(pRemainder);
      if (!$$2.isEmpty()) {
         Direction $$3 = pBlockSource.state().getValue(DispenserBlock.FACING);
         spawnItem(pBlockSource.level(), $$2, 6, $$3, DispenserBlock.getDispensePosition(pBlockSource));
         playDefaultSound(pBlockSource);
         playDefaultAnimation(pBlockSource, $$3);
      }
   }
}
