package net.minecraft.world.entity.ai.behavior;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.animal.allay.AllayAi;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GoAndGiveItemsToTarget<E extends LivingEntity & InventoryCarrier> extends Behavior<E> {
   private static final int CLOSE_ENOUGH_DISTANCE_TO_TARGET = 3;
   private static final int ITEM_PICKUP_COOLDOWN_AFTER_THROWING = 60;
   private final Function<LivingEntity, Optional<PositionTracker>> targetPositionGetter;
   private final float speedModifier;

   public GoAndGiveItemsToTarget(Function<LivingEntity, Optional<PositionTracker>> pTargetPositionGetter, float pSpeedModifier, int pDuration) {
      super(
         Map.of(
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.WALK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS,
            MemoryStatus.REGISTERED
         ),
         pDuration
      );
      this.targetPositionGetter = pTargetPositionGetter;
      this.speedModifier = pSpeedModifier;
   }

   @Override
   protected boolean checkExtraStartConditions(ServerLevel pLevel, E pOwner) {
      return this.canThrowItemToTarget(pOwner);
   }

   @Override
   protected boolean canStillUse(ServerLevel pLevel, E pEntity, long pGameTime) {
      return this.canThrowItemToTarget(pEntity);
   }

   @Override
   protected void start(ServerLevel pLevel, E pEntity, long pGameTime) {
      this.targetPositionGetter.apply(pEntity).ifPresent(p_217206_ -> BehaviorUtils.setWalkAndLookTargetMemories(pEntity, p_217206_, this.speedModifier, 3));
   }

   @Override
   protected void tick(ServerLevel pLevel, E pOwner, long pGameTime) {
      Optional<PositionTracker> $$3 = this.targetPositionGetter.apply(pOwner);
      if (!$$3.isEmpty()) {
         PositionTracker $$4 = $$3.get();
         double $$5 = $$4.currentPosition().distanceTo(pOwner.getEyePosition());
         if ($$5 < 3.0) {
            ItemStack $$6 = pOwner.getInventory().removeItem(0, 1);
            if (!$$6.isEmpty()) {
               throwItem(pOwner, $$6, getThrowPosition($$4));
               if (pOwner instanceof Allay $$7) {
                  AllayAi.getLikedPlayer($$7).ifPresent(p_217224_ -> this.triggerDropItemOnBlock($$4, $$6, p_217224_));
               }

               pOwner.getBrain().setMemory(MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS, 60);
            }
         }
      }
   }

   private void triggerDropItemOnBlock(PositionTracker pPositionTracker, ItemStack pStack, ServerPlayer pPlayer) {
      BlockPos $$3 = pPositionTracker.currentBlockPosition().below();
      CriteriaTriggers.ALLAY_DROP_ITEM_ON_BLOCK.trigger(pPlayer, $$3, pStack);
   }

   private boolean canThrowItemToTarget(E pTarget) {
      if (pTarget.getInventory().isEmpty()) {
         return false;
      } else {
         Optional<PositionTracker> $$1 = this.targetPositionGetter.apply(pTarget);
         return $$1.isPresent();
      }
   }

   private static Vec3 getThrowPosition(PositionTracker pPositionTracker) {
      return pPositionTracker.currentPosition().add(0.0, 1.0, 0.0);
   }

   public static void throwItem(LivingEntity pEntity, ItemStack pStack, Vec3 pThrowPos) {
      Vec3 $$3 = new Vec3(0.2F, 0.3F, 0.2F);
      BehaviorUtils.throwItem(pEntity, pStack, pThrowPos, $$3, 0.2F);
      Level $$4 = pEntity.level();
      if ($$4.getGameTime() % 7L == 0L && $$4.random.nextDouble() < 0.9) {
         float $$5 = Util.<Float>getRandom(Allay.THROW_SOUND_PITCHES, $$4.getRandom());
         $$4.playSound(null, pEntity, SoundEvents.ALLAY_THROW, SoundSource.NEUTRAL, 1.0F, $$5);
      }
   }
}
