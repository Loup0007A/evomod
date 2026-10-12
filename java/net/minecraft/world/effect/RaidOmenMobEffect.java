package net.minecraft.world.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

class RaidOmenMobEffect extends MobEffect {
   protected RaidOmenMobEffect(MobEffectCategory p_329670_, int p_332984_, ParticleOptions p_332864_) {
      super(p_329670_, p_332984_, p_332864_);
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int p_331901_, int p_333973_) {
      return p_331901_ == 1;
   }

   @Override
   public boolean applyEffectTick(LivingEntity p_329323_, int p_331707_) {
      if (p_329323_ instanceof ServerPlayer $$2 && !p_329323_.isSpectator()) {
         ServerLevel $$3 = $$2.serverLevel();
         BlockPos $$4 = $$2.getRaidOmenPosition();
         if ($$4 != null) {
            $$3.getRaids().createOrExtendRaid($$2, $$4);
            $$2.clearRaidOmenPosition();
            return false;
         }
      }

      return true;
   }
}
