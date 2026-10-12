package net.minecraft.world.effect;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class MobEffectUtil {
   public static Component formatDuration(MobEffectInstance pEffect, float pDurationFactor, float pTicksPerSecond) {
      if (pEffect.isInfiniteDuration()) {
         return Component.translatable("effect.duration.infinite");
      } else {
         int $$3 = Mth.floor((float)pEffect.getDuration() * pDurationFactor);
         return Component.literal(StringUtil.formatTickDuration($$3, pTicksPerSecond));
      }
   }

   public static boolean hasDigSpeed(LivingEntity pEntity) {
      return pEntity.hasEffect(MobEffects.DIG_SPEED) || pEntity.hasEffect(MobEffects.CONDUIT_POWER);
   }

   public static int getDigSpeedAmplification(LivingEntity pEntity) {
      int $$1 = 0;
      int $$2 = 0;
      if (pEntity.hasEffect(MobEffects.DIG_SPEED)) {
         $$1 = pEntity.getEffect(MobEffects.DIG_SPEED).getAmplifier();
      }

      if (pEntity.hasEffect(MobEffects.CONDUIT_POWER)) {
         $$2 = pEntity.getEffect(MobEffects.CONDUIT_POWER).getAmplifier();
      }

      return Math.max($$1, $$2);
   }

   public static boolean hasWaterBreathing(LivingEntity pEntity) {
      return pEntity.hasEffect(MobEffects.WATER_BREATHING) || pEntity.hasEffect(MobEffects.CONDUIT_POWER);
   }

   public static List<ServerPlayer> addEffectToPlayersAround(
      ServerLevel pLevel, @Nullable Entity pSource, Vec3 pPos, double pRadius, MobEffectInstance pEffect, int pDuration
   ) {
      Holder<MobEffect> $$6 = pEffect.getEffect();
      List<ServerPlayer> $$7 = pLevel.getPlayers(
         p_267925_ -> p_267925_.gameMode.isSurvival()
               && (pSource == null || !pSource.isAlliedTo(p_267925_))
               && pPos.closerThan(p_267925_.position(), pRadius)
               && (
                  !p_267925_.hasEffect($$6)
                     || p_267925_.getEffect($$6).getAmplifier() < pEffect.getAmplifier()
                     || p_267925_.getEffect($$6).endsWithin(pDuration - 1)
               )
      );
      $$7.forEach(p_238232_ -> p_238232_.addEffect(new MobEffectInstance(pEffect), pSource));
      return $$7;
   }
}
