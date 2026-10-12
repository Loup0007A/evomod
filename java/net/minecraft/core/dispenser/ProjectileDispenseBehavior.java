package net.minecraft.core.dispenser;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class ProjectileDispenseBehavior extends DefaultDispenseItemBehavior {
   private final ProjectileItem projectileItem;
   private final ProjectileItem.DispenseConfig dispenseConfig;

   public ProjectileDispenseBehavior(Item pProjectile) {
      if (pProjectile instanceof ProjectileItem $$1) {
         this.projectileItem = $$1;
         this.dispenseConfig = $$1.createDispenseConfig();
      } else {
         throw new IllegalArgumentException(pProjectile + " not instance of " + ProjectileItem.class.getSimpleName());
      }
   }

   @Override
   public ItemStack execute(BlockSource pBlockSource, ItemStack pItem) {
      Level $$2 = pBlockSource.level();
      Direction $$3 = pBlockSource.state().getValue(DispenserBlock.FACING);
      Position $$4 = this.dispenseConfig.positionFunction().getDispensePosition(pBlockSource, $$3);
      Projectile $$5 = this.projectileItem.asProjectile($$2, $$4, pItem, $$3);
      this.projectileItem
         .shoot($$5, (double)$$3.getStepX(), (double)$$3.getStepY(), (double)$$3.getStepZ(), this.dispenseConfig.power(), this.dispenseConfig.uncertainty());
      $$2.addFreshEntity($$5);
      pItem.shrink(1);
      return pItem;
   }

   @Override
   protected void playSound(BlockSource pBlockSource) {
      pBlockSource.level().levelEvent(this.dispenseConfig.overrideDispenseEvent().orElse(1002), pBlockSource.pos(), 0);
   }
}
