package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownEgg extends ThrowableItemProjectile {
   private static final EntityDimensions ZERO_SIZED_DIMENSIONS = EntityDimensions.fixed(0.0F, 0.0F);

   public ThrownEgg(EntityType<? extends ThrownEgg> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public ThrownEgg(Level pLevel, LivingEntity pShooter) {
      super(EntityType.EGG, pShooter, pLevel);
   }

   public ThrownEgg(Level pLevel, double pX, double pY, double pZ) {
      super(EntityType.EGG, pX, pY, pZ, pLevel);
   }

   @Override
   public void handleEntityEvent(byte pId) {
      if (pId == 3) {
         double $$1 = 0.08;

         for (int $$2 = 0; $$2 < 8; $$2++) {
            this.level()
               .addParticle(
                  new ItemParticleOption(ParticleTypes.ITEM, this.getItem()),
                  this.getX(),
                  this.getY(),
                  this.getZ(),
                  ((double)this.random.nextFloat() - 0.5) * 0.08,
                  ((double)this.random.nextFloat() - 0.5) * 0.08,
                  ((double)this.random.nextFloat() - 0.5) * 0.08
               );
         }
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult pResult) {
      super.onHitEntity(pResult);
      pResult.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0F);
   }

   @Override
   protected void onHit(HitResult pResult) {
      super.onHit(pResult);
      if (!this.level().isClientSide) {
         if (this.random.nextInt(8) == 0) {
            int $$1 = 1;
            if (this.random.nextInt(32) == 0) {
               $$1 = 4;
            }

            for (int $$2 = 0; $$2 < $$1; $$2++) {
               Chicken $$3 = EntityType.CHICKEN.create(this.level());
               if ($$3 != null) {
                  $$3.setAge(-24000);
                  $$3.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                  if (!$$3.fudgePositionAfterSizeChange(ZERO_SIZED_DIMENSIONS)) {
                     break;
                  }

                  this.level().addFreshEntity($$3);
               }
            }
         }

         this.level().broadcastEntityEvent(this, (byte)3);
         this.discard();
      }
   }

   @Override
   protected Item getDefaultItem() {
      return Items.EGG;
   }
}
