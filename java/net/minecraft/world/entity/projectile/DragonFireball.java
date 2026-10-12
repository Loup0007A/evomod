package net.minecraft.world.entity.projectile;

import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class DragonFireball extends AbstractHurtingProjectile {
   public static final float SPLASH_RANGE = 4.0F;

   public DragonFireball(EntityType<? extends DragonFireball> pEntityType, Level pLevel) {
      super(pEntityType, pLevel);
   }

   public DragonFireball(Level pLevel, LivingEntity pOwner, Vec3 pMovement) {
      super(EntityType.DRAGON_FIREBALL, pOwner, pMovement, pLevel);
   }

   @Override
   protected void onHit(HitResult pResult) {
      super.onHit(pResult);
      if (pResult.getType() != HitResult.Type.ENTITY || !this.ownedBy(((EntityHitResult)pResult).getEntity())) {
         if (!this.level().isClientSide) {
            List<LivingEntity> $$1 = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.0, 2.0, 4.0));
            AreaEffectCloud $$2 = new AreaEffectCloud(this.level(), this.getX(), this.getY(), this.getZ());
            Entity $$3 = this.getOwner();
            if ($$3 instanceof LivingEntity) {
               $$2.setOwner((LivingEntity)$$3);
            }

            $$2.setParticle(ParticleTypes.DRAGON_BREATH);
            $$2.setRadius(3.0F);
            $$2.setDuration(600);
            $$2.setRadiusPerTick((7.0F - $$2.getRadius()) / (float)$$2.getDuration());
            $$2.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 1));
            if (!$$1.isEmpty()) {
               for (LivingEntity $$4 : $$1) {
                  double $$5 = this.distanceToSqr($$4);
                  if ($$5 < 16.0) {
                     $$2.setPos($$4.getX(), $$4.getY(), $$4.getZ());
                     break;
                  }
               }
            }

            this.level().levelEvent(2006, this.blockPosition(), this.isSilent() ? -1 : 1);
            this.level().addFreshEntity($$2);
            this.discard();
         }
      }
   }

   @Override
   public boolean hurt(DamageSource pSource, float pAmount) {
      return false;
   }

   @Override
   protected ParticleOptions getTrailParticle() {
      return ParticleTypes.DRAGON_BREATH;
   }

   @Override
   protected boolean shouldBurn() {
      return false;
   }
}
