package net.minecraft.world.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraft.world.level.Level;

class WindChargedMobEffect extends MobEffect {
   protected WindChargedMobEffect(MobEffectCategory p_332863_, int p_333215_) {
      super(p_332863_, p_333215_, ParticleTypes.SMALL_GUST);
   }

   @Override
   public void onMobRemoved(LivingEntity p_333151_, int p_331087_, Entity.RemovalReason p_335248_) {
      if (p_335248_ == Entity.RemovalReason.KILLED && p_333151_.level() instanceof ServerLevel $$3) {
         double $$4 = p_333151_.getX();
         double $$5 = p_333151_.getY() + (double)(p_333151_.getBbHeight() / 2.0F);
         double $$6 = p_333151_.getZ();
         float $$7 = 3.0F + p_333151_.getRandom().nextFloat() * 2.0F;
         $$3.explode(
            p_333151_,
            null,
            AbstractWindCharge.EXPLOSION_DAMAGE_CALCULATOR,
            $$4,
            $$5,
            $$6,
            $$7,
            false,
            Level.ExplosionInteraction.TRIGGER,
            ParticleTypes.GUST_EMITTER_SMALL,
            ParticleTypes.GUST_EMITTER_LARGE,
            SoundEvents.BREEZE_WIND_CHARGE_BURST
         );
      }
   }
}
