package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class PrepareRamNearestTarget<E extends PathfinderMob> extends Behavior<E> {
   public static final int TIME_OUT_DURATION = 160;
   private final ToIntFunction<E> getCooldownOnFail;
   private final int minRamDistance;
   private final int maxRamDistance;
   private final float walkSpeed;
   private final TargetingConditions ramTargeting;
   private final int ramPrepareTime;
   private final Function<E, SoundEvent> getPrepareRamSound;
   private Optional<Long> reachedRamPositionTimestamp = Optional.empty();
   private Optional<PrepareRamNearestTarget.RamCandidate> ramCandidate = Optional.empty();

   public PrepareRamNearestTarget(
      ToIntFunction<E> pGetCooldownOnFall,
      int pMinRamDistance,
      int pMaxRamDistance,
      float pWalkSpeed,
      TargetingConditions pRamTargeting,
      int pRamPrepareTime,
      Function<E, SoundEvent> pGetPrepareRamSound
   ) {
      super(
         ImmutableMap.of(
            MemoryModuleType.LOOK_TARGET,
            MemoryStatus.REGISTERED,
            MemoryModuleType.RAM_COOLDOWN_TICKS,
            MemoryStatus.VALUE_ABSENT,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
            MemoryStatus.VALUE_PRESENT,
            MemoryModuleType.RAM_TARGET,
            MemoryStatus.VALUE_ABSENT
         ),
         160
      );
      this.getCooldownOnFail = pGetCooldownOnFall;
      this.minRamDistance = pMinRamDistance;
      this.maxRamDistance = pMaxRamDistance;
      this.walkSpeed = pWalkSpeed;
      this.ramTargeting = pRamTargeting;
      this.ramPrepareTime = pRamPrepareTime;
      this.getPrepareRamSound = pGetPrepareRamSound;
   }

   protected void start(ServerLevel pLevel, PathfinderMob pEntity, long pGameTime) {
      Brain<?> $$3 = pEntity.getBrain();
      $$3.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
         .flatMap(p_186049_ -> p_186049_.findClosest(p_147789_ -> this.ramTargeting.test(pEntity, p_147789_)))
         .ifPresent(p_147778_ -> this.chooseRamPosition(pEntity, p_147778_));
   }

   protected void stop(ServerLevel pLevel, E pEntity, long pGameTime) {
      Brain<?> $$3 = pEntity.getBrain();
      if (!$$3.hasMemoryValue(MemoryModuleType.RAM_TARGET)) {
         pLevel.broadcastEntityEvent(pEntity, (byte)59);
         $$3.setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, this.getCooldownOnFail.applyAsInt(pEntity));
      }
   }

   protected boolean canStillUse(ServerLevel pLevel, PathfinderMob pEntity, long pGameTime) {
      return this.ramCandidate.isPresent() && this.ramCandidate.get().getTarget().isAlive();
   }

   protected void tick(ServerLevel pLevel, E pOwner, long pGameTime) {
      if (!this.ramCandidate.isEmpty()) {
         pOwner.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.ramCandidate.get().getStartPosition(), this.walkSpeed, 0));
         pOwner.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.ramCandidate.get().getTarget(), true));
         boolean $$3 = !this.ramCandidate.get().getTarget().blockPosition().equals(this.ramCandidate.get().getTargetPosition());
         if ($$3) {
            pLevel.broadcastEntityEvent(pOwner, (byte)59);
            pOwner.getNavigation().stop();
            this.chooseRamPosition(pOwner, this.ramCandidate.get().target);
         } else {
            BlockPos $$4 = pOwner.blockPosition();
            if ($$4.equals(this.ramCandidate.get().getStartPosition())) {
               pLevel.broadcastEntityEvent(pOwner, (byte)58);
               if (this.reachedRamPositionTimestamp.isEmpty()) {
                  this.reachedRamPositionTimestamp = Optional.of(pGameTime);
               }

               if (pGameTime - this.reachedRamPositionTimestamp.get() >= (long)this.ramPrepareTime) {
                  pOwner.getBrain().setMemory(MemoryModuleType.RAM_TARGET, this.getEdgeOfBlock($$4, this.ramCandidate.get().getTargetPosition()));
                  pLevel.playSound(null, pOwner, this.getPrepareRamSound.apply(pOwner), SoundSource.NEUTRAL, 1.0F, pOwner.getVoicePitch());
                  this.ramCandidate = Optional.empty();
               }
            }
         }
      }
   }

   private Vec3 getEdgeOfBlock(BlockPos pPos, BlockPos pOther) {
      double $$2 = 0.5;
      double $$3 = 0.5 * (double)Mth.sign((double)(pOther.getX() - pPos.getX()));
      double $$4 = 0.5 * (double)Mth.sign((double)(pOther.getZ() - pPos.getZ()));
      return Vec3.atBottomCenterOf(pOther).add($$3, 0.0, $$4);
   }

   private Optional<BlockPos> calculateRammingStartPosition(PathfinderMob pPathfinder, LivingEntity pEntity) {
      BlockPos $$2 = pEntity.blockPosition();
      if (!this.isWalkableBlock(pPathfinder, $$2)) {
         return Optional.empty();
      } else {
         List<BlockPos> $$3 = Lists.newArrayList();
         BlockPos.MutableBlockPos $$4 = $$2.mutable();

         for (Direction $$5 : Direction.Plane.HORIZONTAL) {
            $$4.set($$2);

            for (int $$6 = 0; $$6 < this.maxRamDistance; $$6++) {
               if (!this.isWalkableBlock(pPathfinder, $$4.move($$5))) {
                  $$4.move($$5.getOpposite());
                  break;
               }
            }

            if ($$4.distManhattan($$2) >= this.minRamDistance) {
               $$3.add($$4.immutable());
            }
         }

         PathNavigation $$7 = pPathfinder.getNavigation();
         return $$3.stream().sorted(Comparator.comparingDouble(pPathfinder.blockPosition()::distSqr)).filter(p_147753_ -> {
            Path $$2x = $$7.createPath(p_147753_, 0);
            return $$2x != null && $$2x.canReach();
         }).findFirst();
      }
   }

   private boolean isWalkableBlock(PathfinderMob pPathfinder, BlockPos pPos) {
      return pPathfinder.getNavigation().isStableDestination(pPos)
         && pPathfinder.getPathfindingMalus(WalkNodeEvaluator.getPathTypeStatic(pPathfinder, pPos)) == 0.0F;
   }

   private void chooseRamPosition(PathfinderMob pPathfinder, LivingEntity pEntity) {
      this.reachedRamPositionTimestamp = Optional.empty();
      this.ramCandidate = this.calculateRammingStartPosition(pPathfinder, pEntity)
         .map(p_341352_ -> new PrepareRamNearestTarget.RamCandidate(p_341352_, pEntity.blockPosition(), pEntity));
   }

   public static class RamCandidate {
      private final BlockPos startPosition;
      private final BlockPos targetPosition;
      final LivingEntity target;

      public RamCandidate(BlockPos pStartPosition, BlockPos pTargetPosition, LivingEntity pTarget) {
         this.startPosition = pStartPosition;
         this.targetPosition = pTargetPosition;
         this.target = pTarget;
      }

      public BlockPos getStartPosition() {
         return this.startPosition;
      }

      public BlockPos getTargetPosition() {
         return this.targetPosition;
      }

      public LivingEntity getTarget() {
         return this.target;
      }
   }
}
