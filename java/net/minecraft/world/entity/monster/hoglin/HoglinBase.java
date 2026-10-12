package net.minecraft.world.entity.monster.hoglin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public interface HoglinBase {
   int ATTACK_ANIMATION_DURATION = 10;

   int getAttackAnimationRemainingTicks();

   static boolean hurtAndThrowTarget(LivingEntity pHoglin, LivingEntity pTarget) {
      float $$2 = (float)pHoglin.getAttributeValue(Attributes.ATTACK_DAMAGE);
      float $$3;
      if (!pHoglin.isBaby() && (int)$$2 > 0) {
         $$3 = $$2 / 2.0F + (float)pHoglin.level().random.nextInt((int)$$2);
      } else {
         $$3 = $$2;
      }

      DamageSource $$5 = pHoglin.damageSources().mobAttack(pHoglin);
      boolean $$6 = pTarget.hurt($$5, $$3);
      if ($$6) {
         if (pHoglin.level() instanceof ServerLevel $$7) {
            EnchantmentHelper.doPostAttackEffects($$7, pTarget, $$5);
         }

         if (!pHoglin.isBaby()) {
            throwTarget(pHoglin, pTarget);
         }
      }

      return $$6;
   }

   static void throwTarget(LivingEntity pHoglin, LivingEntity pTarget) {
      double $$2 = pHoglin.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
      double $$3 = pTarget.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
      double $$4 = $$2 - $$3;
      if (!($$4 <= 0.0)) {
         double $$5 = pTarget.getX() - pHoglin.getX();
         double $$6 = pTarget.getZ() - pHoglin.getZ();
         float $$7 = (float)(pHoglin.level().random.nextInt(21) - 10);
         double $$8 = $$4 * (double)(pHoglin.level().random.nextFloat() * 0.5F + 0.2F);
         Vec3 $$9 = new Vec3($$5, 0.0, $$6).normalize().scale($$8).yRot($$7);
         double $$10 = $$4 * (double)pHoglin.level().random.nextFloat() * 0.5;
         pTarget.push($$9.x, $$10, $$9.z);
         pTarget.hurtMarked = true;
      }
   }
}
