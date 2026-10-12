package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public class RamTarget extends Behavior<Goat> {
   public static final int TIME_OUT_DURATION = 200;
   public static final float RAM_SPEED_FORCE_FACTOR = 1.65F;
   private final Function<Goat, UniformInt> getTimeBetweenRams;
   private final TargetingConditions ramTargeting;
   private final float speed;
   private final ToDoubleFunction<Goat> getKnockbackForce;
   private Vec3 ramDirection;
   private final Function<Goat, SoundEvent> getImpactSound;
   private final Function<Goat, SoundEvent> getHornBreakSound;

   public RamTarget(
      Function<Goat, UniformInt> pGetTimeBetweenRams,
      TargetingConditions pRamTargeting,
      float pSpeed,
      ToDoubleFunction<Goat> pGetKnockbackForce,
      Function<Goat, SoundEvent> pGetImpactSound,
      Function<Goat, SoundEvent> pGetHornBreakSound
   ) {
      super(ImmutableMap.of(MemoryModuleType.RAM_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT, MemoryModuleType.RAM_TARGET, MemoryStatus.VALUE_PRESENT), 200);
      this.getTimeBetweenRams = pGetTimeBetweenRams;
      this.ramTargeting = pRamTargeting;
      this.speed = pSpeed;
      this.getKnockbackForce = pGetKnockbackForce;
      this.getImpactSound = pGetImpactSound;
      this.getHornBreakSound = pGetHornBreakSound;
      this.ramDirection = Vec3.ZERO;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, Goat pOwner) {
      return pOwner.getBrain().hasMemoryValue(MemoryModuleType.RAM_TARGET);
   }

   protected boolean canStillUse(ServerLevel pLevel, Goat pEntity, long pGameTime) {
      return pEntity.getBrain().hasMemoryValue(MemoryModuleType.RAM_TARGET);
   }

   protected void start(ServerLevel pLevel, Goat pEntity, long pGameTime) {
      BlockPos $$3 = pEntity.blockPosition();
      Brain<?> $$4 = pEntity.getBrain();
      Vec3 $$5 = $$4.getMemory(MemoryModuleType.RAM_TARGET).get();
      this.ramDirection = new Vec3((double)$$3.getX() - $$5.x(), 0.0, (double)$$3.getZ() - $$5.z()).normalize();
      $$4.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget($$5, this.speed, 0));
   }

   protected void tick(ServerLevel pLevel, Goat pOwner, long pGameTime) {
      List<LivingEntity> $$3 = pLevel.getNearbyEntities(LivingEntity.class, this.ramTargeting, pOwner, pOwner.getBoundingBox());
      Brain<?> $$4 = pOwner.getBrain();
      if (!$$3.isEmpty()) {
         LivingEntity $$5 = $$3.get(0);
         DamageSource $$6 = pLevel.damageSources().noAggroMobAttack(pOwner);
         if ($$5.hurt($$6, (float)pOwner.getAttributeValue(Attributes.ATTACK_DAMAGE))) {
            EnchantmentHelper.doPostAttackEffects(pLevel, $$5, $$6);
         }

         int $$7 = pOwner.hasEffect(MobEffects.MOVEMENT_SPEED) ? pOwner.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() + 1 : 0;
         int $$8 = pOwner.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) ? pOwner.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() + 1 : 0;
         float $$9 = 0.25F * (float)($$7 - $$8);
         float $$10 = Mth.clamp(pOwner.getSpeed() * 1.65F, 0.2F, 3.0F) + $$9;
         float $$11 = $$5.isDamageSourceBlocked(pLevel.damageSources().mobAttack(pOwner)) ? 0.5F : 1.0F;
         $$5.knockback((double)($$11 * $$10) * this.getKnockbackForce.applyAsDouble(pOwner), this.ramDirection.x(), this.ramDirection.z());
         this.finishRam(pLevel, pOwner);
         pLevel.playSound(null, pOwner, this.getImpactSound.apply(pOwner), SoundSource.NEUTRAL, 1.0F, 1.0F);
      } else if (this.hasRammedHornBreakingBlock(pLevel, pOwner)) {
         pLevel.playSound(null, pOwner, this.getImpactSound.apply(pOwner), SoundSource.NEUTRAL, 1.0F, 1.0F);
         boolean $$12 = pOwner.dropHorn();
         if ($$12) {
            pLevel.playSound(null, pOwner, this.getHornBreakSound.apply(pOwner), SoundSource.NEUTRAL, 1.0F, 1.0F);
         }

         this.finishRam(pLevel, pOwner);
      } else {
         Optional<WalkTarget> $$13 = $$4.getMemory(MemoryModuleType.WALK_TARGET);
         Optional<Vec3> $$14 = $$4.getMemory(MemoryModuleType.RAM_TARGET);
         boolean $$15 = $$13.isEmpty() || $$14.isEmpty() || $$13.get().getTarget().currentPosition().closerThan($$14.get(), 0.25);
         if ($$15) {
            this.finishRam(pLevel, pOwner);
         }
      }
   }

   private boolean hasRammedHornBreakingBlock(ServerLevel pLevel, Goat pOwner) {
      Vec3 $$2 = pOwner.getDeltaMovement().multiply(1.0, 0.0, 1.0).normalize();
      BlockPos $$3 = BlockPos.containing(pOwner.position().add($$2));
      return pLevel.getBlockState($$3).is(BlockTags.SNAPS_GOAT_HORN) || pLevel.getBlockState($$3.above()).is(BlockTags.SNAPS_GOAT_HORN);
   }

   protected void finishRam(ServerLevel pLevel, Goat pOwner) {
      pLevel.broadcastEntityEvent(pOwner, (byte)59);
      pOwner.getBrain().setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, this.getTimeBetweenRams.apply(pOwner).sample(pLevel.random));
      pOwner.getBrain().eraseMemory(MemoryModuleType.RAM_TARGET);
   }
}
