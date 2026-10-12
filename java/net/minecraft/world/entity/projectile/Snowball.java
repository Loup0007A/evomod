package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class Snowball extends ThrowableItemProjectile {
   public Snowball(EntityType<? extends Snowball> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public Snowball(Level pLevel, LivingEntity pShooter) {
      super(EntityType.SNOWBALL, pShooter, pLevel);
   }

   public Snowball(Level pLevel, double pX, double pY, double pZ) {
      super(EntityType.SNOWBALL, pX, pY, pZ, pLevel);
   }

   @Override
   protected Item getDefaultItem() {
      return Items.SNOWBALL;
   }

   private ParticleOptions getParticle() {
      ItemStack $$0 = this.getItem();
      return (ParticleOptions)(!$$0.isEmpty() && !$$0.is(this.getDefaultItem()) ? new ItemParticleOption(ParticleTypes.ITEM, $$0) : ParticleTypes.ITEM_SNOWBALL);
   }

   @Override
   public void handleEntityEvent(byte pId) {
      if (pId == 3) {
         ParticleOptions $$1 = this.getParticle();

         for (int $$2 = 0; $$2 < 8; $$2++) {
            this.level().addParticle($$1, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
         }
      }
   }

   @Override
   protected void onHitEntity(EntityHitResult pResult) {
      super.onHitEntity(pResult);
      Entity $$1 = pResult.getEntity();
      int $$2 = $$1 instanceof Blaze ? 3 : 0;
      $$1.hurt(this.damageSources().thrown(this, this.getOwner()), (float)$$2);
   }

   @Override
   protected void onHit(HitResult pResult) {
      super.onHit(pResult);
      if (!this.level().isClientSide) {
         this.level().broadcastEntityEvent(this, (byte)3);
         this.discard();
      }
   }
}
