package net.minecraft.world.entity.monster.breeze;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.projectile.windcharge.BreezeWindCharge;
import net.minecraft.world.phys.Vec3;

public class Shoot extends Behavior<Breeze> {
   private static final int ATTACK_RANGE_MIN_SQRT = 4;
   private static final int ATTACK_RANGE_MAX_SQRT = 256;
   private static final int UNCERTAINTY_BASE = 5;
   private static final int UNCERTAINTY_MULTIPLIER = 4;
   private static final float PROJECTILE_MOVEMENT_SCALE = 0.7F;
   private static final int SHOOT_INITIAL_DELAY_TICKS = Math.round(15.0F);
   private static final int SHOOT_RECOVER_DELAY_TICKS = Math.round(4.0F);
   private static final int SHOOT_COOLDOWN_TICKS = Math.round(10.0F);

   @VisibleForTesting
   public Shoot() {
      super(
         ImmutableMap.of(
            MemoryModuleType.ATTACK_TARGET,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.BREEZE_SHOOT_COOLDOWN,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_SHOOT_CHARGING,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_SHOOT_RECOVERING,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_SHOOT,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.BREEZE_JUMP_TARGET,
            MemoryStatus.VALUE_ABSENT
         ),
         SHOOT_INITIAL_DELAY_TICKS + 1 + SHOOT_RECOVER_DELAY_TICKS
      );
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, Breeze pOwner) {
      return pOwner.getPose() != Pose.STANDING
         ? false
         : pOwner.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).map(p_311282_ -> isTargetWithinRange(pOwner, p_311282_)).map(p_311912_ -> {
            if (!p_311912_) {
               pOwner.getBrain().eraseMemory(MemoryModuleType.BREEZE_SHOOT);
            }

            return (Boolean)p_311912_;
         }).orElse(false);
   }

   protected boolean canStillUse(ServerLevel pLevel, Breeze pEntity, long pGameTime) {
      return pEntity.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && pEntity.getBrain().hasMemoryValue(MemoryModuleType.BREEZE_SHOOT);
   }

   protected void start(ServerLevel pLevel, Breeze pEntity, long pGameTime) {
      pEntity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent(p_312466_ -> pEntity.setPose(Pose.SHOOTING));
      pEntity.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_CHARGING, Unit.INSTANCE, (long)SHOOT_INITIAL_DELAY_TICKS);
      pEntity.playSound(SoundEvents.BREEZE_INHALE, 1.0F, 1.0F);
   }

   protected void stop(ServerLevel pLevel, Breeze pEntity, long pGameTime) {
      if (pEntity.getPose() == Pose.SHOOTING) {
         pEntity.setPose(Pose.STANDING);
      }

      pEntity.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_COOLDOWN, Unit.INSTANCE, (long)SHOOT_COOLDOWN_TICKS);
      pEntity.getBrain().eraseMemory(MemoryModuleType.BREEZE_SHOOT);
   }

   protected void tick(ServerLevel pLevel, Breeze pOwner, long pGameTime) {
      Brain<Breeze> $$3 = pOwner.getBrain();
      LivingEntity $$4 = $$3.getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
      if ($$4 != null) {
         pOwner.lookAt(EntityAnchorArgument.Anchor.EYES, $$4.position());
         if (!$$3.getMemory(MemoryModuleType.BREEZE_SHOOT_CHARGING).isPresent() && !$$3.getMemory(MemoryModuleType.BREEZE_SHOOT_RECOVERING).isPresent()) {
            $$3.setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT_RECOVERING, Unit.INSTANCE, (long)SHOOT_RECOVER_DELAY_TICKS);
            if (isFacingTarget(pOwner, $$4)) {
               double $$5 = $$4.getX() - pOwner.getX();
               double $$6 = $$4.getY($$4.isPassenger() ? 0.8 : 0.3) - pOwner.getY(0.5);
               double $$7 = $$4.getZ() - pOwner.getZ();
               BreezeWindCharge $$8 = new BreezeWindCharge(pOwner, pLevel);
               pOwner.playSound(SoundEvents.BREEZE_SHOOT, 1.5F, 1.0F);
               $$8.shoot($$5, $$6, $$7, 0.7F, (float)(5 - pLevel.getDifficulty().getId() * 4));
               pLevel.addFreshEntity($$8);
            }
         }
      }
   }

   @VisibleForTesting
   public static boolean isFacingTarget(Breeze pBreeze, LivingEntity pTarget) {
      Vec3 $$2 = pBreeze.getViewVector(1.0F);
      Vec3 $$3 = pTarget.position().subtract(pBreeze.position()).normalize();
      return $$2.dot($$3) > 0.5;
   }

   private static boolean isTargetWithinRange(Breeze pBreeze, LivingEntity pTarget) {
      double $$2 = pBreeze.position().distanceToSqr(pTarget.position());
      return $$2 > 4.0 && $$2 < 256.0;
   }
}
