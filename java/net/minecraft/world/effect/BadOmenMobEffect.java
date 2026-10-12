package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;

class BadOmenMobEffect extends MobEffect {
   protected BadOmenMobEffect(MobEffectCategory p_298574_, int p_301000_) {
      super(p_298574_, p_301000_);
   }

   @Override
   public boolean shouldApplyEffectTickThisTick(int p_297444_, int p_300866_) {
      return true;
   }

   @Override
   public boolean applyEffectTick(LivingEntity p_299568_, int p_299125_) {
      if (p_299568_ instanceof ServerPlayer $$2 && !$$2.isSpectator()) {
         ServerLevel $$3 = $$2.serverLevel();
         if ($$3.getDifficulty() != Difficulty.PEACEFUL && $$3.isVillage($$2.blockPosition())) {
            Raid $$4 = $$3.getRaidAt($$2.blockPosition());
            if ($$4 == null || $$4.getRaidOmenLevel() < $$4.getMaxRaidOmenLevel()) {
               $$2.addEffect(new MobEffectInstance(MobEffects.RAID_OMEN, 600, p_299125_));
               $$2.setRaidOmenPosition($$2.blockPosition());
               return false;
            }
         }
      }

      return true;
   }
}
